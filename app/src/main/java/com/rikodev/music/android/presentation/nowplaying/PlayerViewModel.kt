package com.rikodev.music.android.presentation.nowplaying

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.music.android.audiofx.EqualizerEngine
import com.rikodev.music.android.audiofx.AudioState
import com.rikodev.music.android.audiofx.ExperienceDataHub
import com.rikodev.music.android.audiofx.PcmAudioProcessor
import com.rikodev.music.android.audiofx.SpectrumFrame
import com.rikodev.music.android.audiofx.WaveformRepository
import com.rikodev.music.android.core.experience.ColorIntelligenceEngine
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.WaveDiagnostics
import com.rikodev.music.android.core.experience.wave.WaveEngine
import com.rikodev.music.android.core.experience.wave.WaveState
import com.rikodev.music.android.core.util.ColorExtractor
import com.rikodev.music.android.data.db.dao.PlayHistoryDao
import com.rikodev.music.android.data.db.entity.PlayHistoryEntity
import com.rikodev.music.android.data.repository.PlaylistRepository
import com.rikodev.music.android.data.repository.SettingsRepository
import com.rikodev.music.android.data.repository.VisualizerPrefs
import com.rikodev.music.android.domain.model.AlbumArtColors
import com.rikodev.music.android.domain.model.AudioFxState
import com.rikodev.music.android.domain.model.PlayerState
import com.rikodev.music.android.domain.model.Playlist
import com.rikodev.music.android.domain.model.QueueItem
import com.rikodev.music.android.domain.model.RepeatMode
import com.rikodev.music.android.domain.model.ShuffleMode
import com.rikodev.music.android.domain.model.EqPreset
import com.rikodev.music.android.domain.model.Song
import com.rikodev.music.android.service.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import com.rikodev.music.android.presentation.miniplayer.MiniPlayerAction
import com.rikodev.music.android.presentation.miniplayer.MiniPlayerEvent
import com.rikodev.music.android.presentation.miniplayer.MiniPlayerMapper
import com.rikodev.music.android.presentation.miniplayer.MiniPlayerReducer
import com.rikodev.music.android.presentation.miniplayer.MiniPlayerState
import com.rikodev.music.android.presentation.miniplayer.MiniPlayerStateFactory
import com.rikodev.music.android.presentation.miniplayer.PressTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — PLAYER VIEWMODEL (Experience OS Integration)
//
// Step 3.3I — Cleaned up: legacy waveform/amplitude/beat flows removed.
// Build-2  — Mini Player 2.0 Experience Bridge added.
//
// This ViewModel is now the bridge between the UI layer and the Experience OS.
// It exposes clean, unified state flows:
//
//   playerState      — playback state (song, position, playing/paused)
//   audioState        — unified audio data (Experience Hub — single source of truth)
//   colorState        — intelligence-driven colors (Color Brain)
//   waveState         — ribbon state + geometry + highlight (Wave Engine)
//   waveDiagnostics   — solver performance + engine debug data (Inspector)
//   realWaveformState — offline waveform envelope (decoded from file, separate concern)
//   audioFxState      — EQ/Virtualizer/BassBoost state (separate from audio analysis)
//   dynamicColors     — legacy bridge for existing screens (will migrate to colorState)
//
//   ⭐ miniPlayerState — Build-2: Mini Player 2.0 complete UI state
//
// REMOVED (Step 3.3I):
//   ❌ waveformData     → Use audioState.waveform
//   ❌ visualizerStatus  → Use audioState.source
//   ❌ pcmDiagnostics    → Use audioState.rms / audioState.energy
//   ❌ amplitude         → Use audioState.energy
//   ❌ visualizerManager (constructor) → No longer needed
//   ❌ pcmAudioProcessor (constructor)  → No longer needed
//
// ⭐ GOLDEN RULES ⭐
//   Nobody creates AudioState.  Only Experience Hub writes.
//   Nobody creates ColorState.  Only Color Brain writes.
//   Nobody creates WaveState.   Only Wave Engine writes.
//   This ViewModel ONLY subscribes.
//
// ⭐ BUILD-2 RULE ⭐
//   No UI reads raw Experience OS.
//   Mini Player components read ONLY MiniPlayerState.
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository,
    private val audioFxManager: EqualizerEngine,
    private val experienceDataHub: ExperienceDataHub,
    private val waveformRepository: WaveformRepository,
    private val colorExtractor: ColorExtractor,
    private val colorIntelligenceEngine: ColorIntelligenceEngine,
    private val waveEngine: WaveEngine,
    private val playHistoryDao: PlayHistoryDao,
    private val pcmAudioProcessor: PcmAudioProcessor
) : ViewModel() {

    // ═════════════════════════════════════════════════════════════════════
    // ⭐ UNIFIED AUDIO STATE — Experience Hub
    // ═════════════════════════════════════════════════════════════════════
    val audioState: StateFlow<AudioState> = experienceDataHub.audioState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioState.IDLE)

    // ═════════════════════════════════════════════════════════════════════
    // ⭐ WAVE STATE — Wave Engine Output
    // ═════════════════════════════════════════════════════════════════════
    val waveState: StateFlow<WaveState> = waveEngine.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WaveState.IDLE)

    val waveDiagnostics: StateFlow<WaveDiagnostics> = waveEngine.diagnostics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WaveDiagnostics.IDLE)

    // ═════════════════════════════════════════════════════════════════════
    // ⭐ SPECTRUM — 40-band visualizer frames (PCM tap, audio thread)
    // ═════════════════════════════════════════════════════════════════════
    // Direct processor subscription, NOT via the hub: the hub carries the
    // 3-band AudioState for engines; the 40-band frame is visualizer-only
    // traffic for AuroraSpectrum. Stale/absent frames → renderer falls back
    // to energy interpolation, so offload/passthrough paths degrade
    // gracefully and playback is never affected.
    /** Direct playback PCM; timeline reads this only while visible and playing. */
    val timelineAmplitude: StateFlow<Float> = pcmAudioProcessor.amplitude

    val spectrumState: StateFlow<SpectrumFrame> = pcmAudioProcessor.spectrum
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SpectrumFrame.IDLE)

    // ⭐ VISUALIZER LAB — persisted user configuration (presets, theme,
    // sensitivity, glow, density, on/off). Single source of truth for the
    // Lab UI; every change is written atomically via updateVisualizerPrefs.
    val visualizerPrefs: StateFlow<VisualizerPrefs> = settingsRepository.visualizerPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VisualizerPrefs())

    fun updateVisualizerPrefs(transform: (VisualizerPrefs) -> VisualizerPrefs) {
        viewModelScope.launch {
            settingsRepository.setVisualizerPrefs(transform(visualizerPrefs.value))
        }
    }

    // Plan 5.5: user studio presets — renderer stays blind to persistence.
    val userVisualizerPresets: kotlinx.coroutines.flow.StateFlow<List<com.rikodev.music.android.data.repository.UserVisualizerPreset>> =
        settingsRepository.userVisualizerPresets.stateIn(
            viewModelScope,
            kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun saveVisualizerPreset(name: String) {
        viewModelScope.launch {
            val p = visualizerPrefs.value
            settingsRepository.saveUserVisualizerPreset(
                com.rikodev.music.android.data.repository.UserVisualizerPreset(
                    id = "user_${System.currentTimeMillis()}",
                    name = name,
                    styleName = p.styleName,
                    themeName = p.theme.name,
                    sensitivity = p.sensitivity,
                    glow = p.glowAmount,
                    density = p.bandDensity,
                    shape = p.shapeName,
                    motionSpeed = p.motionSpeed,
                    reactivityCurve = p.reactivityCurve,
                    barWidth = p.barWidth,
                    barSpacing = p.barSpacing,
                    smoothing = p.smoothing,
                    peakDecay = p.peakDecay,
                    reflection = p.reflectionEnabled,
                    mirror = p.mirrorEnabled
                )
            )
        }
    }

    fun applyVisualizerPreset(preset: com.rikodev.music.android.data.repository.UserVisualizerPreset) {
        updateVisualizerPrefs {
            it.copy(
                styleName = preset.styleName,
                theme = runCatching {
                    com.rikodev.music.android.data.repository.VisualizerTheme.valueOf(preset.themeName)
                }.getOrDefault(it.theme),
                sensitivity = preset.sensitivity.coerceIn(0.5f, 1.5f),
                glowAmount = preset.glow.coerceIn(0f, 1f),
                bandDensity = if (preset.density in listOf(32, 40, 48)) preset.density else it.bandDensity,
                shapeName = preset.shape,
                motionSpeed = preset.motionSpeed.coerceIn(0.25f, 2f),
                reactivityCurve = preset.reactivityCurve.coerceIn(0.25f, 2f),
                barWidth = preset.barWidth.coerceIn(0.5f, 2f),
                barSpacing = preset.barSpacing.coerceIn(0f, 1f),
                smoothing = preset.smoothing.coerceIn(0f, 1f),
                peakDecay = preset.peakDecay.coerceIn(0.1f, 0.9f),
                reflectionEnabled = preset.reflection,
                mirrorEnabled = preset.mirror
            )
        }
    }

    fun renameVisualizerPreset(id: String, name: String) {
        viewModelScope.launch { settingsRepository.renameUserVisualizerPreset(id, name) }
    }

    fun deleteVisualizerPreset(id: String) {
        viewModelScope.launch { settingsRepository.deleteUserVisualizerPreset(id) }
    }

    fun resetVisualizer() {
        viewModelScope.launch { settingsRepository.resetVisualizerPrefs() }
    }

    // ═════════════════════════════════════════════════════════════════════
    // ⭐ COLOR INTELLIGENCE — Color Brain Output
    // ═════════════════════════════════════════════════════════════════════
    val colorState: StateFlow<ColorState> = colorIntelligenceEngine.colorState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ColorState.IDLE)

    // ═════════════════════════════════════════════════════════════════════
    // PLAYER STATE + FX — Core playback + audio effects
    // ═════════════════════════════════════════════════════════════════════
    // Granular player state — individual observable flows for fine-grained recomposition.
    val currentSong: StateFlow<Song?> = playerManager.playerState
        .map { it.currentSong }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** G-5: playback error surfaced for the NP error banner (+ retry). */
    val playbackError: StateFlow<String?> = playerManager.playerState
        .map { it.error }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isPlaying: StateFlow<Boolean> = playerManager.playerState
        .map { it.isPlaying }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val playbackPosition: StateFlow<Long> = playerManager.playerState
        .map { it.position }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val duration: StateFlow<Long> = playerManager.playerState
        .map { it.duration }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val bufferedPosition: StateFlow<Long> = playerManager.playerState
        .map { it.bufferedPosition }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val queue: StateFlow<List<QueueItem>> = playerManager.playerState
        .map { it.queue }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shuffleMode: StateFlow<ShuffleMode> = playerManager.playerState
        .map { it.shuffleMode }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShuffleMode.OFF)

    val repeatMode: StateFlow<RepeatMode> = playerManager.playerState
        .map { it.repeatMode }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RepeatMode.OFF)

    val playbackSpeed: StateFlow<Float> = playerManager.playerState
        .map { it.playbackSpeed }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val isLoading: StateFlow<Boolean> = playerManager.playerState
        .map { it.isLoading }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hasNext: StateFlow<Boolean> = playerManager.playerState
        .map { it.hasNext }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hasPrevious: StateFlow<Boolean> = playerManager.playerState
        .map { it.hasPrevious }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Queue index (kept ABOVE the monolithic combine — Kotlin initializers
    // run in declaration order and the combine references this flow).
    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    // Monolithic state — backward compatibility for screens not yet migrated.
    // NOTE: every PlayerState field must be fed here (error/isLoading and
    // currentQueueIndex were silently defaulted before — consumers reading
    // them got null/0/0 and "worked" by accident).
    @Suppress("UNCHECKED_CAST")
    val playerState: StateFlow<PlayerState> = combine(
        currentSong, isPlaying, playbackPosition, duration, bufferedPosition,
        queue, shuffleMode, repeatMode, playbackSpeed, isLoading, playbackError,
        currentQueueIndex
    ) { values ->
        PlayerState(
            currentSong = values[0] as Song?,
            isPlaying = values[1] as Boolean,
            position = values[2] as Long,
            duration = values[3] as Long,
            bufferedPosition = values[4] as Long,
            queue = values[5] as List<QueueItem>,
            shuffleMode = values[6] as ShuffleMode,
            repeatMode = values[7] as RepeatMode,
            playbackSpeed = values[8] as Float,
            isLoading = values[9] as Boolean,
            error = values[10] as String?,
            currentQueueIndex = values[11] as Int
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerState())

    private companion object {
        /** Play-history retention: entries older than 90 days are trimmed. */
        const val HISTORY_RETENTION_MS = 90L * 24 * 60 * 60 * 1000
    }
    
    init {
        viewModelScope.launch {
            combine(queue, currentSong) { q, song ->
                q.indexOfFirst { it.song?.id == song?.id }
            }.collect { index ->
                _currentQueueIndex.value = index.coerceAtLeast(0)
            }
        }
    }

    val audioFxState: StateFlow<AudioFxState> = audioFxManager.audioFxState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioFxState())

    // ── Offline waveform — decoded from song file, cached per song ─────────
    // This stays separate: offline envelope ≠ live audio (per architecture decision).
    val realWaveformState: StateFlow<com.rikodev.music.android.audiofx.WaveformState> =
        waveformRepository.state

    // ── Legacy dynamic colors — bridge for existing screens ──────────────
    // TODO: Migrate all screens to colorState (Step 3.11)
    @Deprecated("Use colorState instead — will be removed in Step 3.11")
    private val _dynamicColors = MutableStateFlow(AlbumArtColors())
    @Deprecated("Use colorState instead — will be removed in Step 3.11")
    val dynamicColors: StateFlow<AlbumArtColors> = _dynamicColors.asStateFlow()

    // ── Internal ────────────────────────────────────────────────────────
    private val _currentArtwork = MutableStateFlow<Bitmap?>(null)

    val isCurrentSongFavorite: StateFlow<Boolean> = playerState
        .map { it.currentSong?.id }
        .distinctUntilChanged()
        .flatMapLatest { songId ->
            if (songId == null) flowOf(false)
            else playlistRepository.isFavorite(songId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()

    // Full duration the running timer was set to — drives the countdown
    // ring's sweep fraction in the Sleep Timer sheet.
    private val _sleepTimerTotalMs = MutableStateFlow(0L)
    val sleepTimerTotalMs: StateFlow<Long> = _sleepTimerTotalMs.asStateFlow()

    // ═════════════════════════════════════════════════════════════════════
    // ⭐ BUILD-2: MINI PLAYER 2.0 — EXPERIENCE BRIDGE STATE
    // ═════════════════════════════════════════════════════════════════════
    //
    // The Mini Player consumes Experience OS through this bridge.
    // Components read ONLY miniPlayerState — never raw Experience OS.
    //
    // Two update mechanisms:
    //   1. Auto-sync: combine(PlayerState, ColorState, WaveState) → Mapper → State
    //   2. Events:   gestures/controls → Channel → Reducer → State
    //
    // Both write to the same _miniPlayerState. Auto-sync preserves
    // morph/gesture/interaction. Events update only local state.

    /** Internal mutable state for Mini Player. Updated by Mapper (auto-sync) and Reducer (events). */
    private val _miniPlayerState = MutableStateFlow(MiniPlayerStateFactory.coldStart())

    /**
     * ⭐ PUBLIC: Complete Mini Player 2.0 UI state.
     *
     * Every pixel Mini Player renders comes from this single StateFlow.
     * Components collect this and render. No other flows needed.
     *
     * Lifecycle: Started when first subscriber appears (WhileSubscribed 5000ms).
     * This means the state is active as long as any Mini Player composable
     * is in the composition tree, with a 5-second grace period.
     */
    val miniPlayerState: StateFlow<MiniPlayerState> = _miniPlayerState.asStateFlow()

    /**
     * Event channel for Mini Player gestures, controls, and system events.
     *
     * Uses Channel (not MutableStateFlow) because:
     *   - Events are transient (fire-and-forget, not persistent state)
     *   - Multiple rapid events should be processed sequentially
     *   - Events shouldn't be "latest wins" — every event matters
     *   - Channel guarantees FIFO ordering
     *
     * Buffer size: 64 — enough for rapid gesture sequences without
     * blocking the producer (gesture detector runs on main thread).
     */
    private val _miniPlayerEventChannel = Channel<MiniPlayerEvent>(capacity = 64)

    // PERF FIX (real device report — app lags and gets killed after ~20s):
    // WaveEngine's collection runs at real-time audio-frame rate (every PCM
    // buffer callback, likely 50-100+ times/second) with no throttling.
    // PlayerViewModel is created once at the top of RikoNavGraph and shared
    // across every screen — never cleared just by navigating away from Now
    // Playing — so this ran continuously for the whole app session,
    // including fully backgrounded. Same bug class as the VisualOsCoordinator
    // fix (RuntimeFix1), found in a second, even higher-frequency place.
    // WaveEngine.pause() alone doesn't help — it only gates the solver's
    // output, the underlying flow collection keeps running regardless. Real
    // fix: stop()/observe() (which does cancel the subscriptions), driven by
    // app lifecycle — see pauseWaveEngineForBackground() /
    // resumeWaveEngineForForeground(), wired from RikoNavGraph.kt's
    // ON_STOP/ON_START observer.
    private fun startWaveObservation() {
        viewModelScope.launch {
            waveEngine.observe(
                audioStateFlow = experienceDataHub.audioState,
                colorStateFlow = colorIntelligenceEngine.colorState
            )
        }
    }

    /** Called from RikoNavGraph's lifecycle observer on ON_STOP. */
    fun pauseWaveEngineForBackground() {
        waveEngine.stop()
        waveformRepository.loadWaveformFor(null)
    }

    /** Called from RikoNavGraph's lifecycle observer on ON_START. */
    fun resumeWaveEngineForForeground() {
        startWaveObservation()
    }

    init {
        // ── Connect Wave Engine to Audio Hub + Color Brain ────────────────
        // WaveEngine starts observing AudioState and ColorState flows.
        // It will process frames and publish WaveState automatically.
        startWaveObservation()

        // ── Feed artwork to Color Brain ───────────────────────────────────
        viewModelScope.launch {
            _currentArtwork.collectLatest { bitmap ->
                // Legacy path — keeps existing DynamicColors working for old screens
                _dynamicColors.value = colorExtractor.extractColors(bitmap)
                // New path — feeds the Color Intelligence Engine
                colorIntelligenceEngine.processArtwork(bitmap)
            }
        }

        // ── Load offline waveform for current song ────────────────────────
        viewModelScope.launch {
            playerState
                .map { it.currentSong }
                .distinctUntilChanged { old, new ->
                    old?.id == new?.id && old?.uri == new?.uri && old?.size == new?.size &&
                        old?.dateModified == new?.dateModified && old?.duration == new?.duration
                }
                .collectLatest { song ->
                    waveformRepository.loadWaveformFor(song)
                }
        }

        // ── Record play history for Home → Recently Played ───────────────
        // insertHistory existed in the DAO but had zero callers, so Recently
        // Played was permanently empty. A track counts after 5s of listening
        // (collectLatest cancels the wait on skip — skips don't count).
        viewModelScope.launch {
            playerState
                .map { it.currentSong }
                .distinctUntilChanged { old, new -> old?.id == new?.id }
                .collectLatest { song ->
                    if (song != null) {
                        delay(5_000)
                        withContext(Dispatchers.IO) {
                            try {
                                playHistoryDao.insertHistory(PlayHistoryEntity(songId = song.id))
                                playHistoryDao.clearOldHistory(
                                    System.currentTimeMillis() - HISTORY_RETENTION_MS
                                )
                            } catch (_: Exception) {
                                // History must never break playback.
                            }
                        }
                    }
                }
        }

        // ═════════════════════════════════════════════════════════════════
        // ⭐ BUILD-2: MINI PLAYER 2.0 — EXPERIENCE BRIDGE
        // ═════════════════════════════════════════════════════════════════

        // ── 1. Experience OS → MiniPlayerState (auto-sync) ────────────────
        // Combines PlayerState + ColorState + WaveState + isFavorite into
        // a single merged MiniPlayerState via MiniPlayerMapper.
        // Preserves local state (morph, gesture, interaction) across updates.
        viewModelScope.launch {
            combine(
                playerState,
                colorState,
                waveState,
                isCurrentSongFavorite
            ) { ps, cs, ws, fav ->
                MiniPlayerMapper.createSnapshot(
                    playerState = ps,
                    colorState = cs,
                    waveState = ws,
                    isFavorite = fav,
                    albumArtUri = null // Album art URI loaded separately
                )
            }.collectLatest { snapshot ->
                val (playback, color, wave) = snapshot.toSnapshots()
                _miniPlayerState.update { currentState ->
                    MiniPlayerMapper.mergeExperience(
                        playback = playback,
                        color = color,
                        wave = wave,
                        currentState = currentState
                    )
                }
            }
        }

        // ── 2. Mini Player Events → Reducer (event-driven) ──────────────
        // Events from gestures, controls, and system flow through a Channel
        // into the reducer. This is the ONLY way local state changes.
        viewModelScope.launch {
            _miniPlayerEventChannel.receiveAsFlow().collect { event ->
                _miniPlayerState.update { currentState ->
                    val newState = MiniPlayerReducer.reduce(currentState, event)

                    // Extract any actions the reducer produced
                    processMiniPlayerActions(newState, currentState)

                    newState
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Stop the Wave Engine when ViewModel is destroyed.
        // Solvers are @Singleton so their state persists across config changes.
        // But the flow observation in this scope should stop.
        waveEngine.stop()
        waveformRepository.loadWaveformFor(null)
        // Close Mini Player event channel.
        _miniPlayerEventChannel.close()
    }

    fun onArtworkLoaded(bitmap: Bitmap?) {
        _currentArtwork.value = bitmap
    }

    // ── Playback ──────────────────────────────────────────────────────────
    fun playQueue(songs: List<Song>, startIndex: Int) = playerManager.playQueue(songs, startIndex)
    fun togglePlayPause() = playerManager.togglePlayPause()
    fun skipToNext() = playerManager.skipToNext()
    fun skipToPrevious() = playerManager.skipToPrevious()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    fun seekToProgress(progress: Float) = playerManager.seekToProgress(progress)

    /** Main-thread only, sampled by the visible timeline at display vsync.
     * Read the existing ExoPlayer snapshot; never emit high-rate whole-screen state.
     */
    @androidx.annotation.MainThread
    fun timelinePositionMs(songId: Long): Long =
        if (playerManager.player.currentMediaItem?.mediaId == songId.toString())
            playerManager.player.currentPosition.coerceAtLeast(0L) else 0L
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun cycleRepeatMode() = playerManager.cycleRepeatMode()
    fun setPlaybackSpeed(speed: Float) = playerManager.setPlaybackSpeed(speed)

    fun toggleFavoriteCurrentSong() {
        val songId = playerState.value.currentSong?.id ?: return
        viewModelScope.launch { playlistRepository.toggleFavorite(songId) }
    }

    // ── Playlists (Add-to-Playlist sheet) ────────────────────────────────
    /** null = loading (no emission yet); emptyList = genuinely no playlists. */
    val playlists: StateFlow<List<Playlist>?> = playlistRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { playlistRepository.addSongToPlaylist(playlistId, songId) }
    }

    /** Create a playlist, add [songId] to it, then report the new id back. */
    fun createPlaylistWithSong(name: String, songId: Long, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = playlistRepository.createPlaylist(name)
            playlistRepository.addSongToPlaylist(id, songId)
            onCreated(id)
        }
    }

    // ── Retry (Plan 7 §45) ────────────────────────────────────────────────
    // Replay the current track from the start. Used by the error state.
    fun retry() {
        playerManager.clearError()
        playerManager.seekTo(0L)
        playerManager.togglePlayPause()
    }

    // ── Error Recovery (Plan v3.3 §32) ──────────────────────────────────────
    /** Clear the error state after the user acknowledges it or auto-recovery completes. */
    fun clearError() {
        playerManager.clearError()
    }

    // ── Queue ───────────────────────────────────────────────────────────
    fun addToQueue(song: Song) = playerManager.addToQueue(song)
    fun playNext(song: Song) = playerManager.playNext(song)
    fun moveQueueItem(from: Int, to: Int) = playerManager.reorderQueue(from, to)
    fun removeFromQueue(index: Int) = playerManager.removeFromQueue(index)
    /** Undo pair for [removeFromQueue] — restores the song at its position. */
    fun insertInQueue(index: Int, song: Song) =
        playerManager.insertInQueue(index, song)
    fun skipToQueueItem(index: Int) = playerManager.jumpToQueueIndex(index)

    // ═════════════════════════════════════════════════════════════════════
    // ⭐ NOW PLAYING UI STATE — Overlay visibility & interaction coordination
    // ═════════════════════════════════════════════════════════════════════

    /**
     * UI-only state for the Now Playing screen overlays.
     * This is NOT audio state or experience state — it's purely which panels
     * are visible and what the user is currently doing on the screen.
     *
     * Why a separate data class? Because overlay toggles fire rapidly
     * (tap queue → sheet opens → tap again → sheet closes) and bundling
     * them in a single flow avoids multiple independent recompositions.
     */
    data class NowPlayingUiState(
        val isQueueVisible: Boolean = false,
        val isLyricsVisible: Boolean = false,
        val isSeeking: Boolean = false
    )

    private val _nowPlayingUiState = MutableStateFlow(NowPlayingUiState())
    val nowPlayingUiState: StateFlow<NowPlayingUiState> = _nowPlayingUiState.asStateFlow()

    /** Open the queue bottom sheet overlay. */
    fun openQueue() {
        _nowPlayingUiState.update { it.copy(isQueueVisible = true) }
    }

    /** Close the queue bottom sheet overlay. */
    fun closeQueue() {
        _nowPlayingUiState.update { it.copy(isQueueVisible = false) }
    }

    /** Toggle queue visibility. */
    fun toggleQueue() {
        _nowPlayingUiState.update { it.copy(isQueueVisible = !it.isQueueVisible) }
    }

    /** Open the lyrics full-screen overlay. */
    fun openLyrics() {
        _nowPlayingUiState.update { it.copy(isLyricsVisible = true) }
    }

    /** Close the lyrics full-screen overlay. */
    fun closeLyrics() {
        _nowPlayingUiState.update { it.copy(isLyricsVisible = false) }
    }

    /** Toggle lyrics visibility. */
    fun toggleLyrics() {
        _nowPlayingUiState.update { it.copy(isLyricsVisible = !it.isLyricsVisible) }
    }

    /**
     * Close all Now Playing overlays (queue, lyrics).
     * Called on track change, dismiss gesture, or navigation away.
     */
    fun closeAllOverlays() {
        _nowPlayingUiState.update { it.copy(isQueueVisible = false, isLyricsVisible = false) }
    }

    // ── Sleep Timer ──────────────────────────────────────────────────────
    fun setSleepTimer(minutes: Int) {
        _sleepTimerTotalMs.value = minutes * 60_000L
        playerManager.setSleepTimer(minutes * 60_000L) { remaining ->
            _sleepTimerRemaining.value = remaining
        }
    }

    /**
     * Plan v3.3 §17: End of Queue timer — pauses when the last song finishes
     * (and repeat is OFF). No fixed duration — the queue itself is the timer.
     */
    fun setSleepEndOfQueue() {
        _sleepTimerTotalMs.value = 0L
        _sleepTimerRemaining.value = 0L // sentinel: timer is active but no fixed total
        playerManager.setSleepEndOfQueue()
    }

    fun cancelSleepTimer() {
        playerManager.cancelSleepTimer()
        _sleepTimerRemaining.value = null
        _sleepTimerTotalMs.value = 0L
    }

    // ═════════════════════════════════════════════════════════════════════
    // ⭐ BUILD-2: MINI PLAYER 2.0 — PUBLIC API
    // ═════════════════════════════════════════════════════════════════════
    //
    // Components call these methods. They submit events to the channel.
    // The reducer processes them and produces new state + actions.
    //
    // ⭐ RULE: Components NEVER call reducer directly.
    //   Components → submitMiniPlayerEvent() → Channel → Reducer → State
    //
    // ⭐ RULE: Components NEVER read raw Experience OS.
    //   Components → miniPlayerState (single source of truth)

    /**
     * Submit a Mini Player event for processing.
     *
     * Called by Mini Player composables for:
     *   - Gesture events (tap, drag, fling, long-press)
     *   - Control events (play/pause, skip, favorite pressed/released)
     *   - Navigation events (back press, route change)
     *   - System events (config change, screen off/on)
     *   - Animation callbacks (morph settled, beat detected)
     *
     * Events are processed FIFO by the reducer in viewModelScope.
     * This function returns immediately — non-blocking.
     *
     * @param event The event to process.
     */
    fun submitMiniPlayerEvent(event: MiniPlayerEvent) {
        _miniPlayerEventChannel.trySend(event)
    }

    /**
     * Submit multiple Mini Player events in batch.
     *
     * Useful for atomic state transitions that need multiple events
     * processed together (e.g., drag + direction lock in one frame).
     *
     * @param events Events to process in order.
     */
    fun submitMiniPlayerEvents(events: List<MiniPlayerEvent>) {
        events.forEach { _miniPlayerEventChannel.trySend(it) }
    }

    /**
     * Process output actions produced by the reducer.
     *
     * The reducer only produces STATE changes. But some events require
     * side effects (playback control, navigation, haptic feedback).
     * These side effects are detected by comparing old vs new state.
     *
     * Side effect detection rules:
     *   - Interaction hapticPending changes → fire haptic
     *   - Control press during idle → route to playback method
     *   - Morph reaching completion → navigate to Now Playing
     *   - Dismiss reaching completion → hide mini player
     *
     * ⭐ RULE: This is the ONLY place that has side effects for Mini Player.
     *   The reducer is pure. The mapper is pure. This is the bridge to impure.
     *
     * @param newState State after reducer processed the event.
     * @param oldState State before reducer processed the event.
     */
    private fun processMiniPlayerActions(
        newState: MiniPlayerState,
        oldState: MiniPlayerState
    ) {
        // ── Haptic Feedback ───────────────────────────────────────────
        val haptic = newState.interaction.hapticPending
        if (haptic != null && haptic != oldState.interaction.hapticPending) {
            // Haptic will be consumed by the composable via LaunchedEffect.
            // The reducer sets hapticPending, composable reads it, fires,
            // and then sends PressReleased to clear it.
        }

        // ── Playback Control (from button presses) ────────────────────
        // Detect which button was pressed and route to PlayerManager.
        val pressTarget = newState.interaction.pressTarget
        val wasJustPressed = newState.interaction.isPressed && !oldState.interaction.isPressed
        if (wasJustPressed) {
            when (pressTarget) {
                PressTarget.PLAY_PAUSE -> togglePlayPause()
                PressTarget.SKIP_NEXT -> skipToNext()
                PressTarget.SKIP_PREVIOUS -> skipToPrevious()
                PressTarget.FAVORITE -> toggleFavoriteCurrentSong()
                PressTarget.SURFACE -> {
                    // Surface press = expand to Now Playing (handled by morph state)
                }
                PressTarget.NONE -> { /* No action */ }
            }
        }

        // ── Morph Completion → Navigate to Now Playing ───────────────
        val morphJustCompleted =
            newState.morph.morphProgress >= 1f && oldState.morph.morphProgress < 1f
        if (morphJustCompleted) {
            // Navigation will be handled by the composable observing
            // morph.morphProgress reaching 1.0 (triggers NavHost transition).
            // The reducer already set isVisible = false.
        }

        // ── Dismiss Completion → Mini player hidden ──────────────────
        val dismissJustCompleted =
            !newState.isVisible && oldState.isVisible && oldState.morph.isDismissing
        if (dismissJustCompleted) {
            // Mini player state already set to invisible by reducer.
            // Composable will exit the composition tree.
        }
    }

    // ── Mini Player Lifecycle Helpers ──────────────────────────────────

    /**
     * Notify Mini Player that configuration changed (rotation, multi-window).
     * Recalculates layout but preserves morph state.
     */
    fun onMiniPlayerConfigChanged() {
        _miniPlayerState.update { MiniPlayerStateFactory.forConfigChange(it) }
    }

    /**
     * Notify Mini Player that screen turned off.
     */
    fun onMiniPlayerScreenOff() {
        _miniPlayerState.update { MiniPlayerStateFactory.forScreenOff(it) }
    }

    /**
     * Notify Mini Player that screen turned on.
     */
    fun onMiniPlayerScreenOn() {
        _miniPlayerState.update { MiniPlayerStateFactory.forScreenOn(it) }
    }

    /**
     * Notify Mini Player that route changed (e.g., navigated to Now Playing).
     */
    fun onMiniPlayerRouteChanged(isNowPlayingRoute: Boolean) {
        submitMiniPlayerEvent(MiniPlayerEvent.RouteChanged(isNowPlayingRoute))
    }

    /**
     * Force-reset Mini Player to collapsed state.
     * Used by Now Playing screen when user navigates back.
     */
    fun resetMiniPlayer() {
        submitMiniPlayerEvent(MiniPlayerEvent.ReturnedFromNowPlaying)
    }

    // ── EQ ───────────────────────────────────────────────────────────────
    fun setEqEnabled(enabled: Boolean) = audioFxManager.setEqEnabled(enabled)
    fun setBandLevel(bandIndex: Int, levelMillibel: Int) = audioFxManager.setBandLevel(bandIndex, levelMillibel)
    fun applyEqPreset(presetName: String) = audioFxManager.applyPreset(presetName)
    fun resetEq() = audioFxManager.resetEq()
    /** Plan 6.2: canonical frequency labels ("3.6K") — single source (engine). */
    fun eqFrequencyLabel(hz: Int): String = audioFxManager.getFrequencyLabel(hz)
    fun setVirtualizerEnabled(enabled: Boolean) = audioFxManager.setVirtualizerEnabled(enabled)
    fun setVirtualizerStrength(strength: Int) = audioFxManager.setVirtualizerStrength(strength)
    fun setBassBoostEnabled(enabled: Boolean) = audioFxManager.setBassBoostEnabled(enabled)
    fun setBassBoostStrength(strength: Int) = audioFxManager.setBassBoostStrength(strength)
    fun setReverbPreset(presetIndex: Int) = audioFxManager.setReverbPreset(presetIndex)
    fun setReverbEnabled(enabled: Boolean) = audioFxManager.setReverbEnabled(enabled)
    fun setLoudnessEnabled(enabled: Boolean) = audioFxManager.setLoudnessEnabled(enabled)
    fun setLoudnessGain(gainDb: Int) = audioFxManager.setLoudnessGain(gainDb)
    fun setBalanceEnabled(enabled: Boolean) = audioFxManager.setBalanceEnabled(enabled)
    fun setBalanceLevel(level: Int) = audioFxManager.setBalanceLevel(level)
    fun setPreampLevel(levelMb: Int) = audioFxManager.setPreampLevel(levelMb)
    // Flat Equalizer parity: bass-pro, loudness max gain, volume, EQ theme.
    fun setBassBoostFrequency(freq: Int) = audioFxManager.setBassBoostFrequency(freq)
    fun setBassBoostMaxGain(gain: Int) = audioFxManager.setBassBoostMaxGain(gain)
    fun setLoudnessMaxGain(gainDb: Int) = audioFxManager.setLoudnessMaxGain(gainDb)
    fun setVolumeLevel(percent: Int) = audioFxManager.setVolumeLevel(percent)
    fun setEqUiThemeMode(mode: String) = audioFxManager.setEqUiThemeMode(mode)

    val eqPresetNames: List<String>
        get() = audioFxState.value.equalizerState.presets
    val reverbPresetNames: List<String> get() = audioFxManager.reverbPresetNames
    val userPresets: StateFlow<List<EqPreset>> = audioFxManager.userPresets
    fun saveUserPreset(name: String): Boolean = audioFxManager.saveUserPreset(name)
    fun applyUserPreset(id: Long) = audioFxManager.applyUserPreset(id)
    fun deleteUserPreset(id: Long) = audioFxManager.deleteUserPreset(id)
    fun frequencyLabel(hz: Int): String = audioFxManager.getFrequencyLabel(hz)
}
