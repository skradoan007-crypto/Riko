/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 1 / 15 — LibraryScreenContract.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Library Screen Contract declares the *shape* of the Library
 * screen as a whole — its dimensions, safe areas, slot layout, and
 * background. It is the top-level declaration that every other B5.1
 * contract composes into.
 *
 * Rule #54 — Screen Tells a Story.
 * The screen is shaped to lead the eye:
 *
 *   ┌─────────────────────────────┐
 *   │       Top Bar Slot          │  ← orientation, search, sort
 *   ├─────────────────────────────┤
 *   │                             │
 *   │      Content Slot           │  ← hero → collections → music
 *   │                             │     (the story)
 *   ├─────────────────────────────┤
 *   │       Dock Slot             │  ← mini player, always present
 *   └─────────────────────────────┘
 *
 * The slots are not just layout regions — they are *story beats*.
 * Top Bar orients. Content tells. Dock anchors.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * This file does NOT specify Compose scaffolds, Scaffold params,
 * or any framework-specific layout. It declares *intent* — the
 * shape, the slots, the background. The UI layer (B5.3) maps each
 * intent to a concrete Compose layout.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #26  Collection First          — content slot is collection-centric
 * #33  Collection Has Identity   — screen identity is "Library"
 * #54  Screen Tells a Story      — slot order encodes story order
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose, no Android framework, no Coroutine, no Flow, no
 * ViewModel. Pure Kotlin data declaration.
 *
 * © RIKO Experience OS — Constitution Tier
 */

package com.rikodev.music.android.presentation.library.screen.contract

/**
 * The device class the screen is rendering on.
 *
 * Drives slot sizing, dock prominence, and hero card aspect ratio.
 * The renderer (B5.3) reads this and adapts the layout.
 */
enum class LibraryDeviceClass(
    internal val description: String,
    internal val minWidthDp: Int,
    internal val isFoldable: Boolean,
) {
    /** Compact phone — width < 600dp. Single-column everything. */
    COMPACT_PHONE(
        description = "Compact phone — single-column.",
        minWidthDp = 0,
        isFoldable = false,
    ),

    /** Standard phone — 600dp ≤ width < 840dp. */
    STANDARD_PHONE(
        description = "Standard phone — single-column with peek.",
        minWidthDp = 600,
        isFoldable = false,
    ),

    /** Foldable (unfolded) — 840dp ≤ width < 1200dp. May use two-pane. */
    FOLDABLE(
        description = "Foldable unfolded — two-pane capable.",
        minWidthDp = 840,
        isFoldable = true,
    ),

    /** Tablet — width ≥ 1200dp. Two-pane with master/detail. */
    TABLET(
        description = "Tablet — two-pane master/detail.",
        minWidthDp = 1200,
        isFoldable = false,
    ),
}

/**
 * The orientation the screen is currently in.
 */
enum class LibraryScreenOrientation(
    internal val description: String,
) {
    /** Portrait — height > width. Default for phones. */
    PORTRAIT("Portrait — height > width."),

    /** Landscape — width > height. Common for tablets / foldables. */
    LANDSCAPE("Landscape — width > height."),
}

/**
 * The shape of the Library screen.
 *
 * @property deviceClass       Device class — drives slot sizing.
 * @property orientation       Current orientation.
 * @property widthDp           Screen width in dp.
 * @property heightDp          Screen height in dp.
 * @property safeAreaTopDp     Safe area inset top (status bar / notch).
 * @property safeAreaBottomDp  Safe area inset bottom (gesture nav).
 * @property safeAreaLeftDp    Safe area inset left (fold hinge, etc.).
 * @property safeAreaRightDp   Safe area inset right.
 * @property density           Display density (dpi / 160).
 * @property refreshRateHz     Screen refresh rate (60 / 90 / 120 / 144).
 */
