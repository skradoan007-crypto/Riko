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
import com.rikodev.sync.nexmesh.core.protocol.model.RemoteVaultFileItem
import com.rikodev.sync.nexmesh.core.protocol.model.RemoteVaultListResponse
import com.rikodev.sync.nexmesh.core.protocol.model.TransferInitRequest
import com.rikodev.sync.nexmesh.core.protocol.model.TransferStatusResponse
import com.rikodev.sync.nexmesh.core.protocol.model.TrustedPeerRecord
import com.rikodev.sync.nexmesh.core.protocol.model.TrustedReconnectRequest
import com.rikodev.sync.nexmesh.core.protocol.model.TrustedReconnectResponse
import com.rikodev.sync.nexmesh.core.protocol.session.PendingPairingChallenge
import com.rikodev.sync.nexmesh.core.protocol.session.SessionManager
import io.ktor.http.ContentType
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
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.utils.io.readFully
import io.ktor.utils.io.readInt
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.SecureRandom
import android.os.ParcelFileDescriptor

/**
 * Storage Phone Ktor CIO Server.
 * Exposes:
 * - One-Time QR Pairing (/v1/pair/init, /v1/pair/confirm)
 * - Automatic Trusted Reconnect (/v1/pair/reconnect) without re-scanning QR
 * - Live Remote Storage Tree Browsing (/v1/vault/files?parentId=...)
 * - Live Random-Access E2EE Byte-Range Streaming (/v1/vault/range/{fileId}?offset=&length=) for Android DocumentsProvider!
 */
