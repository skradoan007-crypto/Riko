package com.rikodev.sync.nexmesh.core.vdfs.allocator

import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeEntity
import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeStatus

data class ChunkPlacementPlan(
    val chunkIndex: Int,
    val sizeBytes: Long,
    val assignedNodeIds: List<String> // Size 1 (standard) or 2 (2x redundancy across distinct nodes)
)

/**
 * Smart Placement Allocator:
 * Ranks ONLINE swarm nodes by Free Space (50%), Battery Headroom (25%), and Thermal Headroom (25%).
 * Automatically excludes nodes above 42°C, unplugged below 20% battery, or without enough free space.
 * Supports Optional 2x Redundancy placing replicas onto distinct physical nodes whenever >= 2 nodes qualify.
 */
class SmartPlacementAllocator {

    fun allocateChunks(
        totalFileSizeBytes: Long,
        logicalChunkSizeBytes: Int,
        replicationFactor: Int,
        candidateNodes: List<SwarmNodeEntity>,
        reserveHeadroomBytes: Long = 256L * 1024L * 1024L // Keep 256MB free safety cushion per phone
    ): Result<List<ChunkPlacementPlan>> {
        val safeReplication = replicationFactor.coerceIn(1, 2)
        val chunkCount = ((totalFileSizeBytes + logicalChunkSizeBytes - 1) / logicalChunkSizeBytes)
            .toInt()
            .coerceAtLeast(1)

        // Track mutable projected free space across allocation loop
        val projectedFree = candidateNodes.associate { it.nodeId to it.freeBytes }.toMutableMap()

        val plans = mutableListOf<ChunkPlacementPlan>()
        for (chunkIdx in 0 until chunkCount) {
            val offset = chunkIdx.toLong() * logicalChunkSizeBytes
            val chunkSize = minOf(logicalChunkSizeBytes.toLong(), (totalFileSizeBytes - offset).coerceAtLeast(0L))

            val eligible = candidateNodes
                .filter { node ->
                    val rem = projectedFree[node.nodeId] ?: 0L
                    node.status == SwarmNodeStatus.ONLINE &&
                        rem >= (chunkSize + reserveHeadroomBytes) &&
                        (node.tempCelsius == null || node.tempCelsius <= 42.0f) &&
                        (node.isCharging || node.batteryPct >= 20)
                }
                .sortedByDescending { scoreNode(it, projectedFree[it.nodeId] ?: 0L) }

            if (eligible.isEmpty()) {
                return Result.failure(
                    IllegalStateException("Insufficient healthy swarm storage for chunk #$chunkIdx (${chunkSize / (1024 * 1024)} MB)")
                )
            }

            val selected = eligible.take(safeReplication)
            selected.forEach { node ->
                projectedFree[node.nodeId] = (projectedFree[node.nodeId] ?: 0L) - chunkSize
            }

            plans.add(
                ChunkPlacementPlan(
                    chunkIndex = chunkIdx,
                    sizeBytes = chunkSize,
                    assignedNodeIds = selected.map { it.nodeId }
                )
            )
        }
        return Result.success(plans)
    }

    internal fun scoreNode(node: SwarmNodeEntity, currentFreeBytes: Long): Double {
        // 1. Free Space ratio (0..1, weight 0.50)
        val spaceRatio = if (node.totalBytes > 0L) {
            (currentFreeBytes.toDouble() / node.totalBytes.toDouble()).coerceIn(0.0, 1.0)
        } else {
            0.5
        }
        // Bonus for absolute free capacity (normalized up to 64 GB)
        val absSpaceFactor = (currentFreeBytes.toDouble() / (64.0 * 1024 * 1024 * 1024)).coerceIn(0.0, 1.0)
        val spaceScore = (spaceRatio * 0.6 + absSpaceFactor * 0.4)

        // 2. Battery Headroom (0..1, weight 0.25)
        val batteryScore = when {
            node.isCharging -> 1.0
            node.batteryPct < 0 -> 0.5
            else -> ((node.batteryPct - 20).coerceAtLeast(0) / 80.0).coerceIn(0.0, 1.0)
        }

        // 3. Thermal Headroom (0..1, weight 0.25) — neutral 0.7 if temperature unavailable
        val thermalScore = when (val temp = node.tempCelsius) {
            null -> 0.7
            else -> ((42.0 - temp) / 17.0).coerceIn(0.0, 1.0)
        }

        return spaceScore * 0.50 + batteryScore * 0.25 + thermalScore * 0.25
    }
}
