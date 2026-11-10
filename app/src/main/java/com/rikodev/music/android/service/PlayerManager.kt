package com.rikodev.music.android.service

import android.content.Context
import android.content.Intent
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.rikodev.music.android.audiofx.EqualizerEngine
import com.rikodev.music.android.audiofx.VisualizerManager
import com.rikodev.music.android.domain.model.PlayerState
import com.rikodev.music.android.domain.model.QueueItem
import com.rikodev.music.android.domain.model.RepeatMode
import com.rikodev.music.android.domain.model.ShuffleMode
import com.rikodev.music.android.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — PLAYER MANAGER
//
// The central brain of playback. Wraps ExoPlayer and exposes a clean,
// reactive PlayerState via StateFlow that the entire UI observes.
//
// Responsibilities:
//   - Queue management (play, next, previous, reorder, jump-to-index)
//   - Shuffle / Repeat modes
//   - Position tracking (ticks every 250ms while playing)
//   - AudioFx + Visualizer lifecycle (attach on STATE_READY)
//   - Sleep timer
//   - Error handling and recovery
//
// This is a Singleton — survives across the whole app and the foreground
// service, so playback never interrupts when navigating screens.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val player: ExoPlayer,
    private val audioFxManager: EqualizerEngine,
    private val visualizerManager: VisualizerManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionUpdateJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var errorSkipJob: Job? = null
    private var consecutiveErrors = 0

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // The master queue — original order, before shuffle is applied
    private var originalQueue: List<Song> = emptyList()
    // The active playback order — may be shuffled
    private var activeQueue: List<Song> = emptyList()

    // ─────────────────────────────────────────────────────────────────────
    // THE MISSING PIECE: nothing was ever starting MusicPlaybackService.
    // It was correctly declared (manifest, MediaSession, foregroundServiceType)
    // but never actually launched — Hilt injected this PlayerManager singleton
    // straight into the ViewModel, so ExoPlayer ran happily inside the plain
    // Activity process with NO foreground service promotion at all. That's
    // why there was never a system notification, and why playback died a
    // few minutes after backgrounding (a normal background process, not a
    // protected foreground one, is exactly what Android's battery/Doze
    // limits are designed to kill).
    //
    // Starting it here — right as real playback begins — is enough. Once
    // the service process exists and MediaSessionService sees the player
    // become active, its built-in DefaultMediaNotificationProvider takes
    // over and calls startForeground() with a real MediaStyle notification
    // automatically. No manual notification code needed.
    // ─────────────────────────────────────────────────────────────────────
    private fun ensureServiceStarted() {
        // CRASH FIX: startForegroundService() gives the service a hard
        // 5-second deadline to call startForeground(), or Android throws
        // ForegroundServiceDidNotStartInTimeException and kills the app —
        // exactly the "kicks out after ~7s" crash loop. That strict
        // deadline exists for services started from the BACKGROUND
        // (broadcast receivers, JobScheduler). We're always called from a
        // live UI action (user tapping play) — the app is already in the
        // foreground — so a plain startService() is correct here and has
        // no such deadline. Media3's MediaSessionService still promotes
        // itself to a real foreground service internally, the moment the
        // notification is actually ready.
        val intent = Intent(context, MusicPlaybackService::class.java)
        context.startService(intent)
    }

    init {
        setupPlayerListener()
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        // AudioFx attaches only after a valid audio session is ready.
                        val sessionId = player.audioSessionId
                        audioFxManager.initialize(sessionId)
                        // Permission-free policy: do not start android.media.audiofx.Visualizer.
                        // Existing PcmAudioProcessor -> SpectrumEngine remains the live source;
                        // WaveformRepository separately analyses the full local file.

                        _playerState.update {
                            it.copy(
                                duration = player.duration.coerceAtLeast(0L),
                                isLoading = false
                            )
                        }
                    }
                    Player.STATE_BUFFERING -> {
                        _playerState.update { it.copy(isLoading = true) }
                    }
                    Player.STATE_ENDED -> {
                        handlePlaybackEnded()
                    }
                    Player.STATE_IDLE -> {
                        _playerState.update { it.copy(isLoading = false) }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) consecutiveErrors = 0
                _playerState.update { it.copy(isPlaying = isPlaying) }
                visualizerManager.setEnabled(isPlaying)
                if (isPlaying) startPositionTicker() else stopPositionTicker()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                val song = activeQueue.getOrNull(index)
                _playerState.update {
                    it.copy(
                        currentSong = song,
                        currentQueueIndex = index,
                        position = 0L
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                consecutiveErrors++
                _playerState.update {
                    it.copy(
                        error = "Playback error: ${error.errorCodeName}",
                        isLoading = false
                    )
                }
                // Auto-skip to next track on error after a brief delay.
                // PERF P3 (RC-13): debounced — rapid error bursts queue only ONE skip.
                // Wave 8: circuit breaker — stop after 5 consecutive errors to avoid churn.
                if (consecutiveErrors >= 5) {
                    _playerState.update { it.copy(error = "Too many playback errors — stopped") }
                    return
                }
                errorSkipJob?.cancel()
                errorSkipJob = scope.launch {
                    delay(1000)
                    clearError()
                    // Wave 8 P0: must prepare() after STATE_IDLE from error, or playback stays dead.
                    skipToNext()
                    player.prepare()
                    player.playWhenReady = true
                }
            }
        })
    }

    // ── Position Ticker ───────────────────────────────────────────────────────
    // Updates position every 250ms while playing — drives the seek bar smoothly

    // CRASH FIX: ticker MUST stay on Main — ExoPlayer enforces application-thread
    // affinity (verifyApplicationThread) for getCurrentPosition/bufferedPosition.
    // Dispatchers.Default here crashes on play with IllegalStateException.
    private fun startPositionTicker() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (isActive) {
                _playerState.update {
                    it.copy(
                        position = player.currentPosition.coerceAtLeast(0L),
                        bufferedPosition = player.bufferedPosition.coerceAtLeast(0L)
                    )
                }
                delay(250)
            }
        }
    }

    private fun stopPositionTicker() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    // ── Queue Management ───────────────────────────────────────────────────────

    /**
     * Start playing a list of songs starting from a specific index.
     * This is the main entry point — called from All Songs, Playlist, Search.
     */
    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        ensureServiceStarted()

        originalQueue = songs
        activeQueue = if (_playerState.value.shuffleMode == ShuffleMode.ON) {
            shuffleWithCurrentFirst(songs, startIndex)
        } else {
            songs
        }

        val actualStartIndex = if (_playerState.value.shuffleMode == ShuffleMode.ON) 0 else startIndex

        val mediaItems = activeQueue.map { it.toMediaItem() }
        player.setMediaItems(mediaItems, actualStartIndex, 0L)
        player.prepare()
        player.playWhenReady = true

        updateQueueState()
    }

    /**
     * Add a song to play next (right after current).
     */
    fun playNext(song: Song) {
        val insertIndex = player.currentMediaItemIndex + 1
        player.addMediaItem(insertIndex, song.toMediaItem())

        val newQueue = activeQueue.toMutableList()
        newQueue.add(insertIndex.coerceAtMost(newQueue.size), song)
        activeQueue = newQueue
        updateQueueState()
    }

    /**
     * Add a song to the end of the queue.
     */
    fun addToQueue(song: Song) {
        player.addMediaItem(song.toMediaItem())
        activeQueue = activeQueue + song
        updateQueueState()
    }

    /**
     * Reorder the queue (user drag-and-drop reordering).
     */
    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in activeQueue.indices || toIndex !in activeQueue.indices) return
        player.moveMediaItem(fromIndex, toIndex)

        val newQueue = activeQueue.toMutableList()
        val item = newQueue.removeAt(fromIndex)
        newQueue.add(toIndex, item)
        activeQueue = newQueue
        updateQueueState()
    }

    /**
     * Remove a song from the queue.
     */
    fun removeFromQueue(index: Int) {
        if (index < 0 || index >= activeQueue.size) return
        player.removeMediaItem(index)
        activeQueue = activeQueue.toMutableList().apply { removeAt(index) }
        updateQueueState()
    }

    /**
     * Re-insert a song at a specific position — undo pair for [removeFromQueue].
     * Index is clamped: a stale undo target (queue shrank meanwhile) still lands
     * instead of silently dropping the restored song.
     */
    fun insertInQueue(index: Int, song: Song) {
        val at = index.coerceIn(0, activeQueue.size)
        player.addMediaItem(at, song.toMediaItem())
        activeQueue = activeQueue.toMutableList().apply { add(at, song) }
        updateQueueState()
    }

    /**
     * Jump directly to a specific song in the queue.
     */
    fun jumpToQueueIndex(index: Int) {
        if (index < 0 || index >= activeQueue.size) return
        player.seekTo(index, 0L)
        player.playWhenReady = true
    }

    private fun updateQueueState() {
        val queueItems = activeQueue.mapIndexed { index, song ->
            QueueItem(queueIndex = index + 1, song = song)
        }
        _playerState.update {
            it.copy(
                queue = queueItems,
                currentQueueIndex = player.currentMediaItemIndex
            )
        }
    }

    // ── Playback Controls ──────────────────────────────────────────────────────

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else { ensureServiceStarted(); player.play() }
    }

    fun play() {
        ensureServiceStarted()
        player.play()
    }
    fun pause() = player.pause()

    fun skipToNext() {
        // Wave 8 P1: manual next must advance past REPEAT_ONE — user intent is "next track",
        // not "restart current". Temporarily work around ExoPlayer's seekToNext being a no-op
        // under REPEAT_MODE_ONE.
        if (player.repeatMode == Player.REPEAT_MODE_ONE) {
            val nextIndex = player.nextMediaItemIndex
            if (nextIndex != C.INDEX_UNSET) {
                player.seekTo(nextIndex, 0L)
            } else {
                player.seekTo(0, 0L)
            }
        } else if (player.hasNextMediaItem()) {
            player.seekToNext()
        } else if (_playerState.value.repeatMode == RepeatMode.REPEAT_ALL) {
            player.seekTo(0, 0L)
        }
    }

    fun skipToPrevious() {
        // Standard behavior: if more than 3 seconds in, restart current song
        if (player.currentPosition > 3000) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        } else {
            player.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _playerState.update { it.copy(position = positionMs) }
    }

    fun seekToProgress(progress: Float) {
        // Wave 8 P2: guard against unknown/zero duration — seek to 0 instead of NaN.
        val duration = player.duration
        if (duration <= 0L) {
            seekTo(0L)
            return
        }
        val targetPosition = (duration * progress).toLong().coerceIn(0L, duration)
        seekTo(targetPosition)
    }

    // ── Shuffle / Repeat ───────────────────────────────────────────────────────

    fun toggleShuffle() {
        val newMode = if (_playerState.value.shuffleMode == ShuffleMode.OFF)
            ShuffleMode.ON else ShuffleMode.OFF

        // Wave 8 P1: originalQueue must reflect user queue edits (playNext/addToQueue/remove),
        // otherwise un-shuffling discards those edits.
        if (newMode == ShuffleMode.ON) {
            originalQueue = activeQueue.toList()
        }
        val currentIndex = player.currentMediaItemIndex.coerceAtLeast(0)
        val wasPlaying = player.playWhenReady

        activeQueue = if (newMode == ShuffleMode.ON) {
            shuffleWithCurrentFirst(activeQueue, currentIndex)
        } else {
            originalQueue.toList()
        }

        // Rebuild player queue keeping current song playing at current position
        val position = player.currentPosition
        player.setMediaItems(activeQueue.map { it.toMediaItem() })
        player.prepare()
        player.playWhenReady = wasPlaying

        // Seek to current song's new position
        val currentSong = _playerState.value.currentSong
        val newIndex = activeQueue.indexOfFirst { it.id == currentSong?.id }
        if (newIndex >= 0) {
            player.seekTo(newIndex, position)
        }

        _playerState.update { it.copy(shuffleMode = newMode) }
        updateQueueState()
    }

    fun cycleRepeatMode() {
        val newMode = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.REPEAT_ALL
            RepeatMode.REPEAT_ALL -> RepeatMode.REPEAT_ONE
            RepeatMode.REPEAT_ONE -> RepeatMode.OFF
        }

        player.repeatMode = when (newMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.REPEAT_ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
        }

        _playerState.update { it.copy(repeatMode = newMode) }
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _playerState.update { it.copy(playbackSpeed = speed) }
    }

    private fun handlePlaybackEnded() {
        when (_playerState.value.repeatMode) {
            RepeatMode.REPEAT_ONE -> {
                player.seekTo(0L)
                player.play()
            }
            RepeatMode.REPEAT_ALL -> {
                player.seekTo(0, 0L)
                player.play()
            }
            RepeatMode.OFF -> {
                if (player.hasNextMediaItem()) {
                    player.seekToNext()
                } else {
                    _playerState.update { it.copy(isPlaying = false) }
                }
            }
        }
    }

    private fun shuffleWithCurrentFirst(songs: List<Song>, currentIndex: Int): List<Song> {
        if (songs.isEmpty()) return songs
        val current = songs.getOrNull(currentIndex) ?: songs.first()
        val rest = songs.filterIndexed { i, _ -> i != currentIndex }.shuffled()
        return listOf(current) + rest
    }

    // ── Error Recovery ────────────────────────────────────────────────────────

    /**
     * Clear the current error state. Called after user acknowledges the error
     * or after automatic recovery (e.g., skip-to-next).
     * Plan v3.3 §32: "Detect → Explain → Recover → Continue."
     */
    fun clearError() {
        _playerState.update { it.copy(error = null) }
    }

    // ── Sleep Timer ───────────────────────────────────────────────────────────

    /**
     * Pause playback after [durationMs]. [onTick] fires every second with the
     * remaining time so the UI (Now Playing top bar) can show a live countdown.
     *
     * Uses System.currentTimeMillis as the authoritative time source (Plan v3.3
     * §19: "The system timer is authoritative. Animation is not the timer.")
     * to avoid coroutine-delay drift when the app is backgrounded or the device
     * is under load.
     */
    fun setSleepTimer(durationMs: Long, onTick: (Long) -> Unit = {}) {
        sleepTimerJob?.cancel()
        val endTime = System.currentTimeMillis() + durationMs
        sleepTimerJob = scope.launch {
            while (isActive) {
                val remaining = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
                onTick(remaining)
                if (remaining <= 0L) {
                    pause()
                    return@launch
                }
                delay(1000)
            }
        }
    }

    /**
     * Plan v3.3 §17: Timer intents include EndOfQueue — pause playback when
     * the current song is the last in the queue (and repeat is OFF).
     * Uses System.currentTimeMillis for authority, same as fixed-duration.
     */
    fun setSleepEndOfQueue() {
        sleepTimerJob?.cancel()
        sleepTimerJob = scope.launch {
            // Wave 8 P1: pause when the queue naturally ends (STATE_ENDED with repeat OFF).
            // Previous implementation polled UI state and false-fired on user pause.
            playerState.collect { state ->
                if (!state.isPlaying && !state.isLoading && state.error == null && state.currentSong != null) {
                    val atQueueEnd = player.currentMediaItemIndex >= player.mediaItemCount - 1
                    val repeatOff = state.repeatMode == RepeatMode.OFF
                    if (atQueueEnd && repeatOff && player.playbackState == Player.STATE_ENDED) {
                        pause()
                        return@collect
                    }
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }

    // ── User Kill Switch (P3) ────────────────────────────────────────────────

    /**
     * Full user-requested stop (Settings "Exit" row, notification stop).
     * Unlike [pause], after this the player is IDLE with nothing loaded:
     * tickers and sleep timer are cancelled, the queue is emptied and
     * state resets to defaults. MusicPlaybackService calls this right
     * before stopSelf(), so the foreground service and the player wind
     * down together and MediaSessionService has no playback left to
     * promote — the process settles without lingering audio.
     */
    fun stopForUser() {
        stopPositionTicker()
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        player.pause()
        player.stop()
        player.clearMediaItems()
        originalQueue = emptyList()
        activeQueue = emptyList()
        _playerState.value = PlayerState()
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun Song.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(
                        android.net.Uri.parse("content://media/external/audio/albumart/$albumId")
                    )
                    .build()
            )
            .build()
    }

    fun release() {
        stopPositionTicker()
        sleepTimerJob?.cancel()
        scope.cancel()
        audioFxManager.releaseAll()
        visualizerManager.release()
        player.release()
    }
}
