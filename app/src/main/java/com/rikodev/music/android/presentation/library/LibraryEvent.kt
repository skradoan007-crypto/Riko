package com.rikodev.music.android.presentation.library

// ═══════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MILESTONE 2: LIBRARY RUNTIME — INPUT EVENTS
//
// Phase A, File 3: LibraryEvent.kt
//
// All events that can modify LibraryState.
// Events flow into the reducer: Event → Reducer → State → UI.
//
// Event sources:
//   1. User interactions (tap, long-press, scroll, search)
//   2. System events (permission, config change, memory pressure)
//   3. Experience OS (PlayerState, ColorState, WaveState changes)
//   4. Collection Engine callbacks (refresh complete, scan progress)
//
// ⭐ RULE: Events are INPUT ONLY.
//   They describe WHAT happened, not HOW to respond.
//   The reducer decides the response.
// ═══════════════════════════════════════════════════════════════════════════

/**
 * All events that can modify [LibraryState].
 *
 * Every state change in Library goes through this sealed class.
 * The reducer ([LibraryReducer]) is the ONLY function that produces
 * new state from these events.
 *
 * Events are organized by source:
 *   - Navigation events: navigating into/out of Library
 *   - Content events: tapping albums, songs, artists, etc.
 *   - Selection events: multi-select mode
 *   - Scroll events: scroll tracking, fast scroll
 *   - Search events: search bar interactions
 *   - Filter events: content type filtering
 *   - Header events: greeting collapse/expand
 *   - Dock events: Mini Player dock interactions
 *   - Experience OS events: data flowing in from ViewModel
 *   - System events: permission, config change, lifecycle
 *   - Collection Engine events: refresh, scan
 */
sealed class LibraryEvent {

    // ───────────────────────────────────────────────────────────────────
    // NAVIGATION EVENTS — Entering/leaving Library
    // ───────────────────────────────────────────────────────────────────

    /**
     * Library screen became visible (navigated to).
     * Reducer may trigger refresh if data is stale.
     */
    data object ScreenEntered : LibraryEvent()

    /**
     * Library screen became invisible (navigated away).
     * Reducer may pause animations, release resources.
     */
    data object ScreenExited : LibraryEvent()

    /**
     * Returned to Library from another screen (back press).
     */
    data object ScreenResumed : LibraryEvent()

    // ───────────────────────────────────────────────────────────────────
    // CONTENT EVENTS — User tapping on items
    // ───────────────────────────────────────────────────────────────────

    /**
     * User tapped on an album card.
     *
     * @param albumId The album's unique ID.
     */
    data class TapAlbum(val albumId: Long) : LibraryEvent()

    /**
     * User tapped on an artist card.
     *
     * @param artistId The artist's unique ID.
     */
    data class TapArtist(val artistId: Long) : LibraryEvent()

    /**
     * User tapped on a folder card.
     *
     * @param folderPath The folder's path (used as ID).
     */
    data class TapFolder(val folderPath: String) : LibraryEvent()

    /**
     * User tapped on a playlist card.
     *
     * @param playlistId The playlist's unique ID.
     */
    data class TapPlaylist(val playlistId: Long) : LibraryEvent()

    /**
     * User tapped on a song item (play it).
     *
     * @param songId The song's unique ID.
     * @param contextPlaylist ID of the playlist/album providing play context.
     *                       Null means play as single track.
     */
    data class TapSong(
        val songId: Long,
        val contextPlaylist: Long? = null
    ) : LibraryEvent()

    /**
     * User tapped on a smart collection card.
     *
     * @param collectionId The collection's unique string ID.
     */
    data class TapCollection(val collectionId: String) : LibraryEvent()

    /**
     * User tapped on the favorite button for a song.
     *
     * @param songId The song's unique ID.
     */
    data class TapFavorite(val songId: Long) : LibraryEvent()

    // ───────────────────────────────────────────────────────────────────
    // SELECTION EVENTS — Multi-select mode
    // ───────────────────────────────────────────────────────────────────

    /**
     * User long-pressed an item — enter selection mode.
     *
     * @param itemId The item's unique ID.
     */
    data class LongPressItem(val itemId: Long) : LibraryEvent()

