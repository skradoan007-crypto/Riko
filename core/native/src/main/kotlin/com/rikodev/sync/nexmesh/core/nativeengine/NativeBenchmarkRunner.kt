package com.rikodev.sync.nexmesh.core.nativeengine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Deflater
import kotlin.coroutines.coroutineContext

data class NativeBenchmarkResult(
    val engineAbi: String,
    val totalMegabytes: Double,
    val nativeLz4Mbps: Double,
    val jvmDeflaterMbps: Double,
    val nativeHashMbps: Double,
    val jvmCrc32Mbps: Double,
    val compressionRatio: Double,
    val nativeAvailable: Boolean
)

/**
 * Benchmarks C++17 streaming LZ4 + xxHash64/CRC32C against JVM Deflater + CRC32
 * using bounded 128KB DirectByteBuffers (Zero-OOM compliant).
 */
class NativeBenchmarkRunner {

    suspend fun runStreamingBenchmark(
        blockSize: Int = NativeEngineBridge.DEFAULT_BLOCK_BYTES,
        iterations: Int = 64
    ): NativeBenchmarkResult = withContext(Dispatchers.Default) {
        val safeBlock = blockSize.coerceIn(16 * 1024, NativeEngineBridge.MAX_BLOCK_BYTES)
        val totalBytes = safeBlock.toLong() * iterations
        val totalMb = totalBytes / (1024.0 * 1024.0)

        // Generate realistic structured payload (mixed text/telemetry + binary)
        val sampleBytes = ByteArray(safeBlock) { idx ->
            if (idx % 64 < 40) ((idx % 26) + 65).toByte() else ((idx * 31) and 0xFF).toByte()
        }
        val srcDirect = ByteBuffer.allocateDirect(safeBlock).apply {
            put(sampleBytes)
            flip()
        }
        val dstDirect = ByteBuffer.allocateDirect(NativeEngineBridge.maxCompressedCapacity(safeBlock))

        // 1. JVM Deflater baseline
        val jvmOut = ByteArray(safeBlock + 1024)
        val deflater = Deflater(Deflater.BEST_SPEED)
        val jvmCompStart = System.nanoTime()
        repeat(iterations) {
            coroutineContext.ensureActive()
            deflater.reset()
            deflater.setInput(sampleBytes)
            deflater.finish()
            deflater.deflate(jvmOut)
        }
        deflater.end()
        val jvmCompSec = ((System.nanoTime() - jvmCompStart).coerceAtLeast(1L)) / 1e9
        val jvmDeflaterMbps = totalMb / jvmCompSec

        // 2. JVM CRC32 baseline
        val crc32 = CRC32()
        val jvmHashStart = System.nanoTime()
        repeat(iterations) {
            coroutineContext.ensureActive()
            crc32.update(sampleBytes, 0, safeBlock)
        }
        val jvmHashSec = ((System.nanoTime() - jvmHashStart).coerceAtLeast(1L)) / 1e9
        val jvmCrc32Mbps = totalMb / jvmHashSec

        if (!NativeEngineBridge.isAvailable) {
            return@withContext NativeBenchmarkResult(
                engineAbi = "JVM-Only (Native Unavailable)",
                totalMegabytes = totalMb,
                nativeLz4Mbps = 0.0,
                jvmDeflaterMbps = jvmDeflaterMbps,
                nativeHashMbps = 0.0,
                jvmCrc32Mbps = jvmCrc32Mbps,
                compressionRatio = 1.0,
                nativeAvailable = false
            )
        }

        var compressedBytesTotal = 0L
        val nativeCompStart = System.nanoTime()
        NativeStreamSession().use { session ->
            repeat(iterations) {
                coroutineContext.ensureActive()
                compressedBytesTotal += session.compress(srcDirect, safeBlock, dstDirect)
            }
        }
        val nativeCompSec = ((System.nanoTime() - nativeCompStart).coerceAtLeast(1L)) / 1e9
        val nativeLz4Mbps = totalMb / nativeCompSec

        val nativeHashStart = System.nanoTime()
        NativeStreamSession().use { session ->
            repeat(iterations) {
                coroutineContext.ensureActive()
                session.updateHash(srcDirect, safeBlock)
            }
            session.digestXxHash64()
        }
        val nativeHashSec = ((System.nanoTime() - nativeHashStart).coerceAtLeast(1L)) / 1e9
        val nativeHashMbps = totalMb / nativeHashSec

        NativeBenchmarkResult(
            engineAbi = NativeEngineBridge.nativeEngineInfo(),
            totalMegabytes = totalMb,
            nativeLz4Mbps = nativeLz4Mbps,
            jvmDeflaterMbps = jvmDeflaterMbps,
            nativeHashMbps = nativeHashMbps,
            jvmCrc32Mbps = jvmCrc32Mbps,
            compressionRatio = compressedBytesTotal.toDouble() / totalBytes.toDouble(),
            nativeAvailable = true
        )
    }
}
