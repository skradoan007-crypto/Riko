package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.components.RikoPrimaryButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoSecondaryButton
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// D-9 migration: was V2ButtonsTest — V2Buttons retired, canonical Riko
// buttons carry the coverage now (social row deleted with the old component).
@RunWith(AndroidJUnit4::class)
class RikoButtonsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun primaryButton_rendersLabelAndClicks() {
        var clicked = false
        rule.setContent { V2Theme { RikoPrimaryButton(text = "Play All", onClick = { clicked = true }) } }
        rule.onNodeWithText("Play All").performClick()
        assertTrue(clicked)
    }

    @Test fun secondaryButton_rendersLabel() {
        rule.setContent { V2Theme { RikoSecondaryButton(text = "Login", onClick = {}) } }
        rule.onNodeWithText("Login").assertIsDisplayed()
    }
}
