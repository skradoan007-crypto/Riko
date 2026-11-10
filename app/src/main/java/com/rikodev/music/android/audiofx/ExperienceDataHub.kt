package com.rikodev.music.android.audiofx

import android.util.Log
import com.rikodev.music.android.core.util.RikoLog
import com.rikodev.music.android.domain.model.Song
import com.rikodev.music.android.service.PlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — EXPERIENCE DATA HUB
//
// The single writer of AudioState. All audio experience data flows
// through this component before reaching any engine or UI surface.
//
// Architecture:
//
//   PCM Processor ──────┐
//   Visualizer ────────┤
//   Player State ──────┤
//   Song Metadata ─────┤
//                        ▼
//   ═══════════════════════════
//   ║  EXPERIENCE DATA HUB  ║  ← single writer
//   ═══════════════════════════
//                        │
//                   AudioState
//                   StateFlow<AudioState>
//                        │
//   ─────────────────────┼────────────────────
//   │           │         │          │
//   Wave       Color    Motion    Glass
//   Engine     Engine   Engine    Engine
//
// ⭐ GOLDEN RULE ⭐
//   Nobody owns AudioState.
//   Everybody subscribes.
//   Only Experience Hub writes.
//
// RESPONSIBILITY:
//   - Collect raw audio frames from PCM processor and Visualizer
//   - Merge playback state from PlayerManager
//   - Merge song metadata from Song model
//   - Normalize all values to 0f..1f
//   - Apply smoothing to prevent jitter
//   - Detect beats and transients
//   - Track energy history (last 12 frames)
//   - Publish one unified StateFlow<AudioState>
//
// WHAT IT DOES NOT DO:
//   - Render anything (no Canvas, no Composable)
//   - Control playback (no ExoPlayer calls)
//   - Manage EQ (that's EqualizerEngine's job)
//   - Analyze offline files (that's AudioFileAnalyzer's job)
//   - Cache waveforms (that's WaveformRepository's job)
// ─────────────────────────────────────────────────────────────────────────────

private const val TAG = "ExperienceDataHub"

