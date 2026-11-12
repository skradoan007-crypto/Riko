# Musical response correction after user device feedback

The user's device screenshots and three reference recordings showed the intended
visual language: frequency-local peaks, expansion and brightness following sound.
The previous build passing CI did not prove this musical acceptance criterion.
The previous implementation was inadequate; this correction is not a claim of device
acceptance or a guarantee of reference-video visual quality.

## Code findings
1. SpectrumEngine reduced unnormalized FFT magnitudes through the legacy bar UI's
   -60..0 dB range and treble tilt. Larger magnitudes saturated at 1 before MDNA received
   them. This could hide loud/quiet contrast and flatten peaks. The prior finite-range
   test could pass despite that loss of information.
2. OrganismShaders did not consume uBands[40]. Three global averages modulated an
   independently time-animated shape. Averaging across empty low-frequency FFT bins
   further weakened bass response. Automatic camera rotation continued during playback.
3. The unanalyzed-song Genesis fallback also contained time-driven waves/twists.
4. Offline three-band input was mixed into the live spectrum at 25%, flattening the
   frequency-local response. Stored band amplitudes and live bar levels were not
   calibrated to the same physical scale.

## Corrections
- Reuse the same FFT; add optional MDNA-only band power accumulation and Hann/FFT-size
  compensated absolute RMS. Perceptual output is sqrt(RMS), with no per-frame automatic
  peak normalization and no legacy treble-tilt saturation. Existing bar/waveform outputs
  and byte-exact PCM passthrough retain their original behaviour.
- Bass/mid/high envelopes recover summed power from the 40 levels, rather than averaging
  against empty bins. All three organism archetypes now sample individual spectral bins
  to drive local geometry/brightness. The unanalyzed fallback uses stronger local energy
  and bass expansion as well.
- Remove clock-driven surface deformation and automatic rotation from the MDNA organism
  and Genesis fallback. Constant input settles to a constant shape. Silent input settles
  to the resting genome. Camera gestures, tap pulses and deliberate generation birth
  remain independent user/evolution actions. Legacy non-MDNA templates retain rotation.
- Live signal is authoritative when available. The stored timeline supplies a smooth
  fallback when live data is missing; its absolute band RMS is spread with conserved
  power. Export uses the same scale. Offline genome still supplies stable song identity.
- Player volume and system stream volume/mute apply a bounded estimated output gain on
  the render side. This is not calibrated speaker SPL and cannot reconstruct hardware
  EQ, Bluetooth remote gain or downstream DynamicsProcessing exactly.
- Added live bass/mid/high diagnostics inside Controls. Mid energy is explicitly labelled
  as including both voices and instruments. There is no vocal source-separation model.
- Bounded the nebula's maximum excursion and adjusted its fit radius after a viewport
  stress test caught clipping at maximum input.

## Validation actually executed
- tools/test_genesis_final.sh passed existing camera/code/timeline checks and a new
  production Java scaling check against independently computed Hann-windowed PCM DFT:
  absolute sine RMS, 10x PCM amplitude contrast, FFT-size invariance, silence, power
  aggregation, nonfinite input handling.
- tools/organism_shader_probe.py --response passed real ESSL compilation/linking on Mesa,
  six archetype variations, independent bass/mid/treble changes and 18 viewport/quality
  extremes. Same constant or silent inputs at shader times 1s and 99s produced identical
  pixels for all three archetypes and the unanalyzed-song fallback. Moving a spectral
  peak between bins while holding the global band envelopes constant changed the image.
- New Kotlin SpectrumEngine regression tests and existing PCM passthrough tests are
  included in the CI filter, but were not executed here. Their Android compilation and
  a fresh release build remain pending. Previous CI success applies to the previous source.
- Workflow YAML/shell syntax and git diff --check passed. No CI run or remote push made.

## Evidence and its limits
`evidence/response-correction/pcm-response-proof.mp4` is a 6-second diagnostic:
0–1s silence; 1–3s synthetic 100 Hz pulses; 3–5s synthetic 1 kHz pulses; 5–6s silence.
It renders the actual checked-in ESSL on Mesa, driven by a Python FFT/Hann/RMS equivalent
of the calibrated algorithm with attack/release smoothing. It does not execute Android
Media3/GLSurfaceView, the Kotlin SpectrumEngine, device audio routing or output latency.
It is not an Android recording or proof of phone AV synchronization. No supplied
reference image/video is used as an in-app asset or background.

Device gate: compare bass-heavy and vocal-led tracks, silence/mute, soft/loud sections,
seek and route changes; watch Controls levels and shape together. Then judge visual
quality against the references. Hardware EQ/true isolated-vocal response is not solved
by frequency bands alone. Do not mark the musical/visual gate passed without that review.
