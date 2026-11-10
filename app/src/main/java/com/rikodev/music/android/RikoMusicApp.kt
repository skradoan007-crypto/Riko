package com.rikodev.music.android

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RikoMusicApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // Plan 1.2 crossfade policy: NO global crossfade — dense lists must
            // bind instantly during flings. Hero placements opt into a short
            // fade via ArtworkRequestFactory (HERO_CROSSFADE_MS).
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
