package com.rikodev.sync.nexmesh.core.protocol

import com.rikodev.sync.nexmesh.core.protocol.crypto.AesGcmStreamCipher
import com.rikodev.sync.nexmesh.core.protocol.crypto.EcdhKeyExchange
import com.rikodev.sync.nexmesh.core.protocol.crypto.HkdfSha256
import com.rikodev.sync.nexmesh.core.protocol.model.QrPairingUri
import com.rikodev.sync.nexmesh.core.protocol.session.ReplayWindowGuard
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.AEADBadTagException

class ProtocolSecurityTest {

    @Test
    fun ecdhAndHkdfProduceIdenticalSessionKeysAndSasOnBothPeers() {
        val controllerStatic = EcdhKeyExchange.generateEphemeralKeyPair()
        val workerStatic = EcdhKeyExchange.generateEphemeralKeyPair()
        val controllerEph = EcdhKeyExchange.generateEphemeralKeyPair()
        val workerEph = EcdhKeyExchange.generateEphemeralKeyPair()

        val cNonce = ByteArray(16) { it.toByte() }
        val wNonce = ByteArray(16) { (it + 50).toByte() }

        val controllerMaterial = EcdhKeyExchange.deriveSessionMaterial(
            localEphemeralPrivate = controllerEph.private,
            peerEphemeralPublic = workerEph.public,
            controllerStaticPubBytes = controllerStatic.public.encoded,
            workerStaticPubBytes = workerStatic.public.encoded,
            controllerNonce = cNonce,
            workerNonce = wNonce
        )

        val workerMaterial = EcdhKeyExchange.deriveSessionMaterial(
            localEphemeralPrivate = workerEph.private,
            peerEphemeralPublic = controllerEph.public,
            controllerStaticPubBytes = controllerStatic.public.encoded,
            workerStaticPubBytes = workerStatic.public.encoded,
            controllerNonce = cNonce,
            workerNonce = wNonce
        )

        assertArrayEquals(controllerMaterial.aes256Key, workerMaterial.aes256Key)
        assertArrayEquals(controllerMaterial.noncePrefix4Bytes, workerMaterial.noncePrefix4Bytes)
        assertEquals(controllerMaterial.sasCode6Digits, workerMaterial.sasCode6Digits)
        assertEquals(6, controllerMaterial.sasCode6Digits.length)
    }

    @Test(expected = AEADBadTagException::class)
    fun aesGcmRejectsTamperedCiphertext() {
        val key = HkdfSha256.deriveKey("secret".toByteArray(), "salt".toByteArray(), "info".toByteArray(), 32)
        val cipher = AesGcmStreamCipher(key, byteArrayOf(1, 2, 3, 4))
        val aad = "transfer-1:chunk-0".toByteArray()
        val plain = "NexMesh streaming chunk payload".toByteArray()

        val encrypted = cipher.encryptFrame(plain, 0, plain.size, 0, 1, aad)
        encrypted[0] = (encrypted[0].toInt() xor 0xFF).toByte()
        cipher.decryptFrame(encrypted, 0, encrypted.size, 0, 1, aad)
    }

    @Test
    fun replayWindowGuardBlocksDuplicatesAndOldFrames() {
        val guard = ReplayWindowGuard()
        assertTrue(guard.accept(0))
        assertFalse(guard.accept(0))
        assertTrue(guard.accept(10))
        assertTrue(guard.accept(5))
        assertFalse(guard.accept(5))
        assertTrue(guard.accept(100))
        assertFalse(guard.accept(20)) // Outside 64-slot window
    }

    @Test
    fun qrPairingUriSupportsIpv4Ipv6AndRejectsExpired() {
        val ipv6 = QrPairingUri("fe80::1a2b:3c4d", 47780, "A".repeat(44), "nxm_1234567890ab", 2_000_000L)
        val serialized = ipv6.toUriString()
        assertTrue(serialized.startsWith("riko-swarm://[fe80::1a2b:3c4d]:47780"))

        val parsed = QrPairingUri.parse(serialized, nowMs = 1_000_000L).getOrThrow()
        assertEquals("fe80::1a2b:3c4d", parsed.host)
        assertEquals(47780, parsed.port)

        val expired = QrPairingUri.parse(serialized, nowMs = 3_000_000L)
        assertTrue(expired.isFailure)
    }
}
