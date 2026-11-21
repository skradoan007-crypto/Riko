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
import androidx.compose.material3.MaterialTheme
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
import com.rikodev.music.android.core.designsystem.riko.theme.RikoIconSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.core.designsystem.theme.LocalIsLightTheme

private val SettingsRowHeight = 72.dp
private val SettingsIconBadgeSize = 40.dp

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
    val isLight = LocalIsLightTheme.current
    val cardBg = if (isLight) Color(0xB8E8FAF4) else Color(0xA60B2220)
    val badgeBg = if (isLight) Color(0xCCFFFFFF) else Color(0xB8061414)
    val borderCol = if (isLight) Color(0x400D9488) else Color(0x3834D399)
    val primaryText = MaterialTheme.colorScheme.onSurface
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val tint = if (destructive) RikoErrorColor else primaryText
    val iconTint = if (destructive) RikoErrorColor else MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SettingsRowHeight)
            .clip(RoundedCornerShape(RikoCornerRadius.MD))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(RikoCornerRadius.MD))
            .then(
                when {
                    onClick != null -> Modifier.clickable(onClick = onClick)
                    trailing is RikoSettingsTrailing.Toggle && onToggleChange != null ->
                        Modifier.clickable { onToggleChange(!trailing.checked) }
                    else -> Modifier
                }
            )
            .padding(horizontal = RikoSpacing.LG),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(SettingsIconBadgeSize)
                .clip(CircleShape)
                .background(badgeBg)
                .border(1.dp, borderCol, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
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
                    color = secondaryText,
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
                    tint = secondaryText,
                    modifier = Modifier.size(RikoIconSize.Compact)
                )
            }
            is RikoSettingsTrailing.Toggle -> {
                RikoToggleSwitch(
                    checked = trailing.checked,
                    onCheckedChange = {
                        if (onToggleChange != null) onToggleChange(it) else onClick?.invoke()
                    },
                    enabled = onToggleChange != null || onClick != null
                )
            }
            is RikoSettingsTrailing.None -> Unit
        }
    }
}

val RikoErrorColor: Color = RikoError
