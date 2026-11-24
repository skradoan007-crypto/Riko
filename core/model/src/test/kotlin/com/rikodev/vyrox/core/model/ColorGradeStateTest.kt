package com.rikodev.vyrox.core.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class ColorGradeStateTest {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test
    fun `default color grade is neutral`() {
        val grade = ColorGradeState()
        assertThat(grade.isNeutral()).isTrue()
    }

    @Test
    fun `project document survives json serialization round-trip`() {
        val original = ProjectDocument(
            projectId = "proj-001",
            title = "Travel Vlog",
            aspectRatio = AspectRatioPreset.LANDSCAPE_16_9,
            clips = listOf(
                VideoClip(
                    id = "clip-1",
                    mediaAssetId = "asset-1",
                    timelineStartMs = 0L,
                    sourceTrimStartMs = 1_000L,
                    sourceTrimEndMs = 11_000L,
                    colorGrade = ColorGradeState(exposure = 0.45f, contrast = 0.2f, vibrance = 0.3f)
                )
            ),
            updatedAtEpochMs = 1_764_000_000_000L
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<ProjectDocument>(encoded)

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.totalDurationMs).isEqualTo(10_000L)
        assertThat(decoded.clips.first().colorGrade.isNeutral()).isFalse()
    }
}
