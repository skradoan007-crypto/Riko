package com.rikodev.music.android.audiofx

import android.content.Context
import android.net.Uri
import java.nio.ByteBuffer

/** Offline source contract. Returns only after complete EOS; failures/cancellation propagate.
 * Buffers are borrowed until onPcm returns, little-endian, interleaved and timestamped
 * relative to the selected audio track's first sample. Never retain or mutate them.
 */
interface PcmSource {
    suspend fun decode(context: Context, uri: Uri, durationHintMs: Long = 0L,
                       consumer: PcmConsumer): Long
}

interface PcmConsumer {
    fun onStart(durationUs: Long)
    fun onPcm(buffer: ByteBuffer, offset: Int, size: Int, ptsUs: Long,
              sampleRate: Int, channels: Int, encoding: Int, checkCancelled: () -> Unit)
}
