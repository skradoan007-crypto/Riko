/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 15 / 15 — LibraryConstitution.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryConstitution] is THE constitution of the Library screen.
 * It assembles every other B5.1 contract into a single, locked
 * declaration. This is the file a new developer should read to
 * understand the Library screen's shape.
 *
 * Rule #39 — Contracts Are Forever.
 * Once locked, this constitution does not change. The runtime (B5.2)
 * and renderer (B5.3) consume it — they never invent it.
 *
 * RELATIONSHIP TO B1
 * ------------------
 * B1's [CollectionConstitution] is the *catalogue* of collections.
 * B5.1's [LibraryConstitution] is the *shape of the screen* that
 * hosts those collections.
 *
 *   CollectionConstitution (B1)  →  what collections exist
 *   LibraryConstitution (B5.1)   →  how the screen displays them
 *
 * RULES ENFORCED HERE
 * -------------------
 * #33  Collection Has Identity     — screen identity is "Library"
 * #39  Contracts Are Forever       — this constitution is immutable
 * #54  Screen Tells a Story        — section order is story order
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
 * The schema version of the Library Constitution.
 *
 * Bumped when the constitution's shape changes in a breaking way.
 * The runtime (B5.2) checks this and refuses to consume a constitution
 * with an incompatible schema.
 */
enum class LibraryConstitutionSchema(
    internal val version: Int,
    internal val description: String,
) {
    /** Initial schema — B5.1. */
    V1(1, "Initial schema — B5.1."),
}

/**
 * A profile of the Library Constitution — a named preset that
 * bundles multiple contracts into a coherent configuration.
 *
 * Profiles are how the host (B5.5) selects a "mode" for the Library.
 * For example, the AMOLED profile swaps in pure-black backgrounds,
 * the LOW_END profile relaxes performance budgets, etc.
 */
enum class LibraryConstitutionProfile(
    internal val profileName: String,
    internal val description: String,
) {
    /** Default profile — balanced for standard phones. */
    DEFAULT(
        profileName = "default",
        description = "Default — balanced for standard phones.",
    ),

    /** AMOLED profile — pure-black backgrounds, no breathing. */
    AMOLED(
        profileName = "amoled",
        description = "AMOLED — pure black, no breathing.",
    ),

    /** Cinematic profile — slow motion, deep parallax, generous spacing. */
    CINEMATIC(
        profileName = "cinematic",
        description = "Cinematic — slow motion, deep parallax.",
    ),

    /** Compact profile — for tablets / landscape. */
    COMPACT(
        profileName = "compact",
        description = "Compact — tablets / landscape.",
    ),

    /** Low-end profile — relaxed performance, reduced motion. */
    LOW_END(
        profileName = "lowEnd",
        description = "Low-end — relaxed performance.",
    ),

    /** High-end profile — strict performance, aggressive prefetch. */
    HIGH_END(
        profileName = "highEnd",
        description = "High-end — strict performance.",
    ),

    /** Reduced-motion profile — for accessibility. */
    REDUCED_MOTION(
        profileName = "reducedMotion",
        description = "Reduced motion — accessibility.",
    ),

    /** High-contrast profile — for accessibility. */
    HIGH_CONTRAST(
        profileName = "highContrast",
        description = "High contrast — accessibility.",
    ),
}

/**
 * THE LIBRARY CONSTITUTION.
 *
 * This object is the single source of truth for the Library screen's
 * shape. It is composed of every other B5.1 contract.
 *
 * @property schema              Constitution schema version.
 * @property profile             Active profile.
 * @property screen              Screen shape + slots + background.
 * @property topBar              Top bar contract.
 * @property dock                Dock contract.
 * @property hero                Hero contract.
 * @property section             Default section contract (template).
 * @property scroll              Scroll contract.
 * @property gesture             Gesture contract.
 * @property navigation          Navigation contract.
 * @property accessibility       Accessibility contract.
 * @property performance         Performance contract.
 * @property emptyState          Empty state contract.
 * @property visualLanguage      Visual language.
 * @property motionLanguage      Motion language.
 * @property tokens              Design tokens.
 */
