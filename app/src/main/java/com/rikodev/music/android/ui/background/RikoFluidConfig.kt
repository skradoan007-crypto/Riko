package com.rikodev.music.android.ui.background

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class FluidThemeMode {
    DARK,
    LIGHT
}

@Immutable
data class AlbumPigmentState(
    val trackKey: String? = null,
    val primary: Color = Color.Transparent,
    val secondary: Color = Color.Transparent,
    val hasPigment: Boolean = false
)

@Immutable
data class RikoFluidState(
    val themeMode: FluidThemeMode = FluidThemeMode.DARK,
    val pigment: AlbumPigmentState = AlbumPigmentState(),
    val isPlaying: Boolean = false,
    val audioEnergy: Float = 0f,
    val touchImpulseToken: Int = 0,
    val touchNormX: Float = 0.5f,
    val touchNormY: Float = 0.85f
)

/**
 * Configuration constants for RIKO LIQUID SILK & PIGMENT ENGINE v4
 * (Curled 3D Optical Silk + Hue-Preserving Filmic Shading + Cream-and-Dye Marbling).
 */
internal object RikoFluidPalettes {
    // Dark Mode — Obsidian-Teal Void -> Emerald Organza -> Jade-Seafoam -> #C2FBDF Crest
    val darkVoidDeep = Color(0xFF030A0C)
    val darkVoidMid = Color(0xFF091B1C)
    val darkSilkBack = Color(0xFF0B3D34)
    val darkSilkMid = Color(0xFF186B53)
    val darkSilkFront = Color(0xFF3CB586)
    val darkSilkRim = Color(0xFFC2FBDF)

    // Light Mode — Deep Marine-Teal Corners -> Aqua-Glass -> Pearl-Cream -> #FBFFF9 Crest
    val lightCornerDeep = Color(0xFF0D2A2F)
    val lightCornerMid = Color(0xFF1D4F52)
    val lightAquaMid = Color(0xFF458A86)
    val lightAquaBright = Color(0xFF82C2B3)
    val lightPearlSoft = Color(0xFFD7F2DF)
    val lightPearlBright = Color(0xFFF4FCF1)
    val lightSilkRim = Color(0xFFFBFFF9)

    // v4 uPigmentAmount range: 0.0 (Blank) -> 0.48 (Playing) over ~3.0s
    const val MAX_PIGMENT_STRENGTH_DARK = 0.48f
    const val MAX_PIGMENT_STRENGTH_LIGHT = 0.48f

    // Internal GPU resolution scale (0.52^2 = 27.0% pixels -> ~73% fewer pixels than 1080p, razor-crisp edges)
    const val INTERNAL_RESOLUTION_SCALE = 0.52f
}
