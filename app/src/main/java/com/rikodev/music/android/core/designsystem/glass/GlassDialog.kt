package com.rikodev.music.android.core.designsystem.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.core.designsystem.theme.GlassTokens

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — GLASS DIALOG
//
// Replaces stock Material AlertDialog (flat solid container, default title/
// button chrome) everywhere a confirmation or input dialog is needed —
// e.g. "New Playlist". Uses the low-level Dialog primitive (window, focus,
// back-press dismiss — legitimate system plumbing) but every pixel of the
// visible panel is our own glass surface, not Material's.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    accentColor: Color = AccentCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(Color.Transparent),
            style = GlassTokens.Dialog,
            cornerRadius = 26.dp,
            tintColor = accentColor.copy(alpha = 0.06f),
            showBorder = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                content = content
            )
        }
    }
}
