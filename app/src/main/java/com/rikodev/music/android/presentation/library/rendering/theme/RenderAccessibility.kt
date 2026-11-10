/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B4 — Library Rendering Runtime
 *
 * File 9 / 13 — RenderAccessibility.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [RenderAccessibility] is the resolved accessibility contract for
 * a render item / section / snapshot.
 *
 * RIKO is an audio-first product. Accessibility is not a checkbox —
 * it is the difference between "I can use this" and "I can't" for
 * a non-trivial slice of the audience. The renderer pre-resolves
 * accessibility metadata so the UI never has to invent content
 * descriptions, focus orders, or semantic roles on the fly.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * This file does NOT specify platform accessibility APIs (TalkBack,
 * VoiceOver, etc.). Those are implementation details owned by the
 * UI layer (B5+). This file specifies *intent* — "this card is a
 * button", "this list is ordered", "this image is decorative".
 *
 * RULES ENFORCED HERE
 * -------------------
 * #44  Stable Key Never Changes    — focus keys are stable strings
 * #53  Diagnostics Never Affects Engine — accessibility is observation-only
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no Compose, no Coroutine, no Flow. Pure Kotlin data.
 *
 * © RIKO Experience OS — Rendering Tier
 */

package com.rikodev.music.android.presentation.library.rendering.theme

/**
 * Semantic role of a render element.
 *
 * Drives how the UI exposes the element to assistive technologies.
 * The UI maps each role to the platform's accessibility API
 * (e.g. Role.BUTTON → Role.Button in Compose).
 */
enum class RenderSemanticRole(
    internal val description: String,
    internal val isInteractive: Boolean,
) {
    /** A button — tap to perform an action. */
    BUTTON("Button — tap to perform an action.", isInteractive = true),

    /** A toggle — tap to toggle a boolean state. */
    TOGGLE("Toggle — tap to switch on/off.", isInteractive = true),

    /** A link — tap to navigate. */
    LINK("Link — tap to navigate.", isInteractive = true),

    /** An image — non-interactive, may have a description. */
    IMAGE("Image — non-interactive.", isInteractive = false),

    /** A decorative element — hidden from accessibility. */
    DECORATIVE("Decorative — hidden from accessibility tree.", isInteractive = false),

    /** A heading — used for section titles. */
    HEADING("Heading — section title.", isInteractive = false),

    /** A list item — composite role, may contain children. */
    LIST_ITEM("List item — may contain children.", isInteractive = true),

    /** A list container. */
    LIST("List container.", isInteractive = false),

    /** A tab — one of several options in a tab strip. */
    TAB("Tab — one of several options.", isInteractive = true),

    /** A slider — adjustable value. */
    SLIDER("Slider — adjustable value.", isInteractive = true),
}

/**
 * Resolved accessibility metadata for a render item.
 *
 * @property contentDescription  Spoken description for screen readers.
 *                               Already localized + personality-shaped.
 * @property role                Semantic role — drives accessibility API.
 * @property focusKey            Stable key for focus ordering. Rule #44.
 * @property focusOrderHint      Coarse order hint (lower = earlier).
 * @property isImportantForAccessibility  True if the element should be
 *                             exposed to screen readers.
 * @property isHeading          True if this element is a section heading.
 * @property liveRegion         Live-region mode (none / polite / assertive).
 * @property stateDescription   Optional spoken state (e.g. "Selected").
 */
