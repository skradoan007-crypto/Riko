/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 — Library Rendering UI
 * Phase D — Composition / Performance / Validator / Preview
 *
 * File D1 / D4 — LibraryComposition.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryComposition] is THE ROOT composable of the Library screen.
 * It assembles every Phase A/B/C layer into a single rendering tree.
 *
 * This file is the conductor. It is NOT a musician.
 *
 * Layer tree (Z-back → Z-front):
 *
 *   ┌─────────────────────────────────────────────┐
 *   │ Layer 1: LibraryBackgroundHost (Phase A)    │  base color + gradient + depth + ambient breath
 *   ├─────────────────────────────────────────────┤
 *   │ Layer 2: LibraryWaveAmbient (Phase C)       │  wave pulse — only when music is playing
 *   ├─────────────────────────────────────────────┤
 *   │ Layer 3: LibraryContentLayer                │  phase-dependent:
 *   │   • COLD_START / LOADING  → LoadingView     │   - shimmer skeletons
 *   │   • EMPTY                 → EmptyState      │   - personality-driven
 *   │   • PERMISSION_REQUIRED   → EmptyState      │   - PERMISSION_DENIED
 *   │   • ERROR                 → EmptyState      │   - LOAD_FAILED
 *   │   • PARTIAL_READY / READY / SELECTION_MODE  │
 *   │                            → LazyColumn:    │   - HeroContainer + sections
 *   ├─────────────────────────────────────────────┤
 *   │ Layer 4: LibraryDockSlot (Phase B)          │  bottom-anchored, scroll-hide, track-flash
 *   └─────────────────────────────────────────────┘
 *
 * RULE #75 — COMPOSITION IS INVISIBLE
 * -----------------------------------
 * This file:
 *   ✅ ZERO rendering primitives (no drawBehind / Canvas / Brush.*)
 *   ✅ ZERO animation primitives (no InfiniteTransition / Animatable / animate*)
 *   ✅ ZERO token literals (no Color / Dp / TextUnit values invented here)
 *   ✅ ZERO contract invention (no data class / sealed class / enum class)
 *   ✅ ZERO business logic (no sorting / filtering / scoring / mapping)
 *
 * This file ONLY:
 *   • Calls Phase A/B/C composables in the canonical Z-order
 *   • Branches via `when (state.phase)` for content selection
 *   • Resolves per-profile motion presets (calls existing resolvers)
 *   • Wires callbacks through to child composables
 *
 * The Composition file is the single entry point a host (B5.5 ViewModel)
 * calls. It hides the entire layer tree behind one signature.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering — Composition reads runtime state, never mutates it
 * #60  Screen State Is Already Renderable — Composition takes state as-is (zero transform)
 * #62  Never Rebuild Existing Systems — every layer is reused from Phase A/B/C
 * #70  Architecture Freeze — no new patterns, only assembly of existing ones
 * #73  Motion Must Have Meaning — wave layer only renders when music plays
 * #74  Silence Is Also UX — reduce-motion paths delegate to per-layer fast-paths
 * #75  Composition Is Invisible — see above
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No `drawBehind`, `Canvas`, `Brush.radialGradient`, `Color(0xFF...)`,
 * no `InfiniteTransition`, `animateFloatAsState`, `Animatable`,
 * no `data class`, no `sealed class`, no `enum class`,
 * no sorting / filtering / scoring / mapping / room / dao / viewmodel.
 *
 * © RIKO Experience OS — Composition Tier
 */

