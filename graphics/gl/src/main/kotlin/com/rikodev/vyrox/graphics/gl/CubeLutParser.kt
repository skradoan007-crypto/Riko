package com.rikodev.vyrox.graphics.gl

data class CubeLut3D(
    val title: String,
    val size: Int,
    val rgbData: FloatArray // size * size * size * 3
)

object CubeLutParser {
    fun parse(content: String): CubeLut3D {
        var title = "Untitled LUT"
        var size = 0
        val values = ArrayList<Float>()

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            when {
                line.startsWith("TITLE") -> {
                    title = line.removePrefix("TITLE").trim().trim('"')
                }
                line.startsWith("LUT_3D_SIZE") -> {
                    size = line.removePrefix("LUT_3D_SIZE").trim().toInt()
                }
                line.startsWith("DOMAIN_MIN") || line.startsWith("DOMAIN_MAX") -> Unit
                else -> {
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size == 3) {
                        values.add(parts[0].toFloat())
                        values.add(parts[1].toFloat())
                        values.add(parts[2].toFloat())
                    }
                }
            }
        }

        require(size >= 2) { "Invalid or missing LUT_3D_SIZE in .cube file" }
        val expected = size * size * size * 3
        require(values.size == expected) {
            "Expected $expected RGB floats for size $size LUT, found ${values.size}"
        }
        return CubeLut3D(title = title, size = size, rgbData = values.toFloatArray())
    }
}
