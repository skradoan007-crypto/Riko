package com.rikodev.music.android.presentation.nowplaying.visualization

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SPECTRUM STYLE (visualizer_plan §11)
//
// v1 ships TWO renderers, perfected — not eight half-baked styles:
//
//   AURORA  — signature: contour + filled gradient + glow + reflection.
//             "Music reacting", not "equalizer app".
//   CLASSIC — alternate: precise mirrored bars + peak caps.
//
// Plan 5.2 core set completion (canonical SpectrumEngine feed only):
//   SPECTROGRAM — rolling time-frequency waterfall (plan 5.3).
//   SHAPE       — orbital rings/dots (projectM shape VOCABULARY,
//                 reimplemented independently — no external code).
//
// Phase 2 additions (Visualizer Studio):
//   LINE    — smooth interpolated line + gradient fill.
//   DOT     — individual dots per band, modern minimal look.
//   WAVEFORM — full waveform bars + live energy overlay.
//
// One song = one coherent environment: accents always come from the
// artwork-derived palette, never rainbow.
// ─────────────────────────────────────────────────────────────────────────────

enum class SpectrumStyle { AURORA, CLASSIC, SPECTROGRAM, SHAPE, LINE, DOT, WAVEFORM }
