package com.rikodev.music.android.presentation.visualos.host

/**
 * The ONE place `VisualOsCoordinator`'s world-facing callbacks may be
 * assigned (Phase 4, G-4).
 *
 * Previously MainActivity and `VisualOsNavBridge` both assigned the same
 * fields — whichever composed/disposed last won, so the dock could end up
 * with NavBridge's empty stubs (no play/pause, black color extraction) and
 * NavBridge's dispose nulled MainActivity's real wiring. Single owner:
 * MainActivity wires through [VisualOsCoordinatorWiring]; the bridge only
 * syncs Nav → Coordinator scenes and never touches these fields.
 *
 * Pure Kotlin on purpose so the ownership contract is unit-testable
 * (`VisualOsCoordinatorWiringTest`).
 */
object VisualOsCoordinatorWiring {

    /**
     * Assigns every coordinator→world callback and returns the disposer that
     * nulls them again (memory-leak guard, LibraryHostCoordinator pattern).
     * Callers must hold the returned lambda for the lifetime of their
     * composition and invoke it on dispose.
     */
    fun wire(
        coordinator: VisualOsCoordinator,
        navigateToNowPlaying: () -> Unit,
        togglePlayPause: () -> Unit,
        skipToNext: () -> Unit,
        skipToPrevious: () -> Unit,
        backPress: () -> Unit,
        extractColors: (String, (List<Long>, Long, Long) -> Unit) -> Unit,
    ): () -> Unit {
        coordinator.onNavigateToNowPlaying = navigateToNowPlaying
        coordinator.onTogglePlayPause = togglePlayPause
        coordinator.onSkipToNext = skipToNext
        coordinator.onSkipToPrevious = skipToPrevious
        coordinator.onBackPress = backPress
        coordinator.onExtractColors = extractColors
        return {
            coordinator.onNavigateToNowPlaying = null
            coordinator.onTogglePlayPause = null
            coordinator.onSkipToNext = null
            coordinator.onSkipToPrevious = null
            coordinator.onBackPress = null
            coordinator.onExtractColors = null
        }
    }
}
