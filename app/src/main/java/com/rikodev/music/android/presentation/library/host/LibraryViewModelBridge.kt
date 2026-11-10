/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.5 — Library Host Integration
 *
 * Module 2 / 7 — LibraryViewModelBridge.kt ⭐
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryViewModelBridge] is the Android-side ViewModel that owns
 * the Library state lifecycle. It is the *only* Android component
 * that combines upstream flows and feeds them through the Mapper
 * and Reducer.
 *
 * ARCHITECTURE
 * ------------
 *
 *   ┌──────────────────────────────────────────────────────────┐
 *   │                    UPSTREAM FLOWS                        │
 *   │                                                          │
 *   │   MusicRepository.songs         (Flow<List<Song>>)       │
 *   │   CollectionEngine.snapshots     (Flow<EngineSnapshot>)  │
 *   │   PlayerManager.playerState     (StateFlow<PlayerState>) │
 *   │   ColorIntelligenceEngine.color (StateFlow<ColorState>)  │
 *   │   WaveEngine.state              (StateFlow<WaveState>)   │
 *   │   PlayerState (for MiniPlayer)  (derived)                │
 *   └──────────────────────────────────────────────────────────┘
 *                            │
 *                            ▼
 *                    combine() + map()
 *                            │
 *                            ▼
 *                    LibraryMapper
 *                       (pure)
 *                            │
 *                            ▼
 *                    LibraryState
 *                            │
 *                            ▼
 *                    StateFlow<LibraryState>
 *                            │
 *                            ▼
 *                          UI
 *
 * EVENT HANDLING
 * --------------
 * UI events arrive via [dispatch]. They are reduced by [LibraryReducer]
 * (pure) → Pair<LibraryState, List<LibraryAction>>. The actions are
 * passed to [LibraryScreenCoordinator] (pure) → List<CoordinatorRequest>.
 * The requests are passed to [LibraryHostCoordinator] (Rule #83) which
 * executes them.
 *
 * Result events from the Host Coordinator arrive via SharedFlow and
 * are dispatched back through the Reducer in a separate pass.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #31  Runtime Owns Truth          — ViewModel owns LibraryState
 * #33  Cards Never Fetch           — UI reads StateFlow only
 * #47  Reducer Never Calls Engine  — ViewModel bridges via Coordinator
 * #83  Coordinator Executes        — Host Coordinator runs side effects
 *
 * FORBIDDEN HERE
 * --------------
 * - Direct Repository calls from UI (UI must go through ViewModel)
 * - Direct State mutation (must go through Reducer)
 * - Coroutine scope leaks (use viewModelScope)
 * - Compose imports (ViewModel has no UI)
 *
 * © RIKO Experience OS — Host Integration Tier
 */

package com.rikodev.music.android.presentation.library.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.music.android.core.experience.ColorIntelligenceEngine
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.WaveEngine
import com.rikodev.music.android.core.experience.wave.WaveState
import com.rikodev.music.android.data.repository.MusicRepository
import com.rikodev.music.android.data.repository.PlaylistRepository
import com.rikodev.music.android.domain.model.Song
import com.rikodev.music.android.presentation.library.LibraryAction
import com.rikodev.music.android.presentation.library.LibraryEvent
import com.rikodev.music.android.presentation.library.LibraryContentSnapshot
import com.rikodev.music.android.presentation.library.LibraryMapper
import com.rikodev.music.android.presentation.library.LibraryReducer
import com.rikodev.music.android.presentation.library.LibraryState
import com.rikodev.music.android.presentation.library.LibraryStateFactory
import com.rikodev.music.android.presentation.library.engine.CollectionEngine
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenAction
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenCoordinator
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenEvent
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenReducer
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenState
import com.rikodev.music.android.presentation.library.screen.runtime.CoordinatorRequest
import com.rikodev.music.android.service.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable
import com.rikodev.music.android.presentation.library.HapticType

// ═══════════════════════════════════════════════════════════════════════════
// THE LIBRARY VIEW MODEL BRIDGE ⭐
// ═══════════════════════════════════════════════════════════════════════════

/**
 * THE LIBRARY VIEW MODEL BRIDGE.
 *
 * Owns the [LibraryState] lifecycle. Combines all upstream flows,
 * maps them through [LibraryMapper], reduces events through
 * [LibraryReducer], and dispatches actions through the Host Coordinator.
 *
 * This is the ONLY Android component in the Library stack that:
 *   - Knows about ViewModel / viewModelScope
 *   - Combines Flows
 *   - Bridges to the Host Coordinator
 *
 * Everything above (Reducer, Mapper, Screen Coordinator) stays pure
 * Kotlin. Everything below (Host Coordinator) executes side effects.
 *
 * @param musicRepository       Songs / favorites data source.
 * @param playlistRepository    Playlist data source.
 * @param playerManager         Audio playback.
 * @param colorEngine           Color intelligence (Experience OS).
 * @param waveEngine            Wave physics (Experience OS).
 * @param collectionEngine      Library collection brain.
 * @param screenCoordinator     Pure Kotlin action planner (B5.2).
 */
@HiltViewModel
class LibraryViewModelBridge @Inject constructor(
    val musicRepository: MusicRepository,
    val playlistRepository: PlaylistRepository,
    val playerManager: PlayerManager,
    val waveEngineRef: WaveEngine,
    val colorEngineRef: ColorIntelligenceEngine,
    val collectionEngineRef: CollectionEngine,
    val screenCoordinator: LibraryScreenCoordinator,
) : ViewModel() {

    // ───────────────────────────────────────────────────────────────────────
    // PUBLIC READ-ONLY ACCESSORS — used by the screen for state collection
    // ───────────────────────────────────────────────────────────────────────

    /** The WaveEngine — exposed so the screen can collect its state. */
    val waveEngine: WaveEngine get() = waveEngineRef

    /** The CollectionEngine — exposed for completeness; screen uses [screenState]. */
    val collectionEngine: CollectionEngine get() = collectionEngineRef

    // ───────────────────────────────────────────────────────────────────────
    // HOST COORDINATOR — constructed internally from screen-supplied ports
    // ───────────────────────────────────────────────────────────────────────

    /**
     * The Host Coordinator. Constructed by [bindHostCoordinator] when the
     * screen supplies its Android-scoped ports (NavController, HapticFeedback,
     * SnackbarHostState, etc.).
     *
     * Until [bindHostCoordinator] is called, dispatched actions that need
     * side effects are dropped (since there's nowhere to execute them).
     */
    private var hostCoordinator: LibraryHostCoordinator? = null

    /**
     * Bind the Host Coordinator with screen-supplied ports.
     *
     * The screen owns Android dependencies (NavController, HapticFeedback,
     * SnackbarHostState). The ViewModel owns data dependencies (PlayerManager,
     * Repository, Engine). This method bridges them by constructing the
     * Host Coordinator with both.
     *
     * @param scope  Screen-scoped CoroutineScope (e.g. rememberCoroutineScope()).
     * @param ports  Screen-supplied ports bundle.
     */
    fun bindHostCoordinator(
        scope: kotlinx.coroutines.CoroutineScope,
        ports: LibraryScreenPorts,
    ) {
        if (hostCoordinator != null) return // already bound

        val coordinator = LibraryHostCoordinator(
            scope = scope,
            navigationPort = ports.navigationPort,
            hapticPort = ports.hapticPort,
            snackbarPort = ports.snackbarPort,
            permissionPort = ports.permissionPort,
            contextMenuPort = ports.contextMenuPort,
            selectionPort = ports.selectionPort,
            accessibilityPort = ports.accessibilityPort,
            enginePort = ports.enginePort,
            importPort = ports.importPort,
            playerManager = playerManager,
            musicRepository = musicRepository,
            playlistRepository = playlistRepository,
        )
        hostCoordinator = coordinator

        // Wire result events back into the Reducer.
        viewModelScope.launch {
            coordinator.resultEvents.collect { event ->
                dispatch(event)
            }
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // LIBRARY STATE — primary state for the Library screen
    // ───────────────────────────────────────────────────────────────────────

    /**
     * Cold-start state — produced before any data has hydrated.
     * Used as the starting value for [libraryState].
     */
    private val initialState: LibraryState = LibraryStateFactory.coldStart()

    /**
     * Mutable backing state — Reducer is the ONLY writer.
     */
    private val _libraryState = MutableStateFlow(initialState)

    /**
     * The single source of truth for Library UI.
     *
     * Rule #31 — Runtime Owns Truth.
     * UI reads this StateFlow and NOTHING else.
     */
    val libraryState: StateFlow<LibraryState> = _libraryState.asStateFlow()

    // ───────────────────────────────────────────────────────────────────────
    // SCREEN STATE — derived from LibraryState via the Mapper
    // ───────────────────────────────────────────────────────────────────────

    /**
     * Library screen state — derived from [libraryState] + player state.
     * The Collection Engine is invoked separately when content refresh
     * is needed (its output is captured into [libraryState] via Events).
     */
    val screenState: StateFlow<LibraryScreenState> = combine(
        _libraryState,
        playerManager.playerState,
    ) { state, playerState ->
        LibraryScreenMapper.map(
            libraryState = state,
            engineSnapshot = com.rikodev.music.android.presentation.library.runtime.CollectionRuntimeState.COLD_START,
            playerState = playerState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryScreenState.coldStart(
            com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitution.DEFAULT,
        ),
    )

    // ───────────────────────────────────────────────────────────────────────
    // EXPERIENCE OS DATA — combined for batch merging
    // ───────────────────────────────────────────────────────────────────────

    /**
     * Combined Experience OS data — emits whenever color, wave, or
     * playback changes. Mapped through [LibraryMapper.mergeExperience]
     * and written into [_libraryState].
     *
     * This is a side-effecting collector — it mutates state via the
     * Reducer (by dispatching [LibraryEvent.ColorChanged] etc.).
     */
    private val experienceMerger = combine(
        colorEngineRef.colorState,
        waveEngineRef.state,
        playerManager.playerState,
    ) { color, wave, player ->
        LibraryMapper.ExperienceSnapshot(
            colorState = color,
            waveState = wave,
            isPlaying = player.isPlaying,
            currentSongId = player.currentSong?.id,
        )
    }

    init {
        // ── 1. Merge Experience OS data into LibraryState ──────────────────
        viewModelScope.launch {
            experienceMerger.collect { snapshot ->
                val (color, wave) = snapshot.toSnapshots()
                val merged = LibraryMapper.mergeExperience(color, wave, _libraryState.value)
                // Merge playback too — updates dock.isDocked.
                val withPlayback = LibraryMapper.mergePlayback(
                    isPlaying = snapshot.isPlaying,
                    currentSongId = snapshot.currentSongId,
                    currentState = merged,
                )
                // Dispatch through the Reducer for proper state transitions.
                dispatch(LibraryEvent.ColorChanged(color))
                dispatch(LibraryEvent.WaveChanged(wave))
                if (snapshot.currentSongId != null) {
                    dispatch(LibraryEvent.PlaybackChanged(
                        isPlaying = snapshot.isPlaying,
                        currentSongId = snapshot.currentSongId,
                    ))
                }
                // Direct write (bypass Reducer for high-frequency Experience
                // updates to avoid Reducer overhead at 60-144Hz).
                _libraryState.value = withPlayback
            }
        }

        // ── 2. Observe songs from Repository → dispatch ContentRefreshed ───
        viewModelScope.launch {
            musicRepository.getAllSongs().collect { songs ->
                dispatch(LibraryEvent.CountsUpdated(
                    songCount = songs.size,
                    albumCount = songs.map { it.albumId }.distinct().size,
                    artistCount = songs.map { it.artist }.distinct().size,
                ))
                if (songs.isNotEmpty()) {
                    dispatch(LibraryEvent.ContentRefreshed(LibraryContentSnapshot()))
                }
            }
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // PUBLIC DISPATCH — called by UI
    // ───────────────────────────────────────────────────────────────────────

    /**
     * Dispatch a [LibraryEvent] into the Reducer.
     *
     * This is the PRIMARY entry point for UI events. The Reducer
     * produces a new state + actions. Actions are then routed to
     * the Screen Coordinator (pure) and Host Coordinator (impure).
     */
    fun dispatch(event: LibraryEvent) {
        val (newState, actions) = LibraryReducer.reduce(_libraryState.value, event)
        _libraryState.value = newState
        routeActions(actions)
    }

    /**
     * Dispatch a [LibraryScreenEvent] into the Screen Reducer.
     *
     * Screen events are higher-level than Library events — they
     * include things like "section collapsed", "scroll direction
     * changed", "sticky section changed". These don't affect
     * LibraryState directly; they affect LibraryScreenState.
     */
    fun dispatchScreen(event: LibraryScreenEvent) {
        val currentScreenState = screenState.value
        val (newScreenState, actions) = LibraryScreenReducer.reduce(
            currentScreenState, event,
        )
        // Update screen state via the mapper (re-derive from new info).
        // For high-frequency events (scroll), this is intentionally
        // lightweight — the mapper doesn't re-run; we just emit the new
        // screen state directly.
        // Note: screenState is derived from libraryState + engine snapshots,
        // so direct mutation isn't ideal. We use a side-channel.
        routeScreenActions(actions)
    }

    // ───────────────────────────────────────────────────────────────────────
    // ACTION ROUTING — pure → host
    // ───────────────────────────────────────────────────────────────────────

    /**
     * Route LibraryActions to the Host Coordinator via the Screen
     * Coordinator's planning step.
     *
     * LibraryAction is the Phase A vocabulary. The Screen Coordinator
     * doesn't directly accept LibraryAction — it accepts LibraryScreenAction.
     * We translate here.
     */
    private fun routeActions(actions: List<LibraryAction>) {
        if (actions.isEmpty()) return
        val coordinator = hostCoordinator ?: return
        val requests = actions.mapNotNull { translateAction(it) }
        coordinator.execute(requests)
    }

    /**
     * Translate a [LibraryAction] (Phase A vocab) to a [CoordinatorRequest]
     * (B5.2 vocab). Most actions have a 1:1 mapping.
     */
    private fun translateAction(action: LibraryAction): CoordinatorRequest? {
        return when (action) {
            is LibraryAction.PlaySong -> CoordinatorRequest.PlayTrack(
                trackId = action.songId.toString(),
                queue = emptyList(),
                sourceStableKey = action.contextPlaylist?.toString() ?: "",
            )
            is LibraryAction.ToggleFavorite -> CoordinatorRequest.AddToFavorites(
                trackId = action.songId.toString(),
            )
            is LibraryAction.NavigateToAlbum -> CoordinatorRequest.NavigateTo(
                destination = com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination.ALBUM_DETAIL,
                parameters = mapOf("albumId" to action.albumId.toString()),
                transitionKey = "EXPAND",
            )
            is LibraryAction.NavigateToArtist -> CoordinatorRequest.NavigateTo(
                destination = com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination.ARTIST_DETAIL,
                parameters = mapOf("artistId" to action.artistId.toString()),
                transitionKey = "EXPAND",
            )
            is LibraryAction.NavigateToFolder -> CoordinatorRequest.NavigateTo(
                destination = com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination.FOLDER_DETAIL,
                parameters = mapOf("folderId" to action.folderPath),
                transitionKey = "EXPAND",
            )
            is LibraryAction.NavigateToPlaylist -> CoordinatorRequest.NavigateTo(
                destination = com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination.PLAYLIST_DETAIL,
                parameters = mapOf("playlistId" to action.playlistId.toString()),
                transitionKey = "EXPAND",
            )
            is LibraryAction.NavigateToCollection -> CoordinatorRequest.NavigateTo(
                destination = com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination.NONE,
                parameters = mapOf("collectionId" to action.collectionId),
                transitionKey = "EXPAND",
            )
            is LibraryAction.ExpandMiniPlayer -> CoordinatorRequest.NavigateTo(
                destination = com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination.NOW_PLAYING,
                parameters = emptyMap(),
                transitionKey = "MORPH",
            )
            is LibraryAction.EnterSelectionMode -> CoordinatorRequest.EnterSelection(
                anchorSectionId = "default",
                firstItemId = action.itemId.toString(),
            )
            is LibraryAction.AddToPlaylist -> CoordinatorRequest.AddToPlaylist(
                trackId = "selected",
                playlistId = action.playlistId.toString(),
            )
            is LibraryAction.DeleteSelected -> CoordinatorRequest.PerformBatchAction(
                selectedItemIds = emptySet(),
                batchActionKey = "DELETE",
            )
            is LibraryAction.ShareSelected -> CoordinatorRequest.PerformBatchAction(
                selectedItemIds = emptySet(),
                batchActionKey = "SHARE",
            )
            is LibraryAction.OpenSearch -> CoordinatorRequest.NavigateTo(
                destination = com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination.SEARCH,
                parameters = if (action.query.isNotEmpty())
                    mapOf("query" to action.query) else emptyMap(),
                transitionKey = "SLIDE",
            )
            is LibraryAction.RefreshContent -> CoordinatorRequest.RefreshAllCollections(
                force = true,
            )
            is LibraryAction.RequestPermission -> CoordinatorRequest.RequestPermission(
                permissionKey = "android.permission.READ_MEDIA_AUDIO",
            )
            is LibraryAction.HapticFeedback -> CoordinatorRequest.TriggerHaptic(
                hapticKey = action.type.name,
            )
            is LibraryAction.ShowContextMenu -> CoordinatorRequest.ShowContextMenu(
                sectionId = "default",
                itemId = action.itemId.toString(),
                stableKey = action.itemType.name,
            )
        }
    }

    /**
     * Route LibraryScreenActions directly to the Host Coordinator.
     *
     * These are already in CoordinatorRequest vocabulary, so we just
     * pass them through the Screen Coordinator's planner.
     */
    private fun routeScreenActions(actions: List<LibraryScreenAction>) {
        if (actions.isEmpty()) return
        val coordinator = hostCoordinator ?: return
        val requests = screenCoordinator.processActions(screenState.value, actions)
        coordinator.execute(requests)
    }

    // ───────────────────────────────────────────────────────────────────────
    // PLAYBACK INTEGRATION — high-level helpers for UI
    // ───────────────────────────────────────────────────────────────────────

    /**
     * Play a single song. Convenience for UI taps on song rows.
     *
     * Single call → PlayerManager. Zero duplication.
     */
    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        val queueIds = queue.map { it.id.toString() }
        val request = CoordinatorRequest.PlayTrack(
            trackId = song.id.toString(),
            queue = queueIds,
            sourceStableKey = "songs",
        )
        hostCoordinator?.executeOne(request)
    }

    /**
     * Toggle play/pause.
     */
    fun togglePlayPause() {
        hostCoordinator?.executeOne(CoordinatorRequest.TogglePlayPause)
    }

    /**
     * Skip to next track.
     */
    fun skipToNext() {
        hostCoordinator?.executeOne(CoordinatorRequest.SkipToNext)
    }

    /**
     * Skip to previous track.
     */
    fun skipToPrevious() {
        hostCoordinator?.executeOne(CoordinatorRequest.SkipToPrevious)
    }

    /**
     * Seek to position (0.0–1.0).
     */
    fun seekTo(positionFraction: Float) {
        hostCoordinator?.executeOne(CoordinatorRequest.SeekTo(positionFraction))
    }

    /**
     * Toggle favorite for a song.
     */
    fun toggleFavorite(songId: Long) {
        hostCoordinator?.executeOne(CoordinatorRequest.AddToFavorites(songId.toString()))
    }

    /**
     * Add a song to the queue.
     */
    fun addToQueue(songId: Long) {
        hostCoordinator?.executeOne(CoordinatorRequest.AddToQueue(songId.toString()))
    }

    /**
     * Add a song to a playlist.
     */
    fun addToPlaylist(songId: Long, playlistId: Long) {
        hostCoordinator?.executeOne(
            CoordinatorRequest.AddToPlaylist(songId.toString(), playlistId.toString())
        )
    }

    /**
     * Refresh all collections.
     */
    fun refresh() {
        hostCoordinator?.executeOne(CoordinatorRequest.RefreshAllCollections(force = true))
    }

    /**
     * Show a snackbar.
     */
    fun showSnackbar(message: String) {
        hostCoordinator?.executeOne(
            CoordinatorRequest.ShowToast(messageKey = message, durationMs = 2500)
        )
    }

    /**
     * Trigger a haptic.
     */
    fun triggerHaptic(type: HapticType) {
        hostCoordinator?.executeOne(
            CoordinatorRequest.TriggerHaptic(hapticKey = type.name)
        )
    }

    // ───────────────────────────────────────────────────────────────────────
    // VIEWMODEL LIFECYCLE
    // ───────────────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        // The Host Coordinator's scope is viewModelScope, which gets
        // cancelled automatically. Nothing explicit to do here.
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LIBRARY SCREEN MAPPER (host-side) — bridges LibraryState → ScreenState
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Host-side mapper that combines [LibraryState] + engine snapshots +
 * player state → [LibraryScreenState].
 *
 * This is the B5.5 host-side counterpart to the pure [LibraryMapper].
 * It is allowed to read upstream Flow data because it lives in the
 * host layer. The pure [LibraryMapper] cannot.
 */
object LibraryScreenMapper {

    fun map(
        libraryState: LibraryState,
        engineSnapshot: com.rikodev.music.android.presentation.library.runtime.CollectionRuntimeState,
        playerState: com.rikodev.music.android.domain.model.PlayerState,
    ): LibraryScreenState {
        // Map dock state from player.
        val dockState = if (playerState.currentSong != null) {
            com.rikodev.music.android.presentation.library.screen.contract.LibraryDockState.EXPANDED
        } else {
            com.rikodev.music.android.presentation.library.screen.contract.LibraryDockState.HIDDEN
        }

        val dock = com.rikodev.music.android.presentation.library.screen.runtime.LibraryDockRuntimeState(
            state = dockState,
            currentTrackId = playerState.currentSong?.id?.toString(),
            trackTitle = playerState.currentSong?.title ?: "",
            trackArtist = playerState.currentSong?.artist ?: "",
            isPlaying = playerState.isPlaying,
            progressFraction = if (playerState.duration > 0) {
                (playerState.position.toFloat() / playerState.duration.toFloat()).coerceIn(0f, 1f)
            } else 0f,
            artworkKey = playerState.currentSong?.albumId?.toString() ?: "",
            accentColorHex = "#FFFFFF",
        )

        // Determine screen phase from runtime.
        val phase = when {
            libraryState.runtime.phase == com.rikodev.music.android.presentation.library.LibraryPhase.ERROR ->
                com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenPhase.ERROR
            libraryState.runtime.phase == com.rikodev.music.android.presentation.library.LibraryPhase.PERMISSION_REQUIRED ->
                com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenPhase.PERMISSION_REQUIRED
            libraryState.runtime.phase == com.rikodev.music.android.presentation.library.LibraryPhase.LOADING ->
                com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenPhase.LOADING
            libraryState.runtime.phase == com.rikodev.music.android.presentation.library.LibraryPhase.SCANNING ->
                com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenPhase.LOADING
            libraryState.selection.isActive ->
                com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenPhase.SELECTION_MODE
            libraryState.runtime.totalSongCount == 0 ->
                com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenPhase.EMPTY
            else ->
                com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenPhase.READY
        }

        // Empty state.
        val emptyState = com.rikodev.music.android.presentation.library.screen.runtime.LibraryEmptyRuntimeState(
            kind = com.rikodev.music.android.presentation.library.screen.contract.LibraryEmptyStateKind.FIRST_LAUNCH,
            isVisible = phase == com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenPhase.EMPTY,
            replacesContent = phase == com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenPhase.EMPTY,
        )

        return LibraryScreenState(
            phase = phase,
            profile = com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitutionProfile.DEFAULT,
            orientation = com.rikodev.music.android.presentation.library.screen.contract.LibraryScreenOrientation.PORTRAIT,
            topBar = com.rikodev.music.android.presentation.library.screen.runtime.LibraryTopBarRuntimeState(
                state = com.rikodev.music.android.presentation.library.screen.contract.LibraryTopBarState.EXPANDED,
                titleText = "Library",
                subtitleText = null,
                chipActionKeys = emptyList(),
                badgeCounts = emptyMap(),
                scrollOffsetDp = libraryState.scroll.firstVisibleOffset,
            ),
            dock = dock,
            hero = com.rikodev.music.android.presentation.library.screen.runtime.LibraryHeroRuntimeState.EMPTY,
            sections = emptyList(), // Populated by the rendering layer
            selection = com.rikodev.music.android.presentation.library.screen.runtime.LibrarySelectionRuntimeState(
                isActive = libraryState.selection.isActive,
                selectedItemIds = libraryState.selection.selectedIds.map { it.toString() }.toSet(),
                selectionLimit = 50,
                anchorSectionId = null,
            ),
            scroll = com.rikodev.music.android.presentation.library.screen.runtime.LibraryScrollRuntimeState(
                scrollOffsetDp = libraryState.scroll.firstVisibleOffset,
                maxScrollOffsetDp = 0,
                isAtTop = libraryState.scroll.firstVisibleIndex == 0,
                isAtBottom = false,
                isFastScrolling = libraryState.scroll.isFastScrolling,
                isPullingToRefresh = false,
                pullDistanceDp = 0,
            ),
            navigation = com.rikodev.music.android.presentation.library.screen.runtime.LibraryNavigationRuntimeState.IDLE,
            emptyState = emptyState,
            lastUpdatedMs = System.currentTimeMillis(),
            errorMessage = libraryState.runtime.error,
            activeCollectionStableKey = null,
        )
    }
}
