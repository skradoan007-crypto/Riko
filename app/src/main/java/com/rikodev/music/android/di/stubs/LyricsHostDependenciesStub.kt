package com.rikodev.music.android.di.stubs

import com.rikodev.music.android.presentation.lyrics.host.LyricsHostDependencies
import javax.inject.Singleton

@Singleton
class LyricsHostDependenciesStub @javax.inject.Inject constructor() : LyricsHostDependencies {
    override suspend fun getSongTitle(songId: Long): String? = null
    override suspend fun getArtistName(songId: Long): String? = null
    override suspend fun getAlbumName(songId: Long): String? = null
    override suspend fun getSongDurationMs(songId: Long): Long? = null
    override suspend fun seekPlayback(positionMs: Long) {}
    override fun openUrl(url: String) {}
    override fun shareText(text: String) {}
    override fun performHaptic() {}
    override suspend fun showSnackbar(message: String) {}
    override fun logEvent(event: String, params: Map<String, Any?>) {}
    override fun isOffline(): Boolean = false
}
