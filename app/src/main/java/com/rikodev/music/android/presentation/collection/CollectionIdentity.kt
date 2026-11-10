package com.rikodev.music.android.presentation.collection

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — COLLECTION IDENTITY (PLAN 5, spec §12)
//
// "Same skeleton, different identity." Each collection type declares its own
// hero identity + metadata; the shared RikoCollectionHero renders it with the
// right visual treatment (square art / circle avatar / icon identity).
//
// NO entity-specific business logic lives here — pure presentation model.
// ─────────────────────────────────────────────────────────────────────────────

sealed interface CollectionIdentity {
    /** Primary name — rendered large under the hero. */
    val title: String
    /** Secondary metadata lines (artist · N tracks · duration...). */
    val metadata: List<String>
}

data class AlbumIdentity(
    override val title: String,
    override val metadata: List<String>,
    val artworkModel: Any?
) : CollectionIdentity

data class ArtistIdentity(
    override val title: String,
    override val metadata: List<String>
) : CollectionIdentity

data class PlaylistIdentity(
    override val title: String,
    override val metadata: List<String>,
    /** Custom playlist cover — null → deterministic Riko fallback (spec §9). */
    val coverModel: Any?
) : CollectionIdentity

data class FavoritesIdentity(
    override val title: String,
    override val metadata: List<String>
) : CollectionIdentity

data class FolderIdentity(
    override val title: String,
    override val metadata: List<String>,
    /** Full folder path — shown only when useful (spec §11). */
    val path: String? = null
) : CollectionIdentity