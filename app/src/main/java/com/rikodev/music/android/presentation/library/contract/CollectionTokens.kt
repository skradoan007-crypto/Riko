/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 15 / 15 — CollectionTokens.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Design tokens for the Library surface.
 *
 * Tokens are the *physical constants* of the Library's visual
 * language. The renderer (B4) reads these tokens and resolves
 * them against the active theme. The contract NEVER invents
 * spacing, corner, or motion values at runtime — they all live
 * here.
 *
 * Why tokens?
 *   • They make the Library visually consistent.
 *   • They make theme variations trivial (just swap token values).
 *   • They make design changes auditable (one file to change).
 *
 * RULE #28 — Motion Explains Hierarchy
 * RULE #33 — Collection Has Identity
 *
 * Sections in this file:
 *   1. Spacing        — rhythm between elements
 *   2. Corner         — corner radius scale
 *   3. Glass          — glass tint strength
 *   4. Shadow         — shadow elevation
 *   5. Wave           — wave rendering caps
 *   6. Glow           — accent glow parameters
 *   7. Header         — header-specific tokens
 *   8. Section        — section-specific tokens
 *   9. Padding        — outer padding tokens
 *  10. Divider        — divider tokens
 *  11. Animation      — motion profiles
 *  12. Opacity        — opacity scale
 *  13. Scale          — element scale factors
 *  14. Radius         — corner radius dp values
 *  15. Elevation      — z-depth dp values
 *
 * All values are declared as primitive Doubles (dp) or Floats
 * (opacity / scale) to keep the contract Compose-agnostic.
 *
 * No logic. Pure declaration.
 */

package com.rikodev.music.android.presentation.library.contract

/* ═══════════════════════════════════════════════════════════════
 * 1. SPACING — rhythm between elements
 * ═══════════════════════════════════════════════════════════════ */

object CollectionSpacing {
    /** No spacing — for elements that touch. */
    const val NONE: Int = 0
    /** Hairline — 2dp. Used for tight groupings inside a card. */
    const val HAIRLINE: Int = 2
    /** Tight — 4dp. Used between icon and label. */
    const val TIGHT: Int = 4
    /** Compact — 6dp. Used between metadata lines. */
    const val COMPACT: Int = 6
    /** Cozy — 8dp. Default spacing inside cards. */
    const val COZY: Int = 8
    /** Standard — 12dp. Default spacing between cards. */
    const val STANDARD: Int = 12
    /** Spacious — 16dp. Default padding for sections. */
    const val SPACIOUS: Int = 16
    /** Hero — 24dp. Used for hero collections. */
    const val HERO: Int = 24
    /** Section gap — 32dp. Used between major sections. */
    const val SECTION_GAP: Int = 32
    /** Mega gap — 48dp. Used between Library Header and first section. */
    const val MEGA_GAP: Int = 48
}

/* ═══════════════════════════════════════════════════════════════
 * 2. CORNER — corner radius scale
 * ═══════════════════════════════════════════════════════════════ */

object CollectionCorner {
    const val SHARP: Int = 0
    const val SUBTLE: Int = 4
    const val SOFT: Int = 8
    const val STANDARD: Int = 12
    const val ROUNDED: Int = 16
    const val PILL: Int = 24
    const val FULL: Int = 999 // circular
}

/* ═══════════════════════════════════════════════════════════════
 * 3. GLASS — glass tint strength
 * ═══════════════════════════════════════════════════════════════ */

object CollectionGlass {
    const val NONE: Float = 0.0f
    const val LIGHT: Float = 0.3f
    const val MEDIUM: Float = 0.5f
    const val STRONG: Float = 0.7f
    const val HEAVY: Float = 0.9f

    /** Default blur radius in dp for the standard library surface. */
    const val DEFAULT_BLUR_DP: Int = 24

    /** Library glass tint cap — Rule #28 caps wave, this caps glass. */
    const val LIBRARY_TINT_CAP: Float = 0.7f
}

/* ═══════════════════════════════════════════════════════════════
 * 4. SHADOW — shadow elevation
 * ═══════════════════════════════════════════════════════════════ */

object CollectionShadow {
    const val NONE: Float = 0f
    const val LOW: Float = 1f
    const val MEDIUM: Float = 3f
    const val HIGH: Float = 6f
    const val FLOATING: Float = 10f

    /** Default shadow alpha — 12% black. */
    const val DEFAULT_ALPHA: Float = 0.12f

    /** Glow shadow alpha — used for accent-tinted bloom. */
    const val GLOW_ALPHA: Float = 0.30f
}

/* ═══════════════════════════════════════════════════════════════
 * 5. WAVE — wave rendering caps
 * Rule #28 — Library wave is always capped below Now Playing.
 * ═══════════════════════════════════════════════════════════════ */

