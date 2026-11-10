# RIKO DNA (MDNA) — Phase 0: "DNA Bloom" wow test

**Goal:** decide, on a real phone, whether the GLES point-cloud engine is worth building on. Nothing here is wired into the shipping UI (zero existing files edited).

## What P0 contains
| Piece | File |
|---|---|
| Lock-free frame handoff (audio side → render thread) | `dna/core/SpectrumTripleBuffer.kt` |
| Time-constant band follower + beat punch (frame-rate independent) | `dna/core/BandFollower.kt` |
| Demo 120BPM signal (works with no music) | `dna/core/SyntheticSpectrum.kt` |
| GLSL ES 3.00 shaders (validated with glslangValidator) | `dna/render/MdnaShaders.kt` |
| GLES 3.0 renderer: 4 dotted shells + bloom, vsync-locked | `dna/render/MdnaGlRenderer.kt` |
| Touch rotate / pinch zoom view | `dna/render/MdnaGlSurfaceView.kt` |
| Quality tiers (point budgets are STARTING GUESSES) | `dna/render/MdnaQuality.kt` |
| Debug-only lab screen + launcher icon "MDNA Lab" | `src/debug/.../dna/lab/MdnaLabActivity.kt`, `src/debug/AndroidManifest.xml` |
| JVM tests (triple buffer incl. 2-thread no-tearing test, follower, demo signal) | `src/test/.../dna/core/*Test.kt` |

## How to test
1. Install the debug APK. Start a song in RIKO.
2. Open the **MDNA Lab** icon. Source = LIVE (or tap it → DEMO).
3. Drag = rotate, pinch = zoom. Try Quality LOW / MID / HIGH and Bloom on/off.
4. Tune Attack / Release sliders until hits feel punchy but silky.

## What to report back (this decides P1)
- Phone model + Android version.
- The top-line stats for each quality tier (fps / ms / pts) with bloom on and off.
- Does anything look wrong or black? The overlay prints `ERROR: ...` if a shader or framebuffer fails.
- Subjective: do kicks feel *on* the beat, early, or late? (LIVE currently inherits the processor's fixed `AV_SYNC_DELAY_MS`.)
- Attack/Release values that felt best.

## Known P0 limits (by design — P1 fixes these)
- LIVE frames travel DSP → `StateFlow` → main thread → buffer. P1 publishes straight from the audio thread.
- A/V sync uses the processor's fixed 120 ms delay; P1 replaces it with measured output latency + a manual offset slider.
- Not yet a Visualizer style; the extra launcher icon disappears when P1 integrates it.
- Verified off-device: Kotlin compiles (2.0.21), shaders validate (ESSL 3.00), 14 JVM tests pass. **Not** verified: actual GPU behaviour, frame rate, look. That is what this phase is for.
