package com.rikodev.music.android.presentation.visualos.runtime

import com.rikodev.music.android.presentation.visualos.contract.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Pause

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL OS — PURE REDUCER
//
// The brain of the Visual OS state machine.
// Input: (State, Event) → Output: Pair<State, List<Action>>
//
// Rules enforced:
//   - Reducer is pure: no side effects, no coroutine launches
//   - Reducer NEVER calls Engine (Rule #47)
//   - Reducer NEVER mutates state — returns new instances
//   - Reducer NEVER executes actions — Coordinator does (Rule #83)
//   - State is immutable (Architecture Invariant #6)
//   - Actions describe intent — never execution (Rule #48)
//   - Events Are Verbs Past Tense (Rule #45)
//   - Actions Are Nouns (Rule #48)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pure reducer for the Visual OS state machine.
 *
 * Every event that arrives in the Visual OS passes through this reducer.
 * The reducer computes the new state and any actions that need to be
 * executed by the Coordinator.
 *
 * This reducer follows the EXACT pattern established in:
 *   - CollectionRuntimeReducer (B2)
 *   - LibraryScreenReducer (B5.2)
 *   - SearchReducer (M3)
 *   - QueueReducer (M4)
 *
 * Pattern:
 *   fun reduce(state: VisualOsState, event: VisualOsEvent): Pair<VisualOsState, List<VisualOsAction>>
 */
object VisualOsReducer {

