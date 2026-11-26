package com.rikodev.sync.nexmesh.core.compute.video

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.rikodev.sync.nexmesh.core.protocol.crypto.StreamingSha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

data class ReassembledVideoResult(
    val outputFile: File,
    val totalSizeBytes: Long,
    val sha256: String,
    val segmentCount: Int
)

/**
 * Reassembles processed MP4 video segments in strict index order using MediaExtractor + MediaMuxer
 * with monotonic timestamp stitching and Zero-OOM Streaming SHA-256 verification.
 */
class VideoSegmentReassembler {

    suspend fun reassemble(
        orderedSegmentFiles: List<File>,
        outputFile: File
    ): Result<ReassembledVideoResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(orderedSegmentFiles.isNotEmpty()) { "No video segments provided for reassembly" }
            outputFile.parentFile?.mkdirs()

            // Probe first segment for track formats
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null
            val probe = MediaExtractor()
            try {
                probe.setDataSource(orderedSegmentFiles.first().absolutePath)
                for (i in 0 until probe.trackCount) {
                    val fmt = probe.getTrackFormat(i)
                    val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/") && videoFormat == null) videoFormat = fmt
                    if (mime.startsWith("audio/") && audioFormat == null) audioFormat = fmt
                }
            } finally {
                runCatching { probe.release() }
            }

            require(videoFormat != null) { "First segment has no valid video track" }

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val buffer = ByteBuffer.allocateDirect(256 * 1024)
            val info = MediaCodec.BufferInfo()

            try {
                val outVideoTrack = muxer.addTrack(videoFormat)
                val outAudioTrack = audioFormat?.let { muxer.addTrack(it) } ?: -1
                muxer.start()

                var videoPtsOffsetUs = 0L
                var audioPtsOffsetUs = 0L

                for (segFile in orderedSegmentFiles) {
                    coroutineContext.ensureActive()
                    val ext = MediaExtractor()
                    try {
                        ext.setDataSource(segFile.absolutePath)
                        var vTrack = -1
                        var aTrack = -1
                        for (i in 0 until ext.trackCount) {
                            val m = ext.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
                            if (m.startsWith("video/") && vTrack == -1) vTrack = i
                            if (m.startsWith("audio/") && aTrack == -1) aTrack = i
                        }

                        if (vTrack >= 0) {
                            videoPtsOffsetUs = appendTrack(ext, vTrack, muxer, outVideoTrack, videoPtsOffsetUs, buffer, info)
                        }
                        if (aTrack >= 0 && outAudioTrack >= 0) {
                            audioPtsOffsetUs = appendTrack(ext, aTrack, muxer, outAudioTrack, audioPtsOffsetUs, buffer, info)
                        }
                    } finally {
                        runCatching { ext.release() }
                    }
                }
                muxer.stop()
            } finally {
                runCatching { muxer.release() }
            }

            val finalSha = outputFile.inputStream().use { StreamingSha256.hashStream(it) }
            ReassembledVideoResult(
                outputFile = outputFile,
                totalSizeBytes = outputFile.length(),
                sha256 = finalSha,
                segmentCount = orderedSegmentFiles.size
            )
        }
    }

    private fun appendTrack(
        ext: MediaExtractor,
        srcTrack: Int,
        muxer: MediaMuxer,
        dstTrack: Int,
        baseOffsetUs: Long,
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo
    ): Long {
        for (i in 0 until ext.trackCount) ext.unselectTrack(i)
        ext.selectTrack(srcTrack)
        ext.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        var maxPts = baseOffsetUs
        while (true) {
            buffer.clear()
            val read = ext.readSampleData(buffer, 0)
            if (read < 0) break
            val pts = baseOffsetUs + ext.sampleTime.coerceAtLeast(0L)
            info.offset = 0
            info.size = read
            info.presentationTimeUs = pts
            info.flags = ext.sampleFlags
            muxer.writeSampleData(dstTrack, buffer, info)
            if (pts > maxPts) maxPts = pts
            if (!ext.advance()) break
        }
        return maxPts + 33_333L // +1 frame guard gap (~30fps)
    }
}
