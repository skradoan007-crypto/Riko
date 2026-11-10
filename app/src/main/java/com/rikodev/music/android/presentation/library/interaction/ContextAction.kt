/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 7 / 15 — ContextAction.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Define the [ContextAction] sealed class — every action a context
 * menu can request. Pure data; no execution.
 *
 * When the user taps a context menu item, the Runtime produces a
 * `ContextAction`. The Reducer receives this and decides:
 *   - Whether to execute it immediately
 *   - Whether to ask for confirmation first (Rule: destructive actions)
 *   - Whether to dispatch a side-effect request to the Coordinator
 *
 * 8 CORE ACTIONS (locked)
 * -----------------------
 *   Pin, Rename, Delete, Hide, Queue, Favorite, Share, Info
 *
 * Plus per-target-type specializations (Play, PlayAll, Shuffle,
 * PlayNext, AddToPlaylist, Configure, Refresh, Export).
 *
 * RULES ENFORCED HERE
 * -------------------
 * #48  Actions Are Nouns — each action is a noun phrase ("Pin",
 *      "Delete", "Add to Queue"), not a verb command.
 * #78  Interaction Creates Intent, Never Action — these ContextActions
 *      are produced *by the Reducer* after consuming an
 *      InteractionIntent, never directly by a gesture.
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose. No Android framework. No Coroutine. No Flow. No
 * ViewModel. No `var`. No side effects. All actions are pure data.
 *
 * © RIKO Experience OS — Interaction Runtime
 */

package com.rikodev.music.android.presentation.library.interaction

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 1 — CONTEXT ACTION (SEALED)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Every action a context menu can request.
 *
 * Actions are pure data. They describe *what should happen*, not
 * *how to execute it*. The Coordinator (B5.5 Host) is responsible
 * for translating actions into actual side effects (Repository calls,
 * navigation, haptics, etc.).
 *
 * Every action carries:
 *   - `targetType` — what kind of target (Song, Album, etc.)
 *   - `targetItemId` — domain id of the target
 *   - `requiresConfirmation` — true if the Reducer should prompt the
 *     user before executing (typically destructive actions)
 *   - `isDestructive` — true if the action destroys data
 */
sealed class ContextAction {

    abstract val targetType: GestureTargetType
    abstract val targetItemId: Long
    abstract val requiresConfirmation: Boolean
    abstract val isDestructive: Boolean

    // ── 8 CORE ACTIONS ───────────────────────────────────────────────

