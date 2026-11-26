package com.rikodev.sync.nexmesh.core.network.transfer

import android.net.Uri
import com.rikodev.sync.nexmesh.core.nativeengine.NativeEngineBridge
import com.rikodev.sync.nexmesh.core.nativeengine.NativeStreamSession
import com.rikodev.sync.nexmesh.core.network.client.SwarmControllerClient
import com.rikodev.sync.nexmesh.core.protocol.crypto.StreamingSha256
import com.rikodev.sync.nexmesh.core.protocol.model.TransferInitRequest
import com.rikodev.sync.nexmesh.core.protocol.session.ActiveSwarmSession
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.coroutines.coroutineContext

enum class TransferState {
    IDLE,
    PREPARING,
    TRANSFERRING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class TransferProgressSnapshot(
    val transferId: String = "",
    val fileName: String = "",
    val totalBytes: Long = 0L,
    val transferredBytes: Long = 0L,
    val currentChunk: Int = 0,
    val totalChunks: Int = 0,
    val logicalChunkSizeBytes: Int = StreamingTransferEngine.DEFAULT_LOGICAL_CHUNK_BYTES,
    val ioBufferSizeBytes: Int = StreamingTransferEngine.DEFAULT_IO_BUFFER_BYTES,
    val throughputMbps: Double = 0.0,
    val lz4Enabled: Boolean = false,
    val sha256Verified: Boolean = false,
    val state: TransferState = TransferState.IDLE,
    val errorMessage: String? = null
) {
    val progressFraction: Float
        get() = if (totalBytes <= 0L) 0f else (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}

/**
 * Zero-OOM Streaming Transfer Engine.
 * - Logical Chunk Size: 8 MB default (configurable)
 * - Network/Crypto/Native I/O Buffer: 128 KB – 256 KB strictly bounded
 * - Bounded Concurrency: Dispatchers.IO.limitedParallelism(2)
 * - Supports Pause, Cancel, and Atomic Checkpoint Resume after disconnects.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StreamingTransferEngine(
    private val client: SwarmControllerClient,
    private val safAdapter: SafStreamAdapter,
    private val checkpointStore: TransferCheckpointStore,
    private val canProceedThermalBattery: () -> Boolean = { true }
) {
    private val transferDispatcher = Dispatchers.IO.limitedParallelism(2)

    private val _progress = MutableStateFlow(TransferProgressSnapshot())
    val progress: StateFlow<TransferProgressSnapshot> = _progress.asStateFlow()

    @Volatile
    private var paused = false

    fun pauseTransfer() {
        paused = true
        _progress.value = _progress.value.copy(state = TransferState.PAUSED)
    }

    suspend fun uploadSafFile(
        fileUri: Uri,
        host: String,
        port: Int,
        session: ActiveSwarmSession,
        existingTransferId: String? = null,
        logicalChunkSize: Int = DEFAULT_LOGICAL_CHUNK_BYTES,
        ioBufferSize: Int = DEFAULT_IO_BUFFER_BYTES,
        useNativeLz4: Boolean = NativeEngineBridge.isAvailable
    ): Result<TransferProgressSnapshot> = withContext(transferDispatcher) {
        paused = false
        val safeIoBuf = ioBufferSize.coerceIn(128 * 1024, 256 * 1024)
        val lz4Active = useNativeLz4 && NativeEngineBridge.isAvailable

        runCatching {
            val desc = safAdapter.describeFile(fileUri)
                ?: throw IllegalArgumentException("Unable to access selected SAF file")
            val transferId = existingTransferId ?: "tx_${UUID.randomUUID().toString().take(12)}"
            val totalChunks = ((desc.sizeBytes + logicalChunkSize - 1) / logicalChunkSize).toInt().coerceAtLeast(1)

            _progress.value = TransferProgressSnapshot(
                transferId = transferId,
                fileName = desc.displayName,
                totalBytes = desc.sizeBytes,
                logicalChunkSizeBytes = logicalChunkSize,
                ioBufferSizeBytes = safeIoBuf,
                totalChunks = totalChunks,
                lz4Enabled = lz4Active,
                state = TransferState.PREPARING
            )

            // Streaming SHA-256 of source file (Zero-OOM)
            val fileSha = safAdapter.openInputStream(fileUri)?.use { stream ->
                StreamingSha256.hashStream(stream, safeIoBuf) { paused }
            } ?: throw IllegalStateException("Cannot open SAF input stream")

            val remoteStatus = client.initTransfer(
                host = host,
                port = port,
                sessionId = session.sessionId,
                request = TransferInitRequest(
                    transferId = transferId,
                    fileName = desc.displayName,
                    totalSizeBytes = desc.sizeBytes,
                    logicalChunkSizeBytes = logicalChunkSize,
                    totalChunks = totalChunks,
                    expectedFileSha256 = fileSha,
                    compressedWithLz4 = lz4Active
                )
            )

            val completedSet = remoteStatus.completedChunks.toMutableSet()
            var transferredSoFar = remoteStatus.receivedBytes
            val startTimeNs = System.nanoTime()

            _progress.value = _progress.value.copy(
                transferredBytes = transferredSoFar,
                state = TransferState.TRANSFERRING
            )

            val nativeSession = if (lz4Active) NativeStreamSession() else null
            val directSrc = if (lz4Active) ByteBuffer.allocateDirect(safeIoBuf) else null
            val directDst = if (lz4Active) ByteBuffer.allocateDirect(NativeEngineBridge.maxCompressedCapacity(safeIoBuf)) else null

            try {
                for (chunkIdx in 0 until totalChunks) {
                    coroutineContext.ensureActive()
                    if (paused || !canProceedThermalBattery()) {
                        val reason = if (!canProceedThermalBattery()) "Paused by Thermal/Battery Guard" else null
                        val pausedState = _progress.value.copy(state = TransferState.PAUSED, errorMessage = reason)
                        _progress.value = pausedState
                        return@runCatching pausedState
                    }
                    if (completedSet.contains(chunkIdx)) continue

                    val chunkStart = chunkIdx.toLong() * logicalChunkSize
                    val chunkBytesToSend = minOf(logicalChunkSize.toLong(), desc.sizeBytes - chunkStart)

                    // Pre-compute chunk SHA-256 via streaming read
                    val chunkSha = computeChunkSha(fileUri, chunkStart, chunkBytesToSend, safeIoBuf)

                    val streamingBody = object : OutgoingContent.WriteChannelContent() {
                        override val contentType: ContentType = ContentType.Application.OctetStream
                        override suspend fun writeTo(channel: ByteWriteChannel) {
                            safAdapter.openInputStream(fileUri)?.use { input ->
                                skipFully(input, chunkStart)
                                val ioBuf = ByteArray(safeIoBuf)
                                var remaining = chunkBytesToSend
                                var frameSeq = 0

                                while (remaining > 0L) {
                                    coroutineContext.ensureActive()
                                    val toRead = minOf(remaining, safeIoBuf.toLong()).toInt()
                                    val read = input.read(ioBuf, 0, toRead)
                                    if (read <= 0) break

                                    val payloadToEncrypt = if (nativeSession != null && directSrc != null && directDst != null) {
                                        directSrc.clear()
                                        directSrc.put(ioBuf, 0, read)
                                        directSrc.flip()
                                        directDst.clear()
                                        val compLen = nativeSession.compress(directSrc, read, directDst)
                                        ByteArray(compLen).also { directDst.get(it) }
                                    } else {
                                        ioBuf.copyOf(read)
                                    }

                                    val aad = "$transferId:$chunkIdx:$frameSeq".toByteArray(Charsets.UTF_8)
                                    val encrypted = session.cipher.encryptFrame(
                                        plaintext = payloadToEncrypt,
                                        offset = 0,
                                        length = payloadToEncrypt.size,
                                        chunkIndex = chunkIdx,
                                        frameSeq = frameSeq,
                                        aad = aad
                                    )

                                    channel.writeInt(encrypted.size)
                                    channel.writeFully(encrypted, 0, encrypted.size)
                                    remaining -= read
                                    frameSeq++
                                }
                                channel.writeInt(0) // End-of-chunk sentinel
                            }
                        }
                    }

                    val ack = client.uploadEncryptedChunk(
                        host = host,
                        port = port,
                        sessionId = session.sessionId,
                        transferId = transferId,
                        chunkIndex = chunkIdx,
                        chunkSha256 = chunkSha,
                        lz4Compressed = lz4Active,
                        bodyContent = streamingBody
                    )

                    completedSet.add(chunkIdx)
                    transferredSoFar = ack.receivedBytes
                    val elapsedSec = ((System.nanoTime() - startTimeNs).coerceAtLeast(1L)) / 1e9
                    val mbps = (transferredSoFar / (1024.0 * 1024.0)) / elapsedSec

                    checkpointStore.save(
                        TransferCheckpoint(
                            transferId = transferId,
                            fileName = desc.displayName,
                            totalSizeBytes = desc.sizeBytes,
                            logicalChunkSizeBytes = logicalChunkSize,
                            totalChunks = totalChunks,
                            expectedFileSha256 = fileSha,
                            compressedWithLz4 = lz4Active,
                            completedChunks = completedSet.toSet()
                        )
                    )

                    _progress.value = _progress.value.copy(
                        transferredBytes = transferredSoFar,
                        currentChunk = chunkIdx + 1,
                        throughputMbps = mbps,
                        sha256Verified = true,
                        state = TransferState.TRANSFERRING
                    )
                }
            } finally {
                nativeSession?.close()
            }

            val done = _progress.value.copy(
                transferredBytes = desc.sizeBytes,
                currentChunk = totalChunks,
                sha256Verified = true,
                state = TransferState.COMPLETED
            )
            _progress.value = done
            done
        }.onFailure { err ->
            _progress.value = _progress.value.copy(
                state = TransferState.FAILED,
                errorMessage = err.message ?: "Transfer failed"
            )
        }
    }

    private fun computeChunkSha(uri: Uri, startOffset: Long, length: Long, bufSize: Int): String {
        val sha = StreamingSha256()
        safAdapter.openInputStream(uri)?.use { input ->
            skipFully(input, startOffset)
            val buf = ByteArray(bufSize)
            var remaining = length
            while (remaining > 0L) {
                val read = input.read(buf, 0, minOf(remaining, bufSize.toLong()).toInt())
                if (read <= 0) break
                sha.update(buf, 0, read)
                remaining -= read
            }
        }
        return sha.digestHex()
    }

    private fun skipFully(input: InputStream, bytesToSkip: Long) {
        var remaining = bytesToSkip
        while (remaining > 0L) {
            val skipped = input.skip(remaining)
            if (skipped <= 0L) {
                if (input.read() == -1) break
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }

    companion object {
        const val DEFAULT_LOGICAL_CHUNK_BYTES = 8 * 1024 * 1024 // 8 MB
        const val DEFAULT_IO_BUFFER_BYTES = 128 * 1024          // 128 KB (safe on 2GB RAM)
    }
}
