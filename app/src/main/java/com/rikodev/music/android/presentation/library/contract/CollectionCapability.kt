/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 7 / 15 — CollectionCapability.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * A collection's *capabilities* are the set of features the user is
 * allowed to invoke on it. Capabilities are a CONTRACT — if the
 * contract says `canShare = false`, the UI MUST NOT render a share
 * button, even if the underlying data is technically shareable.
 *
 * Capabilities are NOT the same as the supportsX flags on
 * CollectionType. supportsX says "the engine supports this"; canX
 * says "the user is allowed to do this here". They usually align,
 * but not always — e.g. ALL_SONGS supports sort but the user cannot
 * rename it.
 *
 * RULE #36 — Contract Never Thinks
 * Capabilities are pure declarations of what is permitted.
 *
 * The runtime (B2) and the UI (B4) both read capabilities to decide
 * what to render. No business logic in this file.
 */

package com.rikodev.music.android.presentation.library.contract

/**
 * The complete capability set of a RIKO collection.
 *
 * Every field is a feature flag — boolean, no logic. The runtime
 * is free to OR these against user settings to produce a final
 * permission set, but the contract only declares what the
 * collection natively supports.
 */
data class CollectionCapability(
    // ── Destructive / mutative ──────────────────────────────────
    val canRefresh: Boolean,
    val canPin: Boolean,
    val canShare: Boolean,
    val canRename: Boolean,
    val canDelete: Boolean,
    val canExport: Boolean,
    val canImport: Boolean,

    // ── Playback interactions ───────────────────────────────────
    val canShuffle: Boolean,
    val canPlayAll: Boolean,
    val canPlayNext: Boolean,
    val canAddToQueue: Boolean,
    val canAddToPlaylist: Boolean,

    // ── Organizational ──────────────────────────────────────────
    val canSort: Boolean,
    val canSearch: Boolean,
    val canFilter: Boolean,
    val canMultiSelect: Boolean,
    val canDrag: Boolean,

    // ── Visual / motion ─────────────────────────────────────────
    val supportsAnimation: Boolean,
    val supportsDock: Boolean,
    val supportsWave: Boolean,
    val supportsGlass: Boolean,
    val supportsMiniPlayer: Boolean,
    val supportsContextMenu: Boolean,
    val supportsPreview: Boolean,

    // ── Future-ready ────────────────────────────────────────────
    val supportsLyrics: Boolean,
    val supportsSleepTimer: Boolean,
    val supportsEqualizer: Boolean,
    val supportsCrossfade: Boolean,
) {

    companion object {

        /** Used when no explicit capability is declared — everything off. */
        val NONE = CollectionCapability(
            canRefresh = false, canPin = false, canShare = false,
            canRename = false, canDelete = false, canExport = false,
            canImport = false,
            canShuffle = false, canPlayAll = false, canPlayNext = false,
            canAddToQueue = false, canAddToPlaylist = false,
            canSort = false, canSearch = false, canFilter = false,
            canMultiSelect = false, canDrag = false,
            supportsAnimation = false, supportsDock = false,
            supportsWave = false, supportsGlass = false,
            supportsMiniPlayer = false, supportsContextMenu = false,
            supportsPreview = false,
            supportsLyrics = false, supportsSleepTimer = false,
            supportsEqualizer = false, supportsCrossfade = false,
        )

        /**
         * Capability preset for smart / engine-generated collections
         * (HiddenGems, Discovery, ContinueListening, etc.).
         *
         * These collections can be refreshed, shared, queued, and
         * previewed, but not renamed or deleted.
         */
        val SMART = NONE.copy(
            canRefresh = true,
            canShare = true,
            canShuffle = true,
            canPlayAll = true,
            canPlayNext = true,
            canAddToQueue = true,
            canAddToPlaylist = true,
            canSort = true,
            canMultiSelect = true,
            supportsAnimation = true,
            supportsDock = true,
            supportsWave = true,
            supportsGlass = true,
            supportsMiniPlayer = true,
            supportsContextMenu = true,
            supportsPreview = true,
            supportsLyrics = true,
            supportsSleepTimer = true,
            supportsEqualizer = true,
            supportsCrossfade = true,
        )

        /**
         * Capability preset for user-owned collections (Favorites,
         * Pinned, Playlists). User has full organizational control.
         */
        val USER_OWNED = SMART.copy(
            canPin = true,
            canRename = true,
            canDelete = true,
            canExport = true,
            canImport = true,
            canFilter = true,
            canDrag = true,
            canSearch = true,
        )

        /**
         * Capability preset for structural library views (All Songs,
         * All Albums, All Artists, All Folders). Browsing-only —
         * no rename, no delete, no share.
         */
        val LIBRARY_STRUCTURE = SMART.copy(
            canRefresh = false,
            canShare = false,
            canRename = false,
            canDelete = false,
            canExport = false,
            canImport = false,
            canPin = false,
            supportsPreview = false,
        )

        /**
         * Capability preset for history collections (Full History,
         * Time Travel). Read-only — the user cannot modify.
         */
        val HISTORY = SMART.copy(
            canRefresh = false,
            canShare = false,
            canRename = false,
            canDelete = false,
            canPin = false,
            canExport = true,
        )

        /**
         * Capability preset for diagnostic / developer-only
         * collections. No playback actions — view only.
         */
        val DIAGNOSTIC = NONE.copy(
            canRefresh = true,
            canSort = true,
            canSearch = true,
            canFilter = true,
            supportsContextMenu = true,
        )

        /**
         * Capability preset for experimental collections behind a
         * flag. Same as SMART but with extra safeguards off.
         */
        val EXPERIMENTAL = SMART.copy(
            canDelete = false,
            canExport = false,
            canImport = false,
            canRename = false,
            supportsCrossfade = false,
        )
    }
}
