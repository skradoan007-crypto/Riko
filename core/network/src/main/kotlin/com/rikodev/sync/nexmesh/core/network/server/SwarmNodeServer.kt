package com.rikodev.sync.nexmesh.core.network.server

import com.rikodev.sync.nexmesh.core.nativeengine.NativeEngineBridge
import com.rikodev.sync.nexmesh.core.nativeengine.NativeStreamSession
import com.rikodev.sync.nexmesh.core.network.transfer.TransferCheckpoint
import com.rikodev.sync.nexmesh.core.network.transfer.TransferCheckpointStore
import com.rikodev.sync.nexmesh.core.protocol.crypto.Base64UrlCompat
import com.rikodev.sync.nexmesh.core.protocol.crypto.EcdhKeyExchange
import com.rikodev.sync.nexmesh.core.protocol.crypto.StreamingSha256
import com.rikodev.sync.nexmesh.core.protocol.identity.NodeIdentityManager
import com.rikodev.sync.nexmesh.core.protocol.identity.TrustedPeerStore
import com.rikodev.sync.nexmesh.core.protocol.model.NodeHealthPayload
import com.rikodev.sync.nexmesh.core.protocol.model.PairConfirmRequest
import com.rikodev.sync.nexmesh.core.protocol.model.PairConfirmResponse
import com.rikodev.sync.nexmesh.core.protocol.model.PairInitRequest
import com.rikodev.sync.nexmesh.core.protocol.model.PairInitResponse
import com.rikodev.sync.nexmesh.core.protocol.model.TransferInitRequest
import com.rikodev.sync.nexmesh.core.protocol.model.TransferStatusResponse
import com.rikodev.sync.nexmesh.core.protocol.model.TrustedPeerRecord
import com.rikodev.sync.nexmesh.core.protocol.session.PendingPairingChallenge
import com.rikodev.sync.nexmesh.core.protocol.session.SessionManager
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.utils.io.readInt
import io.ktor.utils.io.readFully
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.SecureRandom

/**
 * Embedded Lightweight Ktor CIO Server for Swarm Worker/Storage Nodes.
 * Supports ECDH+SAS pairing, encrypted streaming chunk uploads (128KB–256KB bounded frames),
 * atomic checkpoint resume, and HTTP Range downloads.
 */
