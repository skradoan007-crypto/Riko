package com.rikodev.music.android.dna.analysis;

import com.rikodev.music.android.dna.genome.DnaCodec;
import com.rikodev.music.android.dna.genome.DnaFeatures;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

/** Synthetic, license-free sample streams generated in bounded chunks, not entire songs. */
public final class DnaAnalyzerContracts {
    interface Signal { double at(int sample, int channel, int rate); }
    private DnaAnalyzerContracts() {}
    private static Signal sine(double hz, double amplitude) {
        return (n, c, rate) -> amplitude * StrictMath.sin(2 * StrictMath.PI * hz * n / rate);
    }
    private static DnaAnalysis analyze(int rate, int channels, double seconds, int chunk, int encoding, Signal signal) {
        DnaStreamingAnalyzer analyzer = new DnaStreamingAnalyzer(() -> {});
        feed(analyzer, rate, channels, (int) (seconds * rate), chunk, encoding, signal);
        return analyzer.finish();
    }
    private static void feed(DnaStreamingAnalyzer analyzer, int rate, int channels, int count,
                             int chunk, int encoding, Signal signal) {
        int width = encoding == 4 ? 4 : 2;
        ByteBuffer buffer = ByteBuffer.allocate(chunk * channels * width + 8).order(ByteOrder.LITTLE_ENDIAN);
        for (int start = 0; start < count; start += chunk) {
            int frames = Math.min(chunk, count - start);
            buffer.clear(); buffer.position(4);
            for (int i = 0; i < frames; i++) for (int c = 0; c < channels; c++) {
                double value = signal.at(start + i, c, rate);
                if (encoding == 4) buffer.putFloat((float) value);
                else buffer.putShort((short) Math.max(-32768, Math.min(32767, Math.round(value * 32768))));
            }
            int size = frames * channels * width;
            buffer.limit(size + 4); buffer.position(2);
            analyzer.accept(buffer, 4, size, start * 1_000_000L / rate, rate, channels, encoding);
            check(buffer.position() == 2 && buffer.order() == ByteOrder.LITTLE_ENDIAN, "Borrowed PCM unchanged");
        }
    }
    public static void tonesAndChroma() {
        double[] frequencies = {80, 440, 6000};
        for (int i = 0; i < frequencies.length; i++) {
            DnaAnalysis a = analyze(22050, 1, 2, 733, 4, sine(frequencies[i], 0.6));
            DnaFeatures f = a.getFeatures();
            near(f.getCentroidMilliHz() / 1000.0, frequencies[i], 12, "Tone centroid");
            int[] bands = {f.getBassEnergyPpm(), f.getMidEnergyPpm(), f.getTrebleEnergyPpm()};
            check(bands[i] > 400000, "Tone energy reaches its band");
            check(f.getTempoMilliBpm() == 0, "Sustained tone has no fabricated BPM");
            if (i == 1) {
                int dominant = 0;
                for (int c = 1; c < 12; c++) if (f.chromaAt(c) > f.chromaAt(dominant)) dominant = c;
                check(dominant == 9, "440 Hz produces A chroma");
                check(f.getDynamicRangeMilliDb() < 1000, "Steady tone has small dynamic range");
            }
        }
    }
    public static void antiAliasing() {
        double pass = resampledRms(48000, 1000), stop = resampledRms(48000, 15000);
        near(pass, 0.5 / StrictMath.sqrt(2), 0.002, "Resampler passband amplitude");
        check(stop < pass / 1000, "15 kHz rejected by >60 dB before 48k to 22.05k downsample");
        near(resampledRms(8000, 1000), pass, 0.003, "Upsampling passband");
        near(resampledRms(96000, 1000), pass, 0.003, "96k downsampling passband");
    }
    private static double resampledRms(int rate, double frequency) {
        final double[] power = {0}; final int[] total = {0}, used = {0};
        SincResampler r = new SincResampler(rate, sample -> {
            int index = total[0]++;
            if (index > 1000 && index < 21000) { power[0] += sample * sample; used[0]++; }
        });
        for (int i = 0; i < rate; i++) r.add(0.5 * StrictMath.sin(2 * StrictMath.PI * frequency * i / rate));
        r.finish(); check(total[0] == 22050, "Exact one-second resampler count");
        return StrictMath.sqrt(power[0] / used[0]);
    }
    public static void chunkInvariance() {
        Signal mixed = (n, c, r) -> 0.3 * StrictMath.sin(2 * StrictMath.PI * 110 * n / r)
                + 0.2 * StrictMath.sin(2 * StrictMath.PI * 880 * n / r);
        DnaAnalysis a = analyze(44100, 2, 2.137, 127, 2, mixed);
        DnaAnalysis b = analyze(44100, 2, 2.137, 4093, 2, mixed);
        equal(a, b);
        check(a.getTimeline().getFrameCount() == 22, "Timeline includes partial final tenth-second");
    }
    public static void channelsAndPcmNormalization() {
        Signal positive = sine(440, 0.5);
        DnaAnalysis mono = analyze(22050, 1, 1, 337, 4, positive);
        DnaAnalysis stereo = analyze(22050, 2, 1, 337, 4, positive);
        equal(mono, stereo);
        DnaAnalysis inverted = analyze(22050, 2, 1, 337, 4, (n,c,r) -> positive.at(n,c,r) * (c == 0 ? 1 : -1));
        DnaAnalysis silence = analyze(22050, 1, 1, 337, 4, (n,c,r) -> 0);
        equal(inverted, silence);
        DnaAnalysis badFloats = analyze(22050, 1, 1, 337, 4, (n,c,r) -> n % 2 == 0 ? Double.NaN : Double.POSITIVE_INFINITY);
        equal(badFloats, silence);
        DnaAnalysis clipped = analyze(22050, 4, 1, 337, 4, (n,c,r) -> positive.at(n,c,r) >= 0 ? 10 : -10);
        DnaAnalysis bounded = analyze(22050, 1, 1, 337, 4, (n,c,r) -> positive.at(n,c,r) >= 0 ? 1 : -1);
        equal(clipped, bounded);
    }
    public static void timelineAndSilence() {
        DnaAnalysis a = analyze(48000, 1, 1.25, 503, 2, (n,c,r) -> 0);
        DnaTimeline t = a.getTimeline();
        check(t.getFrameCount() == 13, "10 Hz timeline count");
        near(t.getDurationUs(), 1250000, 50, "Timeline duration");
        for (int i = 0; i < t.getFrameCount(); i++) {
            check(t.timeUs(i) == i * 100000L, "Timestamp grid");
            for (int j = 0; j < DnaTimeline.WIDTH; j++) check(t.valueAt(i,j) == 0, "Silence features");
        }
        check(a.getFeatures().getTempoMilliBpm() == 0 && a.getTempo().getConfidence() == 0, "Silence tempo unknown");
        check(a.getAudioSha256().matches("[0-9a-f]{64}"), "Portable content fingerprint");
        int[] copy = new int[DnaTimeline.WIDTH]; t.copyFrame(0, copy); copy[0] = 1000000;
        check(t.valueAt(0,0) == 0, "Timeline immutability");
    }
    public static void tempoAndAmbiguity() {
        for (double bpm : new double[]{60, 90, 120, 180}) {
            DnaAnalysis a = analyze(22050, 1, 16, 991, 4, (n,c,r) -> {
                double beatTime = (n / (double) r) % (60 / bpm);
                return beatTime < 0.045 ? 0.8 * StrictMath.sin(2 * StrictMath.PI * 1000 * n / r) : 0;
            });
            TempoEstimate e = a.getTempo();
            near(e.getBpm(), bpm, 3, "Pulse tempo " + bpm + " actual=" + e.getBpm());
            check(e.getConfidence() > 0.35, "Periodic pulse confidence");
            if (bpm == 120) {
                check(e.isOctaveAmbiguous(), "Half-time alternative retained");
                near(e.getAlternativeBpm(), 60, 2, "Half-time BPM");
            }
        }
    }
    public static void dynamicRangeAndFlux() {
        DnaAnalysis a = analyze(22050, 1, 4, 673, 4, (n,c,r) ->
                (n < r * 2 ? 0.6 : 0.06) * StrictMath.sin(2 * StrictMath.PI * 440 * n / r));
        near(a.getFeatures().getDynamicRangeMilliDb() / 1000.0, 20, 1, "20 dB amplitude contrast");
        DnaAnalysis change = analyze(22050, 1, 4, 673, 4, (n,c,r) ->
                0.6 * StrictMath.sin(2 * StrictMath.PI * (n < r * 2 ? 440 : 3000) * n / r));
        DnaTimeline t = change.getTimeline();
        check(t.valueAt(20, DnaTimeline.FLUX) > t.valueAt(10, DnaTimeline.FLUX) + 100000, "Localized spectral transient");
    }
    public static void cancellation() {
        AtomicInteger calls = new AtomicInteger();
        DnaStreamingAnalyzer analyzer = new DnaStreamingAnalyzer(() -> {
            if (calls.incrementAndGet() > 5) throw new CancellationException("test cancellation");
        });
        try { feed(analyzer, 48000, 1, 48000, 2048, 4, sine(440,0.5)); throw new AssertionError("Cancellation swallowed"); }
        catch (CancellationException expected) { /* expected */ }
        rejectState(analyzer::finish);
        AtomicInteger finishCancel = new AtomicInteger();
        DnaStreamingAnalyzer tail = new DnaStreamingAnalyzer(() -> {
            if (finishCancel.get() == 1) throw new CancellationException();
        });
        feed(tail, 22050,1,100,100,2,sine(440,0.5)); finishCancel.set(1);
        try { tail.finish(); throw new AssertionError("Finish cancellation swallowed"); }
        catch (CancellationException expected) { /* expected */ }
    }
    public static void discontinuitiesAndFormats() {
        ByteBuffer pcm = ByteBuffer.allocate(400).order(ByteOrder.LITTLE_ENDIAN);
        for (int encoding : new int[]{0, 3, 21}) reject(() -> new DnaStreamingAnalyzer(() -> {}).accept(pcm,0,400,0,22050,1,encoding));
        reject(() -> new DnaStreamingAnalyzer(() -> {}).accept(pcm,0,399,0,22050,1,2));
        reject(() -> new DnaStreamingAnalyzer(() -> {}).accept(pcm,1,400,0,22050,1,2));
        reject(() -> new DnaStreamingAnalyzer(() -> {}).accept(pcm,0,400,0,7999,1,2));
        reject(() -> new DnaStreamingAnalyzer(() -> {}).accept(pcm,0,400,0,22050,33,2));
        DnaStreamingAnalyzer a = new DnaStreamingAnalyzer(() -> {});
        a.accept(pcm,0,400,0,22050,1,2);
        reject(() -> a.accept(pcm,0,400,500000,22050,1,2)); rejectState(a::finish);
        DnaStreamingAnalyzer b = new DnaStreamingAnalyzer(() -> {});
        b.accept(pcm,0,400,0,22050,1,2);
        reject(() -> b.accept(pcm,0,400,9070,44100,1,2));
        reject(() -> new DnaStreamingAnalyzer(() -> {}).accept(pcm,0,400,10000,22050,1,2));
    }
    public static void limitsAndEmpty() {
        reject(() -> new DnaStreamingAnalyzer(() -> {}).finish());
        DnaStreamingAnalyzer shortLimit = new DnaStreamingAnalyzer(() -> {}, 1);
        reject(() -> feed(shortLimit, 22050,1,22051,997,2,sine(440,0.5))); rejectState(shortLimit::finish);
        DnaStreamingAnalyzer exactLimit = new DnaStreamingAnalyzer(() -> {}, 1);
        feed(exactLimit,22050,1,22050,997,2,sine(440,0.5));
        check(exactLimit.finish().getTimeline().getFrameCount() == 10, "Exact duration limit accepted");
        rejectState(exactLimit::finish);
        reject(() -> new DnaStreamingAnalyzer(() -> {}, 0));
        reject(() -> new DnaStreamingAnalyzer(() -> {}, 21601));
    }
    public static void resamplerTimeAndPriming() {
        for (int rate : new int[]{8000, 22050, 44100, 48000, 96000, 192000}) {
            final int[] count = {0};
            SincResampler r = new SincResampler(rate, v -> count[0]++);
            for (int i = 0; i < 1001; i++) r.add(0);
            r.finish();
            check(count[0] == (1001L * 22050 + rate - 1) / rate, "Rational sample count " + rate);
            rejectState(r::finish);
        }
        DnaStreamingAnalyzer a = new DnaStreamingAnalyzer(() -> {});
        ByteBuffer b = ByteBuffer.allocate(882).order(ByteOrder.LITTLE_ENDIAN);
        a.accept(b,0,b.capacity(),-10000,22050,1,2);
        DnaAnalysis result = a.finish();
        check(result.getTimeline().getDurationUs() > 9900 && result.getTimeline().getDurationUs() < 10100, "Negative priming trimmed");
    }
    public static void deterministicGenomeInput() {
        DnaAnalysis a = analyze(48000,1,2,1024,2,sine(880,0.5));
        DnaAnalysis b = analyze(48000,1,2,771,2,sine(880,0.5));
        equal(a,b);
        DnaAnalysis changed = analyze(48000,1,2,1024,2,sine(660,0.5));
        check(!a.getAudioSha256().equals(changed.getAudioSha256()), "Content fingerprint distinguishes tone");
        com.rikodev.music.android.dna.genome.DnaGenome genome = com.rikodev.music.android.dna.genome.DnaGenomeMapper.generate(
                a.getAudioSha256(), a.getFeatures(), com.rikodev.music.android.dna.genome.DnaAlgorithmVersion.GENESIS_V1);
        check(genome.equals(com.rikodev.music.android.dna.genome.DnaGenomeMapper.generate(
                b.getAudioSha256(), b.getFeatures(), com.rikodev.music.android.dna.genome.DnaAlgorithmVersion.GENESIS_V1)), "P2 feeds stable P1 genome");
    }
    public static void frozenAnalyzerVersion() throws Exception {
        java.util.Properties fixture = new java.util.Properties();
        try (java.io.InputStream input = DnaAnalyzerContracts.class.getResourceAsStream("/dna/analyzer-v1.properties")) {
            check(input != null, "Analyzer golden fixture exists"); fixture.load(input);
        }
        DnaAnalysis a = analyze(22050,1,2,137,2,(n,c,r) -> ((n % 100)-50) / 64.0);
        check(Integer.toString(a.getAnalyzerVersion()).equals(fixture.getProperty("analyzerVersion")), "Analyzer version frozen");
        check(a.getAudioSha256().equals(fixture.getProperty("audioSha256")), "V1 PCM fingerprint golden");
        check(hex(DnaCodec.encodeFeatures(a.getFeatures())).equals(fixture.getProperty("features")), "V1 features golden");
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        ByteBuffer value = ByteBuffer.allocate(4);
        for (int i = 0; i < a.getTimeline().getFrameCount(); i++) for (int j = 0; j < DnaTimeline.WIDTH; j++) {
            value.clear(); value.putInt(a.getTimeline().valueAt(i,j)); digest.update(value.array());
        }
        check(hex(digest.digest()).equals(fixture.getProperty("timelineSha256")), "V1 timeline golden");
    }
    private static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        for (byte b : bytes) { text.append(Character.forDigit((b >>> 4) & 15,16)); text.append(Character.forDigit(b & 15,16)); }
        return text.toString();
    }
    public static void safeFailureReasons() {
        check(DnaAnalysisFailure.classify(new IllegalArgumentException("Decoder timestamp discontinuity")) == DnaAnalysisFailure.TIMESTAMP, "Timestamp reason retained");
        check(DnaAnalysisFailure.classify(new java.io.IOException("Decode ended before declared duration")) == DnaAnalysisFailure.INCOMPLETE, "Truncated decode reason retained");
        check(DnaAnalysisFailure.classify(new IllegalArgumentException("Unsupported PCM format")) == DnaAnalysisFailure.FORMAT, "PCM reason retained");
        check(DnaAnalysisFailure.classify(new java.io.FileNotFoundException("content://private/song")) == DnaAnalysisFailure.MISSING_FILE, "Missing file classified");
        check(DnaAnalysisFailure.classify(new SecurityException("/private/music")) == DnaAnalysisFailure.ACCESS, "Permission failure classified");
        DnaAnalysisFailure unknown = DnaAnalysisFailure.classify(new java.io.IOException("secret file /private/song.mp3"));
        check(unknown == DnaAnalysisFailure.DECODE && !unknown.getDescription().contains("private"), "Unknown provider messages stay private");
        check(DnaAnalysisFailure.classify(new IllegalArgumentException()) == DnaAnalysisFailure.ANALYSIS, "Null messages handled");
    }
    public static void main(String[] args) throws Exception {
        String[] tests = {"tonesAndChroma","antiAliasing","chunkInvariance","channelsAndPcmNormalization",
                "timelineAndSilence","tempoAndAmbiguity","dynamicRangeAndFlux","cancellation",
                "discontinuitiesAndFormats","limitsAndEmpty","resamplerTimeAndPriming","deterministicGenomeInput","frozenAnalyzerVersion","safeFailureReasons"};
        for (String name : tests) {
            try { DnaAnalyzerContracts.class.getMethod(name).invoke(null); }
            catch (java.lang.reflect.InvocationTargetException error) { throw new AssertionError("FAIL: " + name, error.getCause()); }
            System.out.println("PASS: " + name);
        }
        System.out.println("PASS: " + tests.length + " offline analyzer JVM contract groups");
    }
    private static void equal(DnaAnalysis a,DnaAnalysis b) {
        check(Arrays.equals(DnaCodec.encodeFeatures(a.getFeatures()), DnaCodec.encodeFeatures(b.getFeatures())), "Identical canonical features");
        check(a.getAudioSha256().equals(b.getAudioSha256()), "Identical fingerprint");
        DnaTimeline x=a.getTimeline(), y=b.getTimeline();
        check(x.getFrameCount()==y.getFrameCount() && x.getDurationUs()==y.getDurationUs(), "Identical timeline extent");
        for(int i=0;i<x.getFrameCount();i++) for(int j=0;j<DnaTimeline.WIDTH;j++) check(x.valueAt(i,j)==y.valueAt(i,j),"Identical timeline values");
    }
    private static void reject(Runnable r) { try { r.run(); } catch(IllegalArgumentException expected) { return; } throw new AssertionError("Invalid input accepted"); }
    private static void rejectState(Runnable r) { try { r.run(); } catch(IllegalStateException expected) { return; } throw new AssertionError("Invalid state accepted"); }
    private static void near(double value,double expected,double tolerance,String reason) { check(Math.abs(value-expected)<=tolerance,reason+": "+value); }
    private static void check(boolean condition,String reason) { if(!condition) throw new AssertionError(reason); }
}
