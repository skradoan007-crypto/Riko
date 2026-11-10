package com.rikodev.music.android

import com.rikodev.music.android.presentation.visualos.host.VisualOsCoordinator
import com.rikodev.music.android.presentation.visualos.host.VisualOsCoordinatorWiring
import com.rikodev.music.android.presentation.visualos.runtime.VisualOsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Phase 4 / G-4: the VisualOsCoordinator → world callbacks have exactly ONE
 * owner — [VisualOsCoordinatorWiring], invoked from MainActivity. The bridge
 * must never assign them again.
 *
 * These tests pin the ownership contract:
 *  1. wire() assigns all six callbacks,
 *  2. each callback delegates to the dependency it was given,
 *  3. the reducer → coordinator action pipeline reaches the wired callback,
 *  4. the disposer nulls everything (memory-leak guard).
 */
class VisualOsCoordinatorWiringTest {

    private lateinit var coordinator: VisualOsCoordinator
    private var scope: CoroutineScope? = null

    private var navigations = 0
    private var toggles = 0
    private var nexts = 0
    private var prevs = 0
    private var backs = 0
    private var extracts = 0
    private var disposeWiring: () -> Unit = {}

    @Before
    fun setUp() {
        coordinator = VisualOsCoordinator()
        scope = CoroutineScope(Dispatchers.Unconfined)
        navigations = 0
        toggles = 0
        nexts = 0
        prevs = 0
        backs = 0
        extracts = 0
        disposeWiring = VisualOsCoordinatorWiring.wire(
            coordinator = coordinator,
            navigateToNowPlaying = { navigations++ },
            togglePlayPause = { toggles++ },
            skipToNext = { nexts++ },
            skipToPrevious = { prevs++ },
            backPress = { backs++ },
            extractColors = { _, callback ->
                extracts++
                callback(emptyList(), 0L, 0L)
            }
        )
    }

    @After
    fun tearDown() {
        disposeWiring()
        scope?.cancel()
    }

    @Test
    fun wire_assignsAllSixCallbacks() {
        assertNotNull(coordinator.onNavigateToNowPlaying)
        assertNotNull(coordinator.onTogglePlayPause)
        assertNotNull(coordinator.onSkipToNext)
        assertNotNull(coordinator.onSkipToPrevious)
        assertNotNull(coordinator.onBackPress)
        assertNotNull(coordinator.onExtractColors)
    }

    @Test
    fun callbacksDelegateToTheirDependencies() {
        coordinator.onNavigateToNowPlaying?.invoke()
        coordinator.onTogglePlayPause?.invoke()
        coordinator.onSkipToNext?.invoke()
        coordinator.onSkipToPrevious?.invoke()
        coordinator.onBackPress?.invoke()
        coordinator.onExtractColors?.invoke("content://media/external/audio/albumart/1") { _, _, _ -> }

        assertEquals(1, navigations)
        assertEquals(1, toggles)
        assertEquals(1, nexts)
        assertEquals(1, prevs)
        assertEquals(1, backs)
        assertEquals(1, extracts)
    }

    @Test
    fun dispatch_pipeline_reachesWiredCallback() {
        // Reducer turns the dock event into VisualOsAction.TogglePlayPause,
        // which the coordinator executes through the wired callback.
        coordinator.init(scope!!)
        coordinator.dispatch(VisualOsEvent.DockPlayPauseToggled)
        assertEquals(1, toggles)
    }

    @Test
    fun dispose_nullsAllCallbacks() {
        disposeWiring()
        assertNull(coordinator.onNavigateToNowPlaying)
        assertNull(coordinator.onTogglePlayPause)
        assertNull(coordinator.onSkipToNext)
        assertNull(coordinator.onSkipToPrevious)
        assertNull(coordinator.onBackPress)
        assertNull(coordinator.onExtractColors)
    }
}
