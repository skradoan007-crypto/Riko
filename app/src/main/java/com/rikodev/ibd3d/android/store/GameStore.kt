package com.rikodev.ibd3d.android.store

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object GameStore {
    const val GAME_PACKAGE = "com.Rohit.IndianBikes"
    const val WEB_URL = "https://play.google.com/store/apps/details?id=$GAME_PACKAGE"

    fun open(context: Context): Boolean = open { context.startActivity(it) }

    internal fun open(launch: (Intent) -> Unit): Boolean {
        val store =
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GAME_PACKAGE"))
                .setPackage("com.android.vending")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tryLaunch(store, launch)) return true
        val browser =
            Intent(Intent.ACTION_VIEW, Uri.parse(WEB_URL))
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return tryLaunch(browser, launch)
    }

    private fun tryLaunch(intent: Intent, launch: (Intent) -> Unit): Boolean =
        try {
            launch(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
}
