package com.rikodev.sync.nexmesh.core.protocol.session

import com.rikodev.sync.nexmesh.core.protocol.crypto.AesGcmStreamCipher
import com.rikodev.sync.nexmesh.core.protocol.crypto.DerivedSessionMaterial
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ActiveSwarmSession(
    val sessionId: String,
    val peerNodeId: String,
    val peerDeviceName: String,
    val cipher: AesGcmStreamCipher,
    val replayGuard: ReplayWindowGuard,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long
) {
    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean = nowMs > expiresAtEpochMs
}

data class PendingPairingChallenge(
    val pairingToken: String,
    val peerNodeId: String,
    val peerDeviceName: String,
    val peerStaticPubKeyB64: String,
    val sasCode: String,
    val derivedMaterial: DerivedSessionMaterial,
    val expiresAtEpochMs: Long,
    var localUserApproved: Boolean = false,
    var localUserRejected: Boolean = false
)

/**
 * Coordinates pending SAS user-approval pairing challenges and active E2EE sessions.
 */
class SessionManager(
    private val sessionTtlMs: Long = 6L * 60L * 60L * 1000L // 6 hours
) {
    private val pendingChallenges = ConcurrentHashMap<String, PendingPairingChallenge>()
    private val activeSessions = ConcurrentHashMap<String, ActiveSwarmSession>()

    fun registerPendingChallenge(
        peerNodeId: String,
        peerDeviceName: String,
        peerStaticPubKeyB64: String,
        material: DerivedSessionMaterial,
        ttlMs: Long = 120_000L
    ): PendingPairingChallenge {
        val token = UUID.randomUUID().toString()
        val challenge = PendingPairingChallenge(
            pairingToken = token,
            peerNodeId = peerNodeId,
            peerDeviceName = peerDeviceName,
            peerStaticPubKeyB64 = peerStaticPubKeyB64,
            sasCode = material.sasCode6Digits,
            derivedMaterial = material,
            expiresAtEpochMs = System.currentTimeMillis() + ttlMs
        )
        pendingChallenges[token] = challenge
        return challenge
    }

    fun getPendingChallenge(token: String): PendingPairingChallenge? {
        val c = pendingChallenges[token] ?: return null
        if (System.currentTimeMillis() > c.expiresAtEpochMs) {
            pendingChallenges.remove(token)
            return null
        }
        return c
    }

    fun listPendingChallenges(): List<PendingPairingChallenge> {
        val now = System.currentTimeMillis()
        pendingChallenges.entries.removeIf { now > it.value.expiresAtEpochMs }
        return pendingChallenges.values.toList()
    }

    fun setLocalDecision(token: String, approved: Boolean): PendingPairingChallenge? {
        val challenge = getPendingChallenge(token) ?: return null
        if (approved) {
            challenge.localUserApproved = true
        } else {
            challenge.localUserRejected = true
        }
        return challenge
    }

    fun promoteToActiveSession(
        peerNodeId: String,
        peerDeviceName: String,
        material: DerivedSessionMaterial,
        sessionId: String = UUID.randomUUID().toString()
    ): ActiveSwarmSession {
        val now = System.currentTimeMillis()
        val session = ActiveSwarmSession(
            sessionId = sessionId,
            peerNodeId = peerNodeId,
            peerDeviceName = peerDeviceName,
            cipher = AesGcmStreamCipher(material.aes256Key, material.noncePrefix4Bytes),
            replayGuard = ReplayWindowGuard(),
            createdAtEpochMs = now,
            expiresAtEpochMs = now + sessionTtlMs
        )
        activeSessions[sessionId] = session
        return session
    }

    fun getActiveSession(sessionId: String): ActiveSwarmSession? {
        val session = activeSessions[sessionId] ?: return null
        if (session.isExpired()) {
            activeSessions.remove(sessionId)
            return null
        }
        return session
    }

    fun removePendingChallenge(token: String) {
        pendingChallenges.remove(token)
    }

    fun clearAll() {
        pendingChallenges.clear()
        activeSessions.clear()
    }
}
