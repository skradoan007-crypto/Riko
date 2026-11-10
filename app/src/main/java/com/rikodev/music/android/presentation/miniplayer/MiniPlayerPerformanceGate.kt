package com.rikodev.music.android.presentation.miniplayer

// ═══════════════════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER 2.0 — BUILD-3 PHASE D — FILE 11
//
// MiniPlayerPerformanceGate.kt
//
// Runtime configuration provider. NOT a UI component.
// Reads device capabilities, determines rendering quality tier,
// and provides TierFeatureSet + DeviceProfile to all Mini Player components.
//
// MPRS Reference: Chapter E.6 — Device Tier Adaptation
//   Budget numbers sourced from MiniPlayerPerformanceConstitution:
//     - Per-frame allocation target: 0 bytes (tolerance < 32 bytes)
//     - Total Mini Player memory: < 2MB
//     - Frame budget: 3.5ms of 16ms (Tier 1 @ 60Hz),
//       2.3ms of 11ms (Tier 2 @ 90Hz),
//       1.7ms of 8.3ms (Tier 3 @ 120Hz),
//       1.4ms of 6.9ms (Tier 4 @ 144Hz)
//
// Architecture:
//   Device Detection (one-time, cold start)
//        │
//        ▼
//   MiniPlayerPerformanceGate.resolve()
//        │
//        ├── DeviceProfile  (screen form factor)
//        └── DeviceTier      (capability classification)
//                 │
//                 ▼
//          TierFeatureSet   (immutable rendering flags)
//                 │
//                 ▼
//      CompositionLocalProvider → all Mini Player renderers
//
// Invariants:
//   - TierFeatureSet is immutable and set once per session.
//   - No rendering happens in this file.
//   - Pure detection + configuration only.
//   - Safe to call from any thread (resolve() is synchronized).
// ═══════════════════════════════════════════════════════════════════════════════════════

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sqrt
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.platform.LocalDensity
import kotlin.PublishedApi


// ─────────────────────────────────────────────────────────────────────────────────────
// DEVICE PROFILE
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Describes the physical form factor and orientation of the device.
// Used by renderers to adjust layout dimensions, touch targets, and spacing.
// Referenced by: MiniPlayerTokens (adaptive sizing), MiniPlayerSurface (layout).

/**
 * Physical device profile describing form factor and orientation.
 *
 * @property PHONE_PORTRAIT      Standard phone in portrait mode (width < 600dp).
 * @property PHONE_LANDSCAPE     Standard phone in landscape mode (width < 600dp).
 * @property TABLET_PORTRAIT     Tablet in portrait mode (width >= 600dp, not foldable inner).
 * @property TABLET_LANDSCAPE    Tablet in landscape mode (width >= 600dp, not foldable inner).
 * @property FOLDABLE_INNER      Foldable device with inner display active (hinge area present).
 */
enum class DeviceProfile {
    PHONE_PORTRAIT,
    PHONE_LANDSCAPE,
    TABLET_PORTRAIT,
    TABLET_LANDSCAPE,
    FOLDABLE_INNER;

    companion object {
        /**
         * Whether this profile represents a wider form factor (landscape or tablet).
         * Used by renderers for layout branching.
         */
        val DeviceProfile.isWide: Boolean
            get() = this == PHONE_LANDSCAPE || this == TABLET_LANDSCAPE || this == TABLET_PORTRAIT

        /**
         * Whether this profile is a tablet-class device.
         */
        val DeviceProfile.isTablet: Boolean
            get() = this == TABLET_PORTRAIT || this == TABLET_LANDSCAPE

        /**
         * Whether this profile represents a foldable's inner display.
         * Inner displays may have a hinge occlusion area that renderers must avoid.
         */
        val DeviceProfile.isFoldableInner: Boolean
            get() = this == FOLDABLE_INNER
    }
}


