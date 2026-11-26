package com.rikodev.sync.nexmesh.core.compute

import com.rikodev.sync.nexmesh.core.compute.engine.ComputeWorkerNode
import com.rikodev.sync.nexmesh.core.compute.engine.SplitComputeOrchestrator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SplitComputeReassignmentTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun reassignsSegmentAutomaticallyWhenWorkerDisconnectsMidTask() = runTest {
        val orchestrator = SplitComputeOrchestrator(maxRetriesPerSegment = 3)
        val seg0 = File(tempFolder.root, "seg0.mp4").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val seg1 = File(tempFolder.root, "seg1.mp4").apply { writeBytes(byteArrayOf(4, 5, 6)) }

        val workers = listOf(
            ComputeWorkerNode("node_flaky", "FlakyWorker", isLocal = false, hardwareCodecAvailable = true),
            ComputeWorkerNode("node_solid", "SolidWorker", isLocal = true, hardwareCodecAvailable = true)
        )

        val result = orchestrator.executeDistributedJob(
            jobId = "job_failover_01",
            segmentInputs = listOf(seg0, seg1),
            workers = workers
        ) { segIdx, inputFile, worker ->
            if (worker.nodeId == "node_flaky") {
                Result.failure(IllegalStateException("Simulated socket disconnect"))
            } else {
                val out = File(tempFolder.root, "processed_${segIdx}_${worker.nodeId}.mp4")
                inputFile.copyTo(out, overwrite = true)
                Result.success(out)
            }
        }

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        assertTrue(orchestrator.snapshot.value.reassignedCount >= 1)
        assertEquals(2, orchestrator.snapshot.value.completedSegments)
    }
}
