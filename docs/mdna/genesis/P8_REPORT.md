# P8 — production integration (source complete; Android acceptance pending)

**Audio-response correction:** See [AUDIO_RESPONSE_CORRECTION.md](AUDIO_RESPONSE_CORRECTION.md).
User device feedback rejected the musical result. New DSP/shader changes need a fresh
Android build and visual review; the earlier green CI does not cover these changes.

**CI update (2026-11-12):** See [P8_CI_RESULT.md](P8_CI_RESULT.md). The supplied CI logs
confirm Android unit tests, device-test compilation and APK assembly/upload passed;
only release-tag publication failed. The pre-build notes below record the original handoff.

**Analysis failure follow-up:** See [ANALYSIS_FAILURE_DIAGNOSTICS.md](ANALYSIS_FAILURE_DIAGNOSTICS.md).
Specific failure codes, retry, single-attempt loading and timeline cache repair are added;
the reported song's exact decoder failure remains unconfirmed.

**Timestamp follow-up (2026-11-13):** See [MP3_TIMESTAMP_CORRECTION.md](MP3_TIMESTAMP_CORRECTION.md).
The supplied device now reports TIMESTAMP. A known legacy MP3 decoder offset was reproduced
and corrected in the shared PCM source. Device verification and the observed FPS drop remain open.

## Integration and architecture
- Direct Now Playing overflow > Music DNA, Song Info > Open Music DNA, and the existing
  Music Visualizer Lab > Open MDNA Genesis all open DnaLabScreen. Shared playback stays
  owned by PlayerManager/MediaSession. Back closes the overlay. No application identity change.
- Kept the existing PlayerSheetHost overlay pattern used by Visualizer. Visual OS's
  scene mapper only maps navigation destinations and four existing sheet types; MDNA
  does not introduce a competing navigation coordinator or alter those contracts.
- Controls are in a scrollable dialog so the organism keeps its display area. Drag,
  pinch and pulse have labelled button alternatives. System reduced-motion disables
  initial auto motion/inertial fling; users can freeze motion. Frame interval diagnostics
  are sampled at 2 Hz, separate from the frame-driven GL rendering path.
- Hybrid input blends timestamped live spectrum with the stored 10 Hz timeline. An
  immutable Media3 position/speed/song anchor is refreshed on player events and each
  100 ms while attached. Render presentation timestamps extrapolate at most 250 ms;
  manual AV offset is applied in the media domain. Live source keeps its output latency
  estimate; timeline position does not subtract that estimate a second time. Invalid
  track matches cannot use the previous song's timeline. Live-to-hybrid blend is eased.
  Status distinguishes LIVE, HYBRID, offline timeline, paused and waiting states.
- DnaTimelineSampler is allocation-free and shared by interactive hybrid playback and
  fixed-step export. The audio-thread ring path has no new database/export work.
- Listening observation now belongs to MusicPlaybackService, not PlayerViewModel.
  Background playback contributes meaningful time. New profiles backfill retained
  meaningful history inside the same Room transaction used for insertion. This resolves
  the two P6 limitations recorded in P6_REPORT. Existing profiles retain generation.
- Repository access serializes analysis and atomic timeline writes. Background analysis
  checks thermal/power/memory conditions during long songs as well as between songs.
  Export cache files older than a day are reclaimed on the next export.

## Executed checks
- tools/test_genesis_dna.sh: 17 production-Java contract groups passed; independent
  Python oracle matched all 3 frozen Genesis V1 vectors.
- tools/test_genesis_analyzer.sh: 13 production-Java analyzer contract groups passed,
  including streaming, cancellation, resampling, synthetic tempo/chroma and timeline.
- tools/test_genesis_final.sh: camera bounds/smoothing/cancel/pulse/invalid input/reset;
  DNA code roundtrip/version rejection, symmetric breeding, comparison, binary timeline
  validation, interpolation/clamping, media clock speed/offset/pause passed.
- Manifest XML and workflow YAML parsed; git diff --check passed.
- Source credential-pattern scan performed before commit; the existing CI secret check
  is retained. Final JVM checks added to that workflow without running it.
- Earlier P6 actual-ESSL Mesa probe passed all three archetypes/two variations and 18
  viewport/quality cases. The shaders are unchanged since that check. The included
  P4 evidence files are labelled synthetic Mesa renders, not Android captures.

## Tests written, not executed
- DnaEvolutionDeviceTest: real Room transaction threshold crossing exactly once,
  meaningful history backfill, monotonic durations, stable genome and generation cap.
- DnaExportDeviceTest: actual EGL PNG decode/dimensions and missing-video-timeline rejection.
- Existing DnaMigrationTest, decoder/renderer device tests and Android Gradle unit suite
  remain in the project. Their compilation/execution was deferred with the app build.

## Build and performance status
No Gradle/APK build, CI trigger, emulator run or physical-device run was performed for
P5–P8, as requested. P4 green is user-reported. Kotlin/Compose/Hilt/Room/MediaCodec changes
are source-reviewed but not compiler-verified here. P8's release/device acceptance gate
is NOT passed. There are no new measured Android FPS, synchronization, memory, audio
underrun, battery or thermal results. A host JVM test is not an Android integration test.

## Remaining practical limits
- Real-device API 26–32/33+, Adreno/Mali, Bluetooth calibration, seek/speed/resume,
  process recovery, large-text/landscape layouts and accessibility need final validation.
- Timeline/live blending uses estimated presentation timing; one-display-frame perceptual
  sync is a target, not a guarantee. Route detection identifies connected output kinds,
  not a measured hardware AudioTrack timestamp. FPS text estimates submitted frame cadence,
  not GPU completion or displayed-frame timing.
- Listening uses conservative one-second samples, five-second persistence checkpoints;
  up to five seconds can be lost at track changes/process death. Existing legacy zero-
  duration events do not count. New-profile backfill uses retained 90-day history; already
  existing profiles are not retroactively recomputed. Generation is capped at 64.
- Background batches resume at song boundaries, not PCM offsets. Cursor is an index in a
  sorted library snapshot; library additions/deletions may defer a song until the next pass.
- PNG/MP4 export uses the portable base genome; interactive camera/evolution presentation
  is not embedded in the DNA code. Imported/bred previews never overwrite the song profile.
  MP4 preview uses the selected song's timeline. Pixel/encoded-byte identity across GPUs
  and encoders is not promised; fixed seed/timeline/frame inputs are reproducible.
- Actual codec support, cancellation cleanup, offscreen GPU memory and 10-second muxed
  output require device tests. Video defaults to silent. Unsupported GLES 3 devices show
  an explicit unavailable state. No static reference-image fallback is used.
- No macrobenchmark module is present in this snapshot (settings includes it only if
  supplied). Macrobenchmark, leak/battery/thermal observation and release verification
  remain final-device work; no fabricated benchmark success is reported.

Files are listed in P8_FILES.txt. Milestone hash is recorded in the delivered ZIP's
DELIVERY.md after commit. Ready for the user's single final Android build and device gate.