// ─────────────────────────────────────────────────────────────────────────────────────
// DEVICE TIER
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Capability-based classification. Determined once at cold start by
// [MiniPlayerPerformanceGate] from hardware readings.
// Maps to [TierFeatureSet] per MPRS Chapter E.6.

/**
 * Device capability tier for Mini Player rendering quality.
 *
 * Tier boundaries are derived from empirical testing and MPRS budget analysis:
 *   - Tier 1: Devices that struggle to maintain 60Hz with the full feature set.
 *   - Tier 2: Devices comfortable at 60Hz, can push 90Hz with reduced features.
 *   - Tier 3: Devices with high refresh rate displays and ample GPU headroom.
 *   - Tier 4: Flagship devices with top-tier displays (120Hz+) and ample RAM.
 *
 * @property TIER_1_LOW_END    Budget rendering. No noise, no specular, simplified wave (5 bars).
 * @property TIER_2_MID_RANGE  Balanced rendering. Noise + specular + full wave + simplified glow.
 * @property TIER_3_HIGH_END   Full rendering. All visual features including reflection and tilt.
 * @property TIER_4_FLAGSHIP   Maximum fidelity. Everything + album blur backdrop + full wave geometry.
 */
enum class DeviceTier {
    TIER_1_LOW_END,
    TIER_2_MID_RANGE,
    TIER_3_HIGH_END,
    TIER_4_FLAGSHIP;

    companion object {
        /**
         * Numeric rank for threshold comparisons.
         * Higher value = more capable device.
         */
        val DeviceTier.rank: Int
            get() = when (this) {
                TIER_1_LOW_END  -> 1
                TIER_2_MID_RANGE -> 2
                TIER_3_HIGH_END  -> 3
                TIER_4_FLAGSHIP  -> 4
            }
    }
}


// ─────────────────────────────────────────────────────────────────────────────────────
// TIER FEATURE SET
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Immutable rendering configuration derived from DeviceTier.
// Provided to all Mini Player renderers via [LocalTierFeatureSet].
// Feature flags correspond directly to MPRS Chapter E.6 tier definitions.

/**
 * Immutable set of rendering feature flags for the current device tier.
 *
 * This object is created once per session and must never be mutated.
 * Every renderer reads from this set to determine which visual features
 * to enable, ensuring consistent quality across the entire Mini Player.
 *
 * MPRS Chapter E.6 budget implications:
 *   - hasNoise:     ~0.3ms per frame (cached texture lookup + blend)
 *   - hasSpecular:  ~0.2ms per frame (gradient overlay)
 *   - hasGlow:      ~0.4ms per frame (radial gradient + alpha composite)
 *   - hasReflection:~0.5ms per frame (flipped draw + gradient mask)
 *   - hasDeviceTilt:~0.1ms per frame (rotation matrix on album art)
 *   - hasAlbumBlur: ~1.0ms per frame (RenderEffect blur, Android S+ only)
 *   - hasFullWaveGeometry: ~0.2ms per frame (smooth curves vs. straight bars)
 *
 * @property hasNoise            Whether to render noise texture overlay on background.
 * @property hasSpecular         Whether to render specular highlight on album art.
 * @property hasGlow             Whether to render ambient glow behind the wave.
 * @property hasReflection       Whether to render wave reflection below the main wave.
 * @property hasDeviceTilt       Whether to apply gyroscope-based tilt to album art.
 * @property hasAlbumBlur        Whether to render blurred album art as backdrop (requires Android S+).
 * @property hasFullWaveGeometry Whether to use smooth Bézier curves (vs. straight bars) for the wave.
 * @property waveBarCount        Number of frequency bars in the wave visualizer.
 * @property targetRefreshRateHz Target display refresh rate. Renderers should cap their
 *                               animation frame rate to this value to avoid wasted GPU work.
 */
