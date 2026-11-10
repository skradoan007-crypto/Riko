/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B2 — Collection Runtime
 *
 * File 1 / 4 — CollectionRuntimeState.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Collection Runtime holds the *live* state of every collection
 * currently surfaced in the Library. It is the bridge between the
 * immutable Constitution (B1) and the Library's overall UI state.
 *
 * Data flow:
 *
 *   CollectionConstitution  (B1 — frozen, immutable)
 *             │
 *             ▼
 *   CollectionRuntimeState  (B2 — this file — live, mutable)
 *             │
 *             ▼
 *   LibraryState            (Phase A — composes runtime into UI state)
 *             │
 *             ▼
 *   UI (B4)                  — renders LibraryState
 *
 * RULES ENFORCED HERE
 * -------------------
 * #26  Collection First          — runtime is collection-centric, not song-centric
 * #31  Runtime Owns Truth        — UI reads runtime state, never the engine
 * #32  Every Card Owns One Snapshot — each card has exactly one snapshot
 * #36  Contract Never Thinks     — runtime does not invent contracts; it consumes them
 * #44  Stable Key Never Changes  — all map keys are stableKey strings
 * #45  Contracts Must Be Serializable — runtime state is also serializable-shaped
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Repository, Room, DAO, Coroutine, Flow, StateFlow, Compose, Canvas,
 * ViewModel. This is pure Kotlin data — no Android framework, no async.
 *
 * © RIKO Experience OS — Runtime Tier
 */

package com.rikodev.music.android.presentation.library.runtime

import com.rikodev.music.android.presentation.library.contract.CollectionContract
import com.rikodev.music.android.presentation.library.contract.CollectionDiagnosticsContract
import com.rikodev.music.android.presentation.library.contract.CollectionHeaderContract
import com.rikodev.music.android.presentation.library.contract.CollectionSectionContract
import com.rikodev.music.android.presentation.library.contract.CollectionVisibility
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.Canvas

/**
 * The phase the Collection Runtime is currently in.
 *
 * Mirrors [com.rikodev.music.android.presentation.library.LibraryPhase]
 * but is collection-specific.
 */
enum class CollectionRuntimePhase {
    /** Cold start — constitution loaded, no collections hydrated yet. */
    COLD_START,

    /** Engine is producing the first batch of collections. */
    HYDRATING,

    /** At least one collection is ready; others may still be loading. */
    PARTIAL_READY,

    /** All visible collections are ready. */
    READY,

    /** Engine reported a fatal error — showing error state. */
    ERROR,

    /** Library is empty — no songs to generate collections from. */
    EMPTY,

    /** Permission not granted — collections cannot be generated. */
    PERMISSION_REQUIRED,
}

/**
 * A single card's runtime snapshot.
 *
 * RULE #32 — Every Card Owns One Snapshot.
 * Each card on screen has exactly one [CardSnapshot]. The card's
 * identity is [cardId]; the card's data is [payload].
 *
 * The payload is intentionally opaque (Any?) so the runtime can
 * host song cards, album cards, artist cards, playlist cards, etc.
 * without coupling to a specific snapshot type. The UI (B4) is
 * responsible for interpreting the payload per the section's
 * [CollectionSectionContract].
 *
 * RULE #44 — [cardId] is a stable string. It must be derived from
 * the underlying entity's stable ID (e.g. songId, albumId). It
 * must NEVER be a list index or a transient pointer.
 */
data class CardSnapshot(
    /** Stable identity — never an index, never null. */
    val cardId: String,
    /** Display title — what the card shows as its primary text. */
    val title: String,
    /** Optional subtitle — secondary text. */
    val subtitle: String,
    /** Optional accent color override — hex string like "#FF8A65". */
    val accentColorHex: String?,
    /** Opaque payload — interpreted by the renderer per section contract. */
    val payload: Any?,
    /** Whether this card is currently selected (multi-select mode). */
    val isSelected: Boolean,
    /** Whether this card is currently playing. */
    val isPlaying: Boolean,
    /** Whether this card is currently the active item in its collection. */
    val isActive: Boolean,
)

/**
 * The runtime snapshot of a single collection.
 *
 * Holds the cards the engine produced for this collection, plus
 * the live visibility state (which may differ from the contract's
 * default — e.g. user collapsed it).
 */
data class CollectionRuntimeSnapshot(
    /** Stable key — matches [CollectionContract.stableKey]. Rule #44. */
    val stableKey: String,
    /** Cards the engine produced, in display order. */
    val cards: List<CardSnapshot>,
    /** Live visibility — may differ from contract default. */
    val visibility: CollectionVisibility,
    /** Whether the engine is currently refreshing this collection. */
    val isRefreshing: Boolean,
    /** Wall-clock time (epoch ms) this snapshot was last updated. */
    val lastUpdated: Long,
    /** Total items the engine considered (may be > cards.size if capped). */
    val totalAvailable: Int,
    /** Diagnostics snapshot for this collection. Rule #42. */
    val diagnostics: CollectionDiagnosticsContract,
) {
    /** True if this collection has zero cards. */
    val isEmpty: Boolean get() = cards.isEmpty()

    /** True if this collection is currently visible to the user. */
    val isVisible: Boolean get() = visibility.rendersCards

    /** Number of cards currently rendered (post-cap, post-collapse). */
    val visibleCardCount: Int
        get() = when (visibility) {
            is CollectionVisibility.Collapsed -> 0
            is CollectionVisibility.Expanded -> visibility.visibleCardCount
            else -> cards.size
        }
}

/**
 * Multi-select state for collections.
 */
