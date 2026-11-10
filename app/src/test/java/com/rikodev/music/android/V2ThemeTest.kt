package com.rikodev.music.android

import androidx.compose.ui.graphics.Color
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2
import org.junit.Assert.assertEquals
import org.junit.Test

class V2ThemeTest {
    @Test fun background_isDeepPurpleBlack() {
        assertEquals(Color(0xFF0D0716), RikoThemeV2.Colors.background)
    }

    @Test fun secondaryText_isNeverDimmerThanAA() {
        val c = RikoThemeV2.Colors.onBackgroundSecondary
        assertEquals(Color(0xFFA79BC7), c)
    }

    @Test fun ctaGradient_hasThreeStops() {
        assertEquals(3, RikoThemeV2.Colors.ctaGradient.size)
    }
}
