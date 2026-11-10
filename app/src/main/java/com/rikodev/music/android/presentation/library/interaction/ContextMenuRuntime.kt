/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 6 / 15 — ContextMenuRuntime.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Generate the *structure* of a context menu for any Library target.
 * Pure: same target + same state → same menu, byte-identical.
 *
 * The Runtime produces a [ContextMenuSpec] — a tree of sections and
 * items. It does NOT produce UI (no @Composable, no Painter, no
 * Color). The UI layer (Phase B Identity) consumes the spec and
 * renders it.
 *
 * MENU STRUCTURE
 * --------------
 * A context menu has:
 *   - Header (optional) — target title, subtitle, artwork ref
 *   - Sections — groups of related actions
 *     - Each section has items
 *     - Each item has: id, title, semantic icon, isDestructive, isEnabled
 *
 * MENUS BY TARGET TYPE
 * --------------------
 *   SONG      — Play, Add to Queue, Add to Playlist, Favorite, Pin,
 *               Share, Info, Hide, Delete
 *   ALBUM     — Play All, Shuffle, Add to Queue, Add to Playlist,
 *               Pin, Share, Info, Delete
 *   ARTIST    — Shuffle All, Play Next, Add to Playlist, Pin, Share,
 *               Info, Hide
 *   FOLDER    — Play All, Shuffle, Add to Queue, Pin, Share, Info,
 *               Rename, Delete
 *   PLAYLIST  — Play, Shuffle, Add to Queue, Rename, Export, Share,
 *               Info, Delete
 *   COLLECTION — Play All, Shuffle, Add to Queue, Pin, Configure,
 *               Refresh, Info, Hide
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering — ContextMenuRuntime produces
 *      data, not Compose UI. Icon references are *semantic* (enum),
 *      not Painter / ImageVector references.
 * #78  Interaction Creates Intent, Never Action — selecting a menu
 *      item produces a [ContextAction] (defined in next file). The
 *      Reducer decides whether to execute it.
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose. No Android framework. No Coroutine. No Flow. No
 * ViewModel. No `var`. No side effects. All functions are pure.
 *
 * © RIKO Experience OS — Interaction Runtime
 */

package com.rikodev.music.android.presentation.library.interaction

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 1 — MENU ITEM ID + ICON
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Stable identifier for a context menu item. Used to route the
 * selected item back to a [ContextAction] (next file).
 *
 * The id is a string (not an enum) because menus are extensible —
 * future collections may add custom menu items without modifying
 * this file.
 */
@JvmInline
value class ContextMenuItemId(val value: String)

/**
 * Semantic icon identifier. The UI layer maps this to a real icon
 * (Painter / ImageVector). The Runtime never references UI icons
 * directly (Rule #58).
 *
 * Adding a new icon requires:
 *   1. Adding a variant here.
 *   2. Updating the UI layer's icon mapping.
 */
