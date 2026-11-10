package com.rikodev.music.android.audiofx

import android.media.audiofx.Visualizer
import android.util.Log
import com.rikodev.music.android.core.util.RikoLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — VISUALIZER MANAGER (with on-screen diagnostics)
//
// A previous version silently gave up on live capture and replaced it with a
// generated pattern — without telling the user, and without ever confirming
// WHY live capture wasn't working. That was wrong. This version tries live
// capture for real, and reports its exact status so it can be read directly
// off the screen (no ADB/logcat needed) — see VisualizerStatus below, wired
// to PlayerViewModel.visualizerStatus and shown in NowPlayingScreen.
// ─────────────────────────────────────────────────────────────────────────────

private const val TAG = "VisualizerManager"

/** Exact, human-readable state — shown directly on screen for diagnosis. */
sealed class VisualizerStatus {
    data object NotStarted : VisualizerStatus()
    data class Initializing(val sessionId: Int) : VisualizerStatus()
    data class Failed(val reason: String) : VisualizerStatus()
    /** Visualizer is running and callbacks are firing, but every sample so
     *  far has been silence/zero — the classic "capture allowed but the
     *  session gives nothing" OEM restriction. */
    data class ActiveButSilent(val callbackCount: Int) : VisualizerStatus()
    /** 3+ seconds in and not ONE callback has fired — enabled=true
     *  succeeded, no exception, but the OS never delivers any audio
     *  frames at all. This is the strongest signal of an OEM/ROM-level
     *  capture restriction that can't be fixed from app code. */
    data class NoCallbacksReceived(val sessionId: Int) : VisualizerStatus()
    /** Genuinely receiving real, non-zero audio data. */
    data class Live(val callbackCount: Int, val lastPeakAmplitude: Float) : VisualizerStatus()
}

@Singleton
class VisualizerManager @Inject constructor() {

    private var visualizer: Visualizer? = null

    private val _waveformData = MutableStateFlow<FloatArray>(FloatArray(128) { 0f })
    val waveformData: StateFlow<FloatArray> = _waveformData.asStateFlow()

    private val _isBeat = MutableStateFlow(false)
    val isBeat: StateFlow<Boolean> = _isBeat.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _status = MutableStateFlow<VisualizerStatus>(VisualizerStatus.NotStarted)
    val status: StateFlow<VisualizerStatus> = _status.asStateFlow()

    private var isInitialized = false
    private var callbackCount = 0
    private var maxAmplitudeSeenEver = 0f
    // PERF P1 (RC-03): double-buffer — zero per-callback allocation
    // (was FloatArray + copyOf ≈1KB per callback). Alternating refs also keep
    // StateFlow emissions firing (same-ref publish would be swallowed).
    private var frontBuffer = FloatArray(128) { 0f }
    private var backBuffer = FloatArray(128) { 0f }
    private var lastSessionId = -1
    private val SMOOTHING_FACTOR = 0.35f
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timeoutJob: Job? = null

    fun initialize(audioSessionId: Int) {
        // PERF P3 (RC-13): same-session guard — STATE_READY fires on every
        // rebuffer/seek; re-creating Visualizer for the same session is churn.
        if (isInitialized && audioSessionId == lastSessionId && visualizer != null) return
        lastSessionId = audioSessionId
        release(keepSession = true)
        callbackCount = 0
        maxAmplitudeSeenEver = 0f
        _status.value = VisualizerStatus.Initializing(audioSessionId)
        RikoLog.d(TAG, "Initializing Visualizer with session ID: $audioSessionId")

        // If not ONE callback has arrived after 3s, say so explicitly
        // instead of silently sitting at "Initializing" forever.
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(3000)
            if (callbackCount == 0) {
                Log.w(TAG, "No Visualizer callbacks received after 3s (session $audioSessionId)")
                _status.value = VisualizerStatus.NoCallbacksReceived(audioSessionId)
            }
        }

        try {
            visualizer = Visualizer(audioSessionId).apply {
                enabled = false

                // Was using the minimum capture size — some devices behave
                // oddly with the smallest allowed buffer and silently never
                // fire callbacks. Trying a mid-range size instead.
                val range = Visualizer.getCaptureSizeRange()
                captureSize = ((range[0] + range[1]) / 2).let { mid ->
                    // round to nearest power of two, Visualizer requires it
                    var size = range[0]
                    while (size * 2 <= mid) size *= 2
                    size
                }
                RikoLog.d(TAG, "Capture size range: ${range[0]}..${range[1]}, using $captureSize")

                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onFftDataCapture(
                            visualizer: Visualizer,
                            fft: ByteArray,
                            samplingRate: Int
                        ) {
                            // We use waveform, not FFT — ignore this
                        }

                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer,
                            waveform: ByteArray,
                            samplingRate: Int
                        ) {
                            processWaveform(waveform)
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )

                enabled = true
                isInitialized = true
                RikoLog.d(TAG, "Visualizer enabled successfully")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Visualizer failed — permission not granted: ${e.message}")
            isInitialized = false
            _status.value = VisualizerStatus.Failed("No RECORD_AUDIO permission: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Visualizer initialization failed: ${e.message}")
            isInitialized = false
            _status.value = VisualizerStatus.Failed("${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun processWaveform(waveform: ByteArray) {
        callbackCount++
        val targetSize = 128
        val step = (waveform.size / targetSize).coerceAtLeast(1)

        var rmsSum = 0.0
        var peak = 0f
        val prev = frontBuffer
        val next = backBuffer
        for (i in 0 until targetSize) {
            val sampleIndex = (i * step).coerceAtMost(waveform.size - 1)
            val sample = (waveform[sampleIndex].toInt() and 0xFF) - 128
            val abs = abs(sample) / 128f
            // Smooth from last published frame into the back buffer (no alloc).
            next[i] = prev[i] * (1f - SMOOTHING_FACTOR) + abs * SMOOTHING_FACTOR
            rmsSum += abs * abs
            if (abs > peak) peak = abs
        }
        if (peak > maxAmplitudeSeenEver) maxAmplitudeSeenEver = peak

        // Swap + publish the fresh ref. Single binder thread writes;
        // collectors only read published refs, never mutated after publish.
        frontBuffer = next
        backBuffer = prev
        _waveformData.value = next

        val rms = sqrt(rmsSum / targetSize).toFloat()
        _amplitude.value = rms
        // Distinct beat gate — was an emission on every callback even unchanged.
        val beat = rms > 0.15f
        if (beat != _isBeat.value) _isBeat.value = beat

        // Update diagnostic status every ~15 callbacks so it's readable,
        // not flickering every frame.
        if (callbackCount % 15 == 0 || callbackCount == 1) {
            _status.value = if (maxAmplitudeSeenEver > 0.01f) {
                VisualizerStatus.Live(callbackCount, maxAmplitudeSeenEver)
            } else {
                VisualizerStatus.ActiveButSilent(callbackCount)
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            visualizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "setEnabled failed: ${e.message}")
        }
    }

    fun release(keepSession: Boolean = false) {
        timeoutJob?.cancel()
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
            isInitialized = false
            if (!keepSession) lastSessionId = -1
            _waveformData.value = FloatArray(128) { 0f }
            _amplitude.value = 0f
            _isBeat.value = false
        } catch (e: Exception) {
            Log.e(TAG, "release failed: ${e.message}")
        }
    }
}
