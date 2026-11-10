/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.5 — Library Host Integration
 *
 * Module 6 / 7 — LibraryNavigationRuntime.kt ⭐
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryNavigationRuntime] is the canonical routing layer for all
 * Library-originated navigation. Every destination the user can reach
 * FROM the Library is declared here as a [LibraryRoute], with its
 * parameters, transition, and back-stack behavior.
 *
 * The Library never calls NavController directly. Instead, it emits
 * a [LibraryNavigationDestination] (B5.1 contract), which the
 * LibraryScreenCoordinator translates to a [CoordinatorRequest.NavigateTo],
 * which the LibraryHostCoordinator executes via the [LibraryNavigationPort].
 *
 * This file is the *registry* — it owns the mapping from abstract
 * destinations to concrete routes + parameters + transitions.
 *
 * ARCHITECTURE
 * ------------
 *
 *   User taps "Album X"
 *       │
 *       ▼
 *   LibraryEvent.TapAlbum(albumId=42)
 *       │
 *       ▼
 *   LibraryReducer (pure)
 *       │
 *       ▼
 *   LibraryAction.NavigateToAlbum(42)
 *       │
 *       ▼
 *   LibraryViewModelBridge.translateAction()
 *       │
 *       ▼
 *   CoordinatorRequest.NavigateTo(
 *       destination = ALBUM_DETAIL,
 *       parameters = {"albumId": "42"},
 *       transitionKey = "EXPAND"
 *   )
 *       │
 *       ▼
 *   LibraryHostCoordinator.dispatch()
 *       │
 *       ▼
 *   LibraryNavigationPort.navigate()
 *       │
 *       ▼
 *   NavControllerNavigationPort.buildRoute()
 *       │
 *       ▼
 *   navController.navigate("album/42")
 *
 * ROUTES
 * ------
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │ DESTINATION          │ ROUTE                  │ TRANSITION   │
 *   ├──────────────────────────────────────────────────────────────┤
 *   │ NONE                 │ library                │ —            │
 *   │ SEARCH               │ search                 │ SLIDE        │
 *   │ NOW_PLAYING          │ nowPlaying             │ MORPH        │
 *   │ ALBUM_DETAIL         │ album/{albumId}        │ EXPAND       │
 *   │ ARTIST_DETAIL        │ artist/{artistId}      │ EXPAND       │
 *   │ PLAYLIST_DETAIL      │ playlist/{playlistId}  │ EXPAND       │
 *   │ GENRE_DETAIL         │ genre/{genreId}        │ EXPAND       │
 *   │ FOLDER_DETAIL        │ folder/{folderId}      │ EXPAND       │
 *   │ SETTINGS             │ settings               │ SLIDE        │
 *   │ QUEUE                │ queue                  │ SLIDE        │
 *   │ LYRICS               │ lyrics                 │ MORPH        │
 *   │ EQUALIZER            │ equalizer              │ SLIDE        │
 *   │ BACK                 │ (pop)                  │ SLIDE        │
 *   └──────────────────────────────────────────────────────────────┘
 *
 * RULES ENFORCED HERE
 * -------------------
 * #44  Stable Key Never Changes   — route keys are stable strings
 * #48  Actions Describe Intent   — navigation is explicit
 * #54  Screen Tells a Story      — transitions extend the story
 * #72  Identity Before Feature   — each route has a distinct identity
 * #83  Coordinator Executes      — actual nav via Host Coordinator
 *
 * © RIKO Experience OS — Host Integration Tier
 */

package com.rikodev.music.android.presentation.library.host

import com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination
import com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationTransition
import com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationTransitionKind

// ═══════════════════════════════════════════════════════════════════════════
// LIBRARY ROUTE — the canonical routing record
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A single Library route.
 *
 * Combines a [LibraryNavigationDestination] with its concrete route
 * template, parameter keys, and transition. The Host Coordinator
 * uses this to build the actual nav route.
 *
 * @property destination      The abstract destination (B5.1 contract).
 * @property routeTemplate    The concrete route template (e.g. "album/{albumId}").
 * @property parameterKeys    Ordered list of parameter keys the route accepts.
 * @property transition       The transition to use (MORPH / SLIDE / EXPAND / etc).
 * @property isTopLevel       True if this is a top-level destination (no back).
 * @property requiresArg      True if the route requires at least one argument.
 */
