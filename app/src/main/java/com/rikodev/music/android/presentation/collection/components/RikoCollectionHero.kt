package com.rikodev.music.android.presentation.collection.components
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import com.rikodev.music.android.core.designsystem.riko.components.RikoArtwork
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoFavoriteActive
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSurface
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSurfaceElevated
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.presentation.collection.AlbumIdentity
import com.rikodev.music.android.presentation.collection.ArtistIdentity
import com.rikodev.music.android.presentation.collection.CollectionIdentity
import com.rikodev.music.android.presentation.collection.FavoritesIdentity
import com.rikodev.music.android.presentation.collection.FolderIdentity
import com.rikodev.music.android.presentation.collection.PlaylistIdentity

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — COLLECTION HERO (PLAN 5, spec §2, §5–12)
//
// One hero component, five identities:
//   Album    → large SQUARE artwork (never circularized, spec §5)
//   Artist   → CIRCLE avatar (spec §6 — artist ≠ album)
//   Playlist → custom cover or deterministic fallback (spec §9)
//   Favorites→ heart icon identity (spec §10)
//   Folder   → light icon identity — NO huge artwork hero (spec §11)
// ─────────────────────────────────────────────────────────────────────────────

private val ALBUM_ART_SIZE = 200.dp
private val ARTIST_ART_SIZE = 140.dp
private val ICON_IDENTITY_SIZE = 88.dp

@Composable
fun RikoCollectionHero(
    identity: CollectionIdentity,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RikoSpacing.LG),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (identity) {
            is AlbumIdentity -> IdentityArtwork(
                model = identity.artworkModel,
                contentDescription = "Album art for ${identity.title}",
                size = ALBUM_ART_SIZE,
                cornerRadius = RikoCornerRadius.XL
            )
            is PlaylistIdentity -> IdentityArtwork(
                model = identity.coverModel,
                contentDescription = "Playlist art for ${identity.title}",
                size = ALBUM_ART_SIZE,
                cornerRadius = RikoCornerRadius.LG
            )
            is ArtistIdentity -> IdentityCircle(identity.title)
            is FavoritesIdentity -> IdentityIcon(
                icon = {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = RikoFavoriteActive,
                        modifier = Modifier.size(40.dp)
                    )
                }
            )
            is FolderIdentity -> IdentityIcon(
                icon = {
                    Icon(
                        Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = RikoTextTertiary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            )
        }

        Spacer(Modifier.height(RikoSpacing.LG))

        Text(
            text = identity.title,
            style = RikoTypography.Title,
            color = RikoTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        identity.metadata.forEach { line ->
            Spacer(Modifier.height(RikoSpacing.XS))
            Text(
                text = line,
                style = RikoTypography.Secondary,
                color = RikoTextTertiary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun IdentityArtwork(
    model: Any?,
    contentDescription: String,
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(RikoSurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            // C4: remembered request — inline Builder rebuilds Coil on every
            // recomposition of the hero card. Context must be read OUTSIDE
            // remember (LocalContext.current is @Composable).
            val context = LocalContext.current
            val heroModel = remember(model, context) {
                ImageRequest.Builder(context)
                    .data(model)
                    .size(256)
                    .memoryCacheKey("${model}_hero256")
                    .crossfade(200)
                    .allowHardware(true)
                    .build()
            }
            // PERF P0 (RC-06): hero thumb prescale — shared key with same uri elsewhere
            RikoArtwork(
                model = heroModel,
                contentDescription = contentDescription,
                modifier = Modifier.size(size)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = RikoTextTertiary,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun IdentityCircle(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(ARTIST_ART_SIZE)
            .clip(CircleShape)
            .background(RikoSurface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "#",
            style = RikoTypography.Display,
            color = RikoTextSecondary
        )
    }
}

@Composable
private fun IdentityIcon(
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(ICON_IDENTITY_SIZE)
            .clip(RoundedCornerShape(RikoCornerRadius.LG))
            .background(RikoSurface),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}