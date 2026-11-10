package com.rikodev.music.android.presentation.nowplaying.eq

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.designsystem.theme.EqShapes
import com.rikodev.music.android.core.designsystem.theme.EqTheme
import com.rikodev.music.android.core.designsystem.theme.EqType

/**
 * Flat Equalizer-style section cards: header (icon + title + JSwitch-style
 * toggle) + body slot. Colors always from [LocalEqColors].
 */

@Composable
fun EqSectionCard(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    onToggle: ((Boolean) -> Unit)? = null,
    available: Boolean = true,
    valueText: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val eq = EqTheme.colors
    val alpha = if (available) 1f else 0.45f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(EqShapes.Card)
            .background(eq.card)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (available && enabled) eq.accent else eq.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = EqType.Title.fontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = if (available) eq.textPrimary else eq.textSecondary
                )
                if (valueText != null) {
                    Text(valueText, fontSize = EqType.Caption.fontSize, color = eq.accent)
                }
            }
            if (onToggle != null) {
                Switch(
                    checked = enabled,
                    onCheckedChange = { if (available) onToggle(it) },
                    enabled = available,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = eq.switchThumb,
                        checkedTrackColor = eq.accent,
                        uncheckedThumbColor = eq.textSecondary,
                        uncheckedTrackColor = eq.track
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "$title enabled"
                    }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            content()
        }
    }
}

