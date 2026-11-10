/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B4 — Library Rendering Runtime
 *
 * File 3 / 13 — RenderItem.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * A [RenderItem] is the *visual* representation of a single card
 * inside a Library section. It is the smallest unit the UI draws.
 *
 * Each [RenderItem] is resolved from a [CardSnapshot] (runtime) +
 * the parent section's contract. The opaque payload on the runtime
 * card is *interpreted* here — the UI never has to inspect `payload`.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #28  Motion Explains Hierarchy   — per-item motion hint
 * #33  Collection Has Identity     — itemId is stable
 * #44  Stable Key Never Changes    — all keys are strings
 * #46  Runtime Never Creates Identity — itemId derives from contract, not invented
 *
 * WHY A SEPARATE TYPE
 * -------------------
 * The runtime's [CardSnapshot] carries an opaque `payload: Any?`.
 * This is intentional — the runtime must remain agnostic to card
 * shape (it hosts song cards, album cards, artist cards, etc.).
 *
 * But the UI cannot render `Any?`. The renderer (B4) interprets
 * the payload and produces a [RenderItem] with concrete fields the
 * UI can consume directly. The opaque payload never crosses into
 * the Compose layer.
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no Compose, no Coroutine, no Flow. Pure Kotlin data.
 *
 * © RIKO Experience OS — Rendering Tier
 */

package com.rikodev.music.android.presentation.library.rendering.model

/**
 * The kind of entity a render item represents.
 *
 * Drives icon selection, default artwork, accessibility role, and
 * click routing in the UI. The renderer resolves this from the
 * parent collection's type — the UI never has to peek at the
 * underlying payload.
 */
enum class RenderItemKind(internal val description: String) {
    /** A single track / song. */
    SONG("A single track."),

    /** An album (collection of songs). */
    ALBUM("An album."),

    /** An artist. */
    ARTIST("An artist."),

    /** A user-created or system-created playlist. */
    PLAYLIST("A playlist."),

    /** A genre. */
    GENRE("A genre."),

    /** A mood / context cluster. */
    MOOD("A mood cluster."),

    /** A decade / time period. */
    TIME_PERIOD("A decade or time period."),

    /** A composite / abstract card (e.g. "Surprise me"). */
    ABSTRACT("An abstract composite card."),
}

/**
 * Visual badge on a card — small chip overlaying the artwork.
 *
 * Badges communicate state at a glance: "playing", "new", "downloaded",
 * "explicit", etc. The renderer resolves which badges to show based
 * on the runtime snapshot + collection personality.
 *
 * @property kind      Badge kind — drives icon and color.
 * @property label     Optional text label (e.g. "HD" for high-quality).
 * @property priority  Lower numbers render first (left-most). 0 = highest.
 */
data class RenderItemBadge(
    val kind: RenderItemBadgeKind,
    val label: String?,
    val priority: Int,
) {
    companion object {
        val PLAYING = RenderItemBadge(
            kind = RenderItemBadgeKind.NOW_PLAYING,
            label = null,
            priority = 0,
        )
        val NEW = RenderItemBadge(
            kind = RenderItemBadgeKind.NEW,
            label = null,
            priority = 10,
        )
    }
}

/**
 * Kinds of badges a card can carry.
 */
enum class RenderItemBadgeKind(internal val description: String) {
    /** Currently playing — animated equalizer icon. */
    NOW_PLAYING("Currently playing."),

    /** Recently added — "NEW" pill. */
    NEW("Recently added."),

    /** Downloaded for offline. */
    DOWNLOADED("Downloaded."),

    /** Explicit content. */
    EXPLICIT("Explicit content."),

    /** High-quality audio available. */
    HIGH_QUALITY("High-quality audio."),

    /** Favorited by the user. */
    FAVORITE("Favorited."),

    /** Smart / dynamic — content changes over time. */
    DYNAMIC("Smart / dynamic."),

    /** Replay-strong — user has played this many times. */
    REPLAY_STRONG("Frequently played."),
}

/**
 * Artwork resolution for a card.
 *
 * The renderer never resolves bitmaps — that is a job for the image
 * loader in B5+. But the renderer DOES resolve *which* artwork key
 * to use, and what fallback to apply if the key has no bitmap.
 *
 * @property artworkKey    Stable key for the image loader (e.g. albumId).
 * @property artworkShape  Shape of the artwork (square / circle / etc.).
 * @property fallbackTintHex  Color to use when no bitmap is available.
 * @property fallbackInitials  1–2 character initials when no bitmap.
 */
data class RenderItemArtwork(
    val artworkKey: String,
    val artworkShape: ArtworkShape,
    val fallbackTintHex: String,
    val fallbackInitials: String,
) {
    companion object {
        fun placeholder(artworkKey: String): RenderItemArtwork = RenderItemArtwork(
            artworkKey = artworkKey,
            artworkShape = ArtworkShape.SQUARE,
            fallbackTintHex = "#888888",
            fallbackInitials = "?",
        )
    }
}

