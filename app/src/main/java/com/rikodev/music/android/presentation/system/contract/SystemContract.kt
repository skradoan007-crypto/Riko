/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 10 — System Integration
 *
 * File 1 / 3 — SystemContract.kt + Reducer + Host + Test
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Integrates RIKO with the broader Android system:
 *   - Android Auto / car mode (driving-safe UI)
 *   - Storage Access Framework (SAF) for file access
 *   - Foldable / tablet adaptive layout
 *   - Permission denial graceful paths
 *
 * SCOPE
 * -----
 *   - Android Auto Media Browser Service
 *   - Car mode UI (larger touch targets, simplified)
 *   - SAF for music file access (no legacy storage permission)
 *   - Foldable layout (Now Playing two-pane on tablet / unfolded)
 *   - Permission denial handling (mic / notification / storage)
 *
 * v2.0 Production Pillars:
 *   🔒 Security — SAF, no deprecated permissions (Play Store policy)
 *   ♿ Accessibility — Android Auto driving safety guidelines
 *   🌍 i18n — RTL layout, foldable form factors
 *
 * © RIKO Experience OS — System Integration Tier
 */

package com.rikodev.music.android.presentation.system.contract

// ═══════════════════════════════════════════════════════════════════════════
// ANDROID AUTO
// ═══════════════════════════════════════════════════════════════════════════

enum class AutoMode(val displayName: String) {
    DISABLED("Disabled"),
    CONNECTED("Connected"),
    DRIVING("Driving"),
}

