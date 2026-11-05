package com.rikodev.ibd3d.core.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.rikodev.ibd3d.core.model.*
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.store by
    preferencesDataStore(
        name = "panel",
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    )

data class UserPreferences(
    val favorites: Set<String> = emptySet(),
    val recent: List<RecentCopy> = emptyList(),
    val copyFeedback: Boolean = true,
    val accent: String = "Purple",
    val noticeSeen: Boolean = false,
    val reminder: Boolean = true,
    val loaded: Boolean = false,
)

data class CatalogData(val items: List<Cheat>, val groups: Map<String, Grouping>) {
    // Built once on the IO dispatcher with the catalog, never rebuilt while typing or scrolling.
    val byId: Map<String, Cheat> = items.associateBy { it.id }
    val browseCount: Int = items.count { it.isBrowseable && it.section == "codes" }
}

class PanelRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var cached: CatalogData? = null
    private val fav = stringSetPreferencesKey("favorites")
    private val recent = stringPreferencesKey("recent_v1")
    private val feedback = booleanPreferencesKey("feedback")
    private val accent = stringPreferencesKey("accent")
    private val notice = booleanPreferencesKey("resource_notice_seen")
    private val reminder = booleanPreferencesKey("copy_reminder")
    val preferences =
        context.store.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { p ->
                UserPreferences(
                    p[fav] ?: emptySet(),
                    runCatching { json.decodeFromString<List<RecentCopy>>(p[recent] ?: "[]") }
                        .getOrDefault(emptyList()),
                    p[feedback] ?: true,
                    p[accent] ?: "Purple",
                    p[notice] ?: false,
                    p[reminder] ?: true,
                    true,
                )
            }

    suspend fun load(): CatalogData =
        mutex.withLock {
            cached
                ?: withContext(Dispatchers.IO) {
                    val c =
                        context.assets.open("catalog.json").bufferedReader().use {
                            json.decodeFromString<Catalog>(it.readText())
                        }
                    validateCatalog(c)
                    val m =
                        context.assets.open("presentation_mapping.json").bufferedReader().use {
                            json.decodeFromString<PresentationMap>(it.readText())
                        }
                    require(c.items.all { it.id in m.items })
                    val aliases =
                        c.items
                            .mapNotNull { row -> row.replacementId?.let { row.id to it } }
                            .toMap()
                    context.store.edit { p ->
                        p[fav] = p[fav].orEmpty().map { aliases[it] ?: it }.toSet()
                        val history =
                            runCatching {
                                    json.decodeFromString<List<RecentCopy>>(p[recent] ?: "[]")
                                }
                                .getOrDefault(emptyList())
                        p[recent] =
                            json.encodeToString(
                                history
                                    .map { it.copy(id = aliases[it.id] ?: it.id) }
                                    .distinctBy { it.id }
                            )
                    }
                    CatalogData(c.items, m.items).also { cached = it }
                }
        }

    suspend fun favorite(id: String) {
        context.store.edit { p ->
            val ids = p[fav] ?: emptySet()
            p[fav] = if (id in ids) ids - id else ids + id
        }
    }

    suspend fun record(id: String) {
        context.store.edit { p ->
            val old =
                runCatching { json.decodeFromString<List<RecentCopy>>(p[recent] ?: "[]") }
                    .getOrDefault(emptyList())
            p[recent] = json.encodeToString(recordCopy(old, id, System.currentTimeMillis()))
        }
    }

    suspend fun feedback(value: Boolean) {
        context.store.edit { it[feedback] = value }
    }

    suspend fun accent(value: String) {
        require(value in listOf("Purple", "Blue", "Rose"))
        context.store.edit { it[accent] = value }
    }

    suspend fun noticeSeen() {
        context.store.edit { it[notice] = true }
    }

    suspend fun reminder(value: Boolean) {
        context.store.edit { it[reminder] = value }
    }

    suspend fun clear() {
        context.store.edit { it.remove(recent) }
    }
}
