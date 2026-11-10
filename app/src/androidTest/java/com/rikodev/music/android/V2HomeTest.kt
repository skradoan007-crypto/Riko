package com.rikodev.music.android

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import com.rikodev.music.android.domain.model.Song
import com.rikodev.music.android.presentation.home.HomeScreenSections
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun testSong(id: Long, title: String) = Song(
    id = id, title = title, artist = "Test Artist", album = "Test Album",
    albumId = 0L, duration = 180000L, size = 0L, path = "", uri = Uri.EMPTY,
    dateAdded = 0L, dateModified = 0L, trackNumber = 0, year = 0,
    bitrate = 0, sampleRate = 0, mimeType = "", isFavorite = false
)

@RunWith(AndroidJUnit4::class)
class V2HomeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun homeSections_showShelves() {
        rule.setContent {
            V2Theme {
                HomeScreenSections(
                    recentSongs = listOf(testSong(1L, "Chill Beats")),
                    topSongs = listOf(testSong(2L, "EDM Pulse"))
                )
            }
        }
        rule.onNodeWithText("Recently Played").assertIsDisplayed()
        rule.onNodeWithText("Top Picks for You").assertIsDisplayed()
    }
}
