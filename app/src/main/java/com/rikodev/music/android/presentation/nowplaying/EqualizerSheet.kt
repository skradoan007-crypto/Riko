package com.rikodev.music.android.presentation.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rikodev.music.android.presentation.nowplaying.eq.EqBandPoint
import com.rikodev.music.android.presentation.nowplaying.eq.EqBassCard
import com.rikodev.music.android.presentation.nowplaying.eq.EqBalanceCard
import com.rikodev.music.android.presentation.nowplaying.eq.EqErrorCard
import com.rikodev.music.android.presentation.nowplaying.eq.EqLoudnessCard
import com.rikodev.music.android.presentation.nowplaying.eq.EqMainCard
import com.rikodev.music.android.presentation.nowplaying.eq.EqReverbCard
import com.rikodev.music.android.presentation.nowplaying.eq.EqSectionCard
import com.rikodev.music.android.core.designsystem.theme.EqTheme
import com.rikodev.music.android.core.designsystem.theme.EqType
import com.rikodev.music.android.core.designsystem.theme.EqUiThemeMode
import com.rikodev.music.android.presentation.nowplaying.eq.EqThemeToggle
import com.rikodev.music.android.presentation.nowplaying.eq.EqVirtualizerCard
import com.rikodev.music.android.presentation.nowplaying.eq.EqVolumeCard
import com.rikodev.music.android.presentation.nowplaying.eq.EqUserPresetPanel

/**
 * Now Playing Equalizer sheet — Flat Equalizer card layout.
 *
 * Architecture: UI → [PlayerViewModel] only. Colors from [EqTheme]
 * (dark + light). One section card per effect, preset dropdown,
 * user presets (max 10), volume, and theme toggle.
 */
@Composable
fun EqualizerSheet(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val audioFxState by viewModel.audioFxState.collectAsStateWithLifecycle()
    val themeMode = when (audioFxState.eqUiThemeMode.uppercase()) {
        "LIGHT" -> EqUiThemeMode.LIGHT
        else -> EqUiThemeMode.DARK
    }

    EqTheme(mode = themeMode) {
        EqualizerSheetContent(
            modifier = modifier,
            viewModel = viewModel,
            themeMode = themeMode,
            onThemeModeChange = { viewModel.setEqUiThemeMode(it.name) }
        )
    }
}

