package com.rikodev.music.android.presentation.library

import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.WaveState
import androidx.compose.animation.core.Animatable

// ═══════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MILESTONE 2: LIBRARY RUNTIME — STATE FACTORY
//
// Phase A, File 7: LibraryStateFactory.kt
//
// Creates initial LibraryState for every lifecycle scenario.
// NO UI. NO Compose. NO side effects. Pure state creation.
//
// Scenarios:
//   1. Cold Start          — App just launched
//   2. Permission Granted   — User granted media access
//   3. Permission Denied    — User denied media access
//   4. Empty Library        — Permission granted, no songs found
//   5. Scanning            — Library scanning for new files
//   6. Ready               — Library fully loaded
//   7. Import Finished     — New songs imported after scan
//   8. Restore Session     — User returned from recent apps / deep link
//   9. Error Recovery       — Error cleared, retrying
//
// ⭐ SINGLE RESPONSIBILITY ⭐
//   Factory only CREATES initial state.
//   It does NOT update state (that's the reducer's job).
//   It does NOT map Experience OS (that's the mapper's job).
//   It uses Mapper for the mapping step, then composes the full state.
//
// ⭐ KEY DESIGN DECISION ⭐
//   Every factory method returns a COMPLETE LibraryState.
//   No partial state, no "then call mergeExperience later".
//   If you get a state from Factory, it's ready to render.
//
// ⭐ PATTERN: Same DNA as MiniPlayerStateFactory ⭐
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Factory for creating initial [LibraryState] instances.
 *
 * Each factory method corresponds to a specific app lifecycle scenario.
 * The factory uses [LibraryMapper] for Experience OS mapping, then
 * composes the complete state including all 10 sub-states.
 *
 * All methods are pure functions — deterministic, no side effects,
 * no Android framework dependencies.
 */
object LibraryStateFactory {

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 1: COLD START
    // ═════════════════════════════════════════════════════════════════

