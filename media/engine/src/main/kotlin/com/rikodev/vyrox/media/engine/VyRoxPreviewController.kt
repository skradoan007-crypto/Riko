package com.rikodev.vyrox.media.engine

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.rikodev.vyrox.core.model.ColorGradeState
import com.rikodev.vyrox.core.model.VideoClip
import com.rikodev.vyrox.graphics.gl.VyRoxColorGlEffect
import java.util.concurrent.atomic.AtomicReference

/**
 * Live GPU-accelerated preview controller backed by Media3 ExoPlayer and
 * VyRoxColorGlEffect (identical shader math to VyRoxExportEngine).
 */
@OptIn(UnstableApi::class)
class VyRoxPreviewController(context: Context) {

    private val colorStateRef = AtomicReference(ColorGradeState())
    private val colorEffect = VyRoxColorGlEffect(colorStateRef)

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
        repeatMode = Player.REPEAT_MODE_ONE
        setVideoEffects(listOf(colorEffect))
    }

    private var loadedUri: String? = null
    private var loadedStartMs: Long = -1L
    private var loadedEndMs: Long = -1L

    fun loadClip(uriString: String, clip: VideoClip) {
        colorStateRef.set(clip.colorGrade)
        player.volume = if (clip.muted) 0f else clip.volume.coerceIn(0f, 1f)
        player.playbackParameters = PlaybackParameters(clip.speed.coerceIn(0.25f, 4f))

        val needsReload = loadedUri != uriString ||
            loadedStartMs != clip.sourceTrimStartMs ||
            loadedEndMs != clip.sourceTrimEndMs

        if (needsReload) {
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(uriString))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.sourceTrimStartMs.coerceAtLeast(0L))
                        .setEndPositionMs(clip.sourceTrimEndMs.coerceAtLeast(clip.sourceTrimStartMs + 250L))
                        .build()
                )
                .build()

            loadedUri = uriString
            loadedStartMs = clip.sourceTrimStartMs
            loadedEndMs = clip.sourceTrimEndMs

            player.setMediaItem(mediaItem)
            player.prepare()
        }
    }

    fun updateColorGrade(grade: ColorGradeState) {
        colorStateRef.set(grade)
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun release() {
        player.release()
    }
}
