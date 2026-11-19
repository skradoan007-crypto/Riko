package com.rikodev.music.android.presentation.continuum

import android.os.Build

// ─────────────────────────────────────────────────────────────────────────────
// RIKO CONTINUUM + LIQUID NEXUS — VISUAL QUALITY ENGINE (P7)
//
// Separation of concerns:
//   1. User Preference  : VisualQualityTier (ULTRA default, BALANCED, LITE)
//                         persisted permanently in DataStore; NEVER silently
//                         overwritten on older Android versions or shader errors.
//   2. Device Capability: SDK/API checks (AGSL >= API 33, RenderEffect >= API 31,
//                         system Reduce Motion) + runtime failure counter.
//   3. Effective Profile: Concrete rendering switches resolved from (1) + (2).
// ─────────────────────────────────────────────────────────────────────────────

enum class VisualQualityTier(
    val displayName: String,
    val subtitle: String
) {
    ULTRA(
        displayName = "Ultra",
        subtitle = "AGSL liquid shaders, dynamic blur, artwork colors & MDNA contour motion"
    ),
    BALANCED(
        displayName = "Balanced",
        subtitle = "Custom Bézier liquid morphing, soft blur & lightweight polished motion"
    ),
    LITE(
        displayName = "Lite",
        subtitle = "Smooth shape transitions, static gradients & dim overlay for max efficiency"
    );

    companion object {
        /** Mandatory default on first launch: Ultra. */
        val DEFAULT: VisualQualityTier = ULTRA

        fun fromPersisted(raw: String?): VisualQualityTier =
            raw?.let { name -> entries.firstOrNull { it.name.equals(name, ignoreCase = true) } }
                ?: DEFAULT
    }
}

data class DeviceRenderCapabilities(
    val sdkInt: Int,
    val supportsAgslShader: Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU,
    val supportsRenderEffectBlur: Boolean = sdkInt >= Build.VERSION_CODES.S,
    val reduceMotion: Boolean = false
) {
    companion object {
        fun current(reduceMotion: Boolean = false): DeviceRenderCapabilities =
            DeviceRenderCapabilities(
                sdkInt = Build.VERSION.SDK_INT,
                supportsAgslShader = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                supportsRenderEffectBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                reduceMotion = reduceMotion
            )
    }
}

data class EffectiveRenderProfile(
    val selectedTier: VisualQualityTier,
    val useAgslShaders: Boolean,
    val useRenderEffectBlur: Boolean,
    val useOrganicBezier: Boolean,
    val useDynamicArtworkColors: Boolean,
    val useMdnaReactivity: Boolean,
    val useGlowAndSpecular: Boolean,
    val useDimScrubOverlay: Boolean,
    val scrubBlurRadiusDp: Float,
    val reduceMotion: Boolean,
    val shaderFailureFallbackActive: Boolean
)

object VisualQualityResolver {

    const val MAX_SHADER_FAILURES_BEFORE_FALLBACK = 3

    /**
     * Computes the actual rendering profile without ever mutating [selectedTier].
     * Older Android versions gracefully degrade unsupported APIs (AGSL / RenderEffect)
     * while preserving the user's saved ULTRA preference.
     */
    fun resolve(
        selectedTier: VisualQualityTier,
        capabilities: DeviceRenderCapabilities,
        shaderFailureCount: Int = 0
    ): EffectiveRenderProfile {
        val shaderFallback = shaderFailureCount >= MAX_SHADER_FAILURES_BEFORE_FALLBACK
        val reduceMotion = capabilities.reduceMotion

        return when (selectedTier) {
            VisualQualityTier.ULTRA -> {
                val canAgsl = capabilities.supportsAgslShader && !shaderFallback && !reduceMotion
                val canBlur = capabilities.supportsRenderEffectBlur
                EffectiveRenderProfile(
                    selectedTier = selectedTier,
                    useAgslShaders = canAgsl,
                    useRenderEffectBlur = canBlur,
                    useOrganicBezier = true,
                    useDynamicArtworkColors = true,
                    useMdnaReactivity = !reduceMotion,
                    useGlowAndSpecular = !shaderFallback,
                    useDimScrubOverlay = !canBlur,
                    scrubBlurRadiusDp = if (canBlur) 18f else 0f,
                    reduceMotion = reduceMotion,
                    shaderFailureFallbackActive = shaderFallback
                )
            }
            VisualQualityTier.BALANCED -> {
                val canBlur = capabilities.supportsRenderEffectBlur
                EffectiveRenderProfile(
                    selectedTier = selectedTier,
                    useAgslShaders = false,
                    useRenderEffectBlur = canBlur,
                    useOrganicBezier = true,
                    useDynamicArtworkColors = true,
                    useMdnaReactivity = !reduceMotion,
                    useGlowAndSpecular = false,
                    useDimScrubOverlay = !canBlur,
                    scrubBlurRadiusDp = if (canBlur) 10f else 0f,
                    reduceMotion = reduceMotion,
                    shaderFailureFallbackActive = false
                )
            }
            VisualQualityTier.LITE -> {
                EffectiveRenderProfile(
                    selectedTier = selectedTier,
                    useAgslShaders = false,
                    useRenderEffectBlur = false,
                    useOrganicBezier = false,
                    useDynamicArtworkColors = false,
                    useMdnaReactivity = false,
                    useGlowAndSpecular = false,
                    useDimScrubOverlay = true,
                    scrubBlurRadiusDp = 0f,
                    reduceMotion = reduceMotion,
                    shaderFailureFallbackActive = false
                )
            }
        }
    }
}
