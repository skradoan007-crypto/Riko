package com.rikodev.sync.nexmesh.core.vdfs

import com.rikodev.sync.nexmesh.core.vdfs.allocator.SmartPlacementAllocator
import com.rikodev.sync.nexmesh.core.vdfs.entity.FileChunkEntity
import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeEntity
import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeStatus
import com.rikodev.sync.nexmesh.core.vdfs.entity.VirtualFileEntity
import com.rikodev.sync.nexmesh.core.vdfs.health.FileAvailabilityState
import com.rikodev.sync.nexmesh.core.vdfs.health.VdfsHealthMonitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VdfsCoreTest {

    @Test
    fun allocatorPrefersCoolHighSpaceNodesAndAssignsDistinct2xReplicas() {
        val allocator = SmartPlacementAllocator()
        val nodes = listOf(
            SwarmNodeEntity("hot", "HotPhone", 64_000_000_000, 32_000_000_000, 90, 44.0f, SwarmNodeStatus.ONLINE, 1000L),
            SwarmNodeEntity("lowbat", "LowBatPhone", 64_000_000_000, 32_000_000_000, 12, 33.0f, SwarmNodeStatus.ONLINE, 1000L, isCharging = false),
            SwarmNodeEntity("coolA", "CoolPhoneA", 64_000_000_000, 48_000_000_000, 88, 32.0f, SwarmNodeStatus.ONLINE, 1000L),
            SwarmNodeEntity("coolB", "CoolPhoneB", 32_000_000_000, 20_000_000_000, 75, 35.0f, SwarmNodeStatus.ONLINE, 1000L)
        )

        val plans = allocator.allocateChunks(
            totalFileSizeBytes = 16L * 1024 * 1024,
            logicalChunkSizeBytes = 8 * 1024 * 1024,
            replicationFactor = 2,
            candidateNodes = nodes
        ).getOrThrow()

        assertEquals(2, plans.size)
        plans.forEach { plan ->
            assertEquals(2, plan.assignedNodeIds.size)
            assertEquals(2, plan.assignedNodeIds.toSet().size) // Distinct physical nodes
            assertTrue("hot" !in plan.assignedNodeIds)
            assertTrue("lowbat" !in plan.assignedNodeIds)
        }
    }

    @Test
    fun healthMonitorMarksFileDegradedWhenOneReplicaNodeGoesOffline() {
        val file = VirtualFileEntity("f1", "video.mp4", "video/mp4", 8_000_000, "sha", 1, replicationFactor = 2, committed = true)
        val chunks = listOf(
            FileChunkEntity("f1_c0_r0", "f1", 0, 8_000_000, "c0", "nodeA", "h0", replicaIndex = 0, verified = true),
            FileChunkEntity("f1_c0_r1", "f1", 0, 8_000_000, "c0", "nodeB", "h1", replicaIndex = 1, verified = true)
        )
        val nodes = listOf(
            SwarmNodeEntity("nodeA", "PhoneA", 64_000_000_000, 30_000_000_000, 80, 34f, SwarmNodeStatus.ONLINE, 1000L),
            SwarmNodeEntity("nodeB", "PhoneB", 64_000_000_000, 30_000_000_000, 80, 34f, SwarmNodeStatus.OFFLINE, 1000L)
        )

        val report = VdfsHealthMonitor.evaluateFileHealth(file, chunks, nodes)
        assertEquals(FileAvailabilityState.DEGRADED, report.availability)
        assertTrue(report.missingChunkIndices.isEmpty())
    }
}
