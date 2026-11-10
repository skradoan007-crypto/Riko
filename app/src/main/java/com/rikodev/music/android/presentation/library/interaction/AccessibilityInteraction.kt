/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 12 / 15 — AccessibilityInteraction.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Define the accessibility interaction layer — keyboard, D-pad,
 * TalkBack, mouse, and controller support. Pure: same inputs →
 * same outputs, byte-identical.
 *
 * ACCESSIBILITY MODES
 * -------------------
 *   TOUCH       — default touch interaction (covered by LibraryGesture)
 *   TALKBACK    — screen reader active; gestures are doubled (explore
 *                 + activate)
 *   KEYBOARD    — physical keyboard attached; Tab / Arrow / Enter
 *   CONTROLLER  — D-pad / gamepad; directional navigation
 *   MOUSE       — mouse attached; hover + click
 *
 * Each mode has different gesture semantics:
 *
 *   - TOUCH      → Tap, LongPress, Swipe, Drag, Pinch, etc.
 *   - TALKBACK   → Double-tap-and-hold = LongPress; Double-tap = Tap
 *   - KEYBOARD   → Enter = Tap; Shift+F10 / Menu = Context; Arrows = MoveFocus
 *   - CONTROLLER → DPad-Center = Tap; DPad-Hold = LongPress; Arrows = MoveFocus
 *   - MOUSE      → Click = Tap; Right-click = Context; Hover = Hover
 *
 * DESIGN PHILOSOPHY
 * -----------------
 * RIKO is fully accessible by default. The same Intent vocabulary
 * (defined in [InteractionIntent]) is produced regardless of input
 * mode — the user just gets there via different physical gestures.
 *
 * This file translates accessibility-specific inputs into the same
 * [InteractionIntent] types. The Reducer does not need to know
 * whether a Tap came from a finger or an Enter key.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #78  Interaction Creates Intent, Never Action — accessibility
 *      inputs produce Intents, not Actions.
 * #80  Gesture Never Knows UI — AccessibilityInteraction does not
 *      reference any Composable.
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose. No Android framework (no KeyEvent, no MotionEvent).
 * No Coroutine. No Flow. No ViewModel. No `var`. No side effects.
 *
 * © RIKO Experience OS — Interaction Runtime
 */

package com.rikodev.music.android.presentation.library.interaction

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 1 — ACCESSIBILITY MODE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The active accessibility input mode.
 */
