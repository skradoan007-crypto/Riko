package com.rikodev.vyrox.android

import com.rikodev.vyrox.core.model.ColorGradeState
import com.rikodev.vyrox.core.model.ProjectDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VyRoxAppSanityTest {
    @Test
    fun defaultProjectSchemaAndColorStateAreValid() {
        val doc = ProjectDocument(projectId = "p1", title = "VyRox")
        assertEquals(ProjectDocument.CURRENT_SCHEMA_VERSION, doc.schemaVersion)
        assertTrue(ColorGradeState().isNeutral())
    }
}
