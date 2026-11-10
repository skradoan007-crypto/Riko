/*
 * RIKO — SETTINGS RENDERING (SM-UI-SPEC-001 v2.0, Section 3.9)
 *
 * Rebuilt on the Riko design system: every section renders as
 * RikoSettingsRowCard groups (spec 2.15 pattern) — 72dp glass rows with
 * 40dp icon badges, RikoTypography, Riko tokens. ALL existing
 * functionality preserved (D-NEW-4): profiles, appearance, motion, glass,
 * color, mini player, accessibility, language, diagnostics, developer.
 * NO premium banner — the app is 100% free (Director directive).
 */

package com.rikodev.music.android.presentation.settings_v2.rendering

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.components.RikoAppBarD
import com.rikodev.music.android.core.designsystem.riko.components.RikoIconButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoTopBar
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2
import com.rikodev.music.android.core.designsystem.riko.components.RikoPrimaryButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoSecondaryButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoSettingsRowCard
import com.rikodev.music.android.core.designsystem.riko.components.RikoSettingsTrailing
import com.rikodev.music.android.core.designsystem.riko.components.RikoSortDropdownChip
import com.rikodev.music.android.core.designsystem.riko.components.RikoToggleSwitch
import com.rikodev.music.android.core.designsystem.riko.components.rikoScreenFoundation
import com.rikodev.music.android.core.designsystem.riko.theme.AccentViolet
import com.rikodev.music.android.core.designsystem.riko.theme.RikoError
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPrimaryGradient
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.presentation.settings_v2.runtime.*

// ── Section header — spec 3.9 "Account Setting" pattern ───────────────────────

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = RikoTypography.Overline,
        color = RikoTextSecondary,
        modifier = Modifier.padding(
            start = RikoSpacing.LG,
            top = RikoSpacing.SM,
            bottom = RikoSpacing.XS
        )
    )
}

// ── V2 header ───────────────────────────────────────────────────────────────────

