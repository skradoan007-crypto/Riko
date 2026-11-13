package com.rikodev.music.android.audiofx;

/**
 * Normalizes the legacy OMX MP3 529-frame timestamp offset after its first output.
 * C2 already timestamps by delivered PCM count and needs no adjustment.
 * See AOSP SoftMP3.cpp (kPVMP3DecoderDelay) and C2SoftMp3Dec.cpp (mProcessedSamples).
 * No PCM is removed, inserted or resampled here. Unknown gaps are passed through
 * unchanged so the strict DNA analyzer can reject them.
 */
public final class Mp3PcmTimestampNormalizer {
    private static final int DECODER_DELAY_FRAMES = 529;
    private final boolean mp3;
    private int buffers, rate, firstFrames;
    private long firstPtsUs, correctionUs;

    public Mp3PcmTimestampNormalizer(boolean mp3) { this.mp3 = mp3; }

    public long normalize(long ptsUs, int frames, int sampleRate) {
        if (!mp3 || frames <= 0 || sampleRate < 8000 || sampleRate > 192000
                || ptsUs < -10_000_000L || ptsUs > 21_600_000_000L) return ptsUs;
        if (buffers == 0) {
            firstFrames = frames;
            firstPtsUs = ptsUs;
            rate = sampleRate;
        } else if (buffers == 1 && sampleRate == rate) {
            // MPEG-1 Layer III has 1152 PCM frames, MPEG-2/2.5 has 576.
            // A whole first batch minus 529 frames is required as independent
            // evidence of decoder trimming, not just a conveniently sized gap.
            int packetFrames = rate >= 32000 ? 1152 : 576;
            boolean trimmedFirstBatch = ((long) firstFrames + DECODER_DELAY_FRAMES) % packetFrames == 0;
            long expectedUs = firstPtsUs + firstFrames * 1_000_000L / rate;
            long delayUs = DECODER_DELAY_FRAMES * 1_000_000L / rate;
            // One sample plus integer timestamp rounding; not a broad tolerance increase.
            long roundingUs = (1_000_000L + rate - 1) / rate + 1;
            if (trimmedFirstBatch && ptsUs >= expectedUs
                    && Math.abs((ptsUs - expectedUs) - delayUs) <= roundingUs) {
                correctionUs = delayUs;
            }
        }
        if (buffers < 2) buffers++;
        return sampleRate == rate ? ptsUs - correctionUs : ptsUs;
    }

    public long getCorrectionUs() { return correctionUs; }
}
