package com.rikodev.music.android

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.components.V2TestTags
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import com.rikodev.music.android.domain.model.Song
import com.rikodev.music.android.presentation.playlist.PlaylistDetailContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun fakeSong(id: Long, title: String) = Song(
    id = id, title = title, artist = "A", album = "B", albumId = 1L,
    duration = 254_000L, size = 8_000_000L, path = "/music/$id.mp3",
    uri = Uri.parse("content://media/external/audio/media/$id"),
    dateAdded = 0L, dateModified = 0L, trackNumber = 1, year = 2024,
    bitrate = 320, sampleRate = 44100, mimeType = "audio/mpeg", isFavorite = false
)

@RunWith(AndroidJUnit4::class)
class V2CollectionTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun playlistDetail_rendersRowsAndPlayAll() {
        var played = false
        val songs = listOf(fakeSong(1L, "T1"), fakeSong(2L, "T2"))
        rule.setContent { V2Theme { PlaylistDetailContent(title = "P", songs = songs, onPlayAll = { played = true }, onPlay = {}, onMenu = {}) } }
        rule.onNodeWithTag(V2TestTags.PLAY_ALL_BUTTON).performClick()
        assertTrue(played)
        rule.onNodeWithText("T1").assertIsDisplayed()
        rule.onNodeWithText("T2").assertIsDisplayed()
    }

    @Test fun collectionDetail_reusesContent() {
        val songs = listOf(fakeSong(9L, "C1"))
        rule.setContent { V2Theme { PlaylistDetailContent(title = "Album", songs = songs, onPlayAll = {}, onPlay = {}, onMenu = {}) } }
        rule.onNodeWithText("C1").assertIsDisplayed()
    }
}
