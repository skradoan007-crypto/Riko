package com.rikodev.ibd3d.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rikodev.ibd3d.android.ui.PanelApp
import com.rikodev.ibd3d.android.updates.PlayUpdates
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var updates: PlayUpdates

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
        )
        updates = PlayUpdates(this)
        setContent { PanelApp(updates) }
    }

    override fun onStart() {
        super.onStart()
        com.rikodev.ibd3d.android.notifications.CopyReminder.foreground(this)
    }
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) com.rikodev.ibd3d.android.notifications.CopyReminder.background(applicationContext)
    }
    override fun onResume() {
        super.onResume()
        if (::updates.isInitialized) updates.resume()
    }

    override fun onDestroy() {
        if (::updates.isInitialized) updates.close()
        super.onDestroy()
    }
}
