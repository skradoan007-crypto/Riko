package com.rikodev.music.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.core.designsystem.riko.components.V2TestTags
import com.rikodev.music.android.core.designsystem.riko.theme.V2Theme
import com.rikodev.music.android.presentation.search.V2SearchBar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2SearchLibraryTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun searchBar_hasTestTagAndHint() {
        rule.setContent { V2Theme { V2SearchBar(query = "", onQuery = {}) } }
        rule.onNodeWithTag(V2TestTags.SEARCH_FIELD).assertIsDisplayed()
    }
}
