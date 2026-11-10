package com.rikodev.music.android.core.designsystem.riko.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoError
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoIconSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — SETTINGS ROW CARD (SM-UI-SPEC-001 v2.0, Section 2.15)
//
// Full-width glass card, height 72dp, radius-md/lg (16-20dp), padding 16dp.
// Layout: leading icon badge (40x40 circle, 1dp glass-border, icon 18-20dp) —
// 12dp gap — text column (Body-Bold title, 2dp gap, Caption subtitle) —
// trailing zone: EITHER chevron-right (20dp, text-secondary) OR ToggleSwitch,
// never both.
//
// D11: destructive rows (e.g. "Exit") tint icon+title in soft red/coral —
// pass [destructive] = true.
// ─────────────────────────────────────────────────────────────────────────────

private val SettingsRowHeight = 72.dp
private val SettingsIconBadgeSize = 40.dp

/** Trailing affordance of a settings row — mutually exclusive (spec 2.15). */
sealed interface RikoSettingsTrailing {
    data object Chevron : RikoSettingsTrailing
    data class Toggle(val checked: Boolean) : RikoSettingsTrailing
    data object None : RikoSettingsTrailing
}

@Composable
fun RikoSettingsRowCard(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: RikoSettingsTrailing = RikoSettingsTrailing.Chevron,
    onToggleChange: ((Boolean) -> Unit)? = null,
    destructive: Boolean = false
) {
    val tint = if (destructive) RikoErrorColor else RikoTextPrimary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SettingsRowHeight)
            .clip(RoundedCornerShape(RikoCornerRadius.MD))
            .background(RikoGlass)
            .border(1.dp, RikoGlassBorder, RoundedCornerShape(RikoCornerRadius.MD))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = RikoSpacing.LG),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading icon badge: 40dp circle, glass fill, 1dp border.
        Box(
            modifier = Modifier
                .size(SettingsIconBadgeSize)
                .clip(CircleShape)
                .background(RikoGlass)
                .border(1.dp, RikoGlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(RikoIconSize.Compact)
            )
        }
        Spacer(Modifier.width(RikoSpacing.MD))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = RikoTypography.Body,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = RikoTypography.Caption,
                    color = RikoTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(RikoSpacing.MD))
        when (trailing) {
            is RikoSettingsTrailing.Chevron -> {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = RikoTextSecondary,
                    modifier = Modifier.size(RikoIconSize.Compact)
                )
            }
            is RikoSettingsTrailing.Toggle -> {
                RikoToggleSwitch(
                    checked = trailing.checked,
                    onCheckedChange = { onToggleChange?.invoke(it) },
                    enabled = onToggleChange != null
                )
            }
            is RikoSettingsTrailing.None -> Unit
        }
    }
}

/** D11 destructive red/coral — exposed so screens reuse the same value. */
val RikoErrorColor: Color = RikoError