@Composable
private fun EqualizerSheetContent(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel,
    themeMode: EqUiThemeMode,
    onThemeModeChange: (EqUiThemeMode) -> Unit
) {
    val eq = EqTheme.colors
    val audioFxState by viewModel.audioFxState.collectAsStateWithLifecycle()
    val fx = audioFxState
    val eqState = fx.equalizerState
    val userPresets by viewModel.userPresets.collectAsStateWithLifecycle()
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(eq.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // ── Header: title + theme toggle + reset ───────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Equalizer", style = EqType.Headline, color = eq.textPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                EqThemeToggle(mode = themeMode, onModeChange = onThemeModeChange)
                IconButton(onClick = { viewModel.resetEq() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reset equalizer", tint = eq.textPrimary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Loading / error states ─────────────────────────────────────
        if (eqState.bands.isEmpty()) {
            EqErrorCard(
                message = "Preparing equalizer… waiting for audio session.",
                onReinit = { viewModel.setEqEnabled(eqState.enabled) }
            )
            Spacer(Modifier.height(12.dp))
        }

        // ── Main EQ card: curve + bands + presets + reset ──────────────
        val bandPoints = eqState.bands.map {
            EqBandPoint(
                frequencyHz = it.frequencyHz,
                levelDb = it.levelDb,
                minDb = it.minLevel / 100f,
                maxDb = it.maxLevel / 100f
            )
        }
        EqMainCard(
            enabled = eqState.enabled,
            onToggle = { viewModel.setEqEnabled(it) },
            currentPreset = eqState.currentPreset,
            bands = bandPoints,
            onBandLevelChange = { index, db ->
                val band = eqState.bands.getOrNull(index) ?: return@EqMainCard
                val millibel = (db * 100).toInt()
                viewModel.setBandLevel(band.index, millibel)
            },
            presetNames = viewModel.eqPresetNames,
            userPresetNames = userPresets.map { it.name },
            onPresetSelected = { name ->
                val user = userPresets.find { it.name == name }
                if (user != null) viewModel.applyUserPreset(user.id)
                else viewModel.applyEqPreset(name)
            },
            onReset = { viewModel.resetEq() }
        )

        Spacer(Modifier.height(12.dp))

        // ── Bass Boost (strength + optional pro frequency/max-gain) ────
        EqBassCard(
            enabled = fx.bassBoostEnabled,
            onToggle = { viewModel.setBassBoostEnabled(it) },
            strength = fx.bassBoostStrength / 1000f,
            onStrengthChange = { viewModel.setBassBoostStrength((it * 1000).toInt()) },
            frequency = fx.bassBoostFrequency / 1000f,
            onFrequencyChange = { viewModel.setBassBoostFrequency((it * 1000).toInt()) },
            maxGain = fx.bassBoostMaxGain / 1000f,
            onMaxGainChange = { viewModel.setBassBoostMaxGain((it * 1000).toInt()) },
            advancedAvailable = fx.bassBoostFreqAvailable || fx.bassBoostMaxGainAvailable,
            available = fx.bassBoostAvailable
        )

        Spacer(Modifier.height(12.dp))

        // ── Virtualizer ────────────────────────────────────────────────
        EqVirtualizerCard(
            enabled = fx.virtualizerEnabled,
            onToggle = { viewModel.setVirtualizerEnabled(it) },
            strength = fx.virtualizerStrength / 1000f,
            onStrengthChange = { viewModel.setVirtualizerStrength((it * 1000).toInt()) },
            available = fx.virtualizerAvailable
        )

        Spacer(Modifier.height(12.dp))

        // ── Loudness ───────────────────────────────────────────────────
        EqLoudnessCard(
            enabled = fx.loudnessEnabled,
            onToggle = { viewModel.setLoudnessEnabled(it) },
            gain = (fx.loudnessGainDb / 20f).coerceIn(0f, 1f),
            onGainChange = { viewModel.setLoudnessGain((it * 20).toInt()) },
            maxGain = (fx.loudnessMaxGainDb / 20f).coerceIn(0f, 1f),
            onMaxGainChange = { viewModel.setLoudnessMaxGain((it * 20).toInt()) },
            maxGainAvailable = true
        )

        Spacer(Modifier.height(12.dp))

        // ── Reverb ─────────────────────────────────────────────────────
        EqReverbCard(
            enabled = fx.reverbEnabled,
            onToggle = { viewModel.setReverbEnabled(it) },
            presetIndex = fx.reverbPreset.toInt(),
            presetNames = viewModel.reverbPresetNames,
            onPresetChange = { viewModel.setReverbPreset(it) },
            available = fx.reverbAvailable
        )

        Spacer(Modifier.height(12.dp))

        // ── Channel Balance ────────────────────────────────────────────
        EqBalanceCard(
            enabled = fx.balanceEnabled,
            onToggle = { viewModel.setBalanceEnabled(it) },
            balance = fx.balanceLevel / 100f,
            onBalanceChange = { viewModel.setBalanceLevel((it * 100).toInt()) },
            available = fx.balanceAvailable
        )

        Spacer(Modifier.height(12.dp))

        // ── Volume ─────────────────────────────────────────────────────
        EqVolumeCard(
            volume = fx.volumeLevel / 100f,
            onVolumeChange = { viewModel.setVolumeLevel((it * 100).toInt()) }
        )

        Spacer(Modifier.height(12.dp))

        // ── User presets (max 10, custom names) ────────────────────────
        EqSectionCard(
            title = "My Presets",
            icon = Icons.Rounded.Favorite,
            enabled = true,
            onToggle = null,
            valueText = "${userPresets.size}/10",
            modifier = Modifier.fillMaxWidth()
        ) {
            EqUserPresetPanel(
                presets = userPresets,
                onSaveClick = { showSaveDialog = true },
                onApply = { viewModel.applyUserPreset(it) },
                onDelete = { viewModel.deleteUserPreset(it) },
                limit = 10
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showSaveDialog) {
        SavePresetDialog(
            defaultName = "My preset ${userPresets.size + 1}",
            onSave = { name ->
                viewModel.saveUserPreset(name.ifBlank { "My preset ${userPresets.size + 1}" })
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }
}

@Composable
private fun SavePresetDialog(
    defaultName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val eq = EqTheme.colors
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = eq.card,
        title = { Text("Save Preset", color = eq.textPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Preset name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) { Text("Save", color = eq.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = eq.textSecondary) }
        }
    )
}
