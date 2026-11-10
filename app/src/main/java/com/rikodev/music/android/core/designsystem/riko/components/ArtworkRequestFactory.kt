package com.rikodev.music.android.core.designsystem.riko.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

/**
 * Canonical album-art request factory (campaign plan 1.2 — artwork pipeline
 * stabilization). Screens build artwork requests HERE, never inline.
 *
 * P0 FIX (album-art mix-up): YouTube-downloaded songs often share the same
 * MediaStore albumId (bad metadata), so `content://media/external/audio/albumart/$albumId`
 * returns the SAME image for different songs. Now extracts **embedded art**
 * per-song via MediaMetadataRetriever — always correct.
 *
 * P0 FIX (cache poison): warmup used to enqueue the colliding album-art URI
 * under the same memoryCacheKey the UI uses for embedded art, so Coil served
 * the WRONG bitmap on a cache hit. Every path (warmup + UI) now resolves
 * through [resolveArtworkData] — one source of truth, one cache identity.
 *
 * Cache-key contract: song identity + size + transformation version — per-song
 * isolation even when albumId collides.
 *
 * Crossfade policy (plan 1.2): dense lists pass the default crossfadeMs = 0
 * (a fling must not animate every row bind); hero placements opt into a
 * short controlled fade via [HERO_CROSSFADE_MS].
 */

/** Bump when the artwork decode/transform pipeline changes shape. */
const val ARTWORK_TRANSFORM_VERSION = 4

/** Controlled short crossfade for hero artwork (banner, NP pages). */
const val HERO_CROSSFADE_MS = 200

/** Prescale budgets (RC-06 convention): rows/mini 128px, cards 256px, hero 512px. */
const val ARTWORK_PX_ROW = 128
const val ARTWORK_PX_SHELF = 256
const val ARTWORK_PX_HERO = 512

/** Album-art content URI for an album, or null when there is no album art. */
fun albumArtUri(albumId: Long): String? =
    if (albumId > 0) "content://media/external/audio/albumart/$albumId" else null

/**
 * Stable cache identity: song + album + size + transformation version.
 * Per-song isolation — two songs sharing a MediaStore albumId never share
 * a cache entry.
 */
fun albumArtCacheKey(songId: Long, albumId: Long, px: Int): String =
    "song_${songId}_album_${albumId}_${px}_v$ARTWORK_TRANSFORM_VERSION"

/**
 * Extract embedded album art from the audio file itself (per-song, always
 * correct). Returns null when no embedded art exists. Runs on caller's
 * thread — call from Dispatchers.IO or accept main-thread cost inside
 * `remember` (cached per song via [embeddedArtCache]).
 */
fun extractEmbeddedArt(context: android.content.Context, songUri: Uri?): Bitmap? {
    if (songUri == null) return null
    return try {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, songUri)
            val art = retriever.embeddedPicture
            if (art != null && art.isNotEmpty()) {
                BitmapFactory.decodeByteArray(art, 0, art.size)
            } else {
                null
            }
        } finally {
            runCatching { retriever.release() }
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Per-song embedded-art cache. Keyed by songUri string so a given file's
 * embedded picture is decoded once, not on every recomposition / scroll.
 * Entries are immutable Bitmaps (never recycled by us).
 */
private val embeddedArtCache = object : LruCache<String, Bitmap>(32) {}

/** Cache hit-or-miss wrapper. Null result is not cached (retried next time). */
fun cachedEmbeddedArt(context: android.content.Context, songUri: Uri?): Bitmap? {
    if (songUri == null) return null
    val key = songUri.toString()
    embeddedArtCache.get(key)?.let { return it }
    val decoded = extractEmbeddedArt(context, songUri) ?: return null
    embeddedArtCache.put(key, decoded)
    return decoded
}

/**
 * THE single artwork data resolver — every screen and warmup goes through here.
 *
 * 1. Prefer per-song embedded art (correct even when MediaStore albumId collides).
 * 2. Fall back to the album-art content URI only when the file has no embedded picture.
 * 3. Null when neither exists.
 */
fun resolveArtworkData(
    context: android.content.Context,
    songId: Long,
    albumId: Long,
    songUri: Uri?
): Any? {
    val embedded = cachedEmbeddedArt(context, songUri)
    if (embedded != null && !embedded.isRecycled) return embedded
    return albumArtUri(albumId)
}

/**
 * Remembered album-art request.
 *
 * P0 FIX: extracts embedded art from the song's own content URI (per-song)
 * instead of the album-art URI (per-album, collides with bad metadata).
 * Falls back to album-art URI when the song has no embedded art.
 */
@Composable
fun rememberAlbumArtworkModel(
    albumId: Long,
    px: Int,
    crossfadeMs: Int = 0,
    songId: Long = 0L,
    songUri: Uri? = null
): Any? {
    val context = LocalContext.current
    return remember(songUri, songId, albumId, px, crossfadeMs) {
        val data = resolveArtworkData(context, songId, albumId, songUri)
            ?: return@remember null
        buildArtworkRequest(context, data, songId, albumId, px, crossfadeMs)
    }
}

/**
 * Build a size-aware, song-isolated Coil request around already-resolved artwork data.
 * Used by Compose call sites and by non-Compose warmup so both share one cache identity.
 */
fun buildArtworkRequest(
    context: android.content.Context,
    data: Any,
    songId: Long,
    albumId: Long,
    px: Int,
    crossfadeMs: Int = 0
): ImageRequest {
    val builder = ImageRequest.Builder(context)
        .data(data)
        .size(px)
        .memoryCacheKey(albumArtCacheKey(songId, albumId, px))
        .diskCacheKey(albumArtCacheKey(songId, albumId, px))
    if (crossfadeMs > 0) builder.crossfade(crossfadeMs) else builder.crossfade(false)
    builder.allowHardware(true)
    return builder.build()
}
