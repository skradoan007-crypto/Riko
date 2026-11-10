package com.rikodev.music.android.di

import android.content.ContentResolver
import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.rikodev.music.android.audiofx.PcmAudioProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import androidx.compose.animation.core.Animatable

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    // PcmAudioProcessor is itself a Hilt @Singleton (see PcmAudioProcessor.kt),
    // so this is the SAME instance the UI/ViewModel observes — Hilt supplies
    // it here automatically since it has an @Inject constructor.
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        pcmAudioProcessor: PcmAudioProcessor
    ): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Real PCM tap — sits directly in ExoPlayer's own audio pipeline
        // (Decoder → PcmAudioProcessor → AudioSink → speaker). This is our
        // OWN decoded audio, not a system-level capture API, so it needs no
        // RECORD_AUDIO permission and isn't subject to the OEM-level
        // android.media.audiofx.Visualizer restrictions that were producing
        // zero callbacks. Official Media3 pattern:
        // https://developer.android.com/media/implement/editing-app
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(pcmAudioProcessor))
                    .build()
            }
        }

        return ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
    }
}
