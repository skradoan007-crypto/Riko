package com.rikodev.vyrox.media.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.rikodev.vyrox.core.model.ExportConfig
import com.rikodev.vyrox.core.model.VideoClip
import com.rikodev.vyrox.core.storage.VyRoxStorageManager
import com.rikodev.vyrox.graphics.gl.VyRoxColorGlEffect
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ExportState {
    data object Idle : ExportState
    data class Running(val progressPct: Int) : ExportState
    data class Completed(val galleryDisplayPath: String, val outputUri: String) : ExportState
    data class Failed(val message: String) : ExportState
}

@Singleton
class VyRoxExportEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: VyRoxStorageManager
) {
    @OptIn(UnstableApi::class)
    fun exportClip(
        sourceUriString: String,
        clip: VideoClip,
        config: ExportConfig
    ): Flow<ExportState> = callbackFlow {
        val mainHandler = Handler(Looper.getMainLooper())
        val safeName = config.outputName.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "VyRox_Export" }
        val fileName = "${safeName}_${System.currentTimeMillis()}.mp4"
        val tempFile = File(storage.renderTmpDir, fileName)
        if (tempFile.exists()) tempFile.delete()

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(sourceUriString))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.sourceTrimStartMs.coerceAtLeast(0L))
                    .setEndPositionMs(clip.sourceTrimEndMs.coerceAtLeast(clip.sourceTrimStartMs + 250L))
                    .build()
            )
            .build()

        val videoEffects = buildList<Effect> {
            config.resolution.targetHeight?.let { h ->
                add(Presentation.createForHeight(h))
            }
            add(VyRoxColorGlEffect(clip.colorGrade))
        }

        val editedItem = EditedMediaItem.Builder(mediaItem)
            .setRemoveAudio(clip.muted)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()

        var transformer: Transformer? = null
        val progressHolder = ProgressHolder()
        var finished = false

        val pollRunnable = object : Runnable {
            override fun run() {
                if (finished) return
                val t = transformer ?: return
                val state = t.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    trySend(ExportState.Running(progressHolder.progress.coerceIn(0, 99)))
                }
                mainHandler.postDelayed(this, 200L)
            }
        }

        mainHandler.post {
            try {
                val t = Transformer.Builder(context)
                    .setVideoMimeType(config.codec.mimeType)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            finished = true
                            mainHandler.removeCallbacks(pollRunnable)
                            val published = publishToMediaStore(tempFile, fileName)
                            tempFile.delete()
                            if (published != null) {
                                trySend(ExportState.Completed("Movies/VyRox/$fileName", published.toString()))
                            } else {
                                trySend(ExportState.Failed("Failed to save exported MP4 to Gallery."))
                            }
                            close()
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            finished = true
                            mainHandler.removeCallbacks(pollRunnable)
                            tempFile.delete()
                            trySend(ExportState.Failed(exportException.localizedMessage ?: "Hardware export failed."))
                            close()
                        }
                    })
                    .build()
                transformer = t
                trySend(ExportState.Running(0))
                t.start(editedItem, tempFile.absolutePath)
                mainHandler.postDelayed(pollRunnable, 200L)
            } catch (e: Exception) {
                finished = true
                trySend(ExportState.Failed(e.localizedMessage ?: "Unable to initialize hardware encoder."))
                close()
            }
        }

        awaitClose {
            finished = true
            mainHandler.post {
                mainHandler.removeCallbacks(pollRunnable)
                transformer?.cancel()
                tempFile.delete()
            }
        }
    }

    private fun publishToMediaStore(sourceFile: File, displayName: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/VyRox")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input -> input.copyTo(out) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            }
            uri
        }.getOrNull()
    }
}
