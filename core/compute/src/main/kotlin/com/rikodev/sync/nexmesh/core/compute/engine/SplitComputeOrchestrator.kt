package com.rikodev.sync.nexmesh.core.compute.engine

import com.rikodev.sync.nexmesh.core.compute.codec.HardwareCodecCapabilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class ComputeWorkerNode(
    val nodeId: String,
    val name: String,
    val isLocal: Boolean,
    val hardwareCodecAvailable: Boolean
)

enum class SegmentTaskStatus {
    QUEUED,
    RUNNING,
    REASSIGNED,
    COMPLETED,
    FAILED
}

data class SegmentComputeTelemetry(
    val segmentIndex: Int,
    val assignedNodeName: String,
    val status: SegmentTaskStatus,
    val retryCount: Int = 0,
    val elapsedMs: Long = 0L,
    val message: String = ""
)

data class SplitComputeSnapshot(
    val jobId: String = "",
    val isRunning: Boolean = false,
    val totalSegments: Int = 0,
    val completedSegments: Int = 0,
    val reassignedCount: Int = 0,
    val segmentTelemetry: List<SegmentComputeTelemetry> = emptyList(),
    val statusMessage: String = "Idle",
    val finalOutputPath: String? = null,
    val finalSha256: String? = null
) {
    val progressFraction: Float
        get() = if (totalSegments <= 0) 0f else (completedSegments.toFloat() / totalSegments.toFloat()).coerceIn(0f, 1f)
}

/**
 * Fault-Tolerant Distributed Split-Compute Orchestrator.
 * - Executes split segments in parallel across healthy hardware-capable worker nodes.
 * - Automatically detects mid-task worker disconnects / failures and reassigns the segment
 *   to another available node (fault-tolerant failover).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplitComputeOrchestrator(
    private val maxRetriesPerSegment: Int = 3
) {
    private val computeDispatcher = Dispatchers.Default.limitedParallelism(3)

    private val _snapshot = MutableStateFlow(SplitComputeSnapshot())
    val snapshot: StateFlow<SplitComputeSnapshot> = _snapshot.asStateFlow()

    suspend fun executeDistributedJob(
        jobId: String,
        segmentInputs: List<File>,
        workers: List<ComputeWorkerNode>,
        canProceedThermalBattery: () -> Boolean = { true },
        segmentExecutor: suspend (segmentIndex: Int, inputFile: File, worker: ComputeWorkerNode) -> Result<File>
    ): Result<List<File>> = coroutineScope {
        if (segmentInputs.isEmpty()) {
            return@coroutineScope Result.failure(IllegalArgumentException("No segments to process"))
        }
        val healthyWorkers = workers.toMutableList()
        if (healthyWorkers.isEmpty()) {
            return@coroutineScope Result.failure(IllegalStateException("No available compute nodes"))
        }

        val failedNodeIds = ConcurrentHashMap.newKeySet<String>()
        val telemetryMap = ConcurrentHashMap<Int, SegmentComputeTelemetry>()
        val completedCount = AtomicInteger(0)
        val reassignedCount = AtomicInteger(0)

        segmentInputs.indices.forEach { idx ->
            telemetryMap[idx] = SegmentComputeTelemetry(idx, "Pending", SegmentTaskStatus.QUEUED)
        }

        fun publishState(running: Boolean, msg: String, outPath: String? = null, sha: String? = null) {
            _snapshot.value = SplitComputeSnapshot(
                jobId = jobId,
                isRunning = running,
                totalSegments = segmentInputs.size,
                completedSegments = completedCount.get(),
                reassignedCount = reassignedCount.get(),
                segmentTelemetry = telemetryMap.values.sortedBy { it.segmentIndex },
                statusMessage = msg,
                finalOutputPath = outPath,
                finalSha256 = sha
            )
        }

        publishState(true, "Dispatching ${segmentInputs.size} segments across ${workers.size} nodes...")

        val results = segmentInputs.mapIndexed { segIdx, segFile ->
            async(computeDispatcher) {
                var attempt = 0
                var lastError: Throwable? = null

                while (attempt < maxRetriesPerSegment) {
                    if (!canProceedThermalBattery()) {
                        throw IllegalStateException("Compute paused by Thermal/Battery Guard")
                    }

                    val candidates = healthyWorkers.filter { it.nodeId !in failedNodeIds }
                    if (candidates.isEmpty()) {
                        throw IllegalStateException("All compute nodes disconnected; unable to reassign segment #$segIdx")
                    }

                    val chosenWorker = candidates[(segIdx + attempt) % candidates.size]
                    val startMs = System.currentTimeMillis()
                    telemetryMap[segIdx] = SegmentComputeTelemetry(
                        segmentIndex = segIdx,
                        assignedNodeName = chosenWorker.name,
                        status = if (attempt == 0) SegmentTaskStatus.RUNNING else SegmentTaskStatus.REASSIGNED,
                        retryCount = attempt,
                        message = if (attempt == 0) "Processing" else "Reassigned after peer disconnect"
                    )
                    publishState(true, "Processing ${completedCount.get()}/${segmentInputs.size} segments...")

                    val outcome = try {
                        segmentExecutor(segIdx, segFile, chosenWorker)
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }

                    if (outcome.isSuccess) {
                        val elapsed = System.currentTimeMillis() - startMs
                        telemetryMap[segIdx] = SegmentComputeTelemetry(
                            segmentIndex = segIdx,
                            assignedNodeName = chosenWorker.name,
                            status = SegmentTaskStatus.COMPLETED,
                            retryCount = attempt,
                            elapsedMs = elapsed,
                            message = "Done in ${elapsed}ms"
                        )
                        completedCount.incrementAndGet()
                        publishState(true, "Completed ${completedCount.get()}/${segmentInputs.size} segments")
                        return@async segIdx to outcome.getOrThrow()
                    } else {
                        lastError = outcome.exceptionOrNull()
                        failedNodeIds.add(chosenWorker.nodeId)
                        attempt++
                        reassignedCount.incrementAndGet()
                        telemetryMap[segIdx] = SegmentComputeTelemetry(
                            segmentIndex = segIdx,
                            assignedNodeName = chosenWorker.name,
                            status = SegmentTaskStatus.REASSIGNED,
                            retryCount = attempt,
                            message = "Node failed (${lastError?.message ?: "disconnected"}), reassigning..."
                        )
                        publishState(true, "Node ${chosenWorker.name} dropped; reassigning segment #$segIdx")
                    }
                }

                telemetryMap[segIdx] = SegmentComputeTelemetry(
                    segmentIndex = segIdx,
                    assignedNodeName = "None",
                    status = SegmentTaskStatus.FAILED,
                    retryCount = attempt,
                    message = lastError?.message ?: "Max retries exhausted"
                )
                publishState(false, "Segment #$segIdx failed after $maxRetriesPerSegment attempts")
                throw lastError ?: IllegalStateException("Segment #$segIdx failed")
            }
        }

        runCatching {
            val orderedFiles = results.awaitAll().sortedBy { it.first }.map { it.second }
            publishState(false, "All ${orderedFiles.size} segments processed (Reassignments: ${reassignedCount.get()})")
            orderedFiles
        }.onFailure { err ->
            publishState(false, "Compute failed: ${err.message}")
        }
    }

    fun markReassembled(outputFile: File, sha256: String) {
        _snapshot.value = _snapshot.value.copy(
            isRunning = false,
            statusMessage = "Video reassembled & SHA-256 verified",
            finalOutputPath = outputFile.absolutePath,
            finalSha256 = sha256
        )
    }
}
