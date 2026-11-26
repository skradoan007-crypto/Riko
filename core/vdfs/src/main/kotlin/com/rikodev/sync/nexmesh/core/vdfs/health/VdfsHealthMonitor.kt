package com.rikodev.sync.nexmesh.core.vdfs.health

import com.rikodev.sync.nexmesh.core.vdfs.entity.FileChunkEntity
import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeEntity
import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeStatus
import com.rikodev.sync.nexmesh.core.vdfs.entity.VirtualFileEntity

enum class FileAvailabilityState {
    HEALTHY,      // All chunks have full desired replication online & verified
    DEGRADED,     // File is readable, but >=1 replica is offline/missing (2x redundancy protecting data)
    UNAVAILABLE   // At least one logical chunk has 0 online verified replicas
}

data class VirtualFileHealthReport(
    val file: VirtualFileEntity,
    val availability: FileAvailabilityState,
    val onlineChunkCount: Int,
    val missingChunkIndices: List<Int>,
    val unverifiedChunkCount: Int,
    val hostingNodeCount: Int
)

/**
 * Evaluates real-time file availability when swarm nodes go offline
 * and detects missing or unverified chunks across the VDFS cluster.
 */
object VdfsHealthMonitor {

    fun evaluateFileHealth(
        file: VirtualFileEntity,
        chunks: List<FileChunkEntity>,
        nodes: List<SwarmNodeEntity>
    ): VirtualFileHealthReport {
        val onlineNodeIds = nodes
            .filter { it.status == SwarmNodeStatus.ONLINE || it.status == SwarmNodeStatus.THROTTLED }
            .map { it.nodeId }
            .toSet()

        val byIndex = chunks.groupBy { it.chunkIndex }
        val missingIndices = mutableListOf<Int>()
        var fullyReplicatedChunks = 0
        var readableChunks = 0

        for (idx in 0 until file.chunkCount) {
            val replicas = byIndex[idx].orEmpty()
            val onlineVerified = replicas.filter { it.verified && it.assignedNodeId in onlineNodeIds }
            if (onlineVerified.isEmpty()) {
                missingIndices.add(idx)
            } else {
                readableChunks++
                if (onlineVerified.size >= file.replicationFactor) {
                    fullyReplicatedChunks++
                }
            }
        }

        val state = when {
            missingIndices.isNotEmpty() -> FileAvailabilityState.UNAVAILABLE
            fullyReplicatedChunks < file.chunkCount -> FileAvailabilityState.DEGRADED
            else -> FileAvailabilityState.HEALTHY
        }

        val unverifiedCount = chunks.count { !it.verified }
        val hostingNodes = chunks.map { it.assignedNodeId }.toSet().size

        return VirtualFileHealthReport(
            file = file,
            availability = state,
            onlineChunkCount = readableChunks,
            missingChunkIndices = missingIndices,
            unverifiedChunkCount = unverifiedCount,
            hostingNodeCount = hostingNodes
        )
    }
}
