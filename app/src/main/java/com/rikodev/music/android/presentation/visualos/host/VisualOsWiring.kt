package com.rikodev.music.android.presentation.visualos.host

import androidx.compose.runtime.*
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL OS — WIRING & COMPOSITION LOCALS
//
// Connects the Visual OS coordinator to the Compose tree.
// Provides CompositionLocals for accessing Visual OS state and dispatch.
//
// This is the Integration layer (final step in the pipeline):
// Contract → Runtime → Reducer → Mapper → Rendering → Integration
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CompositionLocal providing the Visual OS Coordinator.
 *
 * Any composable in the tree can access the coordinator to:
 *   - Read state (coordinator.state)
 *   - Dispatch events (coordinator.dispatch)
 *   - Navigate (coordinator.navigateTo)
 *   - Open panels (coordinator.openPanel)
 */
val LocalVisualOsCoordinator = compositionLocalOf<VisualOsCoordinator?> { null }

/**
 * CompositionLocal providing whether the Visual OS is active.
 * Used to conditionally enable layered rendering.
 */
val LocalVisualOsActive = compositionLocalOf<Boolean> { false }

/**
 * Provides the Visual OS to the Compose tree.
 *
 * Usage in MainActivity or top-level composable:
 * ```kotlin
 * ProvideVisualOs(coordinator) {
 *     RikoNavGraph() // existing navigation is wrapped by Visual OS
 * }
 * ```
 *
 * The Visual OS host wraps around existing navigation and provides:
 *   - Layer-based rendering on top of existing screens
 *   - Living background underneath everything
 *   - Dock rendering (mini player)
 *   - Overlay management
 *   - Transition choreography
 */
@Composable
fun ProvideVisualOs(
    coordinator: VisualOsCoordinator,
    content: @Composable () -> Unit
) {
    // Initialize coordinator with current composition scope
    val scope = rememberCoroutineScope()
    val deviceTier = rememberDeviceTier()

    DisposableEffect(coordinator) {
        coordinator.init(scope, deviceTier)
        onDispose {
            coordinator.dispose()
        }
    }

    // PERF FIX (real device crash/lag report): the background animation
    // loop previously had no concept of the app being backgrounded — it
    // ran a ~60fps state update forever once started, including while
    // the user was in a completely different app. This is the fix:
    // observe the real Activity lifecycle (standard AndroidX API, no new
    // dependency) and pause/resume the loop with it.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, coordinator) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> coordinator.pauseBackgroundAnimation()
                Lifecycle.Event.ON_START -> coordinator.resumeBackgroundAnimation()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CompositionLocalProvider(
        LocalVisualOsCoordinator provides coordinator,
        LocalVisualOsActive provides true
    ) {
        content()
    }
}

/**
 * Hook to access the Visual OS Coordinator from any composable.
 *
 * Usage:
 * ```kotlin
 * val coordinator = useVisualOs()
 * coordinator.navigateTo(RikoSceneType.NowPlaying)
 * ```
 */
@Composable
fun useVisualOs(): VisualOsCoordinator {
    return LocalVisualOsCoordinator.current
        ?: throw IllegalStateException("VisualOsCoordinator not provided. Wrap with ProvideVisualOs.")
}

/**
 * Hook to check if Visual OS is active.
 */
@Composable
fun isVisualOsActive(): Boolean {
    return LocalVisualOsActive.current
}
