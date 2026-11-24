package com.rikodev.vyrox.graphics.gl

import com.rikodev.vyrox.core.model.ColorGradeState
import com.rikodev.vyrox.core.model.ColorPreset

object BuiltInColorPresets {
    val all: List<ColorPreset> = listOf(
        ColorPreset(
            id = "builtin_neutral",
            name = "Original",
            isBuiltIn = true,
            grade = ColorGradeState()
        ),
        ColorPreset(
            id = "builtin_cinematic_teal",
            name = "Cine Teal",
            isBuiltIn = true,
            grade = ColorGradeState(
                contrast = 0.22f,
                warmth = -0.14f,
                tint = 0.08f,
                shadows = 0.12f,
                highlights = -0.15f,
                vibrance = 0.20f,
                vignette = 0.25f
            )
        ),
        ColorPreset(
            id = "builtin_warm_film",
            name = "Warm Film",
            isBuiltIn = true,
            grade = ColorGradeState(
                exposure = 0.10f,
                contrast = 0.14f,
                warmth = 0.28f,
                fade = 0.18f,
                grain = 0.22f,
                vignette = 0.18f
            )
        ),
        ColorPreset(
            id = "builtin_neon_night",
            name = "Neon Night",
            isBuiltIn = true,
            grade = ColorGradeState(
                exposure = 0.15f,
                contrast = 0.30f,
                warmth = -0.18f,
                tint = -0.14f,
                saturation = 0.24f,
                sharpen = 0.22f
            )
        ),
        ColorPreset(
            id = "builtin_obsidian_bw",
            name = "Obsidian B&W",
            isBuiltIn = true,
            grade = ColorGradeState(
                contrast = 0.38f,
                saturation = -1.0f,
                shadows = -0.12f,
                highlights = 0.15f,
                grain = 0.25f,
                vignette = 0.30f
            )
        ),
        ColorPreset(
            id = "builtin_crisp_day",
            name = "Crisp Day",
            isBuiltIn = true,
            grade = ColorGradeState(
                exposure = 0.12f,
                contrast = 0.12f,
                vibrance = 0.28f,
                sharpen = 0.25f,
                shadows = 0.18f
            )
        )
    )
}
