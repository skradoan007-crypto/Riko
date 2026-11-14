package com.rikodev.music.android.dna.render.visual

import android.opengl.GLSurfaceView
import com.rikodev.music.android.dna.core.BandSource

/**
 * One registered RIKO visual: stable id, display name, its vertex shader
 * (kept for contract tests) and a renderer factory for the host.
 */
data class RikoVisual(
    val id: String,
    val name: String,
    val vertSource: String,
    val factory: (BandSource, Float) -> GLSurfaceView.Renderer
)

/** The RIKO visual lineup. Grows as Phase 4 lands the remaining visuals. */
object RikoVisuals {
    val ALL: List<RikoVisual> = listOf(
        RikoVisual(RibbonVisual.ID, RibbonVisual.NAME, RibbonVisual.VERT) { source, refresh ->
            RibbonRenderer(source, refresh)
        },
        RikoVisual(PulseRingsVisual.ID, PulseRingsVisual.NAME, PulseRingsVisual.VERT) { source, refresh ->
            PulseRingsRenderer(source, refresh)
        },
        RikoVisual(AuroraVisual.ID, AuroraVisual.NAME, AuroraVisual.VERT) { source, refresh ->
            AuroraRenderer(source, refresh)
        },
        RikoVisual(SwarmVisual.ID, SwarmVisual.NAME, SwarmVisual.VERT) { source, refresh ->
            SwarmRenderer(source, refresh)
        },
        RikoVisual(BloomVisual.ID, BloomVisual.NAME, BloomVisual.VERT) { source, refresh ->
            BloomRenderer(source, refresh)
        },
        RikoVisual(MirrorVisual.ID, MirrorVisual.NAME, MirrorVisual.VERT) { source, refresh ->
            MirrorRenderer(source, refresh)
        },
        RikoVisual(EmberVisual.ID, EmberVisual.NAME, EmberVisual.VERT) { source, refresh ->
            EmberRenderer(source, refresh)
        },
        RikoVisual(GridVisual.ID, GridVisual.NAME, GridVisual.VERT) { source, refresh ->
            GridRenderer(source, refresh)
        },
        RikoVisual(OrbVisual.ID, OrbVisual.NAME, OrbVisual.VERT) { source, refresh ->
            OrbRenderer(source, refresh)
        }
    )
}
