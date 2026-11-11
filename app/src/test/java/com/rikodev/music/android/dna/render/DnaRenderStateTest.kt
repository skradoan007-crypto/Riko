package com.rikodev.music.android.dna.render

import com.rikodev.music.android.dna.genome.*
import org.junit.Assert.*
import org.junit.Test

class DnaRenderStateTest {
    private val features = DnaFeatures.fromNormalized(.8, .55, .6, 2800.0, .45,
        120.0, .9, 30.0, DoubleArray(12) { 1.0 / 12 })

    @Test fun allFamiliesKeepBoundedAlignedBudgetsAndFiniteUniforms() {
        val families = mutableSetOf<DnaGenome.Archetype>()
        for (seed in 0L..255L) {
            val genome = DnaGenomeMapper.map(features, seed, DnaAlgorithmVersion.GENESIS_V1)
            val state = DnaRenderState(genome)
            families += genome.archetype
            assertEquals(genome.archetype.id - 1f, state.family, 0f)
            assertTrue(state.template.name.endsWith(genome.archetype.name))
            assertTrue(state.seed >= 0f && state.seed < 1f)
            assertTrue(state.shape.all { it.isFinite() && it in 0f..1f })
            assertEquals(1f, state.chroma.sum(), .00001f)
            var last = 0
            for (quality in MdnaQuality.entries) {
                val count = state.pointsPerLayer(quality)
                assertEquals(0, count % 96)
                assertTrue(count >= last)
                assertTrue(count * genome.layerCount in 192..163680)
                assertTrue(quality.renderScale in .5f..1f)
                last = count
            }
        }
        assertEquals(DnaGenome.Archetype.values().toSet(), families)
    }

    @Test fun reconstructingProfileReproducesRenderInputs() {
        val genome = DnaGenomeMapper.map(features, Long.MIN_VALUE, DnaAlgorithmVersion.GENESIS_V1)
        val a = DnaRenderState(genome)
        val b = DnaRenderState(DnaCodec.decodeGenome(DnaCodec.encodeGenome(genome)))
        assertEquals(a.template, b.template)
        assertEquals(a.seed, b.seed, 0f)
        assertArrayEquals(a.shape, b.shape, 0f)
        assertArrayEquals(a.traits, b.traits, 0f)
        assertArrayEquals(a.chroma, b.chroma, 0f)
    }

    @Test fun standaloneTemplateCycleNeverSelectsUninitializedGenomeShaders() {
        var template = MdnaTemplate.GENESIS_ORB
        repeat(30) {
            template = template.nextStandalone()
            assertFalse(template.requiresGenome)
        }
    }

    @Test fun unknownChromaHasBalancedFinitePalette() {
        val silent = DnaFeatures.fromNormalized(0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, DoubleArray(12))
        val state = DnaRenderState(DnaGenomeMapper.map(silent, 0L, DnaAlgorithmVersion.GENESIS_V1))
        assertTrue(state.chroma.all { it.isFinite() && it > 0f })
        assertEquals(1f, state.chroma.sum(), .00001f)
    }
}
