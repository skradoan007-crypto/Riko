package com.rikodev.music.android.dna.analysis;

/** Numeric diagnostics only: no file, URI, title, codec exception text or device identifier. */
public final class DnaTimestampException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;
    private final long expectedUs, actualUs, framesRead;
    private final int sampleRate;

    public DnaTimestampException(boolean initial, long expectedUs, long actualUs,
                                 long framesRead, int sampleRate) {
        super(initial ? "Missing initial audio" : "Decoder timestamp discontinuity");
        this.expectedUs = expectedUs;
        this.actualUs = actualUs;
        this.framesRead = framesRead;
        this.sampleRate = sampleRate;
    }

    public String diagnostic() {
        return "PCM " + sampleRate + " Hz; frames read " + framesRead
                + "; expected " + expectedUs + " us; received " + actualUs + " us";
    }
}
