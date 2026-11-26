package com.rikodev.sync.nexmesh.core.nativeengine

import java.io.Closeable
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class NativeEngineException(val errorCode: Int, message: String) : RuntimeException(message)

/**
 * RAII-style Kotlin wrapper around a native C++17 session handle.
 * Guarantees deterministic native cleanup via Closeable and cooperative cancellation.
 */
class NativeStreamSession(hashSeed: Long = 0L) : Closeable {
    private val closed = AtomicBoolean(false)
    private val handle: Long = if (NativeEngineBridge.isAvailable) {
        NativeEngineBridge.nativeCreateSession(hashSeed)
    } else {
        0L
    }

    val isNativeActive: Boolean get() = handle != 0L && !closed.get()

    fun compress(src: ByteBuffer, srcLength: Int, dst: ByteBuffer): Int {
        ensureOpen()
        require(src.isDirect && dst.isDirect) { "NativeStreamSession requires DirectByteBuffers" }
        require(srcLength in 0..NativeEngineBridge.MAX_BLOCK_BYTES) {
            "Block size $srcLength exceeds Zero-OOM limit ${NativeEngineBridge.MAX_BLOCK_BYTES}"
        }
        val result = NativeEngineBridge.nativeCompressDirect(handle, src, srcLength, dst, dst.capacity())
        if (result < 0) throw NativeEngineException(result, "Native LZ4 compress failed (code=$result)")
        dst.position(0)
        dst.limit(result)
        return result
    }

    fun decompress(src: ByteBuffer, srcLength: Int, dst: ByteBuffer): Int {
        ensureOpen()
        require(src.isDirect && dst.isDirect) { "NativeStreamSession requires DirectByteBuffers" }
        val result = NativeEngineBridge.nativeDecompressDirect(handle, src, srcLength, dst, dst.capacity())
        if (result < 0) throw NativeEngineException(result, "Native LZ4 decompress failed (code=$result)")
        dst.position(0)
        dst.limit(result)
        return result
    }

    fun updateHash(data: ByteBuffer, length: Int) {
        ensureOpen()
        require(data.isDirect) { "NativeStreamSession requires DirectByteBuffer" }
        val status = NativeEngineBridge.nativeHashUpdateDirect(handle, data, length)
        if (status < 0) throw NativeEngineException(status, "Native hash update failed (code=$status)")
    }

    fun digestXxHash64(): Long {
        ensureOpen()
        return NativeEngineBridge.nativeHashDigestXx64(handle)
    }

    fun digestCrc32c(): Int {
        ensureOpen()
        return NativeEngineBridge.nativeHashDigestCrc32c(handle)
    }

    fun cancel() {
        if (!closed.get() && handle != 0L) {
            NativeEngineBridge.nativeCancelSession(handle)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true) && handle != 0L) {
            NativeEngineBridge.nativeDestroySession(handle)
        }
    }

    private fun ensureOpen() {
        check(!closed.get()) { "NativeStreamSession already closed" }
        check(handle != 0L) { "Native library libnexmesh_native.so is unavailable on this runtime" }
    }
}
