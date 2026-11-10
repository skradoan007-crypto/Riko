/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 — Library Rendering UI
 * Phase D — Composition / Performance / Validator / Preview
 *
 * File D4 / D4 — LibraryPreviewKit.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryPreviewKit] is the Living Documentation of the Library screen.
 *
 * A new developer opens this file in Android Studio's Preview pane and
 * immediately sees every meaningful state of the Library: morning, night,
 * AMOLED, 50K songs, 0 songs, error, loading, reduced-motion, tablet,
 * foldable. They don't need to read the code — the previews tell the
 * story.
 *
 * ## Rule #77 — Preview Is Documentation
 *
 * Each `@Preview` annotation includes:
 *   - `name` — human-readable (profile + state + scenario)
 *   - `showBackground = true` + `backgroundColor = 0xFF050505`
 *   - `device` parameter (when form-factor specific)
 *   - `widthDp` / `heightDp` (when form-factor specific)
 *
 * Coverage matrix:
 *   - 8 profiles (DEFAULT, AMOLED, CINEMATIC, COMPACT, LOW_END, HIGH_END,
 *     REDUCED_MOTION, HIGH_CONTRAST)
 *   - 8 phases (COLD_START, LOADING, PARTIAL_READY, READY, EMPTY, ERROR,
 *     PERMISSION_REQUIRED, SELECTION_MODE)
 *   - 4 content scales (0, 50, 500, 50000 songs)
 *   - 4 form factors (phone-portrait, phone-landscape, tablet, foldable)
 *   - 2 times of day (morning, night)
 *
 * ## What this file is NOT
 *
 * This file is NOT a test suite. It doesn't assert anything. It just
 * renders the screen in various states so a human can verify visually.
 *
 * This file is NOT a fixture. Fixtures live in `test/` and are run by
 * the test runner. Previews live in `main/` and are rendered by the
 * IDE.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #60  Screen State Is Already Renderable — previews use real state shape
 * #62  Never Rebuild Existing Systems — previews call LibraryComposition
 * #77  Preview Is Documentation — see above
 *
 * © RIKO Experience OS — Documentation Tier
 */

package com.rikodev.music.android.presentation.library.screen.composition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.rikodev.music.android.core.experience.wave.WaveState
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitution
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitutionProfile
import com.rikodev.music.android.presentation.library.screen.contract.LibraryDockState
import com.rikodev.music.android.presentation.library.screen.contract.LibraryEmptyStateKind
import com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination
import com.rikodev.music.android.presentation.library.screen.contract.LibraryScreenOrientation
import com.rikodev.music.android.presentation.library.screen.contract.LibraryTopBarState
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryDockRuntimeState
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryEmptyRuntimeState
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryHeroRuntimeState
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryNavigationRuntimeState
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenState
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenPhase
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScrollRuntimeState
import com.rikodev.music.android.presentation.library.screen.runtime.LibrarySectionRuntimeState
import com.rikodev.music.android.presentation.library.screen.runtime.LibrarySelectionRuntimeState
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryTopBarRuntimeState
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.Pause

// ═══════════════════════════════════════════════════════════════════════════════════════
// PREVIEW STATE FACTORIES
//
// Pure functions that produce LibraryScreenState instances for previews.
// Each factory is named for the scenario it produces — no magic numbers,
// no ambiguous "PreviewState1" / "PreviewState2".
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * The default preview constitution — DEFAULT profile.
 *
 * Used as the base for most previews. Other profiles are produced by
 * calling [LibraryConstitution.forProfile].
 */
private val PREVIEW_CONSTITUTION_DEFAULT: LibraryConstitution = LibraryConstitution.DEFAULT

/**
 * Resolves a preview constitution for a profile.
 *
 * Uses [LibraryConstitution.forProfile] — Rule #62, never rebuild.
 */
private fun previewConstitution(
    profile: LibraryConstitutionProfile,
): LibraryConstitution = LibraryConstitution.forProfile(profile)

/**
 * A preview WaveState — IDLE (no music) or ACTIVE (music playing).
 *
 * Used by the wave ambient layer. Most previews use IDLE; only the
 * "music playing" previews use ACTIVE.
 */
private val PREVIEW_WAVE_IDLE: WaveState = WaveState.IDLE

/**
 * Produces a COLD_START state for previews.
 *
 * Cold start = constitution loaded, no state hydrated yet. The screen
 * shows the loading shimmer.
 */
