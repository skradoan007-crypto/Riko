/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.2 — Library Screen Runtime
 *
 * File 5 / 8 — LibraryScreenMapper.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryScreenMapper] maps from the Collection Runtime State (B2)
 * to the Library Screen State (B5.2). It is the bridge between the
 * *collection-level* runtime and the *screen-level* runtime.
 *
 * Data flow:
 *
 *   CollectionRuntimeState (B2) ─┐
 *   LibraryConstitution (B5.1) ──┤─► LibraryScreenMapper ─► LibraryScreenState
 *   PlaybackState (Audio) ────────┘
 *
 * The mapper is a *pure* function. It reads inputs, produces a
 * state. It has no side effects, makes no Engine calls, and never
 * mutates its inputs.
 *
 * RULE #55 — Screen Owns Composition.
 * The mapper is where composition happens. It decides which
 * collection becomes the Hero, which become sections, and in what
 * order. Components never make these decisions.
 *
 * Rule #57 — Every Screen Has One Hero.
 * The mapper picks exactly ONE collection as the Hero (or none).
 *
 * RULES ENFORCED HERE
 * -------------------
 * #44  Stable Key Never Changes    — keys preserved through mapping
 * #46  Runtime Never Creates Identity — mapper preserves, not invents
 * #55  Screen Owns Composition     — composition is here, not in components
 * #57  Every Screen Has One Hero   — at most one hero
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose, no Android framework, no Coroutine, no Flow, no
 * ViewModel. Pure Kotlin function.
 *
 * © RIKO Experience OS — Runtime Tier
 */

package com.rikodev.music.android.presentation.library.screen.runtime

import com.rikodev.music.android.presentation.library.contract.CollectionContract
import com.rikodev.music.android.presentation.library.contract.CollectionSectionContract
import com.rikodev.music.android.presentation.library.contract.CollectionType
import com.rikodev.music.android.presentation.library.contract.CollectionVisibility
import com.rikodev.music.android.presentation.library.runtime.CollectionRuntimePhase
import com.rikodev.music.android.presentation.library.runtime.CollectionRuntimeState
import com.rikodev.music.android.presentation.library.runtime.CollectionRuntimeSnapshot
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitution
import com.rikodev.music.android.presentation.library.screen.contract.LibraryDockState
import com.rikodev.music.android.presentation.library.screen.contract.LibraryEmptyStateKind
import com.rikodev.music.android.presentation.library.screen.contract.LibraryHeroProminence
import com.rikodev.music.android.presentation.library.screen.contract.LibraryScreenOrientation
import com.rikodev.music.android.presentation.library.screen.contract.LibrarySectionKind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.Animatable

/**
 * THE LIBRARY SCREEN MAPPER.
 *
 * Pure function. Maps collection runtime state + constitution +
 * playback state → Library screen state.
 */
object LibraryScreenMapper {

    /**
     * Map the collection runtime state into a Library screen state.
     *
     * @property constitution       The Library constitution (B5.1).
     * @property collectionRuntime  The collection runtime state (B2).
     * @property playbackState      Optional playback state (from Audio Runtime).
     * @property previousState      Previous screen state (for diff-aware mapping).
     * @property nowMs              Wall-clock time (epoch ms).
     * @return                      New Library screen state.
     */
    fun map(
        constitution: LibraryConstitution,
        collectionRuntime: CollectionRuntimeState,
        playbackState: PlaybackSnapshot?,
        previousState: LibraryScreenState?,
        nowMs: Long,
    ): LibraryScreenState {
        val phase = resolveScreenPhase(collectionRuntime, constitution)
        val topBar = mapTopBar(constitution, collectionRuntime, previousState)
        val dock = mapDock(constitution, playbackState, previousState)
        val hero = mapHero(constitution, collectionRuntime, previousState)
        val sections = mapSections(constitution, collectionRuntime, previousState)
        val selection = previousState?.selection ?: LibrarySelectionRuntimeState.INACTIVE
        val scroll = previousState?.scroll ?: LibraryScrollRuntimeState.INITIAL
        val navigation = previousState?.navigation ?: LibraryNavigationRuntimeState.IDLE
        val emptyState = mapEmptyState(collectionRuntime, phase, constitution)
        val errorMessage = if (phase == LibraryScreenPhase.ERROR)
            collectionRuntime.errorMessage else null

        return LibraryScreenState(
            phase = phase,
            profile = constitution.profile,
            orientation = LibraryScreenOrientation.PORTRAIT,
            topBar = topBar,
            dock = dock,
            hero = hero,
            sections = sections,
            selection = selection,
            scroll = scroll,
            navigation = navigation,
            emptyState = emptyState,
            lastUpdatedMs = nowMs,
            errorMessage = errorMessage,
            activeCollectionStableKey = previousState?.activeCollectionStableKey,
        )
    }