    /**
     * User tapped an item while in selection mode — toggle selection.
     *
     * @param itemId The item's unique ID.
     */
    data class ToggleSelection(val itemId: Long) : LibraryEvent()

    /**
     * User tapped "Select All" action.
     */
    data object SelectAll : LibraryEvent()

    /**
     * User tapped "Clear Selection" or back pressed in selection mode.
     */
    data object ClearSelection : LibraryEvent()

    // ───────────────────────────────────────────────────────────────────
    // SCROLL EVENTS — Scroll tracking (Rule #35)
    // ───────────────────────────────────────────────────────────────────

    /**
     * Scroll position changed.
     *
     * @param firstVisibleIndex First visible item index.
     * @param firstVisibleOffset Scroll offset of first visible item.
     */
    data class ScrollChanged(
        val firstVisibleIndex: Int,
        val firstVisibleOffset: Int
    ) : LibraryEvent()

    /**
     * Scroll direction detected.
     *
     * @param direction Current scroll direction.
     */
    data class ScrollDirectionChanged(
        val direction: LibraryScrollState.ScrollDirection
    ) : LibraryEvent()

    /**
     * User started/stopped scrolling (finger down/up on list).
     *
     * @param isScrolling True while finger is on screen and dragging.
     */
    data class ScrollActiveChanged(val isScrolling: Boolean) : LibraryEvent()

    /**
     * Fast scroll started (user grabbed the scroll thumb).
     */
    data object FastScrollStarted : LibraryEvent()

    /**
     * Fast scroll section jumped (user dragged thumb to a section).
     *
     * @param section Target section label.
     */
    data class FastScrollJump(val section: String) : LibraryEvent()

    /**
     * Fast scroll ended.
     */
    data object FastScrollEnded : LibraryEvent()

    /**
     * Sticky section header changed during scroll.
     *
     * @param section New sticky section label.
     */
    data class StickySectionChanged(val section: String) : LibraryEvent()

    // ───────────────────────────────────────────────────────────────────
    // SEARCH EVENTS — Search bar interactions
    // ───────────────────────────────────────────────────────────────────

    /**
     * User tapped the search bar — expand search input.
     */
    data object SearchBarTapped : LibraryEvent()

    /**
     * User typed in search bar.
     *
     * @param query Current search query text.
     */
    data class SearchQueryChanged(val query: String) : LibraryEvent()

    /**
     * User submitted search (IME action or enter key).
     *
     * @param query Final search query.
     */
    data class SearchSubmitted(val query: String) : LibraryEvent()

    /**
     * User cleared search (tapped clear button).
     */
    data object SearchCleared : LibraryEvent()

    /**
     * User closed search bar (collapsed).
     */
    data object SearchClosed : LibraryEvent()

    /**
     * Search returned no results.
     */
    data object SearchNoResults : LibraryEvent()

    // ───────────────────────────────────────────────────────────────────
    // FILTER EVENTS — Content type filtering
    // ───────────────────────────────────────────────────────────────────

    /**
     * User changed content type filter (Songs/Albums/Artists/Folders/Playlists).
     *
     * @param contentType New filter type.
     */
    data class FilterChanged(val contentType: LibraryContentType) : LibraryEvent()

    /**
     * User toggled "Favorites Only" filter.
     */
    data object ToggleFavoritesFilter : LibraryEvent()

    /**
     * User tapped a filter chip.
     *
     * @param filterChip The filter chip identifier.
     */
    data class FilterChipTapped(val filterChip: String) : LibraryEvent()

    // ───────────────────────────────────────────────────────────────────
    // HEADER EVENTS — Greeting/Header collapse (Rule #34)
    // ───────────────────────────────────────────────────────────────────

    /**
     * User scrolled down — header should start collapsing.
     */
    data object HeaderCollapseStart : LibraryEvent()

    /**
     * User scrolled up — header should start expanding.
     */
    data object HeaderExpandStart : LibraryEvent()

    /**
     * Header collapse/expand animation progress updated.
     *
     * @param progress 0f = fully expanded, 1f = fully collapsed.
     */
    data class HeaderCollapseProgress(val progress: Float) : LibraryEvent()

    // ───────────────────────────────────────────────────────────────────
    // DOCK EVENTS — Mini Player dock
    // ───────────────────────────────────────────────────────────────────

