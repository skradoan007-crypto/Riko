package com.rikodev.music.android.dna.analysis;

import com.rikodev.music.android.dna.genome.DnaFeatures;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/** One-shot incremental PCM analyzer. Never retains a song's PCM. Not thread-safe.
 * Successful finish is called by the owner only after source EOS. Input failure
 * poisons the instance; cancellation or errors must discard it.
 */
public final class DnaStreamingAnalyzer {
    public static final int RATE = 22050, FFT_SIZE = 2048, HOP = 441;
    public static final int DEFAULT_MAX_SECONDS = 7200;
    private final Runnable cancelled;
    private final int maxSeconds;
    private final OfflineFft fft = new OfflineFft(FFT_SIZE);
    private final double[] ring = new double[FFT_SIZE * 2], real = new double[FFT_SIZE],
            imag = new double[FFT_SIZE], window = new double[FFT_SIZE],
            previousMagnitude = new double[FFT_SIZE / 2 + 1], chroma = new double[12], chromaSum = new double[12];
    private final int[] pitchClass = new int[FFT_SIZE / 2 + 1], frame = new int[DnaTimeline.WIDTH];
    private final long[] levelHistogram = new long[1201];
    private final OnsetTempo tempo = new OnsetTempo();
    private final DnaTimeline.Builder timeline;
    private final MessageDigest digest;
    private final byte[] hashBuffer = new byte[4096];
    private int hashBytes, inputRate, channels;
    private long inputFrames, originUs, samples, nextCenter, windows;
    private double windowPower, bassSum, midSum, trebleSum, centroidSum, fluxSum;
    private double previousMagnitudeSum, onsetAverage;
    private long audibleWindows;
    private boolean terminal, poisoned;
    private SincResampler resampler;

    public DnaStreamingAnalyzer(Runnable checkCancelled) { this(checkCancelled, DEFAULT_MAX_SECONDS); }
    public DnaStreamingAnalyzer(Runnable checkCancelled, int maxSeconds) {
        if (maxSeconds < 1 || maxSeconds > 21600) throw new IllegalArgumentException("Duration limit must be 1s..6h");
        this.maxSeconds = maxSeconds;
        cancelled = java.util.Objects.requireNonNull(checkCancelled);
        timeline = new DnaTimeline.Builder(maxSeconds * 10);
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        digest.update("RIKO:MDNA:PCM:ANALYZER1:22050:S16LE\0".getBytes(StandardCharsets.US_ASCII));
        for (int i = 0; i < FFT_SIZE; i++) {
            window[i] = 0.5 - 0.5 * StrictMath.cos(2 * StrictMath.PI * i / (FFT_SIZE - 1));
            windowPower += window[i] * window[i];
        }
        Arrays.fill(pitchClass, -1);
        for (int bin = 1; bin < pitchClass.length; bin++) {
            double hz = (double) bin * RATE / FFT_SIZE;
            if (hz >= 50 && hz <= 5000) {
                int midi = (int) StrictMath.floor(69 + 12 * StrictMath.log(hz / 440) / StrictMath.log(2) + 0.5);
                pitchClass[bin] = Math.floorMod(midi, 12);
            }
        }
    }