    // -----------------------------------------------------------------
    // PHASE RESOLUTION
    // -----------------------------------------------------------------

    private fun resolveScreenPhase(
        collectionRuntime: CollectionRuntimeState,
        constitution: LibraryConstitution,
    ): LibraryScreenPhase = when (collectionRuntime.phase) {
        CollectionRuntimePhase.COLD_START -> LibraryScreenPhase.COLD_START
        CollectionRuntimePhase.HYDRATING -> LibraryScreenPhase.LOADING
        CollectionRuntimePhase.PARTIAL_READY -> LibraryScreenPhase.PARTIAL_READY
        CollectionRuntimePhase.READY -> LibraryScreenPhase.READY
        CollectionRuntimePhase.ERROR -> LibraryScreenPhase.ERROR
        CollectionRuntimePhase.EMPTY -> LibraryScreenPhase.EMPTY
        CollectionRuntimePhase.PERMISSION_REQUIRED -> LibraryScreenPhase.PERMISSION_REQUIRED
    }

    // -----------------------------------------------------------------
    // TOP BAR MAPPING
    // -----------------------------------------------------------------

    private fun mapTopBar(
        constitution: LibraryConstitution,
        collectionRuntime: CollectionRuntimeState,
        previousState: LibraryScreenState?,
    ): LibraryTopBarRuntimeState {
        val prev = previousState?.topBar
        val title = constitution.topBar.title
        val headerContract = collectionRuntime.header
        val resolvedTitle = headerContract.greeting.displayText.ifEmpty { title.titleText }
        val resolvedSubtitle = headerContract.subtitle.displayText.ifEmpty { null }

        return LibraryTopBarRuntimeState(
            state = prev?.state ?: constitution.topBar.state,
            titleText = resolvedTitle,
            subtitleText = resolvedSubtitle,
            chipActionKeys = constitution.topBar.chips
                .filter { it.isVisible }
                .map { it.kind.name },
            badgeCounts = emptyMap(),
            scrollOffsetDp = prev?.scrollOffsetDp ?: 0,
        )
    }

    // -----------------------------------------------------------------
    // DOCK MAPPING
    // -----------------------------------------------------------------

    private fun mapDock(
        constitution: LibraryConstitution,
        playbackState: PlaybackSnapshot?,
        previousState: LibraryScreenState?,
    ): LibraryDockRuntimeState {
        val prev = previousState?.dock ?: LibraryDockRuntimeState.EMPTY
        if (playbackState == null) {
            return if (prev.hasTrack) prev.copy(state = LibraryDockState.COLLAPSED)
            else LibraryDockRuntimeState.EMPTY
        }

        return LibraryDockRuntimeState(
            state = if (prev.state == LibraryDockState.HIDDEN)
                LibraryDockState.COLLAPSED else prev.state,
            currentTrackId = playbackState.trackId,
            trackTitle = playbackState.title,
            trackArtist = playbackState.artist,
            isPlaying = playbackState.isPlaying,
            progressFraction = playbackState.progressFraction,
            artworkKey = playbackState.artworkKey,
            accentColorHex = playbackState.accentColorHex,
        )
    }

    // -----------------------------------------------------------------
    // HERO MAPPING — Rule #57 (One Hero)
    // -----------------------------------------------------------------

    private fun mapHero(
        constitution: LibraryConstitution,
        collectionRuntime: CollectionRuntimeState,
        previousState: LibraryScreenState?,
    ): LibraryHeroRuntimeState {
        val prev = previousState?.hero ?: LibraryHeroRuntimeState.EMPTY

        // Respect previous dismissal.
        if (prev.isDismissed) return LibraryHeroRuntimeState.EMPTY

        // Pick the first visible, non-empty smart collection as hero.
        val heroContract = pickHeroCollection(collectionRuntime) ?: return prev

        val snapshot = collectionRuntime.snapshotFor(heroContract.stableKey)
            ?: return prev.copy(isVisible = false)

        val firstCard = snapshot.cards.firstOrNull()
            ?: return prev.copy(isVisible = false)

        return LibraryHeroRuntimeState(
            isVisible = true,
            collectionStableKey = heroContract.stableKey,
            primaryItemId = firstCard.cardId,
            primaryTitle = firstCard.title,
            primarySubtitle = firstCard.subtitle,
            artworkKey = firstCard.cardId,
            accentColorHex = firstCard.accentColorHex ?: constitution.hero.overlaySpec.textColorHex,
            isDismissed = false,
        )
    }

