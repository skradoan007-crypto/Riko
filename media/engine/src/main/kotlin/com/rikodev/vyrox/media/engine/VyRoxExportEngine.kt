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
import androidx.media3.transformer.DefaultEncoderFactory
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ExportState {
    data object Idle : ExportState
    data class Running(val progressPct: Int, val stage: String = "Encoding video...") : ExportState
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

        val safeStart = clip.sourceTrimStartMs.coerceAtLeast(0L)
        val safeEnd = clip.sourceTrimEndMs.coerceAtLeast(safeStart + 250L)
        val isTrimmed = safeStart > 0L || (clip.sourceDurationMs > 0L && safeEnd < clip.sourceDurationMs - 100L)

        val itemBuilder = MediaItem.Builder().setUri(Uri.parse(sourceUriString))
        if (isTrimmed) {
            itemBuilder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(safeStart)
                    .setEndPositionMs(safeEnd)
                    .build()
            )
        }

        val videoEffects = buildList<Effect> {
            // Use createForShortSide so BOTH portrait (9:16) and landscape (16:9) keep clean even dimensions
            config.resolution.targetHeight?.let { shortSide ->
                add(Presentation.createForShortSide(shortSide))
            }
            if (clip.colorGrade.enabled && !clip.colorGrade.isNeutral()) {
                add(VyRoxColorGlEffect(clip.colorGrade))
            }
        }

        val editedItem = EditedMediaItem.Builder(itemBuilder.build())
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
                    trySend(ExportState.Running(progressHolder.progress.coerceIn(1, 98), "Rendering hardware frames..."))
                }
                mainHandler.postDelayed(this, 200L)
            }
        }

        mainHandler.post {
            try {
                val encoderFactory = DefaultEncoderFactory.Builder(context)
                    .setEnableFallback(true)
                    .build()

                val t = Transformer.Builder(context)
                    .setVideoMimeType(config.codec.mimeType)
                    .setEncoderFactory(encoderFactory)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            finished = true
                            mainHandler.removeCallbacks(pollRunnable)
                            trySend(ExportState.Running(99, "Saving MP4 to Gallery..."))
                            // CRITICAL: Publish to MediaStore on Dispatchers.IO (never Main Thread!)
                            launch(Dispatchers.IO) {
                                val published = publishToMediaStore(tempFile, fileName)
                                tempFile.delete()
                                if (published != null) {
                                    trySend(ExportState.Completed("Movies/VyRox/$fileName", published.toString()))
                                } else {
                                    trySend(ExportState.Failed("Export rendered, but saving to MediaStore Gallery failed."))
                                }
                                close()
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            finished = true
                            mainHandler.removeCallbacks(pollRunnable)
                            tempFile.delete()
                            val detail = "${exportException.errorCodeName}: ${exportException.cause?.localizedMessage ?: exportException.localizedMessage ?: "Hardware encoder rejected config"}"
                            trySend(ExportState.Failed(detail))
                            close()
                        }
                    })
                    .build()
                transformer = t
                trySend(ExportState.Running(0, "Initializing hardware encoder..."))
                t.start(editedItem, tempFile.absolutePath)
                mainHandler.postDelayed(pollRunnable, 200L)
            } catch (e: Exception) {
                finished = true
                trySend(ExportState.Failed("Init error: ${e.localizedMessage ?: e.javaClass.simpleName}"))
                close()
            }
        }

        awaitClose {
            finished = true
            mainHandler.post {
                mainHandler.removeCallbacks(pollRunnable)
                runCatching { transformer?.cancel() }
                tempFile.delete()
            }
        }
    }

    private suspend fun publishToMediaStore(sourceFile: File, displayName: String): Uri? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return@withContext null
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/VyRox")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        runCatching {
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
