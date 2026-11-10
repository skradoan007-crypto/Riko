package com.rikodev.music.android.core.designsystem.riko.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtworkRequestFactoryTest {

    @Test
    fun cacheKey_encodesSongAlbumSizeAndTransformVersion() {
        val key = albumArtCacheKey(songId = 42L, albumId = 7L, px = 128)
        assertEquals("song_42_album_7_128_v$ARTWORK_TRANSFORM_VERSION", key)
    }

    @Test
    fun cacheKey_sameAlbumDifferentSizesNeverShareEntry() {
        assertNotEquals(
            albumArtCacheKey(songId = 42L, albumId = 7L, px = 128),
            albumArtCacheKey(songId = 42L, albumId = 7L, px = 512)
        )
    }

    @Test
    fun cacheKey_sameSizeDifferentSongsNeverShareEntry() {
        assertNotEquals(
            albumArtCacheKey(songId = 42L, albumId = 7L, px = 128),
            albumArtCacheKey(songId = 43L, albumId = 7L, px = 128)
        )
    }

    @Test
    fun albumArtUri_positiveIdBuildsContentUri() {
        assertEquals("content://media/external/audio/albumart/42", albumArtUri(42L))
    }

    @Test
    fun albumArtUri_invalidIdIsNull() {
        assertNull(albumArtUri(0L))
        assertNull(albumArtUri(-1L))
    }
}
