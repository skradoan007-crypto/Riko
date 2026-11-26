package com.rikodev.sync.nexmesh.core.vdfs.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "virtual_files")
data class VirtualFileEntity(
    @PrimaryKey val fileId: String,
    val originalName: String,
    val mimeType: String,
    val totalSize: Long,
    val sha256: String,
    val chunkCount: Int,
    val replicationFactor: Int, // 1 = standard, 2 = optional 2x redundancy
    val committed: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
