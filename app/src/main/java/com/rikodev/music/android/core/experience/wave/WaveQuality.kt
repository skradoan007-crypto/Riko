package com.rikodev.music.android.core.experience.wave

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — WAVE QUALITY
//
// Configurable quality level for the Wave Engine's path generation.
//
// Controls the number of segments (points) generated per ribbon.
// Higher quality = more points = smoother curves, but more computation.
// Lower quality = fewer points = rougher curves, but faster rendering.
//
// This is NOT a visual effect — it's a performance/quality trade-off.
// The ribbon shape is identical at all quality levels; only the
// point density changes (like DPI for geometry).
//
// USAGE:
//   - Settings screen can expose a "Wave Quality" selector
//   - Low-end devices default to LOW or BALANCED
//   - High-end devices default to HIGH or ULTRA
//   - Battery saver mode can drop to LOW
//   - The generator logic never changes — only segment count does
//
// ⭐ ARCHITECTURAL DECISION
//   Quality is an input to the generator, NOT a rendering concern.
//   The Renderer simply draws whatever OrganicPath it receives.
//   This separation means: change quality → new path → same renderer.
// ─────────────────────────────────────────────────────────────────────────────

enum class WaveQuality(val segmentCount: Int) {

    /**
     * Low quality — 32 segments.
     * Suitable for low-end devices, battery saver mode, or background rendering.
     * Curves are smooth enough for small ribbon sizes but may show
     * slight angularity at larger sizes.
     */
    LOW(32),

    /**
     * Balanced quality — 48 segments.
     * The default for most devices. Good balance of smoothness and performance.
     * Curves look organic at normal ribbon sizes.
     */
    BALANCED(48),

    /**
     * High quality — 64 segments.
     * Recommended for mid-range to high-end devices.
     * Very smooth curves even at large ribbon sizes.
     * Minimal performance impact on modern hardware.
     */
    HIGH(64),

    /**
     * Ultra quality — 96 segments.
     * Maximum smoothness. Use for hero moments, screenshots, or
     * devices with plenty of GPU headroom.
     * Curves are indistinguishable from continuous at any size.
     */
    ULTRA(96)
}
