package com.rikodev.music.android.dna.analysis;

import org.junit.Test;

/** Existing Gradle/JUnit runner executes the same production-source contracts. */
public final class DnaAnalyzerTest {
    @Test public void mp3DecoderTimestampConventions() { DnaAnalyzerContracts.mp3DecoderTimestampConventions(); }
    @Test public void timestampCorrectionDoesNotHideLostAudio() { DnaAnalyzerContracts.timestampCorrectionDoesNotHideLostAudio(); }
    @Test public void timestampDiagnosticsAreNumeric() { DnaAnalyzerContracts.timestampDiagnosticsAreNumeric(); }
    @Test public void safeFailureReasons() { DnaAnalyzerContracts.safeFailureReasons(); }
    @Test public void frozenAnalyzerVersion() throws Exception { DnaAnalyzerContracts.frozenAnalyzerVersion(); }
    @Test public void tonesAndChroma() { DnaAnalyzerContracts.tonesAndChroma(); }
    @Test public void antiAliasing() { DnaAnalyzerContracts.antiAliasing(); }
    @Test public void chunkInvariance() { DnaAnalyzerContracts.chunkInvariance(); }
    @Test public void channelsAndPcmNormalization() { DnaAnalyzerContracts.channelsAndPcmNormalization(); }
    @Test public void timelineAndSilence() { DnaAnalyzerContracts.timelineAndSilence(); }
    @Test public void tempoAndAmbiguity() { DnaAnalyzerContracts.tempoAndAmbiguity(); }
    @Test public void dynamicRangeAndFlux() { DnaAnalyzerContracts.dynamicRangeAndFlux(); }
    @Test public void cancellation() { DnaAnalyzerContracts.cancellation(); }
    @Test public void discontinuitiesAndFormats() { DnaAnalyzerContracts.discontinuitiesAndFormats(); }
    @Test public void limitsAndEmpty() { DnaAnalyzerContracts.limitsAndEmpty(); }
    @Test public void resamplerTimeAndPriming() { DnaAnalyzerContracts.resamplerTimeAndPriming(); }
    @Test public void deterministicGenomeInput() { DnaAnalyzerContracts.deterministicGenomeInput(); }
}
