package com.rikodev.music.android.audiofx.visualizer;

/** Absolute (not auto-normalized) band RMS, compensated for FFT size and Hann energy.
 * Output is sqrt(RMS) for perceptual display range; quieter PCM stays quieter.
 * No per-frame peak normalization and no treble tilt/clipping from the legacy bar UI.
 */
public final class MdnaSpectrumMath {
    private MdnaSpectrumMath() {}
    public static float level(double fftPower, int fftSize, double windowEnergy) {
        if (!Double.isFinite(fftPower) || fftPower <= 0 || fftSize <= 0 || windowEnergy <= 0) return 0;
        double rms = Math.sqrt(2.0 * fftPower / (fftSize * windowEnergy));
        return (float)Math.min(1.0, Math.sqrt(rms));
    }
    /** Summed band power recovered from perceptual levels. Independent of empty FFT bins. */
    public static float envelope(float[] levels, int first, int end) {
        double power = 0;
        for (int i = Math.max(0, first); i < Math.min(end, levels.length); i++) {
            float v = levels[i];
            if (Float.isFinite(v) && v > 0) power += (double)v*v*v*v;
        }
        return (float)Math.min(1.0, Math.pow(power, .25));
    }
}
