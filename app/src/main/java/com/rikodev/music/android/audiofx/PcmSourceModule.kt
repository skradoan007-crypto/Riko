package com.rikodev.music.android.audiofx

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PcmSourceModule {
    @Binds abstract fun bindPcmSource(source: AndroidPcmSource): PcmSource
}