    /**
     * Pick the collection that should become the Hero.
     *
     * Rule #57 — Every Screen Has One Hero.
     * Priority:
     *   1. CONTINUE_LISTENING (if visible and non-empty)
     *   2. RECENTLY_PLAYED
     *   3. First smart collection that is non-empty
     *   4. null (no hero)
     */
    private fun pickHeroCollection(
        runtime: CollectionRuntimeState,
    ): CollectionContract? {
        val priorityTypes = listOf(
            CollectionType.CONTINUE_LISTENING,
            CollectionType.RECENTLY_PLAYED,
            CollectionType.MOST_PLAYED,
            CollectionType.FAVORITES,
        )

        for (type in priorityTypes) {
            val contract = runtime.activeContracts.firstOrNull { it.type == type }
            if (contract != null) {
                val snapshot = runtime.snapshotFor(contract.stableKey)
                if (snapshot != null && snapshot.cards.isNotEmpty()) {
                    return contract
                }
            }
        }
        return null
    }

    // -----------------------------------------------------------------
    // SECTIONS MAPPING
    // -----------------------------------------------------------------

    private fun mapSections(
        constitution: LibraryConstitution,
        collectionRuntime: CollectionRuntimeState,
        previousState: LibraryScreenState?,
    ): List<LibrarySectionRuntimeState> {
        val prevByKey = previousState?.sections
            ?.associateBy { it.sectionId } ?: emptyMap()

        return collectionRuntime.sections.mapIndexed { index, sectionContract ->
            mapSection(sectionContract, collectionRuntime, prevByKey, index)
        }
    }

    private fun mapSection(
        sectionContract: CollectionSectionContract,
        collectionRuntime: CollectionRuntimeState,
        prevByKey: Map<String, LibrarySectionRuntimeState>,
        ordinal: Int,
    ): LibrarySectionRuntimeState {
        val stableKey = sectionContract.hostedCollectionIds.firstOrNull()
            ?: sectionContract.sectionId
        val snapshot = collectionRuntime.snapshotFor(stableKey)
        val prev = prevByKey[sectionContract.sectionId]

        return LibrarySectionRuntimeState(
            sectionId = sectionContract.sectionId,
            stableKey = stableKey,
            isVisible = snapshot?.isVisible ?: true,
            isCollapsed = prev?.isCollapsed ?: false,
            isRefreshing = snapshot?.isRefreshing ?: false,
            itemCount = snapshot?.visibleCardCount ?: 0,
            totalCount = snapshot?.totalAvailable ?: 0,
            firstVisibleItemIndex = prev?.firstVisibleItemIndex ?: 0,
            lastVisibleItemIndex = prev?.lastVisibleItemIndex ?: -1,
        )
    }

    // -----------------------------------------------------------------
    // EMPTY STATE MAPPING
    // -----------------------------------------------------------------

    private fun mapEmptyState(
        collectionRuntime: CollectionRuntimeState,
        screenPhase: LibraryScreenPhase,
        constitution: LibraryConstitution,
    ): LibraryEmptyRuntimeState {
        if (screenPhase != LibraryScreenPhase.EMPTY &&
            screenPhase != LibraryScreenPhase.PERMISSION_REQUIRED) {
            return LibraryEmptyRuntimeState.HIDDEN
        }

        val kind = when {
            collectionRuntime.phase == CollectionRuntimePhase.PERMISSION_REQUIRED ->
                LibraryEmptyStateKind.PERMISSION_DENIED
            collectionRuntime.activeContracts.isEmpty() ->
                LibraryEmptyStateKind.NO_COLLECTIONS
            collectionRuntime.snapshots.values.all { it.isEmpty } ->
                LibraryEmptyStateKind.NO_COLLECTIONS
            else ->
                LibraryEmptyStateKind.FIRST_LAUNCH
        }

        return LibraryEmptyRuntimeState(
            kind = kind,
            isVisible = true,
            replacesContent = true,
        )
    }
}

/**
 * Snapshot of playback state — passed into the mapper.
 *
 * The Audio Runtime produces this; the mapper consumes it. This
 * decouples the screen state from the Audio Runtime's internal
 * representation.
 *
 * @property trackId          Stable ID of the current track.
 * @property title            Track title.
 * @property artist           Track artist.
 * @property isPlaying        True if currently playing.
 * @property progressFraction 0.0–1.0. Playback progress.
 * @property artworkKey       Stable key for artwork loader.
 * @property accentColorHex   Per-track accent color (hex).
 */
data class PlaybackSnapshot(
    val trackId: String,
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val progressFraction: Float,
    val artworkKey: String,
    val accentColorHex: String,
) {
    init {
        require(progressFraction in 0f..1f) {
            "progressFraction must be in [0, 1], got $progressFraction"
        }
    }

    companion object {
        val SILENT = PlaybackSnapshot(
            trackId = "",
            title = "",
            artist = "",
            isPlaying = false,
            progressFraction = 0f,
            artworkKey = "",
            accentColorHex = "#FFFFFF",
        )
    }
}
