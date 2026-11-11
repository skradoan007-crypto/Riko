package com.rikodev.music.android.dna.analysis;

import com.rikodev.music.android.dna.genome.DnaFeatures;

/** Completed analysis only; no partial-success object on cancellation or decode failure. */
public final class DnaAnalysis {
    public static final int ANALYZER_VERSION = 1;
    private final DnaFeatures features;
    private final DnaTimeline timeline;
    private final TempoEstimate tempo;
    private final String audioSha256;
    DnaAnalysis(DnaFeatures features, DnaTimeline timeline, TempoEstimate tempo, String audioSha256) {
        this.features = features; this.timeline = timeline; this.tempo = tempo; this.audioSha256 = audioSha256;
    }
    public int getAnalyzerVersion() { return ANALYZER_VERSION; }
    public DnaFeatures getFeatures() { return features; }
    public DnaTimeline getTimeline() { return timeline; }
    public TempoEstimate getTempo() { return tempo; }
    public String getAudioSha256() { return audioSha256; }
}
