package com.rikodev.music.android.dna.analysis;

/** Explicit octave alternative; confidence is evidence strength, not a probability. */
public final class TempoEstimate {
    private final double bpm, confidence, alternativeBpm;
    TempoEstimate(double bpm, double confidence, double alternativeBpm) {
        this.bpm = bpm; this.confidence = confidence; this.alternativeBpm = alternativeBpm;
    }
    public double getBpm() { return bpm; }
    public double getConfidence() { return confidence; }
    public double getAlternativeBpm() { return alternativeBpm; }
    public boolean isOctaveAmbiguous() { return alternativeBpm > 0; }
}
