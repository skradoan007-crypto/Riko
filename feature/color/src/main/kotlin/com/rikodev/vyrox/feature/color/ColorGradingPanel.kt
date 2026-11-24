package com.rikodev.vyrox.feature.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.vyrox.core.design.LiquidGlassSurface
import com.rikodev.vyrox.core.design.LocalVyRoxGlass
import com.rikodev.vyrox.core.design.VyRoxPalette
import com.rikodev.vyrox.core.model.ColorGradeState
import com.rikodev.vyrox.core.model.ColorPreset
import kotlin.math.roundToInt

enum class ColorParam(
    val label: String,
    val range: ClosedFloatingPointRange<Float>,
    val getter: (ColorGradeState) -> Float,
    val setter: (ColorGradeState, Float) -> ColorGradeState
) {
    EXPOSURE("Exposure", -2f..2f, { it.exposure }, { s, v -> s.copy(exposure = v) }),
    BRIGHTNESS("Brightness", -1f..1f, { it.brightness }, { s, v -> s.copy(brightness = v) }),
    CONTRAST("Contrast", -1f..1f, { it.contrast }, { s, v -> s.copy(contrast = v) }),
    HIGHLIGHTS("Highlights", -1f..1f, { it.highlights }, { s, v -> s.copy(highlights = v) }),
    SHADOWS("Shadows", -1f..1f, { it.shadows }, { s, v -> s.copy(shadows = v) }),
    WARMTH("Warmth", -1f..1f, { it.warmth }, { s, v -> s.copy(warmth = v) }),
    TINT("Tint", -1f..1f, { it.tint }, { s, v -> s.copy(tint = v) }),
    SATURATION("Saturation", -1f..1f, { it.saturation }, { s, v -> s.copy(saturation = v) }),
    VIBRANCE("Vibrance", -1f..1f, { it.vibrance }, { s, v -> s.copy(vibrance = v) }),
    FADE("Fade", 0f..1f, { it.fade }, { s, v -> s.copy(fade = v) }),
    SHARPEN("Sharpen", 0f..1f, { it.sharpen }, { s, v -> s.copy(sharpen = v) }),
    VIGNETTE("Vignette", 0f..1f, { it.vignette }, { s, v -> s.copy(vignette = v) }),
    GRAIN("Grain", 0f..1f, { it.grain }, { s, v -> s.copy(grain = v) })
}

@Composable
fun ColorGradingPanel(
    grade: ColorGradeState,
    presets: List<ColorPreset>,
    onGradeChange: (ColorGradeState) -> Unit,
    onCommitGrade: (ColorGradeState) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalVyRoxGlass.current
    var selectedParam by remember { mutableStateOf(ColorParam.EXPOSURE) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }

    val currentValue = selectedParam.getter(grade)

    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // Presets row + Save/Reset actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Color & Presets",
                    color = glass.inkPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.BookmarkAdd,
                        contentDescription = "Save Preset",
                        tint = VyRoxPalette.Cyan,
                        modifier = Modifier.clickable { showSaveDialog = true }
                    )
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reset Color",
                        tint = glass.inkSecondary,
                        modifier = Modifier.clickable {
                            val neutral = ColorGradeState()
                            onGradeChange(neutral)
                            onCommitGrade(neutral)
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 6.dp)
            ) {
                items(presets, key = { it.id }) { preset ->
                    val active = preset.grade == grade
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (active) VyRoxPalette.Violet.copy(alpha = 0.35f)
                                else Color.White.copy(alpha = if (glass.isDark) 0.08f else 0.55f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (active) VyRoxPalette.Cyan else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onGradeChange(preset.grade)
                                onCommitGrade(preset.grade)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = preset.name,
                            color = glass.inkPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Active parameter label + numeric readout + slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedParam.label,
                    color = glass.inkPrimary,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "${(currentValue * 100f).roundToInt()}",
                    color = VyRoxPalette.Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = currentValue,
                onValueChange = { v -> onGradeChange(selectedParam.setter(grade, v)) },
                onValueChangeFinished = { onCommitGrade(grade) },
                valueRange = selectedParam.range,
                colors = SliderDefaults.colors(
                    thumbColor = VyRoxPalette.Cyan,
                    activeTrackColor = VyRoxPalette.Violet,
                    inactiveTrackColor = glass.inkSecondary.copy(alpha = 0.25f)
                )
            )

            // 13-tool horizontal strip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 6.dp)
            ) {
                items(ColorParam.entries) { param ->
                    val isSelected = param == selectedParam
                    val nonZero = kotlin.math.abs(param.getter(grade)) > 0.005f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isSelected -> VyRoxPalette.Magenta.copy(alpha = 0.28f)
                                    nonZero -> VyRoxPalette.Cyan.copy(alpha = 0.14f)
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) VyRoxPalette.Magenta else glass.inkSecondary.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedParam = param }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = param.label,
                            color = if (isSelected) glass.inkPrimary else glass.inkSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected || nonZero) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Color Preset") },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    label = { Text("Preset name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveCustomPreset(presetNameInput)
                        presetNameInput = ""
                        showSaveDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}
