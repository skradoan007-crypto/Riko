package com.rikodev.music.android.dna.genome;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.CRC32;

/** The same assertion bodies run under JUnit/Gradle and the dependency-free JVM runner. */
public final class DnaCoreContracts {
    private static final DnaAlgorithmVersion V1 = DnaAlgorithmVersion.GENESIS_V1;
    private static final String ID = repeat("01", 32);
    private static final String[] GOLDENS = {"silence", "a_minor_pulse", "bright_noise"};

    private DnaCoreContracts() {}

    public static void splitMixGolden() {
        SplitMix64 random = new SplitMix64(0);
        long[] expected = {0xe220a8397b1dcdafL, 0x6e789e6aa1b965f4L, 0x06c45d188009454fL};
        for (long value : expected) check(value == random.nextLong(), "SplitMix64 reference vector");
        SplitMix64 edge = new SplitMix64(Long.MAX_VALUE);
        SplitMix64 same = new SplitMix64(Long.MAX_VALUE);
        for (int i = 0; i < 10000; i++) check(edge.nextLong() == same.nextLong(), "Signed wraparound");
    }

    public static void boundedRandom() {
        reject(() -> new SplitMix64(0).nextInt(0));
        reject(() -> new SplitMix64(0).nextInt(-1));
        int[] bounds = {1, 2, 3, 97, 4097, 360000, Integer.MAX_VALUE};
        for (int bound : bounds) {
            SplitMix64 random = new SplitMix64(-1);
            for (int i = 0; i < 10000; i++) {
                int value = random.nextInt(bound);
                check(value >= 0 && value < bound, "Random bound " + bound);
            }
        }
    }

    public static void featureQuantization() {
        double[] chroma = new double[12];
        Arrays.fill(chroma, 1.0 / 12.0);
        DnaFeatures a = normalized(0.2500001, chroma);
        DnaFeatures b = normalized(0.2500004, chroma);
        equal(a, b, "Inputs in the same canonical bin");
        equal(a.hashCode(), b.hashCode(), "Feature hash contract");
        check(!a.equals(normalized(0.2500006, chroma)), "Next canonical bin");
        check(a.chromaAt(0) == 83334 && a.chromaAt(3) == 83334 && a.chromaAt(4) == 83333,
                "Largest-remainder tie breaks toward C");
        int sum = 0;
        for (int value : a.getChromaPpm()) sum += value;
        check(sum == DnaFeatures.UNIT, "Chroma exact mass");
        check(normalized(-0.0, new double[12]).getBassEnergyPpm() == 0, "Negative zero canonicalized");
        DnaFeatures maximum = DnaFeatures.fromNormalized(1, 1, 1, 11025, 1, 300, 1, 120, new double[12]);
        check(maximum.getCentroidMilliHz() == 11025000 && maximum.getTempoMilliBpm() == 300000,
                "Input endpoints");
    }