object CollectionWave {
    /** Library wave opacity cap — Rule #28. */
    const val OPACITY_CAP: Float = 0.3f
    /** Library wave thickness cap — Rule #28. */
    const val THICKNESS_CAP: Float = 0.5f

    /** Whisper wave — for night / minimal collections. */
    const val WHISPER_OPACITY: Float = 0.10f
    const val WHISPER_THICKNESS: Float = 0.3f

    /** Subtle wave — for discovery collections. */
    const val SUBTLE_OPACITY: Float = 0.15f
    const val SUBTLE_THICKNESS: Float = 0.4f

    /** Standard library wave — default. */
    const val STANDARD_OPACITY: Float = 0.20f
    const val STANDARD_THICKNESS: Float = 0.4f

    /** Emphasized wave — hero collections only. */
    const val EMPHASIZED_OPACITY: Float = 0.30f
    const val EMPHASIZED_THICKNESS: Float = 0.5f

    /** Wave animation speed — cycles per second. */
    const val DEFAULT_SPEED: Float = 0.6f
    const val CALM_SPEED: Float = 0.4f
    const val ENERGETIC_SPEED: Float = 1.0f
}

/* ═══════════════════════════════════════════════════════════════
 * 6. GLOW — accent glow parameters
 * ═══════════════════════════════════════════════════════════════ */

object CollectionGlow {
    /** No glow. */
    const val NONE: Float = 0.0f
    /** Subtle glow — barely visible bloom around accent. */
    const val SUBTLE: Float = 0.15f
    /** Standard glow — used for hero collections. */
    const val STANDARD: Float = 0.30f
    /** Strong glow — used for night / mood collections. */
    const val STRONG: Float = 0.50f

    /** Glow blur radius in dp. */
    const val BLUR_DP: Int = 16
}

/* ═══════════════════════════════════════════════════════════════
 * 7. HEADER — header-specific tokens
 * ═══════════════════════════════════════════════════════════════ */

object CollectionHeaderTokens {
    /** Greeting text size scale relative to body. */
    const val GREETING_TEXT_SCALE: Float = 2.0f

    /** Subtitle text size scale. */
    const val SUBTITLE_TEXT_SCALE: Float = 1.0f

    /** Context line text size scale. */
    const val CONTEXT_LINE_TEXT_SCALE: Float = 0.85f

    /** Header height collapsed — dp. */
    const val HEIGHT_COLLAPSED: Int = 56

    /** Header height expanded — dp. */
    const val HEIGHT_EXPANDED: Int = 144

    /** Header top inset — dp. */
    const val TOP_INSET: Int = 12

    /** Header horizontal padding — dp. */
    const val HORIZONTAL_PADDING: Int = 20

    /** Header bottom padding — dp. */
    const val BOTTOM_PADDING: Int = 12

    /** Badge size — dp. */
    const val BADGE_SIZE: Int = 12

    /** Chip height — dp. */
    const val CHIP_HEIGHT: Int = 32

    /** Chip corner radius — dp. */
    const val CHIP_CORNER: Int = 16

    /** Header glass strength — default. */
    const val DEFAULT_GLASS: Float = 0.5f
}

/* ═══════════════════════════════════════════════════════════════
 * 8. SECTION — section-specific tokens
 * ═══════════════════════════════════════════════════════════════ */

object CollectionSectionTokens {
    /** Section title text scale. */
    const val TITLE_TEXT_SCALE: Float = 1.4f

    /** Section subtitle text scale. */
    const val SUBTITLE_TEXT_SCALE: Float = 0.9f

    /** Section header height — dp. */
    const val HEADER_HEIGHT: Int = 48

    /** Section top padding — dp. */
    const val TOP_PADDING: Int = 16

    /** Section bottom padding — dp. */
    const val BOTTOM_PADDING: Int = 16

    /** Section horizontal padding — dp. */
    const val HORIZONTAL_PADDING: Int = 16

    /** Gap between header and first card — dp. */
    const val HEADER_TO_CONTENT_GAP: Int = 12

    /** Gap between cards in a strip — dp. */
    const val CARD_GAP_STRIP: Int = 10

    /** Gap between cards in a grid — dp. */
    const val CARD_GAP_GRID: Int = 8

    /** Gap between rows in a list — dp. */
    const val CARD_GAP_LIST: Int = 4

    /** Hero card peek width — dp. */
    const val HERO_PEEK_WIDTH: Int = 32

    /** Compact card peek width — dp. */
    const val COMPACT_PEEK_WIDTH: Int = 16
}

/* ═══════════════════════════════════════════════════════════════
 * 9. PADDING — outer padding tokens
 * ═══════════════════════════════════════════════════════════════ */