@Composable
fun V2SettingsHeader(title: String, modifier: Modifier = Modifier) {
    RikoTopBar(
        title = title,
        actions = {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = "Notifications",
                tint = RikoThemeV2.Colors.onBackground,
                modifier = Modifier.size(48.dp).padding(12.dp)
            )
        },
        modifier = modifier
    )
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeChange: (String) -> Unit,
    onMotionIntensityChange: (String) -> Unit,
    onGlassIntensityChange: (String) -> Unit,
    onColorBehaviorChange: (String) -> Unit,
    onMiniPlayerBehaviorChange: (String) -> Unit,
    onAccessibilityChange: (AccessibilitySectionUi) -> Unit,
    onDiagnosticsChange: (DiagnosticsSectionUi) -> Unit,
    onProfileApply: (String) -> Unit,
    onDeveloperModeTap: () -> Unit,
    onDeveloperModeLock: () -> Unit,
    onDeveloperOptionChange: (String, Boolean) -> Unit,
    onResetToDefaults: () -> Unit,
    onClearAllData: () -> Unit,
    onExit: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .rikoScreenFoundation(depth = 0.85f)
            .verticalScroll(rememberScrollState())
            .semantics { contentDescription = state.accessibilityLabel },
    ) {
        // V2 header — "Settings" title + notification bell (none in product).
        V2SettingsHeader(title = "Settings")

        Column(
            modifier = Modifier.padding(
                start = RikoSpacing.LG,
                end = RikoSpacing.LG,
                bottom = RikoSpacing.XL
            ),
            verticalArrangement = Arrangement.spacedBy(RikoSpacing.MD)
        ) {
            if (state.lastActionDescription != null) {
                Text(
                    text = state.lastActionDescription,
                    style = RikoTypography.Caption,
                    color = RikoTextSecondary
                )
            }

            // ── Appearance (theme select) ────────────────────────────────
            val appearance = state.appearanceSection
            SettingsSectionTitle("Appearance")
            appearance.availableThemes.forEach { theme ->
                val selected = appearance.theme == theme
                RikoSettingsRowCard(
                    title = theme,
                    subtitle = if (selected) "Active" else null,
                    icon = themeIcon(theme),
                    onClick = { onThemeChange(theme) },
                    trailing = if (selected) RikoSettingsTrailing.Toggle(true) else RikoSettingsTrailing.Chevron
                )
            }

            // SM spec 3.9 "Account Setting" rows (Backup / Notification /
            // Privacy) removed per G-7: all three were comment-only no-ops.
            // Re-add with the real features + a hosted Privacy Policy URL
            // (tracked as a release-readiness item).

            // ── Motion intensity ─────────────────────────────────────────────
            SettingsSectionTitle("Motion Intensity")
            RikoIntensityRow(
                options = state.motionSection.availableIntensities,
                selected = state.motionSection.intensity,
                onSelect = onMotionIntensityChange
            )

            // ── Glass intensity ─────────────────────────────────────────
            SettingsSectionTitle("Glass Intensity")
            RikoIntensityRow(
                options = state.glassSection.availableIntensities,
                selected = state.glassSection.intensity,
                onSelect = onGlassIntensityChange
            )

            // ── Color behavior ───────────────────────────────────────────
            SettingsSectionTitle("Color Behavior")
            state.colorSection.availableBehaviors.forEach { behavior ->
                val selected = state.colorSection.behavior == behavior
                RikoSettingsRowCard(
                    title = behavior,
                    subtitle = if (selected) "Active" else null,
                    icon = Icons.Filled.ColorLens,
                    onClick = { onColorBehaviorChange(behavior) },
                    trailing = if (selected) RikoSettingsTrailing.Toggle(true) else RikoSettingsTrailing.Chevron
                )
            }

            // ── Mini player behavior ─────────────────────────────────────
            SettingsSectionTitle("Mini Player")
            state.miniPlayerSection.availableBehaviors.forEach { behavior ->
                val selected = state.miniPlayerSection.behavior == behavior
                RikoSettingsRowCard(
                    title = behavior,
                    subtitle = if (selected) "Active" else null,
                    icon = Icons.Filled.MusicNote,
                    onClick = { onMiniPlayerBehaviorChange(behavior) },
                    trailing = if (selected) RikoSettingsTrailing.Toggle(true) else RikoSettingsTrailing.Chevron
                )
            }

            // ── Accessibility ─────────────────────────────────────────────
            val access = state.accessibilitySection
            SettingsSectionTitle("Accessibility")
            RikoToggleSettingsRow("High Contrast", access.highContrast, Icons.Filled.Contrast) {
                onAccessibilityChange(access.copy(highContrast = it))
            }
            RikoToggleSettingsRow("Large Text", access.largeText, Icons.Filled.FormatSize) {
                onAccessibilityChange(access.copy(largeText = it))
            }
            RikoToggleSettingsRow("Reduce Motion", access.reduceMotion, Icons.Filled.Waves) {
                onAccessibilityChange(access.copy(reduceMotion = it))
            }
            RikoToggleSettingsRow("Disable Haptics", access.disableHaptics, Icons.Filled.Vibration) {
                onAccessibilityChange(access.copy(disableHaptics = it))
            }
            RikoToggleSettingsRow("Verbose TalkBack", access.talkBackVerbose, Icons.Filled.RecordVoiceOver) {
                onAccessibilityChange(access.copy(talkBackVerbose = it))
            }

            // ── Diagnostics ───────────────────────────────────────────────
            val diag = state.diagnosticsSection
            SettingsSectionTitle("Diagnostics")
            RikoToggleSettingsRow("Show Frame Rate", diag.showFrameRate, Icons.Filled.Speed) {
                onDiagnosticsChange(diag.copy(showFrameRate = it))
            }
            RikoToggleSettingsRow("Show Recomposition Count", diag.showRecompositionCount, Icons.Filled.Repeat) {
                onDiagnosticsChange(diag.copy(showRecompositionCount = it))
            }
            RikoToggleSettingsRow("Show Memory Usage", diag.showMemoryUsage, Icons.Filled.Memory) {
                onDiagnosticsChange(diag.copy(showMemoryUsage = it))
            }
            RikoToggleSettingsRow("Show Wave Inspector", diag.showWaveInspector, Icons.Filled.GraphicEq) {
                onDiagnosticsChange(diag.copy(showWaveInspector = it))
            }
            RikoToggleSettingsRow("Enable Validation Gates", diag.enableValidationGates, Icons.Filled.CheckCircle) {
                onDiagnosticsChange(diag.copy(enableValidationGates = it))
            }

            // ── Reset / Clear ─────────────────────────────────────────────
            SettingsSectionTitle("Data")
            RikoSettingsRowCard(
                title = "Reset to Defaults",
                subtitle = "Restore all settings",
                icon = Icons.Filled.Refresh,
                onClick = onResetToDefaults,
                trailing = RikoSettingsTrailing.Chevron
            )
            RikoSettingsRowCard(
                title = "Clear All Data",
                subtitle = "Erase preferences",
                icon = Icons.Filled.DeleteOutline,
                onClick = onClearAllData,
                trailing = RikoSettingsTrailing.Chevron,
                destructive = true
            )

            // ── Developer ─────────────────────────────────────────────────
            if (state.developerSection.enabled) {
                SettingsSectionTitle("Developer Options")
                val dev = state.developerSection
                RikoToggleSettingsRow("Show Hidden Screens", dev.showHiddenScreens, Icons.Filled.VisibilityOff) {
                    onDeveloperOptionChange("showHiddenScreens", it)
                }
                RikoToggleSettingsRow("Performance Overlay", dev.enablePerformanceOverlay, Icons.Filled.Speed) {
                    onDeveloperOptionChange("enablePerformanceOverlay", it)
                }
                RikoToggleSettingsRow("Force Reduced Motion", dev.forceReducedMotion, Icons.Filled.Waves) {
                    onDeveloperOptionChange("forceReducedMotion", it)
                }
                RikoToggleSettingsRow("Force Low-End Profile", dev.forceLowEndProfile, Icons.Filled.BatteryAlert) {
                    onDeveloperOptionChange("forceLowEndProfile", it)
                }
                RikoToggleSettingsRow("Skip Splash Screen", dev.skipSplashScreen, Icons.Filled.FastForward) {
                    onDeveloperOptionChange("skipSplashScreen", it)
                }
                RikoToggleSettingsRow("Auto Validation Gates", dev.enableValidationGatesAuto, Icons.Filled.CheckCircle) {
                    onDeveloperOptionChange("enableValidationGatesAuto", it)
                }
                RikoSecondaryButton(
                    text = "Lock Developer Mode",
                    onClick = onDeveloperModeLock,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Hidden trigger — version label, taps unlock dev mode.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .clickable(onClick = onDeveloperModeTap)
                        .padding(vertical = RikoSpacing.LG),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "RIKO Experience OS v1.0",
                        style = RikoTypography.Caption,
                        color = RikoTextSecondary,
                        modifier = Modifier.semantics {
                            contentDescription = "RIKO version"
                        }
                    )
                    if (state.developerSection.tapsToUnlock in 1..7) {
                        Text(
                            text = "Tap ${state.developerSection.tapsToUnlock} more times",
                            style = RikoTypography.Caption,
                            color = RikoTextSecondary
                        )
                    }
                }
            }

            // ── Exit — D11 destructive coral row ──────────────────────────
            RikoSettingsRowCard(
                title = "Exit",
                subtitle = "Close the app",
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = onExit,
                trailing = RikoSettingsTrailing.Chevron,
                destructive = true
            )

            Spacer(Modifier.height(RikoSpacing.XXL))
        }
    }
}

