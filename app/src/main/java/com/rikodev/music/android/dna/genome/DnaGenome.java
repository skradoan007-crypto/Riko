package com.rikodev.music.android.dna.genome;

import java.util.Arrays;
import java.util.Objects;

/** Immutable base identity. Camera, live animation, quality and evolution are not genome inputs. */
public final class DnaGenome {
    public enum Archetype {
        ORB(1), TERRAIN(2), NEBULA(3);
        private final int id;
        Archetype(int id) { this.id = id; }
        public int getId() { return id; }
        public static Archetype fromId(int id) {
            switch (id) {
                case 1: return ORB;
                case 2: return TERRAIN;
                case 3: return NEBULA;
                default: throw new IllegalArgumentException("Unsupported archetype: " + id);
            }
        }
    }

    private final DnaAlgorithmVersion algorithmVersion;
    private final long seed;
    private final long geometrySeed;
    private final Archetype archetype;
    private final int particleCount, layerCount, symmetry, bodyExpansionPpm, brightnessPpm;
    private final int surfaceSharpnessPpm, transientBurstPpm, rhythmMilliBpm, deformationPpm;
    private final int flowSpeedPpm, microDetailPpm, paletteRotationMilliDegrees;
    private final int[] chromaPpm;

    // Only mapper and validated internal codec construct genomes. No public mutable builder.
    DnaGenome(DnaAlgorithmVersion version, long seed, long geometrySeed, Archetype archetype,
            int particleCount, int layerCount, int symmetry, int bodyExpansionPpm,
            int brightnessPpm, int surfaceSharpnessPpm, int transientBurstPpm,
            int rhythmMilliBpm, int deformationPpm, int flowSpeedPpm, int microDetailPpm,
            int paletteRotationMilliDegrees, int[] chromaPpm) {
        this.algorithmVersion = Objects.requireNonNull(version, "algorithmVersion");
        this.seed = seed;
        this.geometrySeed = geometrySeed;
        this.archetype = Objects.requireNonNull(archetype, "archetype");
        this.particleCount = DnaFeatures.range(particleCount, 4096, 32768, "particle count");
        this.layerCount = DnaFeatures.range(layerCount, 2, 5, "layer count");
        this.symmetry = DnaFeatures.range(symmetry, 2, 12, "symmetry");
        this.bodyExpansionPpm = DnaFeatures.range(bodyExpansionPpm, 0, 500000, "body expansion");
        this.brightnessPpm = unit(brightnessPpm, "brightness");
        this.surfaceSharpnessPpm = unit(surfaceSharpnessPpm, "surface sharpness");
        this.transientBurstPpm = unit(transientBurstPpm, "transient burst");
        this.rhythmMilliBpm = DnaFeatures.range(rhythmMilliBpm, 0, 300000, "rhythm");
        this.deformationPpm = DnaFeatures.range(deformationPpm, 0, 500000, "deformation");
        this.flowSpeedPpm = DnaFeatures.range(flowSpeedPpm, 100000, 2000000, "flow speed");
        this.microDetailPpm = unit(microDetailPpm, "micro detail");
        this.paletteRotationMilliDegrees = DnaFeatures.range(paletteRotationMilliDegrees,
                0, 359999, "palette rotation");
        this.chromaPpm = DnaFeatures.copyChroma(chromaPpm);
    }

    private static int unit(int value, String name) { return DnaFeatures.range(value, 0, DnaFeatures.UNIT, name); }
    public DnaAlgorithmVersion getAlgorithmVersion() { return algorithmVersion; }
    public long getSeed() { return seed; }
    public long getGeometrySeed() { return geometrySeed; }
    public Archetype getArchetype() { return archetype; }
    public int getParticleCount() { return particleCount; }
    public int getLayerCount() { return layerCount; }
    public int getSymmetry() { return symmetry; }
    public int getBodyExpansionPpm() { return bodyExpansionPpm; }
    public int getBrightnessPpm() { return brightnessPpm; }
    public int getSurfaceSharpnessPpm() { return surfaceSharpnessPpm; }
    public int getTransientBurstPpm() { return transientBurstPpm; }
    public int getRhythmMilliBpm() { return rhythmMilliBpm; }
    public int getDeformationPpm() { return deformationPpm; }
    public int getFlowSpeedPpm() { return flowSpeedPpm; }
    public int getMicroDetailPpm() { return microDetailPpm; }
    public int getPaletteRotationMilliDegrees() { return paletteRotationMilliDegrees; }
    public int[] getChromaPpm() { return chromaPpm.clone(); }
    public int chromaAt(int pitchClass) { return chromaPpm[pitchClass]; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DnaGenome)) return false;
        DnaGenome g = (DnaGenome) other;
        return algorithmVersion == g.algorithmVersion && seed == g.seed && geometrySeed == g.geometrySeed
                && archetype == g.archetype && particleCount == g.particleCount && layerCount == g.layerCount
                && symmetry == g.symmetry && bodyExpansionPpm == g.bodyExpansionPpm && brightnessPpm == g.brightnessPpm
                && surfaceSharpnessPpm == g.surfaceSharpnessPpm && transientBurstPpm == g.transientBurstPpm
                && rhythmMilliBpm == g.rhythmMilliBpm && deformationPpm == g.deformationPpm
                && flowSpeedPpm == g.flowSpeedPpm && microDetailPpm == g.microDetailPpm
                && paletteRotationMilliDegrees == g.paletteRotationMilliDegrees && Arrays.equals(chromaPpm, g.chromaPpm);
    }

    @Override public int hashCode() {
        return 31 * Objects.hash(algorithmVersion, seed, geometrySeed, archetype, particleCount,
                layerCount, symmetry, bodyExpansionPpm, brightnessPpm, surfaceSharpnessPpm,
                transientBurstPpm, rhythmMilliBpm, deformationPpm, flowSpeedPpm, microDetailPpm,
                paletteRotationMilliDegrees) + Arrays.hashCode(chromaPpm);
    }
}
