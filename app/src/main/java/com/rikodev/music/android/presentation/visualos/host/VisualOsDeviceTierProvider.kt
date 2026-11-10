package com.rikodev.music.android.presentation.visualos.host

import android.app.Activity
import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.rikodev.music.android.presentation.visualos.runtime.VisualOsSystemState

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL OS — DEVICE TIER PROVIDER
//
// ডিভাইস ক্ষমতা সনাক্তকরণ এবং অ্যানিমেশন বাজেট নির্ধারণ।
//
// Device tiers অ্যানিমেশন জটিলতা নির্ধারণ করে:
//   - TIER_HIGH (120Hz, flagship): সম্পূর্ণ অ্যানিমেশন, সব ইফেক্ট
//   - TIER_MEDIUM (60Hz, mid-range): মান অ্যানিমেশন, কম particles
//   - TIER_LOW (60Hz, budget): সরলীকৃত অ্যানিমেশন, ন্যূনতম ইফেক্ট
//   - TIER_REDUCED_MOTION: System accessibility — সব tier এ সরলীকৃত
//
// সনাক্তকরণ: orientation, multi-window, refresh rate।
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ডিভাইস টায়ার ক্লাসিফিকেশন।
 * হার্ডওয়্যার ক্ষমতা অনুযায়ী অ্যানিমেশন বাজেট নির্ধারণ করে।
 */
enum class DeviceTier {
    /** ১২০Hz+ ফ্ল্যাগশিপ — সম্পূর্ণ অ্যানিমেশন, সব ইফেক্ট সক্রিয় */
    TIER_HIGH,

    /** ৬০Hz মিড-রেঞ্জ — মান অ্যানিমেশন, কম পার্টিকেল */
    TIER_MEDIUM,

    /** ৬০Hz বাজেট — সরলীকৃত অ্যানিমেশন, ন্যূনতম ইফেক্ট */
    TIER_LOW
}

/**
 * বর্তমান ডিভাইস টায়ার মনে রাখো এবং re-compose এ স্থিতিশীল রাখো।
 *
 * টায়ার নির্ধারণের নিয়ম:
 *   - ৯০Hz+ → TIER_HIGH
 *   - ৬০Hz এবং ৪GB+ RAM → TIER_MEDIUM
 *   - ৬০Hz এবং <4GB RAM → TIER_LOW
 *
 * এই মান ActivityManager.MemoryInfo এবং Display refreshRate থেকে আসে।
 */
@Composable
fun rememberDeviceTier(): DeviceTier {
    val context = LocalContext.current
    val activity = context as? Activity

    return remember {
        val display = activity?.windowManager?.defaultDisplay
        val refreshRate = display?.refreshRate?.toInt() ?: 60
        val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE)
            as? android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val totalMemMb = (memoryInfo.totalMem / (1024 * 1024)).toInt()

        when {
            refreshRate >= 90 -> DeviceTier.TIER_HIGH
            totalMemMb >= 4096 -> DeviceTier.TIER_MEDIUM
            else -> DeviceTier.TIER_LOW
        }
    }
}

/**
 * বর্তমান সিস্টেম state মনে রাখো — Visual OS এর জন্য প্রয়োজনীয়।
 *
 * সনাক্তকৃত তথ্য:
 *   - refresh rate (Display.refreshRate)
 *   - reduced motion (Settings.Global animator_duration_scale)
 *   - orientation (Configuration.orientation)
 *   - multi-window mode (Activity.isInMultiWindowMode)
 *
 * কনফিগারেশন পরিবর্তনে (orientation, multi-window) স্বয়ংক্রিয়ভাবে আপডেট হয়।
 */
@Composable
fun rememberSystemState(): VisualOsSystemState {
    val context = LocalContext.current
    val activity = context as? Activity

    // ── Refresh Rate সনাক্তকরণ ───────────────────────────────────────
    val refreshRateHz = remember {
        val display = activity?.windowManager?.defaultDisplay
        display?.refreshRate?.toInt() ?: 60
    }

    // ── Reduced Motion সনাক্তকরণ ────────────────────────────────────
    val reducedMotion = remember {
        Settings.Global.getInt(
            context.contentResolver,
            "animator_duration_scale",
            1
        ) == 0
    }

    // ── কনফিগারেশন পরিবর্তনে reactive update ───────────────────────
    // orientation এবং multi-window কনফিগারেশন পরিবর্তনে আপডেট হয়
    var currentConfig by remember {
        mutableStateOf(activity?.resources?.configuration)
    }

    DisposableEffect(activity) {
        val listener = object : android.content.ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                currentConfig = newConfig
            }
            override fun onLowMemory() { /* no-op */ }
        }
        activity?.registerComponentCallbacks(listener)
        onDispose {
            activity?.unregisterComponentCallbacks(listener)
        }
    }

    val isLandscape = currentConfig?.orientation == Configuration.ORIENTATION_LANDSCAPE

    // ── Multi-Window সনাক্তকরণ ──────────────────────────────────────
    val isMultiWindow = remember(activity) {
        activity?.isInMultiWindowMode ?: false
    }

    // ── Frame Budget হিসাব ───────────────────────────────────────────
    val frameBudgetMs = VisualOsSystemState.frameBudgetForHz(refreshRateHz)

    return remember(reducedMotion, refreshRateHz, isLandscape, isMultiWindow) {
        VisualOsSystemState(
            reducedMotion = reducedMotion,
            refreshRateHz = refreshRateHz,
            isLandscape = isLandscape,
            isMultiWindow = isMultiWindow,
            frameBudgetMs = frameBudgetMs
        )
    }
}
