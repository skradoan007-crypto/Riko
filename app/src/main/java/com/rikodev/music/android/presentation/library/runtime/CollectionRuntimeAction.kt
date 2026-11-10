/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B2 — Collection Runtime
 *
 * File 3 / 4 — CollectionRuntimeAction.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The complete set of output actions the Collection Runtime can
 * emit to the outside world (ViewModel, Engine, Navigation, etc.).
 *
 * An Action describes WHAT TO DO — never HOW.
 * The receiver (ViewModel in B4 integration) decides how to execute.
 *
 * Events come IN to the reducer.
 * Actions go OUT of the reducer.
 *
 *   Event  →  Reducer  →  (new State, list of Actions)
 *
 * The reducer is pure — it cannot execute side effects. Instead it
 * emits Actions that the ViewModel interprets. This keeps the
 * reducer testable and side-effect-free.
 *
 * Action categories:
 *   1. Playback       — play / shuffle / queue
 *   2. Navigation     — navigate to detail screens
 *   3. Selection      — bulk selection operations
 *   4. Engine         — request refresh / regeneration
 *   5. System         — haptic / toast / permission request
 *   6. Diagnostic     — show inspector / debug overlay
 *
 * RULES ENFORCED HERE
 * -------------------
 * #36  Contract Never Thinks     — actions carry data, no logic
 * #44  Stable Key Never Changes  — all stableKey fields are strings
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
 * The type of haptic feedback the runtime can request.
 */
enum class CollectionHapticType {
    /** Light tap — e.g. card selection toggle. */
    LIGHT_TAP,

    /** Medium tap — e.g. button press. */
    MEDIUM_TAP,

    /** Heavy tap — e.g. destructive action. */
    HEAVY_TAP,

    /** Success confirmation — e.g. refresh complete. */
    SUCCESS,

    /** Warning — e.g. hit selection limit. */
    WARNING,

    /** Error — e.g. refresh failed. */
    ERROR,

    /** Rigid tick — e.g. scroll snap. */
    RIGID_TICK,

    /** Soft tick — e.g. subtle UI feedback. */
    SOFT_TICK,
}

/**
 * The destination for a navigation action.
 */
enum class CollectionNavigationDestination {
    /** Navigate to the full collection screen (e.g. "All Songs" detail). */
    COLLECTION_DETAIL,

    /** Navigate to the album detail screen. */
    ALBUM_DETAIL,

    /** Navigate to the artist detail screen. */
    ARTIST_DETAIL,

    /** Navigate to the folder browser. */
    FOLDER_DETAIL,

    /** Navigate to the playlist detail screen. */
    PLAYLIST_DETAIL,

    /** Navigate to the search screen. */
    SEARCH,

    /** Navigate to the now-playing screen. */
    NOW_PLAYING,

    /** Navigate to the settings screen. */
    SETTINGS,

    /** Navigate to the diagnostics / inspector screen (developer only). */
    DIAGNOSTICS,
}

/**
 * The type of toast message the runtime can request.
 */
enum class CollectionToastType {
    /** Informational — neutral tone. */
    INFO,

    /** Success — positive tone. */
    SUCCESS,

    /** Warning — cautionary tone. */
    WARNING,

    /** Error — negative tone. */
    ERROR,
}

/**
 * The complete set of output actions the Collection Runtime can emit.
 *
 * Sealed class — the ViewModel MUST handle every subtype. Adding a
 * new action is a runtime-level decision (does NOT require Constitution
 * change).
 */
sealed class CollectionRuntimeAction {

    // ─────────────────────────────────────────────────────────────
    // 1. PLAYBACK — play / shuffle / queue
    // ─────────────────────────────────────────────────────────────

    /** Play a single card (song). */
    data class PlayCard(
        val cardId: String,
        val stableKey: String,
    ) : CollectionRuntimeAction()

    /** Play an entire collection from the first card. */
    data class PlayCollection(
        val stableKey: String,
    ) : CollectionRuntimeAction()

    /** Shuffle a collection and start playback. */
    data class ShuffleCollection(
        val stableKey: String,
    ) : CollectionRuntimeAction()

    /** Play a card next (insert at top of queue). */
    data class PlayCardNext(
        val cardId: String,
        val stableKey: String,
    ) : CollectionRuntimeAction()

    /** Add a card to the queue (append at bottom). */
    data class AddCardToQueue(
        val cardId: String,
        val stableKey: String,
    ) : CollectionRuntimeAction()

    /** Add multiple cards to the queue (batch). */
    data class AddCardsToQueue(
        val cardIds: List<String>,
        val stableKey: String,
    ) : CollectionRuntimeAction()

    /** Add selected cards to a playlist (opens playlist picker). */
    data class AddSelectedToPlaylist(
        val selectedCardIds: List<String>,
    ) : CollectionRuntimeAction()

