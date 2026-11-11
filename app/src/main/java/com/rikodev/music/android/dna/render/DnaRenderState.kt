package com.rikodev.music.android.dna.render

import com.rikodev.music.android.dna.genome.DnaGenome

/** Created once per profile change. Published atomically, never mutated per frame. */
class DnaRenderState(val genome: DnaGenome) {
    val template = when(genome.archetype) {
        DnaGenome.Archetype.ORB -> MdnaTemplate.ORGANISM_ORB
        DnaGenome.Archetype.TERRAIN -> MdnaTemplate.ORGANISM_TERRAIN
        DnaGenome.Archetype.NEBULA -> MdnaTemplate.ORGANISM_NEBULA
    }
    fun pointsPerLayer(quality: MdnaQuality): Int =
        ((genome.particleCount * quality.pointFactor).toInt().coerceIn(96, 32736) / 96) * 96

    internal val family = (genome.archetype.id - 1).toFloat()
    internal val seed = ((genome.geometrySeed ushr 40) and 0xffffff).toFloat() / 16777216f
    internal val shape = floatArrayOf(genome.bodyExpansionPpm/1e6f,genome.brightnessPpm/1e6f,
        genome.surfaceSharpnessPpm/1e6f,genome.deformationPpm/1e6f)
    internal val traits = floatArrayOf(genome.symmetry.toFloat(),genome.flowSpeedPpm/1e6f,
        genome.microDetailPpm/1e6f,genome.paletteRotationMilliDegrees/360000f)
    internal val chroma = FloatArray(12) { genome.chromaAt(it)/1e6f }.also {
        if (it.sum() == 0f) it.fill(1f/12f)
    }
}