internal fun previewStateColdStart(
    profile: LibraryConstitutionProfile = LibraryConstitutionProfile.DEFAULT,
): LibraryScreenState = LibraryScreenState.coldStart(previewConstitution(profile))

/**
 * Produces a LOADING state for previews.
 *
 * Loading = engine is producing the first batch. The screen shows
 * the shimmer skeletons.
 */
internal fun previewStateLoading(
    profile: LibraryConstitutionProfile = LibraryConstitutionProfile.DEFAULT,
): LibraryScreenState = LibraryScreenState.coldStart(previewConstitution(profile)).copy(
    phase = LibraryScreenPhase.LOADING,
    lastUpdatedMs = 1L,
)

/**
 * Produces a READY state with the given number of songs.
 *
 * Ready = all sections ready. The screen shows the Hero + sections.
 *
 * @param profile     The constitution profile.
 * @param songCount   Total songs in the library. Drives section sizes.
 * @param isPlaying   True if music is currently playing (shows wave + dock).
 * @param hasHero     True if the Hero should be visible.
 */
internal fun previewStateReady(
    profile: LibraryConstitutionProfile = LibraryConstitutionProfile.DEFAULT,
    songCount: Int = 500,
    isPlaying: Boolean = false,
    hasHero: Boolean = true,
): LibraryScreenState {
    val constitution = previewConstitution(profile)
    val sections = previewSections(songCount)
    return LibraryScreenState.coldStart(constitution).copy(
        phase = LibraryScreenPhase.READY,
        lastUpdatedMs = System.currentTimeMillis(),
        hero = if (hasHero) previewHero() else LibraryHeroRuntimeState.EMPTY,
        sections = sections,
        dock = if (isPlaying) previewDockPlaying() else previewDockIdle(),
        scroll = LibraryScrollRuntimeState.INITIAL,
        navigation = LibraryNavigationRuntimeState.IDLE,
        emptyState = LibraryEmptyRuntimeState.HIDDEN,
        errorMessage = null,
        activeCollectionStableKey = sections.firstOrNull()?.stableKey,
    )
}

/**
 * Produces a PARTIAL_READY state — some sections ready, others loading.
 *
 * Useful for previewing the "still hydrating" feel.
 */
internal fun previewStatePartialReady(
    profile: LibraryConstitutionProfile = LibraryConstitutionProfile.DEFAULT,
    songCount: Int = 500,
): LibraryScreenState = previewStateReady(profile, songCount).copy(
    phase = LibraryScreenPhase.PARTIAL_READY,
    sections = previewStateReady(profile, songCount).sections.mapIndexed { index, section ->
        if (index < 2) section else section.copy(isRefreshing = true)
    },
)

/**
 * Produces an EMPTY state with the given kind.
 *
 * Empty = no content. The screen shows the personality-driven empty
 * state view (FIRST_LAUNCH, ALL_DELETED, NO_COLLECTIONS, etc.).
 */
internal fun previewStateEmpty(
    profile: LibraryConstitutionProfile = LibraryConstitutionProfile.DEFAULT,
    kind: LibraryEmptyStateKind = LibraryEmptyStateKind.FIRST_LAUNCH,
): LibraryScreenState {
    val constitution = previewConstitution(profile)
    return LibraryScreenState.coldStart(constitution).copy(
        phase = LibraryScreenPhase.EMPTY,
        lastUpdatedMs = System.currentTimeMillis(),
        sections = emptyList(),
        emptyState = LibraryEmptyRuntimeState(
            kind = kind,
            isVisible = true,
            replacesContent = true,
        ),
    )
}

/**
 * Produces an ERROR state.
 *
 * Error = engine failed. The screen shows the LOAD_FAILED empty state.
 */
internal fun previewStateError(
    profile: LibraryConstitutionProfile = LibraryConstitutionProfile.DEFAULT,
    errorMessage: String = "Could not load Library. Tap retry to try again.",
): LibraryScreenState = LibraryScreenState.coldStart(previewConstitution(profile)).copy(
    phase = LibraryScreenPhase.ERROR,
    lastUpdatedMs = System.currentTimeMillis(),
    sections = emptyList(),
    emptyState = LibraryEmptyRuntimeState(
        kind = LibraryEmptyStateKind.LOAD_FAILED,
        isVisible = true,
        replacesContent = true,
    ),
    errorMessage = errorMessage,
)

