package com.rikodev.vyrox.core.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [Index(value = ["updated_at"])]
)
data class ProjectEntity(
    @PrimaryKey @ColumnInfo(name = "project_id") val projectId: String,
    val title: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
    @ColumnInfo(name = "doc_file_path") val docFilePath: String,
    @ColumnInfo(name = "thumb_path") val thumbPath: String?,
    @ColumnInfo(name = "aspect_ratio") val aspectRatio: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(
    tableName = "media_assets",
    indices = [Index(value = ["content_uri"], unique = true)]
)
data class MediaAssetEntity(
    @PrimaryKey @ColumnInfo(name = "asset_id") val assetId: String,
    @ColumnInfo(name = "content_uri") val contentUri: String,
    @ColumnInfo(name = "media_type") val mediaType: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    val width: Int,
    val height: Int,
    @ColumnInfo(name = "proxy_path") val proxyPath: String?,
    @ColumnInfo(name = "is_available") val isAvailable: Boolean = true
)

@Entity(tableName = "color_presets")
data class ColorPresetEntity(
    @PrimaryKey @ColumnInfo(name = "preset_id") val presetId: String,
    val name: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
    @ColumnInfo(name = "is_builtin") val isBuiltin: Boolean,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(
    tableName = "cache_entries",
    indices = [Index(value = ["last_accessed_at"]), Index(value = ["project_id"])]
)
data class CacheEntryEntity(
    @PrimaryKey @ColumnInfo(name = "cache_key") val cacheKey: String,
    val category: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "project_id") val projectId: String?,
    @ColumnInfo(name = "last_accessed_at") val lastAccessedAt: Long
)
