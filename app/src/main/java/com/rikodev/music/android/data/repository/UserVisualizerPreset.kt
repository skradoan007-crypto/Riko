package com.rikodev.music.android.data.repository

import kotlinx.serialization.Serializable

/**
 * User studio presets (plan 5.5) — a complete Lab setup the user can save,
 * restore, rename and delete. Renderer stays blind to persistence: presets
 * apply onto [VisualizerPrefs] and the renderer only ever sees live values.
 *
 * Schema (plan 5.5):
 * `version, style, theme, sensitivity, glow, density, shape, motionSpeed,
 * reactivityCurve`
 *
 * Migration: [schemaVersion] travels with every preset; missing fields fall
 * back to defaults, so older presets load unchanged. Invalid-preset recovery:
 * decode failures drop only the corrupt entry, never the whole library.
 */
@Serializable
data class UserVisualizerPreset(
    val id: String,
    val name: String,
    val schemaVersion: Int = SCHEMA_VERSION,
    val styleName: String = "AURORA",
    val themeName: String = "ALBUM_ACCENT",
    val sensitivity: Float = 1.0f,
    val glow: Float = 1.0f,
    val density: Int = 40,
    val shape: String = "RIBBON",
    val motionSpeed: Float = 1.0f,
    val reactivityCurve: Float = 1.0f,
    // Phase 2: Visualizer Studio extended parameters
    val barWidth: Float = 1.0f,
    val barSpacing: Float = 0.3f,
    val smoothing: Float = 0.5f,
    val peakDecay: Float = 0.5f,
    val reflection: Boolean = true,
    val mirror: Boolean = false
) {
    companion object {
        const val SCHEMA_VERSION = 2
    }
}

/** Storage envelope — carries the schema version for the whole library. */
@Serializable
data class UserPresetEnvelope(
    val schemaVersion: Int = UserVisualizerPreset.SCHEMA_VERSION,
    val presets: List<UserVisualizerPreset> = emptyList()
)

private val presetJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

/**
 * Tolerant decode (plan 5.5): schema migration + invalid-preset recovery.
 * A corrupt payload degrades to defaults instead of crashing the Lab.
 */
fun decodeUserPresets(raw: String?): List<UserVisualizerPreset> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        presetJson.decodeFromString(UserPresetEnvelope.serializer(), raw)
            .presets
            .map { it.migrated() }
    }.getOrDefault(emptyList())
}

/** Encode the library with the CURRENT schema version. */
fun encodeUserPresets(presets: List<UserVisualizerPreset>): String =
    presetJson.encodeToString(
        UserPresetEnvelope.serializer(),
        UserPresetEnvelope(presets = presets.map { it.copy(schemaVersion = UserVisualizerPreset.SCHEMA_VERSION) })
    )

/** Field-level migration: older presets get sane defaults for newer fields. */
private fun UserVisualizerPreset.migrated(): UserVisualizerPreset = copy(
    schemaVersion = UserVisualizerPreset.SCHEMA_VERSION,
    glow = glow.coerceIn(0f, 1f),
    sensitivity = sensitivity.coerceIn(0.5f, 1.5f),
    density = if (density in listOf(32, 40, 48)) density else 40,
    motionSpeed = motionSpeed.coerceIn(0.25f, 2f),
    reactivityCurve = reactivityCurve.coerceIn(0.25f, 2f),
    barWidth = barWidth.coerceIn(0.5f, 2f),
    barSpacing = barSpacing.coerceIn(0f, 1f),
    smoothing = smoothing.coerceIn(0f, 1f),
    peakDecay = peakDecay.coerceIn(0.1f, 0.9f),
    name = if (name.isBlank()) "Untitled" else name
)
