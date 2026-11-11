package com.rikodev.music.android.dna.analysis;

/** Double-precision offline radix-2 FFT. Live Float FFT and its timing stay unchanged. */
final class OfflineFft {
    private final int size;
    private final double[] cos, sin;
    OfflineFft(int size) {
        if (size < 2 || (size & (size - 1)) != 0) throw new IllegalArgumentException("FFT size");
        this.size = size;
        cos = new double[size / 2]; sin = new double[size / 2];
        for (int i = 0; i < cos.length; i++) {
            cos[i] = StrictMath.cos(-2 * StrictMath.PI * i / size);
            sin[i] = StrictMath.sin(-2 * StrictMath.PI * i / size);
        }
    }
    void forward(double[] real, double[] imag) {
        for (int i = 1, j = 0; i < size; i++) {
            int bit = size >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                double t = real[i]; real[i] = real[j]; real[j] = t;
                t = imag[i]; imag[i] = imag[j]; imag[j] = t;
            }
        }
        for (int length = 2; length <= size; length <<= 1) {
            int half = length / 2, stride = size / length;
            for (int start = 0; start < size; start += length) {
                for (int j = 0; j < half; j++) {
                    int a = start + j, b = a + half, t = j * stride;
                    double re = real[b] * cos[t] - imag[b] * sin[t];
                    double im = real[b] * sin[t] + imag[b] * cos[t];
                    real[b] = real[a] - re; imag[b] = imag[a] - im;
                    real[a] += re; imag[a] += im;
                }
            }
        }
    }
}
