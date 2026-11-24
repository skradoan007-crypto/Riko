package com.rikodev.vyrox.data

import com.rikodev.vyrox.core.db.ColorPresetDao
import com.rikodev.vyrox.core.db.ColorPresetEntity
import com.rikodev.vyrox.core.db.MediaAssetDao
import com.rikodev.vyrox.core.db.MediaAssetEntity
import com.rikodev.vyrox.core.db.ProjectDao
import com.rikodev.vyrox.core.db.ProjectEntity
import com.rikodev.vyrox.core.model.AspectRatioPreset
import com.rikodev.vyrox.core.model.ColorGradeState
import com.rikodev.vyrox.core.model.ColorPreset
import com.rikodev.vyrox.core.model.ImportedMediaMetadata
import com.rikodev.vyrox.core.model.ProjectDocument
import com.rikodev.vyrox.core.model.ProjectSummary
import com.rikodev.vyrox.core.model.VideoClip
import com.rikodev.vyrox.core.storage.VyRoxStorageManager
import com.rikodev.vyrox.domain.ColorPresetRepository
import com.rikodev.vyrox.domain.ProjectRepository
import com.rikodev.vyrox.graphics.gl.BuiltInColorPresets
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class ProjectSerializer @Inject constructor() {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    fun encode(document: ProjectDocument): String = json.encodeToString(document)
    fun decode(raw: String): ProjectDocument = json.decodeFromString(raw)
    fun encodeGrade(grade: ColorGradeState): String = json.encodeToString(grade)
    fun decodeGrade(raw: String): ColorGradeState = json.decodeFromString(raw)
}

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val mediaAssetDao: MediaAssetDao,
    private val storage: VyRoxStorageManager,
    private val serializer: ProjectSerializer
) : ProjectRepository {

    override fun observeRecentProjects(): Flow<List<ProjectSummary>> =
        projectDao.observeAll().map { entities ->
            entities.map { entity ->
                ProjectSummary(
                    projectId = entity.projectId,
                    title = entity.title,
                    durationMs = entity.durationMs,
                    thumbnailPath = entity.thumbPath,
                    aspectRatio = runCatching { AspectRatioPreset.valueOf(entity.aspectRatio) }
                        .getOrDefault(AspectRatioPreset.PORTRAIT_9_16),
                    updatedAtEpochMs = entity.updatedAt
                )
            }
        }

    override suspend fun createProject(
        title: String,
        aspectRatio: AspectRatioPreset
    ): ProjectDocument {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val doc = ProjectDocument(
            projectId = id,
            title = title.ifBlank { "Untitled Project" },
            aspectRatio = aspectRatio,
            updatedAtEpochMs = now
        )
        saveProjectInternal(doc, thumbPath = null)
        return doc
    }

    override suspend fun createProjectFromVideo(media: ImportedMediaMetadata): ProjectDocument {
        val now = System.currentTimeMillis()
        val existingAsset = mediaAssetDao.getByUri(media.contentUri)
        val assetId = existingAsset?.assetId ?: UUID.randomUUID().toString()

        mediaAssetDao.upsert(
            MediaAssetEntity(
                assetId = assetId,
                contentUri = media.contentUri,
                mediaType = "VIDEO",
                durationMs = media.durationMs,
                width = media.width,
                height = media.height,
                proxyPath = existingAsset?.proxyPath,
                isAvailable = true
            )
        )

        val aspect = when {
            media.width > media.height -> AspectRatioPreset.LANDSCAPE_16_9
            media.width == media.height -> AspectRatioPreset.SQUARE_1_1
            else -> AspectRatioPreset.PORTRAIT_9_16
        }

        val clip = VideoClip(
            id = UUID.randomUUID().toString(),
            mediaAssetId = assetId,
            timelineStartMs = 0L,
            sourceDurationMs = media.durationMs,
            sourceTrimStartMs = 0L,
            sourceTrimEndMs = media.durationMs
        )

        val doc = ProjectDocument(
            projectId = UUID.randomUUID().toString(),
            title = media.displayName.ifBlank { "VyRox Edit" },
            aspectRatio = aspect,
            clips = listOf(clip),
            updatedAtEpochMs = now
        )
        saveProjectInternal(doc, thumbPath = media.thumbnailPath)
        return doc
    }

    override suspend fun resolveAssetUri(assetId: String): String? =
        mediaAssetDao.getById(assetId)?.contentUri

    override suspend fun loadProject(projectId: String): ProjectDocument? = withContext(Dispatchers.IO) {
        val file = storage.projectDocumentFile(projectId)
        if (!file.exists()) return@withContext null
        runCatching { serializer.decode(file.readText(Charsets.UTF_8)) }.getOrNull()
    }

    override suspend fun saveProject(document: ProjectDocument) {
        val existing = projectDao.getById(document.projectId)
        saveProjectInternal(document, thumbPath = existing?.thumbPath)
    }

    private suspend fun saveProjectInternal(document: ProjectDocument, thumbPath: String?) {
        val now = System.currentTimeMillis()
        val updated = document.copy(updatedAtEpochMs = now)
        val file = storage.projectDocumentFile(updated.projectId)
        storage.writeAtomicText(file, serializer.encode(updated))

        val existing = projectDao.getById(updated.projectId)
        projectDao.upsert(
            ProjectEntity(
                projectId = updated.projectId,
                title = updated.title,
                schemaVersion = updated.schemaVersion,
                docFilePath = file.absolutePath,
                thumbPath = thumbPath ?: existing?.thumbPath,
                aspectRatio = updated.aspectRatio.name,
                durationMs = updated.totalDurationMs,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
    }

    override suspend fun deleteProject(projectId: String) {
        projectDao.deleteById(projectId)
        storage.deleteProjectFiles(projectId)
    }
}

@Singleton
class ColorPresetRepositoryImpl @Inject constructor(
    private val presetDao: ColorPresetDao,
    private val serializer: ProjectSerializer
) : ColorPresetRepository {

    override fun observePresets(): Flow<List<ColorPreset>> =
        presetDao.observeAll().map { saved ->
            val custom = saved.mapNotNull { entity ->
                runCatching {
                    ColorPreset(
                        id = entity.presetId,
                        name = entity.name,
                        isBuiltIn = false,
                        grade = serializer.decodeGrade(entity.payloadJson)
                    )
                }.getOrNull()
            }
            BuiltInColorPresets.all + custom
        }

    override suspend fun saveCustomPreset(name: String, grade: ColorGradeState): ColorPreset {
        val id = UUID.randomUUID().toString()
        val cleanName = name.trim().ifBlank { "Custom Look" }
        presetDao.upsert(
            ColorPresetEntity(
                presetId = id,
                name = cleanName,
                schemaVersion = ProjectDocument.CURRENT_SCHEMA_VERSION,
                isBuiltin = false,
                payloadJson = serializer.encodeGrade(grade),
                updatedAt = System.currentTimeMillis()
            )
        )
        return ColorPreset(id = id, name = cleanName, isBuiltIn = false, grade = grade)
    }

    override suspend fun deleteCustomPreset(presetId: String) {
        presetDao.deleteCustom(presetId)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindColorPresetRepository(impl: ColorPresetRepositoryImpl): ColorPresetRepository
}
