package com.rikodev.sync.nexmesh.core.protocol.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RFC 5869 HKDF-SHA256 implementation using Android's standard JCA HmacSHA256.
 */
object HkdfSha256 {
    private const val HMAC_ALGO = "HmacSHA256"
    private const val HASH_LEN = 32

    fun extract(salt: ByteArray?, ikm: ByteArray): ByteArray {
        val actualSalt = if (salt == null || salt.isEmpty()) ByteArray(HASH_LEN) else salt
        val mac = Mac.getInstance(HMAC_ALGO)
        mac.init(SecretKeySpec(actualSalt, HMAC_ALGO))
        return mac.doFinal(ikm)
    }

    fun expand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length in 1..(255 * HASH_LEN)) { "Invalid HKDF output length: $length" }
        val mac = Mac.getInstance(HMAC_ALGO)
        mac.init(SecretKeySpec(prk, HMAC_ALGO))

        val result = ByteArray(length)
        var t = ByteArray(0)
        var offset = 0
        var counter = 1

        while (offset < length) {
            mac.reset()
            mac.update(t)
            mac.update(info)
            mac.update(counter.toByte())
            t = mac.doFinal()

            val copyLen = minOf(t.size, length - offset)
            System.arraycopy(t, 0, result, offset, copyLen)
            offset += copyLen
            counter++
        }
        return result
    }

    fun deriveKey(ikm: ByteArray, salt: ByteArray?, info: ByteArray, length: Int = 32): ByteArray {
        val prk = extract(salt, ikm)
        return expand(prk, info, length)
    }
}