@Singleton
class ExperienceDataHub @Inject constructor(
    private val playerManager: PlayerManager,
    private val pcmAudioProcessor: PcmAudioProcessor,
    private val visualizerManager: VisualizerManager
) {

    // ── Output: The single source of truth ───────────────────────────────
    private val _audioState = MutableStateFlow(AudioState.IDLE)
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    // ── Internal state (not exposed) ──────────────────────────────────────
    private val normalizer = AudioNormalizer()
    private val smoother = AudioSmoother()
    private val energyHistory = EnergyHistory()

    // RUNTIME FIX 10: Bridge E (PCM) and Bridge F (Visualizer) each call
    // tryPublish() -> publishFrame() independently, from their own
    // Dispatchers.Default coroutines. Nothing stopped both from entering
    // publishFrame() at the same instant -- confirmed via a real device
    // crash log (ConcurrentModificationException in EnergyHistory, from
    // updatePlaybackState's pause-reset racing a concurrent publishFrame
    // read). EnergyHistory is now self-synchronized (see AudioBands.kt),
    // which fixes the exact crash; this lock additionally serializes the
    // Hub's OTHER shared mutable fields (previousEnergy, beatDecay,
    // lastBeatTime, lastFrameTime, activePcmFrame, activeVisualizerFrame)
    // so this class actually is the single, serialized writer its own
    // header comment already claims it is.
    private val lock = Any()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // RUNTIME FIX 3: the four bridge coroutines below used to be launched
    // via bare `scope.launch { }` calls whose returned Jobs were discarded —
    // `stopBridges()` called `bridgeJob?.cancel()` on a field that was NEVER
    // assigned, so it was a no-op. Same root pattern as RuntimeFix1
    // (VisualOsCoordinator) and RuntimeFix2 (WaveEngine): a background
    // collector kept running forever with no handle to stop it. Now every
    // launched Job is captured here so stopBridges() can actually cancel them.
    private val bridgeJobs = mutableListOf<Job>()

    // ── Beat detection state ──────────────────────────────────────────────
    private var previousEnergy = 0f
    private var beatDecay = 0f
    private var lastBeatTime = 0L
    private var lastFrameTime = 0L

    // ── Source priority ─────────────────────────────────────────────────
    // PCM > Visualizer > IDLE
    // When PCM data is flowing (real tap from ExoPlayer), we prefer it.
    // Visualizer is a fallback for devices where PCM tap doesn't work.
    private var activePcmFrame: AudioFrame? = null
    private var activeVisualizerFrame: AudioFrame? = null

    // ═════════════════════════════════════════════════════════════════════
    // PUBLIC API — Bridges (called by bridge adapters in 3.1D/3.1E/3.1F)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Feed a raw audio frame from the PCM processor.
     * Called on every PCM buffer callback (high frequency).
     */
    fun feedPcmFrame(frame: AudioFrame) = synchronized(lock) {
        activePcmFrame = frame
        tryPublish()
    }

    /**
     * Feed a raw audio frame from the Visualizer.
     * Called on every Visualizer waveform callback.
     */
    fun feedVisualizerFrame(frame: AudioFrame) = synchronized(lock) {
        activeVisualizerFrame = frame
        tryPublish()
    }

    /**
     * Update playback state from PlayerManager.
     * Called when isPlaying, position, duration, speed change.
     */
    fun updatePlaybackState(
        isPlaying: Boolean,
        position: Long,
        duration: Long,
        bufferedPosition: Long,
        playbackSpeed: Float
    ) = synchronized(lock) {
        val currentState = _audioState.value

        // Detect pause → reset history
        if (currentState.isPlaying && !isPlaying) {
            energyHistory.clear()
            smoother.reset()
            previousEnergy = 0f
            beatDecay = 0f
            activePcmFrame = null
            activeVisualizerFrame = null
        }

        _audioState.value = currentState.copy(
            isPlaying = isPlaying,
            position = position,
            duration = duration,
            bufferedPosition = bufferedPosition,
            playbackSpeed = playbackSpeed
        )

        // If not playing, publish idle audio data
        if (!isPlaying) {
            publishFrame(AudioFrame(source = AudioSource.IDLE))
        }
    }

    /**
     * Update song metadata when track changes.
     * Called from PlayerViewModel on media item transition.
     */
    fun updateMetadata(song: Song?) {
        if (song == null) return
        val currentState = _audioState.value
        _audioState.value = currentState.copy(
            metadata = AudioMetadata(
                title = song.title,
                sampleRate = song.sampleRate,
                channels = 0, // Will be updated by PCM processor
                bitrate = song.bitrate,
                duration = song.duration
            )
        )
    }

    /**
     * Update channel count from PCM processor config.
     */
    fun updateChannelInfo(channels: Int, sampleRate: Int) {
        val currentState = _audioState.value
        _audioState.value = currentState.copy(
            metadata = currentState.metadata.copy(
                channels = channels,
                sampleRate = sampleRate
            )
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // BRIDGE AUTO-SUBSCRIPTION
    // ═════════════════════════════════════════════════════════════════════

    private var isBridgesStarted = false

    /**
     * Start collecting from all three audio sources.
     * Call once at app init (e.g., from MainActivity or Application).
     * Safe to call multiple times — guards against duplicate subscriptions.
     *
     * Bridge D: PlayerManager → Hub (playback state + song metadata)
     * Bridge E: PcmAudioProcessor → Hub (PCM amplitude + diagnostics)
     * Bridge F: VisualizerManager → Hub (waveform + beat + status)
     *
     * Lifecycle: called from MainActivity.onStart() — paired with
     * stopBridges() on MainActivity.onStop() — so this only runs while the
     * app is actually visible. See RuntimeFix3.
     */
    fun startBridges() {
        if (isBridgesStarted) return
        isBridgesStarted = true
        RikoLog.d(TAG, "Starting audio bridges — PCM, Visualizer, Player")

        // Bridge D: Player state → Hub
        bridgeJobs += scope.launch {
            playerManager.playerState.collectLatest { playerState ->
                updatePlaybackState(
                    isPlaying = playerState.isPlaying,
                    position = playerState.position,
                    duration = playerState.duration,
                    bufferedPosition = playerState.bufferedPosition,
                    playbackSpeed = playerState.playbackSpeed
                )
                updateMetadata(playerState.currentSong)
            }
        }

        // Bridge E: PCM diagnostics → Hub (amplitude + config info)
        bridgeJobs += scope.launch {
            pcmAudioProcessor.diagnostics.collectLatest { diag ->
                if (diag.inputFormatConfigured) {
                    updateChannelInfo(diag.channelCount, diag.sampleRate)
                }
            }
        }

        // Bridge E continued: PCM amplitude + REAL FFT bands → AudioFrame → Hub
        // (Phase 2: bands now come from PcmAudioProcessor's actual spectrum
        // analysis — AudioNormalizer only estimates when bands are absent, so
        // the PCM path carries the song's true bass/mid/treble energies.)
        bridgeJobs += scope.launch {
            pcmAudioProcessor.amplitude.collectLatest { rms ->
                if (rms > 0f) {
                    val diag = pcmAudioProcessor.diagnostics.value
                    feedPcmFrame(
                        AudioFrame(
                            source = AudioSource.PCM,
                            rms = rms,
                            peak = diag.lastPeak,
                            bands = pcmAudioProcessor.bands.value,
                            channels = diag.channelCount,
                            sampleRate = diag.sampleRate,
                            timestamp = System.nanoTime()
                        )
                    )
                }
            }
        }

        // Bridge F: Visualizer waveform → AudioFrame → Hub
        bridgeJobs += scope.launch {
            visualizerManager.waveformData.collectLatest { waveformData ->
                if (waveformData.any { it > 0f }) {
                    feedVisualizerFrame(
                        AudioFrame(
                            source = AudioSource.VISUALIZER,
                            rms = visualizerManager.amplitude.value,
                            peak = waveformData.maxOrNull() ?: 0f,
                            waveform = waveformData,
                            isBeat = visualizerManager.isBeat.value,
                            isSilent = waveformData.all { it < AudioBands.SILENCE_THRESHOLD },
                            timestamp = System.nanoTime()
                        )
                    )
                }
            }
        }
    }

    /**
     * Stop all bridge subscriptions and reset.
     * Called from MainActivity.onStop() — see RuntimeFix3. Previously a
     * no-op (see comment on `bridgeJobs` above); now actually cancels the
     * four live collectors so nothing keeps sampling audio/player state
     * while the app is backgrounded.
     */
    fun stopBridges() {
        bridgeJobs.forEach { it.cancel() }
        bridgeJobs.clear()
        isBridgesStarted = false
        reset()
    }

    /**
     * Reset everything — called on app background, service destroy, etc.
     */
    fun reset() = synchronized(lock) {
        activePcmFrame = null
        activeVisualizerFrame = null
        energyHistory.clear()
        smoother.reset()
        previousEnergy = 0f
        beatDecay = 0f
        lastBeatTime = 0L
        lastFrameTime = 0L
        _audioState.value = AudioState.IDLE
    }

    // ═════════════════════════════════════════════════════════════════════
    // INTERNAL — Frame Processing Pipeline
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Select the best available frame and publish.
     * Priority: PCM > Visualizer > IDLE
     */
    private fun tryPublish() {
        val frame = activePcmFrame ?: activeVisualizerFrame
        if (frame != null) {
            publishFrame(frame)
        }
    }

    // ── Throttle: max 30Hz publish rate ──────────────────────────────────────
    // PCM/Visualizer callbacks fire at 50-100+Hz. Publishing at that rate
    // saturates the main thread with recompositions. Cap at ~33ms intervals.
    private val MIN_FRAME_INTERVAL_NS = 33_000_000L // 33ms = ~30fps

    /**
     * The core processing pipeline for a single audio frame.
     *
     * Pipeline: Normalize → Smooth → Detect Beat → Detect Transient →
     *           Update Envelope → Build AudioState → Publish
     */
    private fun publishFrame(rawFrame: AudioFrame) = synchronized(lock) {
        // Skip if not playing
        if (!_audioState.value.isPlaying && rawFrame.source != AudioSource.IDLE) {
            return@synchronized
        }

        val now = System.nanoTime()
        val frameInterval = if (lastFrameTime > 0) (now - lastFrameTime) else 0L

        // THROTTLE: skip if less than 33ms since last publish (max ~30fps)
        if (frameInterval in 1 until MIN_FRAME_INTERVAL_NS) {
            return@synchronized
        }

        lastFrameTime = now

        // Step 1: Normalize
        val normalized = normalizer.normalize(rawFrame)

        // Step 2: Smooth (prevents jitter)
        val smoothed = smoother.smooth(normalized)

        // Step 3: Beat detection
        val beatResult = detectBeat(smoothed, frameInterval)
        beatDecay *= 0.85f // decay beat confidence over time

        // Step 4: Transient detection
        val transientLevel = detectTransient(smoothed)

        // Step 5: Build bands and push to history
        val bands = AudioBands(
            bassEnergy = smoothed.bands.bassEnergy,
            midEnergy = smoothed.bands.midEnergy,
            trebleEnergy = smoothed.bands.trebleEnergy
        )
        energyHistory.push(bands)

        // Step 6: Envelope follower (smoothed energy)
        val envelope = smoother.envelope(smoothed.rms)

        // Step 7: Build the final AudioState
        val currentState = _audioState.value
        val newState = currentState.copy(
            energy = smoothed.bands.overallEnergy.coerceIn(0f, 1f),
            bassEnergy = bands.bassEnergy.coerceIn(0f, 1f),
            midEnergy = bands.midEnergy.coerceIn(0f, 1f),
            trebleEnergy = bands.trebleEnergy.coerceIn(0f, 1f),
            peak = smoothed.peak.coerceIn(0f, 1f),
            rms = smoothed.rms.coerceIn(0f, 1f),
            silenceLevel = if (bands.isSilent) 0f else (1f - smoothed.bands.overallEnergy),
            beatConfidence = beatDecay.coerceIn(0f, 1f),
            transient = transientLevel.coerceIn(0f, 1f),
            envelope = envelope.coerceIn(0f, 1f),
            waveform = smoothed.waveform,
            source = smoothed.source,
            timestamp = now,
            energyHistory = energyHistory
        )

        _audioState.value = newState
        previousEnergy = smoothed.bands.overallEnergy
    }

    // ═════════════════════════════════════════════════════════════════════
    // BEAT DETECTION
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Simple energy-based beat detection.
     * A beat is detected when the current energy spikes above the
     * running average by a threshold, with a minimum interval between
     * beats to prevent double-triggering.
     */
    private fun detectBeat(frame: AudioFrame, frameInterval: Long): Boolean {
        val currentEnergy = frame.bands.overallEnergy
        val avgEnergy = energyHistory.averageEnergy

        // Minimum 200ms between beats (prevents double-triggering)
        val minBeatInterval = 200_000_000L // 200ms in nanoseconds
        if (System.nanoTime() - lastBeatTime < minBeatInterval) return false

        // Beat = energy spike above running average
        val threshold = 1.4f // 40% above average = beat
        val isBeat = currentEnergy > avgEnergy * threshold &&
                     currentEnergy > AudioBands.SILENCE_THRESHOLD * 2f

        if (isBeat) {
            lastBeatTime = System.nanoTime()
            beatDecay = 1f // Full confidence, will decay
        }

        return isBeat
    }

    // ═════════════════════════════════════════════════════════════════════
    // TRANSIENT DETECTION
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Detect transients (sharp onsets like snare hits, cymbal crashes).
     * A transient is a rapid energy increase from one frame to the next.
     */
    private fun detectTransient(frame: AudioFrame): Float {
        val currentEnergy = frame.bands.overallEnergy
        val delta = currentEnergy - previousEnergy

        // Transient = sudden positive change, scaled by the magnitude
        return if (delta > 0.05f) (delta * 5f).coerceIn(0f, 1f) else 0f
    }
}
