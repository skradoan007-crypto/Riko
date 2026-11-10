package com.rikodev.music.android.dna.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MdnaTemplateTest {

    private val all = MdnaTemplate.entries

    @Test
    fun namesAndLabels_areUnique() {
        assertEquals(all.size, all.map { it.name }.toSet().size)
        assertEquals(all.size, all.map { it.label }.toSet().size)
    }

    @Test
    fun pointCounts_areGranular_positive_andGrowWithQuality() {
        for (t in all) {
            var last = 0
            for (q in MdnaQuality.entries) {
                val n = t.pointsPerLayer(q)
                assertTrue("${t.label}/${q.label}: $n", n > 0 && n % t.granule == 0)
                assertTrue("${t.label}: $n should exceed $last", n > last)
                last = n
                assertTrue("${t.label}/${q.label} total ${t.totalPoints(q)}", t.totalPoints(q) <= 200_000)
            }
        }
    }

    @Test
    fun dnaBloom_keepsTheShippedP1Budgets() {
        val t = MdnaTemplate.DNA_BLOOM
        assertEquals(4, t.layers)
        assertEquals(2_500, t.pointsPerLayer(MdnaQuality.LOW))
        assertEquals(8_000, t.pointsPerLayer(MdnaQuality.MID))
        assertEquals(20_000, t.pointsPerLayer(MdnaQuality.HIGH))
    }

    @Test
    fun terrain_rowsAreFixed_qualityScalesDotsPerRow() {
        val t = MdnaTemplate.DOT_TERRAIN
        assertTrue("history ring must hold every row", MDNA_TERRAIN_ROWS <= 64)
        var lastCols = 0
        for (q in MdnaQuality.entries) {
            val n = t.pointsPerLayer(q)
            assertEquals(0, n % MDNA_TERRAIN_ROWS)
            val cols = n / MDNA_TERRAIN_ROWS
            assertTrue(cols > lastCols)
            lastCols = cols
        }
    }

    @Test
    fun gain_isNeutralAtMid_andClamped() {
        for (t in all) {
            assertEquals(1f, t.gain(MdnaQuality.MID), 0.02f)
            for (q in MdnaQuality.entries) assertTrue(t.gain(q) in 0.45f..1.3f)
        }
    }

    @Test
    fun fromName_fallsBackToSignatureScene_andCyclesWrap() {
        assertEquals(MdnaTemplate.DNA_BLOOM, MdnaTemplate.fromName(null))
        assertEquals(MdnaTemplate.DNA_BLOOM, MdnaTemplate.fromName("REMOVED_TEMPLATE"))
        assertEquals(MdnaTemplate.HALO, MdnaTemplate.fromName("HALO"))
        var t = MdnaTemplate.entries.first()
        repeat(all.size) { t = t.next() }
        assertEquals(MdnaTemplate.entries.first(), t)
        assertEquals(MdnaTemplate.entries.last(), MdnaTemplate.entries.first().previous())
    }

    @Test
    fun shaderSources_areWellFormed_andKotlinConstantsMatchGlsl() {
        for (t in all) {
            val vs = t.vertexSource.trim()
            val fs = t.fragmentSource.trim()
            assertTrue("${t.label} vs version", vs.startsWith("#version 300 es"))
            assertTrue("${t.label} fs version", fs.startsWith("#version 300 es"))
            assertTrue("${t.label} must set gl_PointSize", vs.contains("gl_PointSize"))
            assertTrue("${t.label} must output vColor", vs.contains("out vec3 vColor"))
            assertTrue("${t.label} fs must emit fragColor", fs.contains("fragColor"))
        }
        // Kotlin ↔ GLSL layout constants must never drift apart.
        assertTrue(MdnaTemplate.DOT_TERRAIN.vertexSource.contains("const float ROWS = 56.0;"))
        assertTrue(MdnaTemplate.DOT_TERRAIN.vertexSource.contains("const float HIST = 64.0;"))
        assertTrue(MdnaTemplate.PULSE_LENS.vertexSource.contains("const float STRANDS = 28.0;"))
        assertEquals(56, MDNA_TERRAIN_ROWS)
        assertEquals(28, MDNA_LENS_STRANDS)
    }
}
