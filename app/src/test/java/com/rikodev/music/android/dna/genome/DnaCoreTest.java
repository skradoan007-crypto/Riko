package com.rikodev.music.android.dna.genome;

import org.junit.Test;

/** Gradle's existing JUnit 4 runner executes the same contracts as tools/test_genesis_dna.sh. */
public final class DnaCoreTest {
    @Test public void splitMixGolden() { DnaCoreContracts.splitMixGolden(); }
    @Test public void boundedRandom() { DnaCoreContracts.boundedRandom(); }
    @Test public void featureQuantization() { DnaCoreContracts.featureQuantization(); }
    @Test public void invalidFeatures() { DnaCoreContracts.invalidFeatures(); }
    @Test public void defensiveCopies() { DnaCoreContracts.defensiveCopies(); }
    @Test public void goldenCompatibility() throws Exception { DnaCoreContracts.goldenCompatibility(); }
    @Test public void stableIdentity() { DnaCoreContracts.stableIdentity(); }
    @Test public void invalidIdentity() { DnaCoreContracts.invalidIdentity(); }
    @Test public void musicalMapping() { DnaCoreContracts.musicalMapping(); }
    @Test public void versionRejection() { DnaCoreContracts.versionRejection(); }
    @Test public void corruptionAndLength() { DnaCoreContracts.corruptionAndLength(); }
    @Test public void malformedPayloads() { DnaCoreContracts.malformedPayloads(); }
    @Test public void silenceAndExtremes() { DnaCoreContracts.silenceAndExtremes(); }
    @Test public void diverseSongs() { DnaCoreContracts.diverseSongs(); }
    @Test public void concurrentDeterminism() throws Exception { DnaCoreContracts.concurrentDeterminism(); }
    @Test public void roundTripCorpus() { DnaCoreContracts.roundTripCorpus(); }
    @Test public void localeAndTimezoneIndependence() { DnaCoreContracts.localeAndTimezoneIndependence(); }
}
