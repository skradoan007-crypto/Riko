package com.rikodev.sync.nexmesh.core.protocol.crypto

import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Per-frame AES-256-GCM authenticated encryption with deterministic unique 96-bit nonces
 * (4-byte session prefix + 4-byte chunkIndex + 4-byte frameSequence) and AAD binding.
 */
class AesGcmStreamCipher(
    aes256Key: ByteArray,
    private val sessionNoncePrefix: ByteArray
) {
    init {
        require(aes256Key.size == 32) { "AES-256 key must be exactly 32 bytes" }
        require(sessionNoncePrefix.size == 4) { "Session nonce prefix must be exactly 4 bytes" }
    }

    private val keySpec = SecretKeySpec(aes256Key.copyOf(), "AES")

    fun buildNonce(chunkIndex: Int, frameSeq: Int): ByteArray {
        val buf = ByteBuffer.allocate(NONCE_SIZE_BYTES)
        buf.put(sessionNoncePrefix)
        buf.putInt(chunkIndex)
        buf.putInt(frameSeq)
        return buf.array()
    }

    fun encryptFrame(
        plaintext: ByteArray,
        offset: Int,
        length: Int,
        chunkIndex: Int,
        frameSeq: Int,
        aad: ByteArray
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val nonce = buildNonce(chunkIndex, frameSeq)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(plaintext, offset, length)
    }

    fun decryptFrame(
        ciphertextWithTag: ByteArray,
        offset: Int,
        length: Int,
        chunkIndex: Int,
        frameSeq: Int,
        aad: ByteArray
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val nonce = buildNonce(chunkIndex, frameSeq)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(ciphertextWithTag, offset, length)
    }

    companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val NONCE_SIZE_BYTES = 12
        const val TAG_BITS = 128
        const val TAG_BYTES = 16
    }
}
