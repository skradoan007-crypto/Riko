package com.rikodev.vyrox.media.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.rikodev.vyrox.core.model.ImportedMediaMetadata
import com.rikodev.vyrox.core.storage.VyRoxStorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaInspector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: VyRoxStorageManager
) {
    suspend fun inspectVideo(uri: Uri): ImportedMediaMetadata = withContext(Dispatchers.IO) {
        val displayName = resolveDisplayName(uri) ?: "VyRox Clip"
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 1080
            val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 1920
            val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
            val (width, height) = if (rot == 90 || rot == 270) rawH to rawW else rawW to rawH

            val thumbPath = extractScaledThumbnail(retriever)
            ImportedMediaMetadata(
                contentUri = uri.toString(),
                displayName = displayName.substringBeforeLast('.'),
                durationMs = durationMs.coerceAtLeast(1_000L),
                width = width,
                height = height,
                rotationDeg = rot,
                thumbnailPath = thumbPath
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extractScaledThumbnail(retriever: MediaMetadataRetriever): String? {
        return runCatching {
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return null
            val targetW = 480
            val targetH = ((frame.height.toFloat() / frame.width.coerceAtLeast(1)) * targetW)
                .toInt().coerceIn(180, 860)
            val scaled = Bitmap.createScaledBitmap(frame, targetW, targetH, true)
            if (scaled !== frame) frame.recycle()

            val file = File(storage.thumbCacheDir, "thumb_${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
            }
            scaled.recycle()
            file.absolutePath
        }.getOrNull()
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()
    }
}
