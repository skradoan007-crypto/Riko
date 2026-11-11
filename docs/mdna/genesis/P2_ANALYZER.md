# Genesis P2 — offline music feature analyzer

## Source audit and implementation checklist

The verified `new` branch baseline is `32563844c84d4fd2fae6e317e1594e6fd7b413de`.
Its source matches the delivered P1 code, plus the user's uploaded delivery record
and P1 patch. The user's P1 CI run succeeded:
<https://github.com/skradoan007-crypto/Riko/actions/runs/42964805486>.
This is baseline evidence, not a P2 build result.

Before editing, inspected `AudioFileAnalyzer`, `WaveformBuckets`, `WaveformRepository`,
`PcmFrames`, the live `SpectrumEngine`/Float `Fft`, the P1 genome boundary, existing
waveform JVM/device tests and generated WAV/AAC fixtures, and the actual release CI.
The decoder was embedded in AudioFileAnalyzer; there was no reusable offline source.

- [x] Move the existing extractor/codec loop into one shared `AndroidPcmSource`.
- [x] Preserve the waveform API and energy reducer; inject a shared source into both consumers.
- [x] Stream normalized/channel-averaged PCM through anti-aliased 22,050 Hz conversion.
- [x] Extract spectral bands, centroid, flux, chroma, dynamic range, tempo/confidence.
- [x] Emit a timestamped 10 Hz feature timeline and versioned audio fingerprint.
- [x] Bound PCM, tempo and timeline storage; propagate cancellation; reject incomplete/error input.
- [x] Exercise deterministic synthetic signals, malformed input, resampling and P1 compatibility.
- [x] Add actual Android fixture/decoder tests and CI compile/regression gates.
- [ ] Run Android Gradle, actual codecs and device performance checks (environment blocked).

## Shared source and waveform compatibility

`PcmSource` is a reusable suspend interface with synchronous `PcmConsumer` callbacks.
The source calls `onStart(durationHintUs)` and delivers borrowed, interleaved,
little-endian buffers with explicit offset, size, rate, channels, encoding and media
PTS. Callbacks must consume before returning; they may not retain/mutate buffers.
The selected track's first extractor timestamp is the origin; no wall-clock time
is used for musical timestamps. System.nanoTime is used only for stall detection.

`AndroidPcmSource` contains the **moved**, not duplicated, MediaExtractor/MediaCodec
loop, raw-PCM path, background priority, 5-second no-progress timeout and cleanup.
Hilt binds the interface to the singleton source: waveform and DNA share one mutex
and at most one active offline decoder. The live Media3 PCM tap remains untouched.
The source rejects network URI schemes; only local file/content-provider sources
are accepted. A content provider's own backing implementation remains outside the
app's control (for example, a user-selected document provider).

`AudioFileAnalyzer.analyze(...)` remains available, including no-argument construction
for existing device tests. It forwards original PCM directly to the unchanged
`WaveformBuckets`, then finishes on Dispatchers.IO. Its cache/repository/data format
are unchanged. Waveform computes mean channel **energy**, preserving inverted stereo;
DNA intentionally downmixes channel **amplitude**, so antiphase channels can cancel.

Known-duration EOS is rejected when decoded audio ends over max(250 ms, 1% duration)
early. This prevents obvious truncated results; badly overstated metadata can also
make an otherwise decodable file unavailable. With no reliable duration metadata,
successful decoder EOS cannot prove the original file was never truncated. PCM
alignment/encoding validation and decoder/provider exceptions still apply. Codec
buffers, decoder, extractor, trace and thread priority are released/restored in
finally blocks, including cancellation. Unavailable results contain exception class
categories only, not paths or provider messages.

## Frozen analyzer V1 signal contract

`DnaStreamingAnalyzer` is an Android-free, one-shot streaming engine. `OfflineDnaAnalyzer`
is the suspend Android adapter; it calls `finish()` only after source EOS and never
publishes an interrupted partial result. Empty input is unavailable. Cancellation
is checked before chunks, every 1,024 source frames, every FFT window and during
tail processing. A failed accept poisons the instance; discard and restart after
cancellation/failure. Resume scheduling is P6, not implicit partial-state reuse.

Input support: 8,000–192,000 Hz, 1–32 channels, signed PCM16 or float PCM. Normalize
PCM16 by 32,768; clamp finite float samples to [-1,1], replace NaN/infinity with zero,
and average all channels. This is a documented equal-weight downmix, not an ITU
surround-layout/LFE-specific mix. ByteBuffer position/order are not changed.
Unknown encodings, partial channel frames, out-of-bounds data, midstream rate/channel
changes and decoder PTS deviations over 2 ms from the accumulated frame clock fail
closed. Negative codec priming is trimmed; an initial positive gap over 2 ms is
rejected. Sub-2-ms timestamp rounding/jitter is tolerated, not accumulated as drift.

Resampling uses a Blackman-windowed sinc low-pass with 256 fractional phases,
radius ceil(24 / min(1, 22050 / sourceRate)), cutoff 0.45 * min(1, rate ratio),
per-phase unity-DC normalization and a fixed PCM ring. Rational sample positions
avoid cumulative floating-point clock drift. The symmetric filter waits for its
lookahead, then labels output at the compensated input position. Start/end padding
is zero; output length is ceil(inputFrames * 22050 / sourceRate). It does not prepend
filter latency to the media timeline. Filtering also runs for 22,050 Hz input so
V1 has one defined preprocessing path. Output overshoot is clipped to [-1,1].