enum class AccessibilityMode(
    internal val displayName: String,
    internal val supportsHover: Boolean,
    internal val supportsMultiTouch: Boolean,
    internal val requiresFocusIndicator: Boolean,
) {
    TOUCH(
        displayName = "Touch",
        supportsHover = false,
        supportsMultiTouch = true,
        requiresFocusIndicator = false,
    ),

    TALKBACK(
        displayName = "TalkBack",
        supportsHover = true, // explore-by-touch
        supportsMultiTouch = false,
        requiresFocusIndicator = true,
    ),

    KEYBOARD(
        displayName = "Keyboard",
        supportsHover = false,
        supportsMultiTouch = false,
        requiresFocusIndicator = true,
    ),

    CONTROLLER(
        displayName = "Controller",
        supportsHover = false,
        supportsMultiTouch = false,
        requiresFocusIndicator = true,
    ),

    MOUSE(
        displayName = "Mouse",
        supportsHover = true,
        supportsMultiTouch = false,
        requiresFocusIndicator = false,
    );

    val isNonTouch: Boolean get() = this != TOUCH
    val requiresSpatialNavigation: Boolean
        get() = this == KEYBOARD || this == CONTROLLER
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — KEY / D-PAD INPUTS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Stable identifier for a key (platform-agnostic). The UI layer maps
 * physical keys (KEYCODE_ENTER, etc.) to these labels.
 */
enum class AccessibilityKey(
    internal val displayName: String,
) {
    ENTER("Enter"),
    TAB("Tab"),
    SHIFT_TAB("Shift+Tab"),
    ESCAPE("Escape"),
    SPACE("Space"),
    BACK("Back"),
    MENU("Menu"),
    CONTEXT("Context"),
    ARROW_UP("Arrow Up"),
    ARROW_DOWN("Arrow Down"),
    ARROW_LEFT("Arrow Left"),
    ARROW_RIGHT("Arrow Right"),
    HOME("Home"),
    END("End"),
    PAGE_UP("Page Up"),
    PAGE_DOWN("Page Down"),
    DPAD_CENTER("D-Pad Center"),
    DPAD_UP("D-Pad Up"),
    DPAD_DOWN("D-Pad Down"),
    DPAD_LEFT("D-Pad Left"),
    DPAD_RIGHT("D-Pad Right"),
    GAMEPAD_A("Gamepad A"),
    GAMEPAD_B("Gamepad B"),
    GAMEPAD_X("Gamepad X"),
    GAMEPAD_Y("Gamepad Y"),
    GAMEPAD_START("Gamepad Start"),
    GAMEPAD_SELECT("Gamepad Select"),
    VOLUME_UP("Volume Up"),
    VOLUME_DOWN("Volume Down"),
}

/**
 * An input event from a non-touch source (keyboard, D-pad, controller).
 *
 * @property key           Which key was pressed.
 * @property isLongPress   True if the key was held (long-press).
 * @property repeatCount   Number of times this key has repeated.
 * @property timestampMs   Wall-clock of the event.
 */
data class AccessibilityKeyEvent(
    val key: AccessibilityKey,
    val isLongPress: Boolean,
    val repeatCount: Int,
    val timestampMs: Long,
)

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — ACCESSIBILITY ACTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A semantic accessibility action — the user's intent at the
 * accessibility level, before it becomes an [InteractionIntent].
 *
 * Example: an ENTER key press becomes [AccessibilityAction.Activate],
 * which the translator converts to [InteractionIntent.CardTapped] /
 * [InteractionIntent.SongTapped] / [InteractionIntent.DockTapped]
 * based on the currently focused element.
 */
sealed class AccessibilityAction {
    /** Activate the focused element (Enter / DPad-Center / Gamepad-A). */
    data class Activate(
        val focusedTargetId: String?,
        val focusedTargetType: GestureTargetType?,
        val focusedTargetItemId: Long?,
    ) : AccessibilityAction()

    /** Long-press the focused element (Shift+F10 / Menu / DPad-Hold). */
    data class LongPress(
        val focusedTargetId: String?,
        val focusedTargetType: GestureTargetType?,
        val focusedTargetItemId: Long?,
    ) : AccessibilityAction()

    /** Move focus in a direction (Arrow / DPad). */
    data class MoveFocus(
        val direction: FocusDirection,
    ) : AccessibilityAction()

    /** Go back (Escape / Back / Gamepad-B). */
    data object Back : AccessibilityAction()

    /** Open the context menu (Menu key / Shift+F10). */
    data object OpenContextMenu : AccessibilityAction()

    /** Tab to next focusable element. */
    data object TabNext : AccessibilityAction()

    /** Tab to previous focusable element. */
    data object TabPrevious : AccessibilityAction()

    /** Scroll in a direction (Page Up / Page Down / Home / End). */
    data class Scroll(
        val direction: ScrollDirection,
        val magnitude: ScrollMagnitude,
    ) : AccessibilityAction()

    /** Dismiss the current overlay / sheet (Escape). */
    data object Dismiss : AccessibilityAction()

    /** Confirm a destructive action (Enter in a confirmation dialog). */
    data object Confirm : AccessibilityAction()

    /** Cancel a destructive action (Escape in a confirmation dialog). */
    data object Cancel : AccessibilityAction()
}

/**
 * Direction of focus movement.
 */
enum class FocusDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    NEXT, // Tab order
    PREVIOUS, // Reverse Tab order
    FIRST, // Home
    LAST, // End
}

/**
 * Direction of scroll.
 */
enum class ScrollDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    PAGE_UP,
    PAGE_DOWN,
    TO_START,
    TO_END,
}

/**
 * Magnitude of a scroll.
 */
enum class ScrollMagnitude {
    /** One item. */
    ITEM,

    /** One page (viewport height). */
    PAGE,

