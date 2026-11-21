package com.rikodev.music.android.ui.background

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Ultra-fast (<1ms) off-main-thread album art pigment extractor.
 * Downsamples artwork to <= 24x24px on Dispatchers.Default and extracts
 * two vibrant, harmonically separated dye colors for fluid marbling.
 */
object RikoPigmentExtractor {

    private val ALBUM_ART_BASE_URI: Uri = Uri.parse("content://media/external/audio/albumart")

    suspend fun extractFromTrack(
        context: Context,
        trackKey: String?,
        albumId: Long?,
        songUri: Uri?,
        fallbackPrimary: Color? = null,
        fallbackSecondary: Color? = null
    ): AlbumPigmentState = withContext(Dispatchers.Default) {
        if (trackKey.isNullOrBlank()) {
            return@withContext AlbumPigmentState()
        }

        val tinyBitmap = decodeTinyArtwork(context, albumId, songUri)
        if (tinyBitmap != null) {
            try {
                return@withContext extractFromTinyBitmap(trackKey, tinyBitmap)
            } finally {
                if (!tinyBitmap.isRecycled) tinyBitmap.recycle()
            }
        }

        // Smooth fallback to already-extracted dynamic colors if MediaStore art is unavailable
        if (fallbackPrimary != null && fallbackPrimary != Color.Transparent) {
            val sec = if (fallbackSecondary != null && fallbackSecondary != Color.Transparent) {
                fallbackSecondary
            } else {
                shiftHueForSecondary(fallbackPrimary)
            }
            return@withContext AlbumPigmentState(
                trackKey = trackKey,
                primary = normalizeDyePigment(fallbackPrimary),
                secondary = normalizeDyePigment(sec),
                hasPigment = true
            )
        }

        AlbumPigmentState(trackKey = trackKey, hasPigment = false)
    }

    private fun decodeTinyArtwork(context: Context, albumId: Long?, songUri: Uri?): Bitmap? {
        if (albumId != null && albumId > 0L) {
            val artUri = ContentUris.withAppendedId(ALBUM_ART_BASE_URI, albumId)
            val bmp = decodeStreamDownsampled(context, artUri)
            if (bmp != null) return bmp
        }
        if (songUri != null) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, songUri)
                val bytes = retriever.embeddedPicture
                if (bytes != null && bytes.isNotEmpty()) {
                    return decodeBytesDownsampled(bytes)
                }
            } catch (_: Throwable) {
                // Ignore unreadable media streams cleanly
            } finally {
                try {
                    retriever.release()
                } catch (_: Throwable) {
                }
            }
        }
        return null
    }

    private fun decodeStreamDownsampled(context: Context, uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 24 && bounds.outHeight / (sample * 2) >= 24) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            val scaled = Bitmap.createScaledBitmap(decoded, 24, 24, true)
            if (scaled !== decoded) decoded.recycle()
            scaled
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeBytesDownsampled(bytes: ByteArray): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 24 && bounds.outHeight / (sample * 2) >= 24) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
            val scaled = Bitmap.createScaledBitmap(decoded, 24, 24, true)
            if (scaled !== decoded) decoded.recycle()
            scaled
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractFromTinyBitmap(trackKey: String, bmp: Bitmap): AlbumPigmentState {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)

        // 12-bin hue histogram weighted by chroma * mid-luminance (favors rich dye pigments)
        val binWeight = FloatArray(12)
        val binR = FloatArray(12)
        val binG = FloatArray(12)
        val binB = FloatArray(12)
        val hsv = FloatArray(3)

        var totalWeight = 0f
        for (px in pixels) {
            val r = AndroidColor.red(px)
            val g = AndroidColor.green(px)
            val b = AndroidColor.blue(px)
            AndroidColor.RGBToHSV(r, g, b, hsv)
            val sat = hsv[1]
            val value = hsv[2]
            if (sat < 0.14f || value < 0.12f || value > 0.97f) continue

            val midLumBias = 1f - abs(value - 0.58f)
            val weight = sat * sat * (0.45f + 0.55f * midLumBias)
            val bin = ((hsv[0] / 30f).toInt()).coerceIn(0, 11)
            binWeight[bin] += weight
            binR[bin] += r * weight
            binG[bin] += g * weight
            binB[bin] += b * weight
            totalWeight += weight
        }

        if (totalWeight <= 0.001f) {
            return AlbumPigmentState(trackKey = trackKey, hasPigment = false)
        }

        var bestBin = 0
        for (i in 1 until 12) {
            if (binWeight[i] > binWeight[bestBin]) bestBin = i
        }

        // Pick a harmonically separated secondary bin (>= 2 bins away in hue)
        var secondBin = -1
        var secondScore = 0f
        for (i in 0 until 12) {
            val dist = min(abs(i - bestBin), 12 - abs(i - bestBin))
            if (dist >= 2 && binWeight[i] > secondScore) {
                secondScore = binWeight[i]
                secondBin = i
            }
        }

        val pR = (binR[bestBin] / binWeight[bestBin]).toInt().coerceIn(0, 255)
        val pG = (binG[bestBin] / binWeight[bestBin]).toInt().coerceIn(0, 255)
        val pB = (binB[bestBin] / binWeight[bestBin]).toInt().coerceIn(0, 255)
        val primary = normalizeDyePigment(Color(pR, pG, pB))

        val secondary = if (secondBin >= 0 && secondScore > binWeight[bestBin] * 0.18f) {
            val sR = (binR[secondBin] / binWeight[secondBin]).toInt().coerceIn(0, 255)
            val sG = (binG[secondBin] / binWeight[secondBin]).toInt().coerceIn(0, 255)
            val sB = (binB[secondBin] / binWeight[secondBin]).toInt().coerceIn(0, 255)
            normalizeDyePigment(Color(sR, sG, sB))
        } else {
            shiftHueForSecondary(primary)
        }

        return AlbumPigmentState(
            trackKey = trackKey,
            primary = primary,
            secondary = secondary,
            hasPigment = true
        )
    }

    /**
     * Keeps extracted dye pigment rich, vibrant, and luminous without blowing out to muddy gray or neon clip.
     */
    private fun normalizeDyePigment(color: Color): Color {
        val hsv = FloatArray(3)
        AndroidColor.RGBToHSV(
            (color.red * 255f).toInt().coerceIn(0, 255),
            (color.green * 255f).toInt().coerceIn(0, 255),
            (color.blue * 255f).toInt().coerceIn(0, 255),
            hsv
        )
        hsv[1] = hsv[1].coerceIn(0.48f, 0.90f)
        hsv[2] = hsv[2].coerceIn(0.54f, 0.94f)
        return Color(AndroidColor.HSVToColor(hsv))
    }

    private fun shiftHueForSecondary(color: Color): Color {
        val hsv = FloatArray(3)
        AndroidColor.RGBToHSV(
            (color.red * 255f).toInt().coerceIn(0, 255),
            (color.green * 255f).toInt().coerceIn(0, 255),
            (color.blue * 255f).toInt().coerceIn(0, 255),
            hsv
        )
        hsv[0] = (hsv[0] + 34f) % 360f
        hsv[1] = max(0.50f, hsv[1] * 0.92f)
        hsv[2] = min(0.95f, hsv[2] * 1.06f)
        return Color(AndroidColor.HSVToColor(hsv))
    }
}