data class LibraryRoute(
    val destination: LibraryNavigationDestination,
    val routeTemplate: String,
    val parameterKeys: List<String>,
    val transition: LibraryNavigationTransition,
    val isTopLevel: Boolean,
    val requiresArg: Boolean,
) {
    /**
     * Build the concrete route string from parameters.
     *
     * Replaces `{key}` placeholders with values from [params].
     * Missing parameters are left as-is (will fail at NavController).
     */
    fun buildRoute(params: Map<String, String>): String {
        var result = routeTemplate
        for (key in parameterKeys) {
            val value = params[key]
            if (value != null) {
                result = result.replace("{$key}", value)
            }
        }
        return result
    }

    /** True if all required parameters are present in [params]. */
    fun hasAllRequiredParams(params: Map<String, String>): Boolean {
        if (!requiresArg) return true
        return parameterKeys.all { it in params }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LIBRARY ROUTE REGISTRY — the master routing table
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The master registry of all Library routes.
 *
 * Each [LibraryNavigationDestination] maps to exactly one [LibraryRoute].
 * The mapping is stable — Rule #44.
 */
object LibraryRouteRegistry {

    /** All known Library routes, keyed by destination. */
    val routes: Map<LibraryNavigationDestination, LibraryRoute> = mapOf(
        LibraryNavigationDestination.NONE to LibraryRoute(
            destination = LibraryNavigationDestination.NONE,
            routeTemplate = "library",
            parameterKeys = emptyList(),
            transition = LibraryNavigationTransition.FADE,
            isTopLevel = true,
            requiresArg = false,
        ),
        LibraryNavigationDestination.SEARCH to LibraryRoute(
            destination = LibraryNavigationDestination.SEARCH,
            routeTemplate = "search",
            parameterKeys = emptyList(),
            transition = LibraryNavigationTransition.SLIDE,
            isTopLevel = true,
            requiresArg = false,
        ),
        LibraryNavigationDestination.NOW_PLAYING to LibraryRoute(
            destination = LibraryNavigationDestination.NOW_PLAYING,
            routeTemplate = "nowPlaying",
            parameterKeys = emptyList(),
            transition = LibraryNavigationTransition.MORPH,
            isTopLevel = false,
            requiresArg = false,
        ),
        LibraryNavigationDestination.ALBUM_DETAIL to LibraryRoute(
            destination = LibraryNavigationDestination.ALBUM_DETAIL,
            routeTemplate = "album/{albumId}",
            parameterKeys = listOf("albumId"),
            transition = LibraryNavigationTransition.EXPAND,
            isTopLevel = false,
            requiresArg = true,
        ),
        LibraryNavigationDestination.ARTIST_DETAIL to LibraryRoute(
            destination = LibraryNavigationDestination.ARTIST_DETAIL,
            routeTemplate = "artist/{artistId}",
            parameterKeys = listOf("artistId"),
            transition = LibraryNavigationTransition.EXPAND,
            isTopLevel = false,
            requiresArg = true,
        ),
        LibraryNavigationDestination.PLAYLIST_DETAIL to LibraryRoute(
            destination = LibraryNavigationDestination.PLAYLIST_DETAIL,
            routeTemplate = "playlist/{playlistId}",
            parameterKeys = listOf("playlistId"),
            transition = LibraryNavigationTransition.EXPAND,
            isTopLevel = false,
            requiresArg = true,
        ),
        LibraryNavigationDestination.GENRE_DETAIL to LibraryRoute(
            destination = LibraryNavigationDestination.GENRE_DETAIL,
            routeTemplate = "genre/{genreId}",
            parameterKeys = listOf("genreId"),
            transition = LibraryNavigationTransition.EXPAND,
            isTopLevel = false,
            requiresArg = true,
        ),
        LibraryNavigationDestination.FOLDER_DETAIL to LibraryRoute(
            destination = LibraryNavigationDestination.FOLDER_DETAIL,
            routeTemplate = "folder/{folderId}",
            parameterKeys = listOf("folderId"),
            transition = LibraryNavigationTransition.EXPAND,
            isTopLevel = false,
            requiresArg = true,
        ),
        LibraryNavigationDestination.SETTINGS to LibraryRoute(
            destination = LibraryNavigationDestination.SETTINGS,
            routeTemplate = "settings",
            parameterKeys = emptyList(),
            transition = LibraryNavigationTransition.SLIDE,
            isTopLevel = true,
            requiresArg = false,
        ),
        LibraryNavigationDestination.QUEUE to LibraryRoute(
            destination = LibraryNavigationDestination.QUEUE,
            routeTemplate = "queue",
            parameterKeys = emptyList(),
            transition = LibraryNavigationTransition.SLIDE,
            isTopLevel = false,
            requiresArg = false,
        ),
        LibraryNavigationDestination.LYRICS to LibraryRoute(
            destination = LibraryNavigationDestination.LYRICS,
            routeTemplate = "lyrics",
            parameterKeys = emptyList(),
            transition = LibraryNavigationTransition.MORPH,
            isTopLevel = false,
            requiresArg = false,
        ),
        LibraryNavigationDestination.EQUALIZER to LibraryRoute(
            destination = LibraryNavigationDestination.EQUALIZER,
            routeTemplate = "equalizer",
            parameterKeys = emptyList(),
            transition = LibraryNavigationTransition.SLIDE,
            isTopLevel = false,
            requiresArg = false,
        ),
        LibraryNavigationDestination.BACK to LibraryRoute(
            destination = LibraryNavigationDestination.BACK,
            routeTemplate = "library",
            parameterKeys = emptyList(),
            transition = LibraryNavigationTransition.SLIDE,
            isTopLevel = false,
            requiresArg = false,
        ),
    )

    /** Look up the route for a destination. */
    fun routeFor(destination: LibraryNavigationDestination): LibraryRoute? =
        routes[destination]

    /** All registered destinations. */
    val allDestinations: Set<LibraryNavigationDestination> get() = routes.keys

    /** All top-level destinations. */
    val topLevelDestinations: Set<LibraryNavigationDestination>
        get() = routes.filter { it.value.isTopLevel }.keys

    /** All destinations that require an argument. */
    val argumentDestinations: Set<LibraryNavigationDestination>
        get() = routes.filter { it.value.requiresArg }.keys
}

// ═══════════════════════════════════════════════════════════════════════════
// LIBRARY NAVIGATION RUNTIME — the executor-side helper
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Library Navigation Runtime.
 *
 * A thin helper that the [LibraryNavigationPort] implementation can
 * use to resolve destinations to concrete routes. Most of the work
 * is in [LibraryRouteRegistry]; this class adds:
 *
 *   1. Transition validation (warns on missing transitions)
 *   2. Parameter validation (warns on missing required params)
 *   3. Back-stack rule enforcement
 *   4. Navigation logging (for diagnostics, Rule #21 — Traceable)
 *
 * @property navLog  Optional callback for logging each navigation.
 */
class LibraryNavigationRuntime(
    private val navLog: ((String) -> Unit)? = null,
) {

    /**
     * Resolve a destination + params to a concrete route string.
     *
     * Returns null if:
     *   - The destination is unknown
     *   - A required parameter is missing
     *
     * In those cases, a warning is logged (if [navLog] is set).
     */
    fun resolveRoute(
        destination: LibraryNavigationDestination,
        params: Map<String, String>,
    ): String? {
        val route = LibraryRouteRegistry.routeFor(destination) ?: run {
            navLog?.invoke("WARN: Unknown destination $destination")
            return null
        }

        if (!route.hasAllRequiredParams(params)) {
            navLog?.invoke(
                "WARN: Missing required params for $destination. " +
                "Required: ${route.parameterKeys}, got: ${params.keys}"
            )
            return null
        }

        val concrete = route.buildRoute(params)
        navLog?.invoke("NAV: $destination → $concrete (transition=${route.transition.kind})")
        return concrete
    }

    /**
     * Resolve the transition for a destination.
     */
    fun transitionFor(destination: LibraryNavigationDestination): LibraryNavigationTransition {
        return LibraryRouteRegistry.routeFor(destination)?.transition
            ?: LibraryNavigationTransition.FADE
    }

    /**
     * Determine if a back-press should:
     *   - Clear selection first (if selection mode active)
     *   - Collapse dock first (if dock expanded)
     *   - Close context menu first (if open)
     *   - Or actually pop the back stack
     *
     * Rule: back press resolves in priority order. The first matching
     * condition wins. If none match, the back stack pops.
     */
    fun resolveBackPress(
        isSelectionModeActive: Boolean,
        isDockExpanded: Boolean,
        isContextMenuOpen: Boolean,
    ): BackPressAction {
        return when {
            isContextMenuOpen -> BackPressAction.CLOSE_CONTEXT_MENU
            isSelectionModeActive -> BackPressAction.CLEAR_SELECTION
            isDockExpanded -> BackPressAction.COLLAPSE_DOCK
            else -> BackPressAction.POP_BACK_STACK
        }
    }
}

/**
 * The action to take on a back-press.
 */
enum class BackPressAction {
    /** Close any open context menu. */
    CLOSE_CONTEXT_MENU,

    /** Clear multi-selection mode. */
    CLEAR_SELECTION,

    /** Collapse an expanded dock. */
    COLLAPSE_DOCK,

    /** Pop the back stack (default). */
    POP_BACK_STACK,
}

// ═══════════════════════════════════════════════════════════════════════════
// NAVIGATION DIAGNOSTICS — verifies all routes are reachable
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Diagnostics for the Library navigation runtime.
 *
 * Verifies:
 *   1. Every destination in [LibraryNavigationDestination] has a route
 *   2. Every route's transition is one of the defined kinds
 *   3. Every route with `requiresArg = true` has at least one parameterKey
 *   4. Every parameterKey has a matching `{key}` placeholder in routeTemplate
 *
 * Used by [LibraryValidationGate] (Module 7).
 */
object LibraryNavigationDiagnostics {

    /** All navigation invariants. */
    val invariants = listOf(
        NavigationInvariant("NAV-1", "Every destination has a route") {
            LibraryNavigationDestination.values().all { dest ->
                LibraryRouteRegistry.routeFor(dest) != null
            }
        },
        NavigationInvariant("NAV-2", "Every route's transition is defined") {
            LibraryRouteRegistry.routes.values.all { route ->
                route.transition.kind in LibraryNavigationTransitionKind.values().toSet()
            }
        },
        NavigationInvariant("NAV-3", "Routes with requiresArg have parameterKeys") {
            LibraryRouteRegistry.routes.values.all { route ->
                !route.requiresArg || route.parameterKeys.isNotEmpty()
            }
        },
        NavigationInvariant("NAV-4", "Every parameterKey has a placeholder in routeTemplate") {
            LibraryRouteRegistry.routes.values.all { route ->
                route.parameterKeys.all { key ->
                    "{$key}" in route.routeTemplate
                }
            }
        },
        NavigationInvariant("NAV-5", "Every route has a non-empty transitionKey") {
            LibraryRouteRegistry.routes.values.all { route ->
                route.transition.kind.name.isNotEmpty()
            }
        },
    )

    /** True if all invariants hold. */
    fun allPassed(): Boolean = invariants.all { it.verify() }

    /** Generate a human-readable report. */
    fun report(): String {
        val sb = StringBuilder()
        sb.appendLine("=== Library Navigation Diagnostics ===")
        for (inv in invariants) {
            val status = if (inv.verify()) "PASS" else "FAIL"
            sb.appendLine("[$status] ${inv.id}: ${inv.description}")
        }
        sb.appendLine()
        sb.appendLine("=== Routes ===")
        for ((dest, route) in LibraryRouteRegistry.routes) {
            sb.appendLine("$dest → ${route.routeTemplate} (${route.transition.kind})")
        }
        return sb.toString()
    }
}

/**
 * A single navigation invariant.
 */
data class NavigationInvariant(
    val id: String,
    val description: String,
    val verify: () -> Boolean,
)

// ═══════════════════════════════════════════════════════════════════════════
// DEEP LINK RESOLVER — handles riko:// library deep links
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Resolver for riko://library deep links.
 *
 * Used by the [LibraryNavigationPort.openDeepLink] implementation
 * to translate deep link URIs into destination + params.
 *
 * Supported deep links:
 *   - riko://library/album?albumId=42     → ALBUM_DETAIL, {albumId: 42}
 *   - riko://library/artist?artistId=42   → ARTIST_DETAIL, {artistId: 42}
 *   - riko://library/playlist?playlistId=1 → PLAYLIST_DETAIL, {playlistId: 1}
 *   - riko://library/search?query=foo     → SEARCH, {query: foo}
 */
object LibraryDeepLinkResolver {

    /**
     * Resolve a deep link URI to a destination + params.
     *
     * Returns null if the URI is not a recognized Library deep link.
     */
    fun resolve(uri: String): Pair<LibraryNavigationDestination, Map<String, String>>? {
        if (!uri.startsWith("riko://library/")) return null

        val path = uri.removePrefix("riko://library/")
        val queryStart = path.indexOf('?')
        val pathPart = if (queryStart >= 0) path.substring(0, queryStart) else path
        val queryPart = if (queryStart >= 0) path.substring(queryStart + 1) else ""

        val destination = when (pathPart) {
            "album" -> LibraryNavigationDestination.ALBUM_DETAIL
            "artist" -> LibraryNavigationDestination.ARTIST_DETAIL
            "playlist" -> LibraryNavigationDestination.PLAYLIST_DETAIL
            "search" -> LibraryNavigationDestination.SEARCH
            else -> return null
        }

        val params = if (queryPart.isEmpty()) {
            emptyMap()
        } else {
            queryPart.split("&").mapNotNull { pair ->
                val idx = pair.indexOf('=')
                if (idx > 0) pair.substring(0, idx) to pair.substring(idx + 1)
                else null
            }.toMap()
        }

        return destination to params
    }
}
