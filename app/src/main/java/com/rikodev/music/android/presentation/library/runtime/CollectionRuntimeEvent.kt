/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B2 — Collection Runtime
 *
 * File 2 / 4 — CollectionRuntimeEvent.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The complete set of events the Collection Runtime can receive.
 *
 * An Event describes WHAT HAPPENED — never WHAT TO DO.
 * The Reducer (File 4) decides what to do; this file only declares
 * the vocabulary of things that can happen.
 *
 * Events are organized by source:
 *   1. Lifecycle     — runtime started / paused / resumed
 *   2. Data          — engine produced / refreshed / failed
 *   3. Selection     — user entered / toggled / cleared multi-select
 *   4. Scroll        — user scrolled within a section
 *   5. User Action   — pin / collapse / refresh / see-all taps
 *   6. Engine        — engine signals (B3 will emit these)
 *   7. System        — permission / category / settings changes
 *
 * RULES ENFORCED HERE
 * -------------------
 * #36  Contract Never Thinks     — events carry only data, no logic
 * #44  Stable Key Never Changes  — all stableKey fields are strings
 *
 * Every event is a sealed class — exhaustive `when` guarantees the
 * reducer handles every case. No silent fallthrough.
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Repository, Room, DAO, Coroutine, Flow, StateFlow, Compose, Canvas,
 * ViewModel. Pure Kotlin sealed class — no Android framework, no async.
 *
 * © RIKO Experience OS — Runtime Tier
 */

package com.rikodev.music.android.presentation.library.runtime

/**
 * The complete vocabulary of events the Collection Runtime receives.
 *
 * Sealed class — the reducer MUST handle every subtype. Adding a
 * new event is a runtime-level decision (does NOT require Constitution
 * change), but the reducer MUST be updated in the same PR.
 */
sealed class CollectionRuntimeEvent {

    // ─────────────────────────────────────────────────────────────
    // 1. LIFECYCLE — runtime started / paused / resumed
    // ─────────────────────────────────────────────────────────────

    /** Runtime has been initialized and is ready to hydrate collections. */
    data object RuntimeStarted : CollectionRuntimeEvent()

    /** Runtime is pausing — typically when Library screen goes background. */
    data object RuntimePaused : CollectionRuntimeEvent()

    /** Runtime is resuming — typically when Library screen returns to foreground. */
    data object RuntimeResumed : CollectionRuntimeEvent()

    /** Runtime is being torn down — typically when user navigates away from Library. */
    data object RuntimeStopped : CollectionRuntimeEvent()

    // ─────────────────────────────────────────────────────────────
    // 2. DATA — engine produced / refreshed / failed
    // ─────────────────────────────────────────────────────────────

    /**
     * Engine produced the initial batch of collections.
     * @param snapshots Map of stableKey → CollectionRuntimeSnapshot.
     */
    data class CollectionsLoaded(
        val snapshots: Map<String, CollectionRuntimeSnapshot>,
    ) : CollectionRuntimeEvent()

    /**
     * Engine produced a refreshed snapshot for a single collection.
     * @param stableKey Rule #44 — stable identity of the collection.
     * @param snapshot The new snapshot.
     */
    data class CollectionRefreshed(
        val stableKey: String,
        val snapshot: CollectionRuntimeSnapshot,
    ) : CollectionRuntimeEvent()

    /**
     * Engine failed to produce or refresh a collection.
     * @param stableKey Rule #44 — stable identity of the failed collection.
     * @param errorMessage Human-readable error from the engine.
     */
    data class CollectionFailed(
        val stableKey: String,
        val errorMessage: String,
    ) : CollectionRuntimeEvent()

    /**
     * Engine produced zero results — library is empty.
     * Triggers transition to EMPTY phase.
     */
    data object LibraryIsEmpty : CollectionRuntimeEvent()

    /**
     * Engine produced an updated header contract.
     * E.g. time-of-day change requires header greeting change.
     */
    data class HeaderUpdated(
        val header: com.rikodev.music.android.presentation.library.contract.CollectionHeaderContract,
    ) : CollectionRuntimeEvent()

    // ─────────────────────────────────────────────────────────────
    // 3. SELECTION — multi-select mode
    // ─────────────────────────────────────────────────────────────

    /** User entered multi-select mode by long-pressing a card. */
    data class SelectionStarted(
        val initialCardId: String,
        val stableKey: String,
    ) : CollectionRuntimeEvent()

    /** User toggled a card's selection state. */
    data class SelectionToggled(
        val cardId: String,
        val stableKey: String,
    ) : CollectionRuntimeEvent()

    /** User selected all cards in a collection. */
    data class SelectAllInCollection(
        val stableKey: String,
    ) : CollectionRuntimeEvent()

    /** User cleared selection (tapped "Deselect All"). */
    data object SelectionCleared : CollectionRuntimeEvent()

    /** User exited multi-select mode. */
    data object SelectionExited : CollectionRuntimeEvent()

    // ─────────────────────────────────────────────────────────────
    // 4. SCROLL — user scrolled within a section
    // ─────────────────────────────────────────────────────────────

    /** User scrolled within a section. */
    data class SectionScrolled(
        val sectionId: String,
        val scrollOffsetDp: Int,
        val firstVisibleCardIndex: Int,
        val lastVisibleCardIndex: Int,
    ) : CollectionRuntimeEvent()

