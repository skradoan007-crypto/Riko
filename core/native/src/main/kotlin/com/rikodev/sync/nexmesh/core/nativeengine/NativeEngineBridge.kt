package com.rikodev.sync.nexmesh.core.nativeengine

import java.nio.ByteBuffer

/**
 * Low-level JNI bridge for Nex Mesh C++17 native engine (libnexmesh_native.so).
 * All data paths require DirectByteBuffer to avoid JNI heap copies.
 */
object NativeEngineBridge {
    const val MAX_BLOCK_BYTES: Int = 256 * 1024
    const val DEFAULT_BLOCK_BYTES: Int = 128 * 1024

    val isAvailable: Boolean = try {
        System.loadLibrary("nexmesh_native")
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    }

    fun maxCompressedCapacity(inputSize: Int): Int = inputSize + (inputSize / 255) + 16

    external fun nativeCreateSession(hashSeed: Long): Long
    external fun nativeCancelSession(handle: Long)
    external fun nativeDestroySession(handle: Long)
    external fun nativeCompressDirect(
        handle: Long,
        srcBuffer: ByteBuffer,
        srcLength: Int,
        dstBuffer: ByteBuffer,
        dstCapacity: Int
    ): Int

    external fun nativeDecompressDirect(
        handle: Long,
        srcBuffer: ByteBuffer,
        srcLength: Int,
        dstBuffer: ByteBuffer,
        dstCapacity: Int
    ): Int

    external fun nativeHashUpdateDirect(
        handle: Long,
        dataBuffer: ByteBuffer,
        length: Int
    ): Int

    external fun nativeHashDigestXx64(handle: Long): Long
    external fun nativeHashDigestCrc32c(handle: Long): Int
    external fun nativeEngineInfo(): String
}
