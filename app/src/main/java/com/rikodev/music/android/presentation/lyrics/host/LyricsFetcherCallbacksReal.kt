/*
 * RIKO EXPERIENCE OS — HOST INTEGRATION TIER (B5.5)
 * Lyrics Engine — E3
 *
 * LyricsFetcherCallbacksReal.kt — replaces LyricsFetcherCallbacksStub.
 *
 * fetchLocalLrc / fetchEmbeddedLyrics are genuinely real I/O.
 * getProviders() returning emptyList() is NOT a stub gap — see the
 * comment on that method.
 *
 * © RIKO Experience OS — Lyrics Host Tier
 */

package com.rikodev.music.android.presentation.lyrics.host

import android.util.Log
import com.rikodev.music.android.data.repository.MusicRepository
import com.rikodev.music.android.presentation.lyrics.contract.LyricsSet
import com.rikodev.music.android.presentation.lyrics.source.Id3LyricsExtractor
import com.rikodev.music.android.presentation.lyrics.source.LyricsFetcherCallbacks
import com.rikodev.music.android.presentation.lyrics.source.LyricsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RikoLyricsFetch"

@Singleton
class LyricsFetcherCallbacksReal @Inject constructor(
    private val musicRepository: MusicRepository,
) : LyricsFetcherCallbacks {

    override suspend fun fetchLocalLrc(songId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val song = musicRepository.getSongById(songId) ?: return@withContext null
            val audioFile = File(song.path)
            val lrcFile = File(audioFile.parentFile, audioFile.nameWithoutExtension + ".lrc")
            if (lrcFile.exists() && lrcFile.canRead()) lrcFile.readText() else null
        } catch (e: Exception) {
            // Real, honest failure mode, not a crash: modern Android's scoped
            // storage can block raw File() access to a sibling file that
            // isn't itself MediaStore-indexed (a .lrc next to an .mp3 usually
            // isn't), depending on OS version. LyricsFetcher's own fallback
            // chain (local → embedded → providers → empty) already treats a
            // null here as "try the next source," not an error.
            Log.w(TAG, "fetchLocalLrc($songId) failed: ${e.message}")
            null
        }
    }

    override suspend fun fetchEmbeddedLyrics(songId: Long): LyricsSet? = withContext(Dispatchers.IO) {
        try {
            val song = musicRepository.getSongById(songId) ?: return@withContext null
            val (language, text) = Id3UsltReader.readUslt(File(song.path)) ?: return@withContext null
            Id3LyricsExtractor.fromUslt(songId, language, text)
        } catch (e: Exception) {
            Log.w(TAG, "fetchEmbeddedLyrics($songId) failed: ${e.message}")
            null
        }
    }

    /**
     * Intentionally empty — NOT a stub gap. RIKO is offline-first by
     * explicit design (no login, no ads, no cloud dependency, no internet
     * required). A remote lyrics provider (LRCLIB/Musixmatch/etc.) would
     * be the first network dependency anywhere in the lyrics pipeline.
     * LyricsFetcher already treats an empty provider list as a normal,
     * handled case — it falls through to LyricsSet.EMPTY, which the UI
     * renders as a clean "no lyrics found" state, not an error. If a
     * provider is ever wanted, it plugs in here without touching
     * anything else in the pipeline (LyricsProvider is already a clean
     * interface for exactly this).
     */
    override fun getProviders(): List<LyricsProvider> = emptyList()
}