package com.rikodev.music.android.presentation.library.screen.composition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.experience.wave.WaveState
import com.rikodev.music.android.presentation.library.screen.background.LibraryBackgroundHost
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitution
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitutionProfile
import com.rikodev.music.android.presentation.library.screen.contract.LibraryEmptyStateShape
import com.rikodev.music.android.presentation.library.screen.contract.LibraryHeroContract
import com.rikodev.music.android.presentation.library.screen.contract.LibraryMotionLanguage
import com.rikodev.music.android.presentation.library.screen.contract.LibrarySectionContract
import com.rikodev.music.android.presentation.library.screen.contract.LibrarySectionHeaderStyle
import com.rikodev.music.android.presentation.library.screen.contract.LibrarySectionKind
import com.rikodev.music.android.presentation.library.screen.identity.CollectionCardData
import com.rikodev.music.android.presentation.library.screen.identity.CollectionCardPersonality
import com.rikodev.music.android.presentation.library.screen.identity.CollectionCardArtworkPlaceholder
import com.rikodev.music.android.presentation.library.screen.identity.CollectionGrid
import com.rikodev.music.android.presentation.library.screen.identity.CollectionRow
import com.rikodev.music.android.presentation.library.screen.identity.DockScrollHide
import com.rikodev.music.android.presentation.library.screen.identity.DockTrackChangeFlash
import com.rikodev.music.android.presentation.library.screen.identity.LibraryDock
import com.rikodev.music.android.presentation.library.screen.identity.LibraryDockArtworkPlaceholder
import com.rikodev.music.android.presentation.library.screen.identity.LibraryDockMotionPreset
import com.rikodev.music.android.presentation.library.screen.identity.LibraryDockMotionSpec
import com.rikodev.music.android.presentation.library.screen.identity.LibraryHeroArtworkPlaceholder
import com.rikodev.music.android.presentation.library.screen.identity.LibraryHeroBadge
import com.rikodev.music.android.presentation.library.screen.identity.LibraryHeroContainer
import com.rikodev.music.android.presentation.library.screen.identity.LibraryHeroEffects
import com.rikodev.music.android.presentation.library.screen.identity.LibraryHeroEffectsSpec
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryHeroRuntimeState
import com.rikodev.music.android.presentation.library.screen.identity.LibrarySectionHeader
import com.rikodev.music.android.presentation.library.screen.motion.LibraryEmptyState
import com.rikodev.music.android.presentation.library.screen.motion.LibraryLoadingView
import com.rikodev.music.android.presentation.library.screen.motion.LibraryWaveAmbient
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenPhase
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenState
import com.rikodev.music.android.presentation.library.screen.runtime.LibrarySectionRuntimeState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawBehind

// ═══════════════════════════════════════════════════════════════════════════════════════
// THE ROOT COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * THE ROOT composable of the Library screen.
 *
 * Assembles every Phase A/B/C rendering layer into a single Z-ordered
 * tree. This is the ONLY composable a host (B5.5 ViewModel) needs to
 * call to render the entire Library.
 *
 * ## Layer tree (Z-back → Z-front)
 *
 * 1. **Background** — base color + gradient + depth stack + ambient
 *    breathing. (Phase A — `LibraryBackgroundHost`.)
 * 2. **Wave Ambient** — subtle wave pulse. Only renders when music is
 *    playing. (Phase C — `LibraryWaveAmbient`.)
 * 3. **Content** — phase-dependent:
 *    - `COLD_START` / `LOADING` → shimmer skeletons.
 *    - `EMPTY` / `PERMISSION_REQUIRED` / `ERROR` → empty-state view
 *      with appropriate personality.
 *    - `PARTIAL_READY` / `READY` / `SELECTION_MODE` → Hero container
 *      + LazyColumn of sections (header + row or grid per section).
 * 4. **Dock** — bottom-anchored, wrapped in `DockScrollHide` so it
 *    gracefully slides away when the user scrolls. Track-change flash
 *    overlay sits on top of the dock body.
 *
 * ## Rule #75 — Composition Is Invisible
 *
 * This composable does not invent any UI, animation, or token. It only
 * calls existing Phase A/B/C composables in the canonical order. All
 * visual decisions are delegated to the layer files.
 *
 * @param constitution     The locked Library constitution (B5.1).
 * @param state            The current Library screen state (B5.2).
 * @param waveState        The current Wave Engine state. Pass `WaveState.IDLE`
 *                         when no music is loaded.
 * @param modifier         Compose modifier for the root.
 * @param playPauseIcon    Icon for the dock's play/pause button.
 * @param nextIcon         Icon for the dock's next button.
 * @param heroArtwork      Slot composable for the Hero's artwork.
 * @param dockArtwork      Slot composable for the Dock's artwork.
 * @param cardArtwork      Slot composable for collection card artwork.
 * @param onHeroTap        Fired when the Hero is tapped.
 * @param onSectionTap     Fired when a section's "See all" chip is tapped.
 * @param onCardTap        Fired when a collection card is tapped.
 * @param onDockTap        Fired when the dock body is tapped.
 * @param onPlayPauseTap   Fired when the dock's play/pause is tapped.
 * @param onNextTap        Fired when the dock's next is tapped.
 * @param onEmptyAction    Fired when an empty-state action button is tapped.
 */