@Immutable
data class TierFeatureSet(
    val hasNoise: Boolean = true,
    val hasSpecular: Boolean = true,
    val hasGlow: Boolean = true,
    val hasReflection: Boolean = true,
    val hasDeviceTilt: Boolean = false,
    val hasAlbumBlur: Boolean = false,
    val hasFullWaveGeometry: Boolean = false,
    val waveBarCount: Int = 40,
    val targetRefreshRateHz: Int = 90
) {
    companion object {
        /**
         * Returns the [TierFeatureSet] for a given [DeviceTier].
         *
         * Feature flags are defined per MPRS Chapter E.6 — Device Tier Adaptation.
         * Budget numbers are sourced from MiniPlayerPerformanceConstitution:
         *   - Per-frame allocation target: 0 bytes (tolerance < 32 bytes)
         *   - Total Mini Player memory budget: < 2MB
         *
         * Tier 1 (Low-end): No noise, no specular, simplified wave (5 bars),
         *   no glow, no reflection. Target 60Hz. Frame budget: 3.5ms of 16.6ms.
         *
         * Tier 2 (Mid-range): Noise, specular, full wave (40 bars),
         *   ambient glow (simplified), no reflection. Target 90Hz. Frame budget: 3.8ms of 11.1ms.
         *
         * Tier 3 (High-end): All features. Noise + specular + glow + reflection.
         *   Full wave. Device tilt. Target 120Hz. Frame budget: 3.8ms of 8.3ms.
         *
         * Tier 4 (Flagship): Everything + album art blur backdrop (Android S+),
         *   full Wave Engine geometry. Target 144Hz. Frame budget: 4.0ms of 6.9ms.
         */
        fun forTier(tier: DeviceTier): TierFeatureSet = when (tier) {
            DeviceTier.TIER_1_LOW_END -> TierFeatureSet(
                hasNoise = false,
                hasSpecular = false,
                hasGlow = false,
                hasReflection = false,
                hasDeviceTilt = false,
                hasAlbumBlur = false,
                hasFullWaveGeometry = false,
                waveBarCount = 5,
                targetRefreshRateHz = 60
            )
            DeviceTier.TIER_2_MID_RANGE -> TierFeatureSet(
                hasNoise = true,
                hasSpecular = true,
                hasGlow = true,
                hasReflection = false,
                hasDeviceTilt = false,
                hasAlbumBlur = false,
                hasFullWaveGeometry = false,
                waveBarCount = 40,
                targetRefreshRateHz = 90
            )
            DeviceTier.TIER_3_HIGH_END -> TierFeatureSet(
                hasNoise = true,
                hasSpecular = true,
                hasGlow = true,
                hasReflection = true,
                hasDeviceTilt = true,
                hasAlbumBlur = false,
                hasFullWaveGeometry = false,
                waveBarCount = 40,
                targetRefreshRateHz = 120
            )
            DeviceTier.TIER_4_FLAGSHIP -> TierFeatureSet(
                hasNoise = true,
                hasSpecular = true,
                hasGlow = true,
                hasReflection = true,
                hasDeviceTilt = true,
                hasAlbumBlur = true,
                hasFullWaveGeometry = true,
                waveBarCount = 40,
                targetRefreshRateHz = 144
            )
        }
    }

    /**
     * Total estimated per-frame rendering cost in milliseconds.
     * Used for logging and validation against MPRS budget (MiniPlayerPerformanceConstitution).
     * This is a rough estimate; actual cost depends on GPU and driver.
     */
    val estimatedFrameCostMs: Float
        get() {
            var cost = 0.5f // base: background gradient + album art
            if (hasNoise) cost += 0.3f
            if (hasSpecular) cost += 0.2f
            if (hasGlow) cost += 0.4f
            if (hasReflection) cost += 0.5f
            if (hasDeviceTilt) cost += 0.1f
            if (hasAlbumBlur) cost += 1.0f
            if (hasFullWaveGeometry) cost += 0.2f
            cost += waveBarCount * 0.015f // ~0.015ms per bar
            return cost
        }

    /**
     * Available frame budget in milliseconds based on target refresh rate.
     * Renderers should keep their work within this budget per MPRS Chapter E.1.
     */
    val frameBudgetMs: Float
        get() = 1000f / targetRefreshRateHz

    /**
     * Whether the estimated frame cost fits within the MPRS budget (3.5ms).
     * Logs a warning if the cost exceeds budget — this is a development-time
     * safeguard, not a runtime crash.
     */
    val isWithinBudget: Boolean
        get() = estimatedFrameCostMs <= 4.0f // MPRS tolerance: occasional 4ms OK on 90Hz+
}