/**
 * Produces a PERMISSION_REQUIRED state.
 *
 * Permission required = no storage permission. The screen shows the
 * PERMISSION_DENIED empty state with a "Grant permission" action.
 */
internal fun previewStatePermissionRequired(
    profile: LibraryConstitutionProfile = LibraryConstitutionProfile.DEFAULT,
): LibraryScreenState = LibraryScreenState.coldStart(previewConstitution(profile)).copy(
    phase = LibraryScreenPhase.PERMISSION_REQUIRED,
    lastUpdatedMs = System.currentTimeMillis(),
    sections = emptyList(),
    emptyState = LibraryEmptyRuntimeState(
        kind = LibraryEmptyStateKind.PERMISSION_DENIED,
        isVisible = true,
        replacesContent = true,
    ),
)

/**
 * Produces a SELECTION_MODE state.
 *
 * Selection mode = user is multi-selecting items. The screen shows
 * the Hero + sections, with selection highlights on a few items.
 */
internal fun previewStateSelectionMode(
    profile: LibraryConstitutionProfile = LibraryConstitutionProfile.DEFAULT,
    songCount: Int = 500,
): LibraryScreenState = previewStateReady(profile, songCount).copy(
    phase = LibraryScreenPhase.SELECTION_MODE,
    selection = LibrarySelectionRuntimeState(
        isActive = true,
        selectedItemIds = setOf("card.0", "card.1", "card.2"),
        selectionLimit = 50,
        anchorSectionId = "albums",
    ),
)

// ═══════════════════════════════════════════════════════════════════════════════════════
// PREVIEW SUB-STATE FACTORIES
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Produces a Hero runtime state for previews.
 */
private fun previewHero(): LibraryHeroRuntimeState = LibraryHeroRuntimeState(
    isVisible = true,
    collectionStableKey = "continue_listening",
    primaryItemId = "hero.continue",
    primaryTitle = "Continue Listening",
    primarySubtitle = "Pick up where you left off",
    artworkKey = "artwork.hero",
    accentColorHex = "#22D3EE",
    isDismissed = false,
)

/**
 * Produces an idle Dock runtime state (no track loaded).
 */
private fun previewDockIdle(): LibraryDockRuntimeState = LibraryDockRuntimeState.EMPTY

/**
 * Produces a playing Dock runtime state (track loaded, playing).
 */
private fun previewDockPlaying(): LibraryDockRuntimeState = LibraryDockRuntimeState(
    state = LibraryDockState.COLLAPSED,
    currentTrackId = "track.bohemian_rhapsody",
    trackTitle = "Bohemian Rhapsody",
    trackArtist = "Queen",
    isPlaying = true,
    progressFraction = 0.42f,
    artworkKey = "artwork.bohemian_rhapsody",
    accentColorHex = "#FF8A65",
)

/**
 * Produces a list of preview sections based on song count.
 *
 * The sections are realistic — Continue Listening (curated), Albums
 * (catalog), Artists (catalog), Folders (catalog). The item counts
 * scale with the total song count.
 */
