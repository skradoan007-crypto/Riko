package com.rikodev.music.android.core.designsystem.riko.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.components.RikoArtwork
import com.rikodev.music.android.core.designsystem.riko.components.RikoArtworkSize
import com.rikodev.music.android.core.designsystem.riko.components.RikoBottomNavigation
import com.rikodev.music.android.core.designsystem.riko.components.RikoFilterTabRow
import com.rikodev.music.android.core.designsystem.riko.components.RikoGlassSurface
import com.rikodev.music.android.core.designsystem.riko.components.RikoIconButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoNavItem
import com.rikodev.music.android.core.designsystem.riko.components.RikoOnboardingLegalText
import com.rikodev.music.android.core.designsystem.riko.components.RikoPrimaryButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoSecondaryButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoSectionHeader
import com.rikodev.music.android.core.designsystem.riko.components.RikoSegmentedControl
import com.rikodev.music.android.core.designsystem.riko.components.RikoTrackRow
import com.rikodev.music.android.core.designsystem.riko.components.RikoSortDropdownChip
import com.rikodev.music.android.core.designsystem.riko.components.RikoToggleSwitch
import com.rikodev.music.android.core.designsystem.riko.theme.RikoBackgroundElevated
import com.rikodev.music.android.core.designsystem.riko.theme.RikoComponentSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassStrong
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPurple
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPurpleBright
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPurpleDeep
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSurface
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSurfaceElevated
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSurfaceStrong
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextDisabled
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DESIGN SYSTEM — PREVIEW LAB 🧪 (PLAN 1)
//
// UI laboratory to visually validate the new design system BEFORE any
// production migration. Every token and component in one place.
//
// QA CHECKLIST (validate each visually):
//   A. Purple overdose? (accent must stay ~2% of the screen)
//   B. Glass too visible? (must feel subtle, not flashy)
//   C. Text hierarchy readable in 5 seconds? (primary/secondary/tertiary)
//   D. Surface separation clear? (3 levels distinguishable)
//   E. Everything token-driven and consistent?
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RikoDesignSystemPreviewScreen() {
    var selectedSegment by remember { mutableIntStateOf(0) }
    var selectedNav by remember { mutableIntStateOf(0) }
    var darkMode by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RikoSpacing.LG)
    ) {
        Spacer(Modifier.height(RikoSpacing.XXXL))

        // ── 1. Header ────────────────────────────────────────────────────
        Text("RIKO Design System", style = RikoTypography.Display, color = RikoTextPrimary)
        Text(
            "Premium Foundation · Plan 1 Preview Lab",
            style = RikoTypography.Secondary,
            color = RikoTextTertiary
        )
        Spacer(Modifier.height(RikoSpacing.XXL))

        // ── 2. Colors ────────────────────────────────────────────────────
        RikoSectionHeader("Colors")
        Spacer(Modifier.height(RikoSpacing.MD))
        SwatchRow("Background", listOf(
            RikoBackgroundElevated to "Elevated", RikoSurface to "Surface",
            RikoSurfaceElevated to "Elev. 2", RikoSurfaceStrong to "Strong"
        ))
        Spacer(Modifier.height(RikoSpacing.SM))
        SwatchRow("Accent", listOf(RikoPurple to "Purple", RikoPurpleBright to "Bright", RikoPurpleDeep to "Deep"))
        Spacer(Modifier.height(RikoSpacing.SM))
        SwatchRow("Text", listOf(
            RikoTextPrimary to "Primary", RikoTextSecondary to "Secondary",
            RikoTextTertiary to "Tertiary", RikoTextDisabled to "Disabled"
        ))
        Spacer(Modifier.height(RikoSpacing.XXL))

        // ── 3. Typography ────────────────────────────────────────────────
        RikoSectionHeader("Typography")
        Spacer(Modifier.height(RikoSpacing.MD))
        TypeSample("Display", RikoTypography.Display)
        TypeSample("LargeTitle", RikoTypography.LargeTitle)
        TypeSample("Title", RikoTypography.Title)
        TypeSample("Section", RikoTypography.Section)
        TypeSample("Body", RikoTypography.Body)
        TypeSample("Secondary", RikoTypography.Secondary)
        TypeSample("Caption", RikoTypography.Caption)
        Spacer(Modifier.height(RikoSpacing.XXL))

        // ── 4. Buttons ───────────────────────────────────────────────────
        RikoSectionHeader("Buttons")
        Spacer(Modifier.height(RikoSpacing.MD))
        RikoPrimaryButton("Play All", onClick = {}, leadingIcon = Icons.Rounded.PlayArrow)
        Spacer(Modifier.height(RikoSpacing.MD))
        RikoSecondaryButton("Shuffle", onClick = {})
        Spacer(Modifier.height(RikoSpacing.MD))
        Row(horizontalArrangement = Arrangement.spacedBy(RikoSpacing.MD)) {
            RikoIconButton(Icons.Rounded.Favorite, "Favorite", onClick = {}, active = true)
            RikoIconButton(Icons.Rounded.Search, "Search", onClick = {})
            RikoIconButton(Icons.Rounded.Settings, "Settings", onClick = {}, container = false)
        }
        Spacer(Modifier.height(RikoSpacing.XXL))

        // ── 5. Segmented control ─────────────────────────────────────────
        RikoSectionHeader("Segmented Control")
        Spacer(Modifier.height(RikoSpacing.MD))
        RikoSegmentedControl(
            options = listOf("Shuffle", "Play", "Queue"),
            selectedIndex = selectedSegment,
            onOptionSelected = { selectedSegment = it }
        )
        Spacer(Modifier.height(RikoSpacing.XXL))

        // ── 5b. Atomics — SM-1 (toggle, sort chip, filter pills, legal) ──
        RikoSectionHeader("Atomics · SM-1")
        Spacer(Modifier.height(RikoSpacing.MD))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RikoSpacing.LG)
        ) {
            Text("Dark Mode", style = RikoTypography.Body, color = RikoTextPrimary)
            RikoToggleSwitch(checked = darkMode, onCheckedChange = { darkMode = it })
            Spacer(Modifier.weight(1f))
            RikoToggleSwitch(checked = !darkMode, onCheckedChange = { darkMode = !it }, enabled = false)
        }
        Spacer(Modifier.height(RikoSpacing.MD))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RikoSpacing.MD)
        ) {
            RikoSortDropdownChip("Data Added", onClick = {})
            RikoSortDropdownChip("Recently Added", onClick = {})
        }
        Spacer(Modifier.height(RikoSpacing.MD))
        var filterIndex by remember { mutableIntStateOf(0) }
        RikoFilterTabRow(
            labels = listOf("All", "Album", "New Artist", "New Release"),
            selectedIndex = filterIndex,
            onSelected = { filterIndex = it },
            contentPadding = PaddingValues(horizontal = 0.dp)
        )
        Spacer(Modifier.height(RikoSpacing.MD))
        RikoOnboardingLegalText(onTermsClick = {}, onPrivacyClick = {})
        Spacer(Modifier.height(RikoSpacing.XXL))

        // ── 6. Artwork scale ─────────────────────────────────────────────
        RikoSectionHeader("Artwork Scale")
        Spacer(Modifier.height(RikoSpacing.MD))
        Row(
            horizontalArrangement = Arrangement.spacedBy(RikoSpacing.MD),
            verticalAlignment = Alignment.Bottom
        ) {
            RikoArtwork(null, RikoArtworkSize.Thumbnail)
            RikoArtwork(null, RikoArtworkSize.Medium)
            RikoArtwork(null, RikoArtworkSize.Large)
        }
        Spacer(Modifier.height(RikoSpacing.XXL))

        // ── 7. Song rows ─────────────────────────────────────────────────
        RikoSectionHeader("Song Rows", actionText = "See all", onAction = {})
        Spacer(Modifier.height(RikoSpacing.SM))
        RikoTrackRow(title = "Midnight Circuit", subtitle = "Neon Vault", artworkModel = null, isPlaying = true)
        RikoTrackRow(title = "Purple Horizon", subtitle = "Ambient Fields", artworkModel = null)
        RikoTrackRow(title = "Glass Theory", subtitle = "Studio Tapes", artworkModel = null, trailing = {
            RikoIconButton(Icons.Rounded.Favorite, "Favorite", onClick = {}, container = false)
        })
        Spacer(Modifier.height(RikoSpacing.XXL))

        // ── 8. Glass surfaces ────────────────────────────────────────────
        RikoSectionHeader("Glass Surfaces")
        Spacer(Modifier.height(RikoSpacing.MD))
        RikoGlassSurface(modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Box(Modifier.padding(RikoSpacing.MD)) {
                Text("Subtle glass · 5.5%", style = RikoTypography.Body, color = RikoTextSecondary)
            }
        }
        Spacer(Modifier.height(RikoSpacing.MD))
        RikoGlassSurface(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            strong = true,
            borderActive = true
        ) {
            Box(Modifier.padding(RikoSpacing.MD)) {
                Text("Strong glass · 8.5% · active border", style = RikoTypography.Body, color = RikoTextPrimary)
            }
        }
        Spacer(Modifier.height(RikoSpacing.XXL))

        // ── 10. Bottom navigation shell ──────────────────────────────────
        RikoSectionHeader("Bottom Navigation Shell")
        Spacer(Modifier.height(RikoSpacing.MD))
        RikoBottomNavigation(
            items = listOf(
                RikoNavItem(Icons.Rounded.Home, "Home"),
                RikoNavItem(Icons.Rounded.LibraryMusic, "Library"),
                RikoNavItem(Icons.Rounded.Search, "Search"),
                RikoNavItem(Icons.Rounded.Settings, "Settings")
            ),
            selectedIndex = selectedNav,
            onItemSelected = { selectedNav = it }
        )
        Spacer(Modifier.height(RikoSpacing.XXXL))
    }
}

@Composable
private fun SwatchRow(label: String, swatches: List<Pair<androidx.compose.ui.graphics.Color, String>>) {
    Text(label, style = RikoTypography.Label, color = RikoTextTertiary)
    Spacer(Modifier.height(RikoSpacing.XS))
    Row(horizontalArrangement = Arrangement.spacedBy(RikoSpacing.SM)) {
        swatches.forEach { (color, name) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RikoComponentSize.CompactButtonHeight)
                        .clip(RoundedCornerShape(RikoCornerRadius.SM))
                        .background(color)
                )
                Spacer(Modifier.height(RikoSpacing.XS))
                Text(name, style = RikoTypography.Caption, color = RikoTextTertiary, maxLines = 1)
            }
        }
    }
    Spacer(Modifier.height(RikoSpacing.SM))
}

@Composable
private fun TypeSample(label: String, style: androidx.compose.ui.text.TextStyle) {
    Column {
        Text(label, style = RikoTypography.Caption, color = RikoPurpleBright)
        Text("Riko Music", style = style, color = RikoTextPrimary)
    }
    Spacer(Modifier.height(RikoSpacing.SM))
}
