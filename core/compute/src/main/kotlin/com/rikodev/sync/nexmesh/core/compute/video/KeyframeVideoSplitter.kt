package com.rikodev.sync.nexmesh.core.compute.video

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.rikodev.sync.nexmesh.core.protocol.crypto.StreamingSha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

data class VideoSegmentDescriptor(
    val segmentIndex: Int,
    val file: File,
    val startTimeUs: Long,
    val endTimeUs: Long,
    val sizeBytes: Long,
    val sha256: String,
    val hasAudio: Boolean
)

/**
 * Keyframe-Aware MP4 Video Splitter using Android MediaExtractor & MediaMuxer.
 * Splits strictly on video sync-frame (keyframe) boundaries, preserves audio/video timestamps
 * and codec metadata, and uses a bounded 256KB ByteBuffer (Zero-OOM compliant).
 */
class KeyframeVideoSplitter(private val context: Context) {

    suspend fun splitVideoAtKeyframes(
        sourceUri: Uri,
        outputDir: File,
        desiredSegments: Int
    ): Result<List<VideoSegmentDescriptor>> = withContext(Dispatchers.IO) {
        runCatching {
            outputDir.mkdirs()
            val safeSegments = desiredSegments.coerceIn(1, 16)
            val extractor = MediaExtractor()
            val pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
                ?: throw IllegalArgumentException("Unable to open video SAF file descriptor")

            try {
                extractor.setDataSource(pfd.fileDescriptor)
                var videoTrackIdx = -1
                var audioTrackIdx = -1
                var videoFormat: MediaFormat? = null
                var audioFormat: MediaFormat? = null

                for (i in 0 until extractor.trackCount) {
                    val fmt = extractor.getTrackFormat(i)
                    val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/") && videoTrackIdx == -1) {
                        videoTrackIdx = i
                        videoFormat = fmt
                    } else if (mime.startsWith("audio/") && audioTrackIdx == -1) {
                        audioTrackIdx = i
                        audioFormat = fmt
                    }
                }

                require(videoTrackIdx >= 0 && videoFormat != null) { "Input file contains no video track" }
                val totalDurationUs = if (videoFormat.containsKey(MediaFormat.KEY_DURATION)) {
                    videoFormat.getLong(MediaFormat.KEY_DURATION)
                } else {
                    0L
                }
                val targetSegmentDurationUs = if (totalDurationUs > 0L) {
                    (totalDurationUs / safeSegments).coerceAtLeast(1_000_000L)
                } else {
                    10_000_000L
                }

                // Pass 1: Locate keyframe cut timestamps
                extractor.selectTrack(videoTrackIdx)
                val cutPointsUs = mutableListOf(0L)
                var nextTargetUs = targetSegmentDurationUs

                while (true) {
                    coroutineContext.ensureActive()
                    val sampleTime = extractor.sampleTime
                    if (sampleTime < 0L) break
                    val flags = extractor.sampleFlags
                    if ((flags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0 && sampleTime >= nextTargetUs && cutPointsUs.size < safeSegments) {
                        cutPointsUs.add(sampleTime)
                        nextTargetUs = sampleTime + targetSegmentDurationUs
                    }
                    if (!extractor.advance()) break
                }

                // Pass 2: Mux each keyframe-bounded segment with preserved timestamps & audio
                val results = mutableListOf<VideoSegmentDescriptor>()
                val ioBuffer = ByteBuffer.allocateDirect(MAX_SAMPLE_BUFFER_BYTES)
                val bufferInfo = MediaCodec.BufferInfo()

                for (segIdx in cutPointsUs.indices) {
                    coroutineContext.ensureActive()
                    val segStartUs = cutPointsUs[segIdx]
                    val segEndUs = if (segIdx + 1 < cutPointsUs.size) cutPointsUs[segIdx + 1] else Long.MAX_VALUE
                    val outFile = File(outputDir, "segment_%03d.mp4".format(segIdx))

                    val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                    try {
                        val muxVideoTrack = muxer.addTrack(videoFormat)
                        val muxAudioTrack = audioFormat?.let { muxer.addTrack(it) } ?: -1
                        muxer.start()

                        // Copy video samples in [segStartUs, segEndUs)
                        copyTrackRange(
                            extractor = extractor,
                            sourceTrackIdx = videoTrackIdx,
                            muxer = muxer,
                            muxTrackIdx = muxVideoTrack,
                            startUs = segStartUs,
                            endUs = segEndUs,
                            buffer = ioBuffer,
                            bufferInfo = bufferInfo
                        )

                        // Copy matching audio samples in [segStartUs, segEndUs)
                        if (audioTrackIdx >= 0 && muxAudioTrack >= 0) {
                            copyTrackRange(
                                extractor = extractor,
                                sourceTrackIdx = audioTrackIdx,
                                muxer = muxer,
                                muxTrackIdx = muxAudioTrack,
                                startUs = segStartUs,
                                endUs = segEndUs,
                                buffer = ioBuffer,
                                bufferInfo = bufferInfo
                            )
                        }
                        muxer.stop()
                    } finally {
                        runCatching { muxer.release() }
                    }

                    val segSha = outFile.inputStream().use { StreamingSha256.hashStream(it) }
                    results.add(
                        VideoSegmentDescriptor(
                            segmentIndex = segIdx,
                            file = outFile,
                            startTimeUs = segStartUs,
                            endTimeUs = if (segEndUs == Long.MAX_VALUE) totalDurationUs else segEndUs,
                            sizeBytes = outFile.length(),
                            sha256 = segSha,
                            hasAudio = audioTrackIdx >= 0
                        )
                    )
                }
                results
            } finally {
                runCatching { extractor.release() }
                runCatching { pfd.close() }
            }
        }
    }

    private fun copyTrackRange(
        extractor: MediaExtractor,
        sourceTrackIdx: Int,
        muxer: MediaMuxer,
        muxTrackIdx: Int,
        startUs: Long,
        endUs: Long,
        buffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        for (i in 0 until extractor.trackCount) extractor.unselectTrack(i)
        extractor.selectTrack(sourceTrackIdx)
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        var firstPtsUs = -1L
        while (true) {
            buffer.clear()
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            val pts = extractor.sampleTime
            if (pts >= endUs) break
            if (pts >= startUs) {
                if (firstPtsUs < 0L) firstPtsUs = pts
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = (pts - firstPtsUs).coerceAtLeast(0L)
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxTrackIdx, buffer, bufferInfo)
            }
            if (!extractor.advance()) break
        }
    }

    companion object {
        private const val MAX_SAMPLE_BUFFER_BYTES = 256 * 1024
    }
}