private fun previewSections(songCount: Int): List<LibrarySectionRuntimeState> {
    if (songCount <= 0) return emptyList()
    val albumCount = (songCount / 12).coerceAtLeast(1).coerceAtMost(200)
    val artistCount = (songCount / 18).coerceAtLeast(1).coerceAtMost(100)
    val folderCount = (songCount / 50).coerceAtLeast(1).coerceAtMost(20)
    return listOf(
        LibrarySectionRuntimeState(
            sectionId = "continue_listening",
            stableKey = "continue_listening",
            isVisible = true,
            isCollapsed = false,
            isRefreshing = false,
            itemCount = 6,
            totalCount = 6,
            firstVisibleItemIndex = 0,
            lastVisibleItemIndex = 3,
        ),
        LibrarySectionRuntimeState(
            sectionId = "albums",
            stableKey = "albums",
            isVisible = true,
            isCollapsed = false,
            isRefreshing = false,
            itemCount = albumCount.coerceAtMost(6),
            totalCount = albumCount,
            firstVisibleItemIndex = 0,
            lastVisibleItemIndex = 2,
        ),
        LibrarySectionRuntimeState(
            sectionId = "artists",
            stableKey = "artists",
            isVisible = true,
            isCollapsed = false,
            isRefreshing = false,
            itemCount = artistCount.coerceAtMost(6),
            totalCount = artistCount,
            firstVisibleItemIndex = 0,
            lastVisibleItemIndex = 2,
        ),
        LibrarySectionRuntimeState(
            sectionId = "folders",
            stableKey = "folders",
            isVisible = true,
            isCollapsed = false,
            isRefreshing = false,
            itemCount = folderCount.coerceAtMost(6),
            totalCount = folderCount,
            firstVisibleItemIndex = 0,
            lastVisibleItemIndex = 2,
        ),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// PREVIEW FRAME — the wrapper that hosts LibraryComposition
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * The preview frame — wraps [LibraryComposition] with preview-friendly
 * defaults (Material Icons, default artwork placeholders, no-op callbacks).
 *
 * Rule #77 — every `@Preview` composable in this file uses this frame.
 * The frame ensures consistency: same icons, same artwork slots, same
 * callback behavior across all previews.
 *
 * @param profile     The constitution profile to preview.
 * @param state       The Library screen state to preview.
 * @param waveState   The Wave Engine state (default: IDLE).
 * @param modifier    Compose modifier.
 */
@Composable
internal fun LibraryPreviewFrame(
    profile: LibraryConstitutionProfile,
    state: LibraryScreenState,
    waveState: WaveState = PREVIEW_WAVE_IDLE,
    modifier: Modifier = Modifier,
) {
    val constitution = previewConstitution(profile)
    LibraryComposition(
        constitution = constitution,
        state = state,
        waveState = waveState,
        modifier = modifier,
        playPauseIcon = Icons.Filled.PlayArrow,
        nextIcon = Icons.Filled.SkipNext,
        // Use default artwork placeholders from Phase B.
        // heroArtwork, dockArtwork, cardArtwork — all default.
        // All callbacks are no-ops in preview.
        onHeroTap = null,
        onSectionTap = null,
        onCardTap = null,
        onDockTap = null,
        onPlayPauseTap = null,
        onNextTap = null,
        onEmptyAction = null,
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// PROFILE PREVIEWS — 8 profiles × READY state
// ═══════════════════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Profile — DEFAULT — Ready — 500 songs",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewProfileDefaultReady500() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.DEFAULT,
            songCount = 500,
        ),
    )
}

@Preview(
    name = "Profile — AMOLED — Ready — 500 songs",
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun PreviewProfileAmoledReady500() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.AMOLED,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.AMOLED,
            songCount = 500,
        ),
    )
}

@Preview(
    name = "Profile — CINEMATIC — Ready — 500 songs",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewProfileCinematicReady500() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.CINEMATIC,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.CINEMATIC,
            songCount = 500,
        ),
    )
}

@Preview(
    name = "Profile — COMPACT — Ready — 500 songs (landscape)",
    showBackground = true,
    backgroundColor = 0xFF050505,
    widthDp = 800,
    heightDp = 360,
)
@Composable
private fun PreviewProfileCompactReady500() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.COMPACT,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.COMPACT,
            songCount = 500,
        ).copy(
            orientation = LibraryScreenOrientation.LANDSCAPE,
        ),
    )
}

@Preview(
    name = "Profile — LOW_END — Ready — 500 songs",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewProfileLowEndReady500() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.LOW_END,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.LOW_END,
            songCount = 500,
        ),
    )
}

@Preview(
    name = "Profile — HIGH_END — Ready — 500 songs",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewProfileHighEndReady500() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.HIGH_END,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.HIGH_END,
            songCount = 500,
        ),
    )
}

@Preview(
    name = "Profile — REDUCED_MOTION — Ready — 500 songs (accessibility)",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewProfileReducedMotionReady500() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.REDUCED_MOTION,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.REDUCED_MOTION,
            songCount = 500,
        ),
    )
}

@Preview(
    name = "Profile — HIGH_CONTRAST — Ready — 500 songs (accessibility)",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewProfileHighContrastReady500() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.HIGH_CONTRAST,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.HIGH_CONTRAST,
            songCount = 500,
        ),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// PHASE PREVIEWS — 8 phases × DEFAULT profile
// ═══════════════════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Phase — COLD_START — DEFAULT",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewPhaseColdStartDefault() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateColdStart(LibraryConstitutionProfile.DEFAULT),
    )
}

@Preview(
    name = "Phase — LOADING — DEFAULT",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewPhaseLoadingDefault() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateLoading(LibraryConstitutionProfile.DEFAULT),
    )
}

