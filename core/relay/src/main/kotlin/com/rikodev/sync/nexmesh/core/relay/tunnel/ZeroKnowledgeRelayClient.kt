package com.rikodev.sync.nexmesh.core.relay.tunnel

import com.rikodev.sync.nexmesh.core.protocol.crypto.Base64UrlCompat
import com.rikodev.sync.nexmesh.core.protocol.session.ActiveSwarmSession
import com.rikodev.sync.nexmesh.core.relay.ledger.UsageCreditLedger
import com.rikodev.sync.nexmesh.core.relay.meter.LocalBandwidthMeter
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger

enum class RelayTunnelState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

@Serializable
data class ZeroKnowledgeEnvelope(
    val sourceNodeId: String,
    val targetNodeId: String,
    val frameSeq: Int,
    val ciphertextB64Url: String // Relay server NEVER sees plaintext or AES-256-GCM keys
)

/**
 * Zero-Knowledge E2EE WebSocket Relay Tunnel for NAT/CGNAT remote swarm traversal.
 * Encrypts every frame locally via AES-256-GCM before transmitting over wss:// and enforces
 * bounded 256KB frame sizes + Pay-Per-GB credit metering.
 */
class ZeroKnowledgeRelayClient(
    private val bandwidthMeter: LocalBandwidthMeter,
    private val creditLedger: UsageCreditLedger
) : Closeable {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val txSeq = AtomicInteger(0)

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingIntervalMillis = 20_000
        }
    }

    private var wsSession: DefaultWebSocketSession? = null
    private var receiveJob: Job? = null

    private val _state = MutableStateFlow(RelayTunnelState.DISCONNECTED)
    val state: StateFlow<RelayTunnelState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _incomingPlaintextFrames = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)
    val incomingPlaintextFrames: SharedFlow<ByteArray> = _incomingPlaintextFrames.asSharedFlow()

    suspend fun connect(
        relayWssUrl: String,
        localNodeId: String,
        e2eeSession: ActiveSwarmSession
    ): Result<Unit> = runCatching {
        require(relayWssUrl.startsWith("wss://") || relayWssUrl.startsWith("ws://")) {
            "Relay endpoint must use wss:// (or ws:// for local testing)"
        }
        disconnect()
        _state.value = RelayTunnelState.CONNECTING
        _lastError.value = null

        val session = client.webSocketSession(urlString = "$relayWssUrl?nodeId=$localNodeId")
        wsSession = session
        _state.value = RelayTunnelState.CONNECTED

        receiveJob = scope.launch {
            runCatching {
                for (frame in session.incoming) {
                    if (!isActive) break
                    if (frame is Frame.Text) {
                        val raw = frame.readText()
                        bandwidthMeter.recordRelayReceived(raw.length.toLong())
                        val env = json.decodeFromString<ZeroKnowledgeEnvelope>(raw)
                        if (!e2eeSession.replayGuard.accept(env.frameSeq.toLong())) continue
                        val cipherBytes = Base64UrlCompat.decode(env.ciphertextB64Url)
                        val aad = "${env.sourceNodeId}:${env.targetNodeId}:${env.frameSeq}".toByteArray()
                        val plain = e2eeSession.cipher.decryptFrame(
                            ciphertextWithTag = cipherBytes,
                            offset = 0,
                            length = cipherBytes.size,
                            chunkIndex = RELAY_CHUNK_CHANNEL,
                            frameSeq = env.frameSeq,
                            aad = aad
                        )
                        _incomingPlaintextFrames.tryEmit(plain)
                    }
                }
            }.onFailure { err ->
                _lastError.value = err.message
                _state.value = RelayTunnelState.ERROR
            }
        }
    }.onFailure { err ->
        _lastError.value = err.message
        _state.value = RelayTunnelState.ERROR
    }

    suspend fun sendEncryptedPayload(
        localNodeId: String,
        targetNodeId: String,
        plaintext: ByteArray,
        e2eeSession: ActiveSwarmSession
    ): Result<Unit> = runCatching {
        require(plaintext.size <= MAX_RELAY_FRAME_BYTES) { "Relay frame exceeds 256KB Zero-OOM limit" }
        val ws = wsSession ?: throw IllegalStateException("Relay tunnel is not connected")
        if (!creditLedger.hasAllowance(plaintext.size.toLong())) {
            throw IllegalStateException("Insufficient Pay-Per-GB relay credit allowance")
        }

        val seq = txSeq.incrementAndGet()
        val aad = "$localNodeId:$targetNodeId:$seq".toByteArray()
        val ciphertext = e2eeSession.cipher.encryptFrame(
            plaintext = plaintext,
            offset = 0,
            length = plaintext.size,
            chunkIndex = RELAY_CHUNK_CHANNEL,
            frameSeq = seq,
            aad = aad
        )

        val envelope = ZeroKnowledgeEnvelope(
            sourceNodeId = localNodeId,
            targetNodeId = targetNodeId,
            frameSeq = seq,
            ciphertextB64Url = Base64UrlCompat.encode(ciphertext)
        )
        val wireText = json.encodeToString(envelope)
        ws.send(Frame.Text(wireText))

        bandwidthMeter.recordRelaySent(wireText.length.toLong())
        creditLedger.consumeRelayBytes(wireText.length.toLong(), "relay_tx_$seq")
    }

    suspend fun disconnect() {
        receiveJob?.cancel()
        receiveJob = null
        runCatching { wsSession?.close(CloseReason(CloseReason.Codes.NORMAL, "User closed relay")) }
        wsSession = null
        _state.value = RelayTunnelState.DISCONNECTED
    }

    override fun close() {
        receiveJob?.cancel()
        client.close()
    }

    companion object {
        const val MAX_RELAY_FRAME_BYTES = 256 * 1024
        private const val RELAY_CHUNK_CHANNEL = 0x7F00
    }
}
