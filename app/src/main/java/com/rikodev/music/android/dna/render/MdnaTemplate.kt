package com.rikodev.music.android.dna.render

import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Rows of the Dot Terrain grid. MUST equal `ROWS` in terrain.vert. */
const val MDNA_TERRAIN_ROWS = 56

/** Strands of the Pulse Lens. MUST equal `STRANDS` in lens.vert. */
const val MDNA_LENS_STRANDS = 28

/** How a template wants the camera. */
enum class CameraKind {
    /** Round object: camera distance is derived so [MdnaTemplate.fitRadius] always fits the screen. */
    SPHERE,

    /** Screen-space layout: the template lays itself out from `uView` (half-extent of the z=0 plane). */
    SCREEN
}

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — TEMPLATE CATALOG
//
// A template = shader pair + a point budget + a camera rule. EVERYTHING else
// (signal, smoothing, beat punch, bloom, latency sync, quality governor, input) is
// shared engine, so adding a template is a shader + one enum entry.
//
// Pure: no GL here, so the catalog invariants are unit-tested on the JVM.
// ─────────────────────────────────────────────────────────────────────────────
enum class MdnaTemplate(
    val label: String,
    val vertexSource: String,
    val fragmentSource: String,
    /** Points per layer at MID quality. */
    val basePointsPerLayer: Int,
    val layers: Int,
    /** Point counts are rounded to a multiple of this (grid / strand layouts need it). */
    val granule: Int,
    val camera: CameraKind,
    /** SPHERE only: the content radius (incl. displacement) that must stay on screen. */
    val fitRadius: Float,
    val rotates: Boolean,
    val defaultPitchDeg: Float,
    val usesHistory: Boolean
) {
    DNA_BLOOM(
        "DNA Bloom", MdnaShaders.SCENE_VERT, MdnaShaders.SCENE_FRAG,
        basePointsPerLayer = 8_000, layers = 4, granule = 1,
        camera = CameraKind.SPHERE, fitRadius = 1.05f,
        rotates = true, defaultPitchDeg = 12f, usesHistory = false
    ),
    HALO(
        "Halo", MdnaShaders.HALO_VERT, MdnaShaders.SPRITE_SOFT_FRAG,
        basePointsPerLayer = 26_000, layers = 1, granule = 1,
        camera = CameraKind.SPHERE, fitRadius = 1.15f,
        rotates = false, defaultPitchDeg = 6f, usesHistory = false
    ),
    DOT_TERRAIN(
        "Dot Terrain", MdnaShaders.TERRAIN_VERT, MdnaShaders.SPRITE_SOFT_FRAG,
        // Rows are FIXED (56 = TERRAIN_ROWS in the shader); quality scales dots per row.
        basePointsPerLayer = 6_720, layers = 1, granule = MDNA_TERRAIN_ROWS,
        camera = CameraKind.SCREEN, fitRadius = 0f,
        rotates = false, defaultPitchDeg = 22f, usesHistory = true
    ),
    PULSE_LENS(
        "Pulse Lens", MdnaShaders.LENS_VERT, MdnaShaders.SPRITE_SOFT_FRAG,
        // 28 strands interleaved in the vertex id (STRANDS in the shader).
        basePointsPerLayer = 14_000, layers = 1, granule = MDNA_LENS_STRANDS,
        camera = CameraKind.SCREEN, fitRadius = 0f,
        rotates = false, defaultPitchDeg = 4f, usesHistory = false
    );

    /** Points per layer for [quality], rounded to this template's [granule] (never 0). */
    fun pointsPerLayer(quality: MdnaQuality): Int {
        val raw = basePointsPerLayer * quality.pointFactor
        val rounded = (raw / granule).roundToInt() * granule
        return if (rounded < granule) granule else rounded
    }

    fun totalPoints(quality: MdnaQuality): Int = pointsPerLayer(quality) * layers

    /** Brightness compensation: fewer points per layer → each one a bit brighter, and vice versa. */
    fun gain(quality: MdnaQuality): Float =
        sqrt(basePointsPerLayer.toFloat() / pointsPerLayer(quality)).coerceIn(0.45f, 1.3f)

    fun next(): MdnaTemplate = entries[(ordinal + 1) % entries.size]

    fun previous(): MdnaTemplate = entries[(ordinal + entries.size - 1) % entries.size]

    companion object {
        /** Persisted by name; an unknown/removed name falls back to the signature scene. */
        fun fromName(name: String?): MdnaTemplate = entries.firstOrNull { it.name == name } ?: DNA_BLOOM
    }
}
