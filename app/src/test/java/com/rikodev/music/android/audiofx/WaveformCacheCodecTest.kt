package com.rikodev.music.android.audiofx

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class WaveformCacheCodecTest {
    @Test fun roundTripIsCompactLocaleIndependentAndVersioned() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val data = WaveformData(FloatArray(1024) { it / 1024f }, FloatArray(1024) { it / 2048f }, 1024, 180_000_000)
            val payload = WaveformCacheCodec.encode("identity", data)
            assertTrue(payload.length < 5600)
            val decoded = WaveformCacheCodec.decode("identity", payload)!!
            assertArrayEquals(data.peaks, decoded.peaks, 1f / 65535f)
            assertArrayEquals(data.rms, decoded.rms, 1f / 65535f)
            assertEquals(data.durationUs, decoded.durationUs)
            assertNull(WaveformCacheCodec.decode("changed-file", payload))
            assertNull(WaveformCacheCodec.decode("identity", payload.replace("riko-wf3", "riko-wf2")))
        } finally { Locale.setDefault(original) }
    }
    @Test fun malformedAndAmbiguousLegacyDataAreMisses() {
        for (payload in listOf("0.5,0.4,0.2,0.1", "riko-wf3|key|bad|9|!", "riko-wf3|key|2|999999|", ""))
            assertNull(WaveformCacheCodec.decode("key", payload))
    }
    @Test fun inputAndOutputArraysCannotMutatePublishedWaveform() {
        val peaks = floatArrayOf(0.8f); val rms = floatArrayOf(0.4f)
        val data = WaveformData(peaks, rms, 1, 1000L)
        peaks[0] = 0f; rms[0] = 0f; data.peaks[0] = 0f
        assertEquals(0.8f, data.peakAt(0), 0f)
        assertEquals(0.4f, data.rmsAt(0), 0f)
    }
}