The offline FFT uses double precision and StrictMath-generated coefficients; the
existing live Float FFT is left unchanged to avoid changing playback visual timing.
FFT size 2,048, Hann window, hop 441 samples = 50 windows/second. Windows are centered
at samples 0, 441, 882, ... and zero-extended at edges. The 10 Hz timeline keeps every
fifth center (0, 100,000 us, ...); the last center precedes the observed duration.
There is no claim of one-display-frame transient precision: a window spans about
93 ms and the exported grid is 100 ms. The live path supplies finer immediate response.

| Feature | Analyzer V1 definition |
| --- | --- |
| Band energies | Square root of integrated Hann-energy-normalized one-sided spectral power; bass 20–250 Hz, mid 250–4,000 Hz, treble 4,000–11,025 Hz; bounded to [0,1] |
| Centroid | Power-weighted Hz above 20 Hz; zero for negligible power; song mean over audible windows (RMS >= 1e-5) |
| Flux | Sum of positive magnitude differences divided by max(current magnitude sum, previous sum, 1e-12); first/silent windows zero |
| Onset | Positive flux minus its causal exponential average (alpha 0.1); bounded [0,1] |
| Chroma | Spectral power from 50–5,000 Hz assigned to nearest equal-tempered pitch class, A4=440 Hz; C..B, L1 normalized |
| RMS | Time-domain RMS of valid samples within each centered window |
| Dynamic range | P95 minus P10 of window RMS dBFS, histogram resolution 0.1 dB, floor -120 dBFS |
| Song bands/flux | Arithmetic mean across all 50 Hz analysis windows |
| Song chroma | Normalize summed pitch-class power across windows |

Summary values pass through the unchanged P1 quantization/validation boundary.
Timeline units are ppm except centroid in milli-Hz. Chroma bins sum to exactly one
million or zero; rounding remainder goes to the strongest bin. This differs from
P1 summary's largest-remainder quantization and is frozen as part of analyzer V1.

## Tempo and ambiguity

Onset autocorrelation uses running pair counts, sums, squared sums and cross-products
for lags up to 76 at 50 Hz. Storage is independent of song duration. Correlation is
mean-corrected and normalized. At least six seconds and four detected attacks are
required; insufficient/weak evidence yields BPM=0, confidence=0. Search is approximately
40–240 BPM with parabolic interpolation around supported peaks.

Autocorrelation can prefer exact multi-beat periods when one beat falls between
sample lags. V1 checks supported shorter local peaks at divisors 2–4 instead of
blindly doubling every slow estimate. This fixed a synthetic 180 BPM -> 60 BPM
failure during implementation. Supported half/double-time alternatives are exposed
as `TempoEstimate.alternativeBpm`; ambiguity reduces confidence by 25%. Confidence
also depends on correlation strength and attack count. It is not a probability.
Global tempo cannot fully represent rubato, tempo changes or every genre; the onset
timeline retains localized events. No perfect beat-grid claim is made.

## Memory, fingerprint and future integration

PCM rings, FFT arrays, histogram and tempo state are fixed size. Only derived
timeline values scale with duration. Default limit is two hours (72,000 rows * 19
ints = 5,472,000 bytes for final values, excluding object/working overhead); an
explicit engine constructor permits 1 second to at most six hours. Builder growth
and final immutable copy may temporarily retain two arrays. Over-limit input fails
without publishing partial DNA. No full decoded track is loaded into memory.

Audio fingerprint SHA-256 covers ASCII `RIKO:MDNA:PCM:ANALYZER1:22050:S16LE` plus
NUL, then all resampled mono samples quantized with floor(value*32768+0.5), saturated
to signed PCM16, in little-endian order. Hashing uses a reusable 4 KiB buffer. No
paths, URI, device ID, title, tags or timestamps enter the fingerprint. Identical
decoded sample streams yield identical content fingerprints/features regardless of
buffer splitting. Different codecs, decoder rounding, encoder delay or transcoding
can yield different fingerprints: this is content identity, not acoustic matching.

`DnaAnalysis` contains analyzerVersion=1, immutable DnaFeatures, DnaTimeline,
TempoEstimate and audioSha256. Pass fingerprint/features to P1's mapper to generate
the base genome. Persist the analyzer version alongside features in the later
persistence design; future analyzer changes must not silently overwrite identity.
P2 does not add Room entities, library scheduling, hybrid-controller wiring, new
screens or visual archetypes. Those belong to later phases. Timeline persistence
and portable sharing serialization are not implemented in this phase.

## References

- Android's PCM format negotiation, borrowed buffer bounds and EOS behavior:
  <https://developer.android.com/reference/android/media/MediaCodec>
- Media timestamps are BufferInfo.presentationTimeUs, not system clock:
  <https://developer.android.com/reference/android/media/MediaCodec.BufferInfo>
- Julius O. Smith, *Physical Audio Signal Processing*, windowed sinc interpolation:
  <https://dsprelated.com/freebooks/pasp/Windowed_Sinc_Interpolation.html>

These references ground platform/DSP behavior. V1's concrete filter/window/feature
parameters and tempo heuristics are RIKO implementation choices validated by the
checked-in tests, not claims of equivalence to a third-party analyzer.
