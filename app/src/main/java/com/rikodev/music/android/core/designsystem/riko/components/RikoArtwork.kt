package com.rikodev.music.android.core.designsystem.riko.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.imageLoader
import coil.request.ImageRequest
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoComponentSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSurfaceElevated
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — ARTWORK (SM-UI-SPEC-001 v2.0, Sections 1.7 / 2.18)
//
// P0 FIX (back-button crash): Coil's `AsyncImage` internally sets
// `progressListener` which throws `IllegalStateException: You must collect
// the progress flow` when the activity is destroyed mid-load (back button).
// This implementation uses `ImageLoader.enqueue()` with explicit `listener`
// callback + `LaunchedEffect` + `mutableStateOf` — NO Coil composable,
// NO progress-flow crash.
// ─────────────────────────────────────────────────────────────────────────────

enum class RikoArtworkSize(
    val size: Dp,
    val shape: Shape
) {
    Thumbnail(RikoComponentSize.ArtworkThumbnail, RikoShape.Artwork),
    /** Home shelf cards (72–88dp range). */
    Shelf(RikoComponentSize.ArtworkShelf, RikoShape.Card),
    Medium(RikoComponentSize.ArtworkMedium, RikoShape.Card),
    Large(RikoComponentSize.ArtworkLarge, RoundedCornerShape(RikoCornerRadius.LG)),
    /** Now Playing circular hero artwork (spec 2.18: 280dp circle). */
    Hero(RikoComponentSize.ArtworkHero, CircleShape),
    /** Playlist-detail circular hero (spec 3.8: 200–220dp circle). */
    FavoritesHero(RikoComponentSize.ArtworkFavoritesHero, CircleShape)
}

/**
 * Load image via ImageLoader.enqueue with explicit listener — no Coil
 * composable, no progress-flow crash. Returns null while loading or on error.
 */
@Composable
private fun rememberSafeArtworkPainter(model: Any?): Painter? {
    val context = LocalContext.current
    var imageBitmap by remember(model) { mutableStateOf<ImageBitmap?>(null) }
    androidx.compose.runtime.LaunchedEffect(model) {
        if (model == null) return@LaunchedEffect
        val result = suspendCancellableCoroutine<ImageBitmap?> { cont ->
            val request = if (model is ImageRequest) {
                model.newBuilder().listener(
                    onSuccess = { _, r ->
                        val bmp = (r.drawable as? BitmapDrawable)?.bitmap
                        if (bmp != null && !bmp.isRecycled && cont.isActive) {
                            cont.resume(bmp.asImageBitmap())
                        } else if (cont.isActive) {
                            cont.resume(null)
                        }
                    },
                    onError = { _, _ -> if (cont.isActive) cont.resume(null) }
                ).build()
            } else {
                ImageRequest.Builder(context)
                    .data(model)
                    .listener(
                        onSuccess = { _, r ->
                            val bmp = (r.drawable as? BitmapDrawable)?.bitmap
                            if (bmp != null && !bmp.isRecycled && cont.isActive) {
                                cont.resume(bmp.asImageBitmap())
                            } else if (cont.isActive) {
                                cont.resume(null)
                            }
                        },
                        onError = { _, _ -> if (cont.isActive) cont.resume(null) }
                    )
                    .build()
            }
            val disposable = context.imageLoader.enqueue(request)
            cont.invokeOnCancellation { disposable.dispose() }
        }
        imageBitmap = result
    }
    return imageBitmap?.let { BitmapPainter(it) }
}

@Composable
fun RikoArtwork(
    model: Any?,
    size: RikoArtworkSize? = null,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    fallback: (@Composable () -> Unit)? = null
) {
    val shell = if (size != null) {
        modifier
            .size(size.size)
            .clip(size.shape)
            .background(RikoSurfaceElevated)
    } else {
        modifier.fillMaxSize()
    }
    Box(
        modifier = shell,
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            val painter = rememberSafeArtworkPainter(model)
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        } else if (fallback != null) {
            fallback()
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = contentDescription,
                tint = RikoTextTertiary,
                modifier = Modifier.size((size?.size ?: 40.dp) * 0.4f)
            )
        }
    }
}