data class LibraryScreenShape(
    val deviceClass: LibraryDeviceClass,
    val orientation: LibraryScreenOrientation,
    val widthDp: Int,
    val heightDp: Int,
    val safeAreaTopDp: Int,
    val safeAreaBottomDp: Int,
    val safeAreaLeftDp: Int,
    val safeAreaRightDp: Int,
    val density: Float,
    val refreshRateHz: Int,
) {
    /** True if the screen is high-refresh-rate (≥ 90Hz). */
    val isHighRefreshRate: Boolean get() = refreshRateHz >= 90

    /** True if the screen is compact (phone-sized). */
    val isCompact: Boolean
        get() = deviceClass == LibraryDeviceClass.COMPACT_PHONE ||
            deviceClass == LibraryDeviceClass.STANDARD_PHONE

    /** True if the screen supports two-pane layouts. */
    val supportsTwoPane: Boolean
        get() = deviceClass == LibraryDeviceClass.FOLDABLE ||
            deviceClass == LibraryDeviceClass.TABLET

    companion object {
        /** Reference shape — standard phone, portrait, 60Hz. */
        val REFERENCE_PHONE_PORTRAIT = LibraryScreenShape(
            deviceClass = LibraryDeviceClass.STANDARD_PHONE,
            orientation = LibraryScreenOrientation.PORTRAIT,
            widthDp = 360,
            heightDp = 800,
            safeAreaTopDp = 24,
            safeAreaBottomDp = 32,
            safeAreaLeftDp = 0,
            safeAreaRightDp = 0,
            density = 2.75f,
            refreshRateHz = 60,
        )
    }
}

/**
 * The slots the Library screen is divided into.
 *
 * Rule #54 — Screen Tells a Story.
 * The order of slots IS the story order:
 *   1. Top Bar    — orientation (where am I, what can I do?)
 *   2. Content    — the story (hero → collections → music)
 *   3. Dock       — anchor (what's playing, always here)
 *
 * @property topBarHeightDp       Top bar slot height (collapsed).
 * @property topBarExpandedHeightDp  Top bar slot height (expanded).
 * @property contentPaddingTopDp  Padding above content (under top bar).
 * @property contentPaddingBottomDp  Padding below content (above dock).
 * @property dockHeightDp         Dock slot height (collapsed/peeking).
 * @property dockExpandedHeightDp Dock slot height (expanded/peek-full).
 * @property dockMarginDp         Margin around dock (floating style).
 * @property horizontalMarginDp   Horizontal margin for content.
 */
data class LibraryScreenSlots(
    val topBarHeightDp: Int,
    val topBarExpandedHeightDp: Int,
    val contentPaddingTopDp: Int,
    val contentPaddingBottomDp: Int,
    val dockHeightDp: Int,
    val dockExpandedHeightDp: Int,
    val dockMarginDp: Int,
    val horizontalMarginDp: Int,
) {
    companion object {
        /** Standard slot layout for compact phones. */
        val COMPACT_PHONE = LibraryScreenSlots(
            topBarHeightDp = 56,
            topBarExpandedHeightDp = 96,
            contentPaddingTopDp = 8,
            contentPaddingBottomDp = 96,
            dockHeightDp = 64,
            dockExpandedHeightDp = 96,
            dockMarginDp = 8,
            horizontalMarginDp = 0,
        )

        /** Slot layout for tablets — wider margins, taller top bar. */
        val TABLET = LibraryScreenSlots(
            topBarHeightDp = 64,
            topBarExpandedHeightDp = 112,
            contentPaddingTopDp = 12,
            contentPaddingBottomDp = 112,
            dockHeightDp = 72,
            dockExpandedHeightDp = 104,
            dockMarginDp = 24,
            horizontalMarginDp = 32,
        )
    }
}

/**
 * The background layer behind the Library content.
 *
 * The Library background is not a flat color — it is a *mood* layer.
 * It shifts subtly with the active collection's personality, the
 * time of day, and the user's chosen visual language.
 *
 * @property backgroundKind      What kind of background.
 * @property baseColorHex        Base color (hex).
 * @property ambientColorHex     Ambient tint — drifts behind content.
 * @property ambientIntensity    0.0–1.0. How strong the ambient layer is.
 * @property waveIntegration     True if the wave layer is visible.
 * @property glassIntegration    True if the glass layer is visible.
 */
