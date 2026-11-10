package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.components.RikoBottomNavigation
import com.rikodev.music.android.core.designsystem.riko.components.RikoNavItem
import com.rikodev.music.android.core.designsystem.riko.components.RikoTopBar
import com.rikodev.music.android.core.designsystem.riko.components.RikoTopBarStyle
import com.rikodev.music.android.core.designsystem.riko.components.V2TestTags
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import com.rikodev.music.android.presentation.components.GlassBottomSheetContainer
import com.rikodev.music.android.presentation.player.PlayPauseHeroButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Rewritten from V2ChromeTest (§89): deleted V2 chrome (V2TopBar, V2BottomNav,
// V2MiniPlayer, V2SheetContainer) replaced by production equivalents —
// RikoTopBar, RikoBottomNavigation, PlayPauseHeroButton, GlassBottomSheetContainer.
@RunWith(AndroidJUnit4::class)
class RikoChromeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun topBar_backClicksAndTitleShows() {
        var back = 0
        rule.setContent {
            V2Theme {
                RikoTopBar(title = "Library", style = RikoTopBarStyle.Back, onBack = { back++ })
            }
        }
        rule.onNodeWithText("Library").assertIsDisplayed()
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, back)
    }

    @Test fun bottomNav_selectsLibrary() {
        var selected = 0
        rule.setContent {
            V2Theme {
                RikoBottomNavigation(
                    items = listOf(
                        RikoNavItem(icon = Icons.Filled.Home, label = "Home"),
                        RikoNavItem(icon = Icons.Filled.Search, label = "Search"),
                        RikoNavItem(icon = Icons.Filled.LibraryMusic, label = "Library")
                    ),
                    selectedIndex = selected,
                    onItemSelected = { selected = it }
                )
            }
        }
        rule.onNodeWithContentDescription("Library").performClick()
        assertEquals(2, selected)
    }

    @Test fun heroPlay_togglesClicks() {
        var toggled = false
        rule.setContent {
            V2Theme {
                PlayPauseHeroButton(
                    isPlaying = false,
                    energy = 0.4f,
                    hasBeat = false,
                    onClick = { toggled = true }
                )
            }
        }
        rule.onNodeWithTag(V2TestTags.NP_PLAY_PAUSE).performClick()
        assertTrue(toggled)
    }

    @Test fun sheetContainer_rendersTitle() {
        rule.setContent {
            V2Theme {
                GlassBottomSheetContainer(accentColor = Color.Cyan) {
                    Text("Queue")
                    Text("body")
                }
            }
        }
        rule.onNodeWithText("Queue").assertIsDisplayed()
        rule.onNodeWithText("body").assertIsDisplayed()
    }
}
