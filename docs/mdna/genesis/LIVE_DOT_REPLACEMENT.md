# MDNA independent live dots — P8 corrective replacement

## User outcome and boundary
The user rejected the autonomous-looking line sphere and explicitly requested an
independent dot spectrum resembling the supplied video. The primary MDNA screen
now renders a flat, deformable radial field of separate luminous points. No mesh,
3D camera, autorotation, idle noise clock, demo fallback or reference texture feeds
this path. Opening MDNA does not start song analysis or read a profile database.

DNA tools remain reachable by an explicit button. An explicit Analyze / retry
operation loads/creates a profile. Closing tools cancels foreground work. Existing
background scheduling, profile serialization, migrations, playlists, listening
history and application identity are retained. Previously scheduled background
analysis is not silently cancelled. Legacy 3D components remain for older debug
screens and compatibility; the canonical song MDNA surface and exporter use dots.

## Architecture
- DnaLabScreen -> DnaOrganism -> LiveDotHost -> LiveDotSurfaceView -> LiveDotRenderer.
- Existing Media3 PcmAudioProcessor -> canonical FFT -> absolute 40 logarithmic
  band levels -> timestamped SpectrumHistoryRing -> compensated liveOnlySource.
- LiveDotInput reuses BandFollower with fast attack/release (18/115 ms) and a slower
  response (90/240 ms). Each dot samples its angular frequency sector and its row's
  mixture of those two responses. Bass power controls expansion; individual bands
  control local displacement/light; their fast/slow difference adds local transients.
- The shader has no time uniform. Static sine functions shape audio displacement;
  a steady spectrum settles to a steady shape. Missing input and pause decay to rest,
  including when motion was manually frozen. LIVE requires actual source frames.
- ESSL 1.00 / GLES 2.0, one static 61,440-byte ID VBO, one GL_POINTS draw, zero
  per-frame geometry allocation/uploads. No framebuffer, blur or bloom passes.
  3,840 / 9,216 / 15,360 points across 48 rows. Adaptive quality only reduces density;
  it does not change audio passthrough. Paused/frozen requests are capped at 15 Hz.
- Choreographer requests GL-thread work. Compose only polls diagnostics at 2 Hz.
  Lifecycle attachment is balanced; context destruction releases GPU resources and
  context recreation rebuilds them. Shader failure stops drawing and reports details.
- Lemon and cyan/violet colours, sensitivity, motion freeze, quality, signal levels
  and output-specific manual AV offset are in Controls. Main screen prioritizes dots.
  Native controls, 48 dp targets, labelled sensitivity/quality and reduced-motion
  initial freeze are included. Device layout/accessibility checks remain pending.
- Optional imported/analyzed genome seed changes static point arrangement. The new
  live surface does not display the former three archetypes or generation layers;
  those stored traits/history are preserved but not presented as completed new visuals.
- PNG and silent 10-second MP4 use the same dot renderer and GLES 2 offscreen context.
  MP4 remains fixed-step from stored three-band timeline, not live 40-band capture;
  the export dialog says so. PNG shows the static identity. Codec results still need
  real-device validation; deterministic inputs do not guarantee bit-identical encoding.

## Why the new timestamp screenshot no longer blocks visualization
Reported later-buffer sample: 48,000 Hz, 910 frames, expected 55,937 us, received
60,979 us. This is a 5,042 us difference after decoding started, distinct from the
previous initial-origin case. This change neither broadens timestamp tolerance nor
pretends this file was repaired. The analyzer can still report TIMESTAMP when
explicitly requested in DNA tools. Its error is retained there with numeric details.
The live spectrum never consults that decoder/timeline, so this failure cannot gate
or replace live data. A problematic audio file/device decoder trace is still needed
to validate an analyzer fix rather than guessing another tolerance.

## Executed validation
- `bash tools/test_genesis_analyzer.sh`: 19 production-Java contract groups passed.
- `bash tools/test_genesis_final.sh`: camera, DNA code/version/breeding, timeline,
  presentation-clock math and absolute spectrum DFT oracle passed.
- `MESA_GLES_VERSION_OVERRIDE=2.0 python tools/live_dot_probe.py`: actual production
  ESSL compiled/linked under Mesa GLES 2.0; silence and constant input were stable;
  a single mid-band changed 3,308 pixels locally; bass response and amplitude ordering
  passed; all three point densities stayed within portrait/landscape/square bounds.
  The Python reference follower also passed pause decay. This is not execution of
  the Kotlin follower or Android audio pipeline.
- Same shader rendered with audio decoded from the user's supplied reference video.
  Included preview is a host shader render, not a screenshot or recording of the app.
  The test driver reproduces Hann/FFT/log-band/absolute-RMS math at 44.1 kHz. Its
  60 Hz sampling does not reproduce Media3 callback cadence or measured output latency.
- `git diff --check`, source credential-pattern check and patch application checks.

## Tests added but not executed here
- LiveDotInputTest: pause while frozen without upstream reads; missing PCM clears
  stale song response; local-band isolation; freeze/resume/reset; 60/120 Hz equivalence.
- GenesisRendererDeviceTest live-dot case: GLES 2 drawing at all tiers, frozen pause
  settling, lost-context recreation and explicit release. Existing exporter tests
  now exercise the new exporter when run. Existing CI filters include these tests.

## Honest limits
No Android build/CI/device test ran for this replacement. No physical FPS, GPU time,
AV-sync, underrun, battery, thermal or memory results are claimed. Host timing is not
phone performance. GPU/vendor compatibility and portrait/landscape/large text/TalkBack
must be checked in the final Android build. Reference imagery was not shipped.

The live tap is before Android hardware audio effects. The existing user-volume
factor is an estimate, not measured speaker loudness; post-hardware EQ/preamp are not
captured. Mid includes both voices and instruments; this is not vocal isolation.
Latency remains estimated with manual calibration. These limitations are not fixed
by drawing dots, and must not be described as resolved.

Acceptance: code ready for final Android build and the user's visual review; not a
claim of finished production validation or a successful analyzer repair.
