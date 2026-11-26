package com.rikodev.sync.nexmesh.core.protocol.crypto

import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

data class DerivedSessionMaterial(
    val aes256Key: ByteArray,
    val noncePrefix4Bytes: ByteArray,
    val sasCode6Digits: String
)

/**
 * Pure Kotlin RFC 4648 Base64URL encoder/decoder — 100% compatible with Android API 24+ and JVM tests
 * without requiring java.util.Base64 (API 26+) or Robolectric.
 */
object Base64UrlCompat {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val DECODE_TABLE = IntArray(128) { -1 }.apply {
        ALPHABET.forEachIndexed { idx, ch -> this[ch.code] = idx }
        this['+'.code] = 62
        this['/'.code] = 63
    }

    fun encode(data: ByteArray): String {
        val sb = StringBuilder(((data.size + 2) / 3) * 4)
        var i = 0
        while (i < data.size) {
            val b0 = data[i++].toInt() and 0xFF
            val hasB1 = i < data.size
            val b1 = if (hasB1) data[i++].toInt() and 0xFF else 0
            val hasB2 = i < data.size
            val b2 = if (hasB2) data[i++].toInt() and 0xFF else 0

            sb.append(ALPHABET[b0 ushr 2])
            sb.append(ALPHABET[((b0 and 0x03) shl 4) or (b1 ushr 4)])
            if (hasB1) sb.append(ALPHABET[((b1 and 0x0F) shl 2) or (b2 ushr 6)])
            if (hasB2) sb.append(ALPHABET[b2 and 0x3F])
        }
        return sb.toString()
    }

    fun decode(input: String): ByteArray {
        val clean = input.trim().trimEnd('=')
        val out = ByteArray((clean.length * 3) / 4)
        var outIdx = 0
        var i = 0
        while (i < clean.length) {
            val c0 = decodeChar(clean[i++])
            val c1 = if (i < clean.length) decodeChar(clean[i++]) else 0
            val hasC2 = i < clean.length
            val c2 = if (hasC2) decodeChar(clean[i++]) else 0
            val hasC3 = i < clean.length
            val c3 = if (hasC3) decodeChar(clean[i++]) else 0

            if (outIdx < out.size) out[outIdx++] = ((c0 shl 2) or (c1 ushr 4)).toByte()
            if (hasC2 && outIdx < out.size) out[outIdx++] = (((c1 and 0x0F) shl 4) or (c2 ushr 2)).toByte()
            if (hasC3 && outIdx < out.size) out[outIdx++] = (((c2 and 0x03) shl 6) or c3).toByte()
        }
        return if (outIdx == out.size) out else out.copyOf(outIdx)
    }

    private fun decodeChar(ch: Char): Int {
        val code = ch.code
        val v = if (code < 128) DECODE_TABLE[code] else -1
        require(v >= 0) { "Invalid Base64URL character: '$ch'" }
        return v
    }
}

/**
 * ECDH P-256 Ephemeral + Identity Key Exchange and 6-digit SAS derivation.
 */
object EcdhKeyExchange {
    private const val CURVE_NAME = "secp256r1"
    private val HKDF_INFO = "NexMesh-RIKO-Swarm-Phase1-Session-v1".toByteArray(Charsets.UTF_8)

    fun generateEphemeralKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec(CURVE_NAME))
        return kpg.generateKeyPair()
    }

    fun encodePublicKeyB64Url(publicKey: PublicKey): String = Base64UrlCompat.encode(publicKey.encoded)

    fun decodePublicKeyB64Url(encoded: String): PublicKey {
        val bytes = Base64UrlCompat.decode(encoded)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(X509EncodedKeySpec(bytes))
    }

    fun computeSharedSecret(privateKey: PrivateKey, peerPublicKey: PublicKey): ByteArray {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(peerPublicKey, true)
        return agreement.generateSecret()
    }

    fun deriveSessionMaterial(
        localEphemeralPrivate: PrivateKey,
        peerEphemeralPublic: PublicKey,
        controllerStaticPubBytes: ByteArray,
        workerStaticPubBytes: ByteArray,
        controllerNonce: ByteArray,
        workerNonce: ByteArray
    ): DerivedSessionMaterial {
        val ecdhSecret = computeSharedSecret(localEphemeralPrivate, peerEphemeralPublic)

        val salt = ByteArray(controllerNonce.size + workerNonce.size + controllerStaticPubBytes.size + workerStaticPubBytes.size)
        var pos = 0
        System.arraycopy(controllerNonce, 0, salt, pos, controllerNonce.size); pos += controllerNonce.size
        System.arraycopy(workerNonce, 0, salt, pos, workerNonce.size); pos += workerNonce.size
        System.arraycopy(controllerStaticPubBytes, 0, salt, pos, controllerStaticPubBytes.size); pos += controllerStaticPubBytes.size
        System.arraycopy(workerStaticPubBytes, 0, salt, pos, workerStaticPubBytes.size)

        val okm = HkdfSha256.deriveKey(ikm = ecdhSecret, salt = salt, info = HKDF_INFO, length = 40)
        val aesKey = okm.copyOfRange(0, 32)
        val noncePrefix = okm.copyOfRange(32, 36)
        val sasInt = ByteBuffer.wrap(okm, 36, 4).int and 0x7FFFFFFF
        val sasCode = String.format("%06d", sasInt % 1_000_000)

        return DerivedSessionMaterial(
            aes256Key = aesKey,
            noncePrefix4Bytes = noncePrefix,
            sasCode6Digits = sasCode
        )
    }
}