    /** Section reached the top — header unpins. */
    data class SectionReachedTop(
        val sectionId: String,
    ) : CollectionRuntimeEvent()

    /** Section reached the bottom — engine may load more cards. */
    data class SectionReachedBottom(
        val sectionId: String,
        val stableKey: String,
    ) : CollectionRuntimeEvent()

    /** User tapped the section header to collapse / expand. */
    data class SectionCollapseToggled(
        val sectionId: String,
        val stableKey: String,
    ) : CollectionRuntimeEvent()

    // ─────────────────────────────────────────────────────────────
    // 5. USER ACTION — pin / refresh / see-all / play
    // ─────────────────────────────────────────────────────────────

    /** User tapped a card. */
    data class CardTapped(
        val cardId: String,
        val stableKey: String,
    ) : CollectionRuntimeEvent()

    /** User long-pressed a card (shows context menu, NOT selection start). */
    data class CardLongPressed(
        val cardId: String,
        val stableKey: String,
    ) : CollectionRuntimeEvent()

    /** User tapped the play button on a collection header. */
    data class PlayCollectionTapped(
        val stableKey: String,
    ) : CollectionRuntimeEvent()

    /** User tapped the shuffle button on a collection header. */
    data class ShuffleCollectionTapped(
        val stableKey: String,
    ) : CollectionRuntimeEvent()

    /** User tapped the refresh button on a collection header. */
    data class RefreshCollectionTapped(
        val stableKey: String,
    ) : CollectionRuntimeEvent()

    /** User tapped the pin / unpin button. */
    data class PinToggled(
        val stableKey: String,
    ) : CollectionRuntimeEvent()

    /** User tapped "See All" on a section header. */
    data class SeeAllTapped(
        val stableKey: String,
        val sectionId: String,
    ) : CollectionRuntimeEvent()

    /** User tapped the search chip on the library header. */
    data object SearchChipTapped : CollectionRuntimeEvent()

    /** User tapped the shuffle-all chip on the library header. */
    data object ShuffleAllChipTapped : CollectionRuntimeEvent()

    /** User tapped the refresh chip on the library header. */
    data object RefreshAllChipTapped : CollectionRuntimeEvent()

    /** User pulled to refresh the entire library. */
    data object PullToRefreshTriggered : CollectionRuntimeEvent()

    // ─────────────────────────────────────────────────────────────
    // 6. ENGINE — signals from B3 engine (future)
    // ─────────────────────────────────────────────────────────────

    /**
     * Engine reports it has started regenerating a collection.
     * Updates refresh state to isRefreshing = true.
     */
    data class EngineStartedRefresh(
        val stableKey: String,
    ) : CollectionRuntimeEvent()

    /**
     * Engine reports it has finished regenerating a collection.
     * The result is in [CollectionRefreshed] — this event only
     * updates the refresh state metadata.
     */
    data class EngineFinishedRefresh(
        val stableKey: String,
        val success: Boolean,
        val durationMs: Long,
    ) : CollectionRuntimeEvent()

    /**
     * Engine updated the diagnostics for a collection.
     * Rule #42 — Diagnostics Is First-Class.
     */
    data class DiagnosticsUpdated(
        val stableKey: String,
        val diagnostics: com.rikodev.music.android.presentation.library.contract.CollectionDiagnosticsContract,
    ) : CollectionRuntimeEvent()

    /**
     * Engine updated the visibility of a collection (e.g. adaptive
     * visibility rule resolved to Hidden because insufficient items).
     */
    data class VisibilityUpdated(
        val stableKey: String,
        val visibility: com.rikodev.music.android.presentation.library.contract.CollectionVisibility,
    ) : CollectionRuntimeEvent()

    /**
     * Engine re-ordered the active contracts (e.g. priority reshuffle).
     */
    data class ActiveContractsReordered(
        val newOrder: List<String>,
    ) : CollectionRuntimeEvent()

    // ─────────────────────────────────────────────────────────────
    // 7. SYSTEM — permission / category / settings
    // ─────────────────────────────────────────────────────────────

    /** User granted media access permission. */
    data object PermissionGranted : CollectionRuntimeEvent()

    /** User denied media access permission. */
    data object PermissionDenied : CollectionRuntimeEvent()

    /** User disabled a category in settings. */
    data class CategoryDisabled(
        val categoryId: String,
    ) : CollectionRuntimeEvent()

    /** User enabled a category in settings. */
    data class CategoryEnabled(
        val categoryId: String,
    ) : CollectionRuntimeEvent()

    /** User changed sort order for a collection. */
    data class SortOrderChanged(
        val stableKey: String,
        val sortMode: String,
    ) : CollectionRuntimeEvent()

    /** System theme changed (light ↔ dark) — runtime may need to refresh glass strengths. */
    data object ThemeChanged : CollectionRuntimeEvent()

    /** User enabled developer mode — diagnostic collections become visible. */
    data object DeveloperModeEnabled : CollectionRuntimeEvent()

    /** User disabled developer mode — diagnostic collections hidden. */
    data object DeveloperModeDisabled : CollectionRuntimeEvent()

    /**
     * Engine reported a fatal error — runtime transitions to ERROR phase.
     * @param errorMessage Human-readable error message.
     */
    data class FatalError(
        val errorMessage: String,
    ) : CollectionRuntimeEvent()

    /** User dismissed the error state and requested retry. */
    data object ErrorRetryRequested : CollectionRuntimeEvent()
}
