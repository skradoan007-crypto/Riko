package com.rikodev.sync.nexmesh.core.vdfs.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.rikodev.sync.nexmesh.core.vdfs.entity.FileChunkEntity
import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeEntity
import com.rikodev.sync.nexmesh.core.vdfs.entity.VirtualFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SwarmNodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(node: SwarmNodeEntity)

    @Query("SELECT * FROM swarm_nodes ORDER BY lastSeen DESC")
    fun observeAllNodes(): Flow<List<SwarmNodeEntity>>

    @Query("SELECT * FROM swarm_nodes WHERE status = 'ONLINE' ORDER BY freeBytes DESC")
    suspend fun getOnlineNodes(): List<SwarmNodeEntity>

    @Query("UPDATE swarm_nodes SET status = :status WHERE nodeId = :nodeId")
    suspend fun updateStatus(nodeId: String, status: String)

    @Query("UPDATE swarm_nodes SET status = 'OFFLINE' WHERE lastSeen < :cutoffEpochMs AND status != 'OFFLINE'")
    suspend fun markStaleNodesOffline(cutoffEpochMs: Long): Int
}

@Dao
interface VirtualFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFile(file: VirtualFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChunks(chunks: List<FileChunkEntity>)

    /**
     * Atomic File Commit: persists VirtualFileEntity + all verified FileChunkEntity rows
     * in a single SQLite transaction so partial/corrupted files never appear committed.
     */
    @Transaction
    suspend fun commitFileAtomically(file: VirtualFileEntity, chunks: List<FileChunkEntity>) {
        upsertFile(file.copy(committed = true))
        upsertChunks(chunks)
    }

    @Query("SELECT * FROM virtual_files WHERE committed = 1 ORDER BY createdAtEpochMs DESC")
    fun observeCommittedFiles(): Flow<List<VirtualFileEntity>>

    @Query("SELECT * FROM virtual_files WHERE fileId = :fileId LIMIT 1")
    suspend fun getFileById(fileId: String): VirtualFileEntity?

    @Query("DELETE FROM virtual_files WHERE fileId = :fileId")
    suspend fun deleteFile(fileId: String)
}

@Dao
interface FileChunkDao {
    @Query("SELECT * FROM file_chunks WHERE fileId = :fileId ORDER BY chunkIndex ASC, replicaIndex ASC")
    suspend fun getChunksForFile(fileId: String): List<FileChunkEntity>

    @Query("SELECT * FROM file_chunks")
    fun observeAllChunks(): Flow<List<FileChunkEntity>>

    @Query("SELECT * FROM file_chunks WHERE assignedNodeId = :nodeId")
    suspend fun getChunksOnNode(nodeId: String): List<FileChunkEntity>

    @Query("SELECT * FROM file_chunks WHERE verified = 0")
    suspend fun getUnverifiedChunks(): List<FileChunkEntity>

    @Query("UPDATE file_chunks SET verified = :verified WHERE chunkId = :chunkId")
    suspend fun setChunkVerified(chunkId: String, verified: Boolean)
}
