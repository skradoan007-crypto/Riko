package com.rikodev.sync.nexmesh.core.vdfs.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "file_chunks",
    foreignKeys = [
        ForeignKey(
            entity = VirtualFileEntity::class,
            parentColumns = ["fileId"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["fileId"]),
        Index(value = ["assignedNodeId"])
    ]
)
data class FileChunkEntity(
    @PrimaryKey val chunkId: String,
    val fileId: String,
    val chunkIndex: Int,
    val sizeBytes: Long,
    val sha256: String,
    val assignedNodeId: String,
    val remoteUriHash: String,
    val replicaIndex: Int = 0, // 0 = primary, 1 = 2x redundancy replica
    val verified: Boolean = false
)
