# P8 correction: preserve the decoder's initial media timestamp

## Confirmed evidence

The new device screenshot for FUNK POWER (Ultra Slowed) reports exactly:
`PCM 48000 Hz; frames read 0; expected 0 us; received 36979 us`.
This is the first non-empty decoded buffer, not a discontinuity after audio has
already been consumed. The prior MP3 529-frame correction addresses a different
case and did not cover this initial offset.

The prior production DnaStreamingAnalyzer from commit 0fc1977 was compiled into
an isolated host-JVM harness. Feeding a buffer with these exact parameters
reproduced the same diagnostic. No user audio file was available; this reproduces
the failing timestamp path, not the full device decode.

## Change and timing contract

- DnaStreamingAnalyzer accepts a positive first PTS up to 250,000 us. This is an
  explicit application bound for short leading decoder offsets, not an Android
  timing guarantee. Larger starts remain TIMESTAMP failures. Negative priming
  trimming, duration limits, cancellation and the 2 ms midstream gap guard remain.
- The original first PTS is retained as DnaTimeline.startUs. No audio is inserted,
  discarded or moved to zero to make this case pass. Before startUs, offline
  sampling returns zero because there are no analyzed features there.
- Grid center i is startUs + i * 100,000 us. durationUs remains the media end
  position, now explicitly including the leading offset. All frame values and
  the audio fingerprint come from the same decoded PCM as before.
- DnaTimelineSampler subtracts the stored origin for interpolation. Both the live
  hybrid controller and fixed-step MP4 exporter already use this shared sampler,
  so both honor the same timing without separate correction constants.
- DnaTimelineCodec introduces MDT2 only for nonzero origins. Zero-origin output
  remains byte-identical MDT1, and the reader supports both. Bounds, grid extent,
  allocation limits and trailing/truncated data are validated. An older app
  version cannot read MDT2; current versions read existing MDT1 caches normally.
- No Room schema change or base-genome algorithm change is needed. Existing
  version-1 feature/fingerprint/timeline-value goldens still pass. Previously
  failing songs had no completed profile to migrate.

Android defines presentationTimeUs as the output buffer's presentation timestamp;
that API does not require a first decoded buffer's timestamp to be zero:
https://developer.android.com/reference/android/media/MediaCodec.BufferInfo#presentationTimeUs

## Files

Production: DnaStreamingAnalyzer.java, DnaTimeline.java, DnaTimelineSampler.java,
DnaTimelineCodec.java. Tests: DnaAnalyzerContracts.java, DnaAnalyzerTest.java,
DnaRepositoryTest.kt. Phase documentation: this report, P8_REPORT.md, P8_FILES.txt.

## Validation actually performed

- Old production analyzer: exact screenshot timestamp failure reproduced on JVM.
- tools/test_genesis_analyzer.sh: 19 contract groups PASS (-Xlint/-Werror).
  New regression covers 48 kHz stereo starting at 36,979 us, tiny offsets and the
  250 ms boundary, unchanged fingerprint/features/grid values, preserved origin,
  no pre-origin animation and correct interpolated sampling.
- MDT1 byte compatibility and MDT2 round-trip/interpolation PASS; malformed start,
  duration/count overflow, inconsistent grid extent, truncation and trailing data
  reject. Later 5 ms gaps still reject; existing MP3-convention tests still pass.
- tools/test_genesis_final.sh: camera, DNA code/timeline and spectrum checks PASS.
- Added Room/Robolectric regression: first decode and cached reload preserve the
  same offset and genome without decoding again. This Android-dependent test is
  written but NOT run locally. No Android build, CI or device run for this revision.
- Diff whitespace, changed-file credential scan, delivery patch reconstruction
  and ZIP integrity are checked before delivery.

## Remaining limitations

This fixes the specific first-buffer rejection shown in the screenshot. It does
not establish why that device's decoder starts there, guarantee the remainder of
that song decodes, or prove audiovisual synchronization on hardware. The leading
interval contains no decoded feature evidence and is not treated as analyzed
silence. Device playback and export verification remain required.
The earlier 45 FPS observation and the requested visual-quality acceptance are
still open. No new renderer, frame-time, battery, thermal or audio-glitch claims.
