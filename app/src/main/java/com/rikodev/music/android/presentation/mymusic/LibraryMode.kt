package com.rikodev.music.android.presentation.mymusic

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — LIBRARY MODE (SM-UI-SPEC-001 v2.0, Section 3.6, D4)
//
// Content mode switcher for My Music — NOT a navigation system (spec §4).
// Modes per spec 3.6: Suggested / Folders / Downloads / New (D4).
// Downloads EXCLUDED from real content: no real data source exists (spec §1).
// ─────────────────────────────────────────────────────────────────────────────

enum class LibraryMode(val label: String) {
    Suggested("Suggested"),
    Folders("Folders"),
    Downloads("Downloads"),  // spec 3.6: present in tabs but empty content
    New("New")              // D4: 4th tab label finalized as "New"
}