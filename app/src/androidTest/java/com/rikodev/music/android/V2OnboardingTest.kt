package com.rikodev.music.android

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import com.rikodev.music.android.presentation.onboarding.OnboardingScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2OnboardingTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun onboarding_showsHeadlineAndCtas() {
        var started = false
        rule.setContent { V2Theme { OnboardingScreen(onGetStarted = { started = true }, onContinueWithGoogle = {}) } }
        rule.onNodeWithText("Together").assertIsDisplayed()
        rule.onNodeWithText("Create Account").performClick()
        assertTrue(started)
    }
}
