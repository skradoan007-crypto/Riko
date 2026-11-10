package com.rikodev.music.android.di.stubs

import com.rikodev.music.android.presentation.lyrics.source.LyricsFetcherCallbacks
import com.rikodev.music.android.presentation.lyrics.source.LyricsProvider
import com.rikodev.music.android.presentation.lyrics.contract.LyricsSet
import javax.inject.Singleton

@Singleton
class LyricsFetcherCallbacksStub @javax.inject.Inject constructor() : LyricsFetcherCallbacks {
    override suspend fun fetchLocalLrc(songId: Long): String? = null
    override suspend fun fetchEmbeddedLyrics(songId: Long): LyricsSet? = null
    override fun getProviders(): List<LyricsProvider> = emptyList()
}