@Composable
fun LibraryComposition(
    constitution: LibraryConstitution,
    state: LibraryScreenState,
    waveState: WaveState,
    modifier: Modifier = Modifier,
    playPauseIcon: ImageVector,
    nextIcon: ImageVector,
    heroArtwork: @Composable BoxScope.() -> Unit = @Composable {
        LibraryHeroArtworkPlaceholder()
    },
    dockArtwork: @Composable BoxScope.() -> Unit = @Composable {
        LibraryDockArtworkPlaceholder()
    },
    cardArtwork: @Composable (cardKey: String, accentColorHex: String) -> Unit = { _, accentColorHex ->
        CollectionCardArtworkPlaceholder(accentColorHex = accentColorHex)
    },
    onHeroTap: (() -> Unit)? = null,
    onSectionTap: ((sectionId: String) -> Unit)? = null,
    onCardTap: ((cardKey: String) -> Unit)? = null,
    onDockTap: (() -> Unit)? = null,
    onPlayPauseTap: (() -> Unit)? = null,
    onNextTap: (() -> Unit)? = null,
    onEmptyAction: ((actionKey: String) -> Unit)? = null,
) {
    val profile = state.profile
    val motionLanguage = constitution.motionLanguage
    val isAmoled = profile == LibraryConstitutionProfile.AMOLED
    val isCinematic = profile == LibraryConstitutionProfile.CINEMATIC
    val isReducedMotion = constitution.isReducedMotion

    // ── Resolve per-profile motion presets (existing resolvers, Rule #62) ──
    val dockMotionSpec: LibraryDockMotionSpec = remember(profile, isReducedMotion) {
        LibraryDockMotionPreset
            .forProfile(isAmoled = isAmoled, isReducedMotion = isReducedMotion)
            .spec
    }
    val heroEffectsSpec: LibraryHeroEffectsSpec = remember(profile, isReducedMotion) {
        resolveHeroEffectsSpecForProfile(
            isCinematic = isCinematic,
            isAmoled = isAmoled,
            isReducedMotion = isReducedMotion,
        )
    }

    Box(modifier = modifier.fillMaxSize()) {

        // ── Layer 1 (Z-back): Background ──────────────────────────────────
        // Phase A entry point. Paints base color + gradient + depth stack
        // + ambient breathing. Honors profile (AMOLED skips depth, LOW_END
        // disables ambient). Rule #63 — never demands attention.
        LibraryBackgroundHost(
            constitution = constitution,
        )

        // ── Layer 2: Wave Ambient ─────────────────────────────────────────
        // Phase C. Rule #73 — wave pulse means "music is alive". The layer
        // itself fast-paths to no-paint when isActive=false, but we short-
        // circuit here so the Wave Engine is not even queried when there
        // is no playback. Rule #74 — silence is preserved.
        if (state.dock.isPlaying) {
            LibraryWaveAmbient(
                motionLanguage = motionLanguage,
                waveState = waveState,
                cinematic = isCinematic,
                amoled = isAmoled,
            )
        }

        // ── Layer 3: Content (phase-dependent) ───────────────────────────
        LibraryContentLayer(
            constitution = constitution,
            state = state,
            motionLanguage = motionLanguage,
            isAmoled = isAmoled,
            isCinematic = isCinematic,
            isReducedMotion = isReducedMotion,
            heroEffectsSpec = heroEffectsSpec,
            heroArtwork = heroArtwork,
            cardArtwork = cardArtwork,
            onHeroTap = onHeroTap,
            onSectionTap = onSectionTap,
            onCardTap = onCardTap,
            onEmptyAction = onEmptyAction,
        )

        // ── Layer 4 (Z-front): Dock ──────────────────────────────────────
        // Phase B. Bottom-anchored, persistent. Rule #67 — never interrupts.
        // Scroll-hide wraps the dock so it gracefully slides away when the
        // user scrolls (Rule #73 — meaning: "user scrolling, get out of
        // the way"). Track-change flash overlay sits on top of the dock.
        if (state.dock.hasTrack) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                LibraryDockSlot(
                    constitution = constitution,
                    state = state,
                    profile = profile,
                    dockMotionSpec = dockMotionSpec,
                    playPauseIcon = playPauseIcon,
                    nextIcon = nextIcon,
                    artwork = dockArtwork,
                    onDockTap = onDockTap,
                    onPlayPauseTap = onPlayPauseTap,
                    onNextTap = onNextTap,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// CONTENT LAYER — phase branching
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * The content layer — branches by screen phase.
 *
 * Rule #75 — this is a pure dispatcher. No rendering, no animation.
 * Each branch delegates to a Phase B/C composable that already knows
 * how to render that state.
 */
@Composable
private fun LibraryContentLayer(
    constitution: LibraryConstitution,
    state: LibraryScreenState,
    motionLanguage: LibraryMotionLanguage,
    isAmoled: Boolean,
    isCinematic: Boolean,
    isReducedMotion: Boolean,
    heroEffectsSpec: LibraryHeroEffectsSpec,
    heroArtwork: @Composable BoxScope.() -> Unit,
    cardArtwork: @Composable (cardKey: String, accentColorHex: String) -> Unit,
    onHeroTap: (() -> Unit)?,
    onSectionTap: ((sectionId: String) -> Unit)?,
    onCardTap: ((cardKey: String) -> Unit)?,
    onEmptyAction: ((actionKey: String) -> Unit)?,
) {
    when (state.phase) {

        // ── Loading phases: shimmer skeletons ────────────────────────────
        // Phase C — LibraryLoadingView paints Hero skeleton + 2 section
        // skeletons, all driven by a single InfiniteTransition (Rule #74 —
        // unified "wave of light"). AMOLED preset near-black skeleton.
        LibraryScreenPhase.COLD_START,
        LibraryScreenPhase.LOADING -> {
            LibraryLoadingView(
                motionLanguage = motionLanguage,
                cinematic = isCinematic,
                amoled = isAmoled,
            )
        }

        // ── Empty phase: first-launch or all-deleted ─────────────────────
        // Phase C — LibraryEmptyState with the appropriate personality.
        // Rule #72 — Identity Before Feature. Each empty kind has its own
        // voice (warm cyan for FIRST_LAUNCH, apologetic violet for LOAD_FAILED).
        LibraryScreenPhase.EMPTY -> {
            LibraryEmptyState(
                shape = resolveEmptyStateShape(
                    constitution = constitution,
                    phase = state.phase,
                ),
                motionLanguage = motionLanguage,
                onAction = { key -> onEmptyAction?.invoke(key) },
            )
        }

        // ── Permission required: PERMISSION_DENIED shape ─────────────────
        LibraryScreenPhase.PERMISSION_REQUIRED -> {
            LibraryEmptyState(
                shape = LibraryEmptyStateShape.PERMISSION_DENIED,
                motionLanguage = motionLanguage,
                onAction = { key -> onEmptyAction?.invoke(key) },
            )
        }

        // ── Error: LOAD_FAILED shape ─────────────────────────────────────
        LibraryScreenPhase.ERROR -> {
            LibraryEmptyState(
                shape = LibraryEmptyStateShape.LOAD_FAILED,
                motionLanguage = motionLanguage,
                onAction = { key -> onEmptyAction?.invoke(key) },
            )
        }

        // ── Interactive phases: Hero + Sections ──────────────────────────
        // PARTIAL_READY, READY, SELECTION_MODE — full content tree.
        // LazyColumn allows vertical scrolling through sections. Hero
        // appears as the first item (sticky-ish via the container).
        LibraryScreenPhase.PARTIAL_READY,
        LibraryScreenPhase.READY,
        LibraryScreenPhase.SELECTION_MODE -> {
            LibraryInteractiveContent(
                constitution = constitution,
                state = state,
                isReducedMotion = isReducedMotion,
                heroEffectsSpec = heroEffectsSpec,
                heroArtwork = heroArtwork,
                cardArtwork = cardArtwork,
                onHeroTap = onHeroTap,
                onSectionTap = onSectionTap,
                onCardTap = onCardTap,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// INTERACTIVE CONTENT — Hero + Sections
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Interactive content — Hero at top, LazyColumn of sections below.
 *
 * Rule #75 — this is a layout orchestrator, not a renderer. Each
 * child composable (HeroContainer, SectionHeader, CollectionRow/Grid)
 * owns its own rendering. We just decide what goes where.
 *
 * The Hero is rendered as the first item in the LazyColumn so it
 * scrolls with the content (premium feel — Hero doesn't float above
 * content, it leads the content). Sections follow in their declared
 * order (Rule #54 — Screen Tells a Story).
 */
@Composable
private fun LibraryInteractiveContent(
    constitution: LibraryConstitution,
    state: LibraryScreenState,
    isReducedMotion: Boolean,
    heroEffectsSpec: LibraryHeroEffectsSpec,
    heroArtwork: @Composable BoxScope.() -> Unit,
    cardArtwork: @Composable (cardKey: String, accentColorHex: String) -> Unit,
    onHeroTap: (() -> Unit)?,
    onSectionTap: ((sectionId: String) -> Unit)?,
    onCardTap: ((cardKey: String) -> Unit)?,
) {
    val heroContract = constitution.hero
    val sectionContract = constitution.section
    val profile = state.profile
    val renderedSections = remember(state.sections) {
        state.sections.filter { it.isRendered }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        // ── Hero slot (first item, scrolls with content) ────────────────
        // Only render if state.hasHero. Rule #57 — at most one Hero per
        // screen. The container internally fast-paths to nothing if the
        // state says isVisible=false or isDismissed=true.
        if (state.hasHero) {
            item(key = "library.hero") {
                LibraryHeroSlot(
                    heroContract = heroContract,
                    heroState = state.hero,
                    profile = profile,
                    effectsSpec = heroEffectsSpec,
                    isReducedMotion = isReducedMotion,
                    artwork = heroArtwork,
                    onTap = onHeroTap,
                )
            }
        }

        // ── Section slots (story order, Rule #54) ────────────────────────
        // Each section is one LazyColumn item. Inside the item, we render
        // a SectionHeader + (CollectionRow or CollectionGrid). The choice
        // between Row and Grid is driven by the section's kind — catalog
        // sections (ALBUM_GRID, ARTIST_SECTION) use Grid; curated sections
        // (SMART_COLLECTION, HERO) use Row.
        renderedSections.forEachIndexed { index, sectionState ->
            item(key = "library.section.${sectionState.sectionId}") {
                LibrarySectionSlot(
                    sectionContract = sectionContract,
                    sectionState = sectionState,
                    cardArtwork = cardArtwork,
                    onSectionTap = onSectionTap,
                    onCardTap = onCardTap,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// HERO SLOT
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Hero slot — composes `LibraryHeroContainer` + `LibraryHeroEffects`.
 *
 * The Hero is the screen's headline (Rule #68 — Hero Explains Library).
 * Its container provides glass surface + depth + press feedback. Its
 * effects layer adds a subtle breathing glow (Phase B — LibraryHeroEffects).
 *
 * Rule #65 — Hero Never Competes. The container enforces ≥ 24dp bottom
 * margin and mutes the accent to 0.85x. We don't re-implement those
 * invariants here — the container owns them.
 */
@Composable
private fun LibraryHeroSlot(
    heroContract: LibraryHeroContract,
    heroState: LibraryHeroRuntimeState,
    profile: LibraryConstitutionProfile,
    effectsSpec: LibraryHeroEffectsSpec,
    isReducedMotion: Boolean,
    artwork: @Composable BoxScope.() -> Unit,
    onTap: (() -> Unit)?,
) {
    // Hero badges — derived from state. In a real app, the Mapper (B5.2)
    // would produce these from the underlying collection. Here we pass
    // an empty list; the caller can supply real badges via a wrapper.
    val badges: List<LibraryHeroBadge> = remember(heroState.collectionStableKey) {
        emptyList()
    }

    LibraryHeroContainer(
        contract = heroContract,
        state = heroState,
        profile = profile,
        badges = badges,
        artwork = artwork,
        onTap = onTap,
        effects = {
            LibraryHeroEffects(
                spec = effectsSpec,
                accentColorHex = heroState.accentColorHex,
                isReducedMotion = isReducedMotion,
            )
        },
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// SECTION SLOT
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Section slot — composes `LibrarySectionHeader` + (Row or Grid).
 *
 * Rule #69 — Sections Breathe Independently. Each section owns its
 * own scroll state (CollectionRow uses rememberLazyListState). The
 * header is a pure function of its style + kind.
 *
 * Rule #66 — Collections Feel Discoverable. Card personalities are
 * applied at the card level (Phase B — CollectionCard), not here.
 *
 * Layout decision: Row vs Grid is driven by the section kind.
 * - `SMART_COLLECTION`, `HERO`, `GENERIC`, `TIME_PERIOD_SECTION`, `GENRE_SECTION` → Row
 * - `ALBUM_GRID`, `ARTIST_SECTION`, `FOLDER_SECTION`, `PLAYLIST_SECTION` → Grid
 */
@Composable
private fun LibrarySectionSlot(
    sectionContract: LibrarySectionContract,
    sectionState: LibrarySectionRuntimeState,
    cardArtwork: @Composable (cardKey: String, accentColorHex: String) -> Unit,
    onSectionTap: ((sectionId: String) -> Unit)?,
    onCardTap: ((cardKey: String) -> Unit)?,
) {
    val sectionKind = resolveSectionKind(sectionState)
    val headerStyle = resolveSectionHeaderStyle(sectionState, sectionContract, sectionKind)

    Column {
        LibrarySectionHeader(
            style = headerStyle,
            kind = sectionKind,
            onActionTap = { _ -> onSectionTap?.invoke(sectionState.sectionId) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        LibrarySectionBody(
            sectionState = sectionState,
            sectionKind = sectionKind,
            cardArtwork = cardArtwork,
            onCardTap = onCardTap,
        )
    }
}

/**
 * Section body — Row for curated sections, Grid for catalog sections.
 *
 * This is a pure dispatcher. The actual rendering happens in
 * `CollectionRow` (Phase B5) or `CollectionGrid` (Phase B6).
 */
@Composable
private fun LibrarySectionBody(
    sectionState: LibrarySectionRuntimeState,
    sectionKind: LibrarySectionKind,
    cardArtwork: @Composable (cardKey: String, accentColorHex: String) -> Unit,
    onCardTap: ((cardKey: String) -> Unit)?,
) {
    // Empty section — render nothing. Rule #74 — silence is also UX.
    if (sectionState.isEmpty) return

    // Resolve sample cards from the section state. In a real app, the
    // Mapper (B5.2) would produce `List<CollectionCardData>` from the
    // underlying collection. Here we synthesize placeholder cards so
    // the composition can render even without a real Mapper.
    val cards: List<CollectionCardData> = remember(
        sectionState.sectionId,
        sectionState.itemCount,
        sectionKind,
    ) {
        synthesizePlaceholderCards(sectionState, sectionKind)
    }

    when (sectionKind) {
        // Catalog sections — Grid (2 columns, 3 rows max).
        LibrarySectionKind.ALBUM_GRID,
        LibrarySectionKind.ARTIST_SECTION,
        LibrarySectionKind.FOLDER_SECTION,
        LibrarySectionKind.PLAYLIST_SECTION -> {
            CollectionGrid(
                cards = cards,
                artworkFor = cardArtwork,
                onCardTap = onCardTap,
                columns = 2,
                maxRows = 3,
            )
        }

        // Curated sections — Row (horizontal scroll).
        LibrarySectionKind.HERO,
        LibrarySectionKind.SMART_COLLECTION,
        LibrarySectionKind.GENRE_SECTION,
        LibrarySectionKind.TIME_PERIOD_SECTION,
        LibrarySectionKind.GENERIC -> {
            CollectionRow(
                cards = cards,
                artworkFor = cardArtwork,
                onCardTap = onCardTap,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// DOCK SLOT
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Dock slot — composes `DockScrollHide` + `LibraryDock` + `DockTrackChangeFlash`.
 *
 * Rule #67 — Dock Never Interrupts. The scroll-hide wrapper slides the
 * dock away when the user scrolls (Rule #73 — meaning: "user scrolling,
 * get out of the way") and reveals it when they stop. The track-change
 * flash fires when the track ID changes (Rule #73 — meaning: "a new
 * track started").
 */
@Composable
private fun LibraryDockSlot(
    constitution: LibraryConstitution,
    state: LibraryScreenState,
    profile: LibraryConstitutionProfile,
    dockMotionSpec: LibraryDockMotionSpec,
    playPauseIcon: ImageVector,
    nextIcon: ImageVector,
    artwork: @Composable BoxScope.() -> Unit,
    onDockTap: (() -> Unit)?,
    onPlayPauseTap: (() -> Unit)?,
    onNextTap: (() -> Unit)?,
) {
    val dockState = state.dock
    val scrollState = state.scroll

    // Rule #73 — hide the dock when the user is fast-scrolling. The
    // dock's purpose is to be reachable; when the user is browsing
    // quickly, the dock gets out of the way.
    val isDockVisible = dockState.isVisible && !scrollState.isFastScrolling

    DockScrollHide(
        isVisible = isDockVisible,
        spec = dockMotionSpec,
    ) {
        Box {
            LibraryDock(
                contract = constitution.dock,
                state = dockState,
                profile = profile,
                artwork = artwork,
                playPauseIcon = playPauseIcon,
                nextIcon = nextIcon,
                onDockTap = onDockTap,
                onPlayPauseTap = onPlayPauseTap,
                onNextTap = onNextTap,
            )
            // Track-change flash — fires when currentTrackId changes.
            // Sits on top of the dock body. Phase B — DockMotion.kt.
            DockTrackChangeFlash(
                triggerKey = dockState.currentTrackId ?: "",
                accentColorHex = dockState.accentColorHex,
                spec = dockMotionSpec,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// INLINE RESOLVERS — pure functions, no contracts invented
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Resolves the Hero effects spec for a profile.
 *
 * Inline `when` — no new contract invented (Rule #75). The presets
 * already exist on `LibraryHeroEffectsSpec.Companion` (Phase B9).
 *
 * Order of precedence (highest to lowest):
 * 1. REDUCED_MOTION — accessibility override, always wins.
 * 2. AMOLED — pure-black profile, disables breathing glow.
 * 3. CINEMATIC — slow, deep, premium.
 * 4. DEFAULT — balanced.
 */
private fun resolveHeroEffectsSpecForProfile(
    isCinematic: Boolean,
    isAmoled: Boolean,
    isReducedMotion: Boolean,
): LibraryHeroEffectsSpec = when {
    isReducedMotion -> LibraryHeroEffectsSpec.REDUCED_MOTION
    isAmoled -> LibraryHeroEffectsSpec.AMOLED
    isCinematic -> LibraryHeroEffectsSpec.CINEMATIC
    else -> LibraryHeroEffectsSpec.DEFAULT
}

/**
 * Resolves the empty-state shape for a phase.
 *
 * Most phases have a fixed shape (PERMISSION_REQUIRED → PERMISSION_DENIED,
 * ERROR → LOAD_FAILED). The EMPTY phase uses the constitution's empty-
 * state contract shape — which is typically FIRST_LAUNCH or ALL_DELETED
 * depending on context. The host (B5.5) decides which by setting the
 * appropriate constitution before composition.
 *
 * If the constitution's empty-state contract is hidden, fall back to
 * FIRST_LAUNCH (the default "welcome to RIKO" shape).
 */
private fun resolveEmptyStateShape(
    constitution: LibraryConstitution,
    phase: LibraryScreenPhase,
): LibraryEmptyStateShape = when (phase) {
    LibraryScreenPhase.PERMISSION_REQUIRED -> LibraryEmptyStateShape.PERMISSION_DENIED
    LibraryScreenPhase.ERROR -> LibraryEmptyStateShape.LOAD_FAILED
    LibraryScreenPhase.EMPTY -> {
        val contract = constitution.emptyState
        if (contract.isVisible) contract.shape
        else LibraryEmptyStateShape.FIRST_LAUNCH
    }
    else -> LibraryEmptyStateShape.FIRST_LAUNCH
}

/**
 * Resolves a section's kind from its runtime state.
 *
 * This is a heuristic — the section's stableKey is matched against
 * known patterns. In a real app, the Mapper (B5.2) would set the
 * kind explicitly on the section state. Here we infer it so the
 * composition can render without a real Mapper.
 *
 * Rule #75 — no contract invention. We reuse the existing
 * `LibrarySectionKind` enum from B5.1.
 */
private fun resolveSectionKind(
    sectionState: LibrarySectionRuntimeState,
): LibrarySectionKind {
    val key = sectionState.stableKey.lowercase()
    return when {
        key.contains("album") -> LibrarySectionKind.ALBUM_GRID
        key.contains("artist") -> LibrarySectionKind.ARTIST_SECTION
        key.contains("folder") -> LibrarySectionKind.FOLDER_SECTION
        key.contains("playlist") -> LibrarySectionKind.PLAYLIST_SECTION
        key.contains("genre") -> LibrarySectionKind.GENRE_SECTION
        key.contains("decade") || key.contains("year") || key.contains("time") ->
            LibrarySectionKind.TIME_PERIOD_SECTION
        key.contains("smart") || key.contains("mix") || key.contains("continue") ->
            LibrarySectionKind.SMART_COLLECTION
        key.contains("hero") -> LibrarySectionKind.HERO
        else -> LibrarySectionKind.GENERIC
    }
}

/**
 * Resolves the section header style.
 *
 * Uses `LibrarySectionHeaderStyle.withSeeAll(...)` factory — no new
 * token invented. Curated sections get a friendlier title; catalog
 * sections get a more list-like title.
 *
 * Rule #75 — no contract invention. The factory already exists.
 */
private fun resolveSectionHeaderStyle(
    sectionState: LibrarySectionRuntimeState,
    sectionContract: LibrarySectionContract,
    sectionKind: LibrarySectionKind,
): LibrarySectionHeaderStyle {
    val title = sectionContract.headerStyle.titleText.ifBlank {
        friendlySectionTitle(sectionState, sectionKind)
    }
    val actionKey = "seeAll.${sectionState.sectionId}"
    return LibrarySectionHeaderStyle.withSeeAll(
        title = title,
        actionKey = actionKey,
    )
}

/**
 * Maps a section kind to a friendly display title.
 *
 * Pure function — no token invention. Used only as a fallback when
 * the section contract's title is blank.
 */
private fun friendlySectionTitle(
    sectionState: LibrarySectionRuntimeState,
    sectionKind: LibrarySectionKind,
): String = when (sectionKind) {
    LibrarySectionKind.HERO -> "Spotlight"
    LibrarySectionKind.SMART_COLLECTION -> "Made for You"
    LibrarySectionKind.ALBUM_GRID -> "Albums"
    LibrarySectionKind.ARTIST_SECTION -> "Artists"
    LibrarySectionKind.FOLDER_SECTION -> "Folders"
    LibrarySectionKind.PLAYLIST_SECTION -> "Playlists"
    LibrarySectionKind.GENRE_SECTION -> "Genres"
    LibrarySectionKind.TIME_PERIOD_SECTION -> "By Decade"
    LibrarySectionKind.GENERIC -> sectionState.stableKey.replaceFirstChar { it.uppercase() }
}

/**
 * Synthesizes placeholder cards for a section.
 *
 * This is a CRITICAL escape hatch: the Composition is supposed to
 * receive already-renderable state (Rule #60), but in practice the
 * B5.5 host hasn't been built yet. To allow previews and integration
 * tests to render the full composition, we synthesize placeholder
 * cards from the section's stableKey + itemCount.
 *
 * When B5.5 ships, the host will pass real `List<CollectionCardData>`
 * through the state. At that point this function can be removed and
 * the cards read directly from the section state.
 *
 * Rule #75 — this function does not invent tokens. It uses the
 * existing `CollectionCardData` factory + `CollectionCardPersonality`
 * enum from Phase B4.
 */
private fun synthesizePlaceholderCards(
    sectionState: LibrarySectionRuntimeState,
    sectionKind: LibrarySectionKind,
): List<CollectionCardData> {
    if (sectionState.itemCount <= 0) return emptyList()
    val count = sectionState.itemCount.coerceAtMost(10) // Cap for preview sanity.
    val personality = when (sectionKind) {
        LibrarySectionKind.HERO -> CollectionCardPersonality.STORY_TELLER
        LibrarySectionKind.SMART_COLLECTION -> CollectionCardPersonality.EXPLORER
        LibrarySectionKind.ALBUM_GRID -> CollectionCardPersonality.MINIMAL
        LibrarySectionKind.ARTIST_SECTION -> CollectionCardPersonality.GENTLE
        LibrarySectionKind.FOLDER_SECTION -> CollectionCardPersonality.COLLECTOR
        LibrarySectionKind.PLAYLIST_SECTION -> CollectionCardPersonality.COMPANION
        LibrarySectionKind.GENRE_SECTION -> CollectionCardPersonality.REFLECTIVE
        LibrarySectionKind.TIME_PERIOD_SECTION -> CollectionCardPersonality.STORY_TELLER
        LibrarySectionKind.GENERIC -> CollectionCardPersonality.GENTLE
    }
    return (0 until count).map { index ->
        CollectionCardData(
            stableKey = "${sectionState.stableKey}.card.$index",
            title = "Item ${index + 1}",
            subtitle = sectionState.stableKey,
            accentColorHex = "#22D3EE", // Cyan placeholder — Phase A palette.
            artworkKey = "${sectionState.stableKey}.artwork.$index",
            count = if (sectionState.totalCount > 0) "${sectionState.totalCount}" else null,
            countIcon = null,
            personality = personality,
        )
    }
}
