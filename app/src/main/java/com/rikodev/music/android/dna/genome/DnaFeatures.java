package com.rikodev.music.android.dna.genome;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, canonical song summary, independent of live spectrum and playback time.
 * Units: normalized energies/flux/confidence/chroma in ppm; centroid in milli-Hz;
 * tempo in milli-BPM; dynamic range in milli-dB. Chroma order is C through B.
 * See docs/mdna/genesis/P1_CORE.md for the frozen V1 extraction-input contract.
 */
public final class DnaFeatures {
    public static final int UNIT = 1_000_000;
    private final int bassEnergyPpm;
    private final int midEnergyPpm;
    private final int trebleEnergyPpm;
    private final int centroidMilliHz;
    private final int spectralFluxPpm;
    private final int tempoMilliBpm;
    private final int tempoConfidencePpm;
    private final int dynamicRangeMilliDb;
    private final int[] chromaPpm;

    private DnaFeatures(int bass, int mid, int treble, int centroid, int flux,
                        int tempo, int confidence, int dynamicRange, int[] chroma) {
        bassEnergyPpm = range(bass, 0, UNIT, "bass");
        midEnergyPpm = range(mid, 0, UNIT, "mid");
        trebleEnergyPpm = range(treble, 0, UNIT, "treble");
        centroidMilliHz = range(centroid, 0, 11_025_000, "centroid");
        spectralFluxPpm = range(flux, 0, UNIT, "flux");
        tempoMilliBpm = range(tempo, 0, 300_000, "tempo");
        tempoConfidencePpm = range(confidence, 0, UNIT, "tempo confidence");
        if (tempo == 0 && confidence != 0) {
            throw new IllegalArgumentException("Unknown tempo must have zero confidence");
        }
        dynamicRangeMilliDb = range(dynamicRange, 0, 120_000, "dynamic range");
        chromaPpm = copyChroma(chroma);
    }

    public static DnaFeatures fromCanonical(int bass, int mid, int treble, int centroid,
            int flux, int tempo, int confidence, int dynamicRange, int[] chroma) {
        return new DnaFeatures(bass, mid, treble, centroid, flux, tempo, confidence,
                dynamicRange, chroma);
    }

    /**
     * Half-up quantization, no silent clamping. Chroma input is already L1 normalized
     * (sum within 1e-6 of 1), or all-zero for unknown/silent chroma. Largest-remainder
     * quantization gives exactly UNIT total; ties go to the lower pitch class.
     */
    public static DnaFeatures fromNormalized(double bass, double mid, double treble,
            double centroidHz, double flux, double bpm, double confidence,
            double dynamicRangeDb, double[] chroma) {
        if (bpm == 0.0 && confidence != 0.0) {
            throw new IllegalArgumentException("Unknown tempo must have zero confidence");
        }
        Objects.requireNonNull(chroma, "chroma");
        if (chroma.length != 12) throw new IllegalArgumentException("Expected 12 chroma bins");
        double[] input = chroma.clone();
        double sum = 0;
        for (double value : input) {
            requireFiniteRange(value, 1.0, "chroma");
            sum += value;
        }
        int[] canonical = new int[12];
        if (sum != 0.0) {
            if (StrictMath.abs(sum - 1.0) > 0.000001) {
                throw new IllegalArgumentException("Chroma must be L1 normalized");
            }
            double[] remainders = new double[12];
            int total = 0;
            for (int i = 0; i < 12; i++) {
                double scaled = input[i] / sum * UNIT;
                canonical[i] = (int) StrictMath.floor(scaled);
                remainders[i] = scaled - canonical[i];
                total += canonical[i];
            }
            for (int left = UNIT - total; left > 0; left--) {
                int best = 0;
                for (int i = 1; i < 12; i++) {
                    if (remainders[i] > remainders[best]) best = i;
                }
                canonical[best]++;
                remainders[best] = -1;
            }
        }
        return fromCanonical(quantize(bass, 1, UNIT, "bass"),
                quantize(mid, 1, UNIT, "mid"), quantize(treble, 1, UNIT, "treble"),
                quantize(centroidHz, 11025, 1000, "centroid"),
                quantize(flux, 1, UNIT, "flux"), quantize(bpm, 300, 1000, "tempo"),
                quantize(confidence, 1, UNIT, "tempo confidence"),
                quantize(dynamicRangeDb, 120, 1000, "dynamic range"), canonical);
    }

    static int range(int value, int min, int max, String name) {
        if (value < min || value > max) throw new IllegalArgumentException(name + " out of range");
        return value;
    }

    private static void requireFiniteRange(double value, double max, String name) {
        if (!Double.isFinite(value) || value < 0 || value > max) {
            throw new IllegalArgumentException(name + " must be finite and in range");
        }
    }

    private static int quantize(double value, double max, int scale, String name) {
        requireFiniteRange(value, max, name);
        return (int) StrictMath.floor(value * scale + 0.5);
    }

    static int[] copyChroma(int[] values) {
        Objects.requireNonNull(values, "chroma");
        if (values.length != 12) throw new IllegalArgumentException("Expected 12 chroma bins");
        int[] copy = values.clone();
        int sum = 0;
        for (int value : copy) sum += range(value, 0, UNIT, "chroma");
        if (sum != 0 && sum != UNIT) throw new IllegalArgumentException("Invalid chroma total");
        return copy;
    }

    public int getBassEnergyPpm() { return bassEnergyPpm; }
    public int getMidEnergyPpm() { return midEnergyPpm; }
    public int getTrebleEnergyPpm() { return trebleEnergyPpm; }
    public int getCentroidMilliHz() { return centroidMilliHz; }
    public int getSpectralFluxPpm() { return spectralFluxPpm; }
    public int getTempoMilliBpm() { return tempoMilliBpm; }
    public int getTempoConfidencePpm() { return tempoConfidencePpm; }
    public int getDynamicRangeMilliDb() { return dynamicRangeMilliDb; }
    public int[] getChromaPpm() { return chromaPpm.clone(); }
    public int chromaAt(int pitchClass) { return chromaPpm[pitchClass]; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DnaFeatures)) return false;
        DnaFeatures f = (DnaFeatures) other;
        return bassEnergyPpm == f.bassEnergyPpm && midEnergyPpm == f.midEnergyPpm
                && trebleEnergyPpm == f.trebleEnergyPpm && centroidMilliHz == f.centroidMilliHz
                && spectralFluxPpm == f.spectralFluxPpm && tempoMilliBpm == f.tempoMilliBpm
                && tempoConfidencePpm == f.tempoConfidencePpm
                && dynamicRangeMilliDb == f.dynamicRangeMilliDb && Arrays.equals(chromaPpm, f.chromaPpm);
    }

    @Override public int hashCode() {
        return 31 * Objects.hash(bassEnergyPpm, midEnergyPpm, trebleEnergyPpm,
                centroidMilliHz, spectralFluxPpm, tempoMilliBpm, tempoConfidencePpm,
                dynamicRangeMilliDb) + Arrays.hashCode(chromaPpm);
    }
}
