package com.rikodev.ibd3d.android.di

import android.content.Context
import com.rikodev.ibd3d.core.data.PanelRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun repo(@ApplicationContext c: Context) = PanelRepository(c)
}
