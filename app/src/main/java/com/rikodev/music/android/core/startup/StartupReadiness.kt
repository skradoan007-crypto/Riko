package com.rikodev.music.android.core.startup

import android.content.Context
import coil.ImageLoader
import coil.imageLoader
import com.rikodev.music.android.audiofx.WaveformRepository
import com.rikodev.music.android.core.designsystem.riko.components.ARTWORK_PX_SHELF
import com.rikodev.music.android.core.designsystem.riko.components.buildArtworkRequest
import com.rikodev.music.android.core.designsystem.riko.components.resolveArtworkData
import com.rikodev.music.android.data.db.dao.FavoriteDao
import com.rikodev.music.android.data.db.dao.PlaylistDao
import com.rikodev.music.android.data.repository.MusicRepository
import com.rikodev.music.android.data.repository.SettingsRepository
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Startup readiness (plan 3.2) — startup is split into two classes:
 *
 *  * CRITICAL: library snapshot, essential settings, player state,
 *    favorites + playlist essentials. Home is not "interactive-ready"
 *    until these have produced a first value.
 *  * BACKGROUND WARMUP: secondary artwork prefetch, waveform prep,
 *    aggregate/secondary cache work — runs only AFTER the app is already
 *    useful, so the preload system never becomes a startup bottleneck.
 *
 * Readiness is observed via cheap [StateFlow] reads only (plan 3.3: the
 * splash keep-on-screen check must never do IO). All IO lives in the
 * suspend/warmup functions here, off the main thread.
 */
@Singleton
class StartupReadiness @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository,
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao,
    private val waveformRepository: WaveformRepository
) {

    enum class CriticalStage { LIBRARY, SETTINGS, PLAYER, FAVORITES, PLAYLISTS }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val completedCritical = mutableSetOf<CriticalStage>()

    private val _criticalReady = MutableStateFlow(false)
    val criticalReady: StateFlow<Boolean> = _criticalReady.asStateFlow()

    /** 0..1 fraction of critical stages done — cheap splash progress source. */
    private val _criticalProgress = MutableStateFlow(0f)
    val criticalProgress: StateFlow<Float> = _criticalProgress.asStateFlow()

    private val _warmupDone = MutableStateFlow(false)
    val warmupDone: StateFlow<Boolean> = _warmupDone.asStateFlow()

    private val criticalStarted = AtomicBoolean(false)
    private val warmupStarted = AtomicBoolean(false)

    /**
     * Runs the CRITICAL stage set. Idempotent. Each stage completes when its
     * data source has produced a first value — the shared library cache (plan
     * 3.1) makes the library stage one cache read, not a fresh MediaStore scan.
     */
    suspend fun completeCriticalReadiness() {
        if (!criticalStarted.compareAndSet(false, true)) return
        withContext(Dispatchers.IO) {
            // FIX: timeout on first() — if MediaStore query fails or hangs
            // (e.g. permissions not granted), don't block splash forever.
            runCatching {
                kotlinx.coroutines.withTimeoutOrNull(5_000L) {
                    musicRepository.getAllSongs().first()
                }
            }
            mark(CriticalStage.LIBRARY)

            runCatching {
                kotlinx.coroutines.withTimeoutOrNull(3_000L) {
                    settingsRepository.settings.first()
                }
            }
            mark(CriticalStage.SETTINGS)

            // Player state is constructed synchronously by Hilt before any UI
            // exists (MainActivity injection → PlayerManager) — by the time
            // readiness runs, the player is live. Stage kept for completeness
            // of the critical contract.
            mark(CriticalStage.PLAYER)

            runCatching {
                kotlinx.coroutines.withTimeoutOrNull(3_000L) {
                    favoriteDao.getAllFavoriteSongIds().first()
                }
            }
            mark(CriticalStage.FAVORITES)

            runCatching {
                kotlinx.coroutines.withTimeoutOrNull(3_000L) {
                    playlistDao.getAllPlaylists().first()
                }
            }
            mark(CriticalStage.PLAYLISTS)
        }
    }

    /**
     * BACKGROUND WARMUP — bounded work after the app is already useful.
     * Idempotent; failures never block (best-effort cache warming only).
     */
    fun startBackgroundWarmup() {
        if (!warmupStarted.compareAndSet(false, true)) return
        scope.launch {
            val songs = runCatching {
                musicRepository.getAllSongs().first()
            }.getOrDefault(emptyList())

            // Secondary artwork: warm Coil's cache for the first shelf.
            // P0 FIX (cache poison): warmup used to enqueue the colliding
            // album-art URI under the UI's cache key — Coil then served that
            // WRONG bitmap to every screen. Now resolves through the same
            // embedded-art-first pipeline as the UI.
            // P0 FIX: enqueue without listener + activity destroyed =
            // IllegalStateException "You must collect the progress flow".
            // runCatching absorbs the crash; listener consumes the result.
            runCatching {
                val loader: ImageLoader = context.imageLoader
                songs.take(WARMUP_ARTWORK_COUNT).forEach { song ->
                    val data = resolveArtworkData(context, song.id, song.albumId, song.uri)
                        ?: return@forEach
                    runCatching {
                        loader.enqueue(
                            buildArtworkRequest(
                                context = context,
                                data = data,
                                songId = song.id,
                                albumId = song.albumId,
                                px = ARTWORK_PX_SHELF
                            ).newBuilder()
                                .listener(
                                    onSuccess = { _, _ -> },
                                    onError = { _, _ -> }
                                )
                                .build()
                        )
                    }
                }
            }

            // Waveform preparation for the first couple of songs.
            runCatching {
                songs.take(WARMUP_WAVEFORM_COUNT).forEach { waveformRepository.loadWaveformFor(it) }
            }

            _warmupDone.value = true
        }
    }

    private fun mark(stage: CriticalStage) {
        val total = CriticalStage.entries.size
        val done = synchronized(completedCritical) {
            completedCritical.add(stage)
            completedCritical.size
        }
        _criticalProgress.value = done.toFloat() / total.toFloat()
        if (done >= total) {
            _criticalReady.value = true
        }
    }

    private companion object {
        const val WARMUP_ARTWORK_COUNT = 10
        const val WARMUP_WAVEFORM_COUNT = 2
    }
}