    /** 16-bit (2) or float (4), interleaved LE; negative priming timestamps are trimmed. */
    public void accept(ByteBuffer pcm, int offset, int size, long ptsUs, int rate, int channelCount, int encoding) {
        if (terminal || poisoned) throw new IllegalStateException("Analyzer no longer accepts input");
        try {
            cancelled.run();
            if (rate < 8000 || rate > 192000 || channelCount < 1 || channelCount > 32
                    || (encoding != 2 && encoding != 4)) throw new IllegalArgumentException("Unsupported PCM format");
            int width = encoding == 4 ? 4 : 2, stride = width * channelCount;
            if (offset < 0 || size < 0 || (long) offset + size > pcm.limit() || size % stride != 0) {
                throw new IllegalArgumentException("Invalid PCM bounds/alignment");
            }
            if (ptsUs < -10_000_000L || ptsUs > (long) maxSeconds * 1_000_000) {
                throw new IllegalArgumentException("Invalid/over-limit PCM timestamp");
            }
            if (size == 0) return;
            if (resampler == null) {
                if (ptsUs > 2000) throw new DnaTimestampException(true, 0, ptsUs, 0, rate);
                inputRate = rate; channels = channelCount; originUs = ptsUs;
                resampler = new SincResampler(rate, this::sample);
            }
            if (inputRate != rate || channels != channelCount) throw new IllegalArgumentException("Midstream PCM format change");
            long expectedUs = originUs + inputFrames * 1_000_000 / rate;
            if (Math.abs(ptsUs - expectedUs) > 2000)
                throw new DnaTimestampException(false, expectedUs, ptsUs, inputFrames, rate);
            ByteBuffer data = pcm.duplicate().order(ByteOrder.LITTLE_ENDIAN);
            int frames = size / stride;
            for (int i = 0; i < frames; i++) {
                if ((i & 1023) == 0) cancelled.run();
                long timeUs = originUs + inputFrames++ * 1_000_000 / rate;
                if (timeUs < 0) continue;
                if (timeUs >= (long) maxSeconds * 1_000_000) throw new IllegalArgumentException("Analysis duration limit exceeded");
                double mono = 0;
                for (int c = 0; c < channels; c++) {
                    int index = offset + i * stride + c * width;
                    double value = encoding == 4 ? data.getFloat(index) : data.getShort(index) / 32768.0;
                    mono += Double.isFinite(value) ? Math.max(-1, Math.min(1, value)) : 0;
                }
                resampler.add(mono / channels);
            }
        } catch (RuntimeException error) { poisoned = true; throw error; }
    }

    private void sample(double value) {
        if (samples >= (long) maxSeconds * RATE) throw new IllegalArgumentException("Analysis duration limit exceeded");
        ring[(int) (samples++ % ring.length)] = value;
        int quantized = (int) Math.max(-32768, Math.min(32767, StrictMath.floor(value * 32768 + 0.5)));
        hashBuffer[hashBytes++] = (byte) quantized;
        hashBuffer[hashBytes++] = (byte) (quantized >> 8);
        if (hashBytes == hashBuffer.length) { digest.update(hashBuffer); hashBytes = 0; }
        while (nextCenter + FFT_SIZE / 2 <= samples) analyzeWindow();
    }

