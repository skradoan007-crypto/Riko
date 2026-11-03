package com.rikodev.ibd3d.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class CatalogTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun resource(name: String) =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)).bufferedReader().use {
            it.readText()
        }

    private val catalog = json.decodeFromString<Catalog>(resource("catalog.json"))
    private val groups =
        json.decodeFromString<PresentationMap>(resource("presentation_mapping.json")).items

    @Test
    fun integrity() {
        validateCatalog(catalog)
        assertEquals(212, catalog.items.size)
        assertTrue(catalog.items.all { it.id in groups })
    }

    @Test
    fun leadingZeroRoundTrip() {
        val decoded = json.decodeFromString<Catalog>(json.encodeToString(catalog))
        for (code in listOf("0000", "0015", "0701", "01212", "1#")) {
            assertTrue(decoded.items.any { it.code == code })
        }
    }

    @Test
    fun duplicateCodesStayIndependent() {
        val duplicate = catalog.items.filter { it.code == "6666" }
        assertEquals(2, duplicate.size)
        assertEquals(2, duplicate.map { it.id }.toSet().size)
        assertEquals(2, duplicate.map { it.name }.toSet().size)
    }

    @Test
    fun searchMatchesNameAndCode() {
        val item = catalog.items.first()
        assertTrue(item in filterCodes(catalog.items, groups, item.name.uppercase(), "All"))
        assertTrue(item in filterCodes(catalog.items, groups, item.code, "All"))
        assertTrue(filterCodes(catalog.items, groups, " no-such-entry-9374 ", "All").isEmpty())
    }

    @Test
    fun categoryFilterComposesWithSearch() {
        val rows = filterCodes(catalog.items, groups, "", "Bike")
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { groups[it.id]?.chip == "Bike" })
    }

    @Test
    fun rgsCommandsRemainExact() {
        val commands = catalog.items.filter { it.status == "rgs_tool" }
        assertEquals(48, commands.size)
        assertEquals(commands, json.decodeFromString<List<Cheat>>(json.encodeToString(commands)))
    }

    @Test
    fun historyUsesIdAndMovesNewestFirst() {
        val h = recordCopy(recordCopy(recordCopy(emptyList(), "a", 1), "b", 2), "a", 3)
        assertEquals(listOf("a", "b"), h.map { it.id })
        assertEquals(3L, h.first().copiedAt)
    }

    @Test
    fun historyIsBounded() {
        var h = emptyList<RecentCopy>()
        repeat(70) { h = recordCopy(h, "id$it", it.toLong()) }
        assertEquals(50, h.size)
        assertEquals("id69", h.first().id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateIds() {
        validateCatalog(
            catalog.copy(count = 2, items = listOf(catalog.items.first(), catalog.items.first()))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownSchema() {
        validateCatalog(catalog.copy(schemaVersion = 99))
    }

    @Test
    fun unrelatedGameEntriesRemainAddressableButAreExcludedFromBrowsing() {
        val archived = catalog.items.filter { it.status == "wrong_game" }
        assertEquals(setOf("875", "309", "247", "736", "914"), archived.map { it.code }.toSet())
        val browsed = filterCodes(catalog.items, groups, "", "All")
        assertEquals(207, browsed.size)
        assertTrue(archived.none { it in browsed })
        assertTrue(archived.all { item -> catalog.items.any { it.id == item.id } })
    }

    @Test
    fun tarzenIsCarAndRobotIsOther() {
        val tarzen = catalog.items.first { it.code == "300" }
        assertTrue(tarzen in filterCodes(catalog.items, groups, "", "Car"))
        assertTrue(tarzen in filterCodes(catalog.items, groups, "", "Vehicles"))
        assertEquals("Other", groups[catalog.items.first { it.code == "3380" }.id]?.chip)
    }

    @Test
    fun unprovenInputAndPublisherAmbiguityAreNotPresentedAsConfirmed() {
        val rumors =
            catalog.items.filter {
                it.code in
                    setOf(
                        "FRANKLIN",
                        "FOGMODE",
                        "HILLSTATION",
                        "NIGHTCITY",
                        "SUPERBIKE2",
                        "DORAEMONS",
                    )
            }
        assertEquals(6, rumors.size)
        assertTrue(rumors.all { it.inputMethod == "unknown" && it.verification == "unverified" })
        val ambiguous =
            catalog.items.filter { it.status == "official" && it.code in setOf("6666", "12345") }
        assertEquals(4, ambiguous.size)
        assertTrue(
            ambiguous.all { it.verification == "official_ambiguous" && "ambiguous" in it.badge }
        )
    }
}