data class LibraryScreenBackground(
    val backgroundKind: LibraryBackgroundKind,
    val baseColorHex: String,
    val ambientColorHex: String,
    val ambientIntensity: Float,
    val waveIntegration: Boolean,
    val glassIntegration: Boolean,
) {
    init {
        require(ambientIntensity in 0f..1f) {
            "ambientIntensity must be in [0, 1], got $ambientIntensity"
        }
    }

    companion object {
        /** Default background — aurora indigo ambient over solid base. */
        val DEFAULT = LibraryScreenBackground(
            backgroundKind = LibraryBackgroundKind.SOLID_WITH_AMBIENT,
            baseColorHex = "#050213",
            ambientColorHex = "#0C0B56",
            ambientIntensity = 0.3f,
            waveIntegration = true,
            glassIntegration = true,
        )

        /** AMOLED-optimized background — pure black, minimal ambient. */
        val AMOLED = LibraryScreenBackground(
            backgroundKind = LibraryBackgroundKind.PURE_BLACK,
            baseColorHex = "#000000",
            ambientColorHex = "#0A0A0A",
            ambientIntensity = 0.15f,
            waveIntegration = true,
            glassIntegration = true,
        )
    }
}

/**
 * Kind of background the Library uses.
 */
enum class LibraryBackgroundKind(
    internal val description: String,
    internal val isAnimated: Boolean,
) {
    /** Pure solid color — most battery-efficient. */
    SOLID("Solid color background.", isAnimated = false),

    /** Solid base with a subtle ambient tint layer above. */
    SOLID_WITH_AMBIENT("Solid base with ambient tint.", isAnimated = true),

    /** Pure black — for AMOLED screens. */
    PURE_BLACK("Pure black — AMOLED optimized.", isAnimated = false),

    /** Pure black with ambient tint above. */
    PURE_BLACK_WITH_AMBIENT("Pure black with ambient tint.", isAnimated = true),

    /** Gradient background — top-to-bottom color drift. */
    GRADIENT("Gradient background.", isAnimated = true),

    /** Personality-driven gradient — shifts with active collection. */
    PERSONALITY_GRADIENT(
        "Personality-driven gradient — shifts with active collection.",
        isAnimated = true,
    ),
}

/**
 * The Library Screen Contract — the top-level declaration.
 *
 * This composes the screen shape, slots, and background into a
 * single contract. Every other B5.1 contract references this one
 * for screen-level context.
 *
 * @property shape           Screen dimensions + safe areas.
 * @property slots           Slot layout (top bar / content / dock).
 * @property background      Background layer spec.
 * @property isFullscreen    True if the screen is in fullscreen mode
 *                           (top bar hidden, dock expanded).
 * @property isImmersive     True if the screen is in immersive mode
 *                           (system bars hidden).
 */
data class LibraryScreenContract(
    val shape: LibraryScreenShape,
    val slots: LibraryScreenSlots,
    val background: LibraryScreenBackground,
    val isFullscreen: Boolean,
    val isImmersive: Boolean,
) {
    companion object {
        /** Default contract for compact phone portrait. */
        val DEFAULT_COMPACT = LibraryScreenContract(
            shape = LibraryScreenShape.REFERENCE_PHONE_PORTRAIT,
            slots = LibraryScreenSlots.COMPACT_PHONE,
            background = LibraryScreenBackground.DEFAULT,
            isFullscreen = false,
            isImmersive = false,
        )

        /** AMOLED-optimized contract. */
        val AMOLED_COMPACT = LibraryScreenContract(
            shape = LibraryScreenShape.REFERENCE_PHONE_PORTRAIT,
            slots = LibraryScreenSlots.COMPACT_PHONE,
            background = LibraryScreenBackground.AMOLED,
            isFullscreen = false,
            isImmersive = false,
        )
    }

    /** True if the top bar slot is visible (not fullscreen). */
    val isTopBarVisible: Boolean get() = !isFullscreen

    /** True if the dock slot is visible. */
    val isDockVisible: Boolean get() = true

    /** Effective content height in dp (screen minus top bar minus dock). */
    val effectiveContentHeightDp: Int
        get() = shape.heightDp -
            shape.safeAreaTopDp -
            shape.safeAreaBottomDp -
            (if (isTopBarVisible) slots.topBarHeightDp else 0) -
            (if (isDockVisible) slots.dockHeightDp + slots.dockMarginDp * 2 else 0) -
            slots.contentPaddingTopDp -
            slots.contentPaddingBottomDp
}
