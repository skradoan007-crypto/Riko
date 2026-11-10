package com.rikodev.music.android.presentation.nowplaying.visualization

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Rolling spectrogram history (plan 5.3) — built ONLY from the canonical
 * playback spectrum (`SpectrumEngine → SpectrumBands`); RIKO never requests
 * microphone access to make the studio look professional.
 *
 * Efficiency contract (plan 5.3): the history is one remembered bitmap;
 * columns are written INCREMENTALLY as spectrum frames arrive (throttled to
 * the DSP cadence), and a frame draw is just two blit slices for the
 * wrap-around. No image/path structures are recreated per frame.
 */
class SpectrogramHistory(
    private val columns: Int = DEFAULT_COLUMNS,
    private val rows: Int = DEFAULT_ROWS
) {

    private val bitmap: Bitmap = Bitmap.createBitmap(columns, rows, Bitmap.Config.ARGB_8888)
    val image: ImageBitmap = bitmap.asImageBitmap()

    /** Next column to be written = the OLDEST slice in the ring. */
    var writeColumn: Int = 0
        private set

    private var lastPushNanos = 0L

    /**
     * Append one spectrum frame as a column (bass at the bottom). Self-
     * throttled to the DSP cadence so a fast display never burns history.
     */
    fun push(levels: FloatArray) {
        if (levels.isEmpty()) return
        val now = System.nanoTime()
        if (now - lastPushNanos < MIN_COLUMN_INTERVAL_NS) return
        lastPushNanos = now

        val n = levels.size
        for (r in 0 until rows) {
            // Row 0 = treble (top), last row = bass (bottom).
            val src = n - 1 - (r * n / rows)
            val level = levels[src.coerceIn(0, n - 1)].coerceIn(0f, 1f)
            bitmap.setPixel(writeColumn, r, cellColor(level))
        }
        writeColumn = (writeColumn + 1) % columns
    }

    /**
     * Cell color: cold (deep violet, transparent) → hot (bright accent) as
     * the level rises. Pure int math — zero allocation per cell.
     */
    private fun cellColor(level: Float): Int {
        val t = level * level // perceptual-ish lift: quiet stays dark
        val a = (40 + 215 * t).toInt()
        val r = (30 + 225 * t).toInt()
        val g = (12 + 150 * t).toInt()
        val b = (70 + 185 * t).toInt()
        return AndroidColor.argb(a, r, g, b)
    }

    private companion object {
        const val DEFAULT_COLUMNS = 128
        const val DEFAULT_ROWS = 48

        /** ~30Hz — matches the canonical DSP emit cadence. */
        const val MIN_COLUMN_INTERVAL_NS = 33_000_000L
    }
}
