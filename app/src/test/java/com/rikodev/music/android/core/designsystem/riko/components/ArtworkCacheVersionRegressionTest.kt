package com.rikodev.music.android.core.designsystem.riko.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * C4 regression guard: bumping [ARTWORK_TRANSFORM_VERSION] must invalidate
 * every prior cache entry — a decode-pipeline change must never serve stale
 * bitmaps from the old key space.
 */
class ArtworkCacheVersionRegressionTest {

    @Test
    fun transformVersion_isEncodedInEveryCacheKey() {
        val key = albumArtCacheKey(songId = 1L, albumId = 1L, px = 128)
        assert(key.contains("_v$ARTWORK_TRANSFORM_VERSION")) {
            "cache key must embed the transform version so a bump invalidates old entries"
        }
    }

    @Test
    fun cacheKey_bumpWouldInvalidate() {
        // Simulate the invariant: two keys that differ only in version never collide.
        val current = "song_1_album_1_128_v$ARTWORK_TRANSFORM_VERSION"
        val hypotheticalNext = "song_1_album_1_128_v${ARTWORK_TRANSFORM_VERSION + 1}"
        assertNotEquals(current, hypotheticalNext)
        assertEquals(current, albumArtCacheKey(1L, 1L, 128))
    }

    @Test
    fun rowAndHeroSizes_neverShareCacheEntry() {
        // 128 (row) vs 512 (hero) must be distinct entries — RC-06 contract.
        assertNotEquals(
            albumArtCacheKey(9L, 9L, ARTWORK_PX_ROW),
            albumArtCacheKey(9L, 9L, ARTWORK_PX_HERO)
        )
    }
}