@Preview(
    name = "Phase — PARTIAL_READY — DEFAULT — 500 songs",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewPhasePartialReadyDefault500() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStatePartialReady(
            profile = LibraryConstitutionProfile.DEFAULT,
            songCount = 500,
        ),
    )
}

@Preview(
    name = "Phase — READY — DEFAULT — 500 songs",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewPhaseReadyDefault500() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.DEFAULT,
            songCount = 500,
        ),
    )
}

@Preview(
    name = "Phase — EMPTY — DEFAULT — First Launch",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewPhaseEmptyDefaultFirstLaunch() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateEmpty(
            profile = LibraryConstitutionProfile.DEFAULT,
            kind = LibraryEmptyStateKind.FIRST_LAUNCH,
        ),
    )
}

@Preview(
    name = "Phase — ERROR — DEFAULT — Load Failed",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewPhaseErrorDefault() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateError(LibraryConstitutionProfile.DEFAULT),
    )
}

@Preview(
    name = "Phase — PERMISSION_REQUIRED — DEFAULT",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewPhasePermissionRequiredDefault() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStatePermissionRequired(LibraryConstitutionProfile.DEFAULT),
    )
}

@Preview(
    name = "Phase — SELECTION_MODE — DEFAULT — 3 items selected",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewPhaseSelectionModeDefault() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateSelectionMode(
            profile = LibraryConstitutionProfile.DEFAULT,
            songCount = 500,
        ),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// CONTENT SCALE PREVIEWS — 4 scales × DEFAULT profile, READY phase
// ═══════════════════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Content Scale — 0 songs — DEFAULT — Empty (First Launch)",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewContentScale0Songs() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateEmpty(
            profile = LibraryConstitutionProfile.DEFAULT,
            kind = LibraryEmptyStateKind.FIRST_LAUNCH,
        ),
    )
}

@Preview(
    name = "Content Scale — 50 songs — DEFAULT — Ready",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewContentScale50Songs() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.DEFAULT,
            songCount = 50,
        ),
    )
}

@Preview(
    name = "Content Scale — 500 songs — DEFAULT — Ready",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewContentScale500Songs() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.DEFAULT,
            songCount = 500,
        ),
    )
}

@Preview(
    name = "Content Scale — 50,000 songs — DEFAULT — Ready (large library)",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewContentScale50KSongs() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.DEFAULT,
            songCount = 50_000,
        ),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// FORM FACTOR PREVIEWS — 4 form factors × DEFAULT profile, READY phase
// ═══════════════════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Form Factor — Phone Portrait — DEFAULT — Ready",
    showBackground = true,
    backgroundColor = 0xFF050505,
    device = Devices.PIXEL_6,
)
@Composable
private fun PreviewFormFactorPhonePortrait() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.DEFAULT,
            songCount = 500,
        ).copy(orientation = LibraryScreenOrientation.PORTRAIT),
    )
}

@Preview(
    name = "Form Factor — Phone Landscape — DEFAULT — Ready",
    showBackground = true,
    backgroundColor = 0xFF050505,
    device = Devices.PIXEL_6,
    widthDp = 800,
    heightDp = 360,
)
@Composable
private fun PreviewFormFactorPhoneLandscape() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.DEFAULT,
            songCount = 500,
        ).copy(orientation = LibraryScreenOrientation.LANDSCAPE),
    )
}

@Preview(
    name = "Form Factor — Tablet — DEFAULT — Ready",
    showBackground = true,
    backgroundColor = 0xFF050505,
    device = Devices.PIXEL_C,
)
@Composable
private fun PreviewFormFactorTablet() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.COMPACT,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.COMPACT,
            songCount = 500,
        ),
    )
}

@Preview(
    name = "Form Factor — Foldable — DEFAULT — Ready",
    showBackground = true,
    backgroundColor = 0xFF050505,
    widthDp = 600,
    heightDp = 1000,
)
@Composable
private fun PreviewFormFactorFoldable() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.DEFAULT,
            songCount = 500,
        ),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// TIME OF DAY PREVIEWS — 2 × DEFAULT profile, READY phase
// ═══════════════════════════════════════════════════════════════════════════════════════
//
// Time-of-day affects the ambient layer's color (cyan for day, violet for
// night). The CINEMATIC profile simulates "night" feel (slow motion, deep
// parallax). DEFAULT simulates "day" feel (balanced).

