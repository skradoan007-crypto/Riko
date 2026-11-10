package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.components.RikoPlaylistCard
import com.rikodev.music.android.core.designsystem.riko.components.RikoSectionHeader
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// D-9 migration: was V2CardsTest — V2Cards retired; canonical RikoPlaylistCard
// + RikoSectionHeader carry the coverage (V2BannerCard died with V2Cards; the
// canonical RikoPromoBannerCard is the banner going forward).
@RunWith(AndroidJUnit4::class)
class RikoCardsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun sectionHeader_actionClicks() {
        var seen = false
        rule.setContent {
            V2Theme {
                RikoSectionHeader(title = "Recently Played", actionText = "See All", onAction = { seen = true })
            }
        }
        rule.onNodeWithText("Recently Played").assertIsDisplayed()
        rule.onNodeWithText("See All").performClick()
        assertTrue(seen)
    }

    @Test fun playlistCard_rendersTitleAndSubtitle() {
        rule.setContent {
            V2Theme {
                RikoPlaylistCard(
                    title = "Chill Beats",
                    subtitle = "Playlist • 42 songs",
                    artworkModel = null,
                    onClick = {},
                    onPlay = {}
                )
            }
        }
        rule.onNodeWithText("Chill Beats").assertIsDisplayed()
        rule.onNodeWithText("Playlist • 42 songs").assertIsDisplayed()
    }
}
