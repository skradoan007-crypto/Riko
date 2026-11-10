package com.rikodev.music.android.core.experience.calibration

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — WAVE CALIBRATION
//
// The Physics BIOS of the entire project.
//
// Every float constant that controls HOW the wave feels lives here.
// No solver, no renderer, no generator contains its own constants.
// Everything flows from this single source of truth.
//
// WHY?
//   - Change PRESSURE_DECAY from 0.96 → 0.955 and the entire engine responds.
//   - No digging into solver internals. No "which file had that constant?"
//   - One file. One truth. One place to tune.
//
// ARCHITECTURE (v2 — Preset System):
//   WaveCalibration is the ACTIVE calibration state.
//   All fields are mutable (var) — presets override them at runtime.
//   reset() restores all values to CLASSIC defaults.
//   applyPreset() applies an ExperiencePreset personality.
//
//   Flow:
//     WaveCalibration (default) → applyPreset(Fluid) → engine feels Fluid
//     WaveCalibration.reset()     → back to Classic defaults
//     WaveCalibration.applyPreset(myCustom) → any personality
//
// THREAD SAFETY:
//   All reads/writes happen on the main thread (Compose UI + render).
//   No cross-thread access expected. If needed, add @Volatile later.
//
// CATEGORIES:
//   1. Energy   — How audio becomes emotion
//   2. Physics  — How emotion becomes fluid
//   3. Motion   — How fluid becomes movement
//   4. Geometry — How movement becomes shape
//   5. Light    — How shape becomes glass
//   6. Glow     — How presence becomes atmosphere
//   7. Layout   — How shape maps to screen
//
// RULE:
//   From today, there are NO magic floats in solver/renderer code.
//   If you see 0.96f in RibbonPhysicsSolver — it's a bug. Report it.
//
// "Code less. Feel more." — RIKO Calibration Philosophy
// "One line. Entire personality change." — RIKO Preset Philosophy
// ─────────────────────────────────────────────────────────────────────────────

object WaveCalibration {

    // ═══════════════════════════════════════════════════════════════════════
    // PRESET SYSTEM
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Currently active profile (or null if using custom/default).
     * Tracked for UI display and snapshot save.
     */
    var activeProfile: ExperienceProfile? = null
        private set

    /**
     * Apply an ExperiencePreset to the entire engine.
     *
     * This modifies WaveCalibration's mutable fields IN PLACE.
     * All solvers and renderers read from WaveCalibration per-frame,
     * so the personality change takes effect on the very next frame.
     *
     * Only the 9 "personality" parameters from the preset are overridden.
     * All other calibration values remain at their CLASSIC defaults.
     *
     * @param preset The preset to apply.
     */
    fun applyPreset(preset: ExperiencePreset) {
        // Reset everything to CLASSIC defaults first
        resetValues()

        // Apply the 9 personality overrides
        Physics.PRESSURE_DECAY = preset.pressureDecay
        Energy.MOMENTUM_RISE = preset.momentumRise
        Energy.MOMENTUM_FALL = preset.momentumFall
        Energy.EXPANSION_RELEASE_DECAY = preset.expansionDecay
        Physics.OPACITY_MINIMUM = preset.breathingOpacity
        Glow.AMBIENT_RADIUS_FRAC = preset.glowIntensity
        Light.HIGHLIGHT_WIDTH_MAX = preset.highlightWidth
        Physics.VISCOSITY_ENERGY_W = preset.turbulence
        Physics.THICKNESS_ACTIVE = preset.ribbonThickness

        // Track active profile
        activeProfile = preset.profile
    }

    /**
     * Reset all calibration values to CLASSIC defaults.
     * Clears any active preset.
     *
     * Call this when:
     *   - Switching back to default feel
     *   - Before applying a new preset (done automatically)
     *   - Disabling developer calibration mode
     */
    fun reset() {
        resetValues()
        activeProfile = null
    }

