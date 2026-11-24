package com.rikodev.vyrox.data

import com.google.common.truth.Truth.assertThat
import com.rikodev.vyrox.core.model.AspectRatioPreset
import com.rikodev.vyrox.core.model.ProjectDocument
import org.junit.Test

class ProjectSerializerTest {
    @Test
    fun `serializer preserves schema version and aspect ratio`() {
        val serializer = ProjectSerializer()
        val doc = ProjectDocument(
            projectId = "vx-100",
            title = "Nature Edit",
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            targetFps = 60,
            updatedAtEpochMs = 123456789L
        )
        val decoded = serializer.decode(serializer.encode(doc))
        assertThat(decoded.schemaVersion).isEqualTo(ProjectDocument.CURRENT_SCHEMA_VERSION)
        assertThat(decoded.targetFps).isEqualTo(60)
        assertThat(decoded.title).isEqualTo("Nature Edit")
    }
}
