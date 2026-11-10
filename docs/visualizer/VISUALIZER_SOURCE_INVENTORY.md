# RIKO Visualizer Source Inventory

> **Plan:** `docs/superpowers/plans/2026-10-05-riko-professional-campaign.md` §5.1 — source/template investigation BEFORE any code import.
> **Rule:** never copy external code into production until license/provenance is acceptable. Conceptual math is reimplemented independently when needed.
> **Tag legend:** `[R]` researched from repository metadata / training knowledge — verify the license file inside each source folder at import time before any code leaves this audit.

**Source root:** `extra rare files to use/`

---

## 1. `visualization templates and sources/android-visualizer-master` (felixpalmer/android-visualizer)

| Field | Record |
|---|---|
| Purpose | Classic Android audio visualizer (Java, Eclipse-era): `VisualizerView` + pluggable `Renderer` interface (BarGraph / Line / Circle / CircleBar) |
| License | MIT `[R]` — verify `LICENSE` in folder |
| Attribution | Required if code is copied (MIT copyright notice) |
| Reusable algorithm | Modular renderer-registry concept (one canvas host, swappable style renderers) — this is exactly RIKO's `SpectrumRenderer` + `SpectrumStyle` structure |
| Reusable code | None — 2012-era Java, coupled to `android.media.audiofx.Visualizer` |
| Incompatible code | `VisualizerView` capture path (OEM system-capture restrictions; RIKO uses the canonical PCM tap — `PcmFrames → SpectrumEngine → SpectrumBands`) |
| Final RIKO usage | **CONCEPT ONLY** — style vocabulary (bar/line/circle) already embodied in CLASSIC/SHAPE styles. No code imported. |

## 2. `visualization templates and sources/audio-visualizer-master` (hannesa2)

| Field | Record |
|---|---|
| Purpose | Kotlin port of #1 (same architecture, modern toolchain) |
| License | MIT `[R]` — verify `LICENSE` in folder |
| Attribution | Required if code is copied |
| Reusable algorithm | Same as #1 (duplicate lineage) |
| Reusable code | None — cleaner Kotlin, but still Visualizer-API-coupled |
| Incompatible code | Same capture path as #1 |
| Final RIKO usage | **CONCEPT ONLY.** No code imported. |

## 3. `visualization templates and sources/Audio-Visualizer-main` ("Flux", sre404)

| Field | Record |
|---|---|
| Purpose | Kotlin library + native C++17 DSP (`audio_processor.cpp`): Cooley-Tukey FFT, log binning, EMA smoothing + decay, dual input (mic `AudioRecord` / system Visualizer) |
| License | Not established in this audit `[R]` — treat as NO-COPY until the folder's license file is verified |
| Attribution | n/a until verified |
| Reusable algorithm | EMA attack/decay constants and log-binning ideas — RIKO's `SpectrumBands` already ships a canonical smoother (ATTACK 0.45 / RELEASE 0.12 + peak-hold), so reference-only |
| Reusable code | None planned |
| Incompatible code | JNI DSP (RIKO has its own tested FFT — `SpectrumEngine`), `RECORD_AUDIO` mic path (RIKO is playback-centric, plan 5.3: no microphone) |
| Final RIKO usage | **CONCEPT ONLY.** No code imported. |

## 4. `visualization templates and sources/JTransforms-3.2-sources` + `JTransforms-master`

| Field | Record |
|---|---|
| Purpose | Pure-Java FFT/DCT/DST/DHT math library |
| License | BSD-3-Clause `[R]` — verify `LICENSE` in folder |
| Attribution | Required if code is copied |
| Reusable algorithm | None needed — RIKO's `Fft`/`SpectrumEngine` is canonical and unit-tested |
| Reusable code | None |
| Incompatible code | n/a |
| Final RIKO usage | **NOT NEEDED.** Possible future reference for offline analysis (BPM/key detection). No code imported. |

## 5. `visualization templates and sources/projectm-master` (projectM 4.x)

| Field | Record |
|---|---|
| Purpose | MilkDrop reimplementation: C++/OpenGL, GLSL preset engine, waveform shape vocabulary (Line/Circle/CenteredSpiro/Star/Flower/Lasso…), noise/warp/echo effects, PCM ring → FFT |
| License | LGPL-2.1 `[R]` — verify `COPYING` in folder. **LGPL linkage risk on Android** → no code copying into a closed app |
| Attribution | Required for any copied code (and LGPL obligations) — avoid entirely |
| Reusable algorithm | (a) waveform **shape vocabulary** for the studio SHAPE/Orbital style; (b) preset = parameter bundle concept (renderer-blind numbers) for the preset system; (c) value-noise/domain-warp vocabulary for AGSL |
| Reusable code | **None** — GLSL multipass/FBO architecture does not port to AGSL `RuntimeShader` anyway |
| Incompatible code | All OpenGL multipass/warp code; external preset files |
| Final RIKO usage | **MATH CONCEPTS ONLY, reimplemented independently** (plan: "Conceptual math can be reimplemented independently when needed"). No code imported. |

## 6. `visualization templates and sources/RealTimeSpectrogramFFT-master` (lebaillyapp)

| Field | Record |
|---|---|
| Purpose | Compose real-time scrolling spectrogram (Hann → radix-2 FFT → rolling history → HSV colormap) + `RotaryKnob` control |
| License | Not established in this audit `[R]` — treat as NO-COPY until folder license verified |
| Attribution | n/a until verified |
| Reusable algorithm | **Rolling spectrogram pipeline concept** (history buffer + colormap) and rotary-control interaction concept — both reimplemented independently |
| Reusable code | None — RIKO's spectrogram reimplementation uses the canonical `SpectrumBands` history (playback spectrum, never mic) |
| Incompatible code | `AudioRecord` mic capture (plan 5.3: RIKO stays playback-centric, no `RECORD_AUDIO`) |
| Final RIKO usage | **CONCEPT ONLY.** `SpectrogramHistory` + `RikoRotaryKnob` are independent implementations. No code imported. |

## 7. `equalizer_ref` (decompiled "Music Volume EQ" APK)

| Field | Record |
|---|---|
| Purpose | Commercial EQ app reference (obfuscated classes + `res/` layouts: save-preset dialog, preset pills, theme chooser) |
| License | **Proprietary / decompiled — provenance UNACCEPTABLE** |
| Attribution | n/a |
| Reusable algorithm | None |
| Reusable code | **NONE — hard rule.** Decompilation output must never enter RIKO production |
| Incompatible code | Everything |
| Final RIKO usage | **FEATURE-CHECKLIST REFERENCE ONLY** (preset pill UX, save-preset dialog patterns — already specified in plan §6). Zero code, zero layout XML. |

---

## Verdict table

| Source | License `[R]` | Import decision |
|---|---|---|
| android-visualizer / audio-visualizer | MIT | Concept only (renderer registry) |
| Flux AudioVisualizer | Unverified | Concept only (smoothing constants) |
| JTransforms | BSD-3 | Not needed |
| projectM | LGPL-2.1 | Math vocabulary only — independent reimplementation, no code |
| RealTimeSpectrogramFFT | Unverified | Concept only — independent reimplementation |
| equalizer_ref | Proprietary (decompiled) | Checklist only — **no code ever** |

**Net result: zero external code enters RIKO production from `extra rare files to use`.** All studio styles (CLASSIC / AURORA-PLASMA / SPECTROGRAM / SHAPE) consume the canonical `SpectrumEngine → SpectrumBands` pipeline and are written natively in RIKO's own renderer layer.
