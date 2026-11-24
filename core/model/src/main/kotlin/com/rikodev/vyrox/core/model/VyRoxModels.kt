package com.rikodev.vyrox.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class AspectRatioPreset(val label: String, val widthRatio: Int, val heightRatio: Int) {
    PORTRAIT_9_16("9:16", 9, 16),
    LANDSCAPE_16_9("16:9", 16, 9),
    SQUARE_1_1("1:1", 1, 1),
    SOCIAL_4_5("4:5", 4, 5),
    CLASSIC_4_3("4:3", 4, 3),
    CINEMA_21_9("21:9", 21, 9),
    CUSTOM("Custom", 16, 9)
}

@Serializable
data class ColorGradeState(
    val enabled: Boolean = true,
    val brightness: Float = 0f,
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val saturation: Float = 0f,
    val vibrance: Float = 0f,
    val fade: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val sharpen: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val lutAssetId: String? = null,
    val lutIntensity: Float = 1f,
    val masterCurvePoints: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
) {
    fun isNeutral(): Boolean =
        brightness == 0f && exposure == 0f && contrast == 0f &&
        warmth == 0f && tint == 0f && saturation == 0f &&
        vibrance == 0f && fade == 0f && highlights == 0f &&
        shadows == 0f && sharpen == 0f && vignette == 0f &&
        grain == 0f && lutAssetId == null
}

@Serializable
data class CurvePoint(val x: Float, val y: Float)

@Serializable
data class NormalizedRect(
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val width: Float = 0.35f,
    val height: Float = 0.35f
)

@Serializable
data class PipLayer(
    val id: String,
    val mediaAssetId: String,
    val startMs: Long,
    val endMs: Long,
    val bounds: NormalizedRect = NormalizedRect(),
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f,
    val cornerRadiusNorm: Float = 0.04f,
    val borderWidthDp: Float = 0f,
    val zOrder: Int = 1,
    val muted: Boolean = false
)

@Serializable
data class VideoClip(
    val id: String,
    val mediaAssetId: String,
    val timelineStartMs: Long,
    val sourceDurationMs: Long = 0L,
    val sourceTrimStartMs: Long,
    val sourceTrimEndMs: Long,
    val speed: Float = 1f,
    val volume: Float = 1f,
    val muted: Boolean = false,
    val colorGrade: ColorGradeState = ColorGradeState()
) {
    val effectiveSourceDurationMs: Long
        get() = maxOf(sourceDurationMs, sourceTrimEndMs, 1_000L)

    val durationMs: Long
        get() = ((sourceTrimEndMs - sourceTrimStartMs).coerceAtLeast(250L) / speed.coerceIn(0.25f, 4f)).toLong()
}

@Serializable
data class ProjectDocument(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val projectId: String,
    val title: String,
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.PORTRAIT_9_16,
    val targetFps: Int = 30,
    val clips: List<VideoClip> = emptyList(),
    val pipLayers: List<PipLayer> = emptyList(),
    val updatedAtEpochMs: Long = 0L
) {
    val totalDurationMs: Long
        get() = clips.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

data class ProjectSummary(
    val projectId: String,
    val title: String,
    val durationMs: Long,
    val thumbnailPath: String?,
    val aspectRatio: AspectRatioPreset,
    val updatedAtEpochMs: Long
)
