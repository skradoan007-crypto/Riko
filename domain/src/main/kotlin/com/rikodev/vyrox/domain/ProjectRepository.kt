package com.rikodev.vyrox.domain

import com.rikodev.vyrox.core.model.AspectRatioPreset
import com.rikodev.vyrox.core.model.ColorGradeState
import com.rikodev.vyrox.core.model.ColorPreset
import com.rikodev.vyrox.core.model.ImportedMediaMetadata
import com.rikodev.vyrox.core.model.ProjectDocument
import com.rikodev.vyrox.core.model.ProjectSummary
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeRecentProjects(): Flow<List<ProjectSummary>>
    suspend fun createProject(title: String, aspectRatio: AspectRatioPreset): ProjectDocument
    suspend fun createProjectFromVideo(media: ImportedMediaMetadata): ProjectDocument
    suspend fun resolveAssetUri(assetId: String): String?
    suspend fun loadProject(projectId: String): ProjectDocument?
    suspend fun saveProject(document: ProjectDocument)
    suspend fun deleteProject(projectId: String)
}

interface ColorPresetRepository {
    fun observePresets(): Flow<List<ColorPreset>>
    suspend fun saveCustomPreset(name: String, grade: ColorGradeState): ColorPreset
    suspend fun deleteCustomPreset(presetId: String)
}
