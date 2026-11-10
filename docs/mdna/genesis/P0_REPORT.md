# Genesis P0 — live organism proof

Status: **implemented; Android compilation and physical-device acceptance pending**.
Do not start P1 until Radoan explicitly approves the P0 look after device review.

## What is delivered

One Genesis Orb uses two GPU-generated continuous spherical contour spirals and sparse
pin lights, cyan/violet/magenta shading, smooth bass expansion, mid folds, treble detail,
and a transient envelope. This is a live-rendered proof, not the P1 song genome.
It reuses the existing GLES engine, quality governor, smoothing and spectrum pipeline.
No reference image is included in the APK. Existing four scenes remain available.

A separate **Genesis Lab** launcher exists in the **debug APK only**. LIVE reads the
existing playback PCM. DEMO is clearly synthetic. The visual can be paused without
pausing audio; pausing playback also stops motion while the energy fades. Controls include
quality, automatic quality reduction, bloom, optional API-33 AGSL, and manual AV offset.
The ordinary RIKO launcher/package, MediaSession, database and stored user data are unchanged.

Full changed-file inventory: [P0_FILES.txt](P0_FILES.txt).

## Architecture changes

- Preserved upstream raw PCM tap wiring missing from the ZIP. A throwing frame consumer
  is now detached; byte-exact audio passthrough remains the established path.
- Ring slots use atomic payload words and generation stamps, rejecting torn reads during
  overwrite; reset floor is monotonic. Retained time interpolation and bounded retry.
- MdnaSyncHub observes playback transitions, position discontinuities, speed and audio-device
  connect/disconnect; detaches listeners/ticker when no visual is active. Position and speed
  are diagnostics only in P0; the offline feature timeline/hybrid mapping belongs to P2+.
- Frame requests come from Choreographer, drawing stays on GLSurfaceView's GL thread.
  No per-frame Compose state/recomposition, Pair objects, geometry buffers or shader compilation.
- Renderer releases GL resources on pause; context recreation forgets stale GL names. Optional
  bloom failures keep the direct geometry renderer. History textures start cleared.
- Correct frequency crossovers use sample rate instead of hard-coded 6/20 band boundaries.
- AGSL is a separate API-33 Canvas atmosphere. GLES3 remains the real 3D renderer on API26+.
  Devices without GLES3 get an explicit unsupported message in the Genesis Lab.
- Existing release CI retains debug-key signing and credential gate, adds JVM tests,
  a debug Lab APK and reports, and retains the direct prerelease asset mechanism.
- Missing :benchmark sources are conditional, preserving support if the module is restored.

## Validation actually performed

| Check | Result |
|---|---|
| ZIP CRC / path traversal check | Passed, 809 ZIP entries |
| Upstream comparison | Verified `new` at `a79695d412e6ff4f5a86378865a646777bfadd0b`; upstream-only fixes preserved |
| GLES shader compilation/link | 8 pairs passed on Mesa llvmpipe, OpenGL ES 3.2 |
| Independent bass/mid/treble pixel response | Passed, four distinct rendered pixel hashes |
| Repeated fixed-input frame | Byte-identical in this renderer/process |
| Quality × aspect ratio extremes | 9 render cases passed, nonempty and no clipping at full energy |
| Visual inspection | Actual checked-in ESSL screenshot and 4-second silent inspection clip |
| YAML and manifest XML | Parsed successfully |
| `git diff --check` | Passed |
| Credential patterns / sensitive transcript | No credential pattern found in changed sources; transcript excluded |
| Frozen library UI | No changes |
| Gradle unit/build attempt | Blocked before tasks: Gradle download unavailable; Android SDK also absent |
| GitHub write / CI start | Blocked: integration returned HTTP 403 for branch/tree creation; no remote mutation |
| JVM tests | 10 new tests + strengthened existing ring test; **not executed** |
| Android device tests | GLES tiers/context loss/pause and API-33 AGSL compile tests added; **not executed** |
| APK / release verification | **No P0 APK built**; previous upstream CI success is not P0 evidence |

`evidence/shader-probe.json` records renderer, pixel hashes and bounds. Reproduce:

```sh
python tools/genesis_shader_probe.py --video
```

The Python harness compiles the actual Kotlin-embedded shader strings and supplies explicit
uniforms to EGL. It does not emulate Compose or the Android Activity. The MP4 is a development
inspection clip, not the P7 export feature. Its 30 fps encoding is NOT a measured device frame rate.
Screenshot/clip omit Android's optional bloom and AGSL layer.

## Tests to run with Android tooling

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rikodev.music.android.dna.render.GenesisRendererDeviceTest
```

Install the **debug** APK, start a song in RIKO, open **Genesis Lab**. Try LIVE and DEMO;
play/pause, seek, next track, 0.75x/1.5x, speaker/Bluetooth connection changes, background/resume,
rotation and repeated opening/closing. Try Low/Mid/High, then auto quality. The runtime diagnostic
FPS is derived from GL frame request intervals, and CPU submit time excludes GPU completion.
Use Perfetto/device instrumentation before making frame-pacing, thermal, battery or latency claims.

## Known limitations / acceptance gate

- There is no P0 Android runtime evidence yet. AGSL, Hilt, full app compilation and physical
  GPU behavior remain unverified. Review is pending, not "production ready".
- Sync uses decoder-processing timestamps and a fixed base + manual correction, NOT AudioTrack
  presentation timestamps. Bluetooth detection reports connected output candidates, not a proven
  active route. It cannot guarantee one-frame perceptual alignment or compensate arbitrary buffering.
- The PCM tap is upstream of platform EQ/preamp, so it does not measure the final post-effect output.
  P0 must not claim exact EQ-reactive loudness. Hybrid offline features are not implemented yet.
- Energy is based on the existing 1024-sample FFT; the beat envelope is transient-aware, not a
  measured BPM/beat-grid detector. Musical identity, persistent evolution, interaction polish and
  sharing are deferred to their specified Genesis phases.
- No physical-device FPS, battery, thermal or leak measurements were available.
- Debug APK includes the lab; the debug-key-signed release APK retains the production entry points.

Next phase: **NOT APPROVED**. P0 visual/device feedback comes first.
