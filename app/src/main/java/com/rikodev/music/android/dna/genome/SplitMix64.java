package com.rikodev.music.android.dna.genome;

/**
 * SplitMix64, Sebastiano Vigna's 2015 public-domain reference algorithm:
 * https://prng.di.unimi.it/splitmix64.c
 * All arithmetic wraps modulo 2^64. Instance-local, deterministic, not cryptographic.
 * Do not share mutable instances between threads. No platform Random dependency.
 */
public final class SplitMix64 {
    private long state;

    public SplitMix64(long seed) { state = seed; }

    public long nextLong() {
        long z = (state += 0x9e3779b97f4a7c15L);
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    /** Uniform [0, bound), with rejection instead of biased modulo reduction. */
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        long bits;
        long value;
        do {
            bits = nextLong() >>> 1;
            value = bits % bound;
        } while (bits - value + (bound - 1L) < 0L);
        return (int) value;
    }
}
