/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 15 / 15 — InteractionPreview.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Living Documentation for the Library Interaction Runtime.
 *
 * A new developer should be able to open this file in Android Studio's
 * Preview pane and understand the entire interaction vocabulary —
 * without reading any other file.
 *
 * Rule #77 (Preview Is Documentation): every meaningful state has at
 * least one @Preview. Preview names are self-explanatory.
 *
 * WHAT'S DOCUMENTED HERE
 * ----------------------
 *   1. The 15 Gesture Kinds (LibraryGestureKind)
 *   2. The 6 Gesture Sources / Priorities (GestureSource / GesturePriority)
 *   3. The 4 Selection Modes (SelectionMode)
 *   4. The 4 Dock Morph States (DockMorphState)
 *   5. The 5 Long-Press Lifecycle States (LongPressLifecycle)
 *   6. The 6 Drag Lifecycle States (DragLifecycle)
 *   7. The 8 Core Context Actions (ContextAction variants)
 *   8. The 15 Haptic Patterns (HapticPattern)
 *   9. The 5 Accessibility Modes (AccessibilityMode)
 *  10. Sample Gesture Explanations (Rule #82)
 *  11. Sample Validation Reports (Rule #82)
 *
 * PREVIEW CONVENTIONS
 * -------------------
 * Every @Preview:
 *   - Has a human-readable name following the pattern
 *     "<Category> — <State> — <Variant>"
 *   - Uses `showBackground = true` with `backgroundColor = 0xFF050505`
 *     (RIKO dark canvas)
 *   - Renders a diagnostic card (not the actual UI) so the file is
 *     self-contained
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No business logic. No state mutation. No side effects. Only @Preview
 * composables + minimal diagnostic card composables.
 *
 * © RIKO Experience OS — Interaction Runtime
 */

package com.rikodev.music.android.presentation.library.interaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 1 — DIAGNOSTIC CARD PRIMITIVES
// ═══════════════════════════════════════════════════════════════════════════

private val RikoCanvas = Color(0xFF050505)
private val CardBackground = Color(0xFF111114)
private val CardBorder = Color(0xFF1F1F24)
private val AccentBlue = Color(0xFF6B9EFF)
private val AccentGreen = Color(0xFF7EE787)
private val AccentYellow = Color(0xFFFFD680)
private val AccentRed = Color(0xFFFF7B72)
private val AccentPurple = Color(0xFFD2A8FF)
private val TextPrimary = Color(0xFFE6E6E6)
private val TextSecondary = Color(0xFF8B8B8B)

@Composable
private fun DiagnosticCard(
    title: String,
    subtitle: String? = null,
    accent: Color = AccentBlue,
    content: @Composable () -> Unit,
) {
    Surface(
        color = CardBackground,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(accent, RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    color = TextSecondary,
                    fontSize = 11.sp,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ProgressBar(progress: Float, color: Color = AccentBlue) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(CardBorder, RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(4.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun Badge(text: String, color: Color = AccentBlue) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — GESTURE KIND PREVIEWS
// ═══════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Gesture Kinds — All 15 — Discrete vs Continuous",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewAllGestureKinds() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Library Gesture Vocabulary — 15 Kinds",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        val discreteKinds = LibraryGestureKind.values().filter { it.isDiscrete }
        val continuousKinds = LibraryGestureKind.values().filter { it.isContinuous }

        DiagnosticCard(
            title = "Discrete Gestures (${discreteKinds.size})",
            subtitle = "Happen at one instant — produce a single Intent",
            accent = AccentBlue,
        ) {
            discreteKinds.forEach { kind ->
                FieldRow(label = kind.name, value = kind.displayName)
            }
        }

        DiagnosticCard(
            title = "Continuous Gestures (${continuousKinds.size})",
            subtitle = "Happen over time — may produce a stream of Intents",
            accent = AccentPurple,
        ) {
            continuousKinds.forEach { kind ->
                FieldRow(label = kind.name, value = kind.displayName)
            }
        }
    }
}

@Preview(
    name = "Gesture Kind — Tap — Detail",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewTapDetail() {
    Column(modifier = Modifier.padding(16.dp)) {
        DiagnosticCard(
            title = "TAP",
            subtitle = "Single, brief touch within touch slop and tap window",
            accent = AccentBlue,
        ) {
            FieldRow("isDiscrete", "true")
            FieldRow("isContinuous", "false")
            FieldRow("maxTouchSlop", "8 dp")
            FieldRow("maxDuration", "200 ms")
            FieldRow("maxDoubleTapGap", "280 ms")
            FieldRow("produces", "CardTapped / SongTapped / HeroTapped / DockTapped")
        }
    }
}

@Preview(
    name = "Gesture Kind — LongPress — Detail",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewLongPressDetail() {
    Column(modifier = Modifier.padding(16.dp)) {
        DiagnosticCard(
            title = "LONG_PRESS",
            subtitle = "Sustained touch beyond minDurationMs",
            accent = AccentYellow,
        ) {
            FieldRow("isDiscrete", "true")
            FieldRow("minDurationMs", "400 ms (DEFAULT)")
            FieldRow("maxMovementDp", "16 dp")
            FieldRow("hapticFires", "at threshold crossing")
            FieldRow("produces", "ContextMenuRequested")
            Spacer(modifier = Modifier.height(8.dp))
            Badge(text = "QUICK preset: 280ms", color = AccentGreen)
            Spacer(modifier = Modifier.height(4.dp))
            Badge(text = "FORGIVING preset: 600ms", color = AccentPurple)
        }
    }
}

@Preview(
    name = "Gesture Kind — DockPull — Detail",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewDockPullDetail() {
    Column(modifier = Modifier.padding(16.dp)) {
        DiagnosticCard(
            title = "DOCK_PULL",
            subtitle = "Swipe up from Mini Player Dock to expand to Now Playing",
            accent = AccentPurple,
        ) {
            FieldRow("isContinuous", "true")
            FieldRow("commitThreshold", "120 dp (DEFAULT)")
            FieldRow("cancelThreshold", "40 dp")
            FieldRow("maxStretch", "60 dp (overscroll)")
            FieldRow("animationDuration", "280 ms")
            FieldRow("produces", "DockPullProgressed / DockPullCommitted")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — GESTURE PRIORITY PREVIEWS
// ═══════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Gesture Priority — Hierarchy — System > Dock > Hero > Card > Child > Overlay",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewGesturePriorityHierarchy() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Priority Hierarchy",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        val priorities = GesturePriority.values()
        val colors = listOf(
            AccentRed, AccentYellow, AccentGreen,
            AccentBlue, AccentPurple, TextSecondary,
        )

        priorities.forEachIndexed { index, priority ->
            DiagnosticCard(
                title = "#${priority.order}  ${priority.displayName}",
                subtitle = priority.rationale,
                accent = colors.getOrElse(index) { AccentBlue },
            ) {
                FieldRow("order", "${priority.order}")
                FieldRow("beats lower", "true")
            }
        }
    }
}

@Preview(
    name = "Conflict Resolution — Long Press + Scroll → Winner?",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewConflictResolutionExample() {
    Column(modifier = Modifier.padding(16.dp)) {
        DiagnosticCard(
            title = "Conflict Resolution Example",
            subtitle = "Long Press vs Scroll on a CARD",
            accent = AccentYellow,
        ) {
            FieldRow("rule 1", "PRIORITY (both CARD → tie)", AccentYellow)
            FieldRow("rule 2", "LIFECYCLE (IN_FLIGHT vs COMPLETED)", AccentBlue)
            FieldRow("result", "Scroll wins during motion", AccentGreen)
            FieldRow("result", "LongPress wins at completion", AccentGreen)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "If movement > 16dp → LongPress is CANCELLED, Scroll wins.\n" +
                        "If duration > 400ms AND movement ≤ 16dp → LongPress ACCEPTED.",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — SELECTION MODE PREVIEWS
// ═══════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Selection — All 4 Modes — NONE/SINGLE/MULTIPLE/RANGE",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewAllSelectionModes() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Selection Modes",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        SelectionMode.values().forEach { mode ->
            DiagnosticCard(
                title = mode.displayName,
                subtitle = "allowsMultiSelect=${mode.allowsMultiSelect}",
                accent = when (mode) {
                    SelectionMode.NONE -> TextSecondary
                    SelectionMode.SINGLE -> AccentBlue
                    SelectionMode.MULTIPLE -> AccentGreen
                    SelectionMode.RANGE -> AccentPurple
                },
            ) {
                when (mode) {
                    SelectionMode.NONE -> {
                        FieldRow("state", "no selection active")
                    }
                    SelectionMode.SINGLE -> {
                        FieldRow("behavior", "tap replaces selection")
                        FieldRow("transition", "NONE → SINGLE (tap)")
                    }
                    SelectionMode.MULTIPLE -> {
                        FieldRow("behavior", "tap toggles items individually")
                        FieldRow("transition", "SINGLE → MULTIPLE (long-press)")
                    }
                    SelectionMode.RANGE -> {
                        FieldRow("behavior", "anchor + extend-to defines range")
                        FieldRow("anchor", "SelectionAnchor(itemId, itemIndex, scopeId)")
                    }
                }
            }
        }
    }
}

@Preview(
    name = "Selection — Range Mode — Anchor + Extend Example",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewRangeSelectionExample() {
    Column(modifier = Modifier.padding(16.dp)) {
        DiagnosticCard(
            title = "Range Selection — Example",
            subtitle = "Anchor at index 2, extend to index 6",
            accent = AccentPurple,
        ) {
            FieldRow("anchor.itemId", "42")
            FieldRow("anchor.itemIndex", "2")
            FieldRow("anchor.scopeId", "albums")
            FieldRow("extend.targetId", "78")
            FieldRow("extend.targetIndex", "6")
            FieldRow("result.selectedIds", "[42, 43, 44, 45, 46, 47, 78]")
            FieldRow("result.count", "7")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 5 — DOCK MORPH PREVIEWS
// ═══════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Dock Morph — All 4 States",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewAllDockMorphStates() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Dock Morph States",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        DockMorphState.values().forEach { state ->
            DiagnosticCard(
                title = state.displayName,
                subtitle = "isMorphing=${state.isMorphing}, isInteractive=${state.isInteractive}",
                accent = when (state) {
                    DockMorphState.COLLAPSED -> TextSecondary
                    DockMorphState.EXPANDING -> AccentYellow
                    DockMorphState.EXPANDED -> AccentGreen
                    DockMorphState.COLLAPSING -> AccentRed
                },
            ) {
                when (state) {
                    DockMorphState.COLLAPSED -> {
                        FieldRow("progress", "0.0")
                        FieldRow("gestures", "Tap → instant expand; Pull up → begin morph")
                    }
                    DockMorphState.EXPANDING -> {
                        FieldRow("progress", "0.0 → 1.0 (user-driven)")
                        FieldRow("gestures", "non-morph gestures REJECTED")
                    }
                    DockMorphState.EXPANDED -> {
                        FieldRow("progress", "1.0")
                        FieldRow("gestures", "Pull down → begin collapse; Back → instant collapse")
                    }
                    DockMorphState.COLLAPSING -> {
                        FieldRow("progress", "1.0 → 0.0 (animated)")
                        FieldRow("gestures", "non-morph gestures REJECTED")
                    }
                }
            }
        }
    }
}

@Preview(
    name = "Dock Morph — Progress 0.5 — Expanding",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewDockMorphProgress50() {
    Column(modifier = Modifier.padding(16.dp)) {
        DiagnosticCard(
            title = "Dock Morph — 50% Progress",
            subtitle = "User is actively pulling the Dock up",
            accent = AccentYellow,
        ) {
            FieldRow("morphState", "EXPANDING")
            FieldRow("morphProgress", "0.5")
            FieldRow("commitThreshold", "120 dp")
            FieldRow("currentPull", "60 dp")
            FieldRow("willCommit", "if released past 120 dp")
            FieldRow("willCancel", "if released before 40 dp")
            Spacer(modifier = Modifier.height(8.dp))
            ProgressBar(progress = 0.5f, color = AccentYellow)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 6 — LONG-PRESS LIFECYCLE PREVIEWS
// ═══════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Long Press — Lifecycle — 5 States",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewLongPressLifecycle() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Long-Press Lifecycle",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        LongPressLifecycle.values().forEach { state ->
            DiagnosticCard(
                title = state.displayName,
                subtitle = "isPending=${state.isPending}, isFired=${state.isFired}",
                accent = when (state) {
                    LongPressLifecycle.IDLE -> TextSecondary
                    LongPressLifecycle.PENDING -> AccentYellow
                    LongPressLifecycle.FIRED -> AccentGreen
                    LongPressLifecycle.COMPLETED -> AccentBlue
                    LongPressLifecycle.CANCELLED -> AccentRed
                },
            ) {
                when (state) {
                    LongPressLifecycle.IDLE -> FieldRow("next", "PENDING (on touch-down)")
                    LongPressLifecycle.PENDING -> {
                        FieldRow("next", "FIRED (after 400ms) or CANCELLED")
                        FieldRow("progress", "0 → 1 toward threshold")
                    }
                    LongPressLifecycle.FIRED -> {
                        FieldRow("haptic", "emitted at threshold")
                        FieldRow("next", "COMPLETED (on release) or CANCELLED")
                    }
                    LongPressLifecycle.COMPLETED -> FieldRow("result", "LongPress gesture emitted")
                    LongPressLifecycle.CANCELLED -> FieldRow("result", "rolled back, no gesture")
                }
            }
        }
    }
}

@Preview(
    name = "Long Press — PENDING — 50% Progress",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewLongPressPending50() {
    Column(modifier = Modifier.padding(16.dp)) {
        DiagnosticCard(
            title = "Long-Press — 50% Toward Threshold",
            subtitle = "User has held for 200ms of 400ms required",
            accent = AccentYellow,
        ) {
            FieldRow("lifecycle", "PENDING")
            FieldRow("startedAtMs", "1000")
            FieldRow("nowMs", "1200")
            FieldRow("elapsed", "200 ms")
            FieldRow("threshold", "400 ms")
            FieldRow("progress", "0.5")
            Spacer(modifier = Modifier.height(8.dp))
            ProgressBar(progress = 0.5f, color = AccentYellow)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 7 — DRAG LIFECYCLE PREVIEWS
// ═══════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Drag Lifecycle — 6 States",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewDragLifecycle() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Drag Lifecycle",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        DragLifecycle.values().forEach { state ->
            DiagnosticCard(
                title = state.displayName,
                subtitle = "isActive=${state.isActive}, isVisible=${state.isVisible}",
                accent = when (state) {
                    DragLifecycle.IDLE -> TextSecondary
                    DragLifecycle.PENDING -> AccentYellow
                    DragLifecycle.ACTIVE -> AccentGreen
                    DragLifecycle.SETTLING -> AccentBlue
                    DragLifecycle.COMMITTED -> AccentPurple
                    DragLifecycle.CANCELLED -> AccentRed
                },
            ) {
                when (state) {
                    DragLifecycle.IDLE -> FieldRow("next", "PENDING (drag started)")
                    DragLifecycle.PENDING -> {
                        FieldRow("next", "ACTIVE (exceeds 16dp) or IDLE (released)")
                        FieldRow("visibility", "not yet visible to user")
                    }
                    DragLifecycle.ACTIVE -> {
                        FieldRow("visibility", "drag preview shown")
                        FieldRow("next", "SETTLING (released)")
                    }
                    DragLifecycle.SETTLING -> {
                        FieldRow("animation", "settling into final position")
                        FieldRow("next", "COMMITTED")
                    }
                    DragLifecycle.COMMITTED -> {
                        FieldRow("result", "reorder / dismiss applied")
                        FieldRow("isTerminal", "true")
                    }
                    DragLifecycle.CANCELLED -> FieldRow("result", "rolled back")
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 8 — CONTEXT MENU PREVIEWS
// ═══════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Context Menu — Song Target — Full Menu",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewSongContextMenu() {
    Column(modifier = Modifier.padding(16.dp)) {
        val spec = ContextMenuRuntime.menuFor(
            targetId = "song-1",
            targetType = GestureTargetType.SONG,
            targetItemId = 1L,
            targetTitle = "Bohemian Rhapsody",
            targetSubtitle = "Queen • A Night at the Opera",
            targetArtworkKey = "artwork-1",
            stateContext = ContextMenuStateContext.DEFAULT,
        )

        DiagnosticCard(
            title = "Context Menu — Song",
            subtitle = "${spec.allItems.size} items across ${spec.sections.size} sections",
            accent = AccentBlue,
        ) {
            spec.sections.forEach { section ->
                Text(
                    text = section.title ?: "(unnamed)",
                    color = AccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                section.items.forEach { item ->
                    val icon = when {
                        item.isDestructive -> "⚠ "
                        item.badge != null -> "[${item.badge}] "
                        else -> "  "
                    }
                    FieldRow(
                        label = "$icon${item.icon.name}",
                        value = item.title,
                        valueColor = if (item.isDestructive) AccentRed else TextPrimary,
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Context Menu — Album Target — Full Menu",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewAlbumContextMenu() {
    Column(modifier = Modifier.padding(16.dp)) {
        val spec = ContextMenuRuntime.menuFor(
            targetId = "album-1",
            targetType = GestureTargetType.ALBUM,
            targetItemId = 1L,
            targetTitle = "A Night at the Opera",
            targetSubtitle = "Queen • 1975 • 12 songs",
            targetArtworkKey = "artwork-2",
            stateContext = ContextMenuStateContext.DEFAULT,
        )

        DiagnosticCard(
            title = "Context Menu — Album",
            subtitle = "${spec.allItems.size} items across ${spec.sections.size} sections",
            accent = AccentGreen,
        ) {
            spec.sections.forEach { section ->
                Text(
                    text = section.title ?: "(unnamed)",
                    color = AccentGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                section.items.forEach { item ->
                    FieldRow(
                        label = item.icon.name,
                        value = item.title,
                        valueColor = if (item.isDestructive) AccentRed else TextPrimary,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 9 — HAPTIC PATTERN PREVIEWS
// ═══════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Haptics — All 15 Patterns — Specs",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewAllHapticPatterns() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Haptic Patterns — 15 Specs",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        HapticPattern.values().forEach { pattern ->
            val spec = HapticContract.specFor(pattern)
            DiagnosticCard(
                title = pattern.displayName,
                subtitle = "category=${pattern.category.name}, waveform=${spec.waveform.name}",
                accent = when (pattern.category) {
                    HapticCategory.VERY_LIGHT -> TextSecondary
                    HapticCategory.LIGHT -> AccentBlue
                    HapticCategory.MEDIUM -> AccentYellow
                    HapticCategory.STRONG -> AccentRed
                },
            ) {
                FieldRow("isEnabled", spec.isEnabled.toString())
                FieldRow("respectsSystem", spec.respectsSystemSetting.toString())
                FieldRow("totalDurationMs", "${spec.totalDurationMs} ms")
                FieldRow("pulses", "${spec.pulses.size}")
                spec.pulses.forEachIndexed { i, pulse ->
                    FieldRow("  pulse[$i]", "delay=${pulse.delayMs}ms, dur=${pulse.durationMs}ms, amp=${pulse.amplitude}")
                }
            }
        }
    }
}

@Preview(
    name = "Haptics — Profile Comparison — DEFAULT vs MINIMAL vs SILENT",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewHapticProfiles() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Haptic Profiles",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        HapticContract.HapticProfile.values().forEach { profile ->
            DiagnosticCard(
                title = profile.displayName,
                subtitle = "amplitudeMultiplier=${profile.amplitudeMultiplier}x",
                accent = when (profile) {
                    HapticContract.HapticProfile.DEFAULT -> AccentBlue
                    HapticContract.HapticProfile.MINIMAL -> AccentYellow
                    HapticContract.HapticProfile.SILENT -> TextSecondary
                    HapticContract.HapticProfile.STRONG -> AccentRed
                },
            ) {
                FieldRow("enabledPatterns", "${profile.enabledPatterns.size} / 15")
                FieldRow("multiplier", "${profile.amplitudeMultiplier}x")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 10 — ACCESSIBILITY PREVIEWS
// ═══════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Accessibility — All 5 Modes — Touch/TalkBack/Keyboard/Controller/Mouse",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewAllAccessibilityModes() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Accessibility Input Modes",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        AccessibilityMode.values().forEach { mode ->
            DiagnosticCard(
                title = mode.displayName,
                subtitle = "supportsHover=${mode.supportsHover}, requiresFocusIndicator=${mode.requiresFocusIndicator}",
                accent = when (mode) {
                    AccessibilityMode.TOUCH -> AccentBlue
                    AccessibilityMode.TALKBACK -> AccentGreen
                    AccessibilityMode.KEYBOARD -> AccentYellow
                    AccessibilityMode.CONTROLLER -> AccentPurple
                    AccessibilityMode.MOUSE -> AccentRed
                },
            ) {
                when (mode) {
                    AccessibilityMode.TOUCH -> {
                        FieldRow("input", "Tap, LongPress, Swipe, Drag, Pinch")
                        FieldRow("focus", "not required")
                    }
                    AccessibilityMode.TALKBACK -> {
                        FieldRow("input", "Double-tap = Tap, Double-tap-and-hold = LongPress")
                        FieldRow("focus", "explore-by-touch")
                    }
                    AccessibilityMode.KEYBOARD -> {
                        FieldRow("input", "Enter = Tap, Shift+F10 = Context, Arrows = MoveFocus")
                        FieldRow("focus", "spatial navigation")
                    }
                    AccessibilityMode.CONTROLLER -> {
                        FieldRow("input", "DPad-Center = Tap, DPad-Hold = LongPress")
                        FieldRow("focus", "spatial navigation")
                    }
                    AccessibilityMode.MOUSE -> {
                        FieldRow("input", "Click = Tap, Right-click = Context, Hover = Hover")
                        FieldRow("focus", "not required")
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 11 — EXPLANATION PREVIEWS (Rule #82)
// ═══════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Explanation — Tap Accepted — Example (Rule #82)",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewTapExplanationExample() {
    Column(modifier = Modifier.padding(16.dp)) {
        DiagnosticCard(
            title = "Gesture Explanation — Example",
            subtitle = "Tap accepted on a Song card",
            accent = AccentGreen,
        ) {
            Text(
                "Tap accepted",
                color = AccentGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Reason:", color = TextSecondary, fontSize = 11.sp)
            FieldRow("  Movement", "3 dp (within slop of 8 dp)")
            FieldRow("  Duration", "82 ms (within tap window of 200 ms)")
            FieldRow("  Priority", "CARD (resolved over CHILD)")
            FieldRow("  Conflict", "LongPress cancelled (movement exceeded slop)")
            FieldRow("  Outcome", "→ SongTappedIntent(songId=42)", AccentGreen)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Context snapshot:",
                color = TextSecondary,
                fontSize = 11.sp,
            )
            FieldRow("  target", "song-card-42")
            FieldRow("  type", "SONG")
            FieldRow("  selection", "false")
            FieldRow("  drag", "false")
        }
    }
}

@Preview(
    name = "Explanation — LongPress Ignored — Example (Rule #82)",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewLongPressIgnoredExplanation() {
    Column(modifier = Modifier.padding(16.dp)) {
        DiagnosticCard(
            title = "Gesture Explanation — Example",
            subtitle = "LongPress ignored (drag in progress)",
            accent = AccentYellow,
        ) {
            Text(
                "LongPress IGNORED",
                color = AccentYellow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Reason:", color = TextSecondary, fontSize = 11.sp)
            FieldRow("  Duration", "425 ms (exceeds 400 ms threshold)")
            FieldRow("  Movement", "2 dp (within 16 dp slop)")
            FieldRow("  Outcome", "IGNORED (no Intent emitted)", AccentYellow)
            FieldRow("  Why", "A drag is in progress on the same target")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Context snapshot:",
                color = TextSecondary,
                fontSize = 11.sp,
            )
            FieldRow("  target", "card-78")
            FieldRow("  type", "ALBUM")
            FieldRow("  selection", "false")
            FieldRow("  drag", "true", AccentRed)
            FieldRow("  dockMorph", "false")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 12 — VALIDATION REPORT PREVIEWS
// ═══════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Validation — Clean Report — Example",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewCleanValidationReport() {
    Column(modifier = Modifier.padding(16.dp)) {
        DiagnosticCard(
            title = "Validation Report — CLEAN ✅",
            subtitle = "All 5 rules (#78-#82) verified",
            accent = AccentGreen,
        ) {
            FieldRow("target", "song-card-42")
            FieldRow("errors", "0", AccentGreen)
            FieldRow("warnings", "0", AccentGreen)
            FieldRow("info", "0")
            FieldRow("status", "CLEAN ✅", AccentGreen)
            Spacer(modifier = Modifier.height(8.dp))
            ValidatedRule.values().forEach { rule ->
                FieldRow("Rule #${rule.ruleNumber}", "${rule.displayName} ✅", AccentGreen)
            }
        }
    }
}

@Preview(
    name = "Validation — Violations Found — Example",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewViolationsReport() {
    Column(modifier = Modifier.padding(16.dp)) {
        DiagnosticCard(
            title = "Validation Report — VIOLATIONS ❌",
            subtitle = "2 errors, 1 warning — needs developer attention",
            accent = AccentRed,
        ) {
            FieldRow("target", "gesture-789")
            FieldRow("errors", "2", AccentRed)
            FieldRow("warnings", "1", AccentYellow)
            FieldRow("status", "VIOLATIONS ❌", AccentRed)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Violations:", color = TextSecondary, fontSize = 11.sp)
            FieldRow("[ERROR] #82", "Explanation.reason is empty", AccentRed)
            FieldRow("[ERROR] #78", "ACCEPTED but no Intent", AccentRed)
            FieldRow("[WARN]  #81", "metrics is ZERO", AccentYellow)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 13 — INTERACTION LOG / INSPECTOR PREVIEW
// ═══════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Inspector — Log Summary — Example",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewInspectorSummary() {
    Column(modifier = Modifier.padding(16.dp)) {
        DiagnosticCard(
            title = "Interaction Log Summary",
            subtitle = "Session: abc-123 • 500 records (max)",
            accent = AccentBlue,
        ) {
            FieldRow("totalInteractions", "247")
            FieldRow("acceptanceRate", "78%", AccentGreen)
            FieldRow("avgResolutionMs", "3.2 ms", AccentGreen)
            FieldRow("conflicts (ignored)", "54", AccentYellow)
            FieldRow("errors", "0", AccentGreen)
            Spacer(modifier = Modifier.height(8.dp))
            Text("By kind:", color = TextSecondary, fontSize = 11.sp)
            FieldRow("  TAP", "132 (53%)")
            FieldRow("  SCROLL", "48 (19%)")
            FieldRow("  LONG_PRESS", "28 (11%)")
            FieldRow("  SWIPE", "18 (7%)")
            FieldRow("  DRAG", "12 (5%)")
            FieldRow("  DOCK_PULL", "9 (4%)")
            Spacer(modifier = Modifier.height(8.dp))
            Text("By source:", color = TextSecondary, fontSize = 11.sp)
            FieldRow("  CARD", "162 (66%)")
            FieldRow("  DOCK", "48 (19%)")
            FieldRow("  HERO", "22 (9%)")
            FieldRow("  CHILD", "15 (6%)")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 14 — RULES DASHBOARD
// ═══════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Rules — Dashboard — All 5 Rules (#78-#82)",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewRulesDashboard() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "B5.4 Rules — Compliance Dashboard",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        val rules = listOf(
            Triple(78, "Interaction Creates Intent", "Gestures emit Intents, never Actions"),
            Triple(79, "Selection Is Independent", "Library owns selection; cards query it"),
            Triple(80, "Gesture Never Knows UI", "Gestures carry only coordinates + time"),
            Triple(81, "Every Interaction Is Replayable", "Inspector can replay any gesture"),
            Triple(82, "Every Gesture Has Explanation", "Resolver produces structured reason"),
        )

        rules.forEach { (num, name, desc) ->
            DiagnosticCard(
                title = "Rule #$num — $name",
                subtitle = desc,
                accent = AccentGreen,
            ) {
                FieldRow("status", "ENFORCED ✅", AccentGreen)
                FieldRow("validator", "InteractionValidator.checkRule$num()")
                FieldRow("checked in", "validateResolution / validateGesture / validateLog")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 15 — FILE MANIFEST
// ═══════════════════════════════════════════════════════════════════════════

@Preview(
    name = "Manifest — All 15 B5.4 Files — Living Index",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewB54Manifest() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "B5.4 — Library Interaction Runtime — File Manifest",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        val files = listOf(
            "1. LibraryGesture.kt" to "15 gesture sealed class",
            "2. GesturePriority.kt" to "Hierarchy + conflict resolution",
            "3. GestureResolver.kt" to "Gesture → Intent translation",
            "4. SelectionRuntime.kt" to "Single/Multiple/Range/Invert/Clear/Pinned/Focused",
            "5. SelectionReducer.kt" to "Pure (state, intent) → (state, actions)",
            "6. ContextMenuRuntime.kt" to "Menu spec generation per target type",
            "7. ContextAction.kt" to "Pin/Rename/Delete/Hide/Queue/Favorite/Share/Info",
            "8. DockInteraction.kt" to "Mini Player Dock morph state machine",
            "9. LibraryDragRuntime.kt" to "Drag/Drop/Reorder/Hover/Threshold",
            "10. LongPressRuntime.kt" to "Press lifecycle (IDLE→PENDING→FIRED→COMPLETED)",
            "11. HapticContract.kt" to "15 haptic patterns + 4 profiles",
            "12. AccessibilityInteraction.kt" to "TalkBack/Keyboard/Controller/Mouse",
            "13. InteractionDiagnostics.kt" to "Inspector + Replay (Rule #81)",
            "14. InteractionValidator.kt" to "Rule #78-#82 enforcement checker",
            "15. InteractionPreview.kt" to "THIS FILE — Living Documentation",
        )

        DiagnosticCard(
            title = "15 Files — Monster Build",
            subtitle = "Pure Kotlin (no Compose) except Item 15 (this file)",
            accent = AccentBlue,
        ) {
            files.forEach { (name, desc) ->
                FieldRow(label = name, value = desc)
            }
        }
    }
}
