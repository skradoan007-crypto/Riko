package com.rikodev.music.android.audiofx

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.rikodev.music.android.core.util.RikoLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import com.rikodev.music.android.audiofx.visualizer.SpectrumConfig
import com.rikodev.music.android.audiofx.visualizer.SpectrumEngine
import com.rikodev.music.android.audiofx.visualizer.SpectrumFrameDelay
import kotlin.math.abs
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — PCM AUDIO PROCESSOR (Phase 2: real frequency-band analysis)
//
// Sits directly inside ExoPlayer's own audio pipeline (Decoder → [HERE] →
// AudioSink → speaker). This is OUR OWN decoded audio, not system-level
// capture — no RECORD_AUDIO permission needed, no dependency on the
// OEM-restricted android.media.audiofx.Visualizer path at all.
//
// Reads real PCM bytes as they flow through:
//   1. RMS + peak (original Phase 1 diagnostics — passthrough untouched)
//   2. REAL frequency-band analysis (Phase 2 — the "soul" input):
//      a throttled radix-2 FFT over the newest 1024 samples derives actual
//      bass (20–250Hz) / mid (250Hz–4kHz) / treble (4kHz–20kHz) energies.
//      This replaces the AudioNormalizer's RMS-estimated bands for the PCM
//      path — Wave Engine's bass/mid/overall reactivity now runs on the
//      song's true spectrum, not a heuristic distribution.
//
// THREADING: queueInput() runs on ExoPlayer's audio processing thread.
// The FFT is preallocated (zero per-frame allocation) and throttled to
// once per ~16ms; a 1024-point FFT costs ~0.05ms — playback is never
// perceptibly affected. Passthrough remains byte-exact.
// ─────────────────────────────────────────────────────────────────────────────

private const val TAG = "PcmAudioProcessor"

/** FFT window size — must be a power of two. 1024 @44.1kHz ≈ 23ms resolution. */
private const val FFT_SIZE = 1024

/** Minimum gap between FFT computations (nanoseconds) — throttles audio-thread cost. */
private const val FFT_INTERVAL_NS = 16_000_000L

/**
 * Bass/mid crossovers for the Wave Engine's three energies (Hz). The energies
 * are the MEAN of the canonical 40 log bands in each range, so the ribbon and
 * the spectrum are computed from identical numbers.
 */
private const val BASS_TOP_HZ = 250f
private const val MID_TOP_HZ = 4000f

/**
 * Minimum gap between StateFlow emissions (nanoseconds) — PERF P1 (RC-04).
 * queueInput runs per audio buffer (dozens-hundreds/sec); every emission
 * wakes ExperienceDataHub collectors + Compose. 33ms caps UI-bound flow
 * traffic at ~30Hz. Passthrough audio is unaffected (emissions are read-only taps).
 */
private const val EMIT_INTERVAL_NS = 33_000_000L

data class PcmDiagnostics(
    val isActive: Boolean = false,
    val inputFormatConfigured: Boolean = false,
    val sampleRate: Int = 0,
    val channelCount: Int = 0,
    val bufferCallbackCount: Long = 0,
    val totalBytesProcessed: Long = 0,
    val lastRms: Float = 0f,
    val lastPeak: Float = 0f,
    val maxPeakEver: Float = 0f
)

@Singleton
class PcmAudioProcessor @Inject constructor() : BaseAudioProcessor() {

    private val _diagnostics = MutableStateFlow(PcmDiagnostics())
    val diagnostics: StateFlow<PcmDiagnostics> = _diagnostics.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    /** Real FFT-derived band energies (0f..1f) — the Wave Engine's reactive input. */
    private val _bands = MutableStateFlow(AudioBands())
    val bands: StateFlow<AudioBands> = _bands.asStateFlow()

    // ── Spectrum workspace — the CANONICAL DSP engine owns the FFT ──
    // This class no longer carries its own radix-2 / window / band mapping.
    // It owns only the throttles, the smoothed band snapshot and the frame
    // emission. One algorithm, one owner (SpectrumEngine).
    /** Optional real-time undelayed spectrum consumer for the MDNA timeline. */
    @Volatile var rawTap: RawSpectrumTap? = null

    private val engine = SpectrumEngine(FFT_SIZE)
    private val specTarget = FloatArray(SpectrumBands.COUNT)
    private val spectrumSmoother = SpectrumBands.Smoother()

    // A/V compensation: the tap runs ahead of the speaker by roughly one
    // AudioTrack buffer, so frames are published through a short ring sized
    // from SpectrumConfig.AV_SYNC_DELAY_MS. Visualization-only.
    private val spectrumDelay = SpectrumFrameDelay(
        bandCount = SpectrumBands.COUNT,
        slotCount = SpectrumFrameDelay.slotsFor(
            SpectrumConfig.AV_SYNC_DELAY_MS,
            EMIT_INTERVAL_NS
        )
    )
    private val _spectrum = MutableStateFlow(SpectrumFrame.IDLE)
    val spectrum: StateFlow<SpectrumFrame> = _spectrum.asStateFlow()
    private var lastSpectrumEmitNanos = 0L
    private var lastFftNanos = 0L
    private var lastEmitNanos = 0L