object CollectionPadding {
    const val NONE: Int = 0
    const val TIGHT: Int = 8
    const val COZY: Int = 12
    const val STANDARD: Int = 16
    const val SPACIOUS: Int = 20
    const val HERO: Int = 24
    const val MEGA: Int = 32

    /** Default Library screen horizontal padding. */
    const val SCREEN_HORIZONTAL: Int = 16

    /** Default Library screen top padding — below the header. */
    const val SCREEN_TOP_BELOW_HEADER: Int = 8

    /** Default Library screen bottom padding — above bottom nav. */
    const val SCREEN_BOTTOM_ABOVE_NAV: Int = 96
}

/* ═══════════════════════════════════════════════════════════════
 * 10. DIVIDER — divider tokens
 * ═══════════════════════════════════════════════════════════════ */

object CollectionDivider {
    /** Hairline divider thickness — dp. */
    const val HAIRLINE_THICKNESS: Float = 0.5f

    /** Thin divider thickness — dp. */
    const val THIN_THICKNESS: Float = 1f

    /** Standard divider thickness — dp. */
    const val STANDARD_THICKNESS: Float = 1.5f

    /** Hairline alpha. */
    const val HAIRLINE_ALPHA: Float = 0.10f

    /** Standard alpha. */
    const val STANDARD_ALPHA: Float = 0.20f

    /** Fade length at each end — dp. */
    const val FADE_END_LENGTH: Int = 32

    /** Section gap when divider is WHITESPACE — dp. */
    const val SECTION_GAP: Int = 32
}

/* ═══════════════════════════════════════════════════════════════
 * 11. ANIMATION — motion profiles
 * ═══════════════════════════════════════════════════════════════ */

object CollectionAnimation {
    /** No animation. */
    const val NONE_DURATION_MS: Int = 0
    const val NONE_DAMPING: Float = 1.0f

    /** Subtle — quick fade. */
    const val SUBTLE_DURATION_MS: Int = 180
    const val SUBTLE_DAMPING: Float = 0.8f

    /** Balanced — default. */
    const val BALANCED_DURATION_MS: Int = 260
    const val BALANCED_DAMPING: Float = 0.7f

    /** Gentle — drift-in. */
    const val GENTLE_DURATION_MS: Int = 340
    const val GENTLE_DAMPING: Float = 0.6f

    /** Dreamy — slow drift with blur. */
    const val DREAMY_DURATION_MS: Int = 460
    const val DREAMY_DAMPING: Float = 0.5f

    /** Hero reveal — large cards unfold. */
    const val HERO_REVEAL_DURATION_MS: Int = 520
    const val HERO_REVEAL_DAMPING: Float = 0.65f

    /** Stagger between cards — ms. */
    const val CARD_STAGGER_MS: Int = 40

    /** Stagger between sections — ms. */
    const val SECTION_STAGGER_MS: Int = 80

    /** Animation cooldown — min ms between two runs. */
    const val COOLDOWN_MS: Int = 1000

    /** Pull-to-refresh threshold — dp. */
    const val PULL_TO_REFRESH_THRESHOLD: Int = 64

    /** Pull-to-refresh max — dp. */
    const val PULL_TO_REFRESH_MAX: Int = 128
}

/* ═══════════════════════════════════════════════════════════════
 * 12. OPACITY — opacity scale
 * ═══════════════════════════════════════════════════════════════ */

object CollectionOpacity {
    /** Fully transparent. */
    const val TRANSPARENT: Float = 0.0f
    /** Whisper — barely visible. */
    const val WHISPER: Float = 0.10f
    /** Subtle. */
    const val SUBTLE: Float = 0.20f
    /** Low. */
    const val LOW: Float = 0.30f
    /** Medium. */
    const val MEDIUM: Float = 0.50f
    /** High. */
    const val HIGH: Float = 0.70f
    /** Strong. */
    const val STRONG: Float = 0.85f
    /** Fully opaque. */
    const val OPAQUE: Float = 1.0f

    /** Disabled state opacity. */
    const val DISABLED: Float = 0.38f

    /** Pressed state opacity. */
    const val PRESSED: Float = 0.70f

    /** Focused state opacity. */
    const val FOCUSED: Float = 0.85f

    /** Hovered state opacity. */
    const val HOVERED: Float = 0.92f
}

/* ═══════════════════════════════════════════════════════════════
 * 13. SCALE — element scale factors
 * ═══════════════════════════════════════════════════════════════ */