    /**
     * Internal: reset all values to CLASSIC defaults without changing activeProfile.
     * Used by applyPreset() before applying overrides.
     */
    private fun resetValues() {
        Energy.reset()
        Physics.reset()
        Motion.reset()
        Geometry.reset()
        Light.reset()
        Glow.reset()
        // Layout, GlassRender, HighlightRender — kept at compile-time const
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 1. ENERGY — How audio becomes emotion
    //    Source: EnergySolver
    // ═══════════════════════════════════════════════════════════════════════

    object Energy {

        // ── Composition Weights ──────────────────────────────────────────
        /** Bass → Pressure composition: bass is dominant. */
        var BASS_PRESSURE_BASS_W: Float = 0.60f
        /** Bass → Pressure composition: overall energy provides context. */
        var BASS_PRESSURE_OVERALL_W: Float = 0.25f
        /** Bass → Pressure composition: attack adds dynamic pressure. */
        var BASS_PRESSURE_ATTACK_W: Float = 0.15f

        /** Mid → Texture composition: mid is primary. */
        var MID_TEXTURE_MID_W: Float = 0.80f
        /** Mid → Texture: subtle treble contribution. */
        var MID_TEXTURE_TREBLE_W: Float = 0.10f
        /** Mid → Texture: subtle bass contribution. */
        var MID_TEXTURE_BASS_W: Float = 0.10f

        /** Treble → Spark composition: treble is primary. */
        var TREBLE_SPARK_TREBLE_W: Float = 0.75f
        /** Treble → Spark: transient adds sharp sparkle. */
        var TREBLE_SPARK_TRANSIENT_W: Float = 0.25f

        // ── Momentum ────────────────────────────────────────────────────
        /** Fast follow for rising energy momentum. */
        var MOMENTUM_RISE: Float = 0.25f
        /** Slow decay for falling energy momentum (long tail). */
        var MOMENTUM_FALL: Float = 0.06f

        // ── Expansion ─────────────────────────────────────────────────────
        /** How fast expansion builds on rising energy. */
        var EXPANSION_ATTACK: Float = 0.20f
        /** Per-frame decay multiplier during release (slow release). */
        var EXPANSION_RELEASE_DECAY: Float = 0.97f
        /** Slow blend toward target during release. */
        var EXPANSION_RELEASE_BLEND: Float = 0.03f
        /** Expansion target: bass-heavy composition. */
        var EXPANSION_PRESSURE_W: Float = 0.55f
        /** Expansion target: overall energy contribution. */
        var EXPANSION_OVERALL_W: Float = 0.45f

        // ── Inertia ─────────────────────────────────────────────────────
        /** How much CURRENT energy matters when rising (responsive). */
        var INERTIA_WEIGHT_RISE: Float = 0.30f
        /** How much CURRENT energy matters when falling (inertial). */
        var INERTIA_WEIGHT_FALL: Float = 0.08f

        // ── Mode Thresholds ────────────────────────────────────────────
        /** Below this energy while playing → BREATHING. */
        var THRESHOLD_BREATHING: Float = 0.08f
        /** Band energy below this → "vocal only" / ambient passage. */
        var THRESHOLD_THIN_BAND: Float = 0.15f
        /** Minimum mid energy to qualify as THIN. */
        var THRESHOLD_THIN_MID_MIN: Float = 0.04f
        /** Attack threshold to trigger PRESSURE_BUILD. */
        var THRESHOLD_PRESSURE_BUILD_ATTACK: Float = 0.20f
        /** Bass pressure minimum for PRESSURE_BUILD. */
        var THRESHOLD_PRESSURE_BUILD_BASS: Float = 0.35f
        /** Energy threshold for EXPANDING mode. */
        var THRESHOLD_EXPANDING: Float = 0.55f
        /** Energy below this when not playing → IDLE. */
        var THRESHOLD_IDLE: Float = 0.03f

        // ── Attack / Release Sensitivity ──────────────────────────────
        /** Multiplier for energy delta → attack. */
        var ATTACK_SENSITIVITY: Float = 5f
        /** Multiplier for energy delta → release. */
        var RELEASE_SENSITIVITY: Float = 5f

        // ── History ─────────────────────────────────────────────────────
        /** Number of frames in inertia memory buffer (structural, not tuning). */
        const val HISTORY_SIZE = 12

        /** Reset to CLASSIC defaults. */
        internal fun reset() {
            BASS_PRESSURE_BASS_W = 0.60f
            BASS_PRESSURE_OVERALL_W = 0.25f
            BASS_PRESSURE_ATTACK_W = 0.15f
            MID_TEXTURE_MID_W = 0.80f
            MID_TEXTURE_TREBLE_W = 0.10f
            MID_TEXTURE_BASS_W = 0.10f
            TREBLE_SPARK_TREBLE_W = 0.75f
            TREBLE_SPARK_TRANSIENT_W = 0.25f
            MOMENTUM_RISE = 0.25f
            MOMENTUM_FALL = 0.06f
            EXPANSION_ATTACK = 0.20f
            EXPANSION_RELEASE_DECAY = 0.97f
            EXPANSION_RELEASE_BLEND = 0.03f
            EXPANSION_PRESSURE_W = 0.55f
            EXPANSION_OVERALL_W = 0.45f
            INERTIA_WEIGHT_RISE = 0.30f
            INERTIA_WEIGHT_FALL = 0.08f
            THRESHOLD_BREATHING = 0.08f
            THRESHOLD_THIN_BAND = 0.15f
            THRESHOLD_THIN_MID_MIN = 0.04f
            THRESHOLD_PRESSURE_BUILD_ATTACK = 0.20f
            THRESHOLD_PRESSURE_BUILD_BASS = 0.35f
            THRESHOLD_EXPANDING = 0.55f
            THRESHOLD_IDLE = 0.03f
            ATTACK_SENSITIVITY = 5f
            RELEASE_SENSITIVITY = 5f
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. PHYSICS — How emotion becomes fluid
    //    Source: RibbonPhysicsSolver
    // ═══════════════════════════════════════════════════════════════════════

    object Physics {

        // ── Pressure ─────────────────────────────────────────────────────
        /** Per-frame pressure carry-over (decay). Higher = longer sustain. */
        var PRESSURE_DECAY: Float = 0.96f
        /** How much current energy feeds into pressure per frame. */
        var PRESSURE_ATTACK: Float = 0.22f

        // ── Elasticity ───────────────────────────────────────────────────
        /** Base elasticity before energy modulation. */
        var ELASTICITY_BASE: Float = 0.55f
        /** How much energy adds to elasticity. */
        var ELASTICITY_ENERGY_W: Float = 0.35f

        // ── Viscosity ───────────────────────────────────────────────────
        /** How much energy reduces viscosity (high energy = fast). */
        var VISCOSITY_ENERGY_W: Float = 0.4f

        // ── Surface Tension ───────────────────────────────────────────
        /** Base surface tension before momentum modulation. */
        var SURFACE_TENSION_BASE: Float = 0.7f
        /** How much momentum increases surface tension. */
        var SURFACE_TENSION_MOMENTUM_W: Float = 0.2f

        // ── Turbulence ─────────────────────────────────────────────────
        /** Mid texture weight in turbulence. */
        var TURBULENCE_MID_W: Float = 0.7f
        /** Treble spark weight in turbulence. */
        var TURBULENCE_TREBLE_W: Float = 0.3f

        // ── Thickness ────────────────────────────────────────────────────
        /** How fast thickness interpolates toward target. */
        var THICKNESS_LERP: Float = 0.15f
        /** Mode thickness targets. */
        var THICKNESS_IDLE: Float = 0.02f
        var THICKNESS_BREATHING: Float = 0.08f
        var THICKNESS_THIN: Float = 0.15f
        var THICKNESS_ACTIVE: Float = 0.35f
        var THICKNESS_PRESSURE_BUILD: Float = 0.55f
        var THICKNESS_EXPANDING: Float = 0.80f
        var THICKNESS_SETTLING_DECAY: Float = 0.04f

        // ── Opacity ─────────────────────────────────────────────────────
        /** Minimum opacity — ribbon is ALWAYS visible. */
        var OPACITY_MINIMUM: Float = 0.12f
        /** How fast opacity responds to energy changes. */
        var OPACITY_LERP: Float = 0.12f

        // ── Stretch ─────────────────────────────────────────────────────
        /** How fast stretch interpolates toward target. */
        var STRETCH_LERP: Float = 0.10f
        /** Mode stretch targets (1.0 = normal). */
        var STRETCH_IDLE: Float = 1.0f
        var STRETCH_BREATHING: Float = 1.0f
        var STRETCH_THIN: Float = 0.85f
        var STRETCH_ACTIVE: Float = 1.0f
        var STRETCH_PRESSURE_BUILD: Float = 1.15f
        var STRETCH_EXPANDING: Float = 1.40f

        // ── Compression ────────────────────────────────────────────────
        /** How fast compression interpolates toward target. */
        var COMPRESSION_LERP: Float = 0.10f
        /** Mode compression targets (1.0 = normal). */
        var COMPRESSION_IDLE: Float = 1.0f
        var COMPRESSION_THIN: Float = 0.70f

        // ── Damping ──────────────────────────────────────────────────────
        /** Mode damping values. Low = slow decay (lingers). */
        var DAMPING_IDLE: Float = 0.40f
        var DAMPING_BREATHING: Float = 0.35f
        var DAMPING_THIN: Float = 0.30f
        var DAMPING_ACTIVE: Float = 0.25f
        var DAMPING_PRESSURE_BUILD: Float = 0.20f
        var DAMPING_EXPANDING: Float = 0.15f
        var DAMPING_SETTLING: Float = 0.10f

        // ── Flow Resistance ──────────────────────────────────────────────
        /** Base flow resistance. */
        var FLOW_RESISTANCE_BASE: Float = 0.3f
        /** How viscosity adds to flow resistance. */
        var FLOW_RESISTANCE_VISCOSITY_W: Float = 0.3f

        // ── Drift ───────────────────────────────────────────────────────
        /** Base drift — always present (ribbon always alive). */
        var DRIFT_BASE: Float = 0.10f
        /** How much energy adds to drift. */
        var DRIFT_ENERGY_W: Float = 0.15f

        // ── Highlight ───────────────────────────────────────────────────
        /** Treble weight in highlight intensity. */
        var HIGHLIGHT_TREBLE_W: Float = 0.7f
        /** Overall energy weight in highlight intensity. */
        var HIGHLIGHT_ENERGY_W: Float = 0.3f

        /** Reset to CLASSIC defaults. */
        internal fun reset() {
            PRESSURE_DECAY = 0.96f
            PRESSURE_ATTACK = 0.22f
            ELASTICITY_BASE = 0.55f
            ELASTICITY_ENERGY_W = 0.35f
            VISCOSITY_ENERGY_W = 0.4f
            SURFACE_TENSION_BASE = 0.7f
            SURFACE_TENSION_MOMENTUM_W = 0.2f
            TURBULENCE_MID_W = 0.7f
            TURBULENCE_TREBLE_W = 0.3f
            THICKNESS_LERP = 0.15f
            THICKNESS_IDLE = 0.02f
            THICKNESS_BREATHING = 0.08f
            THICKNESS_THIN = 0.15f
            THICKNESS_ACTIVE = 0.35f
            THICKNESS_PRESSURE_BUILD = 0.55f
            THICKNESS_EXPANDING = 0.80f
            THICKNESS_SETTLING_DECAY = 0.04f
            OPACITY_MINIMUM = 0.12f
            OPACITY_LERP = 0.12f
            STRETCH_LERP = 0.10f
            STRETCH_IDLE = 1.0f
            STRETCH_BREATHING = 1.0f
            STRETCH_THIN = 0.85f
            STRETCH_ACTIVE = 1.0f
            STRETCH_PRESSURE_BUILD = 1.15f
            STRETCH_EXPANDING = 1.40f
            COMPRESSION_LERP = 0.10f
            COMPRESSION_IDLE = 1.0f
            COMPRESSION_THIN = 0.70f
            DAMPING_IDLE = 0.40f
            DAMPING_BREATHING = 0.35f
            DAMPING_THIN = 0.30f
            DAMPING_ACTIVE = 0.25f
            DAMPING_PRESSURE_BUILD = 0.20f
            DAMPING_EXPANDING = 0.15f
            DAMPING_SETTLING = 0.10f
            FLOW_RESISTANCE_BASE = 0.3f
            FLOW_RESISTANCE_VISCOSITY_W = 0.3f
            DRIFT_BASE = 0.10f
            DRIFT_ENERGY_W = 0.15f
            HIGHLIGHT_TREBLE_W = 0.7f
            HIGHLIGHT_ENERGY_W = 0.3f
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. MOTION — How fluid becomes movement
    //    Source: MotionSolver
    // ═══════════════════════════════════════════════════════════════════════

    object Motion {

        // ── Spring Model ─────────────────────────────────────────────────
        /** How strongly ribbon is pulled back to center. */
        var SPRING_CONSTANT: Float = 0.08f
        /** Stronger pull during calm settling. */
        var SPRING_CONSTANT_SETTLING: Float = 0.12f

        // ── Energy Force ──────────────────────────────────────────────
        /** How strongly energy pushes ribbon from center. */
        var ENERGY_FORCE_SCALE: Float = 0.015f
        /** How much pressure contributes additional push. */
        var PRESSURE_FORCE_SCALE: Float = 0.008f
        /** Beat confidence "thump" impulse. */
        var BEAT_IMPULSE: Float = 0.02f

        // ── Drag ───────────────────────────────────────────────────────
        /** Base drag — velocity retains (1 - drag) per frame. */
        var DRAG_BASE: Float = 0.06f
        /** Drag during settling — higher for faster deceleration. */
        var DRAG_SETTLING: Float = 0.10f
        /** Drag during EXPANDING — lower for dramatic movement. */
        var DRAG_EXPANDING: Float = 0.04f
        /** Drag during BREATHING — higher for controlled movement. */
        var DRAG_BREATHING: Float = 0.08f

        // ── Friction ────────────────────────────────────────────────────
        var FRICTION_BASE: Float = 0.15f

        // ── Direction Hysteresis ────────────────────────────────────────
        /** Velocity threshold to flip direction. */
        var DIRECTION_FLIP_THRESHOLD: Float = 0.002f
        /** Velocity threshold to confirm neutral. */
        var DIRECTION_NEUTRAL_THRESHOLD: Float = 0.0005f

        // ── Momentum ────────────────────────────────────────────────────
        /** Fast follow for rising movement momentum. */
        var MOMENTUM_RISE: Float = 0.20f
        /** Slow decay for falling movement momentum. */
        var MOMENTUM_FALL: Float = 0.05f

        // ── Offset ─────────────────────────────────────────────────────
        /** Maximum vertical offset from center. */
        var OFFSET_MAX: Float = 0.5f

        // ── Settle ─────────────────────────────────────────────────────
        /** Snap to zero when offset is below this during settling. */
        var SETTLE_THRESHOLD: Float = 0.001f

        // ── Breathing ────────────────────────────────────────────────────
        /** Organic noise amplitude for living movement. */
        var BREATH_AMPLITUDE: Float = 0.003f
        /** Breathing oscillation speed (cycles per frame at 60fps). */
        var BREATH_SPEED: Float = 0.02f

        /** Reset to CLASSIC defaults. */
        internal fun reset() {
            SPRING_CONSTANT = 0.08f
            SPRING_CONSTANT_SETTLING = 0.12f
            ENERGY_FORCE_SCALE = 0.015f
            PRESSURE_FORCE_SCALE = 0.008f
            BEAT_IMPULSE = 0.02f
            DRAG_BASE = 0.06f
            DRAG_SETTLING = 0.10f
            DRAG_EXPANDING = 0.04f
            DRAG_BREATHING = 0.08f
            FRICTION_BASE = 0.15f
            DIRECTION_FLIP_THRESHOLD = 0.002f
            DIRECTION_NEUTRAL_THRESHOLD = 0.0005f
            MOMENTUM_RISE = 0.20f
            MOMENTUM_FALL = 0.05f
            OFFSET_MAX = 0.5f
            SETTLE_THRESHOLD = 0.001f
            BREATH_AMPLITUDE = 0.003f
            BREATH_SPEED = 0.02f
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. GEOMETRY — How movement becomes shape
    //    Source: OrganicPathGenerator
    // ═══════════════════════════════════════════════════════════════════════

    object Geometry {

        // ── Pressure Amplitude ──────────────────────────────────────────
        /** How much pressure amplifies wave amplitude. */
        var PRESSURE_AMPLITUDE_SCALE: Float = 0.4f

        // ── Motion Offset ────────────────────────────────────────────────
        /** How much motion solver offset displaces whole ribbon. */
        var MOTION_OFFSET_SCALE: Float = 0.5f

        // ── Turbulence ────────────────────────────────────────────────
        /** How much turbulence displaces each point. */
        var TURBULENCE_SCALE: Float = 0.08f
        /** Turbulence frequency — organic "texture" rate. */
        var TURBULENCE_FREQ: Float = 7.3f

        // ── Thickness Profile (Ink Brush) ─────────────────────────────
        /** Head region end: x < this → fades in. */
        var THICKNESS_HEAD_END: Float = 0.08f
        /** Body full start: x > this → full thickness. */
        var THICKNESS_BODY_START: Float = 0.15f
        /** Body full end: x > this → starts thinning. */
        var THICKNESS_BODY_END: Float = 0.75f
        /** Tail start: x > this → tail fade begins. */
        var THICKNESS_TAIL_START: Float = 0.80f
        /** Tail end: x > this → nearly invisible. */
        var THICKNESS_TAIL_END: Float = 0.98f

        // ── Opacity Profile ────────────────────────────────────────────
        /** Head fade: x < this → opacity fades in. */
        var OPACITY_HEAD_FADE: Float = 0.06f
        /** Tail start: x > this → opacity fades out. */
        var OPACITY_TAIL_START: Float = 0.88f

        // ── Curvature ──────────────────────────────────────────────────
        /** How much momentum influences curvature. */
        var MOMENTUM_CURVATURE_W: Float = 0.3f

        // ── Treble Detail ─────────────────────────────────────────────
        /** Treble adds fine detail to curvature (not height). */
        var TREBLE_DETAIL_SCALE: Float = 0.06f
        /** Treble detail frequency. */
        var TREBLE_DETAIL_FREQ: Float = 11.7f

        // ── Bass Body ──────────────────────────────────────────────────
        /** Bass adds body (thickness multiplier). */
        var BASS_BODY_SCALE: Float = 0.3f

        // ── Glass Pre-computation ──────────────────────────────────────
        /** Highlight concentration at curve peaks. */
        var HIGHLIGHT_CURVATURE_W: Float = 0.4f
        /** Highlight from treble at each point. */
        var HIGHLIGHT_TREBLE_W: Float = 0.6f
        /** Reflection strongest at body center. */
        var REFLECTION_CENTER_W: Float = 0.3f
        /** Edge alpha from surface tension. */
        var EDGE_ALPHA_TENSION_W: Float = 0.5f
        /** Edge alpha from energy. */
        var EDGE_ALPHA_ENERGY_W: Float = 0.5f

        // ── Harmonic Speeds ───────────────────────────────────────────
        /** Phase evolution speeds (prevents repeating shape). */
        val HARMONIC_SPEEDS = floatArrayOf(0.03f, 0.047f, 0.062f, 0.089f)

        /** Reset to CLASSIC defaults. */
        internal fun reset() {
            PRESSURE_AMPLITUDE_SCALE = 0.4f
            MOTION_OFFSET_SCALE = 0.5f
            TURBULENCE_SCALE = 0.08f
            TURBULENCE_FREQ = 7.3f
            THICKNESS_HEAD_END = 0.08f
            THICKNESS_BODY_START = 0.15f
            THICKNESS_BODY_END = 0.75f
            THICKNESS_TAIL_START = 0.80f
            THICKNESS_TAIL_END = 0.98f
            OPACITY_HEAD_FADE = 0.06f
            OPACITY_TAIL_START = 0.88f
            MOMENTUM_CURVATURE_W = 0.3f
            TREBLE_DETAIL_SCALE = 0.06f
            TREBLE_DETAIL_FREQ = 11.7f
            BASS_BODY_SCALE = 0.3f
            HIGHLIGHT_CURVATURE_W = 0.4f
            HIGHLIGHT_TREBLE_W = 0.6f
            REFLECTION_CENTER_W = 0.3f
            EDGE_ALPHA_TENSION_W = 0.5f
            EDGE_ALPHA_ENERGY_W = 0.5f
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5. LIGHT — How shape becomes glass
    //    Source: HighlightSolver
    // ═══════════════════════════════════════════════════════════════════════

    object Light {

        // ── Light Direction ─────────────────────────────────────────────
        /** Base light angle in radians (-45 degrees, upper-left). */
        var BASE_LIGHT_ANGLE: Float = -0.7854f

        // ── Light Following ─────────────────────────────────────────────
        /** How much curvature deflects light direction. */
        var LIGHT_FOLLOW_CURVATURE: Float = 0.4f
        /** How much overall bend shifts light. */
        var LIGHT_BEND_INFLUENCE: Float = 0.3f

        // ── Fresnel ──────────────────────────────────────────────────────
        /** Base fresnel power — falloff sharpness. */
        var BASE_FRESNEL_POWER: Float = 3.0f
        /** Maximum fresnel power with high energy. */
        var MAX_FRESNEL_POWER: Float = 5.0f

        // ── View Direction ──────────────────────────────────────────────
        /** Fixed view direction for fresnel approximation. */
        const val VIEW_DIR_X = 0f
        const val VIEW_DIR_Y = 1f

        // ── Specular ─────────────────────────────────────────────────────
        /** Specular power — highlight concentration. */
        var SPECULAR_POWER: Float = 8.0f
        /** Specular max intensity — premium, not blown out. */
        var SPECULAR_MAX: Float = 0.85f
        /** How much treble drives specular sparkle. */
        var TREBLE_SPARKLE_FACTOR: Float = 0.7f

        // ── Edge Glow ───────────────────────────────────────────────────
        /** Curvature threshold for edge glow. Rule #14: from curvature. */
        var EDGE_GLOW_CURVATURE_THRESHOLD: Float = 0.05f
        /** Maximum edge glow — subtle, never neon. */
        var EDGE_GLOW_MAX: Float = 0.45f
        /** Surface tension amplifies edge glow. */
        var SURFACE_TENSION_GLOW_FACTOR: Float = 0.3f

        // ── Rim Light ───────────────────────────────────────────────────
        /** Rim light intensity base. */
        var RIM_LIGHT_MAX: Float = 0.55f
        /** Normal-to-view angle drives rim light. */
        var RIM_LIGHT_NORMAL_FACTOR: Float = 0.6f

        // ── Reflection ────────────────────────────────────────────────────
        /** Maximum reflection alpha — subtle, not mirror. */
        var REFLECTION_MAX: Float = 0.4f
        /** Reflection base — minimum reflectivity. */
        var REFLECTION_BASE: Float = 0.03f
        /** How much pre-computed reflectionFactor drives result. */
        var POINT_REFLECTION_WEIGHT: Float = 0.6f

        // ── Glass ────────────────────────────────────────────────────────
        /** Maximum glass opacity. Never a solid block. */
        var GLASS_MAX: Float = 0.55f
        /** Minimum glass — hint of glass even in idle. */
        var GLASS_MIN: Float = 0.02f
        /** Surface tension drives glass opacity. */
        var GLASS_TENSION_WEIGHT: Float = 0.35f
        /** Opacity drives glass overlay visibility. */
        var GLASS_OPACITY_WEIGHT: Float = 0.4f

        // ── Shadow ──────────────────────────────────────────────────────
        /** Maximum shadow strength. Soft and subtle. */
        var SHADOW_MAX: Float = 0.35f
        /** Minimum energy before shadows appear. */
        var SHADOW_ENERGY_THRESHOLD: Float = 0.15f
        /** Thickness drives shadow width. */
        var SHADOW_THICKNESS_WEIGHT: Float = 0.3f

        // ── Highlight Width ─────────────────────────────────────────────
        /** Base highlight width (fraction of thickness). */
        var HIGHLIGHT_WIDTH_MIN: Float = 0.25f
        /** Maximum highlight width. Rule #15: wider, not brighter. */
        var HIGHLIGHT_WIDTH_MAX: Float = 0.75f

        // ── Reflection Offset ──────────────────────────────────────────
        /** Light direction shifts reflection position. */
        var REFLECTION_LIGHT_SHIFT: Float = 0.3f
        /** Curvature shifts reflection along curve. */
        var REFLECTION_CURVATURE_SHIFT: Float = 0.2f

        // ── Artwork Adaptation ──────────────────────────────────────────
        /** Dark artwork boosts highlights. */
        var DARK_ARTWORK_HIGHLIGHT_BOOST: Float = 1.15f
        /** High-contrast artwork sharpens specular. */
        var HIGH_CONTRAST_SHARPEN: Float = 0.15f

        // ── Smoothing ────────────────────────────────────────────────────
        /** Temporal smoothing lerp (per-point, prevents flicker). */
        var HIGHLIGHT_SMOOTHING: Float = 0.35f

        /** Reset to CLASSIC defaults. */
        internal fun reset() {
            BASE_LIGHT_ANGLE = -0.7854f
            LIGHT_FOLLOW_CURVATURE = 0.4f
            LIGHT_BEND_INFLUENCE = 0.3f
            BASE_FRESNEL_POWER = 3.0f
            MAX_FRESNEL_POWER = 5.0f
            SPECULAR_POWER = 8.0f
            SPECULAR_MAX = 0.85f
            TREBLE_SPARKLE_FACTOR = 0.7f
            EDGE_GLOW_CURVATURE_THRESHOLD = 0.05f
            EDGE_GLOW_MAX = 0.45f
            SURFACE_TENSION_GLOW_FACTOR = 0.3f
            RIM_LIGHT_MAX = 0.55f
            RIM_LIGHT_NORMAL_FACTOR = 0.6f
            REFLECTION_MAX = 0.4f
            REFLECTION_BASE = 0.03f
            POINT_REFLECTION_WEIGHT = 0.6f
            GLASS_MAX = 0.55f
            GLASS_MIN = 0.02f
            GLASS_TENSION_WEIGHT = 0.35f
            GLASS_OPACITY_WEIGHT = 0.4f
            SHADOW_MAX = 0.35f
            SHADOW_ENERGY_THRESHOLD = 0.15f
            SHADOW_THICKNESS_WEIGHT = 0.3f
            HIGHLIGHT_WIDTH_MIN = 0.25f
            HIGHLIGHT_WIDTH_MAX = 0.75f
            REFLECTION_LIGHT_SHIFT = 0.3f
            REFLECTION_CURVATURE_SHIFT = 0.2f
            DARK_ARTWORK_HIGHLIGHT_BOOST = 1.15f
            HIGH_CONTRAST_SHARPEN = 0.15f
            HIGHLIGHT_SMOOTHING = 0.35f
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 6. GLOW — How presence becomes atmosphere
    //    Source: GlowRenderer
    // ═══════════════════════════════════════════════════════════════════════

    object Glow {

        // ── Ambient Bloom ────────────────────────────────────────────────
        /** Ambient glow radius as fraction of canvas diagonal. */
        var AMBIENT_RADIUS_FRAC: Float = 0.35f

        // ── Outer Aura ──────────────────────────────────────────────────
        /** Aura padding beyond ribbon bounding box. */
        var OUTER_AURA_PAD: Float = 0.25f

        // ── Inner Aura ──────────────────────────────────────────────────
        /** Inner glow height as fraction of ribbon thickness. */
        var INNER_AURA_HEIGHT_FRAC: Float = 0.6f

        // ── Shadow Bloom ────────────────────────────────────────────────
        /** Shadow distance below ribbon center. */
        var SHADOW_OFFSET_FRAC: Float = 0.08f
        /** Shadow blur radius. */
        var SHADOW_BLUR_FRAC: Float = 0.06f

        // ── Beat Pulse ────────────────────────────────────────────────
        /** Beat pulse max radius as fraction of canvas diagonal. */
        var BEAT_PULSE_RADIUS_FRAC: Float = 0.3f
        /** Beat pulse decay per frame (~8 frames at 60fps). */
        var BEAT_PULSE_DECAY: Float = 0.85f

        /** Reset to CLASSIC defaults. */
        internal fun reset() {
            AMBIENT_RADIUS_FRAC = 0.35f
            OUTER_AURA_PAD = 0.25f
            INNER_AURA_HEIGHT_FRAC = 0.6f
            SHADOW_OFFSET_FRAC = 0.08f
            SHADOW_BLUR_FRAC = 0.06f
            BEAT_PULSE_RADIUS_FRAC = 0.3f
            BEAT_PULSE_DECAY = 0.85f
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 7. LAYOUT — How shape maps to screen (CONST — never changes)
    //    Source: RibbonRenderer, GlassRibbonRenderer, HighlightRenderer, GlowRenderer
    //    SHARED by all renderers — alignment guaranteed.
    // ═══════════════════════════════════════════════════════════════════════

    object Layout {

        /** Vertical scale: ribbon y values mapped to this fraction of canvas height. */
        const val Y_SCALE = 0.35f
        /** Horizontal padding from left edge. */
        const val H_PAD = 0.08f
        /** Horizontal span: ribbon x values mapped to this fraction of canvas width. */
        const val H_SPAN = 0.84f
        /** Maximum ribbon thickness as fraction of canvas height. */
        const val MAX_THICKNESS_FRAC = 0.12f
        /** Minimum thickness in pixels (prevents invisible path). */
        const val MIN_THICKNESS_PX = 1f
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 8. GLASS RENDER — Glass-specific rendering intensities (CONST)
    //    Source: GlassRibbonRenderer
    // ═══════════════════════════════════════════════════════════════════════

    object GlassRender {

        /** Internal reflection strength. */
        const val REFLECTION_STRENGTH = 0.3f
        /** Surface sheen height as fraction of ribbon thickness. */
        const val SHEEN_HEIGHT_FRAC = 0.15f
        /** Maximum sheen opacity. */
        const val MAX_SHEEN_ALPHA = 0.6f
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 9. HIGHLIGHT RENDER — Highlight-specific rendering intensities (CONST)
    //    Source: HighlightRenderer
    // ═══════════════════════════════════════════════════════════════════════

    object HighlightRender {

        /** Specular band height as fraction of ribbon half-thickness. */
        const val SPECULAR_BAND_FRAC = 0.3f
        /** Minimum specular sparkle radius in pixels. */
        const val MIN_SPECULAR_RADIUS = 4f
        /** Maximum peak sparkle dots per frame. */
        const val MAX_PEAK_SPARKLES = 8
        /** Fresnel rim stroke width in pixels. */
        const val FRESNEL_STROKE_WIDTH = 2.5f
    }
}
