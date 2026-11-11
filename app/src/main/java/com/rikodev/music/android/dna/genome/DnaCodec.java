package com.rikodev.music.android.dna.genome;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * Internal, bounded binary persistence format; not the P7 portable DNA code.
 * Big-endian, fixed V1 size, separate wire/algorithm versions, CRC32 corruption check.
 * CRC is not authentication. Unknown versions, invalid values and trailing bytes fail closed.
 */
public final class DnaCodec {
    private static final int FEATURES_MAGIC = 0x4d444645; // MDFE
    private static final int GENOME_MAGIC = 0x4d44474e; // MDGN
    private static final int WIRE_VERSION = 1;
    public static final int FEATURES_BYTES = 90;
    public static final int GENOME_BYTES = 125;

    private DnaCodec() {}

    public static byte[] encodeFeatures(DnaFeatures features) {
        Objects.requireNonNull(features, "features");
        ByteBuffer out = start(FEATURES_MAGIC, FEATURES_BYTES);
        putFeaturePayload(out, features);
        return finish(out);
    }

    public static DnaFeatures decodeFeatures(byte[] bytes) {
        ByteBuffer in = checked(bytes, FEATURES_MAGIC, FEATURES_BYTES);
        int bass = in.getInt(), mid = in.getInt(), treble = in.getInt();
        int centroid = in.getInt(), flux = in.getInt(), tempo = in.getInt();
        int confidence = in.getInt(), dynamicRange = in.getInt();
        return DnaFeatures.fromCanonical(bass, mid, treble, centroid, flux, tempo,
                confidence, dynamicRange, readChroma(in));
    }

    public static byte[] encodeGenome(DnaGenome genome) {
        Objects.requireNonNull(genome, "genome");
        ByteBuffer out = start(GENOME_MAGIC, GENOME_BYTES);
        out.putShort((short) genome.getAlgorithmVersion().getId());
        out.putLong(genome.getSeed()).putLong(genome.getGeometrySeed());
        out.put((byte) genome.getArchetype().getId());
        out.putInt(genome.getParticleCount()).putInt(genome.getLayerCount()).putInt(genome.getSymmetry());
        out.putInt(genome.getBodyExpansionPpm()).putInt(genome.getBrightnessPpm());
        out.putInt(genome.getSurfaceSharpnessPpm()).putInt(genome.getTransientBurstPpm());
        out.putInt(genome.getRhythmMilliBpm()).putInt(genome.getDeformationPpm());
        out.putInt(genome.getFlowSpeedPpm()).putInt(genome.getMicroDetailPpm());
        out.putInt(genome.getPaletteRotationMilliDegrees());
        for (int i = 0; i < 12; i++) out.putInt(genome.chromaAt(i));
        return finish(out);
    }

    public static DnaGenome decodeGenome(byte[] bytes) {
        ByteBuffer in = checked(bytes, GENOME_MAGIC, GENOME_BYTES);
        DnaAlgorithmVersion version = DnaAlgorithmVersion.fromId(in.getShort() & 0xffff);
        long seed = in.getLong(), geometrySeed = in.getLong();
        DnaGenome.Archetype archetype = DnaGenome.Archetype.fromId(in.get() & 0xff);
        int particles = in.getInt(), layers = in.getInt(), symmetry = in.getInt();
        int expansion = in.getInt(), brightness = in.getInt(), sharpness = in.getInt();
        int burst = in.getInt(), rhythm = in.getInt(), deformation = in.getInt();
        int flow = in.getInt(), detail = in.getInt(), rotation = in.getInt();
        return new DnaGenome(version, seed, geometrySeed, archetype, particles, layers,
                symmetry, expansion, brightness, sharpness, burst, rhythm, deformation,
                flow, detail, rotation, readChroma(in));
    }

    // This 80-byte order is part of algorithm V1, independent of future wire envelopes.
    static byte[] featurePayloadV1(DnaFeatures features) {
        ByteBuffer out = ByteBuffer.allocate(80).order(ByteOrder.BIG_ENDIAN);
        putFeaturePayload(out, features);
        return out.array();
    }

    private static void putFeaturePayload(ByteBuffer out, DnaFeatures f) {
        out.putInt(f.getBassEnergyPpm()).putInt(f.getMidEnergyPpm()).putInt(f.getTrebleEnergyPpm());
        out.putInt(f.getCentroidMilliHz()).putInt(f.getSpectralFluxPpm()).putInt(f.getTempoMilliBpm());
        out.putInt(f.getTempoConfidencePpm()).putInt(f.getDynamicRangeMilliDb());
        for (int i = 0; i < 12; i++) out.putInt(f.chromaAt(i));
    }

    private static int[] readChroma(ByteBuffer in) {
        int[] chroma = new int[12];
        for (int i = 0; i < chroma.length; i++) chroma[i] = in.getInt();
        return chroma;
    }

    private static ByteBuffer start(int magic, int length) {
        ByteBuffer out = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
        out.putInt(magic).putShort((short) WIRE_VERSION);
        return out;
    }

    private static byte[] finish(ByteBuffer out) {
        if (out.position() != out.capacity() - 4) throw new IllegalStateException("DNA codec size mismatch");
        CRC32 crc = new CRC32();
        crc.update(out.array(), 0, out.position());
        out.putInt((int) crc.getValue());
        return out.array();
    }

    private static ByteBuffer checked(byte[] bytes, int magic, int length) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length != length) throw new IllegalArgumentException("Invalid DNA blob length");
        // Copy once, after the size limit, so caller mutation cannot change validated input.
        byte[] copy = bytes.clone();
        CRC32 crc = new CRC32();
        crc.update(copy, 0, length - 4);
        ByteBuffer in = ByteBuffer.wrap(copy).order(ByteOrder.BIG_ENDIAN);
        if (in.getInt(length - 4) != (int) crc.getValue()) {
            throw new IllegalArgumentException("DNA blob checksum mismatch");
        }
        if (in.getInt() != magic) throw new IllegalArgumentException("Wrong DNA blob type");
        if ((in.getShort() & 0xffff) != WIRE_VERSION) {
            throw new IllegalArgumentException("Unsupported DNA wire version");
        }
        return in;
    }
}
