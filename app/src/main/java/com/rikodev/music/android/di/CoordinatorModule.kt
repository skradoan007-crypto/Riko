package com.rikodev.music.android.di

import com.rikodev.music.android.di.stubs.*
import com.rikodev.music.android.presentation.library.engine.CollectionEngine
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitution
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenCoordinator
import com.rikodev.music.android.presentation.lyrics.host.LyricsHostCoordinator
import com.rikodev.music.android.presentation.lyrics.host.LyricsHostDependencies
import com.rikodev.music.android.presentation.lyrics.host.LyricsHostDependenciesReal
import com.rikodev.music.android.presentation.lyrics.host.LyricsFetcherCallbacksReal
import com.rikodev.music.android.presentation.lyrics.source.LyricsFetcherCallbacks
import com.rikodev.music.android.presentation.lyrics.source.LyricsFetcher
import com.rikodev.music.android.presentation.settings_v2.host.SettingsHostCoordinator
import com.rikodev.music.android.presentation.settings_v2.host.SettingsHostDependencies
import com.rikodev.music.android.presentation.settings_v2.host.SettingsHostDependenciesReal
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
// HOST-TIER SUBSYSTEMS BOUND TO REAL IMPLEMENTATIONS.
// Live here: E3 Lyrics, E4 Settings (the reachable E4 surface). Every Real
// class documents its own honest gaps in its own file header.
// D-9 retirement: search-v2, queue-v2, personalization (Phase 5 tail) and
// audio-v2 (Phase 10, after its a11y patterns were absorbed — D-13) are
// DELETED with their bindings and stubs, not kept as dead code.
// Remaining di/stubs/* files back LIVE stacks only.
// ═══════════════════════════════════════════════════════════════════════════

@Module
@InstallIn(SingletonComponent::class)
abstract class CoordinatorBindsModule {

    // E3 — real. Was: LyricsHostDependenciesStub.
    @Binds
    @Singleton
    abstract fun bindLyricsHostDependencies(impl: LyricsHostDependenciesReal): LyricsHostDependencies

    // E3 — real. Was: LyricsFetcherCallbacksStub.
    @Binds
    @Singleton
    abstract fun bindLyricsFetcherCallbacks(impl: LyricsFetcherCallbacksReal): LyricsFetcherCallbacks

    // E4 — real. Was: SettingsHostDependenciesStub.
    @Binds
    @Singleton
    abstract fun bindSettingsHostDependencies(impl: SettingsHostDependenciesReal): SettingsHostDependencies
}

@Module
@InstallIn(SingletonComponent::class)
object CoordinatorProvidesModule {

    @Provides
    @Singleton
    fun provideLibraryConstitution(): LibraryConstitution =
        LibraryConstitution.DEFAULT

    @Provides
    @Singleton
    fun provideCollectionEngine(): CollectionEngine = CollectionEngine()

    @Provides
    @Singleton
    fun provideLibraryScreenCoordinator(
        constitution: LibraryConstitution,
        engine: CollectionEngine,
    ): LibraryScreenCoordinator =
        LibraryScreenCoordinator(constitution, engine)

    @Provides
    @Singleton
    fun provideLyricsFetcher(callbacks: LyricsFetcherCallbacks): LyricsFetcher =
        LyricsFetcher(callbacks)

    @Provides
    @Singleton
    fun provideLyricsHostCoordinator(
        deps: LyricsHostDependencies,
        fetcher: LyricsFetcher,
    ): LyricsHostCoordinator =
        LyricsHostCoordinator(deps, fetcher)

    @Provides
    @Singleton
    fun provideSettingsHostCoordinator(
        deps: SettingsHostDependencies,
    ): SettingsHostCoordinator =
        SettingsHostCoordinator(deps)
}