object CollectionScale {
    /** No scaling. */
    const val NONE: Float = 1.0f
    /** Pressed scale — slight shrink. */
    const val PRESSED: Float = 0.96f
    /** Focused scale — slight grow. */
    const val FOCUSED: Float = 1.02f
    /** Hovered scale — gentle grow. */
    const val HOVERED: Float = 1.04f
    /** Active scale — emphasized grow. */
    const val ACTIVE: Float = 1.08f
    /** Hero scale — for hero cards. */
    const val HERO: Float = 1.12f
    /** Diminished scale — for collapsed cards. */
    const val DIMINISHED: Float = 0.92f
    /** Mini scale — for thumbnails. */
    const val MINI: Float = 0.75f
}

/* ═══════════════════════════════════════════════════════════════
 * 14. RADIUS — corner radius dp values (re-exported as constants)
 * ═══════════════════════════════════════════════════════════════ */

object CollectionRadius {
    /** Card corner radius — default. */
    const val CARD_DEFAULT: Int = 12
    /** Card corner radius — hero. */
    const val CARD_HERO: Int = 16
    /** Card corner radius — compact. */
    const val CARD_COMPACT: Int = 8

    /** Section background corner radius. */
    const val SECTION_BACKGROUND: Int = 16
    /** Section header corner radius. */
    const val SECTION_HEADER: Int = 8

    /** Album art corner radius — small. */
    const val ALBUM_ART_SMALL: Int = 6
    /** Album art corner radius — default. */
    const val ALBUM_ART_DEFAULT: Int = 12
    /** Album art corner radius — hero. */
    const val ALBUM_ART_HERO: Int = 20

    /** Chip corner radius — pill shape. */
    const val CHIP: Int = 16
    /** Badge corner radius — circular. */
    const val BADGE: Int = 999
    /** Thumbnail corner radius. */
    const val THUMBNAIL: Int = 6
}

/* ═══════════════════════════════════════════════════════════════
 * 15. ELEVATION — z-depth dp values
 * ═══════════════════════════════════════════════════════════════ */

object CollectionElevation {
    /** Flat — no elevation. */
    const val FLAT: Float = 0f
    /** Low — 1dp. */
    const val LOW: Float = 1f
    /** Medium — 3dp. */
    const val MEDIUM: Float = 3f
    /** High — 6dp. */
    const val HIGH: Float = 6f
    /** Floating — 10dp. */
    const val FLOATING: Float = 10f

    /** Card default elevation. */
    const val CARD_DEFAULT: Float = 1f
    /** Card hero elevation. */
    const val CARD_HERO: Float = 3f
    /** Section background elevation. */
    const val SECTION_BACKGROUND: Float = 0f
    /** Header sticky elevation. */
    const val HEADER_STICKY: Float = 4f
    /** Dock elevation. */
    const val DOCK: Float = 8f
}

/* ═══════════════════════════════════════════════════════════════
 * 16. TYPOGRAPHY — text style scale
 * ═══════════════════════════════════════════════════════════════ */

object CollectionTypography {
    /** Hero title text size — sp. */
    const val HERO_TITLE_SP: Int = 28
    /** Large title text size — sp. */
    const val LARGE_TITLE_SP: Int = 22
    /** Title text size — sp. */
    const val TITLE_SP: Int = 18
    /** Subtitle text size — sp. */
    const val SUBTITLE_SP: Int = 14
    /** Body text size — sp. */
    const val BODY_SP: Int = 14
    /** Caption text size — sp. */
    const val CAPTION_SP: Int = 12
    /** Micro text size — sp. */
    const val MICRO_SP: Int = 10

    /** Card title text size — sp. */
    const val CARD_TITLE_SP: Int = 14
    /** Card subtitle text size — sp. */
    const val CARD_SUBTITLE_SP: Int = 12

    /** Badge text size — sp. */
    const val BADGE_SP: Int = 10
    /** Chip text size — sp. */
    const val CHIP_SP: Int = 12
}

/* ═══════════════════════════════════════════════════════════════
 * 17. PERFORMANCE — render performance caps
 * ═══════════════════════════════════════════════════════════════ */

object CollectionPerformance {
    /** Max cards rendered simultaneously per section. */
    const val MAX_VISIBLE_CARDS_PER_SECTION: Int = 12

    /** Prefetch window — cards to render ahead of viewport. */
    const val DEFAULT_PREFETCH_WINDOW: Int = 5
    const val HERO_PREFETCH_WINDOW: Int = 3
    const val LIST_PREFETCH_WINDOW: Int = 15

    /** Max simultaneous wave animations in the Library. */
    const val MAX_SIMULTANEOUS_WAVES: Int = 1

    /** Max simultaneous glass blurs in the Library. */
    const val MAX_SIMULTANEOUS_BLURS: Int = 2

    /** Frame budget — ms. */
    const val FRAME_BUDGET_MS: Int = 16

    /** Scroll fps target. */
    const val SCROLL_FPS_TARGET: Int = 60

    /** Cache size for rendered cards — MB. */
    const val CARD_CACHE_MB: Int = 64
}
