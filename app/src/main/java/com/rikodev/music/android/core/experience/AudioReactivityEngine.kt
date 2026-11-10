package com.rikodev.music.android.core.experience

import android.media.audiofx.Visualizer
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

// Manifest: <uses-permission android:name="android.permission.RECORD_AUDIO" />
// Required even though no microphone audio is captured — Visualizer is
// gated behind it for privacy reasons.

class AudioReactivityEngine(private val player: ExoPlayer) {

    private var visualizer: Visualizer? = null
    private val _intensity = MutableStateFlow(0f)
    val intensity: StateFlow<Float> = _intensity.asStateFlow()

    // Attack/release envelope so bass hits feel punchy without flickering.
    private var envelope = 0f
    private val attack = 0.6f
    private val release = 0.08f

    fun start() {
        val sessionId = player.audioSessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET) return

        try {
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onFftDataCapture(viz: Visualizer, fft: ByteArray, samplingRate: Int) {
                            _intensity.value = extractBassEnergy(fft)
                        }
                        override fun onWaveFormDataCapture(viz: Visualizer, waveform: ByteArray, samplingRate: Int) {
                            // unused — FFT already gives us the bass envelope
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    /* waveform = */ false,
                    /* fft = */ true,
                )
                enabled = true
            }
        } catch (e: Exception) {
            // Visualizer initialization can fail on some devices or if permissions are missing.
            e.printStackTrace()
        }
    }

    // Android's documented FFT packing:
    //   fft[0]     = Re(0)
    //   fft[1]     = Re(n/2)
    //   fft[2k]    = Re(k)      for k in 1..n/2-1
    //   fft[2k+1]  = Im(k)      for k in 1..n/2-1
    private fun extractBassEnergy(fft: ByteArray): Float {
        val bassBins = 2..10 // low-frequency bins for a typical capture size
        var sum = 0f
        for (k in bassBins) {
            val idx = 2 * k
            if (idx + 1 >= fft.size) break
            val re = fft[idx].toInt()
            val im = fft[idx + 1].toInt()
            sum += sqrt((re * re + im * im).toFloat())
        }
        val normalized = (sum / (bassBins.count() * 128f)).coerceIn(0f, 1f)
        envelope += (normalized - envelope) * if (normalized > envelope) attack else release
        return envelope
    }

    fun stop() {
        try {
            visualizer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        visualizer = null
        envelope = 0f
        _intensity.value = 0f
    }
}