    /**
     * Mini Player dock tapped — expand to Now Playing.
     */
    data object DockTapped : LibraryEvent()

    /**
     * Mini Player dock press state changed.
     *
     * @param isPressed True while finger is on the dock.
     */
    data class DockPressChanged(val isPressed: Boolean) : LibraryEvent()

    /**
     * Dock animation offset updated.
     *
     * @param offset Current vertical offset.
     */
    data class DockOffsetChanged(val offset: Float) : LibraryEvent()

    // ───────────────────────────────────────────────────────────────────
    // EXPERIENCE OS EVENTS — Data flowing IN from ViewModel
    // ───────────────────────────────────────────────────────────────────
    // These carry raw Experience OS data. The reducer stores snapshots.
    // Actual mapping is done by LibraryMapper — reducer just assigns.

    /**
     * Playback state changed (from PlayerManager).
     * Used to update dock visibility (is a song playing?).
     */
    data class PlaybackChanged(
        val isPlaying: Boolean,
        val currentSongId: Long?
    ) : LibraryEvent()

    /**
     * Color palette changed (from ColorIntelligenceEngine).
     * Reducer updates LibraryColorSnapshot.
     */
    data class ColorChanged(
        val colorSnapshot: LibraryColorSnapshot
    ) : LibraryEvent()

    /**
     * Wave state updated (from WaveEngine).
     * Reducer updates LibraryWaveSnapshot.
     */
    data class WaveChanged(
        val waveSnapshot: LibraryWaveSnapshot
    ) : LibraryEvent()

    /**
     * Library content refreshed (from Repository via Collection Engine).
     * New organized content available.
     */
    data class ContentRefreshed(
        val content: LibraryContentSnapshot
    ) : LibraryEvent() {
        companion object
    }

    /**
     * Runtime counts updated (from Repository).
     */
    data class CountsUpdated(
        val songCount: Int,
        val albumCount: Int,
        val artistCount: Int
    ) : LibraryEvent()

    // ───────────────────────────────────────────────────────────────────
    // SYSTEM EVENTS — Permission, lifecycle, config
    // ───────────────────────────────────────────────────────────────────

    /**
     * Media permission result received.
     *
     * @param granted Whether user granted permission.
     */
    data class PermissionResult(val granted: Boolean) : LibraryEvent()

    /**
     * Library scanning started.
     */
    data object ScanStarted : LibraryEvent()

    /**
     * Library scanning progress updated.
     *
     * @param progress Scan progress (0f..1f).
     */
    data class ScanProgress(val progress: Float) : LibraryEvent()

    /**
     * Library scanning completed.
     *
     * @param newSongCount Number of new songs found during scan.
     */
    data class ScanComplete(val newSongCount: Int = 0) : LibraryEvent()

    /**
     * Configuration change (rotation, multi-window, density).
     */
    data object ConfigChanged : LibraryEvent()

    /**
     * Memory pressure detected.
     *
     * @param level Trim memory level.
     */
    data class MemoryPressure(val level: Int) : LibraryEvent()

    /**
     * Screen turned off.
     */
    data object ScreenOff : LibraryEvent()

    /**
     * Screen turned on.
     */
    data object ScreenOn : LibraryEvent()

    /**
     * App moved to background.
     */
    data object AppBackgrounded : LibraryEvent()

    /**
     * App moved to foreground.
     */
    data object AppForegrounded : LibraryEvent()

    /**
     * Error occurred loading Library data.
     *
     * @param message Human-readable error message.
     */
    data class ErrorOccurred(val message: String) : LibraryEvent()

    /**
     * Error cleared (user dismissed or retry succeeded).
     */
    data object ErrorCleared : LibraryEvent()

    /**
     * Pull-to-refresh triggered.
     */
    data object PullRefresh : LibraryEvent()

    /**
     * Sort changed (from Collection Engine or user selection).
     * Rule #27: Screen NEVER decides this. Collection Engine decides.
     * This event just updates the display label.
     *
     * @param label New sort label to display.
     * @param isAscending Whether sort is ascending.
     */
    data class SortChanged(
        val label: String,
        val isAscending: Boolean = true
    ) : LibraryEvent()

    /**
     * Sort picker visibility toggled.
     */
    data object ToggleSortPicker : LibraryEvent()
}
