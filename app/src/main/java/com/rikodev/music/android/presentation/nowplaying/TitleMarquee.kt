package com.rikodev.music.android.presentation.nowplaying

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — TITLE MARQUEE (master-prompt §3.2)
//
// Centered [source-app glyph 14dp][6dp][title 14sp SemiBold white].
// Overflow → marquee; the loop repeats glyph + title with ~24dp spacing
// and is hard-clipped at the 30dp content inset.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TitleMarquee(
    title: String,
    sourceIcon: ImageVector?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 30.dp)
            .clipToBounds(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.basicMarquee(
                iterations = Int.MAX_VALUE,
                initialDelayMillis = 2_000
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (sourceIcon != null) {
                Icon(
                    imageVector = sourceIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = PlasmaTokens.Title,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Visible
            )
            // Loop spacing so the repeat reads as glyph + title + gap.
            Spacer(modifier = Modifier.width(24.dp))
        }
    }
}
