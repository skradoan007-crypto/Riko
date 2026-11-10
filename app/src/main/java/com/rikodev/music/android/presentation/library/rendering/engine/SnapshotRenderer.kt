/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B4 — Library Rendering Runtime
 *
 * File 11 / 13 — SnapshotRenderer.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [SnapshotRenderer] is THE orchestrator of the Library Rendering
 * Runtime. It takes a [CollectionRuntimeState] (B2) and produces
 * a [LibraryRenderSnapshot] (B4) — the UI's only input.
 *
 * Data flow:
 *
 *   CollectionRuntimeState   (B2 — runtime, has snapshots + contracts)
 *               │
 *               ▼
 *   SnapshotRenderer         (B4 — this file)
 *               │
 *               ▼
 *   LibraryRenderSnapshot    (B4 — UI's ONLY input)
 *
 * STRATEGIC SUGGESTION (post-B3 lock):
 * -------------------------------------
 * "CollectionEngine → CollectionRuntimeSnapshot → Library Renderer"
 * The UI must NEVER know about Strategy / Scorer / Ranking / Cache.
 * If the entire engine is rewritten, the UI must not change.
 *
 * This file is the *only* place where that contract is enforced.
 * The UI never sees a [CollectionRuntimeState] — only the resolved
 * [LibraryRenderSnapshot].
 *
 * RESPONSIBILITIES
 * ----------------
 *   1. Map runtime phase → render visual state.
 *   2. For each section contract + runtime snapshot, produce a
 *      [RenderSection] with resolved layout, motion, theme, items,
 *      empty state, accessibility.
 *   3. Resolve the Library header.
 *   4. Compute the diff against the previous render.
 *   5. Produce diagnostics summary (Rule #53 — observation only).
 *
 * RULES ENFORCED HERE
 * -------------------
 * #29  Motion Explains Hierarchy
 * #30  Empty State Has Emotion
 * #31  Runtime Owns Truth
 * #44  Stable Key Never Changes
 * #46  Runtime Never Creates Identity — identity comes from contract
 * #51  Ranking Never Filters Identity — section order respects contract
 * #53  Diagnostics Never Affects Engine
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no Compose, no Coroutine, no Flow. Pure Kotlin.
 *
 * © RIKO Experience OS — Rendering Tier
 */

package com.rikodev.music.android.presentation.library.rendering.engine

import com.rikodev.music.android.presentation.library.contract.CollectionContract
import com.rikodev.music.android.presentation.library.contract.CollectionHeaderContract
import com.rikodev.music.android.presentation.library.contract.CollectionPersonality
import com.rikodev.music.android.presentation.library.contract.CollectionSectionContract
import com.rikodev.music.android.presentation.library.contract.CollectionType
import com.rikodev.music.android.presentation.library.contract.CollectionVisibility
import com.rikodev.music.android.presentation.library.rendering.layout.ResolvedLayout
import com.rikodev.music.android.presentation.library.rendering.layout.SectionLayoutRegistry
import com.rikodev.music.android.presentation.library.rendering.model.LibraryRenderSnapshot
import com.rikodev.music.android.presentation.library.rendering.model.RenderDiagnosticsSummary
import com.rikodev.music.android.presentation.library.rendering.model.RenderHeader
import com.rikodev.music.android.presentation.library.rendering.model.RenderItem
import com.rikodev.music.android.presentation.library.rendering.model.RenderItemBadge
import com.rikodev.music.android.presentation.library.rendering.model.RenderItemBadgeKind
import com.rikodev.music.android.presentation.library.rendering.model.RenderItemKind
import com.rikodev.music.android.presentation.library.rendering.model.RenderItemMetadata
import com.rikodev.music.android.presentation.library.rendering.model.RenderItemMotionHint
import com.rikodev.music.android.presentation.library.rendering.model.RenderSection
import com.rikodev.music.android.presentation.library.rendering.model.RenderSectionFlags
import com.rikodev.music.android.presentation.library.rendering.model.HeaderMotionHint
import com.rikodev.music.android.presentation.library.rendering.model.LoadingReason
import com.rikodev.music.android.presentation.library.rendering.model.ErrorReason
import com.rikodev.music.android.presentation.library.rendering.motion.RenderMotion
import com.rikodev.music.android.presentation.library.rendering.state.EmptyAction
import com.rikodev.music.android.presentation.library.rendering.state.EmptyIllustrationHint
import com.rikodev.music.android.presentation.library.rendering.state.LibraryEmptyKind
import com.rikodev.music.android.presentation.library.rendering.state.RenderEmptySectionKind
import com.rikodev.music.android.presentation.library.rendering.state.RenderEmptyState
import com.rikodev.music.android.presentation.library.rendering.state.RenderEmptyTone
import com.rikodev.music.android.presentation.library.rendering.state.RenderVisualState
import com.rikodev.music.android.presentation.library.rendering.theme.RenderItemAccessibility
import com.rikodev.music.android.presentation.library.rendering.theme.RenderLiveRegion
import com.rikodev.music.android.presentation.library.rendering.theme.RenderSemanticRole
import com.rikodev.music.android.presentation.library.rendering.theme.RenderSnapshotAccessibility
import com.rikodev.music.android.presentation.library.rendering.theme.RenderSurfaceVariant
import com.rikodev.music.android.presentation.library.rendering.theme.RenderTheme
import com.rikodev.music.android.presentation.library.rendering.theme.RenderThemeResolver
import com.rikodev.music.android.presentation.library.runtime.CollectionRuntimePhase
import com.rikodev.music.android.presentation.library.runtime.CollectionRuntimeState
import com.rikodev.music.android.presentation.library.runtime.CollectionRuntimeSnapshot
import com.rikodev.music.android.presentation.library.runtime.CardSnapshot
import androidx.compose.ui.unit.Dp

/**
 * The renderer orchestrator.
 *
 * Pure — no side effects, no Android dependencies. The host (B5+)
 * constructs a [SnapshotRenderer] once and calls [render] whenever
 * the runtime state changes.
 *
 * @property surfaceVariant  Active surface variant — drives theme resolution.
 * @property previousSnapshot Cached previous snapshot for diff computation.
 *                             Updated by [render]; do not mutate externally.
 */
class SnapshotRenderer(
    private val surfaceVariant: RenderSurfaceVariant = RenderSurfaceVariant.DARK,
) {

    private var previousSnapshot: LibraryRenderSnapshot? = null

    /**
     * Render a [CollectionRuntimeState] into a [LibraryRenderSnapshot].
     *
     * This is the single entry point. The host calls this whenever
     * the runtime state changes; the renderer produces a new snapshot
     * and a diff against the previous one.
     *
     * @property runtimeState   The current runtime state. May be COLD_START.
     * @property nowMs          Wall-clock time (epoch ms). The renderer never
     *                          reads the clock — the host passes it.
     * @return                  A [RenderResult] containing the new snapshot
     *                          and the diff against the previous render.
     */
    fun render(
        runtimeState: CollectionRuntimeState,
        nowMs: Long,
    ): RenderResult {
        val renderStartMs = nowMs
        val visualState = resolveVisualState(runtimeState)
        val header = resolveHeader(runtimeState, visualState)
        val sections = resolveSections(runtimeState, visualState)
        val fingerprint = computeFingerprint(runtimeState, visualState)

        val reusedCount = countReusedSections(sections)
        val computedCount = sections.size - reusedCount

        val snapshot = LibraryRenderSnapshot(
            schemaVersion = LibraryRenderSnapshot.CURRENT_SCHEMA_VERSION,
            state = visualState,
            sections = sections,
            header = header,
            createdAt = nowMs,
            sourceFingerprint = fingerprint,
            diagnostics = RenderDiagnosticsSummary(
                renderDurationMs = 0L, // host fills in after timing the call
                reusedSections = reusedCount,
                computedSections = computedCount,
                changedItems = 0, // computed by diff below
                relayoutedItems = 0, // computed by diff below
                note = buildDiagnosticNote(visualState, sections),
            ),
        )

        val diff = RenderDiffCalculator.compute(
            oldSnapshot = previousSnapshot?.asSnapshotLike(),
            newSnapshot = snapshot.asSnapshotLike(),
        )

        val snapshotWithDiagnostics = snapshot.copy(
            diagnostics = snapshot.diagnostics.copy(
                changedItems = diff.totalChangedItems,
                relayoutedItems = diff.sectionDiffs.count {
                    it is SectionDiff.Changed && it.layoutChanged
                },
            )
        )

        previousSnapshot = snapshotWithDiagnostics

        return RenderResult(
            snapshot = snapshotWithDiagnostics,
            diff = diff,
            renderDurationMs = 0L, // host fills in
        )
    }

    // -----------------------------------------------------------------
    // VISUAL STATE RESOLUTION
    // -----------------------------------------------------------------

    private fun resolveVisualState(
        runtimeState: CollectionRuntimeState,
    ): RenderVisualState = when (runtimeState.phase) {
        CollectionRuntimePhase.COLD_START -> RenderVisualState.Loading(
            reason = LoadingReason.COLD_START,
            estimatedProgress = 0f,
            visibleSectionsHint = runtimeState.sections.size.coerceAtLeast(3),
        )

        CollectionRuntimePhase.HYDRATING -> RenderVisualState.Loading(
            reason = LoadingReason.ENGINE_INVALIDATION,
            estimatedProgress = 0.3f,
            visibleSectionsHint = runtimeState.sections.size,
        )

        CollectionRuntimePhase.PARTIAL_READY -> RenderVisualState.Ready(
            isPartial = true,
            refreshingCount = runtimeState.refreshStates.values.count { it.isRefreshing },
        )

        CollectionRuntimePhase.READY -> RenderVisualState.Ready.FULL

        CollectionRuntimePhase.ERROR -> RenderVisualState.Error(
            reason = ErrorReason.ENGINE_FAILURE,
            title = "Library couldn't load",
            message = runtimeState.errorMessage ?: "Something went wrong.",
            retryAction = com.rikodev.music.android.presentation.library.rendering.state.ErrorRetryAction(
                label = "Try again",
                actionKey = "library.retry",
            ),
        )

        CollectionRuntimePhase.EMPTY -> resolveEmptyState(runtimeState)

        CollectionRuntimePhase.PERMISSION_REQUIRED ->
            RenderVisualState.PermissionRequired(
                permissionKey = "android.permission.READ_MEDIA_AUDIO",
                title = "Music access needed",
                message = "RIKO needs access to your music library to build collections.",
                grantAction = com.rikodev.music.android.presentation.library.rendering.state.PermissionAction(
                    label = "Grant access",
                    actionKey = "permission.grant",
                ),
                settingsAction = com.rikodev.music.android.presentation.library.rendering.state.PermissionAction(
                    label = "Open settings",
                    actionKey = "permission.settings",
                ),
            )
    }

    private fun resolveEmptyState(
        runtimeState: CollectionRuntimeState,
    ): RenderVisualState.Empty {
        val kind = if (runtimeState.activeContracts.isEmpty()) {
            LibraryEmptyKind.NO_COLLECTIONS
        } else {
            LibraryEmptyKind.FIRST_LAUNCH
        }

        return RenderVisualState.Empty(
            emptyKind = kind,
            title = resolveEmptyTitle(kind),
            message = resolveEmptyMessage(kind),
            illustrationHint = resolveEmptyIllustration(kind),
            primaryAction = resolveEmptyPrimaryAction(kind),
            secondaryAction = resolveEmptySecondaryAction(kind),
        )
    }

    private fun resolveEmptyTitle(kind: LibraryEmptyKind): String = when (kind) {
        LibraryEmptyKind.FIRST_LAUNCH -> "Your library is silent"
        LibraryEmptyKind.ALL_DELETED -> "Your library went quiet"
        LibraryEmptyKind.PERMISSION_REVOKED -> "Music is locked away"
        LibraryEmptyKind.NO_COLLECTIONS -> "Nothing to curate yet"
        LibraryEmptyKind.FILTER_NO_MATCH -> "No matches"
    }

    private fun resolveEmptyMessage(kind: LibraryEmptyKind): String = when (kind) {
        LibraryEmptyKind.FIRST_LAUNCH ->
            "RIKO listens to your music and shapes collections around your taste. Bring your songs in — the silence will end."
        LibraryEmptyKind.ALL_DELETED ->
            "All your songs were removed. The collections will rebuild the moment music returns."
        LibraryEmptyKind.PERMISSION_REVOKED ->
            "Music access was revoked. RIKO can't see your songs until you re-grant permission."
        LibraryEmptyKind.NO_COLLECTIONS ->
            "Your songs are here, but RIKO couldn't shape them into collections yet. Try refreshing."
        LibraryEmptyKind.FILTER_NO_MATCH ->
            "No collections match the current filter. Try widening it."
    }

    private fun resolveEmptyIllustration(kind: LibraryEmptyKind): EmptyIllustrationHint = when (kind) {
        LibraryEmptyKind.FIRST_LAUNCH -> EmptyIllustrationHint.WELCOMING
        LibraryEmptyKind.ALL_DELETED -> EmptyIllustrationHint.EMPTY_SHELF
        LibraryEmptyKind.PERMISSION_REVOKED -> EmptyIllustrationHint.LOCKED_VAULT
        LibraryEmptyKind.NO_COLLECTIONS -> EmptyIllustrationHint.SEARCHING
        LibraryEmptyKind.FILTER_NO_MATCH -> EmptyIllustrationHint.FILTER_EMPTY
    }

    private fun resolveEmptyPrimaryAction(kind: LibraryEmptyKind): EmptyAction? = when (kind) {
        LibraryEmptyKind.FIRST_LAUNCH -> EmptyAction(
            label = "Import music",
            actionKey = "library.import",
            isPrimary = true,
        )
        LibraryEmptyKind.ALL_DELETED -> EmptyAction(
            label = "Import music",
            actionKey = "library.import",
            isPrimary = true,
        )
        LibraryEmptyKind.PERMISSION_REVOKED -> EmptyAction(
            label = "Grant access",
            actionKey = "permission.grant",
            isPrimary = true,
        )
        LibraryEmptyKind.NO_COLLECTIONS -> EmptyAction(
            label = "Refresh",
            actionKey = "library.refresh",
            isPrimary = true,
        )
        LibraryEmptyKind.FILTER_NO_MATCH -> null
    }

    private fun resolveEmptySecondaryAction(kind: LibraryEmptyKind): EmptyAction? = when (kind) {
        LibraryEmptyKind.PERMISSION_REVOKED -> EmptyAction(
            label = "Open settings",
            actionKey = "permission.settings",
            isPrimary = false,
        )
        LibraryEmptyKind.FILTER_NO_MATCH -> EmptyAction(
            label = "Clear filter",
            actionKey = "library.clearFilter",
            isPrimary = false,
        )
        else -> null
    }

    // -----------------------------------------------------------------
    // HEADER RESOLUTION
    // -----------------------------------------------------------------

    private fun resolveHeader(
        runtimeState: CollectionRuntimeState,
        visualState: RenderVisualState,
    ): RenderHeader {
        val contract = runtimeState.header
        val personalityKey = "minimal_architect" // header is system-personality
        val theme = RenderThemeResolver.resolve(
            personalityKey = personalityKey,
            surfaceVariant = surfaceVariant,
            hierarchyWeight = 1.0f, // header is highest hierarchy
        )

        val motionHint = when {
            previousSnapshot == null -> HeaderMotionHint.ENTER_FROM_TOP
            visualState is RenderVisualState.Loading -> HeaderMotionHint.SUBTITLE_CROSSFADE
            visualState is RenderVisualState.Ready && previousSnapshot?.state is RenderVisualState.Loading ->
                HeaderMotionHint.EXPAND
            else -> HeaderMotionHint.NONE
        }

        val headerTitle = contract.greeting.displayText.ifEmpty { "Library" }
        val headerSubtitle = contract.subtitle.displayText.ifEmpty { null }

        return RenderHeader(
            title = headerTitle,
            subtitle = headerSubtitle,
            accentHex = theme.accentHex,
            motionHint = motionHint,
            isVisible = true,
        )
    }

    // -----------------------------------------------------------------
    // SECTION RESOLUTION
    // -----------------------------------------------------------------

    private fun resolveSections(
        runtimeState: CollectionRuntimeState,
        visualState: RenderVisualState,
    ): List<RenderSection> {
        if (visualState !is RenderVisualState.Ready) return emptyList()

        return runtimeState.sections.mapIndexed { index, sectionContract ->
            resolveSection(
                sectionContract = sectionContract,
                runtimeState = runtimeState,
                ordinal = index,
            )
        }
    }

    private fun resolveSection(
        sectionContract: CollectionSectionContract,
        runtimeState: CollectionRuntimeState,
        ordinal: Int,
    ): RenderSection {
        val stableKey = sectionContract.hostedCollectionIds.firstOrNull()
            ?: sectionContract.sectionId
        val collectionContract = runtimeState.contractFor(stableKey)
            ?: return placeholderSection(sectionContract, stableKey, ordinal)

        val runtimeSnapshot = runtimeState.snapshotFor(stableKey)
            ?: return placeholderSection(sectionContract, stableKey, ordinal)

        val personality = collectionContract.personality

        val resolvedLayout: ResolvedLayout = SectionLayoutRegistry.resolve(
            personality = personality,
            itemCount = runtimeSnapshot.cards.size,
        )

        val motion = resolveMotion(personality, ordinal, runtimeSnapshot)
        val theme = RenderThemeResolver.resolve(
            personalityKey = personality.stableKey ?: "",
            surfaceVariant = surfaceVariant,
            hierarchyWeight = motion.hierarchyWeight,
        )

        val items = resolveItems(
            cards = runtimeSnapshot.cards,
            sectionContract = sectionContract,
            collectionContract = collectionContract,
            theme = theme,
            stableKey = stableKey,
        )

        val emptyState = if (items.isEmpty()) {
            resolveSectionEmptyState(collectionContract)
        } else {
            RenderEmptyState.FALLBACK
        }

        val flags = RenderSectionFlags(
            isSticky = ordinal == 0,
            isNewlyAdded = false, // computed via diff
            wasReordered = false, // computed via diff
            isDegraded = runtimeSnapshot.diagnostics.warnings.isNotEmpty(),
            isFeatured = ordinal == 0,
            isActive = false, // host sets this based on playback
        )

        return RenderSection(
            stableKey = stableKey,
            sectionId = sectionContract.sectionId,
            title = collectionContract.type.displayName,
            subtitle = sectionContract.subtitle.ifEmpty { null },
            ordinal = ordinal,
            layout = resolvedLayout.layout,
            motion = motion,
            items = items,
            emptyState = emptyState,
            accentHex = theme.accentHex,
            elevationDp = theme.elevationDp,
            isVisible = runtimeSnapshot.isVisible,
            isRefreshing = runtimeSnapshot.isRefreshing,
            totalCount = runtimeSnapshot.totalAvailable,
            renderFlags = flags,
        )
    }

    private fun placeholderSection(
        sectionContract: CollectionSectionContract,
        stableKey: String,
        ordinal: Int,
    ): RenderSection = RenderSection(
        stableKey = stableKey,
        sectionId = sectionContract.sectionId,
        title = "",
        subtitle = null,
        ordinal = ordinal,
        layout = SectionLayoutRegistry.resolve(
            personality = CollectionPersonality.MINIMAL,
            itemCount = 0,
        ).layout,
        motion = RenderMotion.LOADING_SHIMMER,
        items = emptyList(),
        emptyState = RenderEmptyState.FALLBACK,
        accentHex = "#888888",
        elevationDp = 0f,
        isVisible = false,
        isRefreshing = false,
        totalCount = 0,
        renderFlags = RenderSectionFlags.DEFAULT,
    )

    // -----------------------------------------------------------------
    // ITEM RESOLUTION
    // -----------------------------------------------------------------

    private fun resolveItems(
        cards: List<CardSnapshot>,
        sectionContract: CollectionSectionContract,
        collectionContract: CollectionContract,
        theme: RenderTheme,
        stableKey: String,
    ): List<RenderItem> = cards.mapIndexed { index, card ->
        resolveItem(
            card = card,
            sectionContract = sectionContract,
            collectionContract = collectionContract,
            theme = theme,
            stableKey = stableKey,
            ordinal = index,
        )
    }

    private fun resolveItem(
        card: CardSnapshot,
        sectionContract: CollectionSectionContract,
        collectionContract: CollectionContract,
        theme: RenderTheme,
        stableKey: String,
        ordinal: Int,
    ): RenderItem {
        val kind = resolveItemKind(collectionContract.type)
        val metadata = RenderItemMetadata.EMPTY
            .with(RenderItemMetadata.KEY_ENTITY_ID, card.cardId)
            .with(RenderItemMetadata.KEY_ENTITY_KIND, kind.name)
            .with(RenderItemMetadata.KEY_PARENT_COLLECTION, stableKey)
            .with(RenderItemMetadata.KEY_PARENT_SECTION, sectionContract.sectionId)
            .with(
                RenderItemMetadata.KEY_SOURCE_STRATEGY,
                collectionContract.type.name,
            )

        val badges = mutableListOf<RenderItemBadge>()
        if (card.isPlaying) badges.add(RenderItemBadge.PLAYING)
        if (card.isActive && !card.isPlaying) {
            badges.add(RenderItemBadge(RenderItemBadgeKind.FAVORITE, null, priority = 5))
        }

        val motionHint = when {
            card.isPlaying -> RenderItemMotionHint.PLAYING
            card.isSelected -> RenderItemMotionHint.PRESS
            else -> RenderItemMotionHint.NONE
        }

        return RenderItem(
            itemId = card.cardId,
            kind = kind,
            title = card.title,
            subtitle = card.subtitle.ifEmpty { null },
            tertiaryLabel = null,
            artwork = com.rikodev.music.android.presentation.library.rendering.model.RenderItemArtwork.placeholder(card.cardId),
            badges = badges.sortedBy { it.priority },
            accentHex = card.accentColorHex ?: theme.accentHex,
            motionHint = motionHint,
            isSelected = card.isSelected,
            isPlaying = card.isPlaying,
            isFocused = false,
            isEnabled = true,
            metadata = metadata,
        )
    }

    private fun resolveItemKind(collectionType: CollectionType): RenderItemKind = when (collectionType) {
        CollectionType.CONTINUE_LISTENING -> RenderItemKind.SONG
        CollectionType.FAVORITES -> RenderItemKind.SONG
        CollectionType.HIDDEN_GEMS -> RenderItemKind.SONG
        CollectionType.FORGOTTEN_CLASSICS -> RenderItemKind.SONG
        CollectionType.MOOD_BASED -> RenderItemKind.MOOD
        CollectionType.DISCOVERY -> RenderItemKind.SONG
        CollectionType.MOST_PLAYED -> RenderItemKind.SONG
        CollectionType.RECENTLY_ADDED -> RenderItemKind.ALBUM
        CollectionType.RECENTLY_PLAYED -> RenderItemKind.SONG
        CollectionType.PLAYLIST_USER, CollectionType.PLAYLIST_AUTO -> RenderItemKind.PLAYLIST
        CollectionType.ALL_ALBUMS -> RenderItemKind.ALBUM
        CollectionType.ALL_ARTISTS, CollectionType.ARTIST_SPOTLIGHT -> RenderItemKind.ARTIST
        CollectionType.GENRE_DEEP_DIVE -> RenderItemKind.GENRE
        CollectionType.DECADE_MIX, CollectionType.TIME_TRAVEL -> RenderItemKind.TIME_PERIOD
        else -> RenderItemKind.SONG
    }

    // -----------------------------------------------------------------
    // SECTION EMPTY STATE
    // -----------------------------------------------------------------

    private fun resolveSectionEmptyState(
        collectionContract: CollectionContract,
    ): RenderEmptyState {
        val kind = resolveEmptySectionKind(collectionContract.type)
        return RenderEmptyState(
            sectionKind = kind,
            title = resolveSectionEmptyTitle(collectionContract, kind),
            message = resolveSectionEmptyMessage(collectionContract, kind),
            illustrationHint = EmptyIllustrationHint.EMPTY_SHELF,
            primaryAction = resolveSectionEmptyAction(collectionContract),
            tone = kind.defaultTone,
            isRecoverable = kind != RenderEmptySectionKind.GENERIC,
        )
    }

    private fun resolveEmptySectionKind(type: CollectionType): RenderEmptySectionKind = when (type) {
        CollectionType.CONTINUE_LISTENING -> RenderEmptySectionKind.CONTINUE_LISTENING
        CollectionType.FAVORITES -> RenderEmptySectionKind.FAVORITES
        CollectionType.HIDDEN_GEMS -> RenderEmptySectionKind.HIDDEN_GEMS
        CollectionType.FORGOTTEN_CLASSICS -> RenderEmptySectionKind.FORGOTTEN_CLASSICS
        CollectionType.MOOD_BASED -> RenderEmptySectionKind.MOOD_BASED
        CollectionType.DISCOVERY -> RenderEmptySectionKind.DISCOVERY
        CollectionType.MOST_PLAYED -> RenderEmptySectionKind.MOST_PLAYED
        CollectionType.RECENTLY_ADDED -> RenderEmptySectionKind.RECENTLY_ADDED
        CollectionType.RECENTLY_PLAYED -> RenderEmptySectionKind.RECENTLY_PLAYED
        CollectionType.PLAYLIST_USER, CollectionType.PLAYLIST_AUTO -> RenderEmptySectionKind.PLAYLISTS
        else -> RenderEmptySectionKind.GENERIC
    }

    private fun resolveSectionEmptyTitle(
        contract: CollectionContract,
        kind: RenderEmptySectionKind,
    ): String = when (kind) {
        RenderEmptySectionKind.CONTINUE_LISTENING -> "Pick up where you left off"
        RenderEmptySectionKind.FAVORITES -> "Nothing favorited yet"
        RenderEmptySectionKind.HIDDEN_GEMS -> "Nothing hiding yet"
        RenderEmptySectionKind.FORGOTTEN_CLASSICS -> "No classics surfacing"
        RenderEmptySectionKind.MOOD_BASED -> "No mood matches right now"
        RenderEmptySectionKind.DISCOVERY -> "Nothing to discover yet"
        RenderEmptySectionKind.MOST_PLAYED -> "No play history yet"
        RenderEmptySectionKind.RECENTLY_ADDED -> "No new songs"
        RenderEmptySectionKind.RECENTLY_PLAYED -> "Nothing played recently"
        RenderEmptySectionKind.PLAYLISTS -> "No playlists yet"
        RenderEmptySectionKind.GENERIC -> "Nothing here yet"
    }

    private fun resolveSectionEmptyMessage(
        contract: CollectionContract,
        kind: RenderEmptySectionKind,
    ): String = when (kind) {
        RenderEmptySectionKind.CONTINUE_LISTENING ->
            "Play a few songs — RIKO will remember where you stopped."
        RenderEmptySectionKind.FAVORITES ->
            "Tap the heart on any song to plant it here."
        RenderEmptySectionKind.HIDDEN_GEMS ->
            "RIKO surfaces songs you've loved but forgotten. Listen more to fill this shelf."
        RenderEmptySectionKind.FORGOTTEN_CLASSICS ->
            "Songs you haven't heard in months will resurface here."
        RenderEmptySectionKind.MOOD_BASED ->
            "No collection matches your current mood. Try again later."
        RenderEmptySectionKind.DISCOVERY ->
            "Listen to more songs — RIKO will find new things to show you."
        RenderEmptySectionKind.MOST_PLAYED ->
            "Your most-played songs will land here."
        RenderEmptySectionKind.RECENTLY_ADDED ->
            "New songs you import will appear here."
        RenderEmptySectionKind.RECENTLY_PLAYED ->
            "Songs you've played recently will show up here."
        RenderEmptySectionKind.PLAYLISTS ->
            "Create a playlist or import one — it will live here."
        RenderEmptySectionKind.GENERIC ->
            "This section is empty."
    }

    private fun resolveSectionEmptyAction(
        contract: CollectionContract,
    ): EmptyAction? = when (contract.type) {
        CollectionType.FAVORITES -> EmptyAction(
            label = "Browse songs",
            actionKey = "library.browseSongs",
            isPrimary = true,
        )
        CollectionType.PLAYLIST_USER, CollectionType.PLAYLIST_AUTO -> EmptyAction(
            label = "New playlist",
            actionKey = "playlist.create",
            isPrimary = true,
        )
        CollectionType.DISCOVERY -> EmptyAction(
            label = "Refresh",
            actionKey = "library.refresh",
            isPrimary = true,
        )
        else -> null
    }

    // -----------------------------------------------------------------
    // MOTION RESOLUTION
    // -----------------------------------------------------------------

    private fun resolveMotion(
        personality: CollectionPersonality,
        ordinal: Int,
        snapshot: CollectionRuntimeSnapshot,
    ): RenderMotion {
        val hierarchyWeight = when (ordinal) {
            0 -> 1.0f
            1 -> 0.8f
            2 -> 0.65f
            else -> 0.5f
        }

        if (snapshot.isEmpty) return RenderMotion.EMPTY_QUIET.copy(
            hierarchyWeight = hierarchyWeight,
            priority = ordinal,
        )

        val cadence = when (personality.cadence) {
            com.rikodev.music.android.presentation.library.contract.AnimationCadence.SNAPPY ->
                com.rikodev.music.android.presentation.library.rendering.motion.MotionCadence.SNAPPY
            com.rikodev.music.android.presentation.library.contract.AnimationCadence.BALANCED ->
                com.rikodev.music.android.presentation.library.rendering.motion.MotionCadence.BALANCED
            com.rikodev.music.android.presentation.library.contract.AnimationCadence.GENTLE ->
                com.rikodev.music.android.presentation.library.rendering.motion.MotionCadence.GENTLE
            com.rikodev.music.android.presentation.library.contract.AnimationCadence.DREAMY ->
                com.rikodev.music.android.presentation.library.rendering.motion.MotionCadence.DREAMY
            com.rikodev.music.android.presentation.library.contract.AnimationCadence.DRIFTING ->
                com.rikodev.music.android.presentation.library.rendering.motion.MotionCadence.DRIFTING
            else -> com.rikodev.music.android.presentation.library.rendering.motion.MotionCadence.BALANCED
        }

        val entrance = when (ordinal) {
            0 -> com.rikodev.music.android.presentation.library.rendering.motion.MotionEntrance.FADE_IN_SCALE
            1, 2 -> com.rikodev.music.android.presentation.library.rendering.motion.MotionEntrance.FADE_IN_UP
            else -> com.rikodev.music.android.presentation.library.rendering.motion.MotionEntrance.FADE_IN_ONLY
        }

        val scrollBehavior = when (ordinal) {
            0 -> com.rikodev.music.android.presentation.library.rendering.motion.MotionScrollBehavior.PARALLAX_STRONG
            1 -> com.rikodev.music.android.presentation.library.rendering.motion.MotionScrollBehavior.PARALLAX_SUBTLE
            else -> com.rikodev.music.android.presentation.library.rendering.motion.MotionScrollBehavior.STATIC
        }

        return RenderMotion(
            entrance = entrance,
            scrollBehavior = scrollBehavior,
            itemEntrance = com.rikodev.music.android.presentation.library.rendering.motion.MotionItemEntrance.STAGGERED,
            refreshBehavior = if (snapshot.isRefreshing) {
                com.rikodev.music.android.presentation.library.rendering.motion.MotionRefreshBehavior.SHIMMER
            } else {
                com.rikodev.music.android.presentation.library.rendering.motion.MotionRefreshBehavior.SUBTLE_PULSE
            },
            cadence = cadence,
            hierarchyWeight = hierarchyWeight,
            priority = ordinal,
        )
    }

    // -----------------------------------------------------------------
    // DIAGNOSTICS HELPERS
    // -----------------------------------------------------------------

    private fun computeFingerprint(
        runtimeState: CollectionRuntimeState,
        visualState: RenderVisualState,
    ): String {
        val phaseKey = runtimeState.phase.name
        val sectionsKey = runtimeState.sections.joinToString(",") { it.sectionId }
        val cardsKey = runtimeState.snapshots.values
            .joinToString(",") { "${it.stableKey ?: ""}:${it.cards.size}" }
        val stateKey = visualState::class.simpleName
        return "$phaseKey|$stateKey|$sectionsKey|$cardsKey"
    }

    private fun countReusedSections(sections: List<RenderSection>): Int {
        val prev = previousSnapshot ?: return 0
        val prevKeys = prev.sections.map { it.stableKey ?: "" }.toSet()
        return sections.count { it.stableKey ?: "" in prevKeys }
    }

    private fun buildDiagnosticNote(
        visualState: RenderVisualState,
        sections: List<RenderSection>,
    ): String? {
        if (visualState !is RenderVisualState.Ready) return null
        if (sections.isEmpty()) return "Ready but no sections resolved"
        val empties = sections.count { it.isEmpty }
        if (empties == 0) return null
        return "$empties empty section(s) in snapshot"
    }
}

/**
 * The result of a [SnapshotRenderer.render] call.
 *
 * @property snapshot          The newly produced render snapshot.
 * @property diff              Diff against the previous render.
 *                              [RenderDiff.NO_CHANGE] on first render.
 * @property renderDurationMs  Wall-clock duration of the render call.
 *                              Filled by the host after timing.
 */
data class RenderResult(
    val snapshot: LibraryRenderSnapshot,
    val diff: RenderDiff,
    val renderDurationMs: Long,
)

// ---------------------------------------------------------------------------
// SNAPSHOT → SNAPSHOT-LIKE ADAPTERS
// ---------------------------------------------------------------------------
// These adapters bridge the real [LibraryRenderSnapshot] types to the
// lightweight [SnapshotLike] / [SectionLike] / [ItemLike] interfaces
// consumed by [RenderDiffCalculator]. This keeps the diff calculator
// testable in isolation.
// ---------------------------------------------------------------------------

fun LibraryRenderSnapshot.asSnapshotLike(): SnapshotLike = SnapshotLikeImpl(this)

private class SnapshotLikeImpl(
    private val snapshot: LibraryRenderSnapshot,
) : SnapshotLike {
    override val stateName: String
        get() = snapshot.state::class.simpleName ?: "Unknown"
    override val totalItemCount: Int
        get() = snapshot.totalItemCount
    override val header: HeaderLike
        get() = HeaderLikeImpl(snapshot.header)
    override val state: StateLike
        get() = StateLikeImpl(stateName)
    override val sections: List<SectionLike>
        get() = snapshot.sections.map { SectionLikeImpl(it) }
}

private class HeaderLikeImpl(
    private val header: RenderHeader,
) : HeaderLike {
    override val title: String get() = header.title
    override val subtitle: String? get() = header.subtitle
    override val accentHex: String get() = header.accentHex
    override val motionHint: String get() = header.motionHint.name
    override val isVisible: Boolean get() = header.isVisible
}

private class StateLikeImpl(
    override val stateName: String,
) : StateLike

private class SectionLikeImpl(
    private val section: RenderSection,
) : SectionLike {
    override val stableKey: String get() = section.stableKey ?: ""
    override val layoutKey: String get() = section.layout::class.simpleName ?: "Unknown"
    override val motionKey: String get() = section.motion.cadence.name
    override val accentHex: String get() = section.accentHex
    override val isVisible: Boolean get() = section.isVisible
    override val isRefreshing: Boolean get() = section.isRefreshing
    override val items: List<ItemLike>
        get() = section.items.map { ItemLikeImpl(it) }
}

private class ItemLikeImpl(
    private val item: RenderItem,
) : ItemLike {
    override val itemId: String get() = item.itemId
    override val title: String get() = item.title
    override val subtitle: String? get() = item.subtitle
    override val artworkKey: String get() = item.artwork.artworkKey
    override val badgesKey: String
        get() = item.badges.joinToString(",") { it.kind.name }
    override val isSelected: Boolean get() = item.isSelected
    override val isPlaying: Boolean get() = item.isPlaying
    override val contentHash: String
        get() = "${item.title}|${item.subtitle ?: ""}|${item.artwork.artworkKey}|${badgesKey}|${item.isSelected}|${item.isPlaying}"
}
