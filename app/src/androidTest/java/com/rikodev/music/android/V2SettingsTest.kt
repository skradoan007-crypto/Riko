package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import com.rikodev.music.android.presentation.settings_v2.rendering.V2SettingsHeader
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2SettingsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun settings_rendersV2Title() {
        rule.setContent { V2Theme { V2SettingsHeader(title = "Settings") } }
        rule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