data class RenderItemAccessibility(
    val contentDescription: String,
    val role: RenderSemanticRole,
    val focusKey: String,
    val focusOrderHint: Int,
    val isImportantForAccessibility: Boolean,
    val isHeading: Boolean,
    val liveRegion: RenderLiveRegion,
    val stateDescription: String?,
) {
    companion object {
        /**
         * Default accessibility for a standard interactive card.
         */
        fun defaultForCard(
            contentDescription: String,
            focusKey: String,
            focusOrderHint: Int,
            stateDescription: String? = null,
        ): RenderItemAccessibility = RenderItemAccessibility(
            contentDescription = contentDescription,
            role = RenderSemanticRole.LIST_ITEM,
            focusKey = focusKey,
            focusOrderHint = focusOrderHint,
            isImportantForAccessibility = true,
            isHeading = false,
            liveRegion = RenderLiveRegion.NONE,
            stateDescription = stateDescription,
        )

        /**
         * Accessibility for a section heading.
         */
        fun forHeading(
            contentDescription: String,
            focusKey: String,
            focusOrderHint: Int,
        ): RenderItemAccessibility = RenderItemAccessibility(
            contentDescription = contentDescription,
            role = RenderSemanticRole.HEADING,
            focusKey = focusKey,
            focusOrderHint = focusOrderHint,
            isImportantForAccessibility = true,
            isHeading = true,
            liveRegion = RenderLiveRegion.NONE,
            stateDescription = null,
        )

        /**
         * Accessibility for a decorative element — hidden from screen readers.
         */
        val DECORATIVE = RenderItemAccessibility(
            contentDescription = "",
            role = RenderSemanticRole.DECORATIVE,
            focusKey = "",
            focusOrderHint = Int.MAX_VALUE,
            isImportantForAccessibility = false,
            isHeading = false,
            liveRegion = RenderLiveRegion.NONE,
            stateDescription = null,
        )
    }
}

/**
 * Live-region mode for an element.
 *
 * Polite = screen reader announces changes when the user pauses.
 * Assertive = screen reader interrupts to announce changes.
 * None = changes are not announced.
 */
enum class RenderLiveRegion(internal val description: String) {
    /** Not a live region. */
    NONE("Not a live region."),

    /** Polite — announce when the user pauses. */
    POLITE("Polite announcement."),

    /** Assertive — interrupt to announce. */
    ASSERTIVE("Assertive announcement."),
}

/**
 * Resolved accessibility metadata for a render section.
 *
 * @property sectionHeading        Accessibility for the section's title.
 * @property listRole              Whether the section is exposed as a list.
 * @property itemCountDescription  Spoken description of item count
 *                                 (e.g. "12 songs").
 * @property focusOrderStart       Coarse focus-order start hint for items
 *                                 within this section.
 * @property isLandmark            True if the section is a navigation
 *                                 landmark (e.g. main content area).
 */
data class RenderSectionAccessibility(
    val sectionHeading: RenderItemAccessibility,
    val listRole: Boolean,
    val itemCountDescription: String,
    val focusOrderStart: Int,
    val isLandmark: Boolean,
) {
    companion object {
        fun default(
            sectionTitle: String,
            sectionKey: String,
            focusOrderStart: Int,
            itemCount: Int,
            itemKindLabel: String,
        ): RenderSectionAccessibility = RenderSectionAccessibility(
            sectionHeading = RenderItemAccessibility.forHeading(
                contentDescription = sectionTitle,
                focusKey = "section::$sectionKey",
                focusOrderHint = focusOrderStart,
            ),
            listRole = true,
            itemCountDescription = "$itemCount $itemKindLabel",
            focusOrderStart = focusOrderStart,
            isLandmark = false,
        )
    }
}

/**
 * Resolved accessibility metadata for the entire Library snapshot.
 *
 * @property snapshotTitle         Spoken title for the whole screen.
 * @property focusOrder            Ordered list of focus keys, top to bottom.
 * @property primaryActionKey      Stable key of the primary action on screen.
 * @property liveRegionForState    Live region for state changes (e.g. "Loading
 *                                 complete" announcements).
 */
data class RenderSnapshotAccessibility(
    val snapshotTitle: String,
    val focusOrder: List<String>,
    val primaryActionKey: String?,
    val liveRegionForState: RenderLiveRegion,
) {
    companion object {
        val DEFAULT = RenderSnapshotAccessibility(
            snapshotTitle = "Library",
            focusOrder = emptyList(),
            primaryActionKey = null,
            liveRegionForState = RenderLiveRegion.POLITE,
        )
    }
}