enum class ContextMenuIcon {
    PLAY,
    SHUFFLE,
    ADD_TO_QUEUE,
    ADD_TO_PLAYLIST,
    FAVORITE,
    UNFAVORITE,
    PIN,
    UNPIN,
    SHARE,
    INFO,
    HIDE,
    SHOW,
    RENAME,
    DELETE,
    EXPORT,
    CONFIGURE,
    REFRESH,
    PLAY_NEXT,
    ARTWORK_PLACEHOLDER,
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — MENU ITEM + SECTION + SPEC
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A single item in a context menu.
 *
 * @property id             Stable id (for routing to ContextAction).
 * @property title          Human-readable label.
 * @property subtitle       Optional secondary line (e.g. "12 songs").
 * @property icon           Semantic icon reference.
 * @property isDestructive  True if the action is destructive (Delete,
 *                           Hide). UI renders these in red.
 * @property isEnabled      True if the action is currently available.
 *                           False if some precondition is not met.
 * @property requiresConfirmation  True if the action should prompt
 *                           for confirmation before executing.
 *                           (e.g. Delete requires confirmation.)
 * @property badge          Optional badge text (e.g. "NEW", "BETA").
 */
data class ContextMenuItem(
    val id: ContextMenuItemId,
    val title: String,
    val subtitle: String?,
    val icon: ContextMenuIcon,
    val isDestructive: Boolean,
    val isEnabled: Boolean,
    val requiresConfirmation: Boolean,
    val badge: String?,
) {
    companion object {
        /**
         * Build a standard (non-destructive) menu item.
         */
        fun standard(
            id: String,
            title: String,
            icon: ContextMenuIcon,
            subtitle: String? = null,
            isEnabled: Boolean = true,
            badge: String? = null,
        ): ContextMenuItem = ContextMenuItem(
            id = ContextMenuItemId(id),
            title = title,
            subtitle = subtitle,
            icon = icon,
            isDestructive = false,
            isEnabled = isEnabled,
            requiresConfirmation = false,
            badge = badge,
        )

        /**
         * Build a destructive menu item (Delete, Hide).
         */
        fun destructive(
            id: String,
            title: String,
            icon: ContextMenuIcon,
            subtitle: String? = null,
            isEnabled: Boolean = true,
            requiresConfirmation: Boolean = true,
        ): ContextMenuItem = ContextMenuItem(
            id = ContextMenuItemId(id),
            title = title,
            subtitle = subtitle,
            icon = icon,
            isDestructive = true,
            isEnabled = isEnabled,
            requiresConfirmation = requiresConfirmation,
            badge = null,
        )

        /**
         * Build a toggle menu item (Favorite / Unfavorite, Pin / Unpin).
         */
        fun toggle(
            id: String,
            title: String,
            icon: ContextMenuIcon,
            isOn: Boolean,
            subtitle: String? = null,
        ): ContextMenuItem = ContextMenuItem(
            id = ContextMenuItemId(id),
            title = title,
            subtitle = subtitle,
            icon = icon,
            isDestructive = false,
            isEnabled = true,
            requiresConfirmation = false,
            badge = if (isOn) "ON" else null,
        )
    }
}

/**
 * A section of related items in a context menu.
 *
 * @property title     Optional section header (e.g. "Playback",
 *                      "Organize"). Null = no header.
 * @property items     Ordered list of items in this section.
 */
data class ContextMenuSection(
    val title: String?,
    val items: List<ContextMenuItem>,
) {
    companion object {
        fun unnamed(items: List<ContextMenuItem>): ContextMenuSection =
            ContextMenuSection(title = null, items = items)

        fun titled(title: String, items: List<ContextMenuItem>): ContextMenuSection =
            ContextMenuSection(title = title, items = items)
    }
}

/**
 * Header for a context menu. Shows the target's identity.
 *
 * @property title        Target title (e.g. song name, album name).
 * @property subtitle     Target subtitle (e.g. artist, song count).
 * @property artworkKey   Stable key for artwork lookup. UI resolves
 *                         this to a real bitmap.
 */
data class ContextMenuHeader(
    val title: String,
    val subtitle: String?,
    val artworkKey: String?,
)

/**
 * The full context menu specification for a target.
 *
 * @property targetId       Stable id of the target.
 * @property targetType     Type of the target.
 * @property targetItemId   Domain id of the target (Song.id, etc.).
 * @property header         Optional header.
 * @property sections       Ordered list of sections.
 */
data class ContextMenuSpec(
    val targetId: String,
    val targetType: GestureTargetType,
    val targetItemId: Long,
    val header: ContextMenuHeader?,
    val sections: List<ContextMenuSection>,
) {
    /**
     * All items across all sections, flattened.
     */
    val allItems: List<ContextMenuItem> get() = sections.flatMap { it.items }

    /**
     * Find an item by id.
     */
    fun itemFor(id: ContextMenuItemId): ContextMenuItem? =
        allItems.firstOrNull { it.id == id }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — MENU STATE CONTEXT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * State used by the ContextMenuRuntime to decide which items appear
 * and whether they are enabled.
 *
 * Rule #80: this is a *serializable snapshot*, not a UI reference.
 * The host (B5.5) builds this from current state and passes it in.
 *
 * @property isFavorite          Is the target currently favorited?
 * @property isPinned            Is the target currently pinned?
 * @property isHidden            Is the target currently hidden?
 * @property isPlaying           Is the target currently playing?
 * @property isInQueue           Is the target in the play queue?
 * @property canBeDeleted        True if the user has permission to delete.
 * @property canBeRenamed        True if the user has permission to rename.
 * @property canBeShared         True if sharing is available.
 * @property playlistCount       Number of playlists the user has
 *                                (affects "Add to Playlist" submenu).
 * @property targetTitleOverride  Override the title (for "Pin X" → "Pin 'Song'").
 * @property targetSubtitleOverride  Override the subtitle.
 */
data class ContextMenuStateContext(
    val isFavorite: Boolean,
    val isPinned: Boolean,
    val isHidden: Boolean,
    val isPlaying: Boolean,
    val isInQueue: Boolean,
    val canBeDeleted: Boolean,
    val canBeRenamed: Boolean,
    val canBeShared: Boolean,
    val playlistCount: Int,
    val targetTitleOverride: String?,
    val targetSubtitleOverride: String?,
) {
    companion object {
        val DEFAULT = ContextMenuStateContext(
            isFavorite = false,
            isPinned = false,
            isHidden = false,
            isPlaying = false,
            isInQueue = false,
            canBeDeleted = true,
            canBeRenamed = false,
            canBeShared = true,
            playlistCount = 0,
            targetTitleOverride = null,
            targetSubtitleOverride = null,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — THE RUNTIME (PURE)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Context Menu Runtime — pure functions for generating context
 * menu specs.
 *
 * Every function takes a target + state context and returns a
 * [ContextMenuSpec]. No side effects.
 */
object ContextMenuRuntime {

    /**
     * Generate a context menu for any target.
     *
     * Dispatches to per-type generators.
     */
    fun menuFor(
        targetId: String,
        targetType: GestureTargetType,
        targetItemId: Long,
        targetTitle: String,
        targetSubtitle: String?,
        targetArtworkKey: String?,
        stateContext: ContextMenuStateContext,
    ): ContextMenuSpec {
        val header = ContextMenuHeader(
            title = stateContext.targetTitleOverride ?: targetTitle,
            subtitle = stateContext.targetSubtitleOverride ?: targetSubtitle,
            artworkKey = targetArtworkKey,
        )

        val sections = when (targetType) {
            GestureTargetType.SONG       -> songMenu(stateContext)
            GestureTargetType.ALBUM      -> albumMenu(stateContext)
            GestureTargetType.ARTIST     -> artistMenu(stateContext)
            GestureTargetType.FOLDER     -> folderMenu(stateContext)
            GestureTargetType.PLAYLIST   -> playlistMenu(stateContext)
            GestureTargetType.COLLECTION -> collectionMenu(stateContext)
            else -> genericMenu(stateContext)
        }

        return ContextMenuSpec(
            targetId = targetId,
            targetType = targetType,
            targetItemId = targetItemId,
            header = header,
            sections = sections,
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // SONG
    // ─────────────────────────────────────────────────────────────────

    private fun songMenu(ctx: ContextMenuStateContext): List<ContextMenuSection> {
        val playback = ContextMenuSection.titled(
            title = "Playback",
            items = listOf(
                ContextMenuItem.standard(
                    id = "song.play",
                    title = if (ctx.isPlaying) "Now Playing" else "Play",
                    icon = ContextMenuIcon.PLAY,
                    isEnabled = !ctx.isPlaying,
                ),
                ContextMenuItem.standard(
                    id = "song.play_next",
                    title = "Play Next",
                    icon = ContextMenuIcon.PLAY_NEXT,
                    isEnabled = !ctx.isInQueue,
                ),
                ContextMenuItem.standard(
                    id = "song.add_to_queue",
                    title = "Add to Queue",
                    icon = ContextMenuIcon.ADD_TO_QUEUE,
                    isEnabled = !ctx.isInQueue,
                ),
            ),
        )

        val organize = ContextMenuSection.titled(
            title = "Organize",
            items = listOf(
                ContextMenuItem.toggle(
                    id = "song.favorite",
                    title = if (ctx.isFavorite) "Unfavorite" else "Favorite",
                    icon = if (ctx.isFavorite) ContextMenuIcon.UNFAVORITE else ContextMenuIcon.FAVORITE,
                    isOn = ctx.isFavorite,
                ),
                ContextMenuItem.toggle(
                    id = "song.pin",
                    title = if (ctx.isPinned) "Unpin" else "Pin",
                    icon = if (ctx.isPinned) ContextMenuIcon.UNPIN else ContextMenuIcon.PIN,
                    isOn = ctx.isPinned,
                ),
                ContextMenuItem.standard(
                    id = "song.add_to_playlist",
                    title = "Add to Playlist",
                    icon = ContextMenuIcon.ADD_TO_PLAYLIST,
                    subtitle = if (ctx.playlistCount > 0) "${ctx.playlistCount} playlists" else null,
                ),
            ),
        )

        val share = ContextMenuSection.titled(
            title = "Share",
            items = listOf(
                ContextMenuItem.standard(
                    id = "song.share",
                    title = "Share",
                    icon = ContextMenuIcon.SHARE,
                    isEnabled = ctx.canBeShared,
                ),
                ContextMenuItem.standard(
                    id = "song.info",
                    title = "Info",
                    icon = ContextMenuIcon.INFO,
                ),
            ),
        )

        val danger = ContextMenuSection.titled(
            title = "Danger",
            items = listOf(
                ContextMenuItem.toggle(
                    id = "song.hide",
                    title = if (ctx.isHidden) "Unhide" else "Hide",
                    icon = if (ctx.isHidden) ContextMenuIcon.SHOW else ContextMenuIcon.HIDE,
                    isOn = ctx.isHidden,
                ),
                ContextMenuItem.destructive(
                    id = "song.delete",
                    title = "Delete",
                    icon = ContextMenuIcon.DELETE,
                    isEnabled = ctx.canBeDeleted,
                    requiresConfirmation = true,
                ),
            ),
        )

        return listOf(playback, organize, share, danger)
    }

    // ─────────────────────────────────────────────────────────────────
    // ALBUM
    // ─────────────────────────────────────────────────────────────────

    private fun albumMenu(ctx: ContextMenuStateContext): List<ContextMenuSection> {
        val playback = ContextMenuSection.titled(
            title = "Playback",
            items = listOf(
                ContextMenuItem.standard(
                    id = "album.play_all",
                    title = "Play All",
                    icon = ContextMenuIcon.PLAY,
                    isEnabled = !ctx.isPlaying,
                ),
                ContextMenuItem.standard(
                    id = "album.shuffle",
                    title = "Shuffle",
                    icon = ContextMenuIcon.SHUFFLE,
                ),
                ContextMenuItem.standard(
                    id = "album.add_to_queue",
                    title = "Add to Queue",
                    icon = ContextMenuIcon.ADD_TO_QUEUE,
                    isEnabled = !ctx.isInQueue,
                ),
            ),
        )

        val organize = ContextMenuSection.titled(
            title = "Organize",
            items = listOf(
                ContextMenuItem.toggle(
                    id = "album.pin",
                    title = if (ctx.isPinned) "Unpin" else "Pin",
                    icon = if (ctx.isPinned) ContextMenuIcon.UNPIN else ContextMenuIcon.PIN,
                    isOn = ctx.isPinned,
                ),
                ContextMenuItem.standard(
                    id = "album.add_to_playlist",
                    title = "Add to Playlist",
                    icon = ContextMenuIcon.ADD_TO_PLAYLIST,
                ),
            ),
        )

        val share = ContextMenuSection.titled(
            title = "Share",
            items = listOf(
                ContextMenuItem.standard(
                    id = "album.share",
                    title = "Share",
                    icon = ContextMenuIcon.SHARE,
                    isEnabled = ctx.canBeShared,
                ),
                ContextMenuItem.standard(
                    id = "album.info",
                    title = "Info",
                    icon = ContextMenuIcon.INFO,
                ),
            ),
        )

        val danger = ContextMenuSection.titled(
            title = "Danger",
            items = listOf(
                ContextMenuItem.destructive(
                    id = "album.delete",
                    title = "Delete Album",
                    icon = ContextMenuIcon.DELETE,
                    isEnabled = ctx.canBeDeleted,
                    requiresConfirmation = true,
                ),
            ),
        )

        return listOf(playback, organize, share, danger)
    }

    // ─────────────────────────────────────────────────────────────────
    // ARTIST
    // ─────────────────────────────────────────────────────────────────

    private fun artistMenu(ctx: ContextMenuStateContext): List<ContextMenuSection> {
        val playback = ContextMenuSection.titled(
            title = "Playback",
            items = listOf(
                ContextMenuItem.standard(
                    id = "artist.shuffle_all",
                    title = "Shuffle All",
                    icon = ContextMenuIcon.SHUFFLE,
                ),
                ContextMenuItem.standard(
                    id = "artist.play_next",
                    title = "Play Next",
                    icon = ContextMenuIcon.PLAY_NEXT,
                ),
            ),
        )

        val organize = ContextMenuSection.titled(
            title = "Organize",
            items = listOf(
                ContextMenuItem.toggle(
                    id = "artist.pin",
                    title = if (ctx.isPinned) "Unpin" else "Pin",
                    icon = if (ctx.isPinned) ContextMenuIcon.UNPIN else ContextMenuIcon.PIN,
                    isOn = ctx.isPinned,
                ),
                ContextMenuItem.standard(
                    id = "artist.add_to_playlist",
                    title = "Add to Playlist",
                    icon = ContextMenuIcon.ADD_TO_PLAYLIST,
                ),
            ),
        )

        val share = ContextMenuSection.titled(
            title = "Share",
            items = listOf(
                ContextMenuItem.standard(
                    id = "artist.share",
                    title = "Share",
                    icon = ContextMenuIcon.SHARE,
                    isEnabled = ctx.canBeShared,
                ),
                ContextMenuItem.standard(
                    id = "artist.info",
                    title = "Info",
                    icon = ContextMenuIcon.INFO,
                ),
            ),
        )

        val danger = ContextMenuSection.titled(
            title = "Visibility",
            items = listOf(
                ContextMenuItem.toggle(
                    id = "artist.hide",
                    title = if (ctx.isHidden) "Unhide" else "Hide",
                    icon = if (ctx.isHidden) ContextMenuIcon.SHOW else ContextMenuIcon.HIDE,
                    isOn = ctx.isHidden,
                ),
            ),
        )

        return listOf(playback, organize, share, danger)
    }

    // ─────────────────────────────────────────────────────────────────
    // FOLDER
    // ─────────────────────────────────────────────────────────────────

    private fun folderMenu(ctx: ContextMenuStateContext): List<ContextMenuSection> {
        val playback = ContextMenuSection.titled(
            title = "Playback",
            items = listOf(
                ContextMenuItem.standard(
                    id = "folder.play_all",
                    title = "Play All",
                    icon = ContextMenuIcon.PLAY,
                    isEnabled = !ctx.isPlaying,
                ),
                ContextMenuItem.standard(
                    id = "folder.shuffle",
                    title = "Shuffle",
                    icon = ContextMenuIcon.SHUFFLE,
                ),
                ContextMenuItem.standard(
                    id = "folder.add_to_queue",
                    title = "Add to Queue",
                    icon = ContextMenuIcon.ADD_TO_QUEUE,
                ),
            ),
        )

        val organize = ContextMenuSection.titled(
            title = "Organize",
            items = listOf(
                ContextMenuItem.toggle(
                    id = "folder.pin",
                    title = if (ctx.isPinned) "Unpin" else "Pin",
                    icon = if (ctx.isPinned) ContextMenuIcon.UNPIN else ContextMenuIcon.PIN,
                    isOn = ctx.isPinned,
                ),
            ),
        )

        val share = ContextMenuSection.titled(
            title = "Share",
            items = listOf(
                ContextMenuItem.standard(
                    id = "folder.share",
                    title = "Share",
                    icon = ContextMenuIcon.SHARE,
                    isEnabled = ctx.canBeShared,
                ),
                ContextMenuItem.standard(
                    id = "folder.info",
                    title = "Info",
                    icon = ContextMenuIcon.INFO,
                ),
            ),
        )

        val danger = ContextMenuSection.titled(
            title = "Danger",
            items = listOf(
                ContextMenuItem.standard(
                    id = "folder.rename",
                    title = "Rename",
                    icon = ContextMenuIcon.RENAME,
                    isEnabled = ctx.canBeRenamed,
                ),
                ContextMenuItem.destructive(
                    id = "folder.delete",
                    title = "Delete Folder",
                    icon = ContextMenuIcon.DELETE,
                    isEnabled = ctx.canBeDeleted,
                    requiresConfirmation = true,
                ),
            ),
        )

        return listOf(playback, organize, share, danger)
    }

    // ─────────────────────────────────────────────────────────────────
    // PLAYLIST
    // ─────────────────────────────────────────────────────────────────

    private fun playlistMenu(ctx: ContextMenuStateContext): List<ContextMenuSection> {
        val playback = ContextMenuSection.titled(
            title = "Playback",
            items = listOf(
                ContextMenuItem.standard(
                    id = "playlist.play",
                    title = "Play",
                    icon = ContextMenuIcon.PLAY,
                    isEnabled = !ctx.isPlaying,
                ),
                ContextMenuItem.standard(
                    id = "playlist.shuffle",
                    title = "Shuffle",
                    icon = ContextMenuIcon.SHUFFLE,
                ),
                ContextMenuItem.standard(
                    id = "playlist.add_to_queue",
                    title = "Add to Queue",
                    icon = ContextMenuIcon.ADD_TO_QUEUE,
                ),
            ),
        )

        val organize = ContextMenuSection.titled(
            title = "Organize",
            items = listOf(
                ContextMenuItem.toggle(
                    id = "playlist.pin",
                    title = if (ctx.isPinned) "Unpin" else "Pin",
                    icon = if (ctx.isPinned) ContextMenuIcon.UNPIN else ContextMenuIcon.PIN,
                    isOn = ctx.isPinned,
                ),
                ContextMenuItem.standard(
                    id = "playlist.rename",
                    title = "Rename",
                    icon = ContextMenuIcon.RENAME,
                    isEnabled = ctx.canBeRenamed,
                ),
                ContextMenuItem.standard(
                    id = "playlist.export",
                    title = "Export",
                    icon = ContextMenuIcon.EXPORT,
                ),
            ),
        )

        val share = ContextMenuSection.titled(
            title = "Share",
            items = listOf(
                ContextMenuItem.standard(
                    id = "playlist.share",
                    title = "Share",
                    icon = ContextMenuIcon.SHARE,
                    isEnabled = ctx.canBeShared,
                ),
                ContextMenuItem.standard(
                    id = "playlist.info",
                    title = "Info",
                    icon = ContextMenuIcon.INFO,
                ),
            ),
        )

        val danger = ContextMenuSection.titled(
            title = "Danger",
            items = listOf(
                ContextMenuItem.destructive(
                    id = "playlist.delete",
                    title = "Delete Playlist",
                    icon = ContextMenuIcon.DELETE,
                    isEnabled = ctx.canBeDeleted,
                    requiresConfirmation = true,
                ),
            ),
        )

        return listOf(playback, organize, share, danger)
    }

    // ─────────────────────────────────────────────────────────────────
    // COLLECTION (smart collection)
    // ─────────────────────────────────────────────────────────────────

    private fun collectionMenu(ctx: ContextMenuStateContext): List<ContextMenuSection> {
        val playback = ContextMenuSection.titled(
            title = "Playback",
            items = listOf(
                ContextMenuItem.standard(
                    id = "collection.play_all",
                    title = "Play All",
                    icon = ContextMenuIcon.PLAY,
                    isEnabled = !ctx.isPlaying,
                ),
                ContextMenuItem.standard(
                    id = "collection.shuffle",
                    title = "Shuffle",
                    icon = ContextMenuIcon.SHUFFLE,
                ),
                ContextMenuItem.standard(
                    id = "collection.add_to_queue",
                    title = "Add to Queue",
                    icon = ContextMenuIcon.ADD_TO_QUEUE,
                ),
            ),
        )

        val organize = ContextMenuSection.titled(
            title = "Organize",
            items = listOf(
                ContextMenuItem.toggle(
                    id = "collection.pin",
                    title = if (ctx.isPinned) "Unpin" else "Pin",
                    icon = if (ctx.isPinned) ContextMenuIcon.UNPIN else ContextMenuIcon.PIN,
                    isOn = ctx.isPinned,
                ),
                ContextMenuItem.standard(
                    id = "collection.configure",
                    title = "Configure",
                    icon = ContextMenuIcon.CONFIGURE,
                    badge = "BETA",
                ),
                ContextMenuItem.standard(
                    id = "collection.refresh",
                    title = "Refresh",
                    icon = ContextMenuIcon.REFRESH,
                ),
            ),
        )

        val info = ContextMenuSection.titled(
            title = "Info",
            items = listOf(
                ContextMenuItem.standard(
                    id = "collection.info",
                    title = "Info",
                    icon = ContextMenuIcon.INFO,
                ),
                ContextMenuItem.toggle(
                    id = "collection.hide",
                    title = if (ctx.isHidden) "Unhide" else "Hide",
                    icon = if (ctx.isHidden) ContextMenuIcon.SHOW else ContextMenuIcon.HIDE,
                    isOn = ctx.isHidden,
                ),
            ),
        )

        return listOf(playback, organize, info)
    }

    // ─────────────────────────────────────────────────────────────────
    // GENERIC (fallback for unknown target types)
    // ─────────────────────────────────────────────────────────────────

    private fun genericMenu(ctx: ContextMenuStateContext): List<ContextMenuSection> {
        return listOf(
            ContextMenuSection.unnamed(
                items = listOf(
                    ContextMenuItem.standard(
                        id = "generic.info",
                        title = "Info",
                        icon = ContextMenuIcon.INFO,
                    ),
                    ContextMenuItem.standard(
                        id = "generic.share",
                        title = "Share",
                        icon = ContextMenuIcon.SHARE,
                        isEnabled = ctx.canBeShared,
                    ),
                ),
            ),
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // MENU REDUCTION HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Filter out disabled items from a menu spec. Used when the UI
     * wants to show only enabled items (e.g. compact menu mode).
     */
    fun filterDisabled(spec: ContextMenuSpec): ContextMenuSpec = spec.copy(
        sections = spec.sections.map { section ->
            section.copy(items = section.items.filter { it.isEnabled })
        }.filter { it.items.isNotEmpty() },
    )

    /**
     * Filter out non-destructive items, leaving only destructive ones.
     * Used for the "Danger" confirmation flow.
     */
    fun destructiveOnly(spec: ContextMenuSpec): ContextMenuSpec = spec.copy(
        sections = spec.sections.map { section ->
            section.copy(items = section.items.filter { it.isDestructive })
        }.filter { it.items.isNotEmpty() },
    )

    /**
     * Check if a menu spec has any destructive items.
     */
    fun hasDestructiveItems(spec: ContextMenuSpec): Boolean =
        spec.allItems.any { it.isDestructive }

    /**
     * Find the item in the spec that requires confirmation, if any.
     */
    fun itemsRequiringConfirmation(spec: ContextMenuSpec): List<ContextMenuItem> =
        spec.allItems.filter { it.requiresConfirmation }
}
