package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2SmokeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun staticText_isDisplayed() {
        rule.setContent { Text("RIKO smoke") }
        rule.onNodeWithText("RIKO smoke").assertExists()
    }
}