    /**
     * Cold start — app just launched, nothing loaded.
     *
     * State: Loading phase, no permission checked yet.
     * Library shows loading placeholder while ViewModel checks
     * permission and begins loading content.
     *
     * When does this happen?
     *   - Fresh install, first launch
     *   - App killed and relaunched
     *   - After "Clear Data"
     *
     * What happens next?
     *   - ViewModel checks permission → forPermissionGranted/Denied
     *   - If permission granted, loads content → forReady
     */
    fun coldStart(): LibraryState {
        return LibraryState(
            header = LibraryHeaderState.IDLE,
            selection = LibrarySelectionState.IDLE,
            scroll = LibraryScrollState.IDLE,
            search = LibrarySearchState.IDLE,
            filter = LibraryFilterState.IDLE,
            sort = LibrarySortState.IDLE,
            dock = LibraryDockState.IDLE,
            runtime = LibraryRuntimeState(
                phase = LibraryPhase.LOADING,
                isReady = false,
                hasPermission = false
            ),
            color = LibraryColorSnapshot.DEFAULT,
            wave = LibraryWaveSnapshot.DEFAULT
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 2: PERMISSION GRANTED
    // ═════════════════════════════════════════════════════════════════

    /**
     * Permission granted — user allowed media access.
     *
     * State: Loading phase, permission recorded. Library will start
     * loading content from Repository.
     *
     * When does this happen?
     *   - First launch, user taps "Allow"
     *   - User re-grants permission from settings
     *
     * What happens next?
     *   - ViewModel loads content from Repository
     *   - Content loaded → forReady or forEmptyLibrary
     */
    fun forPermissionGranted(): LibraryState {
        return LibraryState(
            header = LibraryHeaderState(
                title = "Loading Your Music",
                subtitle = "Discovering your collection...",
                isExpanded = true
            ),
            selection = LibrarySelectionState.IDLE,
            scroll = LibraryScrollState.IDLE,
            search = LibrarySearchState.IDLE,
            filter = LibraryFilterState.IDLE,
            sort = LibrarySortState.IDLE,
            dock = LibraryDockState.IDLE,
            runtime = LibraryRuntimeState(
                phase = LibraryPhase.LOADING,
                isReady = false,
                hasPermission = true
            ),
            color = LibraryColorSnapshot.DEFAULT,
            wave = LibraryWaveSnapshot.DEFAULT
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 3: PERMISSION DENIED
    // ═════════════════════════════════════════════════════════════════

    /**
     * Permission denied — user refused media access.
     *
     * State: Permission required phase. Library cannot show content.
     * UI should show permission explanation and request button.
     *
     * When does this happen?
     *   - First launch, user taps "Deny"
     *   - User revoked permission from settings
     *
     * What happens next?
     *   - User taps "Grant Permission" → ViewModel requests permission
     *   - Permission granted → forPermissionGranted
     */
    fun forPermissionDenied(): LibraryState {
        return LibraryState(
            header = LibraryHeaderState(
                title = "Access Required",
                subtitle = "RIKO needs access to your music files",
                isExpanded = true
            ),
            selection = LibrarySelectionState.IDLE,
            scroll = LibraryScrollState.IDLE,
            search = LibrarySearchState.IDLE,
            filter = LibraryFilterState.IDLE,
            sort = LibrarySortState.IDLE,
            dock = LibraryDockState.IDLE,
            runtime = LibraryRuntimeState(
                phase = LibraryPhase.PERMISSION_REQUIRED,
                isReady = false,
                hasPermission = false
            ),
            color = LibraryColorSnapshot.DEFAULT,
            wave = LibraryWaveSnapshot.DEFAULT
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 4: EMPTY LIBRARY
    // ═════════════════════════════════════════════════════════════════

    /**
     * Empty library — permission granted, no songs found.
     *
     * State: Ready phase, but zero songs. Rule #30: Empty State Has Emotion.
     * Header shows emotional greeting, not "No songs".
     * UI should show beautiful empty state experience.
     *
     * When does this happen?
     *   - Fresh install, no music on device
     *   - All songs deleted
     *   - After "Clear Data" on a fresh device
     *
     * What happens next?
     *   - User imports music → ScanStarted → forScanning → forReady
     */
    fun forEmptyLibrary(): LibraryState {
        return LibraryState(
            header = LibraryHeaderState.EMPTY_GREETING,
            selection = LibrarySelectionState.IDLE,
            scroll = LibraryScrollState.IDLE,
            search = LibrarySearchState.IDLE,
            filter = LibraryFilterState.IDLE,
            sort = LibrarySortState.IDLE,
            dock = LibraryDockState.IDLE,
            runtime = LibraryRuntimeState(
                phase = LibraryPhase.READY,
                isReady = true,
                hasPermission = true,
                isScanning = false,
                totalSongCount = 0,
                totalAlbumCount = 0,
                totalArtistCount = 0
            ),
            color = LibraryColorSnapshot.DEFAULT,
            wave = LibraryWaveSnapshot.DEFAULT
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 5: SCANNING
    // ═════════════════════════════════════════════════════════════════

    /**
     * Scanning — Library discovering new files.
     *
     * State: Scanning phase. Shows progress indicator.
     * Existing content is still visible and interactive.
     *
     * When does this happen?
     *   - New files detected on device
     *   - User triggered manual scan
     *   - Pull-to-refresh triggered scan
     *
     * What happens next?
     *   - Scan completes → forImportFinished
     */
    fun forScanning(
        currentState: LibraryState,
        progress: Float = 0f
    ): LibraryState {
        return currentState.copy(
            runtime = currentState.runtime.copy(
                phase = LibraryPhase.SCANNING,
                isScanning = true,
                scanProgress = progress.coerceIn(0f, 1f)
            ),
            // Keep all content visible during scan
            header = if (progress < 0.1f) {
                currentState.header.copy(
                    subtitle = "Scanning for new music..."
                )
            } else {
                currentState.header
            }
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 6: READY — Full Library
    // ═════════════════════════════════════════════════════════════════

    /**
     * Ready — Library fully loaded with content.
     *
     * State: Ready phase. All content available. Fully interactive.
     * This is the "normal" state for Library.
     *
     * When does this happen?
     *   - After initial content load
     *   - After scan completes with content
     *   - After pull-to-refresh
     *
     * @param songCount Total songs in Library.
     * @param albumCount Total albums.
     * @param artistCount Total artists.
     * @param isDocked Whether Mini Player should be docked (song playing).
     */
    fun forReady(
        songCount: Int,
        albumCount: Int,
        artistCount: Int,
        isDocked: Boolean = false
    ): LibraryState {
        return LibraryState(
            header = LibraryHeaderState(
                title = "Good Morning",
                subtitle = buildGreetingSubtitle(songCount),
                isExpanded = true
            ),
            selection = LibrarySelectionState.IDLE,
            scroll = LibraryScrollState.IDLE,
            search = LibrarySearchState.IDLE,
            filter = LibraryFilterState.IDLE,
            sort = LibrarySortState(
                currentSortLabel = "Smart"
            ),
            dock = LibraryDockState(
                isDocked = isDocked
            ),
            runtime = LibraryRuntimeState(
                phase = LibraryPhase.READY,
                isReady = true,
                hasPermission = true,
                isScanning = false,
                totalSongCount = songCount,
                totalAlbumCount = albumCount,
                totalArtistCount = artistCount
            ),
            color = LibraryColorSnapshot.DEFAULT,
            wave = LibraryWaveSnapshot.DEFAULT
        )
    }

    /**
     * Ready — with Experience OS data.
     *
     * Called by ViewModel when it has ColorState and WaveState
     * available at Library ready time.
     */
    fun forReady(
        songCount: Int,
        albumCount: Int,
        artistCount: Int,
        isDocked: Boolean,
        colorState: ColorState,
        waveState: WaveState
    ): LibraryState {
        return forReady(
            songCount, albumCount, artistCount, isDocked
        ).copy(
            color = LibraryMapper.mapColors(colorState),
            wave = LibraryMapper.mapWave(waveState)
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 7: IMPORT FINISHED
    // ═════════════════════════════════════════════════════════════════

    /**
     * Import finished — new songs discovered after scan.
     *
     * State: Ready phase with updated counts. Scan ended.
     * UI can show a subtle "X new songs found" indicator.
     *
     * @param currentState State before import.
     * @param newSongCount Number of new songs found.
     * @param totalSongCount Updated total song count.
     * @param totalAlbumCount Updated total album count.
     * @param totalArtistCount Updated total artist count.
     */
    fun forImportFinished(
        currentState: LibraryState,
        newSongCount: Int,
        totalSongCount: Int,
        totalAlbumCount: Int,
        totalArtistCount: Int
    ): LibraryState {
        return currentState.copy(
            runtime = currentState.runtime.copy(
                phase = LibraryPhase.READY,
                isScanning = false,
                scanProgress = 1f,
                totalSongCount = totalSongCount,
                totalAlbumCount = totalAlbumCount,
                totalArtistCount = totalArtistCount
            ),
            header = if (newSongCount > 0) {
                currentState.header.copy(
                    subtitle = "$newSongCount new songs found"
                )
            } else {
                currentState.header
            }
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 8: RESTORE SESSION
    // ═════════════════════════════════════════════════════════════════

    /**
     * Session restore — user returned from recent apps or deep link.
     *
     * State: Ready (if data exists) or Loading (if needs refresh).
     * Library should resume exactly where the user left off.
     * Scroll position may be restored by LazyListState (outside this factory).
     *
     * When does this happen?
     *   - User switched away and came back (recent apps)
     *   - Deep link opened the app
     *   - Activity recreation (config change)
     *   - Process death recovery
     *
     * Key behavior:
     *   - Selection is RESET (process death = fresh start)
     *   - Search is RESET
     *   - Filter is RESET to ALL
     *   - Scroll position is NOT restored here (LazyListState handles it)
     *
     * @param songCount Known song count (from saved state or quick query).
     * @param albumCount Known album count.
     * @param artistCount Known artist count.
     * @param isDocked Whether a song was playing (Mini Player dock).
     */
    fun forSessionRestore(
        songCount: Int,
        albumCount: Int,
        artistCount: Int,
        isDocked: Boolean = false
    ): LibraryState {
        return LibraryState(
            header = LibraryHeaderState(
                title = "Good Morning",
                subtitle = buildGreetingSubtitle(songCount),
                isExpanded = true
            ),
            selection = LibrarySelectionState.IDLE,
            scroll = LibraryScrollState.IDLE,
            search = LibrarySearchState.IDLE,
            filter = LibraryFilterState.IDLE,
            sort = LibrarySortState.IDLE,
            dock = LibraryDockState(isDocked = isDocked),
            runtime = LibraryRuntimeState(
                phase = if (songCount > 0) LibraryPhase.READY
                        else LibraryPhase.LOADING,
                isReady = songCount > 0,
                hasPermission = true,
                totalSongCount = songCount,
                totalAlbumCount = albumCount,
                totalArtistCount = artistCount
            ),
            color = LibraryColorSnapshot.DEFAULT,
            wave = LibraryWaveSnapshot.DEFAULT
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // SCENARIO 9: ERROR RECOVERY
    // ═════════════════════════════════════════════════════════════════

    /**
     * Error recovery — error was cleared, retrying load.
     *
     * State: Loading phase. Previous error cleared.
     * UI shows loading state again.
     *
     * @param currentState State before error (preserves color/wave).
     */
    fun forErrorRecovery(currentState: LibraryState): LibraryState {
        return currentState.copy(
            runtime = LibraryRuntimeState(
                phase = LibraryPhase.LOADING,
                isReady = false,
                hasPermission = currentState.runtime.hasPermission,
                error = null
            ),
            selection = LibrarySelectionState.IDLE,
            search = LibrarySearchState.IDLE
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // HELPER — Greeting subtitle builder
    // ═════════════════════════════════════════════════════════════════

    /**
     * Build contextual greeting subtitle based on song count.
     *
     * Rule #34: Header Explains Context.
     * Subtitle tells user what they're looking at.
     *
     * Examples:
     *   - 1 song → "1 song in your collection"
     *   - 150 songs → "150 songs in your collection"
     *   - 1500 songs → "1,500 songs in your collection"
     */
    private fun buildGreetingSubtitle(songCount: Int): String {
        val formatted = if (songCount >= 1000) {
            "%,d".format(songCount)
        } else {
            songCount.toString()
        }
        val plural = if (songCount == 1) "song" else "songs"
        return "$formatted $plural in your collection"
    }
}
