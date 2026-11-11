package com.rikodev.music.android.dna.analysis;

/** Streaming mean-corrected onset autocorrelation at 50 Hz; constant memory per song. */
final class OnsetTempo {
    private static final int MAX_LAG = 76;
    private final double[] ring = new double[MAX_LAG + 1];
    private final double[] xy = new double[MAX_LAG + 1], x = new double[MAX_LAG + 1],
            y = new double[MAX_LAG + 1], xx = new double[MAX_LAG + 1], yy = new double[MAX_LAG + 1];
    private final long[] pairs = new long[MAX_LAG + 1];
    private long count;
    private int attacks;
    private double previous;
    void add(double onset) {
        if (onset > 0.08 && previous <= 0.08) attacks++;
        previous = onset;
        for (int lag = 1; lag <= MAX_LAG && lag <= count; lag++) {
            double old = ring[(int) ((count - lag) % ring.length)];
            xy[lag] += onset * old; x[lag] += onset; y[lag] += old;
            xx[lag] += onset * onset; yy[lag] += old * old; pairs[lag]++;
        }
        ring[(int) (count++ % ring.length)] = onset;
    }
    TempoEstimate finish() {
        if (count < 300 || attacks < 4) return new TempoEstimate(0, 0, 0);
        double[] correlation = new double[MAX_LAG + 1];
        for (int lag = 1; lag <= MAX_LAG; lag++) {
            double n = pairs[lag];
            double variance = (xx[lag] - x[lag] * x[lag] / n) * (yy[lag] - y[lag] * y[lag] / n);
            correlation[lag] = variance <= 1e-16 ? 0 :
                    Math.max(0, Math.min(1, (xy[lag] - x[lag] * y[lag] / n) / StrictMath.sqrt(variance)));
        }
        int best = 0;
        for (int lag = 13; lag <= 75; lag++) {
            if (correlation[lag] >= correlation[lag - 1] && correlation[lag] >= correlation[lag + 1]
                    && (best == 0 || correlation[lag] > correlation[best] + 0.01)) best = lag;
        }
        if (best == 0 || correlation[best] < 0.15) return new TempoEstimate(0, 0, 0);
        // A non-integral beat period (e.g. 180 BPM = 16.67 hops) may correlate
        // more strongly at its exact 3-beat period. Prefer a supported shorter
        // local peak at a divisor, not an unconditional tempo-range doubling.
        int longest = best;
        for (int divisor = 2; divisor <= 4; divisor++) {
            int candidate = (int) Math.round(longest / (double) divisor);
            for (int lag = Math.max(13, candidate - 1); lag <= Math.min(75, candidate + 1); lag++) {
                if (lag < best && correlation[lag] >= correlation[lag - 1]
                        && correlation[lag] >= correlation[lag + 1]
                        && correlation[lag] >= Math.max(0.15, correlation[longest] * 0.55)) best = lag;
            }
        }
        double denom = correlation[best - 1] - 2 * correlation[best] + correlation[best + 1];
        double shift = denom == 0 ? 0 : 0.5 * (correlation[best - 1] - correlation[best + 1]) / denom;
        shift = Math.max(-0.5, Math.min(0.5, shift));
        double bpm = Math.max(40, Math.min(240, 3000 / (best + shift)));
        int halfLag = (int) Math.round(best / 2.0), doubleLag = best * 2;
        double alternative = 0;
        if (doubleLag <= 75 && correlation[doubleLag] >= correlation[best] * 0.8) alternative = bpm / 2;
        if (halfLag >= 13 && correlation[halfLag] >= correlation[best] * 0.8
                && (alternative == 0 || correlation[halfLag] > correlation[doubleLag])) alternative = bpm * 2;
        double confidence = correlation[best] * Math.min(1, attacks / 12.0) * (alternative > 0 ? 0.75 : 1);
        return new TempoEstimate(bpm, confidence, alternative);
    }
}
