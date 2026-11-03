package com.rikodev.ibd3d.android

import android.content.ActivityNotFoundException
import android.content.Intent
import com.rikodev.ibd3d.android.store.GameStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class GameStoreTest {
    @Test
    fun opensListingNotGame() {
        val intents = mutableListOf<Intent>()
        assertTrue(
            GameStore.open {
                intents.add(it)
                Unit
            }
        )
        assertEquals(1, intents.size)
        assertEquals(Intent.ACTION_VIEW, intents.single().action)
        assertEquals("com.android.vending", intents.single().`package`)
        assertEquals("market://details?id=com.Rohit.IndianBikes", intents.single().dataString)
    }

    @Test
    fun unavailablePlayFallsBackToWebListing() {
        val intents = mutableListOf<Intent>()
        assertTrue(
            GameStore.open {
                intents.add(it)
                if (intents.size == 1) throw ActivityNotFoundException()
            }
        )
        assertEquals(2, intents.size)
        val web = intents.last()
        assertEquals(Intent.ACTION_VIEW, web.action)
        assertNull(web.`package`)
        assertTrue(web.hasCategory(Intent.CATEGORY_BROWSABLE))
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.Rohit.IndianBikes",
            web.dataString,
        )
        assertTrue(intents.none { it.`package` == GameStore.GAME_PACKAGE })
    }

    @Test
    fun restrictedPlayAlsoFallsBack() {
        var attempts = 0
        assertTrue(GameStore.open { if (++attempts == 1) throw SecurityException() })
        assertEquals(2, attempts)
    }

    @Test
    fun noHandlerReturnsFailureInsteadOfCrashing() {
        var attempts = 0
        assertFalse(
            GameStore.open {
                attempts++
                throw ActivityNotFoundException()
            }
        )
        assertEquals(2, attempts)
    }
}
