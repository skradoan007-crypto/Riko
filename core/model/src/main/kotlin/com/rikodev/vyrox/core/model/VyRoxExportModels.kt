package com.rikodev.vyrox.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ExportResolution(val label: String, val targetHeight: Int?) {
    HD_720P("720p HD", 720),
    FHD_1080P("1080p Full HD", 1080),
    ORIGINAL("Original", null)
}

@Serializable
enum class ExportVideoCodec(val label: String, val mimeType: String) {
    H264("H.264 (Max Compatibility)", "video/avc"),
    HEVC("H.265 HEVC (Smaller File)", "video/hevc")
}

@Serializable
data class ExportConfig(
    val outputName: String = "VyRox_Export",
    val resolution: ExportResolution = ExportResolution.FHD_1080P,
    val codec: ExportVideoCodec = ExportVideoCodec.H264
)

@Serializable
data class ColorPreset(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean,
    val grade: ColorGradeState
)

data class ImportedMediaMetadata(
    val contentUri: String,
    val displayName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotationDeg: Int,
    val thumbnailPath: String?
)