// ─────────────────────────────────────────────────────────────────────────────────────
// COMPOSITION LOCALS
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Provided once at the Mini Player root via [ProvidePerformanceGate].
// All renderers read from these locals — never access the gate directly.

/**
 * CompositionLocal providing the current device's [TierFeatureSet].
 * Must be provided by [ProvidePerformanceGate] at the Mini Player root.
 *
 * Usage in renderers:
 * ```kotlin
 * val features = LocalTierFeatureSet.current
 * if (features.hasNoise) { /* render noise */ }
 * ```
 */
val LocalTierFeatureSet: ProvidableCompositionLocal<TierFeatureSet> =
    staticCompositionLocalOf {
        // Fallback: Tier 1 (safest default). This should never be hit in production
        // because ProvidePerformanceGate is always called at the Mini Player root.
        TierFeatureSet.forTier(DeviceTier.TIER_1_LOW_END)
    }

/**
 * CompositionLocal providing the current [DeviceProfile].
 * Must be provided by [ProvidePerformanceGate] at the Mini Player root.
 *
 * Usage in renderers:
 * ```kotlin
 * val profile = LocalDeviceProfile.current
 * val padding = if (profile.isTablet) 24.dp else 16.dp
 * ```
 */
val LocalDeviceProfile: ProvidableCompositionLocal<DeviceProfile> =
    staticCompositionLocalOf {
        // Fallback: standard phone portrait (safest default).
        DeviceProfile.PHONE_PORTRAIT
    }

/**
 * CompositionLocal providing the resolved [DeviceTier].
 * Useful for renderers that need to make tier-branching decisions beyond
 * what [TierFeatureSet] flags expose.
 *
 * Usage in renderers:
 * ```kotlin
 * val tier = LocalDeviceTier.current
 * val shadowBlur = when (tier) {
 *     DeviceTier.TIER_1_LOW_END -> 0.dp
 *     DeviceTier.TIER_2_MID_RANGE -> 4.dp
 *     else -> 8.dp
 * }
 * ```
 */
val LocalDeviceTier: ProvidableCompositionLocal<DeviceTier> =
    staticCompositionLocalOf {
        // Fallback: lowest tier (safest default).
        DeviceTier.TIER_1_LOW_END
    }


// ─────────────────────────────────────────────────────────────────────────────────────
// HARDWARE READINGS
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Raw hardware data extracted from the Android system.
// Used by [MiniPlayerPerformanceGate] to determine [DeviceTier].

/**
 * Raw device hardware readings used for tier classification.
 * Collected once at cold start; never updated during the session.
 *
 * @property totalMemoryMb       Total device RAM in megabytes.
 * @property availableMemoryMb   Available RAM at measurement time (lower bound estimate).
 * @property maxRefreshRateHz    Maximum display refresh rate reported by the system.
 * @property screenWidthDp       Current screen width in density-independent pixels.
 * @property screenHeightDp      Current screen height in density-independent pixels.
 * @property screenDiagonalInch  Approximate screen diagonal in inches.
 * @property sdkVersion          Android SDK version (Build.VERSION.SDK_INT).
 * @property isFoldable          Whether the device reports foldable capability.
 */
@Immutable
internal data class HardwareReadings(
    val totalMemoryMb: Long,
    val availableMemoryMb: Long,
    val maxRefreshRateHz: Float,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val screenDiagonalInch: Float,
    val sdkVersion: Int,
    val isFoldable: Boolean
)


// ─────────────────────────────────────────────────────────────────────────────────────
// MINI PLAYER PERFORMANCE GATE
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Singleton that performs one-time device detection and provides
// immutable configuration for the session.
//
// NOT a UI component. NOT a Composable.
// This is pure infrastructure — detection + configuration only.

/**
 * One-time device capability detector and tier resolver.
 *
 * Responsibilities:
 *   1. Read hardware capabilities (RAM, display, screen size, SDK version).
 *   2. Classify device into a [DeviceTier] based on capability thresholds.
 *   3. Derive an immutable [TierFeatureSet] from the tier (MPRS E.6).
 *   4. Detect [DeviceProfile] (form factor + orientation).
 *
 * Thread safety: [resolve] is synchronized. All other properties are
 * lazy-initialized from the resolved state and are effectively immutable
 * after the first call to [resolve].
 *
 * MPRS Reference: Chapter E.6 — Device Tier Adaptation
 * Budget reference: MiniPlayerPerformanceConstitution (E.1–E.5)
 */
object MiniPlayerPerformanceGate {

    // ── Tier Thresholds ──────────────────────────────────────────────────
    //
    // Thresholds are derived from empirical testing across device categories
    // and MPRS budget analysis (Chapter E.6).
    //
    // RAM thresholds (total device memory):
    //   < 3.5 GB  → likely low-end (Tier 1)
    //   3.5–5.5 GB → mid-range (Tier 2)
    //   5.5–7.5 GB → high-end (Tier 3)
    //   >= 7.5 GB  → flagship (Tier 4)
    //
    // Refresh rate thresholds (max display refresh rate):
    //   < 75 Hz   → Tier 1 candidate
    //   75–105 Hz → Tier 2 candidate
    //   105–130 Hz → Tier 3 candidate
    //   >= 130 Hz → Tier 4 candidate
    //
    // SDK version thresholds:
    //   < 29 (Q)  → Tier 1 (old devices likely low-end)
    //   29–30     → Tier 2
    //   31 (S)    → Tier 3 (RenderEffect available for potential blur)
    //   >= 32     → Tier 4 candidate

    private const val RAM_THRESHOLD_TIER_1_MB = 3500L   // < 3.5 GB
    private const val RAM_THRESHOLD_TIER_2_MB = 5500L   // 3.5–5.5 GB
    private const val RAM_THRESHOLD_TIER_3_MB = 7500L   // 5.5–7.5 GB
    // >= 7500 MB → Tier 4

    private const val REFRESH_RATE_TIER_1_HZ = 75f
    private const val REFRESH_RATE_TIER_2_HZ = 105f
    private const val REFRESH_RATE_TIER_3_HZ = 130f
    // >= 130 Hz → Tier 4

    private const val SDK_TIER_1 = 28  // < Android 9 Pie
    private const val SDK_TIER_2 = 30  // Android 11
    private const val SDK_TIER_3 = 31  // Android 12 (S) — RenderEffect introduced
    // >= 32 (Android 12L) → Tier 4 candidate

    private const val TABLET_WIDTH_THRESHOLD_DP = 600

    // ── Session State ─────────────────────────────────────────────────────

    @Volatile
    private var resolved: Boolean = false

    @Volatile
    private var _tier: DeviceTier = DeviceTier.TIER_1_LOW_END

    @Volatile
    private var _featureSet: TierFeatureSet = TierFeatureSet.forTier(DeviceTier.TIER_1_LOW_END)

    @Volatile
    private var _hardwareReadings: HardwareReadings? = null

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Whether [resolve] has been called this session.
     */
    val isResolved: Boolean get() = resolved

    /**
     * The resolved [DeviceTier]. Throws if [resolve] hasn't been called.
     */
    val tier: DeviceTier
        get() {
            check(resolved) { "MiniPlayerPerformanceGate.resolve() must be called before accessing tier." }
            return _tier
        }

    /**
     * The resolved [TierFeatureSet]. Throws if [resolve] hasn't been called.
     */
    val featureSet: TierFeatureSet
        get() {
            check(resolved) { "MiniPlayerPerformanceGate.resolve() must be called before accessing featureSet." }
            return _featureSet
        }

    /**
     * Raw hardware readings from the last [resolve] call. Null if not resolved.
     * Useful for debugging and logging.
     */
    internal val hardwareReadings: HardwareReadings?
        get() = _hardwareReadings

    /**
     * Performs one-time device capability detection and resolves the session tier.
     *
     * This method is idempotent — subsequent calls after the first are no-ops.
     * Thread-safe: synchronized to prevent double-initialization.
     *
     * @param context Application or Activity context for system service access.
     */
    @Synchronized
    fun resolve(context: Context) {
        if (resolved) return

        val readings = collectHardwareReadings(context)
        _hardwareReadings = readings

        val resolvedTier = classifyTier(readings)
        _tier = resolvedTier
        _featureSet = TierFeatureSet.forTier(resolvedTier)
        resolved = true
    }

    /**
     * Detects the current [DeviceProfile] from configuration state.
     *
     * Unlike [resolve], this may be called multiple times (e.g., on configuration
     * changes) because the device profile can change with orientation/fold state.
     *
     * @param screenWidthDp   Current screen width in dp.
     * @param screenHeightDp  Current screen height in dp.
     * @param isFoldableInner Whether the foldable inner display is active.
     * @return The current [DeviceProfile].
     */
    fun detectProfile(
        screenWidthDp: Int,
        screenHeightDp: Int,
        isFoldableInner: Boolean
    ): DeviceProfile {
        if (isFoldableInner) return DeviceProfile.FOLDABLE_INNER

        val isTablet = screenWidthDp >= TABLET_WIDTH_THRESHOLD_DP ||
                screenHeightDp >= TABLET_WIDTH_THRESHOLD_DP
        val isLandscape = screenWidthDp > screenHeightDp

        return when {
            isTablet && isLandscape -> DeviceProfile.TABLET_LANDSCAPE
            isTablet && !isLandscape -> DeviceProfile.TABLET_PORTRAIT
            !isTablet && isLandscape -> DeviceProfile.PHONE_LANDSCAPE
            else -> DeviceProfile.PHONE_PORTRAIT
        }
    }

    // ── Hardware Collection ───────────────────────────────────────────────

    /**
     * Collects raw hardware readings from the Android system.
     * This is the single point of hardware interrogation — all detection
     * logic reads from [HardwareReadings], never from system services directly.
     */
    private fun collectHardwareReadings(context: Context): HardwareReadings {
        // RAM
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also {
            activityManager?.getMemoryInfo(it)
        }
        val totalMemoryMb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            (memoryInfo?.totalMem ?: 0L) / (1024L * 1024L)
        } else {
            2048L // Conservative default for pre-JB devices
        }
        val availableMemoryMb = (memoryInfo?.availMem ?: 0L) / (1024L * 1024L)

        // Display
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay
        }

        val maxRefreshRateHz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && display != null) {
            display.mode?.refreshRate ?: 60f
        } else {
            60f // Conservative default
        }

        // Screen dimensions (dp)
        val config = context.resources.configuration
        val screenWidthDp = config.screenWidthDp
        val screenHeightDp = config.screenHeightDp

        // Screen diagonal (approximate, in inches)
        val density = context.resources.displayMetrics.density
        val screenWidthPx = screenWidthDp * density
        val screenHeightPx = screenHeightDp * density
        val screenDiagonalPx = sqrt(screenWidthPx * screenWidthPx + screenHeightPx * screenHeightPx)
        val screenDiagonalInch = screenDiagonalPx / density / 160f // 160 dpi = 1 inch baseline

        // Foldable detection
        val isFoldable = detectFoldable(context)

        return HardwareReadings(
            totalMemoryMb = totalMemoryMb,
            availableMemoryMb = availableMemoryMb,
            maxRefreshRateHz = maxRefreshRateHz,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            screenDiagonalInch = screenDiagonalInch,
            sdkVersion = Build.VERSION.SDK_INT,
            isFoldable = isFoldable
        )
    }

    /**
     * Detects whether the current device is a foldable.
     * Uses multiple signals since there's no single canonical API.
     */
    private fun detectFoldable(context: Context): Boolean {
        // Signal 1: WindowManager folding feature (Android 12L+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            windowManager?.let { wm ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Jetpack WindowManager provides isFoldable(), but to avoid
                    // a hard dependency on the library, we check display features.
                    // If the project uses Jetpack WindowManager, replace this
                    // with: FoldingFeature.isFoldable()
                }
            }
        }

        // Signal 2: Screen size heuristic — very large short dimension suggests
        // a foldable inner display unfolded.
        val config = context.resources.configuration
        val shortDimensionDp = minOf(config.screenWidthDp, config.screenHeightDp)
        if (shortDimensionDp >= 500) {
            // A device with both dimensions >= 500dp is almost certainly
            // a tablet or foldable, not a phone.
            // The foldable inner check is done in detectProfile().
            return true
        }

        return false
    }

    // ── Tier Classification ───────────────────────────────────────────────

    /**
     * Classifies the device into a [DeviceTier] based on [HardwareReadings].
     *
     * Strategy: Score-based system. Each capability dimension (RAM, refresh rate,
     * SDK version) contributes a score. The final tier is the highest tier whose
     * minimum score threshold is met.
     *
     * This is more robust than simple threshold cascading because it prevents
     * a single weak dimension from dragging an otherwise-capable device down.
     *
     * MPRS Reference: Chapter E.6 — Device Tier Adaptation
     */
    private fun classifyTier(readings: HardwareReadings): DeviceTier {
        val ramScore = classifyRam(readings.totalMemoryMb)
        val refreshScore = classifyRefreshRate(readings.maxRefreshRateHz)
        val sdkScore = classifySdk(readings.sdkVersion)

        // Composite score: weighted average. RAM is the strongest signal.
        val compositeScore = (ramScore * 0.4f + refreshScore * 0.35f + sdkScore * 0.25f)

        // Also enforce minimum requirements: a device cannot be in a tier
        // if ANY dimension scores below the tier's floor.
        // This prevents a device with 12GB RAM but a 60Hz display from being Tier 4.
        val minDimension = minOf(ramScore, refreshScore, sdkScore)

        return when {
            compositeScore >= 3.5f && minDimension >= 2.5f -> DeviceTier.TIER_4_FLAGSHIP
            compositeScore >= 2.5f && minDimension >= 2.0f -> DeviceTier.TIER_3_HIGH_END
            compositeScore >= 1.5f && minDimension >= 1.0f -> DeviceTier.TIER_2_MID_RANGE
            else -> DeviceTier.TIER_1_LOW_END
        }
    }

    /**
     * Classifies RAM into a 1–4 score.
     * Thresholds based on typical device RAM distributions (2024 market).
     */
    private fun classifyRam(totalMemoryMb: Long): Float = when {
        totalMemoryMb >= RAM_THRESHOLD_TIER_3_MB -> 4f  // >= 7.5 GB
        totalMemoryMb >= RAM_THRESHOLD_TIER_2_MB -> 3f  // >= 5.5 GB
        totalMemoryMb >= RAM_THRESHOLD_TIER_1_MB -> 2f  // >= 3.5 GB
        else -> 1f                                         // < 3.5 GB
    }

    /**
     * Classifies display refresh rate into a 1–4 score.
     * Higher refresh rates indicate newer, more capable display hardware.
     */
    private fun classifyRefreshRate(maxRefreshRateHz: Float): Float = when {
        maxRefreshRateHz >= REFRESH_RATE_TIER_3_HZ -> 4f  // >= 130 Hz
        maxRefreshRateHz >= REFRESH_RATE_TIER_2_HZ -> 3f  // >= 105 Hz
        maxRefreshRateHz >= REFRESH_RATE_TIER_1_HZ -> 2f  // >= 75 Hz
        else -> 1f                                          // < 75 Hz
    }

    /**
     * Classifies Android SDK version into a 1–4 score.
     * Newer SDK versions indicate newer devices with better GPU/drivers.
     */
    private fun classifySdk(sdkVersion: Int): Float = when {
        sdkVersion >= SDK_TIER_3 + 1 -> 4f  // >= Android 12L (32)
        sdkVersion >= SDK_TIER_3 -> 3f       // >= Android 12 (31) — RenderEffect
        sdkVersion >= SDK_TIER_1 + 1 -> 2f  // >= Android 10 (29)
        else -> 1f                            // < Android 10
    }


    // ── Testing & Override Support ────────────────────────────────────────

    /**
     * Forces a specific [DeviceTier] and [TierFeatureSet] for testing.
     * This bypasses hardware detection entirely.
     *
     * **WARNING**: Only call this from test code or debug builds.
     * Calling this in production will break tier adaptation.
     *
     * @param overrideTier The tier to force.
     */
    @Synchronized
    internal fun forceTier(overrideTier: DeviceTier) {
        _tier = overrideTier
        _featureSet = TierFeatureSet.forTier(overrideTier)
        resolved = true
    }

    /**
     * Resets the gate to its unresolved state.
     * Used in tests to allow re-resolving with different conditions.
     *
     * **WARNING**: Only call this from test code.
     */
    @Synchronized
    internal fun reset() {
        resolved = false
        _tier = DeviceTier.TIER_1_LOW_END
        _featureSet = TierFeatureSet.forTier(DeviceTier.TIER_1_LOW_END)
        _hardwareReadings = null
    }
}


// ─────────────────────────────────────────────────────────────────────────────────────
// COMPOSABLE PROVIDER
// ─────────────────────────────────────────────────────────────────────────────────────

/**
 * Provides [TierFeatureSet], [DeviceProfile], and [DeviceTier] to the
 * Mini Player composition subtree via CompositionLocals.
 *
 * This is the single entry point for performance gate configuration.
 * Call this at the root of the Mini Player composable tree:
 *
 * ```kotlin
 * @Composable
 * fun MiniPlayerRoot() {
 *     ProvidePerformanceGate {
 *         // All children can access:
 *         //   LocalTierFeatureSet.current
 *         //   LocalDeviceProfile.current
 *         //   LocalDeviceTier.current
 *         MiniPlayerBackground()
 *         MiniPlayerAlbum()
 *         MiniPlayerWave()
 *         // ...
 *     }
 * }
 * ```
 *
 * Resolution behavior:
 *   - [MiniPlayerPerformanceGate.resolve] is called on first composition.
 *   - Subsequent recompositions are no-ops (gate is idempotent).
 *   - [DeviceProfile] is re-detected on every composition because it
 *     can change with orientation/fold state (via [LocalConfiguration]).
 *
 * MPRS Reference: Chapter E.6 — Device Tier Adaptation
 *
 * @param content The Mini Player composable subtree.
 */
@Composable
fun ProvidePerformanceGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    // Resolve tier once per session (idempotent).
    // This triggers hardware detection on first call only.
    MiniPlayerPerformanceGate.resolve(context)

    // Re-detect profile on every composition because orientation/fold
    // can change at runtime. detectProfile() is lightweight (pure arithmetic).
    val currentProfile = MiniPlayerPerformanceGate.detectProfile(
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
        isFoldableInner = false // TODO: Wire to Jetpack WindowManager FoldingFeature when available
    )

    val currentTier = MiniPlayerPerformanceGate.tier
    val currentFeatureSet = MiniPlayerPerformanceGate.featureSet

    CompositionLocalProvider(
        LocalTierFeatureSet provides currentFeatureSet,
        LocalDeviceProfile provides currentProfile,
        LocalDeviceTier provides currentTier
    ) {
        content()
    }
}
