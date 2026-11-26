package com.rikodev.sync.nexmesh.core.nativeengine

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class NativeEngineInstrumentedTest {

    @Test
    fun lz4StreamingRoundTripPreservesExactBytes() {
        assertTrue("libnexmesh_native.so must load on ARMv7/ARM64", NativeEngineBridge.isAvailable)

        val size = 128 * 1024
        val original = ByteArray(size) { i -> ((i / 7 + i % 19) and 0xFF).toByte() }
        val src = ByteBuffer.allocateDirect(size).apply { put(original); flip() }
        val compressed = ByteBuffer.allocateDirect(NativeEngineBridge.maxCompressedCapacity(size))
        val restored = ByteBuffer.allocateDirect(size)

        NativeStreamSession().use { session ->
            val compLen = session.compress(src, size, compressed)
            assertTrue("Structured payload should compress", compLen < size)

            val decompLen = session.decompress(compressed, compLen, restored)
            assertEquals(size, decompLen)

            val outBytes = ByteArray(decompLen)
            restored.get(outBytes)
            assertArrayEquals(original, outBytes)
        }
    }

    @Test
    fun streamingXxHash64IsDeterministicAcrossChunkBoundaries() {
        val fullSize = 64 * 1024
        val data = ByteArray(fullSize) { (it * 13 and 0xFF).toByte() }
        val fullBuf = ByteBuffer.allocateDirect(fullSize).apply { put(data); flip() }

        val fullDigest = NativeStreamSession(42L).use { s ->
            s.updateHash(fullBuf, fullSize)
            s.digestXxHash64()
        }

        val half = fullSize / 2
        val part1 = ByteBuffer.allocateDirect(half).apply { put(data, 0, half); flip() }
        val part2 = ByteBuffer.allocateDirect(half).apply { put(data, half, half); flip() }
        val streamedDigest = NativeStreamSession(42L).use { s ->
            s.updateHash(part1, half)
            s.updateHash(part2, half)
            s.digestXxHash64()
        }

        assertEquals(fullDigest, streamedDigest)
    }
}
