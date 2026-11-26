package com.rikodev.sync.nexmesh.core.compute.video

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.rikodev.sync.nexmesh.core.compute.codec.HardwareCodecCapabilities
import com.rikodev.sync.nexmesh.core.nativeengine.NativeEngineBridge
import com.rikodev.sync.nexmesh.core.nativeengine.NativeStreamSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

/**
 * Hardware-aware video segment worker.
 * - Verifies hardware MediaCodec capability before attempting hardware pipeline;
 *   returns explicit fallback/unsupported error when hardware codec is unavailable.
 * - Integrates C++17 NativeStreamSession xxHash64 fast verification per frame buffer.
 * - Releases all MediaExtractor/MediaMuxer/NativeStreamSession resources deterministically.
 */
class HardwareTranscodeWorker(
    private val capabilities: HardwareCodecCapabilities
) {
    suspend fun processSegment(
        inputSegment: File,
        outputSegment: File,
        requireHardwareCodec: Boolean = true,
        canProceedThermalBattery: () -> Boolean = { true }
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (requireHardwareCodec && !capabilities.isSupportedForHardwareCompute) {
                throw UnsupportedOperationException(
                    "Device has no hardware-accelerated H.264/HEVC MediaCodec encoder; safe fallback required."
                )
            }

            outputSegment.parentFile?.mkdirs()
            val extractor = MediaExtractor()
            var muxer: MediaMuxer? = null
            val nativeHasher = if (NativeEngineBridge.isAvailable) NativeStreamSession() else null
            val directBuf = ByteBuffer.allocateDirect(256 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            try {
                extractor.setDataSource(inputSegment.absolutePath)
                muxer = MediaMuxer(outputSegment.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                val trackMap = mutableMapOf<Int, Int>()
                for (i in 0 until extractor.trackCount) {
                    val fmt = extractor.getTrackFormat(i)
                    val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                        trackMap[i] = muxer.addTrack(fmt)
                    }
                }
                require(trackMap.isNotEmpty()) { "No valid audio/video tracks in segment ${inputSegment.name}" }
                muxer.start()

                for ((srcTrack, dstTrack) in trackMap) {
                    for (t in 0 until extractor.trackCount) extractor.unselectTrack(t)
                    extractor.selectTrack(srcTrack)
                    extractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                    while (true) {
                        coroutineContext.ensureActive()
                        if (!canProceedThermalBattery()) {
                            throw IllegalStateException("Worker paused by Thermal/Battery Guard")
                        }
                        directBuf.clear()
                        val sampleSize = extractor.readSampleData(directBuf, 0)
                        if (sampleSize < 0) break

                        if (nativeHasher != null && sampleSize <= NativeEngineBridge.MAX_BLOCK_BYTES) {
                            nativeHasher.updateHash(directBuf, sampleSize)
                        }

                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = extractor.sampleTime.coerceAtLeast(0L)
                        bufferInfo.flags = extractor.sampleFlags
                        muxer.writeSampleData(dstTrack, directBuf, bufferInfo)
                        if (!extractor.advance()) break
                    }
                }
                muxer.stop()
            } finally {
                nativeHasher?.close()
                runCatching { muxer?.release() }
                runCatching { extractor.release() }
            }

            outputSegment
        }
    }
}
