package com.rikodev.music.android.presentation.continuum

import com.rikodev.music.android.presentation.navigation.RikoDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class ContinuumNavHistoryTest {

    @Test
    fun `switching from Explore AlbumDetail to Library and back restores AlbumDetail`() {
        var state = ContinuumNavHistoryState()
        val albumRoute = RikoDestination.AlbumDetail.createRoute(99L)

        state = ContinuumNavHistoryReducer.onRouteVisited(state, RikoDestination.MyMusic.route, 1L)
        state = ContinuumNavHistoryReducer.onRouteVisited(state, albumRoute, 2L)
        assertEquals(ContinuumSection.EXPLORE, state.activeSection)

        val (inLibrary, libRoute) = ContinuumNavHistoryReducer.onSectionSelected(state, ContinuumSection.LIBRARY, 3L)
        assertEquals(ContinuumSection.LIBRARY, inLibrary.activeSection)
        assertEquals(RikoDestination.LibraryHub.route, libRoute)

        val (backInExplore, restoredRoute) = ContinuumNavHistoryReducer.onSectionSelected(inLibrary, ContinuumSection.EXPLORE, 4L)
        assertEquals(ContinuumSection.EXPLORE, backInExplore.activeSection)
        assertEquals(albumRoute, restoredRoute)
    }

    @Test
    fun `tapping active section while deep inside resets to section root`() {
        var state = ContinuumNavHistoryState()
        state = ContinuumNavHistoryReducer.onRouteVisited(state, RikoDestination.AlbumDetail.createRoute(12L), 1L)
        val (resetState, rootRoute) = ContinuumNavHistoryReducer.onSectionSelected(state, ContinuumSection.EXPLORE, 2L)
        assertEquals(RikoDestination.MyMusic.route, rootRoute)
        assertEquals(RikoDestination.MyMusic.route, resetState.targetRouteFor(ContinuumSection.EXPLORE))
    }

    @Test
    fun `contextual action pocket matches active CONTINUUM section`() {
        assertEquals(
            listOf(ContinuumContextAction.QUEUE, ContinuumContextAction.LYRICS, ContinuumContextAction.MDNA),
            ContinuumContextAction.forSection(ContinuumSection.LISTEN)
        )
        assertEquals(
            listOf(ContinuumContextAction.ARTIST, ContinuumContextAction.ALBUM, ContinuumContextAction.SONG_INFO),
            ContinuumContextAction.forSection(ContinuumSection.EXPLORE)
        )
        assertEquals(
            listOf(ContinuumContextAction.FAVORITE, ContinuumContextAction.ADD_TO_PLAYLIST, ContinuumContextAction.PLAYLISTS),
            ContinuumContextAction.forSection(ContinuumSection.LIBRARY)
        )
    }
}
