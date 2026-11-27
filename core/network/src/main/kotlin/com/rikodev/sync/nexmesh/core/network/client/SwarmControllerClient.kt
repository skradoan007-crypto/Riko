package com.rikodev.sync.nexmesh.core.network.client

import com.rikodev.sync.nexmesh.core.network.server.SwarmNodeServer
import com.rikodev.sync.nexmesh.core.protocol.model.NodeHealthPayload
import com.rikodev.sync.nexmesh.core.protocol.model.PairConfirmRequest
import com.rikodev.sync.nexmesh.core.protocol.model.PairConfirmResponse
import com.rikodev.sync.nexmesh.core.protocol.model.PairInitRequest
import com.rikodev.sync.nexmesh.core.protocol.model.PairInitResponse
import com.rikodev.sync.nexmesh.core.protocol.model.RemoteVaultListResponse
import com.rikodev.sync.nexmesh.core.protocol.model.TransferInitRequest
import com.rikodev.sync.nexmesh.core.protocol.model.TransferStatusResponse
import com.rikodev.sync.nexmesh.core.protocol.session.ActiveSwarmSession
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readFully
import io.ktor.utils.io.readInt
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.io.OutputStream

/**
 * Lightweight Ktor CIO Controller Client with pairing, streaming upload,
 * AND E2EE Remote Node SAF Vault browsing + streaming download.
 */
class SwarmControllerClient : Closeable {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 120_000
            socketTimeoutMillis = 30_000
        }
        engine {
            maxConnectionsCount = 8
            endpoint {
                maxConnectionsPerRoute = 4
                keepAliveTime = 15_000
            }
        }
    }

    private fun baseUrl(host: String, port: Int): String {
        val safeHost = if (host.contains(':') && !host.startsWith('[')) "[$host]" else host
        return "http://$safeHost:$port"
    }

    suspend fun fetchHealth(host: String, port: Int, timeoutMs: Long = 1_500L): NodeHealthPayload? {
        return runCatching {
            val response = httpClient.get("${baseUrl(host, port)}/v1/health") {
                timeout {
                    connectTimeoutMillis = timeoutMs
                    requestTimeoutMillis = timeoutMs
                    socketTimeoutMillis = timeoutMs
                }
            }
            if (response.status.isSuccess()) response.body<NodeHealthPayload>() else null
        }.getOrNull()
    }

    suspend fun initPairing(host: String, port: Int, request: PairInitRequest): PairInitResponse {
        val response = httpClient.post("${baseUrl(host, port)}/v1/pair/init") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        check(response.status.isSuccess()) { "Pair init failed with HTTP ${response.status.value}" }
        return response.body()
    }

    suspend fun confirmPairing(host: String, port: Int, request: PairConfirmRequest): PairConfirmResponse {
        val response = httpClient.post("${baseUrl(host, port)}/v1/pair/confirm") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.body()
    }

    /**
     * Fetches the list of files inside the paired Node's mounted SAF folder.
     */
    suspend fun listRemoteVaultFiles(
        host: String,
        port: Int,
        sessionId: String
    ): RemoteVaultListResponse {
        val response = httpClient.get("${baseUrl(host, port)}/v1/vault/files") {
            header("X-NexMesh-Session", sessionId)
        }
        check(response.status.isSuccess()) { "Vault listing failed: HTTP ${response.status.value}" }
        return response.body()
    }

    /**
     * Zero-OOM E2EE streaming download of a file from the paired Node's mounted SAF folder.
     */
    suspend fun downloadRemoteVaultFile(
        host: String,
        port: Int,
        session: ActiveSwarmSession,
        fileId: String,
        totalSizeBytes: Long,
        outputStream: OutputStream,
        onBytesDownloaded: (downloadedBytes: Long) -> Unit = {}
    ): Long {
        var totalWritten = 0L
        httpClient.prepareGet("${baseUrl(host, port)}/v1/vault/download/$fileId") {
            header("X-NexMesh-Session", session.sessionId)
            timeout {
                requestTimeoutMillis = 600_000L
                socketTimeoutMillis = 60_000L
            }
        }.execute { response ->
            check(response.status.isSuccess()) { "Download failed: HTTP ${response.status.value}" }
            val channel = response.bodyAsChannel()
            outputStream.use { out ->
                var frameSeq = 0
                while (!channel.isClosedForRead) {
                    val frameLen = try {
                        channel.readInt()
                    } catch (_: Exception) {
                        break
                    }
                    if (frameLen == 0) break
                    val cipherBytes = ByteArray(frameLen)
                    channel.readFully(cipherBytes, 0, frameLen)
                    val aad = "vault:$fileId:$frameSeq".toByteArray(Charsets.UTF_8)
                    val plain = session.cipher.decryptFrame(
                        ciphertextWithTag = cipherBytes,
                        offset = 0,
                        length = frameLen,
                        chunkIndex = SwarmNodeServer.VAULT_DOWNLOAD_CHANNEL,
                        frameSeq = frameSeq,
                        aad = aad
                    )
                    out.write(plain)
                    totalWritten += plain.size
                    onBytesDownloaded(totalWritten)
                    frameSeq++
                }
                out.flush()
            }
        }
        return totalWritten
    }

    suspend fun initTransfer(
        host: String,
        port: Int,
        sessionId: String,
        request: TransferInitRequest
    ): TransferStatusResponse {
        val response = httpClient.post("${baseUrl(host, port)}/v1/transfer/init") {
            header("X-NexMesh-Session", sessionId)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        check(response.status.isSuccess()) { "Transfer init failed: HTTP ${response.status.value}" }
        return response.body()
    }

    suspend fun uploadEncryptedChunk(
        host: String,
        port: Int,
        sessionId: String,
        transferId: String,
        chunkIndex: Int,
        chunkSha256: String,
        lz4Compressed: Boolean,
        bodyContent: OutgoingContent.WriteChannelContent
    ): TransferStatusResponse {
        val response = httpClient.put("${baseUrl(host, port)}/v1/transfer/$transferId/chunk/$chunkIndex") {
            header("X-NexMesh-Session", sessionId)
            header("X-Chunk-Sha256", chunkSha256)
            header("X-NexMesh-Lz4", if (lz4Compressed) "1" else "0")
            setBody(bodyContent)
        }
        check(response.status.isSuccess()) { "Chunk #$chunkIndex upload failed: HTTP ${response.status.value}" }
        return response.body()
    }

    override fun close() {
        httpClient.close()
    }
}