    private void analyzeWindow() {
        cancelled.run();
        Arrays.fill(imag, 0);
        double square = 0;
        int valid = 0;
        for (int i = 0; i < FFT_SIZE; i++) {
            long index = nextCenter - FFT_SIZE / 2 + i;
            double value = index < 0 || index >= samples ? 0 : ring[(int) (index % ring.length)];
            if (index >= 0 && index < samples) { square += value * value; valid++; }
            real[i] = value * window[i];
        }
        double rms = valid == 0 ? 0 : StrictMath.sqrt(square / valid);
        fft.forward(real, imag);
        Arrays.fill(chroma, 0);
        double bass = 0, mid = 0, treble = 0, powerSum = 0, weighted = 0, positive = 0, magnitudeSum = 0;
        for (int bin = 1; bin <= FFT_SIZE / 2; bin++) {
            double hz = (double) bin * RATE / FFT_SIZE;
            double power = (real[bin] * real[bin] + imag[bin] * imag[bin])
                    * (bin == FFT_SIZE / 2 ? 1 : 2) / (FFT_SIZE * windowPower);
            double magnitude = StrictMath.sqrt(power);
            positive += Math.max(0, magnitude - previousMagnitude[bin]);
            previousMagnitude[bin] = magnitude; magnitudeSum += magnitude;
            if (hz < 20) continue;
            if (hz < 250) bass += power; else if (hz < 4000) mid += power; else treble += power;
            weighted += hz * power; powerSum += power;
            if (pitchClass[bin] >= 0) chroma[pitchClass[bin]] += power;
        }
        double flux = windows == 0 || rms < 0.00001 ? 0 :
                Math.min(1, positive / Math.max(1e-12, Math.max(magnitudeSum, previousMagnitudeSum)));
        previousMagnitudeSum = magnitudeSum;
        double onset = Math.max(0, flux - onsetAverage);
        onsetAverage += (flux - onsetAverage) * 0.1;
        tempo.add(onset);
        // Fixed full-scale RMS compression, not per-song peak normalization.
        bass = Math.min(1, StrictMath.sqrt(bass)); mid = Math.min(1, StrictMath.sqrt(mid)); treble = Math.min(1, StrictMath.sqrt(treble));
        double centroid = powerSum > 1e-10 ? weighted / powerSum : 0;
        bassSum += bass; midSum += mid; trebleSum += treble; fluxSum += flux;
        if (rms >= 0.00001) { centroidSum += centroid; audibleWindows++; }
        for (int i = 0; i < 12; i++) chromaSum[i] += chroma[i];
        int dbBin = (int) Math.round(Math.max(0, Math.min(1200, (20 * StrictMath.log10(Math.max(rms, 1e-6)) + 120) * 10)));
        levelHistogram[dbBin]++;
        if (windows % 5 == 0) {
            frame[0] = ppm(bass); frame[1] = ppm(mid); frame[2] = ppm(treble);
            frame[3] = (int) Math.round(centroid * 1000); frame[4] = ppm(flux);
            frame[5] = ppm(rms); frame[6] = ppm(onset);
            double sum = 0; for (double c : chroma) sum += c;
            int total = 0, largest = 0;
            for (int i = 0; i < 12; i++) {
                if (chroma[i] > chroma[largest]) largest = i;
                frame[7 + i] = sum > 1e-10 ? (int) (chroma[i] / sum * 1000000) : 0;
                total += frame[7 + i];
            }
            if (sum > 1e-10) frame[7 + largest] += 1000000 - total;
            timeline.add(frame);
        }
        windows++; nextCenter += HOP;
    }

    /** Call only after complete source EOS. Cooperative cancellation also covers the final tail. */
    public DnaAnalysis finish() {
        if (terminal || poisoned) throw new IllegalStateException("Analyzer is finished or failed");
        terminal = true;
        cancelled.run();
        if (resampler == null) throw new IllegalArgumentException("Empty decoded audio");
        resampler.finish();
        if (samples == 0) throw new IllegalArgumentException("No non-priming audio");
        while (nextCenter < samples) analyzeWindow();
        cancelled.run();
        digest.update(hashBuffer, 0, hashBytes);
        byte[] hash = digest.digest();
        StringBuilder fingerprint = new StringBuilder(64);
        for (byte b : hash) { fingerprint.append(Character.forDigit((b >>> 4) & 15, 16)); fingerprint.append(Character.forDigit(b & 15, 16)); }
        TempoEstimate estimate = tempo.finish();
        double sum = 0; for (double c : chromaSum) sum += c;
        double[] normalized = new double[12];
        if (sum > 1e-10) for (int i = 0; i < 12; i++) normalized[i] = chromaSum[i] / sum;
        double rangeDb = (percentile(0.95) - percentile(0.10)) / 10.0;
        DnaFeatures features = DnaFeatures.fromNormalized(bassSum / windows, midSum / windows,
                trebleSum / windows, audibleWindows == 0 ? 0 : centroidSum / audibleWindows,
                fluxSum / windows, estimate.getBpm(), estimate.getConfidence(), rangeDb, normalized);
        long durationUs = (samples * 1_000_000 + RATE - 1) / RATE;
        return new DnaAnalysis(features, timeline.finish(durationUs), estimate, fingerprint.toString());
    }

    private int percentile(double fraction) {
        long target = Math.max(1, (long) StrictMath.ceil(windows * fraction)), total = 0;
        for (int i = 0; i < levelHistogram.length; i++) { total += levelHistogram[i]; if (total >= target) return i; }
        return 1200;
    }
    private static int ppm(double value) { return (int) Math.round(Math.max(0, Math.min(1, value)) * 1000000); }
}
