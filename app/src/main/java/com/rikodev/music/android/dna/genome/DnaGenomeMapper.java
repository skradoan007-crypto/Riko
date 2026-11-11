package com.rikodev.music.android.dna.genome;

import java.util.Objects;

/** Stateless mapping. Frozen V1 integer math is reproducible on Android/JVM and any CPU ABI. */
public final class DnaGenomeMapper {
    private DnaGenomeMapper() {}

    public static DnaGenome generate(String audioSha256, DnaFeatures features, DnaAlgorithmVersion version) {
        return map(features, DnaSeedGenerator.generate(audioSha256, features, version), version);
    }

    /** Rebuild from a persisted seed. Call generate() when starting from song identity. */
    public static DnaGenome map(DnaFeatures features, long seed, DnaAlgorithmVersion version) {
        Objects.requireNonNull(features, "features");
        Objects.requireNonNull(version, "version");
        switch (version) {
            case GENESIS_V1: return mapV1(features, seed);
            default: throw new IllegalArgumentException("Unsupported DNA algorithm");
        }
    }

    private static DnaGenome mapV1(DnaFeatures f, long seed) {
        SplitMix64 random = new SplitMix64(seed);
        long geometrySeed = random.nextLong();
        int bass = f.getBassEnergyPpm(), mid = f.getMidEnergyPpm(), treble = f.getTrebleEnergyPpm();
        int orbWeight = 250000 + bass / 2 + f.getTempoConfidencePpm() / 4;
        int terrainWeight = 250000 + mid / 2 + (DnaFeatures.UNIT - f.getSpectralFluxPpm()) / 4;
        int nebulaWeight = 250000 + treble / 2 + f.getSpectralFluxPpm() / 4;
        int choice = random.nextInt(orbWeight + terrainWeight + nebulaWeight);
        DnaGenome.Archetype archetype = choice < orbWeight ? DnaGenome.Archetype.ORB
                : choice < orbWeight + terrainWeight ? DnaGenome.Archetype.TERRAIN : DnaGenome.Archetype.NEBULA;
        int particles = 4096 + scale(bass, 16384, DnaFeatures.UNIT)
                + scale(treble, 8192, DnaFeatures.UNIT) + random.nextInt(4097);
        int layers = 2 + scale(mid + treble, 2, 2 * DnaFeatures.UNIT) + random.nextInt(2);
        int dominant = 0;
        for (int i = 1; i < 12; i++) if (f.chromaAt(i) > f.chromaAt(dominant)) dominant = i;
        int symmetry = 2 + dominant % 6 + scale(f.chromaAt(dominant), 4, DnaFeatures.UNIT);
        int expansion = 50000 + scale(bass, 300000, DnaFeatures.UNIT) + random.nextInt(50001);
        int brightness = 250000 + scale(f.getCentroidMilliHz(), 550000, 11025000) + random.nextInt(100001);
        int sharpness = 100000 + scale(f.getCentroidMilliHz(), 650000, 11025000) + random.nextInt(150001);
        int burst = 50000 + scale(f.getSpectralFluxPpm(), 850000, DnaFeatures.UNIT) + random.nextInt(100001);
        // Low-confidence BPM does not create a pretend beat grid or an arbitrary default tempo.
        int rhythm = f.getTempoConfidencePpm() >= 350000 ? f.getTempoMilliBpm() : 0;
        int deformation = 30000 + scale(f.getDynamicRangeMilliDb(), 350000, 120000) + random.nextInt(50001);
        int flow = 100000 + scale(mid, 500000, DnaFeatures.UNIT)
                + scale(rhythm, 800000, 300000) + random.nextInt(100001);
        int microDetail = 100000 + scale(treble, 750000, DnaFeatures.UNIT) + random.nextInt(150001);
        int paletteRotation = random.nextInt(360000);
        return new DnaGenome(DnaAlgorithmVersion.GENESIS_V1, seed, geometrySeed, archetype,
                particles, layers, symmetry, expansion, brightness, sharpness, burst,
                rhythm, deformation, flow, microDetail, paletteRotation, f.getChromaPpm());
    }

    private static int scale(int value, int numerator, int denominator) {
        return (int) ((long) value * numerator / denominator);
    }
}