class SwarmNodeServer(
    private val identityManager: NodeIdentityManager,
    private val trustedPeerStore: TrustedPeerStore,
    private val sessionManager: SessionManager,
    private val checkpointStore: TransferCheckpointStore,
    private val storageStagingDir: File,
    private val healthProvider: () -> NodeHealthPayload,
    private val vaultChildrenProvider: (parentId: String?) -> RemoteVaultListResponse = { RemoteVaultListResponse(mounted = false) },
    private val vaultMetaProvider: (fileId: String) -> RemoteVaultFileItem? = { null },
    private val vaultFileOpener: (fileId: String) -> Pair<String, InputStream>? = { null },
    private val vaultPfdOpener: (fileId: String) -> ParcelFileDescriptor? = { null },
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

            // One-Time Pairing Reconnect: previously trusted devices reconnect seamlessly with fresh ephemeral ECDH keys
            post("/v1/pair/reconnect") {
                val req = call.receive<TrustedReconnectRequest>()
                if (!trustedPeerStore.isTrusted(req.nodeId, req.staticPubKeyB64)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        TrustedReconnectResponse(accepted = false, reason = "Device is not paired as a trusted device")
                    )
                    return@post
                }

                val localIdentity = identityManager.getOrCreateIdentity()
                val workerEph = EcdhKeyExchange.generateEphemeralKeyPair()
                val peerEphPub = EcdhKeyExchange.decodePublicKeyB64Url(req.ephemeralPubKeyB64)
                val peerStaticBytes = Base64UrlCompat.decode(req.staticPubKeyB64)
                val peerNonce = Base64UrlCompat.decode(req.nonceB64)
                val workerNonce = ByteArray(16).also { SecureRandom().nextBytes(it) }

                val material = EcdhKeyExchange.deriveSessionMaterial(
                    localEphemeralPrivate = workerEph.private,
                    peerEphemeralPublic = peerEphPub,
                    controllerStaticPubBytes = peerStaticBytes,
                    workerStaticPubBytes = localIdentity.publicKey.encoded,
                    controllerNonce = peerNonce,
                    workerNonce = workerNonce
                )

                val session = sessionManager.promoteToActiveSession(
                    peerNodeId = req.nodeId,
                    peerDeviceName = req.deviceName,
                    material = material
                )

                call.respond(
                    TrustedReconnectResponse(
                        accepted = true,
                        sessionId = session.sessionId,
                        workerNodeId = localIdentity.nodeId,
                        workerDeviceName = localIdentity.deviceName,
                        workerStaticPubKeyB64 = localIdentity.publicKeyB64Url,
                        workerEphemeralPubKeyB64 = EcdhKeyExchange.encodePublicKeyB64Url(workerEph.public),
                        workerNonceB64 = Base64UrlCompat.encode(workerNonce),
                        sessionExpiresAtEpochMs = session.expiresAtEpochMs
                    )
                )
            }

            // Live Remote Storage Browsing (root shared folders/files or subfolder children)
            get("/v1/vault/files") {
                val sessionId = call.request.headers["X-NexMesh-Session"]
                if (sessionId == null || sessionManager.getActiveSession(sessionId) == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized session")
                    return@get
                }
                val parentId = call.request.queryParameters["parentId"]
                call.respond(vaultChildrenProvider(parentId))
            }

            get("/v1/vault/meta/{fileId}") {
                val sessionId = call.request.headers["X-NexMesh-Session"]
                if (sessionId == null || sessionManager.getActiveSession(sessionId) == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }
                val fileId = call.parameters["fileId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val meta = vaultMetaProvider(fileId) ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(meta)
            }

            // Live Random-Access E2EE Byte-Range Read for Android DocumentsProvider ProxyFileDescriptor (Zero Duplication!)
            get("/v1/vault/range/{fileId}") {
                val sessionId = call.request.headers["X-NexMesh-Session"]
                val session = sessionId?.let { sessionManager.getActiveSession(it) }
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val fileId = call.parameters["fileId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val offset = call.request.queryParameters["offset"]?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                val length = call.request.queryParameters["length"]?.toIntOrNull()?.coerceIn(1, 256 * 1024) ?: (128 * 1024)
                val seq = call.request.queryParameters["seq"]?.toIntOrNull() ?: 0

                val pfd = vaultPfdOpener(fileId) ?: return@get call.respond(HttpStatusCode.NotFound)
                val plainBytes = pfd.use { descriptor ->
                    FileInputStream(descriptor.fileDescriptor).use { fis ->
                        val fc = fis.channel
                        val fileSize = fc.size()
                        if (offset >= fileSize) {
                            ByteArray(0)
                        } else {
                            fc.position(offset)
                            val toRead = minOf(length.toLong(), fileSize - offset).toInt()
                            val buf = ByteBuffer.allocate(toRead)
                            var totalRead = 0
                            while (totalRead < toRead) {
                                val r = fc.read(buf)
                                if (r <= 0) break
                                totalRead += r
                            }
                            if (totalRead == toRead) buf.array() else buf.array().copyOf(totalRead)
                        }
                    }
                }

                val aad = "range:$fileId:$offset:$seq".toByteArray(Charsets.UTF_8)
                val encrypted = session.cipher.encryptFrame(
                    plaintext = plainBytes,
                    offset = 0,
                    length = plainBytes.size,
                    chunkIndex = VAULT_RANGE_CHANNEL,
                    frameSeq = seq,
                    aad = aad
                )
                call.respondBytes(encrypted, ContentType.Application.OctetStream)
            }

            // Full Streaming E2EE Download (Zero-OOM 128KB frames)
            get("/v1/vault/download/{fileId}") {
                val sessionId = call.request.headers["X-NexMesh-Session"]
                val session = sessionId?.let { sessionManager.getActiveSession(it) }
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val fileId = call.parameters["fileId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val opened = vaultFileOpener(fileId) ?: return@get call.respond(HttpStatusCode.NotFound)
                val (fileName, inputStream) = opened

                call.response.header("X-NexMesh-File-Name", fileName)
                call.respondBytesWriter(contentType = ContentType.Application.OctetStream) {
                    inputStream.use { input ->
                        val ioBuf = ByteArray(128 * 1024)
                        var frameSeq = 0
                        while (true) {
                            val read = input.read(ioBuf)
                            if (read <= 0) break
                            val aad = "vault:$fileId:$frameSeq".toByteArray(Charsets.UTF_8)
                            val encrypted = session.cipher.encryptFrame(
                                plaintext = ioBuf,
                                offset = 0,
                                length = read,
                                chunkIndex = VAULT_DOWNLOAD_CHANNEL,
                                frameSeq = frameSeq,
                                aad = aad
                            )
                            writeInt(encrypted.size)
                            writeFully(encrypted, 0, encrypted.size)
                            frameSeq++
                        }
                        writeInt(0)
                    }
                }
            }

            post("/v1/transfer/init") {
                val sessionId = call.request.headers["X-NexMesh-Session"]
                if (sessionId == null || sessionManager.getActiveSession(sessionId) == null) {
                    call.respond(HttpStatusCode.Unauthorized)
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
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)

                val transferId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val chunkIndex = call.parameters["chunkIndex"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                val expectedChunkSha = call.request.headers["X-Chunk-Sha256"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                val isLz4Compressed = call.request.headers["X-NexMesh-Lz4"] == "1"

                val checkpoint = checkpointStore.load(transferId)
                    ?: return@put call.respond(HttpStatusCode.NotFound)

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
                            if (frameLen == 0) break
                            if (frameLen < 0 || frameLen > NativeEngineBridge.MAX_BLOCK_BYTES + 2048) {
                                call.respond(HttpStatusCode.BadRequest)
                                return@put
                            }

                            val globalSeq = (chunkIndex.toLong() shl 20) + frameSeq
                            if (!session.replayGuard.accept(globalSeq)) {
                                call.respond(HttpStatusCode.Conflict)
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
                    call.respond(HttpStatusCode.UnprocessableEntity)
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

    companion object {
        const val VAULT_DOWNLOAD_CHANNEL = 0x7E00
        const val VAULT_RANGE_CHANNEL = 0x7D00
    }
}
