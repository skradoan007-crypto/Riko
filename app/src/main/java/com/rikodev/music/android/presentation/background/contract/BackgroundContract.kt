/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 9 — Background Experience
 *
 * File 1 / 5 — BackgroundContract.kt + Reducer + Host + Test
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The most "production vs prototype" differentiating milestone.
 * Playback must survive:
 *   - Screen off (1+ hour)
 *   - Doze mode / battery optimization
 *   - Audio focus loss (incoming call)
 *   - Background crash detection
 *
 * SCOPE
 * -----
 *   - Foreground service notification (correct type for Android 14+)
 *   - Doze mode / battery optimization handling
 *   - Audio focus loss → duck or pause → resume
 *   - Background crash reporting
 *   - Lock-screen / notification controls
 *   - MediaSession integration
 *
 * v2.0 Production Pillars enforced:
 *   🔒 Security — foreground service type declared correctly
 *   📊 Performance — battery drain acceptable
 *   🛡 Resilience — 1-hour background playback stable
 *
 * © RIKO Experience OS — Background Tier
 */

package com.rikodev.music.android.presentation.background.contract

// ═══════════════════════════════════════════════════════════════════════════
// FOREGROUND SERVICE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Foreground service notification config.
 *
 * Android 14+ requires foreground service type declaration.
 * For media playback, type = "mediaPlayback".
 */
data class ForegroundServiceConfig(
    val notificationChannelId: String,
    val notificationChannelName: String,
    val notificationChannelDescription: String,
    val serviceType: String, // "mediaPlayback" for Android 14+
    val showLockScreenControls: Boolean,
    val showNotificationControls: Boolean,
    val smallIconResId: Int,
    val contentIntentPendingIntentRequestCode: Int,
) {
    companion object {
        val DEFAULT = ForegroundServiceConfig(
            notificationChannelId = "riko_playback",
            notificationChannelName = "RIKO Playback",
            notificationChannelDescription = "Media playback controls for RIKO",
            serviceType = "mediaPlayback",
            showLockScreenControls = true,
            showNotificationControls = true,
            smallIconResId = 0, // filled by host layer from R.drawable.*
            contentIntentPendingIntentRequestCode = 1001,
        )
    }
}

/**
 * State of the foreground service.
 */
enum class ForegroundServiceState(val displayName: String) {
    STOPPED("Stopped"),
    STARTING("Starting"),
    RUNNING("Running"),
    STOPPING("Stopping"),
    FAILED("Failed"),
}

// ═══════════════════════════════════════════════════════════════════════════
// DOZE / BATTERY OPTIMIZATION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Doze mode state.
 */
enum class DozeState(val displayName: String) {
    ACTIVE("Active (device in use)"),
    INACTIVE("Inactive (screen off, not dozing)"),
    LIGHT_DOZE("Light Doze"),
    DEEP_DOZE("Deep Doze"),
}

/**
 * Battery optimization state.
 */
enum class BatteryOptimizationState(val displayName: String) {
    OPTIMIZED("Battery optimized (may interrupt playback)"),
    NOT_OPTIMIZED("Not optimized (playback safe)"),
    UNKNOWN("Unknown"),
}

// ═══════════════════════════════════════════════════════════════════════════
// AUDIO FOCUS
// ═══════════════════════════════════════════════════════════════════════════

enum class AudioFocusState(val displayName: String) {
    HAVE_FOCUS("Have focus — playing normally"),
    DUCKED("Ducked — lowered volume for transient loss"),
    PAUSED("Paused — sustained focus loss"),
    LOST("Lost — another app holds focus"),
}

enum class AudioFocusLossType(val displayName: String) {
    TRANSIENT("Transient (e.g. notification sound)"),
    TRANSIENT_DUCK("Transient with duck (e.g. navigation voice)"),
    SUSTAINED("Sustained (e.g. another music app)"),
    PERMANENT("Permanent (e.g. phone call)"),
}

// ═══════════════════════════════════════════════════════════════════════════
// MEDIA SESSION
// ═══════════════════════════════════════════════════════════════════════════

data class MediaSessionState(
    val isActive: Boolean,
    val playbackState: String, // PLAYING / PAUSED / BUFFERING / STOPPED
    val currentPositionMs: Long,
    val durationMs: Long,
    val title: String?,
    val artist: String?,
    val albumArtUri: String?,
    val queueSize: Int,
    val queueIndex: Int?,
)

