/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B4 — Library Rendering Runtime
 *
 * File 12 / 13 — SectionLayoutRegistry.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [SectionLayoutRegistry] maps a [CollectionPresentation] (contract
 * layer) + item count + device class hint into a concrete
 * [RenderLayout] (render layer).
 *
 * This is the single source of truth for "given this contract's
 * presentation tokens, what concrete layout should the UI apply?".
 * Centralizing this decision means:
 *
 *   1. The renderer (file 11) does not contain layout decision logic.
 *   2. Layout overrides (item-count fallbacks, accessibility
 *      overrides, device-class overrides) live in one place.
 *   3. The Inspector overlay can surface *why* a layout was chosen
 *      via [ResolvedLayout.sourceHint].
 *
 * RULES ENFORCED HERE
 * -------------------
 * #33  Collection Has Identity     — layout follows contract identity
 * #53  Diagnostics Never Affects Engine — sourceHint is observation only
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no Compose, no Coroutine, no Flow. Pure Kotlin.
 *
 * © RIKO Experience OS — Rendering Tier
 */

package com.rikodev.music.android.presentation.library.rendering.layout

import com.rikodev.music.android.presentation.library.contract.CollectionPersonality
import com.rikodev.music.android.presentation.library.contract.CardLayoutPreference

/**
 * The registry that resolves a [RenderLayout] from contract tokens.
 *
 * Pure object — no state, no side effects. The renderer calls
 * [resolve] for each section; the registry returns a [ResolvedLayout]
 * containing the layout + the reason it was chosen.
 *
 * Override priority (highest first):
 *   1. Accessibility override (e.g. reduce motion → list)
 *   2. Item-count override (too few items → smaller layout)
 *   3. Device-class override (tablet vs phone) — reserved for future
 *   4. Contract preference
 *   5. Default fallback
 */
object SectionLayoutRegistry {

    /**
     * The minimum number of items required for each layout variant.
     * Below the threshold, the registry downgrades to a smaller layout.
     *
     * Tunable — these values are deliberately conservative to avoid
     * "lonely grid" syndromes (a 2x2 grid with only 1 card looks bad).
     */
    private const val GRID_2_MIN_ITEMS = 4
    private const val GRID_3_MIN_ITEMS = 6
    private const val STRIP_MIN_ITEMS = 2
    private const val STACK_MIN_ITEMS = 3

    /**
     * Resolve a layout for the given personality + item count.
     *
     * @property personality     The contract's personality (carries preferredLayout).
     * @property itemCount      Number of items currently in the section.
     * @property reduceMotion   True if the user has enabled "reduce motion"
     *                          accessibility. Forces a static layout.
     */
    fun resolve(
        personality: CollectionPersonality,
        itemCount: Int,
        reduceMotion: Boolean = false,
    ): ResolvedLayout {
        val contractLayout = layoutFromPreference(personality.preferredLayout)

        // 1. Accessibility override — reduce motion forces list.
        if (reduceMotion) {
            return ResolvedLayout(
                layout = RenderLayout.ListVertical(),
                sourceHint = LayoutSourceHint.ACCESSIBILITY_OVERRIDE,
            )
        }

        // 2. Item-count override — too few items for the chosen layout.
        val countAdjusted = adjustForItemCount(contractLayout, itemCount)
        if (countAdjusted != null) {
            return ResolvedLayout(
                layout = countAdjusted,
                sourceHint = LayoutSourceHint.ITEM_COUNT_OVERRIDE,
            )
        }

        // 3. Contract preference — happy path.
        return ResolvedLayout(
            layout = contractLayout,
            sourceHint = LayoutSourceHint.FROM_CONTRACT,
        )
    }

    /**
     * Map a [CardLayoutPreference] (from contract) to a [RenderLayout].
     */
    private fun layoutFromPreference(
        preference: CardLayoutPreference,
    ): RenderLayout {
        return when (preference) {
            CardLayoutPreference.HERO_STRIP -> RenderLayout.HeroStrip()
            CardLayoutPreference.COMPACT_STRIP -> RenderLayout.CompactStrip()
            CardLayoutPreference.GRID_SQUARE -> RenderLayout.GridSquare2()
            CardLayoutPreference.GRID_DENSE -> RenderLayout.GridDense3()
            CardLayoutPreference.LIST_VERTICAL -> RenderLayout.ListVertical()
            CardLayoutPreference.STACK_DEPTH -> RenderLayout.StackDepth()
        }
    }

    /**
     * If the chosen layout requires more items than available, return
     * a downgraded layout. Otherwise return null (keep the chosen layout).
     */
    private fun adjustForItemCount(
        layout: RenderLayout,
        itemCount: Int,
    ): RenderLayout? = when (layout) {
        is RenderLayout.GridSquare2 ->
            if (itemCount < GRID_2_MIN_ITEMS) RenderLayout.CompactStrip() else null

        is RenderLayout.GridDense3 ->
            if (itemCount < GRID_3_MIN_ITEMS) RenderLayout.GridSquare2() else null

        is RenderLayout.HeroStrip ->
            if (itemCount < STRIP_MIN_ITEMS) RenderLayout.HeroSingle() else null

        is RenderLayout.CompactStrip ->
            if (itemCount < STRIP_MIN_ITEMS) RenderLayout.HeroSingle() else null

        is RenderLayout.StackDepth ->
            if (itemCount < STACK_MIN_ITEMS) RenderLayout.HeroStrip() else null

        is RenderLayout.ListVertical -> null
        is RenderLayout.HeroSingle -> null
        is RenderLayout.TextList -> null
    }

    /**
     * Convenience: resolve a layout for an empty section.
     *
     * Empty sections always get a placeholder layout — the UI shows
     * the empty state instead of items, but the layout spec is still
     * needed for sizing the empty slot.
     */
    fun resolveForEmpty(
        personality: CollectionPersonality,
    ): ResolvedLayout = ResolvedLayout(
        layout = RenderLayout.HeroSingle(),
        sourceHint = LayoutSourceHint.DEFAULT_FALLBACK,
    )

    /**
     * Convenience: resolve a layout for a loading section.
     *
     * Loading sections get the contract's preferred layout so the
     * skeleton matches the eventual content shape.
     */
    fun resolveForLoading(
        personality: CollectionPersonality,
    ): ResolvedLayout = ResolvedLayout(
        layout = layoutFromPreference(personality.preferredLayout),
        sourceHint = LayoutSourceHint.FROM_CONTRACT,
    )
}

/**
 * Per-section layout decision log — for the Inspector overlay.
 *
 * Rule #53 — Diagnostics Never Affects Engine.
 * This is for observation only. The UI never branches on it.
 */
data class LayoutDecisionLog(
    val stableKey: String,
    val chosenLayout: String,
    val sourceHint: LayoutSourceHint,
    val itemCount: Int,
    val reduceMotion: Boolean,
) {
    val summary: String
        get() = "[$stableKey] → $chosenLayout ($sourceHint, items=$itemCount, reduceMotion=$reduceMotion)"
}