    /**
     * Pin a target. Pinned items appear at the top of their section
     * and survive selection-clear operations.
     */
    data class Pin(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    /**
     * Unpin a target.
     */
    data class Unpin(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    /**
     * Rename a target. Only valid for Folders, Playlists. Opens a
     * rename dialog — the actual rename is a separate action after
     * the user types a new name.
     */
    data class Rename(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
        val currentName: String,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    /**
     * Delete a target. Destructive — always requires confirmation.
     */
    data class Delete(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
        val targetDisplayName: String,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = true
        override val isDestructive: Boolean = true
    }

    /**
     * Hide a target. Non-destructive — target remains in storage but
     * is excluded from default views.
     */
    data class Hide(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    /**
     * Unhide a target. Restores visibility in default views.
     */
    data class Unhide(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    /**
     * Add a target to the play queue.
     */
    data class AddToQueue(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
        val playNext: Boolean = false,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    /**
     * Toggle favorite on a target.
     */
    data class ToggleFavorite(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
        val isCurrentlyFavorite: Boolean,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    /**
     * Share a target. Triggers the system share sheet.
     */
    data class Share(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    /**
     * Show info about a target. Opens the info sheet (metadata,
     * file path, lyrics availability, etc.).
     */
    data class ShowInfo(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    // ── PLAYBACK ACTIONS ─────────────────────────────────────────────

    /**
     * Play a single target (typically a Song).
     */
    data class Play(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
        val contextPlaylistId: Long? = null,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    /**
     * Play all items in a target (Album, Folder, Playlist, Collection).
     */
    data class PlayAll(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
        val shuffle: Boolean = false,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    /**
     * Shuffle all items in a target.
     */
    data class Shuffle(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    /**
     * Play a target next (insert at the front of the queue).
     */
    data class PlayNext(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    // ── ORGANIZE ACTIONS ─────────────────────────────────────────────

    /**
     * Add a target to a specific playlist. Opens playlist picker
     * if `playlistId` is null; adds directly if `playlistId` is set.
     */
    data class AddToPlaylist(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
        val playlistId: Long?,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    /**
     * Export a target (typically a Playlist to M3U file).
     */
    data class Export(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    /**
     * Configure a smart collection (edit its rules).
     */
    data class Configure(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }

    /**
     * Refresh a target's content (re-fetch from Repository).
     */
    data class Refresh(
        override val targetType: GestureTargetType,
        override val targetItemId: Long,
    ) : ContextAction() {
        override val requiresConfirmation: Boolean = false
        override val isDestructive: Boolean = false
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — CONTEXT MENU ITEM ID → CONTEXT ACTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Map a [ContextMenuItemId] (from the menu spec) to a [ContextAction].
 *
 * This is the bridge between "what the user tapped in the menu" and
 * "what action the Reducer should consider executing".
 *
 * Pure function: same inputs → same outputs.
 *
 * If the menu item id is not recognized for the target type, returns
 * null (the Reducer should ignore the tap and log a warning).
 */
fun contextActionFor(
    menuItemId: ContextMenuItemId,
    targetType: GestureTargetType,
    targetItemId: Long,
    stateContext: ContextMenuStateContext,
    targetDisplayName: String,
): ContextAction? {
    val id = menuItemId.value
    return when {
        // Playback
        id.startsWith("song.play") && id == "song.play" ->
            ContextAction.Play(targetType, targetItemId, contextPlaylistId = null)
        id == "song.play_next" || id == "artist.play_next" ->
            ContextAction.PlayNext(targetType, targetItemId)
        id.endsWith(".add_to_queue") ->
            ContextAction.AddToQueue(targetType, targetItemId, playNext = false)
        id == "album.play_all" || id == "folder.play_all" || id == "collection.play_all" || id == "playlist.play" ->
            ContextAction.PlayAll(targetType, targetItemId, shuffle = false)
        id.endsWith(".shuffle") || id == "album.shuffle" || id == "artist.shuffle_all" ->
            ContextAction.Shuffle(targetType, targetItemId)

        // Favorite (toggle)
        id == "song.favorite" ->
            ContextAction.ToggleFavorite(targetType, targetItemId, stateContext.isFavorite)

        // Pin (toggle)
        id.endsWith(".pin") ->
            if (stateContext.isPinned) {
                ContextAction.Unpin(targetType, targetItemId)
            } else {
                ContextAction.Pin(targetType, targetItemId)
            }

        // Hide (toggle)
        id.endsWith(".hide") ->
            if (stateContext.isHidden) {
                ContextAction.Unhide(targetType, targetItemId)
            } else {
                ContextAction.Hide(targetType, targetItemId)
            }

        // Add to Playlist
        id.endsWith(".add_to_playlist") ->
            ContextAction.AddToPlaylist(targetType, targetItemId, playlistId = null)

        // Share
        id.endsWith(".share") ->
            ContextAction.Share(targetType, targetItemId)

        // Info
        id.endsWith(".info") ->
            ContextAction.ShowInfo(targetType, targetItemId)

        // Rename
        id == "folder.rename" || id == "playlist.rename" ->
            ContextAction.Rename(targetType, targetItemId, currentName = targetDisplayName)

        // Delete
        id.endsWith(".delete") ->
            ContextAction.Delete(targetType, targetItemId, targetDisplayName = targetDisplayName)

        // Export
        id == "playlist.export" ->
            ContextAction.Export(targetType, targetItemId)

        // Configure
        id == "collection.configure" ->
            ContextAction.Configure(targetType, targetItemId)

        // Refresh
        id == "collection.refresh" ->
            ContextAction.Refresh(targetType, targetItemId)

        // Unknown
        else -> null
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — CONFIRMATION DIALOG
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A confirmation dialog spec for destructive actions.
 *
 * Produced by the Reducer when a destructive [ContextAction] is
 * requested. The UI layer renders this dialog; the user's response
 * triggers either confirmation (proceed with action) or cancellation
 * (discard action).
 *
 * @property action              The action being confirmed.
 * @property title               Dialog title (e.g. "Delete 'Song'?").
 * @property message             Dialog body (e.g. "This cannot be undone.").
 * @property confirmButtonLabel  Label for the confirm button (e.g. "Delete").
 * @property cancelButtonLabel   Label for the cancel button (e.g. "Cancel").
 */
data class ConfirmationDialogSpec(
    val action: ContextAction,
    val title: String,
    val message: String,
    val confirmButtonLabel: String,
    val cancelButtonLabel: String,
)

/**
 * Build a confirmation dialog for a destructive action.
 *
 * Returns null if the action does not require confirmation.
 */
fun confirmationDialogFor(
    action: ContextAction,
    targetDisplayName: String,
): ConfirmationDialogSpec? {
    if (!action.requiresConfirmation || !action.isDestructive) return null

    return when (action) {
        is ContextAction.Delete -> ConfirmationDialogSpec(
            action = action,
            title = "Delete \"$targetDisplayName\"?",
            message = when (action.targetType) {
                GestureTargetType.SONG ->
                    "This song will be removed from your library and from disk. " +
                    "This cannot be undone."
                GestureTargetType.ALBUM ->
                    "This will remove the album from your library. " +
                    "Songs will remain on disk unless individually deleted."
                GestureTargetType.FOLDER ->
                    "This will remove the folder from your library. " +
                    "Files on disk will not be affected."
                GestureTargetType.PLAYLIST ->
                    "This playlist will be permanently deleted. " +
                    "Songs in the playlist will remain in your library."
                else -> "This action cannot be undone."
            },
            confirmButtonLabel = "Delete",
            cancelButtonLabel = "Cancel",
        )

        else -> null
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — CONTEXT ACTION EVENT (Rule #81 replay support)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A recorded context action for replay (Rule #81).
 *
 * Wraps a [ContextAction] with metadata about when it was triggered,
 * what gesture produced it, and whether it was confirmed by the user.
 */
data class RecordedContextAction(
    val action: ContextAction,
    val recordedAtMs: Long,
    val sourceGestureId: String?,
    val wasConfirmed: Boolean,
    val recorderSessionId: String,
)

/**
 * Result of attempting to execute a context action.
 *
 * - Executed: the action was executed successfully.
 * - RequiresConfirmation: the action needs user confirmation first.
 *   The Reducer should emit a [ConfirmationDialogSpec].
 * - Rejected: the action was rejected (e.g. permission denied).
 * - Failed: the action failed to execute.
 */
sealed class ContextActionResult {
    data class Executed(
        val action: ContextAction,
        val atMs: Long,
    ) : ContextActionResult()

    data class RequiresConfirmation(
        val action: ContextAction,
        val dialogSpec: ConfirmationDialogSpec,
    ) : ContextActionResult()

    data class Rejected(
        val action: ContextAction,
        val reason: String,
    ) : ContextActionResult()

    data class Failed(
        val action: ContextAction,
        val errorMessage: String,
    ) : ContextActionResult()
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 5 — INTENT (Rule #78)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * An Intent emitted when the user taps a context menu item.
 *
 * Distinct from [ContextAction] — this is the user's *intent* to
 * trigger an action. The Reducer decides whether to:
 *   - Execute the action directly (non-destructive)
 *   - Emit a ConfirmationDialogSpec (destructive)
 *   - Reject the action (permission / state precondition)
 *
 * @property menuItemId    The id of the tapped menu item.
 * @property targetType     Target type (Song, Album, etc.).
 * @property targetItemId   Domain id of the target.
 * @property targetDisplayName  Display name for confirmation dialogs.
 * @property stateContext   Snapshot of target state at tap time.
 * @property sourceGestureId  Links back to the gesture (Rule #81).
 */
data class ContextMenuItemTappedIntent(
    val menuItemId: ContextMenuItemId,
    val targetType: GestureTargetType,
    val targetItemId: Long,
    val targetDisplayName: String,
    val stateContext: ContextMenuStateContext,
    val sourceGestureId: String?,
)

/**
 * An Intent emitted when the user confirms a destructive action
 * in the confirmation dialog.
 */
data class ContextActionConfirmedIntent(
    val action: ContextAction,
    val sourceGestureId: String?,
)

/**
 * An Intent emitted when the user cancels a destructive action
 * in the confirmation dialog.
 */
data class ContextActionCancelledIntent(
    val action: ContextAction,
    val sourceGestureId: String?,
)
