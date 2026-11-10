package com.rikodev.music.android.dna.settings

import android.content.Context
import android.content.SharedPreferences
import com.rikodev.music.android.dna.render.MdnaTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — SETTINGS (P2)
//
// The user's chosen template, remembered across launches. Deliberately its own
// tiny SharedPreferences file (not VisualizerPrefs/DataStore): MDNA ships without
// a migration of the shared visualizer settings. A removed/renamed template falls
// back to the signature scene instead of failing.
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class MdnaSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var template: MdnaTemplate
        get() = MdnaTemplate.fromName(prefs.getString(KEY_TEMPLATE, null))
        set(value) {
            prefs.edit().putString(KEY_TEMPLATE, value.name).apply()
        }

    private companion object {
        const val PREFS_NAME = "mdna_settings"
        const val KEY_TEMPLATE = "template"
    }
}