/**
 * Shape of a card's artwork.
 */
enum class ArtworkShape(internal val description: String) {
    /** Square — albums, playlists, genres. */
    SQUARE("Square artwork."),

    /** Circle — artists, moods. */
    CIRCLE("Circle artwork."),

    /** Rounded rectangle — abstract / hero cards. */
    ROUNDED_RECT("Rounded rectangle artwork."),

    /** Wide banner — time-period cards. */
    WIDE_BANNER("Wide banner artwork."),
}

/**
 * Per-item motion hint.
 *
 * Rule #29 — Motion Explains Hierarchy.
 * Per-item motion is *subordinate* to section-level motion. An item
 * never animates independently of its section — that would break
 * the visual hierarchy contract.
 */
enum class RenderItemMotionHint(internal val description: String) {
    /** No motion — item is stable. */
    NONE("No motion."),

    /** Item just appeared (newly added or freshly hydrated). */
    ENTER("Item enters its slot."),

    /** Item was removed — its slot should collapse. */
    EXIT("Item exits; slot collapses."),

    /** Item changed position within the section. */
    REORDER("Item moved to a new position."),

    /** Item content changed (title / artwork / badge). */
    CONTENT_CHANGE("Item content updated; layout stable."),

    /** Item is being pressed — gentle scale-down. */
    PRESS("Item is being pressed."),

    /** Item is being played — equalizer animation hint. */
    PLAYING("Item is playing — equalizer animation."),
}

/**
 * The resolved visual representation of a single card.
 *
 * @property itemId           Stable identity — derives from underlying entity.
 *                             Rule #44 / #46. Never an index.
 * @property kind             Entity kind — drives icon routing.
 * @property title            Primary title text (already localized).
 * @property subtitle         Optional subtitle (e.g. artist name).
 * @property tertiaryLabel    Optional third line (e.g. "Last played 2h ago").
 * @property artwork          Resolved artwork spec.
 * @property badges           Ordered list of badges (lowest priority first).
 * @property accentHex        Per-item accent color override (else use section's).
 * @property motionHint       Motion hint for this item. Rule #29.
 * @property isSelected       True if currently in multi-select selection.
 * @property isPlaying        True if this is the currently-playing item.
 * @property isFocused        True if this item has accessibility focus.
 * @property isEnabled        False if the item is non-interactive (e.g. locked).
 * @property metadata         Optional serializable metadata bag — for the
 *                             UI to attach click-routing hints without
 *                             forcing the renderer to know about routing.
 */
data class RenderItem(
    val itemId: String,
    val kind: RenderItemKind,
    val title: String,
    val subtitle: String?,
    val tertiaryLabel: String?,
    val artwork: RenderItemArtwork,
    val badges: List<RenderItemBadge>,
    val accentHex: String,
    val motionHint: RenderItemMotionHint,
    val isSelected: Boolean,
    val isPlaying: Boolean,
    val isFocused: Boolean,
    val isEnabled: Boolean,
    val metadata: RenderItemMetadata,
) {
    /** True if the item carries the now-playing badge. */
    val hasNowPlayingBadge: Boolean
        get() = badges.any { it.kind == RenderItemBadgeKind.NOW_PLAYING }

    /** True if the item has any badges. */
    val hasBadges: Boolean get() = badges.isNotEmpty()
}

/**
 * Serializable metadata bag for a render item.
 *
 * This is the *only* channel for the renderer to pass click-routing
 * hints to the UI without coupling. The UI reads these keys when
 * the user taps an item and dispatches the right action.
 *
 * Rule #44 — All keys are stable strings.
 * Rule #45 — Values are serializable-shaped (String / Int / Long / Boolean).
 *
 * Standard keys (defined as constants on the companion object) form
 * the contract between renderer and UI. Unknown keys are ignored
 * by the UI — this allows the renderer to evolve ahead of the UI.
 */
data class RenderItemMetadata(
    val values: Map<String, String>,
) {
    companion object {
        // Standard keys — DO NOT rename. UI depends on these strings.
        const val KEY_ENTITY_ID = "entity.id"
        const val KEY_ENTITY_KIND = "entity.kind"
        const val KEY_PARENT_COLLECTION = "parent.collection"
        const val KEY_PARENT_SECTION = "parent.section"
        const val KEY_CLICK_ROUTE = "click.route"
        const val KEY_LONG_PRESS_ROUTE = "longpress.route"
        const val KEY_SOURCE_STRATEGY = "source.strategy"

        val EMPTY = RenderItemMetadata(values = emptyMap())
    }

    /** Read a key, or null if absent. */
    fun get(key: String): String? = values[key]

    /** Read a key with a fallback. */
    fun getOrDefault(key: String, default: String): String =
        values[key] ?: default

    /** Builder-style copy with an additional key set. */
    fun with(key: String, value: String): RenderItemMetadata =
        copy(values = values + (key to value))
}
