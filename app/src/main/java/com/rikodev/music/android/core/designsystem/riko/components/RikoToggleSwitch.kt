package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPrimaryGradient
import com.rikodev.music.android.core.designsystem.riko.theme.RikoToggleThumb
import com.rikodev.music.android.core.designsystem.riko.theme.RikoToggleTrackOff
import com.rikodev.music.android.core.designsystem.riko.theme.RikoToggleTrackOn

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — TOGGLE SWITCH (SM-UI-SPEC-001 v2.0, Section 2.16)
//
// Track 44x24 radius-full. OFF: white 14%. ON: Aurora gradient.
// Thumb 20dp white circle, 2dp inset, slides with spring-snappy
// (duration-medium feel, ~300ms); track color cross-fades simultaneously
// over duration-base. Track 1:1 with spec Section 5.6.
// ─────────────────────────────────────────────────────────────────────────────

/** Track 44x24, thumb 20dp, 2dp inset (spec 1.7 / 2.16). */
private val SwitchTrackWidth = 44.dp
private val SwitchTrackHeight = 24.dp
private val SwitchThumbSize = 20.dp
private val SwitchInset = 2.dp
private val SwitchThumbTravel = SwitchTrackWidth - SwitchThumbSize - SwitchInset * 2

@Composable
fun RikoToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = ""
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) RikoToggleTrackOn else RikoToggleTrackOff,
        animationSpec = RikoMotion.normal(),
        label = "rikoToggleTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) SwitchThumbTravel else 0.dp,
        animationSpec = RikoMotion.snappy(),
        label = "rikoToggleThumb"
    )

    Box(
        modifier = modifier
            .size(SwitchTrackWidth, SwitchTrackHeight)
            .clip(CircleShape)
            .background(
                if (checked) Brush.horizontalGradient(RikoPrimaryGradient)
                else Brush.horizontalGradient(listOf(trackColor, trackColor))
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) { if (enabled) onCheckedChange(!checked) }
            .semantics {
                role = Role.Switch
                if (label.isNotEmpty()) contentDescription = label
                stateDescription = if (checked) "On" else "Off"
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = SwitchInset)
                .offset(x = thumbOffset)
                .size(SwitchThumbSize)
                .clip(CircleShape)
                .background(RikoToggleThumb)
        )
    }
}
