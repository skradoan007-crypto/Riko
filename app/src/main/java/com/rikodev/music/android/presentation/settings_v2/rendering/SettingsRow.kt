/*
 * RIKO EXPERIENCE OS — Settings Tier
 * SettingsRow.kt
 *
 * The repeating row pattern from the reference design: colored icon
 * badge + bold title + gray subtitle + trailing control (button/
 * radio/toggle/chevron). One component, every Settings row uses it.
 *
 * © RIKO Experience OS — Settings Tier
 */

package com.rikodev.music.android.presentation.settings_v2.rendering

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.designsystem.components.IconBadge
import com.rikodev.music.android.core.designsystem.theme.TextPrimary
import com.rikodev.music.android.core.designsystem.theme.TextSecondary

@Composable
fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconBadge(icon = icon, tint = iconTint, contentDescription = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
        trailing()
    }
}
