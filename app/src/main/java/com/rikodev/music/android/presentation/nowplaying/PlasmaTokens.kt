package com.rikodev.music.android.presentation.nowplaying

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PlasmaTokens {
    // Card [M]
    const val RefCardWidthDp = 360f
    const val CardAspect = 2.38f
    val CardRadius = 34.dp

    // Row centers as a fraction of card height [M]
    const val HeaderY = 0.16f
    const val TitleY = 0.33f
    const val BaselineY = 0.505f
    const val TimeY = 0.61f
    const val ControlsY = 0.795f

    // Timeline [M]
    val BarInset = 28.dp
    val Stroke = 3.25.dp
    const val InactiveAlpha = 0.42f
    val HumpMax = 12.5.dp
    const val HumpStartU = 0.07f
    const val HumpEndOfProgress = 0.85f
    const val HumpPeakU = 0.28f

    val ThumbOuter = 16.5.dp
    val ThumbRing = 2.5.dp
    val ThumbGlow = 26.dp

    val CanvasHeight = 64.dp
    val TouchTargetHeight = 48.dp

    // Controls [M]
    val Disc = 42.dp
    val SideGlyph = 18.dp
    val ControlGap = 20.dp
    val ControlsSidePad = 58.dp
    const val PrevNextDiscAlpha = 0.12f
    const val PlayDiscAlpha = 0.07f

    // Type [M/A]
    val Label = 12.sp
    val Title = 14.sp
    val Time = 11.sp

    // Motion [R/A]
    const val GrowMs = 800
    const val FlattenMs = 550
    const val IdleAmp = 0.25f
    const val IdleSpeed = 0.2f
    const val PlaySpeed = 1.0f

    // State machine [A]
    const val ScrubAmp = 0.10f
    const val BufferAmp = 0.15f
    const val ScrubSpeed = 0.2f
    const val BufferSpeed = 0.5f

    // Master-prompt §9: Reduced Motion & Battery Saver
    const val ReducedMotionAmp = 0.25f
    const val ReducedMotionSpeed = 0.0f
    const val BatterySaverMaxAmp = 0.60f
    const val BatterySaverSpeed = 0.50f
    const val BatterySaverPlasma = 0.60f
}