data class AutoConfig(
    val enabled: Boolean,
    val minTouchTargetDp: Int, // 96dp for driving safety
    val simplifiedLayout: Boolean,
    val voiceActionsEnabled: Boolean,
) {
    companion object {
        val DRIVING_SAFE = AutoConfig(
            enabled = true,
            minTouchTargetDp = 96,
            simplifiedLayout = true,
            voiceActionsEnabled = true,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// STORAGE ACCESS FRAMEWORK
// ═══════════════════════════════════════════════════════════════════════════

data class SafConfig(
    val useScopedStorage: Boolean,
    val treeUris: List<String>, // user-granted folder URIs
    val maxTreeUris: Int,
) {
    companion object {
        val DEFAULT = SafConfig(
            useScopedStorage = true,
            treeUris = emptyList(),
            maxTreeUris = 10,
        )
    }
}

enum class SafPermissionState(val displayName: String) {
    GRANTED("Granted"),
    DENIED("Denied"),
    PARTIAL("Partial"),
    NOT_REQUESTED("Not Requested"),
}

// ═══════════════════════════════════════════════════════════════════════════
// FOLDABLE / FORM FACTOR
// ═══════════════════════════════════════════════════════════════════════════

enum class FormFactor(val displayName: String) {
    PHONE_PORTRAIT("Phone Portrait"),
    PHONE_LANDSCAPE("Phone Landscape"),
    TABLET_PORTRAIT("Tablet Portrait"),
    TABLET_LANDSCAPE("Tablet Landscape"),
    FOLDABLE_UNFOLDED("Foldable Unfolded"),
    FOLDABLE_FOLDED("Foldable Folded"),
}

data class FormFactorConfig(
    val currentFormFactor: FormFactor,
    val isTwoPaneLayout: Boolean,
    val isTablet: Boolean,
    val isFoldable: Boolean,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
)

// ═══════════════════════════════════════════════════════════════════════════
// PERMISSIONS
// ═══════════════════════════════════════════════════════════════════════════

enum class PermissionType(val displayName: String, val androidPermission: String) {
    NOTIFICATION("Notifications", "android.permission.POST_NOTIFICATIONS"),
    MICROPHONE("Microphone", "android.permission.RECORD_AUDIO"),
    STORAGE("Storage (SAF)", "android.permission.READ_MEDIA_AUDIO"),
    FOREGROUND_SERVICE("Foreground Service", "android.permission.FOREGROUND_SERVICE"),
    FOREGROUND_SERVICE_MEDIA_PLAYBACK("Media Playback Service", "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"),
    WAKE_LOCK("Wake Lock", "android.permission.WAKE_LOCK"),
    BLUETOOTH_CONNECT("Bluetooth Connect", "android.permission.BLUETOOTH_CONNECT"),
}

enum class PermissionState(val displayName: String) {
    GRANTED("Granted"),
    DENIED("Denied"),
    DENIED_WITH_RATIONALE("Denied (don't ask again)"),
    NOT_REQUESTED("Not Requested"),
    NOT_REQUIRED("Not Required"),
}

data class PermissionStatus(
    val type: PermissionType,
    val state: PermissionState,
    val shouldShowRationale: Boolean,
    val lastRequestedMs: Long?,
)

// ═══════════════════════════════════════════════════════════════════════════
// SYSTEM STATE
// ═══════════════════════════════════════════════════════════════════════════

data class SystemState(
    val autoMode: AutoMode,
    val autoConfig: AutoConfig,
    val safConfig: SafConfig,
    val safPermission: SafPermissionState,
    val formFactor: FormFactorConfig,
    val permissions: List<PermissionStatus>,
    val isProcessing: Boolean,
    val lastActionDescription: String?,
) {
    companion object {
        val INITIAL = SystemState(
            autoMode = AutoMode.DISABLED,
            autoConfig = AutoConfig.DRIVING_SAFE,
            safConfig = SafConfig.DEFAULT,
            safPermission = SafPermissionState.NOT_REQUESTED,
            formFactor = FormFactorConfig(
                currentFormFactor = FormFactor.PHONE_PORTRAIT,
                isTwoPaneLayout = false,
                isTablet = false,
                isFoldable = false,
                screenWidthDp = 360,
                screenHeightDp = 640,
            ),
            permissions = PermissionType.values().map {
                PermissionStatus(it, PermissionState.NOT_REQUESTED, false, null)
            },
            isProcessing = false,
            lastActionDescription = null,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ACTION / EVENT / REQUEST
// ═══════════════════════════════════════════════════════════════════════════

sealed class SystemAction {
    data class SetAutoMode(val mode: AutoMode) : SystemAction()
    data class RequestSafPermission(val initialUri: String?) : SystemAction()
    data class AddSafTreeUri(val uri: String) : SystemAction()
    data class RemoveSafTreeUri(val uri: String) : SystemAction()
    data class RequestPermission(val type: PermissionType) : SystemAction()
    data class OpenAppSettings(val type: PermissionType?) : SystemAction()
    data class SetFormFactor(val config: FormFactorConfig) : SystemAction()
}

sealed class SystemEvent {
    data class AutoModeChanged(val mode: AutoMode) : SystemEvent()
    data class SafPermissionGranted(val treeUris: List<String>) : SystemEvent()
    data class SafPermissionDenied(val reason: String) : SystemEvent()
    data class SafTreeUriAdded(val uri: String) : SystemEvent()
    data class SafTreeUriRemoved(val uri: String) : SystemEvent()
    data class PermissionGranted(val type: PermissionType) : SystemEvent()
    data class PermissionDenied(val type: PermissionType, val withRationale: Boolean) : SystemEvent()
    data class FormFactorChanged(val config: FormFactorConfig) : SystemEvent()
    data class AppSettingsOpened(val type: PermissionType?) : SystemEvent()
}

sealed class SystemCoordinatorRequest {
    data class RequestSafPermission(val initialUri: String?) : SystemCoordinatorRequest()
    data class RequestPermission(val type: PermissionType) : SystemCoordinatorRequest()
    data class OpenAppSettings(val type: PermissionType?) : SystemCoordinatorRequest()
    data class PersistSafTreeUris(val uris: List<String>) : SystemCoordinatorRequest()
    data class SetAutoMode(val mode: AutoMode) : SystemCoordinatorRequest()
    data object EmitHaptic : SystemCoordinatorRequest()
    data class ShowSnackbar(val message: String) : SystemCoordinatorRequest()
    data class LogEvent(val event: String, val params: Map<String, Any?>) : SystemCoordinatorRequest()
}

// ═══════════════════════════════════════════════════════════════════════════
// REDUCER + COORDINATOR + HOST + TEST (combined)
// ═══════════════════════════════════════════════════════════════════════════

object SystemReducer {
    fun reduce(state: SystemState, event: SystemEvent): Pair<SystemState, List<SystemAction>> {
        return when (event) {
            is SystemEvent.AutoModeChanged -> state.copy(
                autoMode = event.mode,
                lastActionDescription = "Auto: ${event.mode.displayName}",
            ) to listOf(SystemAction.SetAutoMode(event.mode))

            is SystemEvent.SafPermissionGranted -> {
                val newConfig = state.safConfig.copy(treeUris = event.treeUris)
                state.copy(
                    safConfig = newConfig,
                    safPermission = if (event.treeUris.isEmpty()) SafPermissionState.GRANTED else SafPermissionState.GRANTED,
                    lastActionDescription = "Storage access granted",
                ) to event.treeUris.map { SystemAction.AddSafTreeUri(it) }
            }

            is SystemEvent.SafPermissionDenied -> state.copy(
                safPermission = SafPermissionState.DENIED,
                lastActionDescription = "Storage access denied: ${event.reason}",
            ) to emptyList()

            is SystemEvent.SafTreeUriAdded -> {
                if (event.uri in state.safConfig.treeUris) {
                    state to emptyList()
                } else {
                    val newUris = (state.safConfig.treeUris + event.uri).take(state.safConfig.maxTreeUris)
                    state.copy(
                        safConfig = state.safConfig.copy(treeUris = newUris),
                        lastActionDescription = "Folder added",
                    ) to listOf(SystemAction.AddSafTreeUri(event.uri))
                }
            }

            is SystemEvent.SafTreeUriRemoved -> {
                val newUris = state.safConfig.treeUris - event.uri
                state.copy(
                    safConfig = state.safConfig.copy(treeUris = newUris),
                    lastActionDescription = "Folder removed",
                ) to listOf(SystemAction.RemoveSafTreeUri(event.uri))
            }

            is SystemEvent.PermissionGranted -> {
                val newPerms = state.permissions.map {
                    if (it.type == event.type) it.copy(
                        state = PermissionState.GRANTED,
                        lastRequestedMs = System.currentTimeMillis(),
                    ) else it
                }
                state.copy(
                    permissions = newPerms,
                    lastActionDescription = "${event.type.displayName} granted",
                ) to emptyList()
            }

            is SystemEvent.PermissionDenied -> {
                val newPerms = state.permissions.map {
                    if (it.type == event.type) it.copy(
                        state = if (event.withRationale) PermissionState.DENIED_WITH_RATIONALE else PermissionState.DENIED,
                        shouldShowRationale = event.withRationale,
                        lastRequestedMs = System.currentTimeMillis(),
                    ) else it
                }
                state.copy(
                    permissions = newPerms,
                    lastActionDescription = "${event.type.displayName} denied",
                ) to emptyList()
            }

            is SystemEvent.FormFactorChanged -> state.copy(
                formFactor = event.config,
                lastActionDescription = "Form factor: ${event.config.currentFormFactor.displayName}",
            ) to listOf(SystemAction.SetFormFactor(event.config))

            is SystemEvent.AppSettingsOpened -> state.copy(
                lastActionDescription = "App settings opened",
            ) to listOf(SystemAction.OpenAppSettings(event.type))
        }
    }
}

object SystemScreenCoordinator {
    fun plan(state: SystemState, actions: List<SystemAction>): List<SystemCoordinatorRequest> {
        val requests = mutableListOf<SystemCoordinatorRequest>()
        actions.forEach { action ->
            when (action) {
                is SystemAction.SetAutoMode -> {
                    requests += SystemCoordinatorRequest.SetAutoMode(action.mode)
                    requests += SystemCoordinatorRequest.LogEvent("auto_mode_changed", mapOf("mode" to action.mode.name))
                }
                is SystemAction.RequestSafPermission -> {
                    requests += SystemCoordinatorRequest.RequestSafPermission(action.initialUri)
                }
                is SystemAction.AddSafTreeUri -> {
                    requests += SystemCoordinatorRequest.PersistSafTreeUris(state.safConfig.treeUris)
                }
                is SystemAction.RemoveSafTreeUri -> {
                    requests += SystemCoordinatorRequest.PersistSafTreeUris(state.safConfig.treeUris)
                }
                is SystemAction.RequestPermission -> {
                    requests += SystemCoordinatorRequest.RequestPermission(action.type)
                    requests += SystemCoordinatorRequest.LogEvent("permission_requested", mapOf("type" to action.type.name))
                }
                is SystemAction.OpenAppSettings -> {
                    requests += SystemCoordinatorRequest.OpenAppSettings(action.type)
                }
                is SystemAction.SetFormFactor -> {
                    requests += SystemCoordinatorRequest.LogEvent("form_factor_changed", mapOf(
                        "factor" to action.config.currentFormFactor.name,
                        "two_pane" to action.config.isTwoPaneLayout,
                    ))
                }
            }
        }
        return requests
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TEST + PRODUCTION GATE
// ═══════════════════════════════════════════════════════════════════════════

data class SystemTestCase(val id: String, val category: SystemTestCategory, val description: String)

enum class SystemTestCategory(val displayName: String) {
    ANDROID_AUTO("Android Auto Test"),
    SAF("Storage Access Framework Test"),
    FOLDABLE("Foldable / Tablet Test"),
    PERMISSION("Permission Denial Path Test"),
    RTL("RTL Layout Test"),
}

object SystemTestRegistry {
    val allTestCases: List<SystemTestCase> = listOf(
        SystemTestCase("SYS-AA-001", SystemTestCategory.ANDROID_AUTO, "Auto Media Browser Service connects"),
        SystemTestCase("SYS-AA-002", SystemTestCategory.ANDROID_AUTO, "Car mode shows simplified UI"),
        SystemTestCase("SYS-AA-003", SystemTestCategory.ANDROID_AUTO, "Touch targets ≥96dp in driving mode"),
        SystemTestCase("SYS-AA-004", SystemTestCategory.ANDROID_AUTO, "Voice actions work in car"),
        SystemTestCase("SYS-AA-005", SystemTestCategory.ANDROID_AUTO, "Auto disconnect → return to normal UI"),

        SystemTestCase("SYS-SAF-001", SystemTestCategory.SAF, "SAF file picker opens"),
        SystemTestCase("SYS-SAF-002", SystemTestCategory.SAF, "Tree URI persisted across restarts"),
        SystemTestCase("SYS-SAF-003", SystemTestCategory.SAF, "Multiple tree URIs supported"),
        SystemTestCase("SYS-SAF-004", SystemTestCategory.SAF, "No legacy storage permission used"),
        SystemTestCase("SYS-SAF-005", SystemTestCategory.SAF, "Music files accessed via SAF only"),

        SystemTestCase("SYS-FLD-001", SystemTestCategory.FOLDABLE, "Foldable unfolded → two-pane Now Playing"),
        SystemTestCase("SYS-FLD-002", SystemTestCategory.FOLDABLE, "Foldable folded → single-pane"),
        SystemTestCase("SYS-FLD-003", SystemTestCategory.FOLDABLE, "Tablet landscape → two-pane"),
        SystemTestCase("SYS-FLD-004", SystemTestCategory.FOLDABLE, "Tablet portrait → adaptive layout"),
        SystemTestCase("SYS-FLD-005", SystemTestCategory.FOLDABLE, "Layout transition smooth (no jank)"),

        SystemTestCase("SYS-PER-001", SystemTestCategory.PERMISSION, "Mic denied → voice search graceful"),
        SystemTestCase("SYS-PER-002", SystemTestCategory.PERMISSION, "Notification denied → silent playback"),
        SystemTestCase("SYS-PER-003", SystemTestCategory.PERMISSION, "Storage denied → empty state with explanation"),
        SystemTestCase("SYS-PER-004", SystemTestCategory.PERMISSION, "Bluetooth denied → wired fallback"),
        SystemTestCase("SYS-PER-005", SystemTestCategory.PERMISSION, "Permission rationale shown before re-request"),
        SystemTestCase("SYS-PER-006", SystemTestCategory.PERMISSION, "Open App Settings deep link works"),

        SystemTestCase("SYS-RTL-001", SystemTestCategory.RTL, "Arabic layout renders correctly"),
        SystemTestCase("SYS-RTL-002", SystemTestCategory.RTL, "Hebrew layout renders correctly"),
        SystemTestCase("SYS-RTL-003", SystemTestCategory.RTL, "Bangla layout renders correctly"),
        SystemTestCase("SYS-RTL-004", SystemTestCategory.RTL, "Layout direction follows locale"),
    )
}

object SystemProductionGate {

    data class PerformanceBudget(
        val safPermissionRequestMs: Long,
        val autoModeSwitchMs: Long,
        val formFactorTransitionMs: Long,
        val twoPaneLayoutRenderMs: Long,
    )

    val performanceBudget = PerformanceBudget(
        safPermissionRequestMs = 500,
        autoModeSwitchMs = 200,
        formFactorTransitionMs = 400,
        twoPaneLayoutRenderMs = 100,
    )

    val securityRequirements = listOf(
        "No deprecated storage permissions (READ_EXTERNAL_STORAGE)",
        "SAF used for file access — Play Store compliant",
        "Android Auto Media Browser Service isolated",
        "Permission denial does not crash app",
        "Bluetooth only requested when needed",
        "No background location access",
    )

    val accessibilityRequirements = listOf(
        "Android Auto touch targets ≥96dp",
        "Car mode UI simplified (large text, minimal options)",
        "Permission denial messages are descriptive",
        "Settings deep link has contentDescription",
    )

    val observabilityEvents = listOf(
        "auto_mode_changed", "auto_connected", "auto_disconnected",
        "saf_permission_granted", "saf_permission_denied",
        "saf_tree_uri_added", "saf_tree_uri_removed",
        "permission_granted", "permission_denied", "permission_denied_with_rationale",
        "form_factor_changed", "app_settings_opened",
    )

    val resilienceScenarios = listOf(
        "Android Auto connect/disconnect smooth",
        "SAF permission denied → graceful empty state",
        "Mic permission denied → voice search disabled",
        "Notification permission denied → silent playback continues",
        "Storage permission denied → user can re-grant via settings",
        "Foldable unfold/fold → layout adapts smoothly",
        "Tablet orientation change → no crash",
        "Permission denial paths all graceful (no crash)",
    )

    val i18nStrings = listOf(
        "system_auto_mode_disabled", "system_auto_mode_connected", "system_auto_mode_driving",
        "system_saf_request_title", "system_saf_request_message",
        "system_saf_granted", "system_saf_denied", "system_saf_partial",
        "system_perm_notification", "system_perm_microphone", "system_perm_storage",
        "system_perm_fg_service", "system_perm_fg_service_media", "system_perm_wake_lock", "system_perm_bt_connect",
        "system_perm_granted", "system_perm_denied", "system_perm_denied_rationale",
        "system_perm_open_settings", "system_perm_rationale_message",
        "system_form_factor_phone_portrait", "system_form_factor_phone_landscape",
        "system_form_factor_tablet_portrait", "system_form_factor_tablet_landscape",
        "system_form_factor_foldable_unfolded", "system_form_factor_foldable_folded",
    )

    val modularityRules = listOf(
        "System contract package has ZERO Android imports",
        "System runtime package has ZERO Android imports",
        "System host package is the ONLY place with Android imports",
        "Android Auto Media Browser Service isolated",
        "SAF permission flow isolated in host layer",
    )

    fun summary(): String = buildString {
        appendLine("═══ M10 SYSTEM INTEGRATION — PRODUCTION GATE SUMMARY ═══")
        appendLine()
        appendLine("🧪 Tests: ${SystemTestRegistry.allTestCases.size}")
        appendLine("📊 Perf: SAF request <${performanceBudget.safPermissionRequestMs}ms, form factor <${performanceBudget.formFactorTransitionMs}ms")
        appendLine("🔒 Security: ${securityRequirements.size} requirements (Play Store compliant)")
        appendLine("♿ Accessibility: ${accessibilityRequirements.size} requirements (Android Auto driving safety)")
        appendLine("📡 Observability: ${observabilityEvents.size} events")
        appendLine("🛡 Resilience: ${resilienceScenarios.size} scenarios")
        appendLine("🌍 i18n: ${i18nStrings.size} strings (incl. RTL)")
        appendLine("🧩 Modularity: ${modularityRules.size} rules")
        appendLine()
        appendLine("Definition of Done: Scoped storage works, no deprecated permissions, foldable adaptive.")
    }
}
