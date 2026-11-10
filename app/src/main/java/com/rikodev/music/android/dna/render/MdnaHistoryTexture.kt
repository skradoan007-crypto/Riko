package com.rikodev.music.android.dna.render

import android.opengl.GLES30
import com.rikodev.music.android.dna.core.HistoryClock
import java.nio.ByteBuffer

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — SPECTRUM HISTORY TEXTURE (render thread only)
//
// A rolling [rows] × [bands] R8 texture: each pushed row is one smoothed spectrum
// snapshot. Vertex shaders fetch it with texelFetch to draw "the last N moments"
// (Dot Terrain now; Ripple Terrain / Silk Ribbon later). [head] is the ring index
// of the NEWEST row; a shader reads age a at (head − a) mod rows.
// ─────────────────────────────────────────────────────────────────────────────
internal class MdnaHistoryTexture(val bands: Int, val rows: Int = ROWS) {

    var texture = 0
        private set

    /** Ring index of the newest row. */
    var head = 0
        private set

    private val rowBytes = ByteArray(bands)
    private val rowBuffer: ByteBuffer = ByteBuffer.allocateDirect(bands)

    fun create() {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        texture = ids[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_R8, bands, rows, 0,
            GLES30.GL_RED, GLES30.GL_UNSIGNED_BYTE, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        head = 0
    }

    /** Appends one spectrum snapshot as the newest row. */
    fun pushRow(levels: FloatArray) {
        HistoryClock.quantize(levels, rowBytes)
        rowBuffer.clear()
        rowBuffer.put(rowBytes)
        rowBuffer.flip()
        head = (head + 1) % rows
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glTexSubImage2D(
            GLES30.GL_TEXTURE_2D, 0, 0, head, bands, 1,
            GLES30.GL_RED, GLES30.GL_UNSIGNED_BYTE, rowBuffer
        )
    }

    fun bind(unit: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
    }

    fun release() {
        if (texture != 0) GLES30.glDeleteTextures(1, intArrayOf(texture), 0)
        texture = 0
    }

    companion object {
        /** Must equal `HIST` in terrain.vert. */
        const val ROWS = 64
    }
}