// ═══════════════════════════════════════════════════════════════════════════
// BACKGROUND STATE
// ═══════════════════════════════════════════════════════════════════════════

data class BackgroundState(
    val foregroundServiceState: ForegroundServiceState,
    val dozeState: DozeState,
    val batteryOptimization: BatteryOptimizationState,
    val audioFocus: AudioFocusState,
    val mediaSession: MediaSessionState,
    val lastBackgroundCrashMs: Long?,
    val backgroundSessionDurationMs: Long,
    val batteryDrainPercent: Float,
    val isProcessing: Boolean,
    val lastActionDescription: String?,
) {
    companion object {
        val INITIAL = BackgroundState(
            foregroundServiceState = ForegroundServiceState.STOPPED,
            dozeState = DozeState.ACTIVE,
            batteryOptimization = BatteryOptimizationState.UNKNOWN,
            audioFocus = AudioFocusState.LOST,
            mediaSession = MediaSessionState(
                isActive = false,
                playbackState = "STOPPED",
                currentPositionMs = 0,
                durationMs = 0,
                title = null,
                artist = null,
                albumArtUri = null,
                queueSize = 0,
                queueIndex = null,
            ),
            lastBackgroundCrashMs = null,
            backgroundSessionDurationMs = 0,
            batteryDrainPercent = 0f,
            isProcessing = false,
            lastActionDescription = null,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ACTION / EVENT / REQUEST
// ═══════════════════════════════════════════════════════════════════════════

sealed class BackgroundAction {
    data object StartForegroundService : BackgroundAction()
    data object StopForegroundService : BackgroundAction()
    data class UpdateNotification(val mediaSession: MediaSessionState) : BackgroundAction()
    data class HandleAudioFocusLoss(val lossType: AudioFocusLossType) : BackgroundAction()
    data object HandleAudioFocusRegain : BackgroundAction()
    data class HandleDozeMode(val doze: DozeState) : BackgroundAction()
    data object RequestBatteryOptimizationExemption : BackgroundAction()
    data object ReportBackgroundCrash : BackgroundAction()
}

sealed class BackgroundEvent {
    data object ForegroundServiceStarted : BackgroundEvent()
    data object ForegroundServiceStopped : BackgroundEvent()
    data object ForegroundServiceFailed : BackgroundEvent()
    data class NotificationUpdated(val mediaSession: MediaSessionState) : BackgroundEvent()
    data class AudioFocusLost(val lossType: AudioFocusLossType) : BackgroundEvent()
    data object AudioFocusRegained : BackgroundEvent()
    data class DozeModeChanged(val doze: DozeState) : BackgroundEvent()
    data class BatteryOptimizationChanged(val state: BatteryOptimizationState) : BackgroundEvent()
    data object BatteryOptimizationExemptionRequested : BackgroundEvent()
    data class BackgroundSessionDurationUpdated(val durationMs: Long) : BackgroundEvent()
    data class BatteryDrainUpdated(val percent: Float) : BackgroundEvent()
    data class BackgroundCrashReported(val timestampMs: Long, val stackTrace: String) : BackgroundEvent()
}

sealed class BackgroundCoordinatorRequest {
    data class StartForegroundService(val config: ForegroundServiceConfig, val mediaSession: MediaSessionState) : BackgroundCoordinatorRequest()
    data object StopForegroundService : BackgroundCoordinatorRequest()
    data class UpdateNotification(val mediaSession: MediaSessionState) : BackgroundCoordinatorRequest()
    data class DuckPlayback(val duckFactor: Float) : BackgroundCoordinatorRequest()
    data object PausePlayback : BackgroundCoordinatorRequest()
    data object ResumePlayback : BackgroundCoordinatorRequest()
    data class UpdateMediaSession(val state: MediaSessionState) : BackgroundCoordinatorRequest()
    data object RequestBatteryOptimizationExemption : BackgroundCoordinatorRequest()
    data class ReportCrash(val timestampMs: Long, val stackTrace: String) : BackgroundCoordinatorRequest()
    data object EmitHaptic : BackgroundCoordinatorRequest()
    data class LogEvent(val event: String, val params: Map<String, Any?>) : BackgroundCoordinatorRequest()
}
