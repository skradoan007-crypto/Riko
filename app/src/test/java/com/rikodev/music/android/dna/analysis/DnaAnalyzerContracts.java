package com.rikodev.music.android.dna.analysis;

import com.rikodev.music.android.dna.genome.DnaCodec;
import com.rikodev.music.android.audiofx.Mp3PcmTimestampNormalizer;
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
        feed(analyzer, rate, channels, count, chunk, encoding, signal, 0);
    }
    private static void feed(DnaStreamingAnalyzer analyzer, int rate, int channels, int count,
                             int chunk, int encoding, Signal signal, long startUs) {
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
            analyzer.accept(buffer, 4, size, startUs + start * 1_000_000L / rate, rate, channels, encoding);
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
        reject(() -> new DnaStreamingAnalyzer(() -> {}).accept(pcm,0,400,DnaTimeline.MAX_START_US + 1,22050,1,2));
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
    // Mirrors AOSP OMX's first output trimmed by 529 PCM frames, while its next
    // timestamp still counts those frames. C2 counts delivered frames instead.
    private static DnaAnalysis mp3Trace(int rate, boolean legacy, boolean normalize, int firstPackets) {
        DnaStreamingAnalyzer analyzer = new DnaStreamingAnalyzer(() -> {});
        Mp3PcmTimestampNormalizer clock = new Mp3PcmTimestampNormalizer(true);
        int packetFrames = rate >= 32000 ? 1152 : 576;
        int framesRead = 0, block = 0;
        ByteBuffer pcm = ByteBuffer.allocate(4608).order(ByteOrder.LITTLE_ENDIAN);
        while (framesRead < rate) {
            int count = Math.min(rate - framesRead, block == 0 ? packetFrames * firstPackets - 529 : packetFrames);
            pcm.clear();
            for (int i = 0; i < count; i++) pcm.putShort((short) Math.round(12000 * StrictMath.sin(2 * StrictMath.PI * 440 * (framesRead + i) / rate)));
            pcm.flip();
            long ptsUs = (framesRead + (legacy && block > 0 ? 529L : 0L)) * 1_000_000 / rate;
            if (normalize) ptsUs = clock.normalize(ptsUs, count, rate);
            analyzer.accept(pcm, 0, count * 2, ptsUs, rate, 1, 2);
            framesRead += count; block++;
        }
        if (normalize) check(clock.getCorrectionUs() == (legacy ? 529_000_000L / rate : 0), "Correct convention detected");
        return analyzer.finish();
    }
    public static void mp3DecoderTimestampConventions() {
        for (int rate : new int[]{8000,16000,22050,24000,32000,44100,48000}) {
            reject(() -> mp3Trace(rate, true, false, 1)); // Reproduces the old TIMESTAMP failure.
            DnaAnalysis legacy = mp3Trace(rate, true, true, 1);
            DnaAnalysis c2 = mp3Trace(rate, false, true, 1);
            equal(legacy, c2);
            equal(legacy, mp3Trace(rate, true, true, 2)); // Batched first output.
            near(legacy.getTimeline().getDurationUs(), 1_000_000, 50, "Canonical one-second timeline");
        }
    }
    public static void timestampCorrectionDoesNotHideLostAudio() {
        long offset = 529_000_000L / 44100;
        Mp3PcmTimestampNormalizer wrongMime = new Mp3PcmTimestampNormalizer(false);
        wrongMime.normalize(0,623,44100);
        check(wrongMime.normalize(26122,1152,44100) == 26122, "Non-MP3 untouched");
        Mp3PcmTimestampNormalizer untrimmed = new Mp3PcmTimestampNormalizer(true);
        untrimmed.normalize(0,1152,44100);
        check(untrimmed.normalize(26122 + offset,1152,44100) == 26122 + offset, "Gap without trimmed first output untouched");
        Mp3PcmTimestampNormalizer wrongGap = new Mp3PcmTimestampNormalizer(true);
        wrongGap.normalize(0,623,44100);
        check(wrongGap.normalize(28122,1152,44100) == 28122, "Non-529 gap untouched");
        Mp3PcmTimestampNormalizer clock = new Mp3PcmTimestampNormalizer(true);
        ByteBuffer pcm = ByteBuffer.allocate(2304);
        DnaStreamingAnalyzer analyzer = new DnaStreamingAnalyzer(() -> {});
        analyzer.accept(pcm,0,1246,clock.normalize(0,623,44100),44100,1,2);
        analyzer.accept(pcm,0,2304,clock.normalize(26122,1152,44100),44100,1,2);
        reject(() -> analyzer.accept(pcm,0,2304,clock.normalize(552244,1152,44100),44100,1,2));
        rejectState(analyzer::finish);
        check(clock.getCorrectionUs() == offset, "Correction applied once, never follows later gaps");
        Mp3PcmTimestampNormalizer empty = new Mp3PcmTimestampNormalizer(true);
        check(empty.normalize(98765,0,44100) == 98765, "Empty output cannot set anchor");
        check(empty.normalize(Long.MAX_VALUE,623,44100) == Long.MAX_VALUE, "Invalid timestamp cannot poison clock arithmetic");
        empty.normalize(0,623,44100);
        check(empty.normalize(26122,1152,44100) == 26122-offset, "Empty output ignored before first PCM");
    }
    public static void timestampDiagnosticsAreNumeric() {
        try {
            new DnaStreamingAnalyzer(() -> {}).accept(ByteBuffer.allocate(400),0,400,DnaTimeline.MAX_START_US + 1,22050,1,2);
            throw new AssertionError("Missing initial audio accepted");
        } catch (DnaTimestampException error) {
            check(error.diagnostic().equals("PCM 22050 Hz; frames read 0; expected 0 us; received 250001 us"), "Safe values identify initial mismatch");
            check(DnaAnalysisFailure.classify(error) == DnaAnalysisFailure.TIMESTAMP, "Typed error preserves failure code");
        }
    }
    public static void initialTimestampOffset() {
        DnaAnalysis zero = analyze(48000, 2, 0.3, 1024, 2, sine(100,0.6));
        for (long origin : new long[]{1, 2000, 2001, 36979, DnaTimeline.MAX_START_US}) {
            DnaStreamingAnalyzer analyzer = new DnaStreamingAnalyzer(() -> {});
            feed(analyzer, 48000, 2, 14400, 1024, 2, sine(100,0.6), origin);
            DnaAnalysis shifted = analyzer.finish();
            check(zero.getAudioSha256().equals(shifted.getAudioSha256()), "Offset never changes PCM fingerprint");
            check(Arrays.equals(DnaCodec.encodeFeatures(zero.getFeatures()), DnaCodec.encodeFeatures(shifted.getFeatures())), "Offset never changes musical features");
            DnaTimeline t = shifted.getTimeline(), baseline = zero.getTimeline();
            check(t.getStartUs() == origin && t.getDurationUs() == baseline.getDurationUs() + origin, "Timeline preserves media extent");
            check(t.getFrameCount() == baseline.getFrameCount(), "No invented silent feature frames");
            for (int i = 0; i < t.getFrameCount(); i++) {
                check(t.timeUs(i) == baseline.timeUs(i) + origin, "All windows retain media origin");
                for (int j = 0; j < DnaTimeline.WIDTH; j++) check(t.valueAt(i,j) == baseline.valueAt(i,j), "Same feature grid");
            }
            check(DnaTimelineSampler.sample(t, origin-1, DnaTimeline.BASS) == 0, "No early offline animation");
            near(DnaTimelineSampler.sample(t, origin, DnaTimeline.BASS), t.valueAt(0,DnaTimeline.BASS)/1000000.0, 0.000001, "First feature at first PCM PTS");
            near(DnaTimelineSampler.sample(t, origin+50000,DnaTimeline.BASS), DnaTimelineSampler.sample(baseline,50000,DnaTimeline.BASS), 0.000001, "Offset-aware interpolation shared by live and export");
        }
        DnaStreamingAnalyzer broken = new DnaStreamingAnalyzer(() -> {});
        ByteBuffer chunk = ByteBuffer.allocate(4096);
        broken.accept(chunk,0,4096,36979,48000,2,2);
        reject(() -> broken.accept(chunk,0,4096,36979+21333+5000,48000,2,2));
        rejectState(broken::finish); // Initial-offset handling never excuses a later 5 ms gap.
    }
    public static void offsetTimelineSerialization() throws Exception {
        int[] ramp = new int[38]; ramp[0] = 200000; ramp[19] = 800000;
        for (long origin : new long[]{0,36979,250000}) {
            DnaTimeline timeline = new DnaTimeline(ramp.clone(),origin,origin+200000);
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            DnaTimelineCodec.write(timeline,bytes);
            byte[] encoded = bytes.toByteArray();
            check(ByteBuffer.wrap(encoded).getInt() == (origin == 0 ? 0x4d445431 : 0x4d445432), "Versioned offset serialization");
            if (origin == 0) {
                ByteBuffer legacy = ByteBuffer.allocate(16 + ramp.length*4).putInt(0x4d445431).putLong(200000).putInt(2);
                for (int value : ramp) legacy.putInt(value);
                check(Arrays.equals(legacy.array(), encoded), "MDT1 encoding byte-compatible");
            }
            DnaTimeline restored = DnaTimelineCodec.read(new java.io.ByteArrayInputStream(encoded));
            check(restored.getStartUs()==origin && restored.timeUs(1)==origin+100000, "Roundtrip exact grid origin");
            near(DnaTimelineSampler.sample(restored, origin+50000,0),.5,0.000001,"Roundtrip interpolation");
        }
        for (long[] invalid : new long[][]{{-1,200000,2},{250001,450001,2},{36979,36979,1},
                {36979,Long.MAX_VALUE,1},{36979,236979,216002},{36979,236979,1}}) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(bytes);
            out.writeInt(0x4d445432);out.writeLong(invalid[1]);out.writeLong(invalid[0]);out.writeInt((int)invalid[2]);
            try { DnaTimelineCodec.read(new java.io.ByteArrayInputStream(bytes.toByteArray())); throw new AssertionError("Invalid offset header accepted"); }
            catch(java.io.IOException expected) { check("Timeline bounds".equals(expected.getMessage()), "Bounds checked before allocation/read"); }
        }
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        DnaTimelineCodec.write(new DnaTimeline(ramp,36979,236979),bytes);
        byte[] encoded = bytes.toByteArray();
        for (byte[] invalid : new byte[][]{Arrays.copyOf(encoded,encoded.length-1),Arrays.copyOf(encoded,encoded.length+1)}) {
            try { DnaTimelineCodec.read(new java.io.ByteArrayInputStream(invalid)); throw new AssertionError("Truncated/trailing timeline accepted"); }
            catch(java.io.IOException expected) { /* expected */ }
        }
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
                "discontinuitiesAndFormats","limitsAndEmpty","resamplerTimeAndPriming","deterministicGenomeInput","frozenAnalyzerVersion","safeFailureReasons","mp3DecoderTimestampConventions",
                "timestampCorrectionDoesNotHideLostAudio","timestampDiagnosticsAreNumeric",
                "initialTimestampOffset","offsetTimelineSerialization"};
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
        check(x.getFrameCount()==y.getFrameCount() && x.getStartUs()==y.getStartUs() && x.getDurationUs()==y.getDurationUs(), "Identical timeline extent");
        for(int i=0;i<x.getFrameCount();i++) for(int j=0;j<DnaTimeline.WIDTH;j++) check(x.valueAt(i,j)==y.valueAt(i,j),"Identical timeline values");
    }
    private static void reject(Runnable r) { try { r.run(); } catch(IllegalArgumentException expected) { return; } throw new AssertionError("Invalid input accepted"); }
    private static void rejectState(Runnable r) { try { r.run(); } catch(IllegalStateException expected) { return; } throw new AssertionError("Invalid state accepted"); }
    private static void near(double value,double expected,double tolerance,String reason) { check(Math.abs(value-expected)<=tolerance,reason+": "+value); }
    private static void check(boolean condition,String reason) { if(!condition) throw new AssertionError(reason); }
}
