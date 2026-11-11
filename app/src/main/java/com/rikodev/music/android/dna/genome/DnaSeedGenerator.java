package com.rikodev.music.android.dna.genome;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/** Domain-separated stable identity; no file path, Android ID, locale, clock or hashCode. */
public final class DnaSeedGenerator {
    private static final byte[] DOMAIN = "RIKO:MDNA:GENESIS:SEED\0".getBytes(StandardCharsets.US_ASCII);

    private DnaSeedGenerator() {}

    /**
     * audioSha256 is exactly 64 hex characters of a stable audio-content SHA-256.
     * Upper/lower hex spellings are equivalent. P2 owns computing/caching the content
     * fingerprint; no metadata/path fallback is invented here. Features must be frozen
     * before calling. The first 8 digest bytes are interpreted as a big-endian long.
     */
    public static long generate(String audioSha256, DnaFeatures features, DnaAlgorithmVersion version) {
        Objects.requireNonNull(features, "features");
        Objects.requireNonNull(version, "version");
        byte[] identity = parseFingerprint(audioSha256);
        switch (version) {
            case GENESIS_V1:
                MessageDigest digest = sha256();
                digest.update(DOMAIN);
                digest.update((byte) (version.getId() >>> 8));
                digest.update((byte) version.getId());
                digest.update(identity);
                digest.update(DnaCodec.featurePayloadV1(features));
                return ByteBuffer.wrap(digest.digest()).getLong();
            default:
                throw new IllegalArgumentException("Unsupported DNA algorithm");
        }
    }

    private static byte[] parseFingerprint(String value) {
        Objects.requireNonNull(value, "audioSha256");
        if (value.length() != 64) throw new IllegalArgumentException("Expected SHA-256 audio fingerprint");
        byte[] bytes = new byte[32];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) ((hex(value.charAt(i * 2)) << 4) | hex(value.charAt(i * 2 + 1)));
        }
        return bytes;
    }

    private static int hex(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        throw new IllegalArgumentException("Fingerprint must contain ASCII hexadecimal only");
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossibleOnAndroidOrJava) {
            throw new IllegalStateException("SHA-256 unavailable", impossibleOnAndroidOrJava);
        }
    }
}
