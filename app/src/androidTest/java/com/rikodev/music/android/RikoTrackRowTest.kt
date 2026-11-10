package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.components.RikoTrackRow
import com.rikodev.music.android.core.designsystem.riko.components.V2TestTags
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Rewritten from V2TrackRowTest (§89): production track row is RikoTrackRow —
// same V2TestTags.TRACK_ROW_PREFIX contract, artwork now via `artworkModel`.
@RunWith(AndroidJUnit4::class)
class RikoTrackRowTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun trackRow_rendersAllFieldsAndClicks() {
        var clicked = false
        var menu = false
        rule.setContent {
            V2Theme {
                RikoTrackRow(id = 7L, title = "Usay Toofan Keht", subtitle = "Prince Lovers \u2022 Topic", duration = "4:14", artworkModel = null, isPlaying = true, onClick = { clicked = true }, onMenu = { menu = true })
            }
        }
        rule.onNodeWithTag(V2TestTags.TRACK_ROW_PREFIX + 7L).assertIsDisplayed()
        rule.onNodeWithText("Usay Toofan Keht").assertIsDisplayed()
        rule.onNodeWithText("4:14").assertIsDisplayed()
        rule.onNodeWithText("Usay Toofan Keht").performClick()
        assertTrue(clicked)
    }
}
