# Genesis P2 — offline music features milestone

## Implementation

Refactored AudioFileAnalyzer's real decode loop into a shared PcmSource, retained
waveform reduction, and added incremental mono normalization, anti-aliased 22.05 kHz
resampling, spectral/band/chroma/dynamics analysis, onset-autocorrelation tempo with
confidence and octave ambiguity, a centered 10 Hz timeline, and an analyzer-versioned
content fingerprint that feeds the existing P1 mapper. Added strict cancellation,
format/timestamp validation, one-decoder concurrency and bounded memory/duration.

Full contract/audit: `P2_ANALYZER.md`. Exact file inventory: `P2_FILES.txt`.
The final commit message is `mdna(genesis-p2): offline music features`; its exact
hash is in Git and the generated delivery record. No P3 changes are included.

## Validation performed

Host: OpenJDK 21.0.13, source/target 17, Linux x86_64. No Android SDK or downloaded
Gradle distribution available. No new third-party dependency was required.

| Check | Result |
| --- | --- |
| Actual production Java analyzer/core compile, warnings as errors | PASS |
| P2 synthetic JVM contract groups, -Xmx64m | 13 PASS; `evidence/genesis-p2-jvm.txt` |
| P1 regression groups | 17 PASS; independent Python oracle still matches all three P1 vectors |
| Tone centroid/band placement and A440 chroma | PASS |
| Passband preservation, 48k downsample alias rejection, 8k upsample and 96k downsample | PASS |
| 48k 15 kHz rejection relative to 1 kHz passband | Exceeds the test's 60 dB threshold; synthetic host test, not phone measurement |
| Identical output across source chunk boundaries | PASS; full feature bytes, fingerprint, all timeline values |
| Mono/stereo, antiphase, 4-channel/clipping, NaN/infinite PCM sanitation | PASS |
| 60/90/120/180 BPM pulse fixtures and 120/60 ambiguity | PASS, primary BPM within 3 BPM |
| 20 dB synthetic dynamic-range contrast | PASS, within 1 dB |
| Localized flux, silence, exact 10 Hz timestamps, resampler duration and priming trim | PASS |
| Cancellation during input/finalization; discontinuity; unsupported format; bounds and duration limits | PASS |
| Frozen V1 feature bytes, content fingerprint and full-timeline hash | PASS against committed numeric PCM fixture |
| Workflow YAML, shell syntax, Git whitespace and credential-pattern scan | PASS at milestone preparation |
| Android Gradle tests, instrumentation compilation, release assembly | BLOCKED before Gradle launch; wrapper download failed with `Network is unreachable` |

Commands executed: `bash tools/test_genesis_analyzer.sh`, `bash tools/test_genesis_dna.sh`,
plus an attempted offline Gradle invocation for DnaAnalyzerTest, Android-test compilation
and assembleRelease. No passing Kotlin/Android compilation or new APK is claimed.

13 JUnit wrappers reuse the same Java contract bodies. Existing waveform JVM suites
are added to the CI gate, and the existing waveform device tests remain. Three new
Android device tests exercise actual WAV/AAC decode, waveform compatibility, repeated
content identity, invalid-file recovery and no partial publication after errors or
cancellation. **Those Android tests have not run here.** The fixtures are the existing
generated `waveform-sections.wav/.m4a`, not copyrighted music or static visual assets.

## Known limits and remaining verification

- No real-device decoding, playback-interference, thermal/battery or memory-leak
  measurement; no screenshot is relevant to this data-only phase. The JVM heap cap
  is evidence of that synthetic suite only, not an Android memory benchmark.
- P2 touches the shared offline decoder, so real WAV/AAC/MP3/FLAC/provider and
  lifecycle/cancellation regression checks are important before release use.
- PCM16/float only, 8–192 kHz, equal-weight channel downmix, default two-hour analysis
  cap. Midstream rate/channel changes and >2 ms timestamp discontinuities are
  unavailable rather than silently stitched. Metadata that overstates duration
  may trigger the early-EOS guard.
- Tempo is a global heuristic with explicit ambiguity, not a guaranteed beat grid.
  Very short audio, silence and weak onset evidence return unknown tempo. Different
  codec/decoder sample output is not guaranteed to share one acoustic identity.
- The source is exposed as an API, not yet connected to background library scanning,
  Room persistence or the hybrid rendering controller. P3/P6 and later integration
  phases still own those tasks.
- Existing application identity, package/build versions, Room data, playback PCM,
  EQ, waveform cache format, rendering and frozen library UI are unchanged.
- No remote push was performed; the previous GitHub write-permission denial remains.
  Source ZIP and one milestone patch are provided. P1's confirmed successful CI run
  is not represented as P2 success.

P3 can proceed after the user accepts this phase, with P2 Android build/device
validation still explicitly pending. Stop at P2.