    public static void invalidFeatures() {
        for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.1, 1.1}) {
            reject(() -> normalized(bad, new double[12]));
            double[] chroma = new double[12]; chroma[0] = bad;
            reject(() -> normalized(0, chroma));
        }
        reject(() -> normalized(0, new double[11]));
        reject(() -> normalized(0, new double[13]));
        double[] half = new double[12]; half[0] = 0.5;
        reject(() -> normalized(0, half));
        reject(() -> DnaFeatures.fromNormalized(0, 0, 0, 11026, 0, 0, 0, 0, new double[12]));
        reject(() -> DnaFeatures.fromNormalized(0, 0, 0, 0, 0, 0, 0.1, 0, new double[12]));
        reject(() -> DnaFeatures.fromCanonical(0, 0, 0, 0, 0, 0, 1, 0, new int[12]));
        reject(() -> DnaFeatures.fromCanonical(0, 0, 0, 0, 0, 300001, 0, 0, new int[12]));
        reject(() -> DnaFeatures.fromCanonical(0, 0, 0, 0, 0, 0, 0, -1, new int[12]));
        int[] invalid = new int[12]; Arrays.fill(invalid, Integer.MAX_VALUE);
        reject(() -> DnaFeatures.fromCanonical(0, 0, 0, 0, 0, 0, 0, 0, invalid));
    }

    public static void defensiveCopies() {
        int[] chroma = new int[12]; chroma[0] = 1000000;
        DnaFeatures f = DnaFeatures.fromCanonical(0, 0, 0, 0, 0, 0, 0, 0, chroma);
        int hash = f.hashCode();
        chroma[0] = 0;
        f.getChromaPpm()[0] = 0;
        check(f.chromaAt(0) == 1000000 && hash == f.hashCode(), "Feature defensive copy");
        DnaGenome genome = DnaGenomeMapper.generate(ID, f, V1);
        byte[] before = DnaCodec.encodeGenome(genome);
        int genomeHash = genome.hashCode();
        genome.getChromaPpm()[0] = 0;
        check(genome.chromaAt(0) == 1000000 && genomeHash == genome.hashCode(), "Genome defensive copy");
        bytes(before, DnaCodec.encodeGenome(genome), "Genome serialization immutable");
        byte[] blob = DnaCodec.encodeFeatures(f);
        DnaFeatures decoded = DnaCodec.decodeFeatures(blob);
        Arrays.fill(blob, (byte) 0);
        equal(f, decoded, "Decoded feature owns its data");
        double[] normalized = new double[12]; normalized[1] = 1;
        DnaFeatures fromDouble = normalized(0.1, normalized); normalized[1] = 0;
        check(fromDouble.chromaAt(1) == 1000000, "Normalized input is copied");
    }

    public static void goldenCompatibility() throws Exception {
        Properties p = goldens();
        for (String name : GOLDENS) {
            DnaFeatures f = DnaCodec.decodeFeatures(unhex(p.getProperty(name + ".features")));
            bytes(unhex(p.getProperty(name + ".features")), DnaCodec.encodeFeatures(f), name + " feature V1 bytes");
            String id = p.getProperty(name + ".identity");
            long seed = Long.parseUnsignedLong(p.getProperty(name + ".seed"), 16);
            check(seed == DnaSeedGenerator.generate(id, f, V1), name + " seed V1 golden");
            byte[] expected = unhex(p.getProperty(name + ".genome"));
            DnaGenome generated = DnaGenomeMapper.generate(id, f, V1);
            bytes(expected, DnaCodec.encodeGenome(generated), name + " genome V1 bytes");
            equal(generated, DnaCodec.decodeGenome(expected), name + " old V1 decode");
            equal(generated.hashCode(), DnaCodec.decodeGenome(expected).hashCode(), name + " genome hash");
            equal(generated, DnaGenomeMapper.map(f, seed, V1), name + " persisted seed reconstruction");
        }
    }

    public static void stableIdentity() {
        DnaFeatures f = sample();
        DnaGenome expected = DnaGenomeMapper.generate(ID, f, V1);
        for (int i = 0; i < 200; i++) equal(expected, DnaGenomeMapper.generate(ID, f, V1), "Repeat identity");
        String lower = repeat("ab", 32);
        equal(DnaGenomeMapper.generate(lower, f, V1), DnaGenomeMapper.generate(lower.toUpperCase(Locale.ROOT), f, V1),
                "Hex casing is not identity");
        check(expected.getSeed() != DnaSeedGenerator.generate(repeat("02", 32), f, V1), "Other song");
        DnaFeatures changed = DnaFeatures.fromCanonical(f.getBassEnergyPpm() + 1, f.getMidEnergyPpm(),
                f.getTrebleEnergyPpm(), f.getCentroidMilliHz(), f.getSpectralFluxPpm(),
                f.getTempoMilliBpm(), f.getTempoConfidencePpm(), f.getDynamicRangeMilliDb(), f.getChromaPpm());
        check(expected.getSeed() != DnaSeedGenerator.generate(ID, changed, V1), "Changed canonical feature");
    }

    public static void invalidIdentity() {
        for (String bad : new String[]{"", "song-title", "/storage/emulated/0/music.mp3", repeat("0", 63),
                repeat("0", 65), repeat("g", 64), repeat("\uff10", 64), repeat("0", 63) + " "}) {
            reject(() -> DnaSeedGenerator.generate(bad, sample(), V1));
        }
    }

    public static void musicalMapping() {
        int[] base = {100000, 100000, 100000, 1000000, 100000, 60000, 800000, 10000};
        DnaGenome low = fixed(base, new int[12]);
        int[] high = base.clone(); high[0] = 900000;
        DnaGenome bass = fixed(high, new int[12]);
        check(bass.getBodyExpansionPpm() > low.getBodyExpansionPpm() && bass.getParticleCount() > low.getParticleCount(), "Bass mapping");
        high = base.clone(); high[3] = 9000000;
        DnaGenome centroid = fixed(high, new int[12]);
        check(centroid.getBrightnessPpm() > low.getBrightnessPpm() && centroid.getSurfaceSharpnessPpm() > low.getSurfaceSharpnessPpm(), "Centroid mapping");
        high = base.clone(); high[4] = 900000;
        check(fixed(high, new int[12]).getTransientBurstPpm() > low.getTransientBurstPpm(), "Flux mapping");
        high = base.clone(); high[5] = 180000;
        check(fixed(high, new int[12]).getRhythmMilliBpm() == 180000, "Tempo mapping");
        high[6] = 349999;
        check(fixed(high, new int[12]).getRhythmMilliBpm() == 0, "Uncertain tempo is not a beat grid");
        high = base.clone(); high[7] = 100000;
        check(fixed(high, new int[12]).getDeformationPpm() > low.getDeformationPpm(), "Dynamic range mapping");
        high = base.clone(); high[2] = 900000;
        check(fixed(high, new int[12]).getMicroDetailPpm() > low.getMicroDetailPpm(), "Treble mapping");
        high = base.clone(); high[1] = 900000;
        check(fixed(high, new int[12]).getFlowSpeedPpm() > low.getFlowSpeedPpm(), "Mid mapping");
        int[] chroma = new int[12]; chroma[9] = 1000000;
        DnaGenome tonal = fixed(base, chroma);
        check(tonal.getSymmetry() != low.getSymmetry() && tonal.chromaAt(9) == 1000000, "Chroma symmetry and palette mass");
    }

    public static void versionRejection() {
        equal(V1, DnaAlgorithmVersion.fromId(1), "V1 stable algorithm ID");
        for (int bad : new int[]{0, 2, -1, 65535}) reject(() -> DnaAlgorithmVersion.fromId(bad));
        byte[] genome = DnaCodec.encodeGenome(DnaGenomeMapper.generate(ID, sample(), V1));
        byte[] features = DnaCodec.encodeFeatures(sample());
        for (byte[] blob : new byte[][]{genome, features}) {
            ByteBuffer.wrap(blob).putShort(4, (short) 2); checksum(blob);
            if (blob.length == DnaCodec.GENOME_BYTES) reject(() -> DnaCodec.decodeGenome(blob));
            else reject(() -> DnaCodec.decodeFeatures(blob));
        }
        genome = DnaCodec.encodeGenome(DnaGenomeMapper.generate(ID, sample(), V1));
        ByteBuffer.wrap(genome).putShort(6, (short) 2); checksum(genome);
        final byte[] unknownAlgorithm = genome;
        reject(() -> DnaCodec.decodeGenome(unknownAlgorithm));
    }

    public static void corruptionAndLength() {
        for (byte[] blob : new byte[][]{DnaCodec.encodeFeatures(sample()), DnaCodec.encodeGenome(DnaGenomeMapper.generate(ID, sample(), V1))}) {
            boolean isGenome = blob.length == DnaCodec.GENOME_BYTES;
            for (int i = 0; i < blob.length; i++) {
                byte[] corrupt = blob.clone(); corrupt[i] ^= 1;
                reject(() -> decode(corrupt, isGenome));
                byte[] truncated = Arrays.copyOf(blob, i);
                reject(() -> decode(truncated, isGenome));
            }
            reject(() -> decode(Arrays.copyOf(blob, blob.length + 1), isGenome));
            byte[] wrongType = blob.clone(); wrongType[0] = 'X'; checksum(wrongType);
            reject(() -> decode(wrongType, isGenome));
            reject(() -> decode(new byte[1000000], isGenome));
        }
    }

    public static void malformedPayloads() {
        byte[] feature = DnaCodec.encodeFeatures(sample());
        for (int offset : new int[]{6, 10, 14, 18, 22, 26, 30, 34, 38}) {
            byte[] corrupt = feature.clone(); ByteBuffer.wrap(corrupt).putInt(offset, Integer.MAX_VALUE); checksum(corrupt);
            reject(() -> DnaCodec.decodeFeatures(corrupt));
        }
        byte[] genome = DnaCodec.encodeGenome(DnaGenomeMapper.generate(ID, sample(), V1));
        byte[] archetype = genome.clone(); archetype[24] = 4; checksum(archetype);
        reject(() -> DnaCodec.decodeGenome(archetype));
        for (int offset = 25; offset < 121; offset += 4) {
            byte[] corrupt = genome.clone(); ByteBuffer.wrap(corrupt).putInt(offset, Integer.MAX_VALUE); checksum(corrupt);
            reject(() -> DnaCodec.decodeGenome(corrupt));
        }
        byte[] mass = genome.clone(); ByteBuffer.wrap(mass).putInt(73, 1); checksum(mass);
        reject(() -> DnaCodec.decodeGenome(mass));
    }

    public static void silenceAndExtremes() {
        DnaFeatures silence = DnaFeatures.fromCanonical(0, 0, 0, 0, 0, 0, 0, 0, new int[12]);
        DnaFeatures maximum = DnaFeatures.fromCanonical(1000000, 1000000, 1000000, 11025000,
                1000000, 300000, 1000000, 120000, new int[12]);
        for (long seed : new long[]{0, -1, Long.MIN_VALUE, Long.MAX_VALUE}) {
            DnaGenome s = DnaGenomeMapper.map(silence, seed, V1);
            check(s.getRhythmMilliBpm() == 0, "Silence must not invent tempo");
            equal(s, DnaCodec.decodeGenome(DnaCodec.encodeGenome(s)), "Silent genome round trip");
            DnaGenome max = DnaGenomeMapper.map(maximum, seed, V1);
            check(max.getParticleCount() <= 32768 && max.getLayerCount() <= 5, "Bounded geometry budget");
            equal(max, DnaCodec.decodeGenome(DnaCodec.encodeGenome(max)), "Maximum genome round trip");
        }
    }

    public static void diverseSongs() {
        Set<String> visuals = new HashSet<>();
        Set<DnaGenome.Archetype> families = new HashSet<>();
        for (int i = 0; i < 1024; i++) {
            String id = String.format(Locale.ROOT, "%064x", i);
            DnaGenome g = DnaGenomeMapper.generate(id, sample(), V1);
            // Compare visible parameters, deliberately excluding seed and geometrySeed.
            visuals.add(g.getArchetype() + ":" + g.getParticleCount() + ":" + g.getBodyExpansionPpm()
                    + ":" + g.getSurfaceSharpnessPpm() + ":" + g.getPaletteRotationMilliDegrees());
            families.add(g.getArchetype());
        }
        check(visuals.size() == 1024 && families.size() == 3, "Distinct songs drive visual variations across families");
    }

    public static void concurrentDeterminism() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            DnaGenome expected = DnaGenomeMapper.generate(ID, sample(), V1);
            Callable<Boolean> run = () -> {
                for (int i = 0; i < 256; i++) {
                    if (!expected.equals(DnaGenomeMapper.generate(ID, sample(), V1))) return false;
                }
                return true;
            };
            for (Future<Boolean> result : pool.invokeAll(Arrays.asList(run, run, run, run))) {
                check(result.get(), "Independent concurrent genome generation");
            }
        } finally { pool.shutdownNow(); }
    }

    public static void roundTripCorpus() {
        SplitMix64 random = new SplitMix64(998877);
        for (int i = 0; i < 1000; i++) {
            int[] chroma = new int[12];
            int dominant = random.nextInt(12), other = (dominant + 1) % 12;
            chroma[dominant] = random.nextInt(1000001); chroma[other] = 1000000 - chroma[dominant];
            DnaFeatures f = DnaFeatures.fromCanonical(random.nextInt(1000001), random.nextInt(1000001),
                    random.nextInt(1000001), random.nextInt(11025001), random.nextInt(1000001),
                    1 + random.nextInt(300000), random.nextInt(1000001), random.nextInt(120001), chroma);
            equal(f, DnaCodec.decodeFeatures(DnaCodec.encodeFeatures(f)), "Feature round trip corpus");
            DnaGenome g = DnaGenomeMapper.map(f, random.nextLong(), V1);
            equal(g, DnaCodec.decodeGenome(DnaCodec.encodeGenome(g)), "Genome round trip corpus");
        }
    }

    public static void localeAndTimezoneIndependence() {
        Locale locale = Locale.getDefault(); TimeZone zone = TimeZone.getDefault();
        DnaGenome expected = DnaGenomeMapper.generate(ID, sample(), V1);
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR")); TimeZone.setDefault(TimeZone.getTimeZone("Asia/Dhaka"));
            equal(expected, DnaGenomeMapper.generate(ID, sample(), V1), "Locale/timezone cannot alter DNA");
        } finally { Locale.setDefault(locale); TimeZone.setDefault(zone); }
    }

    public static void main(String[] args) throws Exception {
        String[] names = {"splitMixGolden", "boundedRandom", "featureQuantization", "invalidFeatures",
                "defensiveCopies", "goldenCompatibility", "stableIdentity", "invalidIdentity", "musicalMapping",
                "versionRejection", "corruptionAndLength", "malformedPayloads", "silenceAndExtremes",
                "diverseSongs", "concurrentDeterminism", "roundTripCorpus", "localeAndTimezoneIndependence"};
        for (String name : names) {
            try { DnaCoreContracts.class.getMethod(name).invoke(null); }
            catch (java.lang.reflect.InvocationTargetException failure) {
                throw new AssertionError("FAIL: " + name, failure.getCause());
            }
            System.out.println("PASS: " + name);
        }
        System.out.println("PASS: " + names.length + " JVM contract groups");
    }

    private static DnaFeatures sample() {
        int[] chroma = new int[12]; chroma[0] = 250000; chroma[4] = 250000; chroma[9] = 500000;
        return DnaFeatures.fromCanonical(700000, 420000, 180000, 2100000, 310000, 124500, 870000, 18750, chroma);
    }

    private static DnaFeatures normalized(double bass, double[] chroma) {
        return DnaFeatures.fromNormalized(bass, 0.4, 0.2, 2000, 0.3, 120, 0.8, 20, chroma);
    }

    private static DnaGenome fixed(int[] f, int[] chroma) {
        return DnaGenomeMapper.map(DnaFeatures.fromCanonical(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], chroma), 42, V1);
    }

    private static Properties goldens() throws Exception {
        Properties p = new Properties();
        try (InputStream in = DnaCoreContracts.class.getResourceAsStream("/dna/genesis-v1.properties")) {
            check(in != null, "Golden resource must exist"); p.load(in);
        }
        return p;
    }

    private static void decode(byte[] blob, boolean genome) {
        if (genome) DnaCodec.decodeGenome(blob); else DnaCodec.decodeFeatures(blob);
    }

    private static void checksum(byte[] bytes) {
        CRC32 crc = new CRC32(); crc.update(bytes, 0, bytes.length - 4);
        ByteBuffer.wrap(bytes).putInt(bytes.length - 4, (int) crc.getValue());
    }

    private static String repeat(String value, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }

    private static byte[] unhex(String value) {
        check(value != null && value.length() % 2 == 0, "Valid fixture hex");
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        return bytes;
    }

    private static void reject(Runnable operation) {
        try { operation.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid input was accepted");
    }

    private static void equal(Object expected, Object actual, String message) { check(expected.equals(actual), message); }
    private static void bytes(byte[] expected, byte[] actual, String message) { check(Arrays.equals(expected, actual), message); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
