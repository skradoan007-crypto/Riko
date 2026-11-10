package com.rikodev.music.android.core.designsystem.riko.theme

import androidx.compose.ui.graphics.Color

/** Owner-selected white → middle → outer combinations for the liquid timeline. */
object RikoTimelineColors {
    enum class Combo(val middle: Color, val outer: Color) {
        PURPLE_BLUE(Color(0xFF8000FF), Color(0xFF0061FF)),
        PINK_RED(Color(0xFFFF4D9D), Color(0xFFFF1744)),
        YELLOW_MAGENTA(Color(0xFFFFE600), Color(0xFFFF00FF)),
        LIGHT_DARK_RED(Color(0xFFFF6B6B), Color(0xFFB71C1C)),
        CYBER_SPARK(Color(0xFFFF007F), Color(0xFF7B2CBF)),
        ICE_NEON(Color(0xFF00F5D4), Color(0xFF0061FF)),
        SOLAR_FLARE(Color(0xFFFF5400), Color(0xFFFFE600)),
        ACID_MINT(Color(0xFF39FF14), Color(0xFF00B0FF)),
        VAPORWAVE_POP(Color(0xFF8000FF), Color(0xFFE6004C)),
        SUNSET_GLOW(Color(0xFFFF7A00), Color(0xFFD500F9)),
        NEUTRAL(Color(0xFFBFC6CF), Color(0xFF69727F));

        val core: Color get() = Color.White
    }
}
