package com.rikodev.vyrox.graphics.gl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CubeLutParserTest {
    @Test
    fun `parses valid 2x2x2 cube lut`() {
        val sample = """
            # VyRox Identity 2x2x2 LUT
            TITLE "Identity"
            LUT_3D_SIZE 2
            0.0 0.0 0.0
            1.0 0.0 0.0
            0.0 1.0 0.0
            1.0 1.0 0.0
            0.0 0.0 1.0
            1.0 0.0 1.0
            0.0 1.0 1.0
            1.0 1.0 1.0
        """.trimIndent()

        val parsed = CubeLutParser.parse(sample)
        assertThat(parsed.title).isEqualTo("Identity")
        assertThat(parsed.size).isEqualTo(2)
        assertThat(parsed.rgbData.size).isEqualTo(24)
        assertThat(parsed.rgbData.last()).isEqualTo(1.0f)
    }
}
