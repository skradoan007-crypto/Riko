package com.rikodev.music.android.core.designsystem.riko.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — SOUND MOVES MOTION TOKENS (SM-UI-SPEC-001 v2.0, Section 5.1)
//
// Durations map 1:1 to the spec:
//   fast=100ms press/tap · base=200ms fades/color · medium=300ms pill/tab
//   morph + toggle slide · slow=400ms screen transitions / shared-element
// Easings: standard (decelerate) for entering/appearing; emphasized enter/exit
// for shared-element/hero; spring-snappy (stiffness 300, damping 26) for nav
// active indicator, toggle thumb, like burst.
//
// RULE: Never hardcode tween(300) — use RikoMotion presets.
// ─────────────────────────────────────────────────────────────────────────────

object RikoMotion {

    // ── Global motion scale (Settings → Motion intensity, A14) ──────────────
    // 1.0 = Normal · 0.4 = Reduced · 0 = Off · 1.3 = Expressive. The §5.1
    // tween ladder reads this at spec-creation, so a settings change applies
    // to every NEW animation. Springs keep their tuned physics (durationless
    // by nature) — documented gap, honest scope.
    @Volatile
    var motionScale: Float = 1.0f

    private fun scaled(base: Int): Int = (base * motionScale).toInt()

    // ── Durations (ms) — spec 5.1 ────────────────────────────────────────────
    const val Fast = 100          // duration-fast: press feedback, toggle flip
    const val Normal = 200        // duration-base: simple fades, color/opacity
    const val Emphasized = 300    // duration-medium: pill/tab morphs, toggle slide
    const val Screen = 400        // duration-slow: screen transitions, shared element
    const val Hero = 400          // duration-slow (hero/shared-element transform)
    const val ColorChange = 700   // album-art colour crossfade (spec 5.1)

    // ── Easings — spec 5.1 ───────────────────────────────────────────────────
    /** Standard decelerate curve — anything entering/appearing. */
    val FastOutSlowIn: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val Standard: Easing = FastOutSlowIn
    val Decelerate: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val Accelerate: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val Linear: Easing = LinearEasing

    /** Emphasized enter — shared-element/hero transitions (spec 5.1). */
    val EmphasizedEnter: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    /** Emphasized exit — shared-element/hero transitions (spec 5.1). */
    val EmphasizedExit: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    // ── Spring — spec spring-snappy (stiffness ~300, damping ~26) ────────────
    const val SpringStiffness = 300f
    const val SpringDamping = 26f

    fun <T> snappy() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = 300f
    )

    /** Color spring for icon/tint cross-fades synced to the nav slide (5.3). */
    fun springSpec() = spring<Color>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = 300f
    )

    // ── §24 canonical motion classes ─────────────────────────────────────────
    // §24 names six classes: fast · snappy · travel · normal · screen · settle.
    // fast() / normal() / screen() live in the tween block below and snappy()
    // sits above. travel() and settle() are the two that had no name; they are
    // pure references to specs that already ship their tuned values, so adding
    // the names introduces no new motion value (§80).
    /** §24 "travel" — an element moving distance: sheets, docks, expanders. */
    fun <T> travel(): FiniteAnimationSpec<T> = bottomSheet()

    /** §24 "settle" — coming to rest after interaction: seek release, scrub end. */
    fun <T> settle(): FiniteAnimationSpec<T> = seekBarSettle()

    // ── Tween presets ────────────────────────────────────────────────────────
    fun <T> fast() = tween<T>(scaled(Fast), easing = FastOutSlowIn)
    fun <T> normal() = tween<T>(scaled(Normal), easing = FastOutSlowIn)
    fun <T> emphasized() = tween<T>(scaled(Emphasized), easing = FastOutSlowIn)
    fun <T> screen() = tween<T>(scaled(Screen), easing = FastOutSlowIn)
    fun <T> hero() = tween<T>(scaled(Hero), easing = EmphasizedEnter)

    // ── Press feedback constants ─────────────────────────────────────────────
    /** Subtle press scale — spec 5.4 (scale 0.96, duration-fast). */
    const val PressScale = 0.96f
    const val PressScaleRelease = 1.0f

    /** Pressed surface dimming — spec 5.4 (primary buttons darken ~8%). */
    const val PressedOverlayAlpha = 0.08f

    // ═════════════════════════════════════════════════════════════════════════
    // ROLE-BASED SPECS — migrated from core.designsystem.animation.RikoMotion
    //
    // RIKO had THREE objects named RikoMotion (animation/, theme/, riko/theme/).
    // Member names are preserved deliberately so that migrating an old call-site
    // is a ONE-LINE import change with NO body edit for every role-based spec.
    // The legacy `Fast()`/`Normal()`/`Medium()`/`Slow()` tween helpers could not
    // keep their names (`Fast`/`Normal` already exist here as ms constants), so
    // those call-sites moved to the matching §5.1 tier instead: `Fast()` became
    // `fast()`, `Normal()` became `normal()`, `Medium()` became `emphasized()`,
    // and `Slow()` became `screen()`. There is no legacy 150/250/400/500 ladder
    // left anywhere in this object — callers with an old raw number must use the
    // §5.1 scale via fast() / normal() / emphasized() / screen().
    // ═════════════════════════════════════════════════════════════════════════

    /** Page-level screen transitions. No bounce — professional, clean. */
    fun <T> screenTransition(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** The satisfying bounce when you press a bubble icon. */
    fun <T> iconPress(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Playlist cards, EQ panel — medium bounce on open. */
    fun <T> cardExpand(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** EQ sheet, info sheet — gentle, weighty open/close. */
    fun <T> bottomSheet(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Album-change colour crossfade. Duration-based: colour lerp is linear. */
    fun <T> colorTransition(): FiniteAnimationSpec<T> = tween<T>(
        durationMillis = scaled(ColorChange),
        easing = FastOutSlowInEasing
    )

    /** Living background morph on song change — interruptible, velocity-safe. */
    fun <T> colorMorphTransition(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.82f,
        stiffness = 280f
    )

    /** Very slow, gentle spring for energy-reactive ambient pulsing. */
    fun <T> ambientBreathing(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.9f,
        stiffness = 120f
    )

    /** Glass tint shift on song change — snappier than the background. */
    fun <T> glassTintTransition(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.85f,
        stiffness = 350f
    )

    /** Seek: instant tracking while dragging (snapTo), gentle settle on release. */
    fun <T> seekBarSettle(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )

    /** EQ band sliders — spring to the exact value, medium bounce. */
    fun <T> eqSlider(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Mini player appear/disappear. */
    fun <T> miniPlayerSlide(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** New screen or card appearing. */
    fun <T> scaleIn(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Notification panel expand. */
    fun <T> notificationExpand(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Momentum-scroll fling decay. */
    fun flingDecay(): DecayAnimationSpec<Float> = exponentialDecay(frictionMultiplier = 1.0f)

    /** Simple spring presets — stiffness-based replacements for raw spring(). */
    fun <T> MediumSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> SoftSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> StiffSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Callers with a 150/250/400/500ms legacy number should use fast(),
     * normal(), emphasized() or screen() — the SM-UI-SPEC-001 §5.1 scale
     * (100/200/300/400) is the single source of truth, there are no
     * parallel duration ladders. */

    /** Bubble float reference values (driven by InfiniteTransition in BubbleIcon). */
    const val BUBBLE_FLOAT_DURATION = 3200
    const val BUBBLE_FLOAT_AMPLITUDE_DP = 6f

}