    /** Shuffle the entire library. */
    data object ShuffleAll : CollectionRuntimeAction()

    // ─────────────────────────────────────────────────────────────
    // 2. NAVIGATION
    // ─────────────────────────────────────────────────────────────

    /** Navigate to a destination, optionally with a stable key payload. */
    data class NavigateTo(
        val destination: CollectionNavigationDestination,
        val stableKey: String?,
    ) : CollectionRuntimeAction()

    /** Navigate to "See All" view for a collection. */
    data class NavigateToSeeAll(
        val stableKey: String,
        val sectionId: String,
    ) : CollectionRuntimeAction()

    /** Navigate back from current screen. */
    data object NavigateBack : CollectionRuntimeAction()

    // ─────────────────────────────────────────────────────────────
    // 3. SELECTION — bulk operations
    // ─────────────────────────────────────────────────────────────

    /** Add selected cards to queue. */
    data class AddSelectedToQueue(
        val selectedCardIds: List<String>,
    ) : CollectionRuntimeAction()

    /** Remove selected cards from library (with confirmation). */
    data class DeleteSelected(
        val selectedCardIds: List<String>,
    ) : CollectionRuntimeAction()

    /** Share selected cards (system share sheet). */
    data class ShareSelected(
        val selectedCardIds: List<String>,
    ) : CollectionRuntimeAction()

    /** Mark selected cards as favorites. */
    data class FavoriteSelected(
        val selectedCardIds: List<String>,
    ) : CollectionRuntimeAction()

    /** Unmark selected cards as favorites. */
    data class UnfavoriteSelected(
        val selectedCardIds: List<String>,
    ) : CollectionRuntimeAction()

    // ─────────────────────────────────────────────────────────────
    // 4. ENGINE — request refresh / regeneration
    // ─────────────────────────────────────────────────────────────

    /** Request engine to refresh a single collection. */
    data class RequestRefresh(
        val stableKey: String,
    ) : CollectionRuntimeAction()

    /** Request engine to refresh all visible collections. */
    data object RequestRefreshAll : CollectionRuntimeAction()

    /** Request engine to pin / unpin a collection. */
    data class RequestPinToggle(
        val stableKey: String,
        val pin: Boolean,
    ) : CollectionRuntimeAction()

    /** Request engine to reorder active contracts. */
    data class RequestReorder(
        val newOrder: List<String>,
    ) : CollectionRuntimeAction()

    /** Request engine to apply a new sort mode to a collection. */
    data class RequestSortChange(
        val stableKey: String,
        val sortMode: String,
    ) : CollectionRuntimeAction()

    /** Request engine to retry after a fatal error. */
    data object RequestErrorRetry : CollectionRuntimeAction()

    // ─────────────────────────────────────────────────────────────
    // 5. SYSTEM — haptic / toast / permission
    // ─────────────────────────────────────────────────────────────

    /** Fire a haptic feedback. */
    data class HapticFeedback(
        val type: CollectionHapticType,
    ) : CollectionRuntimeAction()

    /** Show a toast message. */
    data class ShowToast(
        val message: String,
        val type: CollectionToastType,
    ) : CollectionRuntimeAction()

    /** Request media access permission. */
    data object RequestPermission : CollectionRuntimeAction()

    /** Open system settings (for category toggles, etc.). */
    data object OpenSystemSettings : CollectionRuntimeAction()

    /** Trigger a system share sheet for a single card. */
    data class ShareCard(
        val cardId: String,
        val stableKey: String,
    ) : CollectionRuntimeAction()

    /** Show the context menu for a card. */
    data class ShowContextMenu(
        val cardId: String,
        val stableKey: String,
    ) : CollectionRuntimeAction()

    // ─────────────────────────────────────────────────────────────
    // 6. DIAGNOSTIC — inspector / debug overlay
    // ─────────────────────────────────────────────────────────────

    /** Show the diagnostics inspector for a collection (developer only). */
    data class ShowDiagnostics(
        val stableKey: String,
    ) : CollectionRuntimeAction()

    /** Show the diagnostic overlay (developer only). */
    data object ShowDiagnosticOverlay : CollectionRuntimeAction()

    /** Hide the diagnostic overlay. */
    data object HideDiagnosticOverlay : CollectionRuntimeAction()

    /** Force the engine to regenerate a collection (developer only). */
    data class ForceRegenerate(
        val stableKey: String,
    ) : CollectionRuntimeAction()

    /** Toggle a developer flag for a collection (developer only). */
    data class ToggleDeveloperFlag(
        val stableKey: String,
        val flagName: String,
    ) : CollectionRuntimeAction()

    /** Log a diagnostic event to the inspector. */
    data class LogDiagnostic(
        val tag: String,
        val message: String,
    ) : CollectionRuntimeAction()
}
