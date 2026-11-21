package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Screen foundation — kept transparent so the live blurred GPU Liquid Silk
 * background (`RikoFluidBackground`) shines seamlessly behind every screen
 * in both Emerald Dark and Emerald Light modes.
 */
fun Modifier.rikoScreenFoundation(depth: Float = 0.78f): Modifier {
    @Suppress("UNUSED_VARIABLE")
    val unusedDepth = depth
    return this.background(Color.Transparent)
}
