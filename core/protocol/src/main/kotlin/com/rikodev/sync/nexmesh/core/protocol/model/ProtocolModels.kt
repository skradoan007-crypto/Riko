package com.rikodev.sync.nexmesh.core.protocol.model

import kotlinx.serialization.Serializable

@Serializable
data class PairInitRequest(
    val protocolVersion: Int = 1,
    val controllerNodeId: String,
    val controllerDeviceName: String,
    val controllerStaticPubKeyB64: String,
    val controllerEphemeralPubKeyB64: String,
    val nonceB64: String,
    val timestampEpochMs: Long
)

@Serializable
data class PairInitResponse(
    val pairingToken: String,
    val workerNodeId: String,
    val workerDeviceName: String,
    val workerStaticPubKeyB64: String,
    val workerEphemeralPubKeyB64: String,
    val workerNonceB64: String,
    val sasCode: String,
    val expiresAtEpochMs: Long
)

@Serializable
data class PairConfirmRequest(
    val pairingToken: String,
    val controllerNodeId: String,
    val sasApproved: Boolean
)

@Serializable
data class PairConfirmResponse(
    val status: String, // "APPROVED", "PENDING_WORKER_APPROVAL", "REJECTED", "EXPIRED"
    val sessionId: String? = null,
    val sessionExpiresAtEpochMs: Long = 0L
)

@Serializable
data class NodeHealthPayload(
    val nodeId: String,
    val deviceName: String,
    val protocolVersion: Int = 1,
    val batteryPct: Int,
    val isCharging: Boolean,
    val tempCelsius: Float?,
    val thermalThrottled: Boolean,
    val freeStorageBytes: Long,
    val serverPort: Int
)

@Serializable
data class TrustedPeerRecord(
    val nodeId: String,
    val deviceName: String,
    val publicKeyB64Url: String,
    val lastKnownHost: String,
    val lastKnownPort: Int,
    val pairedAtEpochMs: Long
)

@Serializable
data class TransferInitRequest(
    val transferId: String,
    val fileName: String,
    val totalSizeBytes: Long,
    val logicalChunkSizeBytes: Int,
    val totalChunks: Int,
    val expectedFileSha256: String,
    val compressedWithLz4: Boolean
)

@Serializable
data class TransferStatusResponse(
    val transferId: String,
    val completedChunks: List<Int>,
    val receivedBytes: Long,
    val completed: Boolean
)

@Serializable
data class RemoteVaultFileItem(
    val fileId: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val isDirectory: Boolean = false,
    val lastModifiedMs: Long = 0L
)

@Serializable
data class RemoteVaultListResponse(
    val mounted: Boolean,
    val vaultName: String? = null,
    val files: List<RemoteVaultFileItem> = emptyList()
)
