package com.rikodev.music.android.dna.analysis;

import java.util.Arrays;

/** Immutable 10 Hz feature grid; timestamps are window centers on the media timeline.
 * No decoded PCM retained. Units match DnaFeatures; RMS/onset use ppm. Chroma C..B.
 */
public final class DnaTimeline {
    public static final long STEP_US = 100_000;
    /** Bounded leading decoder offset; later gaps are never repaired by this allowance. */
    public static final long MAX_START_US = 250_000;
    public static final int BASS = 0, MID = 1, TREBLE = 2, CENTROID = 3, FLUX = 4,
            RMS = 5, ONSET = 6, CHROMA = 7, WIDTH = 19;
    private final int[] values;
    private final long startUs, durationUs;

    DnaTimeline(int[] ownedValues, long durationUs) {
        this(ownedValues, 0, durationUs);
    }
    DnaTimeline(int[] ownedValues, long startUs, long durationUs) {
        // Package-private ownership transfer from the one-shot builder; no external alias.
        int count = ownedValues.length / WIDTH;
        validateBounds(startUs, durationUs, count);
        if (ownedValues.length % WIDTH != 0) throw new IllegalArgumentException("Timeline width");
        this.values = ownedValues;
        this.startUs = startUs;
        this.durationUs = durationUs;
    }
    static void validateBounds(long startUs, long durationUs, int count) {
        if (startUs < 0 || startUs > MAX_START_US || durationUs <= startUs
                || durationUs > 21_600_000_000L || count < 1 || count > 216001
                || count != (durationUs - startUs + STEP_US - 1) / STEP_US) {
            throw new IllegalArgumentException("Timeline bounds");
        }
    }
    public int getFrameCount() { return values.length / WIDTH; }
    public long getStartUs() { return startUs; }
    /** Media end position, including the leading offset (zero for legacy timelines). */
    public long getDurationUs() { return durationUs; }
    public long timeUs(int frame) { check(frame); return startUs + frame * STEP_US; }
    public int valueAt(int frame, int feature) {
        check(frame);
        if (feature < 0 || feature >= WIDTH) throw new IndexOutOfBoundsException("Feature column");
        return values[frame * WIDTH + feature];
    }
    public void copyFrame(int frame, int[] destination) {
        check(frame);
        if (destination.length < WIDTH) throw new IllegalArgumentException("Frame destination too small");
        System.arraycopy(values, frame * WIDTH, destination, 0, WIDTH);
    }
    private void check(int frame) {
        if (frame < 0 || frame >= getFrameCount()) throw new IndexOutOfBoundsException("Timeline frame");
    }
    static final class Builder {
        private final int maxFrames;
        private int[] values;
        private int count;
        Builder(int maxFrames) {
            this.maxFrames = maxFrames;
            values = new int[Math.min(512, maxFrames) * WIDTH];
        }
        void add(int[] frame) {
            if (count >= maxFrames) throw new IllegalArgumentException("Analysis duration limit exceeded");
            if ((count + 1) * WIDTH > values.length) {
                values = Arrays.copyOf(values, Math.min(maxFrames * WIDTH, values.length * 2));
            }
            System.arraycopy(frame, 0, values, count++ * WIDTH, WIDTH);
        }
        DnaTimeline finish(long durationUs) {
            return new DnaTimeline(Arrays.copyOf(values, count * WIDTH), durationUs);
        }
        DnaTimeline finish(long startUs, long durationUs) {
            return new DnaTimeline(Arrays.copyOf(values, count * WIDTH), startUs, durationUs);
        }
    }
}
