package com.rikodev.music.android.dna.analysis;

/** Bounded streaming Blackman-windowed sinc, 256 fractional phases, compensated lookahead.
 * Source samples never accumulate beyond the fixed ring; flush uses zero edge extension.
 */
public final class SincResampler {
    public interface Sink { void sample(double value); }
    public static final int OUTPUT_RATE = 22050;
    private static final int PHASES = 256;
    private final int rate, radius, taps;
    private final double[][] kernel;
    private final double[] ring;
    private final Sink sink;
    private long received, emitted;
    private boolean finished;

    public SincResampler(int inputRate, Sink sink) {
        if (inputRate < 8000 || inputRate > 192000) throw new IllegalArgumentException("Unsupported sample rate");
        this.rate = inputRate;
        this.sink = java.util.Objects.requireNonNull(sink);
        double ratio = StrictMath.min(1.0, (double) OUTPUT_RATE / rate);
        radius = (int) StrictMath.ceil(24 / ratio);
        taps = radius * 2 + 1;
        ring = new double[taps * 2];
        kernel = new double[PHASES][taps];
        double cutoff = 0.45 * ratio;
        for (int phase = 0; phase < PHASES; phase++) {
            double sum = 0, fraction = (double) phase / PHASES;
            for (int k = -radius; k <= radius; k++) {
                double x = k - fraction;
                double angle = 2 * StrictMath.PI * cutoff * x;
                double sinc = x == 0 ? 2 * cutoff : StrictMath.sin(angle) / (StrictMath.PI * x);
                double window = StrictMath.abs(x) > radius ? 0 : 0.42
                        + 0.5 * StrictMath.cos(StrictMath.PI * x / radius)
                        + 0.08 * StrictMath.cos(2 * StrictMath.PI * x / radius);
                sum += kernel[phase][k + radius] = sinc * window;
            }
            for (int k = 0; k < taps; k++) kernel[phase][k] /= sum;
        }
    }

    public void add(double value) {
        if (finished) throw new IllegalStateException("Resampler finished");
        if (!Double.isFinite(value) || value < -1 || value > 1) throw new IllegalArgumentException("Invalid PCM");
        ring[(int) (received++ % ring.length)] = value;
        while (emitted * rate / OUTPUT_RATE + radius < received) emit();
    }

    public void finish() {
        if (finished) throw new IllegalStateException("Resampler finished");
        finished = true;
        long count = (received * OUTPUT_RATE + rate - 1) / rate;
        while (emitted < count) emit();
    }

    private void emit() {
        long numerator = emitted * rate;
        long center = numerator / OUTPUT_RATE;
        int phase = (int) ((numerator % OUTPUT_RATE) * PHASES / OUTPUT_RATE);
        double sum = 0;
        for (int k = -radius; k <= radius; k++) {
            long index = center + k;
            if (index >= 0 && index < received) {
                if (index < received - ring.length) throw new IllegalStateException("Resampler history overrun");
                sum += ring[(int) (index % ring.length)] * kernel[phase][k + radius];
            }
        }
        emitted++;
        sink.sample(StrictMath.max(-1, StrictMath.min(1, sum)));
    }
}
