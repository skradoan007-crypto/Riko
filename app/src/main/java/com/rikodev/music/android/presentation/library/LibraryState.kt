package com.rikodev.music.android.presentation.library

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.Animatable

// ═══════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MILESTONE 2: LIBRARY RUNTIME — STATE MODEL
//
// Phase A, File 1: LibraryState.kt
//
// Pure Kotlin data classes. Immutable. No Compose. No business logic.
// No side effects. No Experience OS computation.
//
// ═══ PHILOSOPHY ═══
//   "The library should feel alive even before the user presses Play."
//
//   Library ≠ Database Screen.
//   Library = Memory, Emotion, Collection, Identity.
//
// ═══ ARCHITECTURE ═══
//
//   Repository (Room)
//       │
//       ▼
//   LibraryMapper (pure)
//       │
//       ▼
//   LibraryReducer (pure)
//       │
//       ▼
//   LibraryState (immutable)
//       │
//       ▼
//   StateFlow<LibraryState>
//       │
//       ▼
//   (Waiting for Rendering)
//
// ⭐ GOLDEN RULE ⭐
//   Library UI reads ONLY LibraryState.
//   NEVER Room. NEVER DAO. NEVER Repository.
//   NEVER PlayerState. NEVER ColorState.
//
// ═══ RULES APPLIED ═══
//   #26 — Collection First: Library renders collections, not database rows.
//   #27 — Screen Never Sorts: Collection Engine decides sorting.
//   #28 — Every Card Owns One Snapshot.
//   #29 — Motion Explains Hierarchy.
//   #30 — Empty State Has Emotion.
//   #31 — Runtime Owns Truth.
//   #32 — Collection Has Identity.
//   #33 — Cards Never Fetch.
//   #34 — Header Explains Context.
//   #35 — Scrolling Never Loses Context.
// ═══════════════════════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────────────────────────────────
// HEADER STATE — Rule #34: Header Explains Context
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Header contextual state. Tells the user WHY they are seeing this content.
 *
 * Rule #34: Header is not just a title. It explains context.
 * Example: "Recently Played" → "12 songs from the last 3 days"
 *
 * Source: Collection Engine → mapped to this by Mapper.
 * UI NEVER computes this.
 */
