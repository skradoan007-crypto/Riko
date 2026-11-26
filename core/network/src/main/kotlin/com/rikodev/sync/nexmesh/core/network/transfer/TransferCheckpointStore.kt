package com.rikodev.sync.nexmesh.core.network.transfer

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class TransferCheckpoint(
    val transferId: String,
    val fileName: String,
    val totalSizeBytes: Long,
    val logicalChunkSizeBytes: Int,
    val totalChunks: Int,
    val expectedFileSha256: String,
    val compressedWithLz4: Boolean,
    val completedChunks: Set<Int> = emptySet(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val completedBytes: Long
        get() = completedChunks.sumOf { chunkIdx ->
            val start = chunkIdx.toLong() * logicalChunkSizeBytes
            minOf(logicalChunkSizeBytes.toLong(), (totalSizeBytes - start).coerceAtLeast(0L))
        }

    val isComplete: Boolean
        get() = totalChunks > 0 && completedChunks.size == totalChunks
}

/**
 * Atomic JSON checkpoint persistence for interrupted chunk transfer resume.
 */
class TransferCheckpointStore(private val baseDir: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    init {
        baseDir.mkdirs()
    }

    fun save(checkpoint: TransferCheckpoint) = synchronized(lock) {
        val target = fileFor(checkpoint.transferId)
        val tmp = File(baseDir, "${checkpoint.transferId}.chk.tmp")
        tmp.writeText(json.encodeToString(checkpoint))
        if (!tmp.renameTo(target)) {
            target.writeText(tmp.readText())
            tmp.delete()
        }
    }

    fun load(transferId: String): TransferCheckpoint? = synchronized(lock) {
        val target = fileFor(transferId)
        if (!target.exists()) return null
        runCatching { json.decodeFromString<TransferCheckpoint>(target.readText()) }.getOrNull()
    }

    fun markChunkCompleted(transferId: String, chunkIndex: Int): TransferCheckpoint? = synchronized(lock) {
        val current = load(transferId) ?: return null
        val updated = current.copy(
            completedChunks = current.completedChunks + chunkIndex,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        save(updated)
        updated
    }

    fun delete(transferId: String) = synchronized(lock) {
        fileFor(transferId).delete()
    }

    private fun fileFor(transferId: String): File {
        val safeId = transferId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(baseDir, "$safeId.chk.json")
    }
}