@Preview(
    name = "Time of Day — Morning — CINEMATIC — Ready — 500 songs",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewTimeOfDayMorning() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.CINEMATIC,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.CINEMATIC,
            songCount = 500,
            isPlaying = false,
        ),
    )
}

@Preview(
    name = "Time of Day — Night — AMOLED — Ready — 500 songs",
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun PreviewTimeOfDayNight() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.AMOLED,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.AMOLED,
            songCount = 500,
            isPlaying = false,
        ),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// PLAYBACK PREVIEWS — wave + dock with music playing
// ═══════════════════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Playback — Music Playing — DEFAULT — Ready — Wave + Dock visible",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewPlaybackPlayingDefault() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.DEFAULT,
            songCount = 500,
            isPlaying = true,
        ),
        waveState = WaveState.IDLE.copy(isActive = true),
    )
}

@Preview(
    name = "Playback — Music Playing — AMOLED — Ready",
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun PreviewPlaybackPlayingAmoled() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.AMOLED,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.AMOLED,
            songCount = 500,
            isPlaying = true,
        ),
        waveState = WaveState.IDLE.copy(isActive = true),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// EMPTY STATE KIND PREVIEWS — 7 kinds × DEFAULT profile
// ═══════════════════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Empty — First Launch — DEFAULT",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewEmptyFirstLaunch() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateEmpty(
            profile = LibraryConstitutionProfile.DEFAULT,
            kind = LibraryEmptyStateKind.FIRST_LAUNCH,
        ),
    )
}

@Preview(
    name = "Empty — All Deleted — DEFAULT",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewEmptyAllDeleted() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateEmpty(
            profile = LibraryConstitutionProfile.DEFAULT,
            kind = LibraryEmptyStateKind.ALL_DELETED,
        ),
    )
}

@Preview(
    name = "Empty — No Collections — DEFAULT",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewEmptyNoCollections() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateEmpty(
            profile = LibraryConstitutionProfile.DEFAULT,
            kind = LibraryEmptyStateKind.NO_COLLECTIONS,
        ),
    )
}

@Preview(
    name = "Empty — Filter No Match — DEFAULT",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewEmptyFilterNoMatch() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateEmpty(
            profile = LibraryConstitutionProfile.DEFAULT,
            kind = LibraryEmptyStateKind.FILTER_NO_MATCH,
        ),
    )
}

@Preview(
    name = "Empty — Search No Result — DEFAULT",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewEmptySearchNoResult() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateEmpty(
            profile = LibraryConstitutionProfile.DEFAULT,
            kind = LibraryEmptyStateKind.SEARCH_NO_RESULT,
        ),
    )
}

@Preview(
    name = "Empty — Permission Denied — DEFAULT",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewEmptyPermissionDenied() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStatePermissionRequired(LibraryConstitutionProfile.DEFAULT),
    )
}

@Preview(
    name = "Empty — Load Failed — DEFAULT",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewEmptyLoadFailed() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateError(LibraryConstitutionProfile.DEFAULT),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// HERO VISIBILITY PREVIEWS — with and without Hero
// ═══════════════════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Hero — Visible — DEFAULT — Ready — 500 songs",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewHeroVisible() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.DEFAULT,
            songCount = 500,
            hasHero = true,
        ),
    )
}

@Preview(
    name = "Hero — Hidden (dismissed) — DEFAULT — Ready — 500 songs",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewHeroHidden() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.DEFAULT,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.DEFAULT,
            songCount = 500,
            hasHero = false,
        ),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// AMOLED DEEP DIVE — multiple phases
// ═══════════════════════════════════════════════════════════════════════════════════════

@Preview(
    name = "AMOLED — Loading",
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun PreviewAmoledLoading() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.AMOLED,
        state = previewStateLoading(LibraryConstitutionProfile.AMOLED),
    )
}

@Preview(
    name = "AMOLED — Empty — First Launch",
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun PreviewAmoledEmptyFirstLaunch() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.AMOLED,
        state = previewStateEmpty(
            profile = LibraryConstitutionProfile.AMOLED,
            kind = LibraryEmptyStateKind.FIRST_LAUNCH,
        ),
    )
}

