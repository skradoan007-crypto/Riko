package com.rikodev.music.android.presentation.debug.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.experience.calibration.ExperienceProfile

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — PROFILE SELECTOR
//
// Horizontal chip selector for the 5 experience profiles.
// Click → WaveCalibration.applyPreset(...)
// Nothing else.
//
// DESIGN:
//   - Horizontal scrollable row
//   - Active profile = accent colored, others = dark
//   - Monospace font (developer tool aesthetic)
//
// PRODUCER: Phase 3.4J.2
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileSelector(
    activeProfile: ExperienceProfile,
    onProfileSelected: (ExperienceProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExperienceProfile.entries.forEach { profile ->
            ProfileChip(
                name = profile.displayName,
                isActive = profile == activeProfile,
                onClick = { onProfileSelected(profile) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProfileChip(
    name: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) Color(0xFF19B7FF) else Color(0xFF1A1A2E)
    val textColor = if (isActive) Color.White else Color(0xFF666666)
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = textColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = if (isActive) 0.5.sp else 0.sp
        )
    }
}

/** Human-readable display name for each profile. */
private val ExperienceProfile.displayName: String
    get() = when (this) {
        ExperienceProfile.CLASSIC -> "Classic"
        ExperienceProfile.FLUID -> "Fluid"
        ExperienceProfile.ELASTIC -> "Elastic"
        ExperienceProfile.LIQUID_GLASS -> "Liquid Glass"
        ExperienceProfile.EXPERIMENTAL -> "Experimental"
    }
