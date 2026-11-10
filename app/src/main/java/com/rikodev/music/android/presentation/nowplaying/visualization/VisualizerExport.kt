package com.rikodev.music.android.presentation.nowplaying.visualization

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import com.rikodev.music.android.data.repository.UserVisualizerPreset
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — VISUALIZER EXPORT
//
// Export options:
// 1. Screenshot (PNG) — capture the visualizer preview at 2x resolution
// 2. Preset JSON — share visualizer settings as a portable file
//
// Both are offline, no network required. Files saved to Pictures/RIKO/Visualizer/
// ─────────────────────────────────────────────────────────────────────────────

object VisualizerExport {

    private val json = Json { prettyPrint = true }

    /**
     * Exports the visualizer preview as a PNG screenshot.
     * Saves to Pictures/RIKO/Visualizer/ and returns the URI.
     */
    fun exportScreenshot(
        context: Context,
        imageBitmap: ImageBitmap,
        fileName: String = "riko_visualizer_${System.currentTimeMillis()}"
    ): Result<Uri> = runCatching {
        val bitmap = imageBitmap.asAndroidBitmap()

        // Create 2x resolution copy for quality
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            bitmap.width * 2,
            bitmap.height * 2,
            true
        )

        // Save to Pictures/RIKO/Visualizer/
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val rikoDir = File(picturesDir, "RIKO/Visualizer")
        rikoDir.mkdirs()

        val file = File(rikoDir, "$fileName.png")
        FileOutputStream(file).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        scaledBitmap.recycle()

        // Return content URI via FileProvider
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Exports visualizer presets as a JSON file.
     * Saves to Documents/RIKO/Presets/ and returns the URI.
     */
    fun exportPresets(
        context: Context,
        presets: List<UserVisualizerPreset>,
        fileName: String = "riko_visualizer_presets_${System.currentTimeMillis()}"
    ): Result<Uri> = runCatching {
        val envelope = com.rikodev.music.android.data.repository.UserPresetEnvelope(
            presets = presets
        )
        val jsonString = json.encodeToString(envelope)

        // Save to Documents/RIKO/Presets/
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val rikoDir = File(documentsDir, "RIKO/Presets")
        rikoDir.mkdirs()

        val file = File(rikoDir, "$fileName.json")
        file.writeText(jsonString)

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Shares a screenshot via Android share sheet.
     */
    fun shareScreenshot(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Visualizer"))
    }

    /**
     * Shares preset JSON via Android share sheet.
     */
    fun sharePresets(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Presets"))
    }

    /**
     * Imports presets from a JSON file.
     * Returns the list of imported presets, or empty list on failure.
     */
    fun importPresets(jsonString: String): List<UserVisualizerPreset> {
        return runCatching {
            json.decodeFromString<com.rikodev.music.android.data.repository.UserPresetEnvelope>(jsonString)
                .presets
        }.getOrDefault(emptyList())
    }
}