    /**
     * Reduce: process an event against current state.
     *
     * @param state Current immutable state
     * @param event Event that happened
     * @return Pair of (new state, list of actions to execute)
     */
    fun reduce(
        state: VisualOsState,
        event: VisualOsEvent
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return when (event) {
            // ── Navigation Events ───────────────────────────────────────
            is VisualOsEvent.SceneNavigated -> handleSceneNavigated(state, event)
            is VisualOsEvent.BackPressed -> handleBackPressed(state)
            is VisualOsEvent.SystemBackPressed -> handleSystemBackPressed(state)

            // ── Layer Events ───────────────────────────────────────────
            is VisualOsEvent.LayerPushed -> handleLayerPushed(state, event)
            is VisualOsEvent.LayerPopped -> handleLayerPopped(state, event)
            is VisualOsEvent.LayerFocused -> handleLayerFocused(state, event)
            is VisualOsEvent.LayerUnfocused -> handleLayerUnfocused(state, event)

            // ── Panel Events ──────────────────────────────────────────
            is VisualOsEvent.PanelOpened -> handlePanelOpened(state, event)
            is VisualOsEvent.PanelClosed -> handlePanelClosed(state, event)
            is VisualOsEvent.PanelDismissed -> handlePanelDismissed(state, event)

            // ── Overlay Events ───────────────────────────────────────
            is VisualOsEvent.OverlayShown -> handleOverlayShown(state, event)
            is VisualOsEvent.OverlayHidden -> handleOverlayHidden(state, event)
            is VisualOsEvent.OverlayOutsideTapped -> handleOverlayOutsideTap(state)

            // ── Dock Events ───────────────────────────────────────────
            is VisualOsEvent.DockAppeared -> handleDockAppeared(state, event)
            is VisualOsEvent.DockDisappeared -> handleDockDisappeared(state)
            is VisualOsEvent.DockTapped -> handleDockTapped(state)
            is VisualOsEvent.DockPlayPauseToggled -> handleDockPlayPause(state)
            is VisualOsEvent.DockNextRequested -> handleDockNext(state)
            is VisualOsEvent.DockPreviousRequested -> handleDockPrevious(state)

            // ── Transition Events ──────────────────────────────────────
            is VisualOsEvent.TransitionTicked -> handleTransitionTicked(state, event)
            is VisualOsEvent.TransitionCompleted -> handleTransitionCompleted(state, event)
            is VisualOsEvent.TransitionInterrupted -> handleTransitionInterrupted(state, event)

            // ── Song / Background Events ───────────────────────────────
            is VisualOsEvent.SongChanged -> handleSongChanged(state, event)
            is VisualOsEvent.AlbumArtColorsExtracted -> handleColorsExtracted(state, event)
            is VisualOsEvent.BreathingPhaseUpdated -> handleBreathingUpdated(state, event)
            is VisualOsEvent.BackgroundDriftUpdated -> handleDriftUpdated(state, event)

            // ── System Events ─────────────────────────────────────────
            is VisualOsEvent.SystemConfigurationChanged -> handleConfigChanged(state, event)
            is VisualOsEvent.ReducedMotionChanged -> handleReducedMotion(state, event)
            is VisualOsEvent.RefreshRateChanged -> handleRefreshRate(state, event)

            // ── Handoff Events ────────────────────────────────────────
            is VisualOsEvent.HandoffToNowPlayingStarted -> handleHandoffToNowPlaying(state)
            is VisualOsEvent.HandoffToDockStarted -> handleHandoffToDock(state)
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // NAVIGATION HANDLERS
    // ═════════════════════════════════════════════════════════════════════

    private fun handleSceneNavigated(
        state: VisualOsState,
        event: VisualOsEvent.SceneNavigated
    ): Pair<VisualOsState, List<VisualOsAction>> {
        val targetLayerType = SceneSpec.resolveLayerType(event.target)

        // If navigating to a base scene, handle as tab switch
        if (targetLayerType == RikoLayerType.BASE) {
            return handleBaseSwitch(state, event.target)
        }

        // If navigating to NowPlaying, handle as handoff from dock
        if (event.target is RikoSceneType.NowPlaying) {
            return handleHandoffToNowPlaying(state)
        }

        // If navigating to a panel scene, open as panel
        if (targetLayerType == RikoLayerType.PANEL) {
            return handlePanelOpened(state, VisualOsEvent.PanelOpened(event.target))
        }

        // Default: push scene
        val newScene = SceneSpec.create(event.target, event.context)
        val background = state.sceneStack.lastOrNull()
        val transitionSpec = TransitionSpec.push(newScene, background)

        val newStack = state.sceneStack.map { scene ->
            if (scene == background && background != null) {
                // De-emphasize the background scene
                scene.copy(
                    lifecycle = SceneLifecycle.DEEMPHASIZED,
                    visualState = VisualState.deemphasizedForLayer(scene.layerType)
                )
            } else {
                scene
            }
        } + newScene.copy(lifecycle = SceneLifecycle.ACTIVE)

        val newState = state.copy(
            sceneStack = newStack,
            focusOwner = when (targetLayerType) {
                RikoLayerType.FOCUS -> FocusOwner.FOCUS_SCENE
                RikoLayerType.PANEL -> FocusOwner.PANEL
                else -> state.focusOwner
            },
            activeTransition = ActiveTransition(
                spec = transitionSpec,
                enteringVisualState = newScene.visualState
            )
        )

        return newState to listOf(
            VisualOsAction.BeginTransition(transitionSpec)
        )
    }

    private fun handleBaseSwitch(
        state: VisualOsState,
        target: RikoSceneType
    ): Pair<VisualOsState, List<VisualOsAction>> {
        // Tab switch: replace current base scene, no de-emphasis
        val currentBase = state.baseScene
        val newBase = SceneSpec.create(target)

        val newStack = if (currentBase != null) {
            // Keep non-base scenes, replace base
            state.sceneStack.map { scene ->
                if (scene.id == currentBase.id) {
                    newBase.copy(lifecycle = SceneLifecycle.ACTIVE)
                } else {
                    scene
                }
            }
        } else {
            listOf(newBase.copy(lifecycle = SceneLifecycle.ACTIVE))
        }

        val transitionSpec = TransitionSpec(
            type = TransitionType.SWITCH,
            targetSceneId = newBase.id,
            backgroundSceneId = currentBase?.id,
            durationMs = KeyframeTimings.BASE_SWITCH_TRANSITION,
            motionWord = MotionWord.HANDOFF,
            spring = SpringTokens.Smooth,
            keyframes = KeyframeSequence.defaultForType(TransitionType.SWITCH),
            isEnter = true,
            animateBackground = false // Tab switch doesn't blur background
        )

        return state.copy(
            sceneStack = newStack,
            focusOwner = FocusOwner.BASE,
            activeTransition = ActiveTransition(spec = transitionSpec)
        ) to listOf(VisualOsAction.BeginTransition(transitionSpec))
    }

    private fun handleBackPressed(
        state: VisualOsState
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return when {
            // Priority 1: Close overlay if showing
            state.isOverlayShowing -> {
                val newState = state.copy(
                    overlayState = state.overlayState.copy(isActive = false),
                    focusOwner = when (state.focusOwner) {
                        FocusOwner.OVERLAY -> FocusOwner.FOCUS_SCENE
                        else -> state.focusOwner
                    }
                )
                newState to listOf(VisualOsAction.HideOverlay(state.overlayState.type))
            }

            // Priority 2: Close panel if open
            state.isPanelOpen -> {
                val panel = state.activePanel!!
                val newStack = state.sceneStack.filter { it.id != panel.id }
                // Restore the focus/base scene below
                val restoredStack = newStack.mapIndexed { index, scene ->
                    if (index == newStack.lastIndex) {
                        scene.copy(
                            lifecycle = SceneLifecycle.ACTIVE,
                            visualState = VisualState.activeForLayer(scene.layerType)
                        )
                    } else {
                        scene
                    }
                }
                state.copy(
                    sceneStack = restoredStack,
                    focusOwner = if (state.focusScene != null) FocusOwner.FOCUS_SCENE else FocusOwner.BASE,
                    activeTransition = ActiveTransition(
                        spec = TransitionSpec.pop(panel, restoredStack.last())
                    )
                ) to listOf(VisualOsAction.CompleteTransition(panel.id))
            }

            // Priority 3: Pop focus scene if not at base
            state.focusScene != null -> {
                val focus = state.focusScene!!
                val newStack = state.sceneStack.filter { it.id != focus.id }
                val restoredStack = newStack.mapIndexed { index, scene ->
                    if (index == newStack.lastIndex) {
                        scene.copy(
                            lifecycle = SceneLifecycle.ACTIVE,
                            visualState = VisualState.activeForLayer(scene.layerType)
                        )
                    } else {
                        scene
                    }
                }
                state.copy(
                    sceneStack = restoredStack,
                    focusOwner = FocusOwner.BASE,
                    activeTransition = ActiveTransition(
                        spec = TransitionSpec.pop(focus, restoredStack.last())
                    )
                ) to listOf(VisualOsAction.CompleteTransition(focus.id))
            }

            // At base — nothing to pop
            else -> state to emptyList()
        }
    }

    private fun handleSystemBackPressed(
        state: VisualOsState
    ): Pair<VisualOsState, List<VisualOsAction>> = handleBackPressed(state)

    // ═════════════════════════════════════════════════════════════════════
    // LAYER HANDLERS
    // ═════════════════════════════════════════════════════════════════════

    private fun handleLayerPushed(
        state: VisualOsState,
        event: VisualOsEvent.LayerPushed
    ): Pair<VisualOsState, List<VisualOsAction>> {
        // Already handled by SceneNavigated — this is for internal use
        return state to emptyList()
    }

    private fun handleLayerPopped(
        state: VisualOsState,
        event: VisualOsEvent.LayerPopped
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return handleBackPressed(state)
    }

    private fun handleLayerFocused(
        state: VisualOsState,
        event: VisualOsEvent.LayerFocused
    ): Pair<VisualOsState, List<VisualOsAction>> {
        val newStack = state.sceneStack.map { scene ->
            when (scene.id) {
                event.sceneId -> scene.copy(
                    lifecycle = SceneLifecycle.ACTIVE,
                    visualState = VisualState.activeForLayer(scene.layerType)
                )
                else -> if (scene.isVisible) {
                    scene.copy(
                        lifecycle = SceneLifecycle.DEEMPHASIZED,
                        visualState = VisualState.deemphasizedForLayer(scene.layerType)
                    )
                } else scene
            }
        }
        return state.copy(sceneStack = newStack) to emptyList()
    }

    private fun handleLayerUnfocused(
        state: VisualOsState,
        event: VisualOsEvent.LayerUnfocused
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return state to emptyList()
    }

    // ═════════════════════════════════════════════════════════════════════
    // PANEL HANDLERS
    // ═════════════════════════════════════════════════════════════════════

    private fun handlePanelOpened(
        state: VisualOsState,
        event: VisualOsEvent.PanelOpened
    ): Pair<VisualOsState, List<VisualOsAction>> {
        // Only allow panel on top of NowPlaying (spec: panels open from NP)
        if (state.focusScene?.type != RikoSceneType.NowPlaying) {
            return state to emptyList()
        }

        val newPanel = SceneSpec.create(event.type)
        val background = state.focusScene

        val transitionSpec = TransitionSpec(
            type = TransitionType.PANEL_OPEN,
            targetSceneId = newPanel.id,
            backgroundSceneId = background?.id,
            durationMs = KeyframeTimings.PANEL_TRANSITION,
            motionWord = MotionWord.LIFT,
            spring = SpringTokens.Comfortable,
            keyframes = KeyframeSequence.defaultForType(TransitionType.PANEL_OPEN),
            isEnter = true,
            animateBackground = true
        )

        val newStack = state.sceneStack.map { scene ->
            if (scene.id == background?.id) {
                scene.copy(
                    lifecycle = SceneLifecycle.DEEMPHASIZED,
                    visualState = VisualState.deemphasizedForLayer(RikoLayerType.FOCUS)
                )
            } else {
                scene
            }
        } + newPanel.copy(lifecycle = SceneLifecycle.ACTIVE)

        return state.copy(
            sceneStack = newStack,
            focusOwner = FocusOwner.PANEL,
            activeTransition = ActiveTransition(
                spec = transitionSpec,
                enteringVisualState = newPanel.visualState
            )
        ) to listOf(VisualOsAction.BeginTransition(transitionSpec))
    }

    private fun handlePanelClosed(
        state: VisualOsState,
        event: VisualOsEvent.PanelClosed
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return handlePanelDismissed(state, VisualOsEvent.PanelDismissed(event.sceneId))
    }

    private fun handlePanelDismissed(
        state: VisualOsState,
        event: VisualOsEvent.PanelDismissed
    ): Pair<VisualOsState, List<VisualOsAction>> {
        val panel = state.sceneStack.find { it.id == event.sceneId } ?: return state to emptyList()

        val newStack = state.sceneStack
            .filter { it.id != event.sceneId }
            .mapIndexed { index, scene ->
                if (index == state.sceneStack.size - 2) {
                    // Restore the scene below the panel
                    scene.copy(
                        lifecycle = SceneLifecycle.ACTIVE,
                        visualState = VisualState.activeForLayer(scene.layerType)
                    )
                } else {
                    scene
                }
            }

        val background = newStack.lastOrNull()

        return state.copy(
            sceneStack = newStack,
            focusOwner = if (state.focusScene != null && state.focusScene!!.id != event.sceneId)
                FocusOwner.FOCUS_SCENE else FocusOwner.BASE,
            activeTransition = if (background != null) {
                ActiveTransition(
                    spec = TransitionSpec.pop(panel, background)
                )
            } else null
        ) to listOfNotNull(
            background?.let { VisualOsAction.CompleteTransition(event.sceneId) }
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // OVERLAY HANDLERS
    // ═════════════════════════════════════════════════════════════════════

    private fun handleOverlayShown(
        state: VisualOsState,
        event: VisualOsEvent.OverlayShown
    ): Pair<VisualOsState, List<VisualOsAction>> {
        // RUNTIME FIX 11: same cycle shape as handleColorsExtracted — this
        // emitted VisualOsAction.ShowOverlay(...) here, whose only
        // Coordinator handler re-dispatched THIS SAME OverlayShown event,
        // an unconditional loop straight to a stack overflow. State is
        // already applied directly below; the action was redundant.
        return state.copy(
            overlayState = state.overlayState.copy(
                isActive = true,
                type = event.type,
                visualState = VisualState.defaultForLayer(RikoLayerType.OVERLAY),
                anchorPosition = event.anchor,
                contentData = event.data,
                dismissOnOutsideTap = true
            ),
            focusOwner = FocusOwner.OVERLAY
        ) to emptyList()
    }

    private fun handleOverlayHidden(
        state: VisualOsState,
        event: VisualOsEvent.OverlayHidden
    ): Pair<VisualOsState, List<VisualOsAction>> {
        // RUNTIME FIX 11: same cycle shape as handleColorsExtracted —
        // removed the redundant VisualOsAction.HideOverlay(...) emission.
        return state.copy(
            overlayState = state.overlayState.copy(isActive = false),
            focusOwner = if (state.isPanelOpen) FocusOwner.PANEL
                else if (state.focusScene != null) FocusOwner.FOCUS_SCENE
                else FocusOwner.BASE
        ) to emptyList()
    }

    private fun handleOverlayOutsideTap(
        state: VisualOsState
    ): Pair<VisualOsState, List<VisualOsAction>> {
        if (!state.isOverlayShowing || !state.overlayState.dismissOnOutsideTap) {
            return state to emptyList()
        }
        return handleOverlayHidden(state, VisualOsEvent.OverlayHidden(state.overlayState.type))
    }

    // ═════════════════════════════════════════════════════════════════════
    // DOCK HANDLERS
    // ═════════════════════════════════════════════════════════════════════

    private fun handleDockAppeared(
        state: VisualOsState,
        event: VisualOsEvent.DockAppeared
    ): Pair<VisualOsState, List<VisualOsAction>> {
        // Don't show dock if NowPlaying is active
        if (state.focusScene?.type == RikoSceneType.NowPlaying) {
            return state to emptyList()
        }

        // RUNTIME FIX 11: same cycle shape as handleColorsExtracted —
        // removed the redundant VisualOsAction.ShowDock(...) emission.
        // This one is the most frequently-triggered of the three fixed
        // here: it fires every time the dock appears while navigating
        // away from Now Playing during playback — a very common action,
        // and the corresponding crash was one accidental navigation away
        // from happening in testing, not just song-transition-specific.
        return state.copy(
            dockState = state.dockState.copy(
                visibility = DockVisibility.VISIBLE,
                visualState = VisualState.activeForLayer(RikoLayerType.DOCK),
                songTitle = event.songTitle,
                songArtist = event.songArtist,
                accentColor = event.accentColor,
                isPlaying = event.isPlaying
            )
        ) to emptyList()
    }

    private fun handleDockDisappeared(
        state: VisualOsState
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return state.copy(
            dockState = state.dockState.copy(
                visibility = DockVisibility.HIDDEN,
                visualState = VisualState.defaultForLayer(RikoLayerType.DOCK)
            )
        ) to listOf(VisualOsAction.HideDock)
    }

    private fun handleDockTapped(
        state: VisualOsState
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return handleHandoffToNowPlaying(state)
    }

    private fun handleDockPlayPause(
        state: VisualOsState
    ): Pair<VisualOsState, List<VisualOsAction>> {
        val newIsPlaying = !state.dockState.isPlaying
        return state.copy(
            dockState = state.dockState.copy(isPlaying = newIsPlaying)
        ) to listOf(VisualOsAction.TogglePlayPause)
    }

    private fun handleDockNext(
        state: VisualOsState
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return state to listOf(VisualOsAction.SkipToNext)
    }

    private fun handleDockPrevious(
        state: VisualOsState
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return state to listOf(VisualOsAction.SkipToPrevious)
    }

    // ═════════════════════════════════════════════════════════════════════
    // TRANSITION HANDLERS
    // ═════════════════════════════════════════════════════════════════════

    private fun handleTransitionTicked(
        state: VisualOsState,
        event: VisualOsEvent.TransitionTicked
    ): Pair<VisualOsState, List<VisualOsAction>> {
        val transition = state.activeTransition ?: return state to emptyList()

        val updatedTransition = transition.copy(progress = event.progress)

        return if (updatedTransition.isComplete) {
            state.copy(activeTransition = null) to listOf(
                VisualOsAction.CompleteTransition(transition.spec.targetSceneId)
            )
        } else {
            state.copy(activeTransition = updatedTransition) to emptyList()
        }
    }

    private fun handleTransitionCompleted(
        state: VisualOsState,
        event: VisualOsEvent.TransitionCompleted
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return state.copy(activeTransition = null) to emptyList()
    }

    private fun handleTransitionInterrupted(
        state: VisualOsState,
        event: VisualOsEvent.TransitionInterrupted
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return state.copy(
            activeTransition = state.activeTransition?.copy(isInterrupted = true)
        ) to emptyList()
    }

    // ═════════════════════════════════════════════════════════════════════
    // SONG / BACKGROUND HANDLERS
    // ═════════════════════════════════════════════════════════════════════

    private fun handleSongChanged(
        state: VisualOsState,
        event: VisualOsEvent.SongChanged
    ): Pair<VisualOsState, List<VisualOsAction>> {
        val actions = mutableListOf<VisualOsAction>()

        // Update song context
        // Update dock content if dock is visible
        val newDockState = if (state.isDockVisible) {
            state.dockState.copy(
                songTitle = event.context.title,
                songArtist = event.context.artist,
                isPlaying = event.context.isPlaying
            ).also {
                actions.add(VisualOsAction.UpdateDockContent(
                    event.context.title, event.context.artist, 0xFF000000
                ))
                actions.add(VisualOsAction.UpdateDockPlayback(event.context.isPlaying))
            }
        } else if (event.context.isPlaying) {
            // Song started playing — show dock
            state.dockState.copy(
                visibility = DockVisibility.VISIBLE,
                songTitle = event.context.title,
                songArtist = event.context.artist,
                isPlaying = true
            ).also {
                actions.add(VisualOsAction.ShowDock(
                    event.context.title, event.context.artist, 0xFF000000, true
                ))
            }
        } else {
            state.dockState
        }

        // Trigger album art color extraction if URI available
        val uri = event.context.albumArtUri
        if (uri != null) {
            actions.add(VisualOsAction.ExtractAlbumArtColors(uri))
        }

        return state.copy(
            songContext = event.context,
            dockState = newDockState
        ) to actions
    }

    private fun handleColorsExtracted(
        state: VisualOsState,
        event: VisualOsEvent.AlbumArtColorsExtracted
    ): Pair<VisualOsState, List<VisualOsAction>> {
        // RUNTIME FIX 11 (confirmed via real device crash log —
        // java.lang.StackOverflowError, main thread): this used to also
        // return listOf(VisualOsAction.UpdateBackgroundColors(...)) below.
        // That action's only handler (VisualOsCoordinator.executeActions)
        // re-dispatched THIS SAME AlbumArtColorsExtracted event, which
        // reaches this exact function again, which emitted the action
        // again -- an unconditional two-step cycle with no exit, straight
        // to a stack overflow on every song's album art color extraction.
        // The state update directly above already applies the colors —
        // that action was entirely redundant, not just badly implemented.
        // No other code path emits VisualOsAction.UpdateBackgroundColors
        // (confirmed via grep — this was its only emission site), so
        // removing it here fully closes the cycle at its root.
        return state.copy(
            backgroundState = state.backgroundState.copy(
                albumArtColors = event.colors,
                dominantColor = event.dominant,
                vibrantColor = event.vibrant
            )
        ) to emptyList()
    }

    private fun handleBreathingUpdated(
        state: VisualOsState,
        event: VisualOsEvent.BreathingPhaseUpdated
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return state.copy(
            backgroundState = state.backgroundState.copy(breathingPhase = event.phase)
        ) to emptyList()
    }

    private fun handleDriftUpdated(
        state: VisualOsState,
        event: VisualOsEvent.BackgroundDriftUpdated
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return state.copy(
            backgroundState = state.backgroundState.copy(
                driftOffsetX = event.offsetX,
                driftOffsetY = event.offsetY
            )
        ) to emptyList()
    }

    // ═════════════════════════════════════════════════════════════════════
    // SYSTEM HANDLERS
    // ═════════════════════════════════════════════════════════════════════

    private fun handleConfigChanged(
        state: VisualOsState,
        event: VisualOsEvent.SystemConfigurationChanged
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return state.copy(systemState = event.state) to emptyList()
    }

    private fun handleReducedMotion(
        state: VisualOsState,
        event: VisualOsEvent.ReducedMotionChanged
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return state.copy(
            systemState = state.systemState.copy(reducedMotion = event.enabled)
        ) to emptyList()
    }

    private fun handleRefreshRate(
        state: VisualOsState,
        event: VisualOsEvent.RefreshRateChanged
    ): Pair<VisualOsState, List<VisualOsAction>> {
        return state.copy(
            systemState = state.systemState.copy(
                refreshRateHz = event.hz,
                frameBudgetMs = VisualOsSystemState.frameBudgetForHz(event.hz)
            )
        ) to emptyList()
    }

    // ═════════════════════════════════════════════════════════════════════
    // HANDOFF HANDLERS (Dock ↔ NowPlaying)
    // ═════════════════════════════════════════════════════════════════════

    private fun handleHandoffToNowPlaying(
        state: VisualOsState
    ): Pair<VisualOsState, List<VisualOsAction>> {
        // Check if NowPlaying is already in the stack
        if (state.focusScene?.type == RikoSceneType.NowPlaying) {
            return state to emptyList()
        }

        val newScene = SceneSpec.create(RikoSceneType.NowPlaying)
        val background = state.baseScene
        val transitionSpec = TransitionSpec(
            type = TransitionType.HANDOFF,
            targetSceneId = newScene.id,
            backgroundSceneId = background?.id,
            durationMs = KeyframeTimings.HANDOFF_TRANSITION,
            motionWord = MotionWord.HANDOFF,
            spring = SpringTokens.Smooth,
            keyframes = KeyframeSequence.defaultForType(TransitionType.HANDOFF),
            isEnter = true,
            animateBackground = true
        )

        // Hide dock (morphing into NowPlaying)
        val newDock = state.dockState.copy(
            visibility = DockVisibility.HIDDEN,
            isMorphing = true,
            morphProgress = 0f
        )

        // De-emphasize base scene
        val newStack = state.sceneStack.map { scene ->
            if (scene.layerType == RikoLayerType.BASE) {
                scene.copy(
                    lifecycle = SceneLifecycle.DEEMPHASIZED,
                    visualState = VisualState.deemphasizedForLayer(RikoLayerType.BASE)
                )
            } else {
                scene
            }
        } + newScene.copy(lifecycle = SceneLifecycle.ACTIVE)

        return state.copy(
            sceneStack = newStack,
            focusOwner = FocusOwner.FOCUS_SCENE,
            dockState = newDock,
            activeTransition = ActiveTransition(
                spec = transitionSpec,
                enteringVisualState = newScene.visualState
            ),
            backgroundState = state.backgroundState.copy(
                servingScene = RikoSceneType.NowPlaying
            )
        ) to listOf(
            VisualOsAction.BeginTransition(transitionSpec),
            VisualOsAction.HideDock,
            VisualOsAction.NavigateToNowPlaying
        )
    }

    private fun handleHandoffToDock(
        state: VisualOsState
    ): Pair<VisualOsState, List<VisualOsAction>> {
        val nowPlaying = state.focusScene ?: return handleBackPressed(state)
        if (nowPlaying.type != RikoSceneType.NowPlaying) {
            return handleBackPressed(state)
        }

        // Remove NowPlaying from stack, restore base
        val newStack = state.sceneStack
            .filter { it.id != nowPlaying.id }
            .mapIndexed { index, scene ->
                if (index == 0) { // Base scene
                    scene.copy(
                        lifecycle = SceneLifecycle.ACTIVE,
                        visualState = VisualState.activeForLayer(RikoLayerType.BASE)
                    )
                } else {
                    scene
                }
            }

        // Show dock (NowPlaying collapsed to dock)
        val songCtx = state.songContext
        val newDock = state.dockState.copy(
            visibility = DockVisibility.VISIBLE,
            isMorphing = false,
            morphProgress = 0f,
            songTitle = songCtx?.title ?: "",
            songArtist = songCtx?.artist ?: "",
            isPlaying = songCtx?.isPlaying ?: false
        )

        return state.copy(
            sceneStack = newStack,
            focusOwner = FocusOwner.BASE,
            dockState = newDock,
            activeTransition = ActiveTransition(
                spec = TransitionSpec.pop(nowPlaying, newStack.first())
            ),
            backgroundState = state.backgroundState.copy(
                servingScene = newStack.firstOrNull()?.type
            )
        ) to listOf(
            VisualOsAction.CompleteTransition(nowPlaying.id),
            VisualOsAction.ShowDock(
                newDock.songTitle, newDock.songArtist, newDock.accentColor, newDock.isPlaying
            )
        )
    }
}
