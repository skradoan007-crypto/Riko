package com.rikodev.vyrox.media.engine

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.rikodev.vyrox.core.model.ColorGradeState
import com.rikodev.vyrox.core.model.VideoClip
import com.rikodev.vyrox.graphics.gl.VyRoxColorGlEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference

sealed interface PreviewStatus {
    data object Idle : PreviewStatus
    data object Loading : PreviewStatus
    data object Ready : PreviewStatus
    data class Error(val message: String) : PreviewStatus
}

@OptIn(UnstableApi::class)
class VyRoxPreviewController(context: Context) {

    private val colorStateRef = AtomicReference(ColorGradeState())
    private val colorEffect = VyRoxColorGlEffect(colorStateRef, allowNoOp = false)

    private val _status = MutableStateFlow<PreviewStatus>(PreviewStatus.Idle)
    val status: StateFlow<PreviewStatus> = _status.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
        repeatMode = Player.REPEAT_MODE_ONE
        setVideoEffects(listOf(colorEffect))
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> _status.value = PreviewStatus.Loading
                    Player.STATE_READY -> _status.value = PreviewStatus.Ready
                    else -> Unit
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _status.value = PreviewStatus.Error(
                    error.errorCodeName + ": " + (error.cause?.localizedMessage ?: error.localizedMessage ?: "Video decode failed")
                )
            }
        })
    }

    private var loadedUri: String? = null
    private var loadedStartMs: Long = -1L
    private var loadedEndMs: Long = -1L
    private var lastFrameNudgeMs: Long = 0L

    fun loadClip(uriString: String, clip: VideoClip, autoPlay: Boolean = true) {
        colorStateRef.set(clip.colorGrade)
        player.volume = if (clip.muted) 0f else clip.volume.coerceIn(0f, 1f)
        player.playbackParameters = PlaybackParameters(clip.speed.coerceIn(0.25f, 4f))

        val safeStart = clip.sourceTrimStartMs.coerceAtLeast(0L)
        val safeEnd = clip.sourceTrimEndMs.coerceAtLeast(safeStart + 250L)

        val needsReload = loadedUri != uriString ||
            loadedStartMs != safeStart ||
            loadedEndMs != safeEnd

        if (needsReload) {
            _status.value = PreviewStatus.Loading
            val builder = MediaItem.Builder().setUri(Uri.parse(uriString))
            val isTrimmed = safeStart > 0L || (clip.sourceDurationMs > 0L && safeEnd < clip.sourceDurationMs - 100L)
            if (isTrimmed) {
                builder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(safeStart)
                        .setEndPositionMs(safeEnd)
                        .build()
                )
            }

            loadedUri = uriString
            loadedStartMs = safeStart
            loadedEndMs = safeEnd

            player.stop()
            player.clearMediaItems()
            player.setMediaItem(builder.build())
            player.prepare()
            player.seekTo(0L)
            player.playWhenReady = autoPlay
        }
    }

    /**
     * Updates shader uniforms immediately. When paused, nudges the decoder
     * (rate-limited to ~20fps) so the user sees live color grading even on a still frame.
     */
    fun updateColorGrade(grade: ColorGradeState) {
        colorStateRef.set(grade)
        if (!player.isPlaying && player.playbackState == Player.STATE_READY) {
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastFrameNudgeMs >= 50L) {
                lastFrameNudgeMs = now
                player.seekTo(player.currentPosition)
            }
        }
    }

    fun pauseAndFreeDecoder() {
        player.pause()
        player.stop()
        loadedUri = null
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        val dur = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
        player.seekTo(positionMs.coerceIn(0L, dur))
    }

    fun release() {
        player.release()
    }
}