data class CollectionSelectionState(
    /** True if selection mode is currently active. */
    val isActive: Boolean,
    /** Set of selected cardIds — stable strings per Rule #44. */
    val selectedCardIds: Set<String>,
    /** Maximum cards the user can select (from CollectionConstraints). */
    val selectionLimit: Int,
) {
    companion object {
        val INACTIVE = CollectionSelectionState(
            isActive = false,
            selectedCardIds = emptySet(),
            selectionLimit = 50,
        )
    }

    val selectedCount: Int get() = selectedCardIds.size
    val isAtLimit: Boolean get() = selectedCardIds.size >= selectionLimit
}

/**
 * Per-section scroll state.
 */
data class CollectionScrollState(
    /** Section ID — matches [CollectionSectionContract.sectionId]. */
    val sectionId: String,
    /** Current scroll offset in dp. */
    val scrollOffsetDp: Int,
    /** Whether the section is currently pinned (sticky header active). */
    val isPinned: Boolean,
    /** Whether the user has scrolled past the first card. */
    val hasScrolled: Boolean,
    /** First visible card index, or -1 if section is empty. */
    val firstVisibleCardIndex: Int,
    /** Last visible card index, or -1 if section is empty. */
    val lastVisibleCardIndex: Int,
)

/**
 * Per-collection refresh state.
 */
data class CollectionRefreshState(
    /** Stable key — matches [CollectionContract.stableKey]. Rule #44. */
    val stableKey: String,
    /** True if a refresh is currently in progress. */
    val isRefreshing: Boolean,
    /** Wall-clock time (epoch ms) of the last successful refresh. */
    val lastRefreshedAt: Long,
    /** Wall-clock time (epoch ms) of the last failed refresh, or 0. */
    val lastFailedAt: Long,
    /** Number of consecutive failures since last success. */
    val consecutiveFailures: Int,
    /** Optional error message from the last failure. */
    val lastErrorMessage: String?,
) {
    val isStale: Boolean get() = lastRefreshedAt == 0L
    val isFailing: Boolean get() = consecutiveFailures > 0
}

/**
 * The complete Collection Runtime State.
 *
 * This is the single source of truth for "what collections are
 * currently on screen and what state are they in". The UI (B4)
 * reads this — never the engine, never the constitution directly.
 *
 * RULE #31 — Runtime Owns Truth.
 */
data class CollectionRuntimeState(
    /** Current phase of the runtime. */
    val phase: CollectionRuntimePhase,
    /** Contracts currently active (filtered from Constitution by context). */
    val activeContracts: List<CollectionContract>,
    /** Sections in display order. */
    val sections: List<CollectionSectionContract>,
    /** Current Library header contract. */
    val header: CollectionHeaderContract,
    /** Per-collection snapshots, keyed by stableKey. Rule #44. */
    val snapshots: Map<String, CollectionRuntimeSnapshot>,
    /** Multi-select state. */
    val selection: CollectionSelectionState,
    /** Per-section scroll state, keyed by sectionId. */
    val scrollStates: Map<String, CollectionScrollState>,
    /** Per-collection refresh state, keyed by stableKey. */
    val refreshStates: Map<String, CollectionRefreshState>,
    /** Wall-clock time (epoch ms) the runtime was last updated. */
    val lastUpdated: Long,
    /** Optional error message if phase == ERROR. */
    val errorMessage: String?,
) {
    companion object {

        /** Cold-start empty state — no collections, no sections, no header. */
        val COLD_START = CollectionRuntimeState(
            phase = CollectionRuntimePhase.COLD_START,
            activeContracts = emptyList(),
            sections = emptyList(),
            header = CollectionHeaderContract.DEFAULT,
            snapshots = emptyMap(),
            selection = CollectionSelectionState.INACTIVE,
            scrollStates = emptyMap(),
            refreshStates = emptyMap(),
            lastUpdated = 0L,
            errorMessage = null,
        )
    }

    /** True if the runtime is in any of the loading-ish phases. */
    val isLoading: Boolean
        get() = phase == CollectionRuntimePhase.COLD_START ||
            phase == CollectionRuntimePhase.HYDRATING ||
            phase == CollectionRuntimePhase.PARTIAL_READY

    /** True if the runtime is ready for the user to interact with. */
    val isReady: Boolean get() = phase == CollectionRuntimePhase.READY

    /** True if the runtime is in an error or blocked state. */
    val isBlocked: Boolean
        get() = phase == CollectionRuntimePhase.ERROR ||
            phase == CollectionRuntimePhase.EMPTY ||
            phase == CollectionRuntimePhase.PERMISSION_REQUIRED

    /** Total cards across all visible collections. */
    val totalVisibleCards: Int
        get() = snapshots.values.sumOf { it.visibleCardCount }

    /** Number of collections currently visible (cards rendered). */
    val visibleCollectionCount: Int
        get() = snapshots.values.count { it.isVisible }

    /** Look up a snapshot by stable key. Rule #44. */
    fun snapshotFor(stableKey: String): CollectionRuntimeSnapshot? =
        snapshots[stableKey]

    /** Look up a contract by stable key. */
    fun contractFor(stableKey: String): CollectionContract? =
        activeContracts.firstOrNull { it.stableKey == stableKey }

    /** Look up refresh state by stable key. */
    fun refreshStateFor(stableKey: String): CollectionRefreshState? =
        refreshStates[stableKey]

    /** True if any collection is currently refreshing. */
    val hasAnyRefreshInProgress: Boolean
        get() = refreshStates.values.any { it.isRefreshing }
}
