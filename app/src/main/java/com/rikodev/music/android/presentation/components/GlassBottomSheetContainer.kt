package com.rikodev.music.android.presentation.components

import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.glass.GlassSurface
import com.rikodev.music.android.core.designsystem.theme.AuroraBase
import com.rikodev.music.android.core.designsystem.theme.AuroraTop
import com.rikodev.music.android.core.designsystem.theme.GlassTokens
import com.rikodev.music.android.core.designsystem.theme.RikoShapes

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — GLASS BOTTOM SHEET CONTAINER
//
// Every ModalBottomSheet in the app (Equalizer, Queue, Song Info, Sleep
// Timer) renders through this panel. The panel is now OPAQUE at its base:
// a solid aurora substrate sits under the frosted layers, so the Now Playing
// screen behind the sheet can never bleed through (transparency made the
// EQ/Info sheets unreadable). The glass frost, border and specular highlight
// still render on top so the sheet keeps the app's glass identity.
//
// Used with ModalBottomSheet(containerColor = Color.Transparent, dragHandle
// = null, shape = <matching top-rounded shape>) so this panel IS the visible
// surface, not a second layer on top of a solid one.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassBottomSheetContainer(
    modifier: Modifier = Modifier,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        style = GlassTokens.Dialog,
        shape = RikoShapes.TopSheet,
        tintColor = accentColor.copy(alpha = 0.05f),
        showBorder = true
    ) {
        // ── OPAQUE SUBSTRATE ────────────────────────────────────────────
        // Solid aurora base under the frost. Without this the sheet is
        // translucent and the Now Playing content behind it bleeds through,
        // making the Equalizer / Song Info sheets unreadable.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(AuroraTop, AuroraBase)
                    )
                )
        ) {
            // Hand-drawn drag handle — lives inside the glass panel itself
            // instead of floating disconnected above it. Light-on-dark so it
            // reads against the aurora substrate (reference handle style).
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .align(Alignment.CenterHorizontally)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RikoShape.Pill)
                    .background(Color.White.copy(alpha = 0.22f))
            )
            content()
        }
    }
}