data class LibraryConstitution(
    val schema: LibraryConstitutionSchema,
    val profile: LibraryConstitutionProfile,
    val screen: LibraryScreenContract,
    val topBar: LibraryTopBarContract,
    val dock: LibraryDockContract,
    val hero: LibraryHeroContract,
    val section: LibrarySectionContract,
    val scroll: LibraryScrollContract,
    val gesture: LibraryGestureContract,
    val navigation: LibraryNavigationContract,
    val accessibility: LibraryAccessibilityContract,
    val performance: LibraryPerformanceContract,
    val emptyState: LibraryEmptyStateContract,
    val visualLanguage: LibraryVisualLanguage,
    val motionLanguage: LibraryMotionLanguage,
    val tokens: LibraryTokens,
) {
    companion object {

        /**
         * The default constitution — balanced for standard phones.
         *
         * This is the constitution the host (B5.5) uses when the user
         * has not selected a specific profile.
         */
        val DEFAULT = LibraryConstitution(
            schema = LibraryConstitutionSchema.V1,
            profile = LibraryConstitutionProfile.DEFAULT,
            screen = LibraryScreenContract.DEFAULT_COMPACT,
            topBar = LibraryTopBarContract.DEFAULT,
            dock = LibraryDockContract.DEFAULT,
            hero = LibraryHeroContract.DEFAULT,
            section = LibrarySectionContract.hero(
                sectionId = "section.hero",
                hostedCollectionId = "continue_listening",
            ),
            scroll = LibraryScrollContract.DEFAULT,
            gesture = LibraryGestureContract.DEFAULT,
            navigation = LibraryNavigationContract.DEFAULT,
            accessibility = LibraryAccessibilityContract.DEFAULT,
            performance = LibraryPerformanceContract.DEFAULT,
            emptyState = LibraryEmptyStateContract.HIDDEN,
            visualLanguage = LibraryVisualLanguage.DEFAULT,
            motionLanguage = LibraryMotionLanguage.DEFAULT,
            tokens = LibraryTokens.DEFAULT,
        )

        /** AMOLED constitution — pure black, no breathing. */
        val AMOLED = DEFAULT.copy(
            profile = LibraryConstitutionProfile.AMOLED,
            screen = LibraryScreenContract.AMOLED_COMPACT,
            dock = LibraryDockContract.AMOLED,
        )

        /** Cinematic constitution — slow, deep, premium. */
        val CINEMATIC = DEFAULT.copy(
            profile = LibraryConstitutionProfile.CINEMATIC,
            hero = LibraryHeroContract.CINEMATIC,
            visualLanguage = LibraryVisualLanguage.CINEMATIC,
            motionLanguage = LibraryMotionLanguage.CINEMATIC,
        )

        /** Compact constitution — for tablets / landscape. */
        val COMPACT = DEFAULT.copy(
            profile = LibraryConstitutionProfile.COMPACT,
            hero = LibraryHeroContract.COMPACT,
            topBar = LibraryTopBarContract.COMPACT,
            visualLanguage = LibraryVisualLanguage.COMPACT,
        )

        /** Low-end constitution — relaxed performance. */
        val LOW_END = DEFAULT.copy(
            profile = LibraryConstitutionProfile.LOW_END,
            performance = LibraryPerformanceContract.LOW_END,
            motionLanguage = LibraryMotionLanguage.REDUCED,
            dock = LibraryDockContract.DEFAULT.copy(
                breathingSpec = LibraryDockBreathingSpec.NONE,
            ),
        )

        /** High-end constitution — strict performance. */
        val HIGH_END = DEFAULT.copy(
            profile = LibraryConstitutionProfile.HIGH_END,
            performance = LibraryPerformanceContract.HIGH_END,
            hero = LibraryHeroContract.CINEMATIC,
            motionLanguage = LibraryMotionLanguage.CINEMATIC,
        )

        /** Reduced-motion constitution — accessibility. */
        val REDUCED_MOTION = DEFAULT.copy(
            profile = LibraryConstitutionProfile.REDUCED_MOTION,
            accessibility = LibraryAccessibilityContract.REDUCED_MOTION,
            motionLanguage = LibraryMotionLanguage.REDUCED,
            hero = LibraryHeroContract.DEFAULT.copy(
                motionProfile = LibraryHeroMotionProfile.REDUCED,
            ),
            dock = LibraryDockContract.DEFAULT.copy(
                breathingSpec = LibraryDockBreathingSpec.NONE,
            ),
        )

        /** High-contrast constitution — accessibility. */
        val HIGH_CONTRAST = DEFAULT.copy(
            profile = LibraryConstitutionProfile.HIGH_CONTRAST,
            accessibility = LibraryAccessibilityContract.HIGH_CONTRAST,
        )

        /**
         * Resolve a constitution for a profile.
         *
         * @property profile  Desired profile.
         * @return            The constitution preset for that profile.
         */
        fun forProfile(profile: LibraryConstitutionProfile): LibraryConstitution =
            when (profile) {
                LibraryConstitutionProfile.DEFAULT -> DEFAULT
                LibraryConstitutionProfile.AMOLED -> AMOLED
                LibraryConstitutionProfile.CINEMATIC -> CINEMATIC
                LibraryConstitutionProfile.COMPACT -> COMPACT
                LibraryConstitutionProfile.LOW_END -> LOW_END
                LibraryConstitutionProfile.HIGH_END -> HIGH_END
                LibraryConstitutionProfile.REDUCED_MOTION -> REDUCED_MOTION
                LibraryConstitutionProfile.HIGH_CONTRAST -> HIGH_CONTRAST
            }
    }

    /** True if this constitution is the AMOLED profile. */
    val isAmoled: Boolean
        get() = profile == LibraryConstitutionProfile.AMOLED

    /** True if this constitution is the cinematic profile. */
    val isCinematic: Boolean
        get() = profile == LibraryConstitutionProfile.CINEMATIC

    /** True if this constitution is a low-end profile. */
    val isLowEnd: Boolean
        get() = profile == LibraryConstitutionProfile.LOW_END

    /** True if this constitution is a high-end profile. */
    val isHighEnd: Boolean
        get() = profile == LibraryConstitutionProfile.HIGH_END

    /** True if this constitution has reduced motion enabled. */
    val isReducedMotion: Boolean
        get() = profile == LibraryConstitutionProfile.REDUCED_MOTION ||
            accessibility.isReduceMotionEnabled ||
            motionLanguage.isReduceMotionEnabled

    /** True if this constitution has high contrast enabled. */
    val isHighContrast: Boolean
        get() = profile == LibraryConstitutionProfile.HIGH_CONTRAST ||
            accessibility.isHighContrastEnabled
}
