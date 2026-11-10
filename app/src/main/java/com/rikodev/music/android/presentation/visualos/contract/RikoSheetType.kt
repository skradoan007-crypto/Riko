package com.rikodev.music.android.presentation.visualos.contract

/**
 * The app's modal bottom-sheet contract: the four sheets Visual OS scenes can
 * map onto (EQ, Queue, Song Info, Sleep Timer).
 *
 * Moved here from `VisualOsSceneMap.kt` (Phase 4, G-18): the enum is a
 * contract shared by the scene map and the nav bridge, not part of the map
 * itself, so it lives with the other Visual OS contracts.
 */
enum class RikoSheetType {
    /** Audio-effect panel — opened from the Now Playing surface. */
    EQUALIZER,

    /** Play-queue panel — shows the upcoming playback order. */
    QUEUE,

    /** Song metadata panel — album, year, path and file details. */
    SONG_INFO,

    /** Sleep-timer panel — auto-stop countdown with preset durations. */
    SLEEP_TIMER
}
