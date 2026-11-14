package com.rikodev.music.android.dna.render.visual

import org.junit.Assert.*
import org.junit.Test

/**
 * The visual contract is law: every registered visual must be well-formed and
 * MUST react to the class reaction matrix (>= 3 of the six reaction uniforms).
 * Runs over the whole registry — new visuals are covered automatically.
 */
class RikoVisualsContractTest {

    @Test fun registryIsWellFormed() {
        assertTrue("registry must not be empty", RikoVisuals.ALL.isNotEmpty())
        assertEquals("ids must be unique",
            RikoVisuals.ALL.size, RikoVisuals.ALL.map { it.id }.toSet().size)
        assertEquals("names must be unique",
            RikoVisuals.ALL.size, RikoVisuals.ALL.map { it.name }.toSet().size)
    }

    @Test fun everyVisualShaderHonorsTheContract() {
        for (visual in RikoVisuals.ALL) {
            val vert = visual.vertSource
            assertTrue("${visual.id}: must read aId", vert.contains("attribute float aId"))
            assertTrue("${visual.id}: must read uViewport", vert.contains("uniform vec2 uViewport"))
            assertTrue("${visual.id}: must branch on uPass", vert.contains("uniform float uPass"))
            assertTrue("${visual.id}: must output vColor/vLight",
                vert.contains("varying vec3 vColor") && vert.contains("varying float vLight"))
            assertTrue("${visual.id}: must set gl_Position", vert.contains("gl_Position"))
            assertTrue("${visual.id}: must set gl_PointSize", vert.contains("gl_PointSize"))
            assertEquals("${visual.id}: unbalanced braces",
                vert.count { it == '{' }, vert.count { it == '}' })
            val reactionUse = VisualContract.REACTION_UNIFORMS.count { vert.contains(it) }
            assertTrue(
                "${visual.id}: must react to the class matrix (uses only $reactionUse of 6)",
                reactionUse >= 3
            )
        }
    }
}