@Preview(
    name = "AMOLED — Error",
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun PreviewAmoledError() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.AMOLED,
        state = previewStateError(LibraryConstitutionProfile.AMOLED),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// CINEMATIC DEEP DIVE
// ═══════════════════════════════════════════════════════════════════════════════════════

@Preview(
    name = "CINEMATIC — Loading",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewCinematicLoading() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.CINEMATIC,
        state = previewStateLoading(LibraryConstitutionProfile.CINEMATIC),
    )
}

@Preview(
    name = "CINEMATIC — Empty — First Launch",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewCinematicEmptyFirstLaunch() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.CINEMATIC,
        state = previewStateEmpty(
            profile = LibraryConstitutionProfile.CINEMATIC,
            kind = LibraryEmptyStateKind.FIRST_LAUNCH,
        ),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// ACCESSIBILITY DEEP DIVE
// ═══════════════════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Accessibility — Reduced Motion — Loading",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewAccessibilityReducedMotionLoading() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.REDUCED_MOTION,
        state = previewStateLoading(LibraryConstitutionProfile.REDUCED_MOTION),
    )
}

@Preview(
    name = "Accessibility — Reduced Motion — Empty",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewAccessibilityReducedMotionEmpty() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.REDUCED_MOTION,
        state = previewStateEmpty(
            profile = LibraryConstitutionProfile.REDUCED_MOTION,
            kind = LibraryEmptyStateKind.FIRST_LAUNCH,
        ),
    )
}

@Preview(
    name = "Accessibility — High Contrast — Loading",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewAccessibilityHighContrastLoading() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.HIGH_CONTRAST,
        state = previewStateLoading(LibraryConstitutionProfile.HIGH_CONTRAST),
    )
}

@Preview(
    name = "Accessibility — High Contrast — Ready",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewAccessibilityHighContrastReady() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.HIGH_CONTRAST,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.HIGH_CONTRAST,
            songCount = 500,
        ),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// LOW_END DEEP DIVE
// ═══════════════════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Low-End — Loading",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewLowEndLoading() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.LOW_END,
        state = previewStateLoading(LibraryConstitutionProfile.LOW_END),
    )
}

@Preview(
    name = "Low-End — Empty",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewLowEndEmpty() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.LOW_END,
        state = previewStateEmpty(
            profile = LibraryConstitutionProfile.LOW_END,
            kind = LibraryEmptyStateKind.FIRST_LAUNCH,
        ),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// HIGH_END DEEP DIVE
// ═══════════════════════════════════════════════════════════════════════════════════════

@Preview(
    name = "High-End — Loading",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewHighEndLoading() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.HIGH_END,
        state = previewStateLoading(LibraryConstitutionProfile.HIGH_END),
    )
}

@Preview(
    name = "High-End — Ready — 50,000 songs (large library stress)",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewHighEndReady50K() {
    LibraryPreviewFrame(
        profile = LibraryConstitutionProfile.HIGH_END,
        state = previewStateReady(
            profile = LibraryConstitutionProfile.HIGH_END,
            songCount = 50_000,
            isPlaying = true,
        ),
        waveState = WaveState.IDLE.copy(isActive = true),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// COVERAGE SUMMARY
//
// This file produces 40+ @Preview composables, covering:
//
//   • 8 profiles (DEFAULT, AMOLED, CINEMATIC, COMPACT, LOW_END, HIGH_END,
//     REDUCED_MOTION, HIGH_CONTRAST)
//   • 8 phases (COLD_START, LOADING, PARTIAL_READY, READY, EMPTY, ERROR,
//     PERMISSION_REQUIRED, SELECTION_MODE)
//   • 4 content scales (0, 50, 500, 50,000 songs)
//   • 4 form factors (phone-portrait, phone-landscape, tablet, foldable)
//   • 2 times of day (morning via CINEMATIC, night via AMOLED)
//   • 2 playback states (idle, playing)
//   • 7 empty-state kinds (FIRST_LAUNCH, ALL_DELETED, NO_COLLECTIONS,
//     FILTER_NO_MATCH, SEARCH_NO_RESULT, PERMISSION_DENIED, LOAD_FAILED)
//   • 2 hero visibility states (visible, hidden)
//
// A new developer opens this file in Android Studio's Preview pane and
// immediately understands every meaningful state of the Library screen.
// Rule #77 — Preview Is Documentation.
// ═══════════════════════════════════════════════════════════════════════════════════════
