package com.rikodev.sync.nexmesh.core.compute.codec

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build

data class HardwareCodecCapabilities(
    val hasHardwareH264Encoder: Boolean,
    val hasHardwareH264Decoder: Boolean,
    val hasHardwareHevcEncoder: Boolean,
    val hasHardwareHevcDecoder: Boolean,
    val preferredH264EncoderName: String?,
    val preferredHevcEncoderName: String?
) {
    val isSupportedForHardwareCompute: Boolean
        get() = hasHardwareH264Encoder || hasHardwareHevcEncoder
}

/**
 * Inspects real device MediaCodecList for hardware-accelerated H.264 & HEVC support.
 * Explicitly reports unsupported status rather than silently pretending software codecs are hardware.
 */
object HardwareCodecInspector {

    fun inspect(): HardwareCodecCapabilities {
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos

        var h264Enc: String? = null
        var h264Dec = false
        var hevcEnc: String? = null
        var hevcDec = false

        for (info in codecs) {
            if (!isHardwareAcceleratedCodec(info)) continue
            for (type in info.supportedTypes) {
                when {
                    type.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) -> {
                        if (info.isEncoder && h264Enc == null) h264Enc = info.name
                        if (!info.isEncoder) h264Dec = true
                    }
                    type.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true) -> {
                        if (info.isEncoder && hevcEnc == null) hevcEnc = info.name
                        if (!info.isEncoder) hevcDec = true
                    }
                }
            }
        }

        return HardwareCodecCapabilities(
            hasHardwareH264Encoder = h264Enc != null,
            hasHardwareH264Decoder = h264Dec,
            hasHardwareHevcEncoder = hevcEnc != null,
            hasHardwareHevcDecoder = hevcDec,
            preferredH264EncoderName = h264Enc,
            preferredHevcEncoderName = hevcEnc
        )
    }

    private fun isHardwareAcceleratedCodec(info: MediaCodecInfo): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return info.isHardwareAccelerated && !info.isSoftwareOnly
        }
        val lower = info.name.lowercase()
        val isSoftware = lower.startsWith("omx.google.") ||
            lower.startsWith("c2.android.") ||
            lower.contains(".sw.") ||
            lower.contains("ffmpeg")
        return !isSoftware
    }
}