    // ── Negotiated input format (visualizer_plan §21 Phase-1 matrix) ──
    // ExoPlayer can hand the sink 16-bit OR float PCM, mono OR stereo. The
    // analysis tap must read the format it was actually given; assuming
    // 16-bit stereo would turn float frames into noise. Passthrough is
    // unaffected either way — we never transform bytes in either direction.
    private var configuredEncoding: Int = C.ENCODING_PCM_16BIT
    private var configuredChannels: Int = 2

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        RikoLog.d(TAG, "onConfigure: sampleRate=${inputAudioFormat.sampleRate}, " +
                "channelCount=${inputAudioFormat.channelCount}, encoding=${inputAudioFormat.encoding}")
        _diagnostics.value = _diagnostics.value.copy(
            inputFormatConfigured = true,
            sampleRate = inputAudioFormat.sampleRate,
            channelCount = inputAudioFormat.channelCount
        )
        // Remember the negotiated format for the analysis tap. Media3's
        // AudioSink only ever emits PCM_16BIT or PCM_FLOAT, but we still
        // guard: anything else is analysed as 16-bit and SAYS SO in logcat
        // instead of silently producing a meaningless spectrum.
        configuredEncoding = inputAudioFormat.encoding
        configuredChannels = inputAudioFormat.channelCount.coerceAtLeast(1)
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            Log.w(
                TAG,
                "Unsupported analysis encoding=${inputAudioFormat.encoding}; " +
                    "analysing as 16-bit. Playback is unaffected (passthrough)."
            )
        }
        // Passthrough — same format out as in. We only READ, never transform.
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        // Read samples for analysis WITHOUT consuming the buffer — we still
        // need to hand the exact same bytes downstream untouched.
        val duplicate = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        var sumSquares = 0.0
        var peak = 0f
        var sampleCount = 0

        // Frame-accurate, format-aware read (16-bit / float, mono / stereo).
        val frameStride = frameStrideBytes()
        val firstByte = duplicate.position()
        val frameCount = PcmFrames.frameCount(duplicate, firstByte, frameStride)
        for (f in 0 until frameCount) {
            val sample = monoFrame(duplicate, firstByte + f * frameStride)
            val absSample = abs(sample)
            sumSquares += (sample * sample).toDouble()
            if (absSample > peak) peak = absSample
            sampleCount++
        }

        if (sampleCount > 0) {
            val rms = sqrt(sumSquares / sampleCount).toFloat()
            val now = System.nanoTime()
            // Throttled UI-bound emissions — audio passthrough below is untouched.
            if (now - lastEmitNanos >= EMIT_INTERVAL_NS) {
                lastEmitNanos = now
                _amplitude.value = rms
                val diag = _diagnostics.value
                _diagnostics.value = diag.copy(
                    isActive = true,
                    bufferCallbackCount = diag.bufferCallbackCount + 1,
                    totalBytesProcessed = diag.totalBytesProcessed + remaining,
                    lastRms = rms,
                    lastPeak = peak,
                    maxPeakEver = maxOf(diag.maxPeakEver, peak)
                )
            }

            // Throttled real spectrum analysis on the newest FFT_SIZE samples.
            // G-19: reuse the RMS pass's duplicate (absolute reads never move
            // its position) — was a second inputBuffer.duplicate() per chunk.
            if (now - lastFftNanos >= FFT_INTERVAL_NS) {
                lastFftNanos = now
                computeBandEnergies(duplicate, sampleRate = _diagnostics.value.sampleRate)
            }
        }

        // Passthrough — copy input to output buffer completely unmodified.
        // This is the part that MUST NOT break playback.
        val outputBuffer = replaceOutputBuffer(remaining)
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()
    }

    /**
     * One canonical DSP pass over the buffer: newest window → mono → Hann →
     * FFT → magnitude → dB → 40 log bands, then the Wave Engine's three
     * energies derived FROM those same bands (one band map, not two).
     *
     * Runs on the audio thread — preallocated workspace, throttled, and
     * format-aware; passthrough is unaffected.
     *
     * @param view an ABSOLUTE-read duplicate owned by [queueInput] (G-19: no
     * second duplicate per chunk). Never consumed — all reads are indexed.
     */
    private fun computeBandEnergies(view: ByteBuffer, sampleRate: Int) {
        if (sampleRate <= 0) return
        val frameStride = frameStrideBytes()
        val firstByte = view.position()
        val totalFrames = PcmFrames.frameCount(view, firstByte, frameStride)
        if (totalFrames < FFT_SIZE / 4) return // not enough audio yet

        // Newest window: seek/pause transitions fall out on their own (no
        // stale-spectrum latch). Channel-accurate downmix; stereo never assumed.
        val windowFirstFrame = (totalFrames - FFT_SIZE).coerceAtLeast(0)
        engine.analyze(
            buffer = view,
            startByte = firstByte + windowFirstFrame * frameStride,
            frameCount = totalFrames - windowFirstFrame,
            encoding = configuredEncoding,
            channels = configuredChannels,
            sampleRateHz = sampleRate,
            outTargets = specTarget
        )

        // Wave-Engine energies — the mean of the canonical bands in each range
        // (bass ≤250Hz · mid 250Hz–4kHz · treble ≥4kHz). These are the SAME
        // numbers the visualizer draws, so the ribbon and the spectrum can
        // never disagree about what the song is doing.
        val bassEnd = SpectrumBands.bandIndex(BASS_TOP_HZ, sampleRate)
        val midEnd = SpectrumBands.bandIndex(MID_TOP_HZ, sampleRate)
        _bands.value = AudioBands(
            bassEnergy = meanBands(specTarget, 0, bassEnd),
            midEnergy = meanBands(specTarget, bassEnd, midEnd),
            trebleEnergy = meanBands(specTarget, midEnd, SpectrumBands.COUNT)
        )

        // 40-band log spectrum for AuroraSpectrum — throttled to ~30Hz.
        // A/V compensation: the frame is published through the delay ring so
        // the bars land on the beat the SPEAKER is playing, not the one the
        // decoder just read. Snapshot copies keep the UI off the audio
        // thread's working arrays.
        val nowSpec = System.nanoTime()
        // Send the undelayed FFT bands before the UI-only A/V delay ring.
        // This callback is only attached while MDNA is visible.
        try {
            rawTap?.onFrame(specTarget, nowSpec)
        } catch (e: Exception) {
            Log.w(TAG, "Raw spectrum consumer rejected a frame", e)
        }
        if (nowSpec - lastSpectrumEmitNanos >= EMIT_INTERVAL_NS) {
            lastSpectrumEmitNanos = nowSpec
            spectrumSmoother.update(specTarget, nowSpec)
            val due = spectrumDelay.push(spectrumSmoother.levels, spectrumSmoother.peaks)
            if (due != SpectrumFrameDelay.NOT_READY) {
                _spectrum.value = SpectrumFrame(
                    bands = spectrumDelay.levelsAt(due).copyOf(),
                    peaks = spectrumDelay.peaksAt(due).copyOf(),
                    sampleRate = sampleRate,
                    timestampNanos = nowSpec,
                    isActive = true
                )
            }
        }
    }

    /** Mean of [targets] over the half-open band range [from, to). */
    private fun meanBands(targets: FloatArray, from: Int, to: Int): Float {
        if (to <= from) return 0f
        var sum = 0f
        for (b in from until to.coerceAtMost(targets.size)) sum += targets[b]
        return (sum / (to - from)).coerceIn(0f, 1f)
    }

    /**
     * Bytes per interleaved frame for the negotiated encoding/channels.
     * Delegates to the pure, unit-tested [PcmFrames] (visualizer_plan §21).
     */
    private fun frameStrideBytes(): Int =
        PcmFrames.strideBytes(configuredEncoding, configuredChannels)

    /**
     * Mono-mixes the interleaved frame that begins at [frameByteOffset].
     * Delegates to the pure, unit-tested [PcmFrames]: absolute reads only, so
     * the buffer's own position is never moved and the passthrough copy
     * downstream stays byte-exact.
     */
    private fun monoFrame(buffer: ByteBuffer, frameByteOffset: Int): Float =
        PcmFrames.monoFrame(buffer, frameByteOffset, configuredEncoding, configuredChannels)

    override fun onQueueEndOfStream() {
        // Nothing buffered internally to flush — passthrough only.
    }

    override fun onFlush() {
        RikoLog.d(TAG, "onFlush — seek or track change")
        // Never let the next track/seek emit ribbons from the previous PCM frame.
        _amplitude.value = 0f
        lastEmitNanos = 0L
        _bands.value = AudioBands()
        _spectrum.value = SpectrumFrame.IDLE
        spectrumSmoother.reset()
        spectrumDelay.clear()
        try {
            rawTap?.onReset()
        } catch (e: Exception) {
            Log.w(TAG, "Raw spectrum consumer reset failed", e)
        }
        lastFftNanos = 0L
        lastSpectrumEmitNanos = 0L
    }

    override fun onReset() {
        RikoLog.d(TAG, "onReset")
        // The analysis format is re-negotiated by the next onConfigure();
        // clear it so a stale encoding can never outlive its pipeline.
        configuredEncoding = C.ENCODING_PCM_16BIT
        configuredChannels = 2
        _diagnostics.value = PcmDiagnostics()
        _amplitude.value = 0f
        _bands.value = AudioBands()
        _spectrum.value = SpectrumFrame.IDLE
        spectrumSmoother.reset()
        spectrumDelay.clear()
        try {
            rawTap?.onReset()
        } catch (e: Exception) {
            Log.w(TAG, "Raw spectrum consumer reset failed", e)
        }
        lastFftNanos = 0L
        lastSpectrumEmitNanos = 0L
    }

    // No isActive() override needed — BaseAudioProcessor's own implementation
    // already reports "active" once onConfigure() has accepted a format
    // (which ours always does, for passthrough), so queueInput() gets called
    // normally without needing to reimplement that check here.
}
