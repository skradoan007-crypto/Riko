package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPurple
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSurface
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — GLOBAL TOP BAR (SM-UI-SPEC-001 v2.0, Sections 2.2–2.5)
//
// Four spec AppBar variants (plus a Search style for in-header search):
//   A (title + search + heart)     — Favourites, Playlist
//   B (back + centered title + heart) — Now Playing, My Music, Fav Songs
//   B-titleless (back + heart)     — My Favourite Songs (spec 3.8)
//   C (avatar + search + heart)    — Home
//   D (title + bell)               — Setting
//
// Geometry: 44dp glass icon buttons, 8dp gap between them, 20dp screen
// margin, 8dp top padding from safe area, 24dp bottom padding to content.
// Title = Title-XL (28sp Bold) for A/D, Title-L (22sp) for B.
// ─────────────────────────────────────────────────────────────────────────────

enum class RikoTopBarStyle { Standard, Back, Centered, Search, Hero }

@Composable
fun RikoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    style: RikoTopBarStyle = RikoTopBarStyle.Standard,
    subtitle: String? = null,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    searchPlaceholder: String = "Search",
    onBack: (() -> Unit)? = null,
    scrolled: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (scrolled) RikoSurface.copy(alpha = 0.92f) else Color.Transparent,
        animationSpec = RikoMotion.normal(),
        label = "rikoTopBarBackground"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(horizontal = RikoSpacing.LG),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (style) {
                RikoTopBarStyle.Standard -> {
                    TopBarTitle(title, modifier = Modifier.weight(1f))
                    actions()
                }
                RikoTopBarStyle.Back -> {
                    BackButton(onBack)
                    TopBarTitle(title, modifier = Modifier.weight(1f))
                    actions()
                }
                RikoTopBarStyle.Centered -> {
                    BackButton(onBack)
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) { TopBarTitle(title) }
                    actions()
                }
                RikoTopBarStyle.Search -> {
                    BackButton(onBack)
                    SearchField(
                        query = query,
                        onQueryChange = onQueryChange,
                        placeholder = searchPlaceholder,
                        modifier = Modifier.weight(1f)
                    )
                    actions()
                }
                RikoTopBarStyle.Hero -> {
                    Column(modifier = Modifier.weight(1f)) {
                        TopBarTitle(title)
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = RikoTypography.Secondary,
                                color = RikoTextTertiary
                            )
                        }
                    }
                    actions()
                }
            }
        }
    }
}

@Composable
private fun TopBarTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = RikoTypography.Title,
        color = RikoTextPrimary,
        maxLines = 1,
        modifier = modifier
    )
}

@Composable
private fun BackButton(onBack: (() -> Unit)?) {
    if (onBack != null) {
        RikoIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
            container = false
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = RikoTypography.Body.copy(color = RikoTextPrimary),
        cursorBrush = SolidColor(RikoPurple),
        singleLine = true,
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = RikoTypography.Body,
                        color = RikoTextTertiary
                    )
                }
                innerTextField()
            }
        }
    )
}

// ── SM-spec AppBar variants (Sections 2.2–2.5) ───────────────────────────────

private val AppBarIconGap = 8.dp
private val AppBarTopPadding = 8.dp
private val AppBarBottomPadding = 24.dp

/**
 * AppBar Variant A — "Title + 2 Icons" (spec 2.2).
 * Screen title (Title-XL, text-primary, left) + IconButton(search) +
 * IconButton(heart-outline), 44dp, 8dp gap. Used on Favourites, Playlist.
 */
@Composable
fun RikoAppBarA(
    title: String,
    onSearchClick: () -> Unit,
    onHeartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = AppBarTopPadding, start = RikoSpacing.XL, end = RikoSpacing.XL)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = RikoTypography.LargeTitle,
            color = RikoTextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        RikoIconButton(Icons.Rounded.Search, "Search", onClick = onSearchClick)
        Spacer(Modifier.width(AppBarIconGap))
        RikoIconButton(Icons.Rounded.FavoriteBorder, "Favorites", onClick = onHeartClick)
    }
}

/**
 * AppBar Variant B — "Back + Centered Title + 1 Icon" (spec 2.3).
 * Used on Now Playing, My Music. Pass [title] = null for the titleless
 * My-Favourite-Songs variant (spec 3.8): back + heart only.
 * Title is CENTERED absolutely in the bar.
 */
@Composable
fun RikoAppBarB(
    onBack: () -> Unit,
    onHeartClick: () -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = AppBarTopPadding, start = RikoSpacing.XL, end = RikoSpacing.XL)
            .heightIn(min = 44.dp)
    ) {
        RikoIconButton(
            Icons.AutoMirrored.Rounded.ArrowBack,
            "Back",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        if (title != null) {
            Text(
                text = title,
                style = RikoTypography.Title,
                color = RikoTextPrimary,
                maxLines = 1,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        RikoIconButton(
            Icons.Rounded.FavoriteBorder,
            "Favorites",
            onClick = onHeartClick,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

/**
 * AppBar Variant C — "Avatar + 2 Icons" (spec 2.4). Home only.
 * Avatar (circular 44dp) left + IconButton(search) + IconButton(heart) right.
 * No title text.
 */
@Composable
fun RikoAppBarC(
    onSearchClick: () -> Unit,
    onHeartClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarModel: Any? = null,
    onAvatarClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = AppBarTopPadding, start = RikoSpacing.XL, end = RikoSpacing.XL)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RikoAppBarAvatar(
            model = avatarModel,
            onClick = onAvatarClick,
            modifier = Modifier.weight(1f),
            alignStart = true
        )
        RikoIconButton(Icons.Rounded.Search, "Search", onClick = onSearchClick)
        Spacer(Modifier.width(AppBarIconGap))
        RikoIconButton(Icons.Rounded.FavoriteBorder, "Favorites", onClick = onHeartClick)
    }
}

/**
 * AppBar Variant D — "Title + Notification Icon" (spec 2.5). Setting screen.
 * Screen title (Title-XL) left + IconButton(bell) right.
 */
@Composable
fun RikoAppBarD(
    title: String,
    onBellClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = AppBarTopPadding, start = RikoSpacing.XL, end = RikoSpacing.XL)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = RikoTypography.LargeTitle,
            color = RikoTextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        RikoIconButton(Icons.Rounded.Notifications, "Notifications", onClick = onBellClick)
    }
}

/** 44dp circular avatar with glass border (spec 2.4). */
@Composable
private fun RikoAppBarAvatar(
    model: Any?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    alignStart: Boolean = false
) {
    val base = modifier
        .then(if (alignStart) Modifier else Modifier)
    Box(
        modifier = base
            .size(44.dp)
            .clip(CircleShape)
            .background(RikoSurface)
            .border(1.dp, RikoGlassBorder, CircleShape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            RikoArtwork(
                model = model,
                contentDescription = "Profile",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = "Profile",
                tint = RikoTextTertiary
            )
        }
    }
}