package com.rikodev.sync.nexmesh.android

import android.app.Application
import com.rikodev.sync.nexmesh.android.di.AppContainer

class NexMeshApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