data class LibraryHeaderState(
    /** Main greeting or section title. e.g. "Good Morning", "Recently Played" */
    val title: String = "",

    /** Contextual subtitle. e.g. "12 songs from the last 3 days" */
    val subtitle: String = "",

    /** Whether the header is in expanded (greeting) or collapsed mode. */
    val isExpanded: Boolean = true,

    /** Collapse/expand animation progress. 0f = expanded, 1f = collapsed. */
    val collapseProgress: Float = 0f
) {
    companion object {
        /** Default state — no context, header expanded. */
        val IDLE = LibraryHeaderState()

        /** Empty library greeting — emotional, not technical (Rule #30). */
        val EMPTY_GREETING = LibraryHeaderState(
            title = "Your Music World",
            subtitle = "Start building your collection",
            isExpanded = true
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SELECTION STATE — Multi-select mode
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Multi-selection state for batch operations (delete, add to playlist, share).
 *
 * Source: User tap events → Reducer → this.
 * UI reads this to show selection overlay, action bar, etc.
 */
data class LibrarySelectionState(
    /** Whether selection mode is active. */
    val isActive: Boolean = false,

    /** Set of selected item IDs (Song.id, Album.id, etc.). */
    val selectedIds: Set<Long> = emptySet(),

    /** Total count of currently selected items. */
    val selectionCount: Int = 0
) {
    companion object {
        val IDLE = LibrarySelectionState()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCROLL STATE — Rule #35: Scrolling Never Loses Context
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Scroll tracking state. Sticky, adaptive, alive.
 *
 * Rule #35: Scrolling never loses context.
 * Sticky headers survive scroll. Section index visible.
 *
 * Source: Scroll events → Reducer → this.
 * UI NEVER computes scroll position from LazyColumn directly.
 */
data class LibraryScrollState(
    /** Current first visible item index. */
    val firstVisibleIndex: Int = 0,

    /** Current first visible item scroll offset in pixels. */
    val firstVisibleOffset: Int = 0,

    /** Current scroll direction. */
    val direction: ScrollDirection = ScrollDirection.NONE,

    /** Whether user is actively scrolling (finger on screen). */
    val isScrolling: Boolean = false,

    /** Current section label visible at top (sticky header). */
    val stickySection: String = "",

    /** Fast scroll active state. */
    val isFastScrolling: Boolean = false
) {
    enum class ScrollDirection { NONE, UP, DOWN }

    companion object {
        val IDLE = LibraryScrollState()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SEARCH STATE — Search entry point (full search in Milestone 3)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Search state within Library.
 *
 * This is the "entry point" search — quick filter within Library.
 * Full search experience comes in Milestone 3 (Search Experience).
 * Architecture ready for future Search integration.
 *
 * Source: User input → Reducer → this.
 */
data class LibrarySearchState(
    /** Whether search bar is expanded. */
    val isExpanded: Boolean = false,

    /** Current search query text. */
    val query: String = "",

    /** Whether a search is actively filtering content. */
    val isSearching: Boolean = false,

    /** Whether search yielded zero results. */
    val hasNoResults: Boolean = false
) {
    companion object {
        val IDLE = LibrarySearchState()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FILTER STATE — Active content filters
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Filter state — what content the user wants to see.
 *
 * Rule #27: Screen Never Sorts. But filtering is a user action.
 * Filters narrow scope. Sorting is Collection Engine's job.
 *
 * Source: User filter actions → Reducer → this.
 */
data class LibraryFilterState(
    /** Current content type filter. */
    val contentType: LibraryContentType = LibraryContentType.ALL,

    /** Whether only favorites should be shown. */
    val favoritesOnly: Boolean = false,

    /** Active filter chips. */
    val activeFilters: Set<String> = emptySet()
) {
    companion object {
        val IDLE = LibraryFilterState()
    }
}

/**
 * Content type filter for Library view.
 */
enum class LibraryContentType {
    /** Show all content (default). */
    ALL,

    /** Show only songs. */
    SONGS,

    /** Show only albums. */
    ALBUMS,

    /** Show only artists. */
    ARTISTS,

    /** Show only folders/file system view. */
    FOLDERS,

    /** Show only playlists. */
    PLAYLISTS
}

// ─────────────────────────────────────────────────────────────────────────────
// SORT STATE — Rule #27: Screen Never Sorts
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sort display state.
 *
 * Rule #27: Screen NEVER decides sorting.
 * This state only DISPLAYS what Collection Engine decided.
 * UI shows the current sort label, but never computes sort order.
 */
data class LibrarySortState(
    /** Current sort method label (set by Collection Engine). */
    val currentSortLabel: String = "Smart",

    /** Whether sort order is ascending. */
    val isAscending: Boolean = true,

    /** Whether sort picker is visible. */
    val isPickerVisible: Boolean = false
) {
    companion object {
        val IDLE = LibrarySortState()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DOCK STATE — Mini Player Dock
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mini Player dock state within Library.
 *
 * When a song is playing, Mini Player docks at the bottom of Library.
 * This state tracks dock visibility and interaction readiness.
 *
 * Source: PlayerState (via ViewModel) → mapped to this by Mapper.
 */
data class LibraryDockState(
    /** Whether Mini Player dock should be visible. */
    val isDocked: Boolean = false,

    /** Dock vertical offset for animation (0f = fully visible). */
    val dockOffset: Float = 0f,

    /** Whether dock is being pressed (to expand). */
    val isPressed: Boolean = false
) {
    companion object {
        val IDLE = LibraryDockState()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RUNTIME STATE — Loading, errors, permissions, lifecycle
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Runtime lifecycle state.
 *
 * Tracks: loading, errors, permission status, scanning state.
 * This is Library's OWN state — not from Experience OS.
 */
data class LibraryRuntimeState(
    /** Current lifecycle phase of Library. */
    val phase: LibraryPhase = LibraryPhase.LOADING,

    /** Whether Library has finished initial load. */
    val isReady: Boolean = false,

    /** Whether media permission has been granted. */
    val hasPermission: Boolean = false,

    /** Whether Library is currently scanning for new files. */
    val isScanning: Boolean = false,

    /** Scan progress (0f..1f). 0 = just started, 1 = complete. */
    val scanProgress: Float = 0f,

    /** Total song count in Library. */
    val totalSongCount: Int = 0,

    /** Total album count. */
    val totalAlbumCount: Int = 0,

    /** Total artist count. */
    val totalArtistCount: Int = 0,

    /** Error message if Library failed to load. Null if no error. */
    val error: String? = null
) {
    /** True when Library is fully loaded and ready for interaction. */
    val isLoaded: Boolean
        get() = isReady && !isScanning && hasPermission && error == null

    /** True when Library is completely empty (no songs at all). */
    val isEmpty: Boolean
        get() = isReady && totalSongCount == 0

    companion object {
        val IDLE = LibraryRuntimeState()
    }
}

/**
 * Library lifecycle phases.
 */
enum class LibraryPhase {
    /** Initial loading — data being fetched from repository. */
    LOADING,

    /** Permission required — user hasn't granted media access yet. */
    PERMISSION_REQUIRED,

    /** Scanning — new files being discovered and indexed. */
    SCANNING,

    /** Ready — Library fully loaded, interactive. */
    READY,

    /** Error — failed to load Library data. */
    ERROR
}

// ─────────────────────────────────────────────────────────────────────────────
// COLOR SNAPSHOT — Derived from ColorState (Rule #31)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Lightweight color snapshot for Library rendering.
 *
 * Rule #31: Runtime Owns Truth.
 * Color comes from ColorIntelligenceEngine → ColorState → mapped here.
 * Library NEVER reads ColorState directly.
 *
 * Library color usage is different from Mini Player:
 * - More ambient, less reactive
 * - Background tint, card tint, section accents
 * - Subtle wave (not dominant like Now Playing)
 */
data class LibraryColorSnapshot(
    val primary: Color = Color(0xFF19B7FF),
    val secondary: Color = Color(0xFF9A6CFF),
    val accent: Color = Color(0xFFFF6FD8),
    val backgroundTint: Color = Color(0x0AFFFFFF),
    val cardGlass: Color = Color(0x0FFFFFFF),
    val sectionAccent: Color = Color(0xFF19B7FF),
    val dockTint: Color = Color(0x14FFFFFF),
    val dominantLuminance: Float = 0.15f,
    val isDark: Boolean = true
) {
    companion object {
        val DEFAULT = LibraryColorSnapshot()

        val FALLBACK = LibraryColorSnapshot(
            primary = Color(0xFF1A1A2E),
            secondary = Color(0xFF16213E),
            accent = Color(0xFFE94560),
            isDark = true
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WAVE SNAPSHOT — Subtle wave for Library (Rule #29)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Lightweight wave snapshot for Library rendering.
 *
 * Rule #29: Motion Explains Hierarchy.
 * Library wave is SUBTLE — alive but not dominant.
 * Now Playing = dominant wave. Library = ambient wave.
 * This hierarchy is communicated through the wave snapshot.
 *
 * Source: WaveEngine → WaveState → mapped here by Mapper.
 */
data class LibraryWaveSnapshot(
    val isActive: Boolean = false,
    val energy: Float = 0f,
    val opacity: Float = 0.3f,
    val thickness: Float = 0.5f
) {
    val isIdle: Boolean
        get() = !isActive || energy < 0.01f

    companion object {
        val DEFAULT = LibraryWaveSnapshot()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN STATE — The single source of truth for Library UI
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The complete, immutable state for Library Experience.
 *
 * Every pixel Library renders is derived from this single data class.
 * Components collect one StateFlow of this type and render.
 * No direct access to Room, DAO, Repository, PlayerState, or ColorState.
 *
 * ═══ IMMUTABLE ═══
 *   This is a data class. UI cannot mutate it.
 *   Every change creates a new instance via copy().
 *   Flow: LibraryViewModel → StateFlow<LibraryState> → Composable.
 *
 * ═══ CONSUMER RULE ═══
 *   LibraryState does NOT own:
 *     - ColorState (read-only snapshot via LibraryColorSnapshot)
 *     - WaveState (read-only snapshot via LibraryWaveSnapshot)
 *     - PlayerState (read-only dock presence via LibraryDockState)
 *   It does NOT contain any Room queries, DAO calls, or Repository access.
 *   It does NOT overlap with NowPlayingUiState or MiniPlayerState.
 *
 * ═══ COMPOSITION ═══
 *   Contains 10 sub-states for logical grouping:
 *     header     — greeting, context, collapse (Rule #34)
 *     selection  — multi-select mode
 *     scroll     — scroll tracking, sticky headers (Rule #35)
 *     search     — quick filter entry point
 *     filter     — content type filter
 *     sort       — sort display label (Rule #27)
 *     dock       — Mini Player dock presence
 *     runtime    — lifecycle, loading, errors
 *     color      — from ColorState (Experience OS)
 *     wave       — from WaveState (Experience OS)
 */
data class LibraryState(
    // ── Header (Rule #34) ───────────────────────────────────────────────
    /** Greeting and contextual header. */
    val header: LibraryHeaderState = LibraryHeaderState(),

    // ── Selection ──────────────────────────────────────────────────────
    /** Multi-selection mode state. */
    val selection: LibrarySelectionState = LibrarySelectionState(),

    // ── Scroll (Rule #35) ────────────────────────────────────────────
    /** Scroll tracking with sticky context. */
    val scroll: LibraryScrollState = LibraryScrollState(),

    // ── Search ─────────────────────────────────────────────────────────
    /** Quick search/filter entry point. */
    val search: LibrarySearchState = LibrarySearchState(),

    // ── Filter ────────────────────────────────────────────────────────
    /** Content type filter. */
    val filter: LibraryFilterState = LibraryFilterState(),

    // ── Sort (Rule #27) ──────────────────────────────────────────────
    /** Sort display state. Screen never decides sorting. */
    val sort: LibrarySortState = LibrarySortState(),

    // ── Dock (Mini Player) ────────────────────────────────────────────
    /** Mini Player dock at bottom of Library. */
    val dock: LibraryDockState = LibraryDockState(),

    // ── Runtime ────────────────────────────────────────────────────────
    /** Lifecycle, loading, permissions, scanning. */
    val runtime: LibraryRuntimeState = LibraryRuntimeState(),

    // ── Color (from Experience OS) ─────────────────────────────────────
    /** Color snapshot for Library rendering. */
    val color: LibraryColorSnapshot = LibraryColorSnapshot(),

    // ── Wave (from Experience OS) ──────────────────────────────────────
    /** Wave snapshot for Library rendering. */
    val wave: LibraryWaveSnapshot = LibraryWaveSnapshot()
) {
    /** True when Library is fully loaded and ready for interaction. */
    val isLoaded: Boolean
        get() = runtime.isLoaded

    /** True when Library is empty (no songs at all). Rule #30. */
    val isEmpty: Boolean
        get() = runtime.isEmpty

    /** True when an error is present. */
    val hasError: Boolean
        get() = runtime.error != null

    /** True when permission is required before showing content. */
    val needsPermission: Boolean
        get() = runtime.phase == LibraryPhase.PERMISSION_REQUIRED

    /** True when Library is scanning for new files. */
    val isScanning: Boolean
        get() = runtime.isScanning

    companion object {
        /** Initial state — nothing loaded, permission not checked. */
        val IDLE = LibraryState()
    }
}
