package com.rikodev.ibd3d.core.model

import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data class Catalog(
    val schemaVersion: Int,
    val catalogVersion: Int,
    val count: Int,
    val items: List<Cheat>,
)

@Serializable
data class Cheat(
    val id: String,
    val code: String,
    val name: String,
    val category: String,
    val status: String,
    val inputMethod: String,
    val image: String? = null,
    val imageStatus: String = "",
    val notes: String = "",
    val verification: String = "",
    val reviewedOn: String = "",
    val sources: List<String> = emptyList(),
    val section: String = "codes",
    val replacementId: String? = null,
    val modelAsset: String? = null,
    val modelFormat: String = "",
) {
    val isBrowseable: Boolean
        get() = status !in setOf("wrong_game", "superseded")

    val badge: String
        get() =
            when (status) {
                "official" ->
                    if (verification == "official_ambiguous") "Official • ambiguous"
                    else "Official list"
                "community" -> "Community catalog"
                "plugin" -> "Plugin link"
                "model" -> "Bundled offline"
                "superseded" -> "Replaced entry"
                "wrong_game" -> "Different game"
                "conflict" -> "Conflicting"
                "derived" -> "Experimental"
                "rgs_tool" -> "RGS Tool • community"
                else -> "Unverified"
            }
}

@Serializable data class Grouping(val chip: String, val vehiclesShortcut: Boolean)

@Serializable data class PresentationMap(val items: Map<String, Grouping>)

@Serializable data class RecentCopy(val id: String, val copiedAt: Long)

fun recordCopy(history: List<RecentCopy>, id: String, time: Long) =
    (listOf(RecentCopy(id, time)) + history.filterNot { it.id == id }).take(50)

fun filterCodes(
    items: List<Cheat>,
    groups: Map<String, Grouping>,
    query: String,
    category: String,
): List<Cheat> {
    val q = query.trim().lowercase(Locale.ROOT)
    return items.filter {
        it.isBrowseable && it.section == "codes" &&
            (category == "All" ||
                (if (category == "Vehicles") groups[it.id]?.vehiclesShortcut == true
                else groups[it.id]?.chip == category)) &&
            (q.isEmpty() ||
                it.name.lowercase(Locale.ROOT).contains(q) ||
                it.code.lowercase(Locale.ROOT).contains(q))
    }
}

fun validateCatalog(c: Catalog) {
    require(c.schemaVersion == 1)
    require(c.count == c.items.size)
    require(c.items.map { it.id }.toSet().size == c.items.size)
    require(c.items.all { it.id.isNotBlank() && it.code.isNotBlank() && it.name.isNotBlank() })
}