class SwarmNodeServer(
    private val identityManager: NodeIdentityManager,
    private val trustedPeerStore: TrustedPeerStore,
    private val sessionManager: SessionManager,
    private val checkpointStore: TransferCheckpointStore,
    private val storageStagingDir: File,
    private val healthProvider: () -> NodeHealthPayload,
    private val onPendingChallenge: (PendingPairingChallenge) -> Unit = {}
) {
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    var activePort: Int = 47780
        private set

    init {
        storageStagingDir.mkdirs()
    }

    fun start(port: Int = 47780) {
        if (_isRunning.value) return
        activePort = port
        server = embeddedServer(CIO, host = "0.0.0.0", port = port) {
            configureSwarmRoutes()
        }.start(wait = false)
        _isRunning.value = true
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 500, timeoutMillis = 2_000)
        server = null
        _isRunning.value = false
    }

    private fun Application.configureSwarmRoutes() {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
        install(PartialContent)

        routing {
            get("/v1/health") {
                call.respond(healthProvider())
            }

            post("/v1/pair/init") {
                val req = call.receive<PairInitRequest>()
                val localIdentity = identityManager.getOrCreateIdentity()
                val workerEph = EcdhKeyExchange.generateEphemeralKeyPair()
                val controllerEphPub = EcdhKeyExchange.decodePublicKeyB64Url(req.controllerEphemeralPubKeyB64)
                val controllerStaticBytes = Base64UrlCompat.decode(req.controllerStaticPubKeyB64)
                val controllerNonce = Base64UrlCompat.decode(req.nonceB64)
                val workerNonce = ByteArray(16).also { SecureRandom().nextBytes(it) }

                val material = EcdhKeyExchange.deriveSessionMaterial(
                    localEphemeralPrivate = workerEph.private,
                    peerEphemeralPublic = controllerEphPub,
                    controllerStaticPubBytes = controllerStaticBytes,
                    workerStaticPubBytes = localIdentity.publicKey.encoded,
                    controllerNonce = controllerNonce,
                    workerNonce = workerNonce
                )

                val challenge = sessionManager.registerPendingChallenge(
                    peerNodeId = req.controllerNodeId,
                    peerDeviceName = req.controllerDeviceName,
                    peerStaticPubKeyB64 = req.controllerStaticPubKeyB64,
                    material = material
                )
                onPendingChallenge(challenge)

                call.respond(
                    PairInitResponse(
                        pairingToken = challenge.pairingToken,
                        workerNodeId = localIdentity.nodeId,
                        workerDeviceName = localIdentity.deviceName,
                        workerStaticPubKeyB64 = localIdentity.publicKeyB64Url,
                        workerEphemeralPubKeyB64 = EcdhKeyExchange.encodePublicKeyB64Url(workerEph.public),
                        workerNonceB64 = Base64UrlCompat.encode(workerNonce),
                        sasCode = challenge.sasCode,
                        expiresAtEpochMs = challenge.expiresAtEpochMs
                    )
                )
            }

            post("/v1/pair/confirm") {
                val req = call.receive<PairConfirmRequest>()
                val challenge = sessionManager.getPendingChallenge(req.pairingToken)
                if (challenge == null) {
                    call.respond(HttpStatusCode.Gone, PairConfirmResponse(status = "EXPIRED"))
                    return@post
                }
                if (!req.sasApproved || challenge.localUserRejected) {
                    sessionManager.removePendingChallenge(req.pairingToken)
                    call.respond(HttpStatusCode.Forbidden, PairConfirmResponse(status = "REJECTED"))
                    return@post
                }
                if (!challenge.localUserApproved) {
                    call.respond(HttpStatusCode.Accepted, PairConfirmResponse(status = "PENDING_WORKER_APPROVAL"))
                    return@post
                }

                // Both Controller & Worker explicitly approved the 6-digit SAS code
                trustedPeerStore.saveTrustedPeer(
                    TrustedPeerRecord(
                        nodeId = challenge.peerNodeId,
                        deviceName = challenge.peerDeviceName,
                        publicKeyB64Url = challenge.peerStaticPubKeyB64,
                        lastKnownHost = call.request.local.remoteAddress,
                        lastKnownPort = activePort,
                        pairedAtEpochMs = System.currentTimeMillis()
                    )
                )
                val session = sessionManager.promoteToActiveSession(
                    peerNodeId = challenge.peerNodeId,
                    peerDeviceName = challenge.peerDeviceName,
                    material = challenge.derivedMaterial
                )
                sessionManager.removePendingChallenge(req.pairingToken)

                call.respond(
                    PairConfirmResponse(
                        status = "APPROVED",
                        sessionId = session.sessionId,
                        sessionExpiresAtEpochMs = session.expiresAtEpochMs
                    )
                )
            }

            post("/v1/transfer/init") {
                val sessionId = call.request.headers["X-NexMesh-Session"]
                val session = sessionId?.let { sessionManager.getActiveSession(it) }
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid or expired E2EE session")
                    return@post
                }
                val req = call.receive<TransferInitRequest>()
                val existing = checkpointStore.load(req.transferId)
                val checkpoint = existing ?: TransferCheckpoint(
                    transferId = req.transferId,
                    fileName = req.fileName,
                    totalSizeBytes = req.totalSizeBytes,
                    logicalChunkSizeBytes = req.logicalChunkSizeBytes,
                    totalChunks = req.totalChunks,
                    expectedFileSha256 = req.expectedFileSha256,
                    compressedWithLz4 = req.compressedWithLz4
                ).also { checkpointStore.save(it) }

                call.respond(
                    TransferStatusResponse(
                        transferId = checkpoint.transferId,
                        completedChunks = checkpoint.completedChunks.sorted(),
                        receivedBytes = checkpoint.completedBytes,
                        completed = checkpoint.isComplete
                    )
                )
            }

            head("/v1/transfer/{id}/status") {
                val id = call.parameters["id"] ?: return@head call.respond(HttpStatusCode.BadRequest)
                val chk = checkpointStore.load(id) ?: return@head call.respond(HttpStatusCode.NotFound)
                call.response.header("X-NexMesh-Received-Bytes", chk.completedBytes.toString())
                call.response.header("X-NexMesh-Completed-Chunks", chk.completedChunks.size.toString())
                call.respond(HttpStatusCode.OK)
            }

            get("/v1/transfer/{id}/status") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val chk = checkpointStore.load(id) ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(
                    TransferStatusResponse(
                        transferId = chk.transferId,
                        completedChunks = chk.completedChunks.sorted(),
                        receivedBytes = chk.completedBytes,
                        completed = chk.isComplete
                    )
                )
            }

            put("/v1/transfer/{id}/chunk/{chunkIndex}") {
                val sessionId = call.request.headers["X-NexMesh-Session"]
                val session = sessionId?.let { sessionManager.getActiveSession(it) }
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, "Unauthorized session")

                val transferId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val chunkIndex = call.parameters["chunkIndex"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                val expectedChunkSha = call.request.headers["X-Chunk-Sha256"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing X-Chunk-Sha256")
                val isLz4Compressed = call.request.headers["X-NexMesh-Lz4"] == "1"

                val checkpoint = checkpointStore.load(transferId)
                    ?: return@put call.respond(HttpStatusCode.NotFound, "Transfer not initialized")

                val targetFile = File(storageStagingDir, "${transferId}_${checkpoint.fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}")
                val chunkOffset = chunkIndex.toLong() * checkpoint.logicalChunkSizeBytes
                val chunkSha = StreamingSha256()
                val channel = call.receiveChannel()

                val nativeSession = if (isLz4Compressed && NativeEngineBridge.isAvailable) NativeStreamSession() else null
                val directIn = if (nativeSession != null) ByteBuffer.allocateDirect(NativeEngineBridge.maxCompressedCapacity(NativeEngineBridge.MAX_BLOCK_BYTES)) else null
                val directOut = if (nativeSession != null) ByteBuffer.allocateDirect(NativeEngineBridge.MAX_BLOCK_BYTES) else null

                try {
                    RandomAccessFile(targetFile, "rw").use { raf ->
                        raf.seek(chunkOffset)
                        var frameSeq = 0
                        while (!channel.isClosedForRead) {
                            val frameLen = try {
                                channel.readInt()
                            } catch (_: Exception) {
                                break
                            }
                            if (frameLen == 0) break // End-of-chunk sentinel
                            if (frameLen < 0 || frameLen > NativeEngineBridge.MAX_BLOCK_BYTES + 2048) {
                                call.respond(HttpStatusCode.BadRequest, "Frame exceeds Zero-OOM limit")
                                return@put
                            }

                            val globalSeq = (chunkIndex.toLong() shl 20) + frameSeq
                            if (!session.replayGuard.accept(globalSeq)) {
                                call.respond(HttpStatusCode.Conflict, "Replay frame rejected")
                                return@put
                            }

                            val cipherBytes = ByteArray(frameLen)
                            channel.readFully(cipherBytes, 0, frameLen)
                            val aad = "$transferId:$chunkIndex:$frameSeq".toByteArray(Charsets.UTF_8)
                            val decrypted = session.cipher.decryptFrame(
                                ciphertextWithTag = cipherBytes,
                                offset = 0,
                                length = frameLen,
                                chunkIndex = chunkIndex,
                                frameSeq = frameSeq,
                                aad = aad
                            )

                            val plainBytes = if (nativeSession != null && directIn != null && directOut != null) {
                                directIn.clear()
                                directIn.put(decrypted)
                                directIn.flip()
                                directOut.clear()
                                val outLen = nativeSession.decompress(directIn, decrypted.size, directOut)
                                ByteArray(outLen).also { directOut.get(it) }
                            } else {
                                decrypted
                            }

                            chunkSha.update(plainBytes, 0, plainBytes.size)
                            raf.write(plainBytes)
                            frameSeq++
                        }
                        raf.fd.sync()
                    }
                } finally {
                    nativeSession?.close()
                }

                val actualSha = chunkSha.digestHex()
                if (!actualSha.equals(expectedChunkSha, ignoreCase = true)) {
                    call.respond(HttpStatusCode.UnprocessableEntity, "Chunk SHA-256 mismatch")
                    return@put
                }

                val updated = checkpointStore.markChunkCompleted(transferId, chunkIndex)!!
                call.respond(
                    TransferStatusResponse(
                        transferId = updated.transferId,
                        completedChunks = updated.completedChunks.sorted(),
                        receivedBytes = updated.completedBytes,
                        completed = updated.isComplete
                    )
                )
            }

            get("/v1/transfer/{id}/download") {
                val sessionId = call.request.headers["X-NexMesh-Session"]
                if (sessionId == null || sessionManager.getActiveSession(sessionId) == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val chk = checkpointStore.load(id) ?: return@get call.respond(HttpStatusCode.NotFound)
                val file = File(storageStagingDir, "${id}_${chk.fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}")
                if (!file.exists()) return@get call.respond(HttpStatusCode.NotFound)
                call.respondFile(file)
            }
        }
    }
}
