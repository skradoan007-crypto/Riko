package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.presentation.debug.WaveLaboratoryScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 4 / §107 back-target coverage (gate: navigation instrumented test).
 *
 * - WaveLaboratoryScreen: header back arrow AND system back both route to the
 *   same `onBack` target (wired to Settings when the D-14 hidden entry lands
 *   in Phase 7 F).
 * - The full in-app back chain (visualizer overlay → NP full → NP mini →
 *   route → system, §22) needs a real device pass — reported as [V] with the
 *   phase, since androidTest never compiles in CI.
 */
@RunWith(AndroidJUnit4::class)
class RikoBackChainTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun lab_headerBack_invokesOnBack() {
        var back = 0
        rule.setContent { WaveLaboratoryScreen(onBack = { back++ }) }
        rule.onNodeWithContentDescription("Back").assertIsDisplayed()
        rule.onNodeWithContentDescription("Back").performClick()
        assertTrue(back == 1)
    }

    @Test fun lab_systemBack_invokesOnBack() {
        var back = 0
        rule.setContent { WaveLaboratoryScreen(onBack = { back++ }) }
        Espresso.pressBack()
        assertTrue(back == 1)
    }
}
