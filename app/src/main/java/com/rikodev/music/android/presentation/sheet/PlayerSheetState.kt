package com.rikodev.music.android.presentation.sheet

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — PLAYER SHEET STATE (M1)
//
// Hoisted holder so RikoNavGraph owns the anchor truth and screens trigger
// Expand without navigation. The host calls updateAnchors() once measured;
// until then [anchorsReady] is false and nothing renders (offset is NaN
// before anchors arrive — rendering then would flash a full-screen sheet).
// ─────────────────────────────────────────────────────────────────────────────

@Stable
@OptIn(ExperimentalFoundationApi::class)
class PlayerSheetState internal constructor(
    val draggable: AnchoredDraggableState<SheetAnchor>
) {
    var anchorsReady by mutableStateOf(false)
        internal set

    val settled: SheetAnchor get() = draggable.settledValue

    suspend fun expand() {
        draggable.animateTo(PlayerSheetReducer.targetFor(PlayerSheetEvent.Expand, settled))
    }

    suspend fun collapse() {
        draggable.animateTo(PlayerSheetReducer.targetFor(PlayerSheetEvent.Collapse, settled))
    }

    suspend fun snapToMini() {
        draggable.snapTo(SheetAnchor.Mini)
    }

    internal fun installAnchors(anchors: DraggableAnchors<SheetAnchor>) {
        draggable.updateAnchors(anchors)
        anchorsReady = true
    }
}

/** Creates the hoisted sheet state. Call once per NavGraph lifetime. */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun rememberPlayerSheetState(): PlayerSheetState {
    val draggable = remember {
        AnchoredDraggableState(
            initialValue = SheetAnchor.Mini,
            positionalThreshold = { totalDistance ->
                totalDistance * PlayerSheetReducer.SNAP_FRACTION
            },
            velocityThreshold = { 400f },
            snapAnimationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioLowBouncy
            ),
            decayAnimationSpec = exponentialDecay(frictionMultiplier = 1f, absVelocityThreshold = 0.1f),
            confirmValueChange = { true }
        )
    }
    return remember(draggable) { PlayerSheetState(draggable) }
}
