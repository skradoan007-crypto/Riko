package com.rikodev.ibd3d.android

import android.app.Application
import com.rikodev.ibd3d.core.data.PanelRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RepositoryTest {
    @Test
    fun packagedCatalogAndImages() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repo = PanelRepository(context)
        val data = repo.load()
        assertEquals(212, data.items.size)
        for (item in data.items) {
            item.image
                ?.takeIf { it.isNotBlank() }
                ?.let { path -> context.assets.open(path).use { assertTrue(it.read() != -1) } }
        }
    }

    @Test
    fun preferencesSurviveRepositoryRecreation() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repo = PanelRepository(context)
        val ids = repo.load().items.filter { it.code == "6666" }.map { it.id }
        for (id in ids) if (id in repo.preferences.first().favorites) repo.favorite(id)
        repo.favorite(ids[0])
        repo.record(ids[0])
        repo.record(ids[1])
        repo.record(ids[0])
        repo.feedback(false)
        repo.accent("Blue")
        val restored = PanelRepository(context).preferences.first()
        assertTrue(ids[0] in restored.favorites)
        assertFalse(ids[1] in restored.favorites)
        assertEquals(ids, restored.recent.take(2).map { it.id })
        assertFalse(restored.copyFeedback)
        assertEquals("Blue", restored.accent)
        repo.favorite(ids[0])
        repo.clear()
        repo.feedback(true)
        repo.accent("Purple")
    }
}
