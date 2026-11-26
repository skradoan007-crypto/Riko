package com.rikodev.sync.nexmesh.core.network.client

import com.rikodev.sync.nexmesh.core.protocol.model.NodeHealthPayload
import com.rikodev.sync.nexmesh.core.protocol.model.PairConfirmRequest
import com.rikodev.sync.nexmesh.core.protocol.model.PairConfirmResponse
import com.rikodev.sync.nexmesh.core.protocol.model.PairInitRequest
import com.rikodev.sync.nexmesh.core.protocol.model.PairInitResponse
import com.rikodev.sync.nexmesh.core.protocol.model.TransferInitRequest
import com.rikodev.sync.nexmesh.core.protocol.model.TransferStatusResponse
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
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.Closeable

/**
 * Lightweight Ktor CIO Controller Client with configurable timeouts and streaming upload support.
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
