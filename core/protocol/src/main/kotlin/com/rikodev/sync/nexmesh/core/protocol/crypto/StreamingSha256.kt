package com.rikodev.sync.nexmesh.core.protocol.crypto

import java.io.InputStream
import java.security.MessageDigest

/**
 * Zero-OOM streaming SHA-256 helper operating strictly on 128KB–256KB buffers.
 */
class StreamingSha256 {
    private val digest: MessageDigest = MessageDigest.getInstance("SHA-256")

    fun update(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size) {
        if (length > 0) {
            digest.update(buffer, offset, length)
        }
    }

    fun digestHex(): String = digest.digest().joinToString("") { "%02x".format(it) }

    fun reset() {
        digest.reset()
    }

    companion object {
        const val DEFAULT_BUFFER_BYTES = 128 * 1024

        fun hashStream(
            input: InputStream,
            bufferSize: Int = DEFAULT_BUFFER_BYTES,
            shouldCancel: () -> Boolean = { false }
        ): String {
            val safeBufSize = bufferSize.coerceIn(32 * 1024, 256 * 1024)
            val buffer = ByteArray(safeBufSize)
            val sha = StreamingSha256()
            while (true) {
                if (shouldCancel()) throw InterruptedException("SHA-256 hashing cancelled")
                val read = input.read(buffer)
                if (read == -1) break
                sha.update(buffer, 0, read)
            }
            return sha.digestHex()
        }
    }
}
