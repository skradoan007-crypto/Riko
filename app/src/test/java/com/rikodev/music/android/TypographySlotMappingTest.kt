package com.rikodev.music.android

import com.rikodev.music.android.core.designsystem.riko.theme.RikoMaterialTypography
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 5 Step 0 / G-1 + G-16: every Material3 typography slot resolves to a
 * canonical [RikoTypography] role — one mapping, one source. Theme entries
 * (`RikoMusicTheme`, `RikoTheme`) both consume [RikoMaterialTypography], so
 * `MaterialTheme.typography.X` can never drift from `RikoTypography.Y`.
 */
class TypographySlotMappingTest {

    @Test fun displaySlots_useDisplayAndLargeTitle() {
        assertEquals(RikoTypography.Display, RikoMaterialTypography.displayLarge)
        assertEquals(RikoTypography.Display, RikoMaterialTypography.displayMedium)
        assertEquals(RikoTypography.LargeTitle, RikoMaterialTypography.displaySmall)
    }

    @Test fun headlineSlots_useLargeTitleAndTitle() {
        assertEquals(RikoTypography.LargeTitle, RikoMaterialTypography.headlineLarge)
        assertEquals(RikoTypography.LargeTitle, RikoMaterialTypography.headlineMedium)
        assertEquals(RikoTypography.Title, RikoMaterialTypography.headlineSmall)
    }

    @Test fun titleSlots_useTitleSectionBody() {
        assertEquals(RikoTypography.Title, RikoMaterialTypography.titleLarge)
        assertEquals(RikoTypography.Section, RikoMaterialTypography.titleMedium)
        assertEquals(RikoTypography.Body, RikoMaterialTypography.titleSmall)
    }

    @Test fun bodySlots_useBodySecondaryCaption() {
        assertEquals(RikoTypography.Body, RikoMaterialTypography.bodyLarge)
        assertEquals(RikoTypography.Secondary, RikoMaterialTypography.bodyMedium)
        assertEquals(RikoTypography.Caption, RikoMaterialTypography.bodySmall)
    }

    @Test fun labelSlots_useLabelAndCaption() {
        assertEquals(RikoTypography.Label, RikoMaterialTypography.labelLarge)
        assertEquals(RikoTypography.Label, RikoMaterialTypography.labelMedium)
        assertEquals(RikoTypography.Caption, RikoMaterialTypography.labelSmall)
    }

    @Test fun roles_carryTheBalooBrandFamily() {
        // The retired legacy scale (theme/Type.kt) used FontFamily.Default;
        // every canonical role must ship the bundled Baloo 2 family instead.
        assertEquals(RikoFontFamilyProbe, RikoTypography.Body.fontFamily)
    }

    private val RikoFontFamilyProbe =
        com.rikodev.music.android.core.designsystem.riko.theme.RikoFontFamily
}
