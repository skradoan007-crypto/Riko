package com.rikodev.music.android.presentation.continuum

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.designsystem.riko.theme.AccentMagenta
import com.rikodev.music.android.core.designsystem.riko.theme.AccentViolet
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO CONTINUUM + LIQUID NEXUS — VISUAL EXPERIENCE SETTINGS SECTION (P7)
//
// Lets the user switch between Ultra (default), Balanced, and Lite rendering
// tiers, persisted in DataStore. Never silently downgrades the saved tier on
// older Android versions — surfaces device compatibility transparently.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VisualExperienceSettingsSection(
    selectedTier: VisualQualityTier,
    effectiveProfile: EffectiveRenderProfile,
    onSelectTier: (VisualQualityTier) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RikoSpacing.SM)
    ) {
        Text(
            text = "VISUAL EXPERIENCE",
            style = RikoTypography.Overline,
            color = RikoTextSecondary,
            modifier = Modifier.padding(start = RikoSpacing.XS, top = RikoSpacing.SM)
        )

        VisualQualityTier.entries.forEach { tier ->
            val isSelected = tier == selectedTier
            val icon = when (tier) {
                VisualQualityTier.ULTRA -> Icons.Rounded.AutoAwesome
                VisualQualityTier.BALANCED -> Icons.Rounded.Tune
                VisualQualityTier.LITE -> Icons.Rounded.Bolt
            }
            val shape = RoundedCornerShape(18.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(
                        if (isSelected) {
                            Brush.horizontalGradient(
                                listOf(
                                    AccentViolet.copy(alpha = 0.24f),
                                    AccentMagenta.copy(alpha = 0.16f)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(listOf(RikoGlass, RikoGlass))
                        }
                    )
                    .border(
                        width = if (isSelected) 1.4.dp else 1.dp,
                        color = if (isSelected) AccentViolet.copy(alpha = 0.78f) else RikoGlassBorder,
                        shape = shape
                    )
                    .clickable(role = Role.RadioButton) { onSelectTier(tier) }
                    .semantics {
                        selected = isSelected
                        contentDescription = "${tier.displayName} visual quality: ${tier.subtitle}"
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) AccentViolet.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.08f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else RikoTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tier.displayName,
                            style = RikoTypography.Body.copy(fontWeight = FontWeight.SemiBold),
                            color = RikoTextPrimary
                        )
                        if (tier == VisualQualityTier.DEFAULT) {
                            Text(
                                text = "Default",
                                style = RikoTypography.Caption.copy(fontSize = 10.sp),
                                color = AccentViolet,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(AccentViolet.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tier.subtitle,
                        style = RikoTypography.Caption,
                        color = RikoTextSecondary
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = AccentViolet,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Transparent capability notice when running Ultra on older Android APIs
        if (selectedTier == VisualQualityTier.ULTRA && (!effectiveProfile.useAgslShaders || !effectiveProfile.useRenderEffectBlur)) {
            Text(
                text = "Ultra preference preserved — compatible custom-path rendering and fallback blur active for this Android version.",
                style = RikoTypography.Caption,
                color = RikoTextSecondary.copy(alpha = 0.85f),
                modifier = Modifier.padding(horizontal = RikoSpacing.XS, vertical = 2.dp)
            )
        }
    }
}
