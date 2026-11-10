package com.rikodev.music.android.presentation.nowplaying.eq

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.designsystem.theme.EqShapes
import com.rikodev.music.android.core.designsystem.theme.EqTheme
import com.rikodev.music.android.core.designsystem.theme.EqType
import com.rikodev.music.android.domain.model.EqPreset

/**
 * Flat Equalizer `spnr_preset` parity: dropdown with factory presets
 * and user presets (grouped), plus optional save entry handled by caller.
 */
@Composable
fun EqPresetDropdown(
    current: String,
    factoryNames: List<String>,
    userNames: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val eq = EqTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(EqShapes.Inner)
            .background(eq.cardAlt)
            .clickable { expanded = true }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { contentDescription = "Preset selector" }
    ) {
        Text("Presets", fontSize = EqType.Body.fontSize, color = eq.textSecondary)
        Spacer(Modifier.width(10.dp))
        Text(
            current,
            fontSize = EqType.BodyLarge.fontSize,
            color = eq.accent,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = eq.accent)

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (factoryNames.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("Factory", fontSize = EqType.Micro.fontSize, color = eq.textSecondary) },
                    onClick = {},
                    enabled = false
                )
                factoryNames.forEach { name ->
                    DropdownMenuItem(
                        text = {
                            Text(name, color = if (name == current) eq.accent else eq.textPrimary)
                        },
                        onClick = {
                            expanded = false
                            onSelected(name)
                        }
                    )
                }
            }
            if (userNames.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("User", fontSize = EqType.Micro.fontSize, color = eq.textSecondary) },
                    onClick = {},
                    enabled = false
                )
                userNames.forEach { name ->
                    DropdownMenuItem(
                        text = {
                            Text(name, color = if (name == current) eq.accent else eq.textPrimary)
                        },
                        onClick = {
                            expanded = false
                            onSelected(name)
                        }
                    )
                }
            }
        }
    }
}

/**
 * User preset list with apply/delete + "Save current" entry.
 * Flat Equalizer profile management parity (max [limit] saved).
 */
@Composable
fun EqUserPresetPanel(
    presets: List<EqPreset>,
    onSaveClick: () -> Unit,
    onApply: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    limit: Int = 10,
    modifier: Modifier = Modifier,
) {
    val eq = EqTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        // Save current as new preset
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(EqShapes.Inner)
                .background(eq.cardAlt)
                .clickable(enabled = presets.size < limit, onClick = onSaveClick)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .semantics { contentDescription = "Save current as new preset" }
        ) {
            Text(
                if (presets.size < limit) "+ Save current as new preset"
                else "Limit reached ($limit/$limit) — delete one first",
                color = if (presets.size < limit) eq.accent else eq.danger,
                fontSize = EqType.Body.fontSize,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(8.dp))

        if (presets.isEmpty()) {
            Text(
                "No saved presets yet",
                color = eq.textSecondary,
                fontSize = EqType.Small.fontSize,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        presets.forEach { preset ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(EqShapes.Inner)
                    .background(eq.cardAlt)
                    .clickable { onApply(preset.id) }
                    .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                    .semantics { contentDescription = "Apply preset ${preset.name}" }
            ) {
                Text(
                    preset.name,
                    color = eq.textPrimary,
                    fontSize = EqType.Body.fontSize,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onDelete(preset.id) }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete ${preset.name}",
                        tint = eq.textSecondary
                    )
                }
            }
        }
    }
}
