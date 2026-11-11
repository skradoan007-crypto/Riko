package com.rikodev.music.android.dna.genome;

/** Algorithm IDs are persistent data. Never reorder, reuse, or silently upgrade them. */
public enum DnaAlgorithmVersion {
    GENESIS_V1(1);

    private final int id;

    DnaAlgorithmVersion(int id) { this.id = id; }

    public int getId() { return id; }

    public static DnaAlgorithmVersion fromId(int id) {
        if (id == 1) return GENESIS_V1;
        throw new IllegalArgumentException("Unsupported DNA algorithm: " + id);
    }
}
