package com.rikodev.music.android.dna.core

import kotlin.math.exp
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — SYNTHETIC SPECTRUM (lab / demo only)
//
// A deterministic 120BPM "song": kick on the beat, hats on the off-beat and a
// slow melodic peak sweeping across the bands. Lets the visual be judged (and
// profiled) with no music playing. Never used for real playback visuals.
// ─────────────────────────────────────────────────────────────────────────────
object SyntheticSpectrum {

    fun fill(out: FloatArray, timeSec: Float) {
        val n = out.size
        if (n == 0) return
        val beat = timeSec * 2f                      // 120 BPM
        val kickPhase = beat - kotlin.math.floor(beat)
        val kick = exp(-kickPhase * 7f)
        val offPhase = (beat + 0.5f) - kotlin.math.floor(beat + 0.5f)
        val hat = exp(-offPhase * 14f)
        val center = 0.5f + 0.35f * sin(timeSec * 0.4f)
        val vib = 0.6f + 0.4f * sin(timeSec * 3f)
        for (i in 0 until n) {
            val x = if (n > 1) i / (n - 1f) else 0f
            val bassShape = exp(-(x / 0.16f) * (x / 0.16f))
            val dm = (x - center) / 0.08f
            val melody = 0.55f * exp(-dm * dm) * vib
            val hatShape = if (x > 0.6f) (x - 0.6f) / 0.4f else 0f
            val v = 0.06f + 0.85f * kick * bassShape + melody + 0.45f * hat * hatShape
            out[i] = v.coerceIn(0f, 1f)
        }
    }
}
