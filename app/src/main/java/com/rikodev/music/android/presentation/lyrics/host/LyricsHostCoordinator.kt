/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 5 — Lyrics Experience
 *
 * File 5 / 12 — LyricsHostCoordinator.kt (Rule #83 executor)
 * --------------------------------------------------------------------
 * Executes lyrics coordinator requests: fetch, seek, share, haptic.
 * © RIKO Experience OS — Lyrics Host Tier
 */

package com.rikodev.music.android.presentation.lyrics.host

import com.rikodev.music.android.presentation.lyrics.contract.*
import com.rikodev.music.android.presentation.lyrics.runtime.LyricsScreenCoordinator
import com.rikodev.music.android.presentation.lyrics.source.LyricsFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface LyricsHostDependencies {
    suspend fun getSongTitle(songId: Long): String?
    suspend fun getArtistName(songId: Long): String?
    suspend fun getAlbumName(songId: Long): String?
    suspend fun getSongDurationMs(songId: Long): Long?
    suspend fun seekPlayback(positionMs: Long)
    fun openUrl(url: String)
    fun shareText(text: String)
    fun performHaptic()
    suspend fun showSnackbar(message: String)
    fun logEvent(event: String, params: Map<String, Any?>)
    fun isOffline(): Boolean
}

class LyricsHostCoordinator(
    private val dependencies: LyricsHostDependencies,
    private val fetcher: LyricsFetcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _events = MutableSharedFlow<LyricsEvent>(extraBufferCapacity = 64)
    private var currentFetchJob: Job? = null

    val events: SharedFlow<LyricsEvent> = _events.asSharedFlow()

    fun execute(requests: List<LyricsCoordinatorRequest>) {
        requests.forEach { executeOne(it) }
    }

    private fun executeOne(request: LyricsCoordinatorRequest) {
        when (request) {
            is LyricsCoordinatorRequest.FetchLyrics -> fetchLyrics(request)
            is LyricsCoordinatorRequest.SeekPlayback -> seekPlayback(request)
            is LyricsCoordinatorRequest.EmitHaptic -> performHaptic()
            is LyricsCoordinatorRequest.ShowSnackbar -> showSnackbar(request)
            is LyricsCoordinatorRequest.OpenUrl -> openUrl(request)
            is LyricsCoordinatorRequest.ShareText -> shareText(request)
            is LyricsCoordinatorRequest.CancelFetch -> cancelFetch()
        }
    }

    private fun fetchLyrics(request: LyricsCoordinatorRequest.FetchLyrics) {
        currentFetchJob?.cancel()
        currentFetchJob = scope.launch {
            try {
                val title = withContext(Dispatchers.IO) { dependencies.getSongTitle(request.songId) }
                val artist = withContext(Dispatchers.IO) { dependencies.getArtistName(request.songId) }
                val album = withContext(Dispatchers.IO) { dependencies.getAlbumName(request.songId) }
                val duration = withContext(Dispatchers.IO) { dependencies.getSongDurationMs(request.songId) ?: request.durationMs }

                if (title == null || artist == null) {
                    _events.emit(LyricsEvent.NoLyricsFound(request.songId))
                    return@launch
                }

                val lyrics = withContext(Dispatchers.IO) {
                    fetcher.fetch(request.songId, title, artist, album, duration)
                }

                if (lyrics.isEmpty) {
                    _events.emit(LyricsEvent.NoLyricsFound(request.songId))
                    dependencies.logEvent("lyrics_not_found", mapOf("song_id" to request.songId))
                } else {
                    _events.emit(LyricsEvent.LyricsLoaded(request.songId, lyrics))
                    dependencies.logEvent("lyrics_loaded", mapOf(
                        "song_id" to request.songId,
                        "source" to lyrics.source.name,
                        "is_synced" to lyrics.isSynced,
                        "line_count" to lyrics.lineCount,
                    ))
                }
            } catch (e: Exception) {
                val isOffline = dependencies.isOffline()
                _events.emit(LyricsEvent.LyricsLoadFailed(
                    songId = request.songId,
                    error = e.message ?: "Unknown error",
                    isOffline = isOffline,
                ))
                dependencies.logEvent("lyrics_error", mapOf(
                    "song_id" to request.songId,
                    "error" to (e::class.simpleName ?: "Unknown"),
                    "is_offline" to isOffline,
                ))
            }
        }
    }

    private fun seekPlayback(request: LyricsCoordinatorRequest.SeekPlayback) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { dependencies.seekPlayback(request.positionMs) }
            } catch (e: Exception) {
                dependencies.logEvent("lyrics_seek_error", mapOf("error" to e::class.simpleName))
            }
        }
    }

    private fun performHaptic() {
        try { dependencies.performHaptic() } catch (_: Exception) {}
    }

    private fun showSnackbar(request: LyricsCoordinatorRequest.ShowSnackbar) {
        scope.launch {
            try { dependencies.showSnackbar(request.message) } catch (_: Exception) {}
        }
    }

    private fun openUrl(request: LyricsCoordinatorRequest.OpenUrl) {
        try { dependencies.openUrl(request.url) } catch (_: Exception) {}
    }

    private fun shareText(request: LyricsCoordinatorRequest.ShareText) {
        try { dependencies.shareText(request.text) } catch (_: Exception) {}
    }

    private fun cancelFetch() {
        currentFetchJob?.cancel()
        currentFetchJob = null
    }

    fun release() {
        cancelFetch()
        scope.cancel()
    }
}