    /** All the way to the start / end. */
    EXTENT,
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — ACCESSIBILITY STATE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The accessibility interaction state.
 *
 * @property mode              Active input mode.
 * @property focusedTargetId   Currently focused element id. Null if
 *                              no element has focus.
 * @property focusedTargetType  Type of the focused element.
 * @property focusedTargetItemId  Domain id of the focused item.
 * @property focusOrder        Ordered list of focusable element ids
 *                              (for Tab navigation).
 * @property focusIndex        Current index in `focusOrder`. -1 if none.
 * @property announcement      Optional TalkBack announcement text.
 *                              Null = no announcement pending.
 */
data class AccessibilityState(
    val mode: AccessibilityMode,
    val focusedTargetId: String?,
    val focusedTargetType: GestureTargetType?,
    val focusedTargetItemId: Long?,
    val focusOrder: List<String>,
    val focusIndex: Int,
    val announcement: String?,
) {
    val hasFocus: Boolean get() = focusedTargetId != null && focusIndex >= 0

    companion object {
        val TOUCH_DEFAULT = AccessibilityState(
            mode = AccessibilityMode.TOUCH,
            focusedTargetId = null,
            focusedTargetType = null,
            focusedTargetItemId = null,
            focusOrder = emptyList(),
            focusIndex = -1,
            announcement = null,
        )

        val KEYBOARD_DEFAULT = AccessibilityState(
            mode = AccessibilityMode.KEYBOARD,
            focusedTargetId = null,
            focusedTargetType = null,
            focusedTargetItemId = null,
            focusOrder = emptyList(),
            focusIndex = -1,
            announcement = null,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 5 — ACCESSIBILITY RUNTIME (PURE)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Accessibility Interaction Runtime — pure functions for
 * accessibility state transitions and translations.
 */
object AccessibilityInteractionRuntime {

    /**
     * Translate a [AccessibilityKeyEvent] into an [AccessibilityAction].
     *
     * Returns null if the key is not recognized for the given mode.
     */
    fun translateKey(
        keyEvent: AccessibilityKeyEvent,
        mode: AccessibilityMode,
        state: AccessibilityState,
    ): AccessibilityAction? {
        return when (keyEvent.key) {
            AccessibilityKey.ENTER,
            AccessibilityKey.DPAD_CENTER,
            AccessibilityKey.GAMEPAD_A,
            AccessibilityKey.SPACE -> {
                if (keyEvent.isLongPress) {
                    AccessibilityAction.LongPress(
                        focusedTargetId = state.focusedTargetId,
                        focusedTargetType = state.focusedTargetType,
                        focusedTargetItemId = state.focusedTargetItemId,
                    )
                } else {
                    AccessibilityAction.Activate(
                        focusedTargetId = state.focusedTargetId,
                        focusedTargetType = state.focusedTargetType,
                        focusedTargetItemId = state.focusedTargetItemId,
                    )
                }
            }

            AccessibilityKey.TAB -> AccessibilityAction.TabNext
            AccessibilityKey.SHIFT_TAB -> AccessibilityAction.TabPrevious

            AccessibilityKey.ARROW_UP,
            AccessibilityKey.DPAD_UP -> AccessibilityAction.MoveFocus(FocusDirection.UP)

            AccessibilityKey.ARROW_DOWN,
            AccessibilityKey.DPAD_DOWN -> AccessibilityAction.MoveFocus(FocusDirection.DOWN)

            AccessibilityKey.ARROW_LEFT,
            AccessibilityKey.DPAD_LEFT -> AccessibilityAction.MoveFocus(FocusDirection.LEFT)

            AccessibilityKey.ARROW_RIGHT,
            AccessibilityKey.DPAD_RIGHT -> AccessibilityAction.MoveFocus(FocusDirection.RIGHT)

            AccessibilityKey.HOME -> AccessibilityAction.MoveFocus(FocusDirection.FIRST)
            AccessibilityKey.END -> AccessibilityAction.MoveFocus(FocusDirection.LAST)

            AccessibilityKey.PAGE_UP -> AccessibilityAction.Scroll(
                direction = ScrollDirection.UP,
                magnitude = ScrollMagnitude.PAGE,
            )

            AccessibilityKey.PAGE_DOWN -> AccessibilityAction.Scroll(
                direction = ScrollDirection.DOWN,
                magnitude = ScrollMagnitude.PAGE,
            )

            AccessibilityKey.ESCAPE,
            AccessibilityKey.BACK,
            AccessibilityKey.GAMEPAD_B -> {
                if (state.hasFocus) AccessibilityAction.Dismiss
                else AccessibilityAction.Back
            }

            AccessibilityKey.MENU,
            AccessibilityKey.CONTEXT,
            AccessibilityKey.GAMEPAD_Y,
            AccessibilityKey.GAMEPAD_START -> AccessibilityAction.OpenContextMenu

            else -> null
        }
    }

    /**
     * Move focus in a direction. Returns the new state.
     *
     * For Tab / Shift+Tab, moves through `focusOrder`.
     * For Arrow keys, the host (B5.5) must supply a spatial focus map
     * (which element is up/down/left/right of the current focus).
     */
    fun moveFocus(
        state: AccessibilityState,
        direction: FocusDirection,
        spatialMap: SpatialFocusMap?,
        nowMs: Long,
    ): AccessibilityState {
        if (state.focusOrder.isEmpty()) return state

        return when (direction) {
            FocusDirection.NEXT -> {
                val newIndex = (state.focusIndex + 1) % state.focusOrder.size
                state.copy(
                    focusIndex = newIndex,
                    focusedTargetId = state.focusOrder[newIndex],
                )
            }

            FocusDirection.PREVIOUS -> {
                val newIndex = if (state.focusIndex <= 0) {
                    state.focusOrder.lastIndex
                } else {
                    state.focusIndex - 1
                }
                state.copy(
                    focusIndex = newIndex,
                    focusedTargetId = state.focusOrder[newIndex],
                )
            }

            FocusDirection.FIRST -> state.copy(
                focusIndex = 0,
                focusedTargetId = state.focusOrder.firstOrNull(),
            )

            FocusDirection.LAST -> state.copy(
                focusIndex = state.focusOrder.lastIndex,
                focusedTargetId = state.focusOrder.lastOrNull(),
            )

            FocusDirection.UP, FocusDirection.DOWN,
            FocusDirection.LEFT, FocusDirection.RIGHT -> {
                val map = spatialMap ?: return state
                val currentId = state.focusedTargetId ?: return state
                val nextId = map.nextTarget(currentId, direction)
                    ?: return state
                val nextIndex = state.focusOrder.indexOf(nextId)
                if (nextIndex < 0) return state
                state.copy(
                    focusIndex = nextIndex,
                    focusedTargetId = nextId,
                )
            }
        }
    }

    /**
     * Set the focused element directly (e.g. when the user taps an
     * element with Touch while a keyboard is attached).
     */
    fun setFocus(
        state: AccessibilityState,
        targetId: String?,
        targetType: GestureTargetType?,
        targetItemId: Long?,
        nowMs: Long,
    ): AccessibilityState {
        val newIndex = targetId?.let { state.focusOrder.indexOf(it) } ?: -1
        return state.copy(
            focusedTargetId = targetId,
            focusedTargetType = targetType,
            focusedTargetItemId = targetItemId,
            focusIndex = newIndex,
        )
    }

    /**
     * Update the focus order (when the visible items change).
     */
    fun updateFocusOrder(
        state: AccessibilityState,
        newOrder: List<String>,
        nowMs: Long,
    ): AccessibilityState {
        val newFocusedId = state.focusedTargetId?.let { id ->
            if (id in newOrder) id else newOrder.firstOrNull()
        }
        val newIndex = newFocusedId?.let { newOrder.indexOf(it) } ?: -1

        return state.copy(
            focusOrder = newOrder,
            focusedTargetId = newFocusedId,
            focusIndex = newIndex,
        )
    }

    /**
     * Make an announcement (TalkBack will read this aloud).
     */
    fun announce(
        state: AccessibilityState,
        message: String,
        nowMs: Long,
    ): AccessibilityState = state.copy(
        announcement = message,
    )

    /**
     * Clear any pending announcement.
     */
    fun clearAnnouncement(state: AccessibilityState): AccessibilityState =
        state.copy(announcement = null)

    /**
     * Switch the active accessibility mode.
     */
    fun switchMode(
        state: AccessibilityState,
        newMode: AccessibilityMode,
        nowMs: Long,
    ): AccessibilityState = state.copy(
        mode = newMode,
        // Clear focus if switching to TOUCH (touch doesn't need focus)
        focusedTargetId = if (newMode == AccessibilityMode.TOUCH) null else state.focusedTargetId,
        focusedTargetType = if (newMode == AccessibilityMode.TOUCH) null else state.focusedTargetType,
        focusedTargetItemId = if (newMode == AccessibilityMode.TOUCH) null else state.focusedTargetItemId,
        focusIndex = if (newMode == AccessibilityMode.TOUCH) -1 else state.focusIndex,
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 6 — SPATIAL FOCUS MAP
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Spatial focus navigation map — for Arrow / DPad navigation.
 *
 * Maps each focusable element to its neighbors in 4 directions.
 *
 * Rule #80: this is a *snapshot* of the spatial layout, not a UI
 * reference. The host (B5.5) builds this from the current screen
 * state and passes it to the Runtime.
 *
 * @property neighbors  Map from targetId → (direction → next targetId).
 */
data class SpatialFocusMap(
    val neighbors: Map<String, Map<FocusDirection, String>>,
) {
    /**
     * Get the next target in a direction from the current target.
     * Returns null if no neighbor exists in that direction.
     */
    fun nextTarget(currentId: String, direction: FocusDirection): String? =
        neighbors[currentId]?.get(direction)

    companion object {
        val EMPTY = SpatialFocusMap(neighbors = emptyMap())
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 7 — ACCESSIBILITY → INTERACTION INTENT TRANSLATION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Translate an [AccessibilityAction] into an [InteractionIntent].
 *
 * This bridges the accessibility layer to the standard interaction
 * layer. The Reducer receives the same [InteractionIntent] type
 * regardless of whether the user used Touch or Keyboard or Controller.
 *
 * Returns null if the action cannot be translated (e.g. Activate
 * with no focused target).
 */
fun AccessibilityAction.toInteractionIntent(
    timestampMs: Long,
    source: GestureSource,
): InteractionIntent? {
    return when (this) {
        is AccessibilityAction.Activate -> {
            val targetId = focusedTargetId ?: return null
            val targetType = focusedTargetType ?: return null
            val targetItemId = focusedTargetItemId ?: -1L

            when (targetType) {
                GestureTargetType.SONG -> InteractionIntent.SongTapped(
                    sourceGestureId = "a11y-activate-$timestampMs",
                    source = source,
                    timestampMs = timestampMs,
                    songId = targetItemId,
                    contextPlaylistId = null,
                )

                GestureTargetType.HERO -> InteractionIntent.HeroTapped(
                    sourceGestureId = "a11y-activate-$timestampMs",
                    source = source,
                    timestampMs = timestampMs,
                )

                GestureTargetType.DOCK -> InteractionIntent.DockTapped(
                    sourceGestureId = "a11y-activate-$timestampMs",
                    source = source,
                    timestampMs = timestampMs,
                )

                else -> InteractionIntent.CardTapped(
                    sourceGestureId = "a11y-activate-$timestampMs",
                    source = source,
                    timestampMs = timestampMs,
                    targetId = targetId,
                    targetType = targetType,
                    targetItemId = targetItemId,
                )
            }
        }

        is AccessibilityAction.LongPress -> {
            val targetId = focusedTargetId ?: return null
            val targetType = focusedTargetType ?: return null
            val targetItemId = focusedTargetItemId ?: -1L

            InteractionIntent.ContextMenuRequested(
                sourceGestureId = "a11y-longpress-$timestampMs",
                source = source,
                timestampMs = timestampMs,
                targetId = targetId,
                targetType = targetType,
                targetItemId = targetItemId,
                trigger = ContextTrigger.ACCESSIBILITY,
            )
        }

        is AccessibilityAction.MoveFocus -> null // Handled by AccessibilityInteractionRuntime.moveFocus directly

        is AccessibilityAction.Scroll -> null // Translated to Scrolled intent by host

        AccessibilityAction.Back,
        AccessibilityAction.OpenContextMenu,
        AccessibilityAction.TabNext,
        AccessibilityAction.TabPrevious,
        AccessibilityAction.Dismiss,
        AccessibilityAction.Confirm,
        AccessibilityAction.Cancel -> null // These are handled by host directly
    }
}
