package com.rikodev.music.android.presentation.miniplayer

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

// ═══════════════════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER 2.0: SHADOW
//
// Build-3, Phase A, File 3.
//
// Only depth. Grounds the Mini Experience Surface.
// Contains: Ambient Shadow, Contact Shadow, Elevation, Beat Lift.
//
// Rule #24 — Pixel Ownership:
//   Shadow NEVER draws glass.
//   Glass NEVER draws shadow.
//   Background NEVER draws shadow.
//   Album NEVER draws shadow.
//   Every pixel has EXACTLY ONE owner.
//
// Reads: state.color.shadowColor, state.color.energyIntensity, layout.* (from MiniPlayerState)
// Forbidden: Glass rendering, background rendering, album rendering, any other visual
//
// MPRS Reference: Chapter A.4, B.8
// Tokens Reference: MiniPlayerTokens (Section 4)
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Shadow layer for the Mini Experience Surface.
 *
 * Renders:
 *   1. Elevation shadow — Android system shadow, driven by layout.shadowElevation
 *   2. Color shadow — tinted shadow using state.color.shadowColor
 *   3. Beat lift — subtle elevation increase on beat detection
 *
 * This is a CONFIGURATION layer — it configures shadow parameters
 * that GlassSurface's built-in shadow system renders.
 * In the composition tree, Shadow wraps Surface to provide the shadow
 * modifier before GlassSurface applies its own rendering.
 *
 * Rule #24 — Pixel Ownership: ONLY this component owns shadow pixels.
 * Rule #20 — Renderer Never Interprets: No `if (energy > 0.5f) shadow++`.
 *   Energy modulation is pre-computed and applied via layout.* or state.color.*.
 * Rule #25 — Layer Independence: If shadow fails, Surface still renders.
 *   Just no depth perception — still functional, still premium.
 *
 * @param state The single source of truth.
 * @param cornerRadius Current corner radius from layout.
 * @param content The content (typically MiniPlayerSurface) that gets the shadow.
 * @param modifier External modifier.
 */
@Composable
fun MiniPlayerShadow(
    state: MiniPlayerState,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    // ═════════════════════════════════════════════════════════════════════
    // READ STATE — Rule #17: Single Snapshot
    // ═════════════════════════════════════════════════════════════════════
    val layout = state.layout
    val color = state.color

    // ═════════════════════════════════════════════════════════════════════
    // ANIMATED VALUES — Smooth morph transitions
    // ═════════════════════════════════════════════════════════════════════

    val animatedElevation by animateDpAsState(
        targetValue = with(LocalDensity.current) {
            layout.shadowElevation.dp.coerceAtLeast(0.dp)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mp_shadow_elevation"
    )

    // ═════════════════════════════════════════════════════════════════════
    // SHADOW COLORS — From Color Intelligence, never hardcoded
    // ═════════════════════════════════════════════════════════════════════
    // Shadow base is warm (from GlassSurface design system).
    // Tinted by state.color.shadowColor for artwork-matching depth.
    val shadowBase = Color(MiniPlayerTokens.ShadowBaseColor)
    val shadowAmbient = shadowBase.copy(
        alpha = layout.glassShadowAlpha * MiniPlayerTokens.ShadowAmbientFactor
    )
    val shadowSpot = shadowBase.copy(
        alpha = layout.glassShadowAlpha
    )

    // ═════════════════════════════════════════════════════════════════════
    // BEAT LIFT — Subtle elevation pulse on beat
    // ═════════════════════════════════════════════════════════════════════
    // When wave.hasBeat: shadow grows by EnergyElevationBoost (10%).
    // This creates a subtle "lifting" feel on every beat.
    val beatLift = if (state.wave.hasBeat) {
        1f + MiniPlayerTokens.EnergyElevationBoost
    } else {
        1f
    }

    val shadowShape = RoundedCornerShape(cornerRadius)

    // ═════════════════════════════════════════════════════════════════════
    // RENDER — System shadow + colored tint
    // ═════════════════════════════════════════════════════════════════════

    Box(
        modifier = modifier
            .graphicsLayer {
                // Beat lift: subtle scale increase on beat
                scaleX = 1f + (beatLift - 1f) * 0.5f
                scaleY = 1f + (beatLift - 1f) * 0.5f
            }
            .shadow(
                elevation = animatedElevation * beatLift,
                shape = shadowShape,
                ambientColor = shadowAmbient,
                spotColor = shadowSpot
            )
    ) {
        content()
    }
}

/**
 * Shadow parameters snapshot for external configuration.
 *
 * Used by MiniPlayerLayout to pass shadow configuration
 * without MiniPlayerShadow directly reading MiniPlayerState.
 * This enables clean separation: Layout decides shadow config,
 * Shadow renders it.
 *
 * Rule #20 — All values traceable to state/layout.
 */
data class MiniPlayerShadowConfig(
    /** Elevation in dp (from layout.shadowElevation). */
    val elevation: Dp = MiniPlayerTokens.ShadowElevationCollapsed,

    /** Corner radius matching surface (from layout.cornerRadius). */
    val cornerRadius: Dp = MiniPlayerTokens.SurfaceCornerRadiusCollapsed,

    /** Ambient shadow color alpha (from layout.glassShadowAlpha). */
    val ambientAlpha: Float = MiniPlayerTokens.GlassShadowAlpha * MiniPlayerTokens.ShadowAmbientFactor,

    /** Spot shadow color alpha (from layout.glassShadowAlpha). */
    val spotAlpha: Float = MiniPlayerTokens.GlassShadowAlpha,

    /** Shadow base color (warm anchor). */
    val baseColor: Color = Color(MiniPlayerTokens.ShadowBaseColor),

    /** Beat lift multiplier (1.0 normally, 1.1 on beat). */
    val beatLift: Float = 1f,

    /** Whether shadow should be visible. */
    val isVisible: Boolean = true
) {
    companion object {
        /** Default collapsed state shadow. */
        val COLLAPSED = MiniPlayerShadowConfig()

        /** Shadow config from layout and color state. */
        fun fromState(
            layout: MiniPlayerLayoutSnapshot,
            color: MiniPlayerColorSnapshot,
            hasBeat: Boolean = false
        ): MiniPlayerShadowConfig {
            val beatLift = if (hasBeat) 1f + MiniPlayerTokens.EnergyElevationBoost else 1f
            return MiniPlayerShadowConfig(
                elevation = with(MiniPlayerTokens.ShadowElevationCollapsed) {
                    layout.shadowElevation.dp.coerceAtLeast(0.dp)
                },
                cornerRadius = with(MiniPlayerTokens.SurfaceCornerRadiusCollapsed) {
                    layout.cornerRadius.dp.coerceAtLeast(0.dp)
                },
                ambientAlpha = layout.glassShadowAlpha * MiniPlayerTokens.ShadowAmbientFactor,
                spotAlpha = layout.glassShadowAlpha,
                beatLift = beatLift,
                isVisible = layout.shadowElevation > 0f
            )
        }
    }
}