/** Main EQ card: curve + bands + preset dropdown + reset. */
@Composable
fun EqMainCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    currentPreset: String,
    bands: List<EqBandPoint>,
    onBandLevelChange: (Int, Float) -> Unit,
    presetNames: List<String>,
    userPresetNames: List<String>,
    onPresetSelected: (String) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val eq = EqTheme.colors
    EqSectionCard(
        title = "Equalizer",
        icon = Icons.Rounded.GraphicEq,
        enabled = enabled,
        onToggle = onToggle,
        valueText = currentPreset,
        modifier = modifier,
    ) {
        Column {
            EqCurve(
                bands = bands,
                onBandLevelChange = if (enabled) onBandLevelChange else null
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bands.forEachIndexed { index, band ->
                    EqBandRow(
                        label = formatFreq(band.frequencyHz),
                        levelDb = band.levelDb,
                        minDb = band.minDb,
                        maxDb = band.maxDb,
                        onLevelChangeDb = { db -> if (enabled) onBandLevelChange(index, db) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            EqPresetDropdown(
                current = currentPreset,
                factoryNames = presetNames,
                userNames = userPresetNames,
                onSelected = onPresetSelected
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "Reset",
                    color = eq.accent,
                    fontSize = EqType.Body.fontSize,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(EqShapes.Inner)
                        .clickable(onClick = onReset)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/** Bass Boost card with strength arc + optional advanced frequency/max-gain. */
@Composable
fun EqBassCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    strength: Float,
    onStrengthChange: (Float) -> Unit,
    frequency: Float,
    onFrequencyChange: (Float) -> Unit,
    maxGain: Float,
    onMaxGainChange: (Float) -> Unit,
    advancedAvailable: Boolean,
    available: Boolean = true,
    modifier: Modifier = Modifier,
) {
    EqSectionCard(
        title = "Bass Boost",
        icon = Icons.Rounded.MusicNote,
        enabled = enabled,
        onToggle = onToggle,
        available = available,
        valueText = "${(strength * 100).toInt()}%",
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ArcSeekBar(
                value = strength,
                onValueChange = { if (enabled && available) onStrengthChange(it) },
                contentDescriptionLabel = "Bass boost strength"
            )
            if (advancedAvailable) {
                Spacer(Modifier.height(8.dp))
                Text("Frequency", fontSize = EqType.Caption.fontSize, color = EqTheme.colors.textSecondary)
                EqSimpleSlider(
                    value = frequency,
                    onValueChange = { if (enabled && available) onFrequencyChange(it) },
                    label = "Bass boost frequency"
                )
                Spacer(Modifier.height(6.dp))
                Text("Max Gain", fontSize = EqType.Caption.fontSize, color = EqTheme.colors.textSecondary)
                EqSimpleSlider(
                    value = maxGain,
                    onValueChange = { if (enabled && available) onMaxGainChange(it) },
                    label = "Bass boost max gain"
                )
            }
        }
    }
}

@Composable
fun EqVirtualizerCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    strength: Float,
    onStrengthChange: (Float) -> Unit,
    available: Boolean = true,
    modifier: Modifier = Modifier,
) {
    EqSectionCard(
        title = "Virtualizer",
        icon = Icons.Rounded.SurroundSound,
        enabled = enabled,
        onToggle = onToggle,
        available = available,
        valueText = "${(strength * 100).toInt()}%",
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ArcSeekBar(
                value = strength,
                onValueChange = { if (enabled && available) onStrengthChange(it) },
                contentDescriptionLabel = "Virtualizer strength"
            )
        }
    }
}

@Composable
fun EqLoudnessCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    gain: Float,
    onGainChange: (Float) -> Unit,
    maxGain: Float,
    onMaxGainChange: (Float) -> Unit,
    maxGainAvailable: Boolean,
    available: Boolean = true,
    modifier: Modifier = Modifier,
) {
    EqSectionCard(
        title = "Loudness",
        icon = Icons.Rounded.Speaker,
        enabled = enabled,
        onToggle = onToggle,
        available = available,
        valueText = "${(gain * 100).toInt()}%",
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ArcSeekBar(
                value = gain,
                onValueChange = { if (enabled && available) onGainChange(it) },
                contentDescriptionLabel = "Loudness gain"
            )
            if (maxGainAvailable) {
                Spacer(Modifier.height(8.dp))
                Text("Max Gain", fontSize = EqType.Caption.fontSize, color = EqTheme.colors.textSecondary)
                EqSimpleSlider(
                    value = maxGain,
                    onValueChange = { if (enabled && available) onMaxGainChange(it) },
                    label = "Loudness max gain"
                )
            }
        }
    }
}

@Composable
fun EqReverbCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    presetIndex: Int,
    presetNames: List<String>,
    onPresetChange: (Int) -> Unit,
    available: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val eq = EqTheme.colors
    EqSectionCard(
        title = "Reverb",
        icon = Icons.Rounded.GraphicEq,
        enabled = enabled,
        onToggle = onToggle,
        available = available,
        valueText = presetNames.getOrNull(presetIndex) ?: "",
        modifier = modifier,
    ) {
        Column {
            presetNames.forEachIndexed { index, name ->
                val selected = index == presetIndex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(EqShapes.Inner)
                        .background(if (selected) eq.accent.copy(alpha = 0.18f) else eq.cardAlt)
                        .clickable(enabled = enabled && available) { onPresetChange(index) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        name,
                        color = if (selected) eq.accent else eq.textPrimary,
                        fontSize = EqType.Body.fontSize,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun EqBalanceCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    balance: Float,
    onBalanceChange: (Float) -> Unit,
    available: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val leftPct = (((1f - balance) / 2f) * 100).toInt().coerceIn(0, 100)
    val rightPct = 100 - leftPct
    EqSectionCard(
        title = "Audio Balance",
        icon = Icons.Rounded.Speaker,
        enabled = enabled,
        onToggle = onToggle,
        available = available,
        valueText = "L $leftPct% / R $rightPct%",
        modifier = modifier,
    ) {
        MidSeekBar(
            value = balance,
            onValueChange = { if (enabled && available) onBalanceChange(it) },
            valueRange = -1f..1f,
            contentDescriptionLabel = "Left right balance"
        )
    }
}

@Composable
fun EqVolumeCard(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    EqSectionCard(
        title = "Volume",
        icon = Icons.Rounded.VolumeUp,
        enabled = true,
        onToggle = null,
        valueText = "${(volume * 100).toInt()}%",
        modifier = modifier,
    ) {
        EqSimpleSlider(
            value = volume,
            onValueChange = onVolumeChange,
            label = "Stream volume"
        )
    }
}

@Composable
fun EqErrorCard(
    message: String,
    onReinit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val eq = EqTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(EqShapes.Card)
            .background(eq.card)
            .padding(16.dp)
    ) {
        Text(message, color = eq.danger, fontSize = EqType.Body.fontSize)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .clip(EqShapes.Inner)
                .background(eq.accent)
                .clickable(onClick = onReinit)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Reinitialize", color = eq.switchThumb, fontWeight = FontWeight.Medium)
        }
    }
}

// ── shared slider for volume / advanced params ──────────────────────

@Composable
fun EqSimpleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val eq = EqTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics { contentDescription = label }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        ) {
            val trackH = 6.dp.toPx()
            val cy = size.height / 2f
            drawRoundRect(
                color = eq.track,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(size.width, trackH),
                cornerRadius = CornerRadius(trackH / 2f)
            )
            drawRoundRect(
                color = eq.accent,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(size.width * value.coerceIn(0f, 1f), trackH),
                cornerRadius = CornerRadius(trackH / 2f)
            )
            drawCircle(
                color = eq.switchThumb,
                radius = 10.dp.toPx(),
                center = Offset(size.width * value.coerceIn(0f, 1f), cy)
            )
            drawCircle(
                color = eq.accent,
                radius = 6.dp.toPx(),
                center = Offset(size.width * value.coerceIn(0f, 1f), cy)
            )
        }
    }
}

fun formatFreq(hz: Int): String =
    if (hz >= 1000) "${hz / 1000}K" else "$hz"
