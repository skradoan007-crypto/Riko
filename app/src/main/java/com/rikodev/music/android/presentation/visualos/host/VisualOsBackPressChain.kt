package com.rikodev.music.android.presentation.visualos.host

import com.rikodev.music.android.presentation.visualos.runtime.VisualOsState
import com.rikodev.music.android.presentation.visualos.runtime.VisualOsEvent

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL OS — BACK PRESS CHAIN
//
// ব্যাক প্রেস অগ্রাধিকার চেইন:
//   ১. Overlay বন্ধ করো (যদি সক্রিয় থাকে)
//   ২. Panel বন্ধ করো (যদি খোলা থাকে)
//   ৩. Focus scene pop করো (যদি থাকে)
//   ৪. NavController কে প্রপাগেট করো (system default)
//
// এই চেইন Visual OS back press handling এর ক্রম নির্ধারণ করে।
// Rule #83: Coordinator Executes — চেইন শুধু dispatch করে, execute করে না।
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ব্যাক প্রেস অগ্রাধিকার চেইন।
 *
 * @param state বর্তমান Visual OS state
 * @param coordinator কোঅর্ডিনেটর যেখানে event dispatch হবে
 * @return true যদি ব্যাক প্রেস Visual OS দ্বারা গ্রাস করা হয়, false যদি NavController কে পাঠাতে হয়
 */
object VisualOsBackPressChain {

    /**
     * ব্যাক প্রেস হ্যান্ডেল করো — অগ্রাধিকার চেইন অনুসারে।
     *
     * ক্রম:
     *   ১. Overlay → সবচেয়ে উপরের ট্রানজিয়েন্ট স্তর, প্রথমে বন্ধ হবে
     *   ২. Panel → বটম শিট/প্যানেল, দ্বিতীয়তে বন্ধ হবে
     *   ৩. Focus Scene → ডিটেইল স্ক্রিন, তৃতীয়তে pop হবে
     *   ৪. System → কিছুই গ্রাস করা না গেলে NavController কে দাও
     */
    fun handleBackPress(
        state: VisualOsState,
        coordinator: VisualOsCoordinator
    ): Boolean {
        // ── ধাপ ১: Overlay চেক ──────────────────────────────────────
        // Overlay সবচেয়ে উচ্চ অগ্রাধিকার — ট্রানজিয়েন্ট UI
        if (state.isOverlayShowing) {
            coordinator.dispatch(VisualOsEvent.OverlayHidden(state.overlayState.type))
            return true
        }

        // ── ধাপ ২: Panel চেক ───────────────────────────────────────
        // Panel (EQ, Queue, Info, Timer) দ্বিতীয় অগ্রাধিকার
        val panel = state.activePanel
        if (panel != null) {
            coordinator.dispatch(VisualOsEvent.PanelDismissed(panel.id))
            return true
        }

        // ── ধাপ ৩: Focus Scene চেক ─────────────────────────────────
        // Focus scene (যেমন NowPlaying, PlaylistDetail) pop করো
        val focusScene = state.focusScene
        if (focusScene != null) {
            coordinator.dispatch(VisualOsEvent.LayerPopped(focusScene.id))
            return true
        }

        // ── ধাপ ৪: গ্রাস করা হয়নি — NavController কে দাও
        return false
    }
}
