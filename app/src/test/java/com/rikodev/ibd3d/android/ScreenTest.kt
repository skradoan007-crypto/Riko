package com.rikodev.ibd3d.android

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.rikodev.ibd3d.android.ui.screens.*
import com.rikodev.ibd3d.android.updates.UpdateState
import com.rikodev.ibd3d.core.data.PanelRepository
import com.rikodev.ibd3d.core.data.UserPreferences
import com.rikodev.ibd3d.core.designsystem.PanelTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w360dp-h800dp")
class ScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun vehicleShortcut() {
        var chosen = ""
        var storeOpened = false
        compose.setContent {
            PanelTheme("Purple") {
                Surface(Modifier.fillMaxSize()) {
                    HomeScreen(
                        null,
                        UserPreferences(),
                        { chosen = it },
                        {},
                        {},
                        {},
                        { true },
                        {},
                        {},
                        null,
                        {},
                        onGetGame = { storeOpened = true },
                    )
                }
            }
        }
        compose.onNodeWithText("Vehicles").performClick()
        assertEquals("Vehicles", chosen)
        compose.onNodeWithText("Get the Game").performClick()
        assertTrue(storeOpened)
    }

    @Test
    fun settingsCheckUpdates() {
        var checked = false
        compose.setContent {
            PanelTheme("Purple") {
                Surface(Modifier.fillMaxSize()) {
                    SettingsScreen(
                        UserPreferences(),
                        UpdateState(),
                        { checked = true },
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                    )
                }
            }
        }
        compose.onNodeWithText("Check for Update").performClick()
        assertTrue(checked)
    }

    @Test
    fun pluginsIsComingSoon() {
        compose.setContent {
            PanelTheme("Purple") { Surface(Modifier.fillMaxSize()) { PluginsScreen() } }
        }
        compose.onNodeWithText("Coming Soon").assertIsDisplayed()
        compose.onAllNodesWithText("Install").assertCountEquals(0)
    }

    @Test
    fun copyLeadingZeroAndFavoriteById() {
        val data = runBlocking { PanelRepository(RuntimeEnvironment.getApplication()).load() }
        var copied = ""
        var favorite = ""
        compose.setContent {
            PanelTheme("Purple") {
                Surface(Modifier.fillMaxSize()) {
                    CodesScreen(
                        data,
                        UserPreferences(),
                        "0701",
                        "All",
                        {},
                        {},
                        {
                            copied = it.code
                            true
                        },
                        { favorite = it },
                        {},
                        null,
                        {},
                    )
                }
            }
        }
        val item = data.items.first { it.code == "0701" }
        compose.onNodeWithContentDescription("Copy ${item.name} code ${item.code}").performClick()
        assertEquals("0701", copied)
        compose.onNodeWithContentDescription("Favorite ${item.name}").performClick()
        assertEquals(item.id, favorite)
    }

    @Test
    fun duplicateCodesBothVisible() {
        val data = runBlocking { PanelRepository(RuntimeEnvironment.getApplication()).load() }
        compose.setContent {
            PanelTheme("Purple") {
                Surface(Modifier.fillMaxSize()) {
                    CodesScreen(
                        data,
                        UserPreferences(),
                        "6666",
                        "All",
                        {},
                        {},
                        { true },
                        {},
                        {},
                        null,
                        {},
                    )
                }
            }
        }
        compose.onAllNodesWithText("6666").assertCountEquals(2)
    }
}