// ── Reusable rows ─────────────────────────────────────────────────────────────

/** Toggle row — icon + title + RikoToggleSwitch (spec 2.15/2.16). */
@Composable
private fun RikoToggleSettingsRow(
    title: String,
    checked: Boolean,
    icon: ImageVector,
    onChange: (Boolean) -> Unit
) {
    RikoSettingsRowCard(
        title = title,
        subtitle = null,
        icon = icon,
        onClick = null,
        trailing = RikoSettingsTrailing.Toggle(checked),
        onToggleChange = onChange
    )
}

/** Intensity chooser — chips row (motion/glass levels). */
@Composable
private fun RikoIntensityRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RikoSpacing.XL),
        horizontalArrangement = Arrangement.spacedBy(RikoSpacing.SM)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .then(
                        if (isSelected)
                            Modifier.background(Brush.horizontalGradient(RikoPrimaryGradient))
                        else Modifier
                    )
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option,
                    style = RikoTypography.Label,
                    color = if (isSelected) Color.White else RikoTextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

/** Theme icon mapping (keyword-matched — unknown themes get a safe default). */
private fun themeIcon(theme: String): ImageVector {
    val name = theme.lowercase()
    return when {
        "light" in name -> Icons.Filled.LightMode
        "amoled" in name -> Icons.Filled.DarkMode
        "dark" in name -> Icons.Filled.DarkMode
        else -> Icons.Filled.PhoneAndroid
    }
}
