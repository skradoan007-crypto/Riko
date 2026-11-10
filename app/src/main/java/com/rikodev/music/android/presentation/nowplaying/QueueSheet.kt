package com.rikodev.music.android.presentation.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rikodev.music.android.core.designsystem.riko.components.ARTWORK_PX_ROW
import com.rikodev.music.android.core.designsystem.riko.components.RikoArtwork
import com.rikodev.music.android.core.designsystem.riko.components.rememberAlbumArtworkModel
import com.rikodev.music.android.core.designsystem.riko.gesture.RikoTouch
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.glass.GlassCard
import com.rikodev.music.android.core.designsystem.theme.GlassTokens
import com.rikodev.music.android.core.designsystem.theme.LocalDynamicColors
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import com.rikodev.music.android.core.designsystem.theme.TextPrimary
import com.rikodev.music.android.core.designsystem.theme.TextSecondary
import com.rikodev.music.android.core.designsystem.theme.TextTertiary
import com.rikodev.music.android.domain.model.QueueItem
import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel
import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — QUEUE SHEET
//
// Full play-queue workflow: numbered positions (1, 2, 3, 4...), tap any
// song to jump directly to it, long-press + drag the handle to reorder,
// swipe-style X button to remove. Current song is pinned visually distinct
// with a glowing equalizer glyph instead of a number.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QueueSheet(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
    /** Shell snackbar host for the A14 remove-UNDO (root owns it). */
    snackbarHostState: SnackbarHostState? = null
) {
    // GRANULAR (PERF PASS Priority 1): the queue sheet renders the queue and
    // the playing index — it never consumed position, so it must not
    // recompose on the 4Hz position ticker.
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()
    val dynamicColors = LocalDynamicColors.current
    val accentColor = dynamicColors.vibrantColor
    val context = LocalContext.current
    val undoScope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    // Drag-to-reorder state — plan 2.2: identity follows the ENTRY's stable
    // key (song id + duplicate ordinal), never a captured queue position.
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    // Stable keys: "songId#dupOrdinal" contains NO queue position, so the key
    // set survives any reorder (pure song.id would crash on duplicate songs —
    // insertInQueue allows them). Identical duplicates swapping is cosmetic.
    val queueKeys = remember(queue) {
        val seen = HashMap<Long, Int>()
        queue.map { item ->
            val ordinal = seen[item.song.id] ?: 0
            seen[item.song.id] = ordinal + 1
            "${item.song.id}#$ordinal"
        }
    }
    // Fresh views for the running drag gesture (plan 2.2: stale captured
    // indices must never drive swaps).
    val queueKeysState = rememberUpdatedState(queueKeys)
    val queueSizeState = rememberUpdatedState(queue.size)
    val rowSwapThresholdPx = with(LocalDensity.current) { 76.dp.toPx() } * 0.6f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Up Next",
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary
            )
            Text(
                text = "${queue.size} songs",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(queue, key = { index, _ -> queueKeys[index] }, contentType = { _, _ -> "queue_row" }) { index, queueItem ->
                val isCurrentlyPlaying = index == currentQueueIndex
                val isDragged = queueKeys[index] == draggedKey

                QueueRow(
                    queueItem = queueItem,
                    displayPosition = index + 1,
                    isCurrentlyPlaying = isCurrentlyPlaying,
                    accentColor = accentColor,
                    context = context,
                    modifier = Modifier.graphicsLayer {
                        translationY = if (isDragged) dragOffsetY else 0f
                        scaleX = if (isDragged) 1.02f else 1f
                        scaleY = if (isDragged) 1.02f else 1f
                        shadowElevation = if (isDragged) 12f else 0f
                    },
                    onTap = { viewModel.skipToQueueItem(index) },
                    // A14: remove = instant + snackbar UNDO (re-insert at the
                    // same position via PlayerManager.insertInQueue).
                    onRemove = {
                        val removedIndex = index
                        val removedSong = queueItem.song
                        viewModel.removeFromQueue(removedIndex)
                        snackbarHostState?.let { host ->
                            undoScope.launch {
                                val result = host.showSnackbar(
                                    message = "Removed from queue",
                                    actionLabel = "UNDO",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.insertInQueue(removedIndex, removedSong)
                                }
                            }
                        }
                    },
                    onDragStart = { draggedKey = queueKeys[index]; dragOffsetY = 0f },
                    onDrag = { delta ->
                        dragOffsetY += delta
                        // Fresh identity lookup every move — survives N swaps in
                        // one continuous drag (plan 2.2).
                        val key = draggedKey ?: return@QueueRow
                        val from = queueKeysState.value.indexOf(key)
                        if (from < 0) return@QueueRow
                        if (dragOffsetY > rowSwapThresholdPx && from < queueSizeState.value - 1) {
                            viewModel.moveQueueItem(from, from + 1)
                            dragOffsetY = 0f
                        } else if (dragOffsetY < -rowSwapThresholdPx && from > 0) {
                            viewModel.moveQueueItem(from, from - 1)
                            dragOffsetY = 0f
                        }
                    },
                    onDragEnd = { draggedKey = null; dragOffsetY = 0f }
                )
            }

            if (queue.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Queue is empty",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Play a song — it lands here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    modifier: Modifier = Modifier,
    queueItem: QueueItem,
    displayPosition: Int,
    isCurrentlyPlaying: Boolean,
    accentColor: Color,
    context: android.content.Context,
    onTap: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) RikoTouch.PressScale else 1f,
        animationSpec = RikoMotion.MediumSpring(),
        label = "queue_row_press"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale },
        style = GlassTokens.FloatingCard,
        cornerRadius = 16.dp,
        enableShadow = false,
        tintColor = if (isCurrentlyPlaying) accentColor.copy(alpha = 0.10f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RikoShape.Card)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onTap
                )
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position number / now-playing indicator
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentlyPlaying) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = "Now playing",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = displayPosition.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                    )
                }
            }

            Spacer(modifier = Modifier.size(10.dp))

            Box(
                modifier = Modifier.size(44.dp).clip(RikoShape.MiniArtwork)
            ) {
                // PERF P0 (RC-06): 44dp row thumb prescale — factory request
                // (stable identity, zero crossfade in dense list — plan 1.2).
                RikoArtwork(
                    model = rememberAlbumArtworkModel(queueItem.song.albumId, ARTWORK_PX_ROW, songId = queueItem.song.id, songUri = queueItem.song.uri),
                    contentDescription = queueItem.song.title,
                    modifier = Modifier.fillMaxWidth().size(44.dp)
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = queueItem.song.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isCurrentlyPlaying) accentColor else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = queueItem.song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration — reference rows show mm:ss at the trailing edge.
            Text(
                text = queueItem.song.durationFormatted,
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )

            Spacer(modifier = Modifier.size(4.dp))

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove from queue",
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    // Plan R6: 48dp touch target (was 32dp — under minimum).
                    .size(RikoTouch.TouchTarget)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = TextTertiary.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

