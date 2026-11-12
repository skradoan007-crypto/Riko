package com.rikodev.music.android.dna.data
import com.rikodev.music.android.service.PlayerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
class DnaListeningTracker @Inject constructor(private val dnaListening: DnaListening) {
 fun start(manager: PlayerManager, scope: CoroutineScope) {
        // Count elapsed audible time, never seek distance or paused wall time.
        scope.launch {
            manager.playerState.map { it.currentSong }.distinctUntilChanged { a, b -> a?.id == b?.id }
                .collectLatest { song ->
                    if (song == null) return@collectLatest
                    // Preserve immediate Recently Played entries; zero duration does not evolve DNA.
                    var row = try { withContext(Dispatchers.IO) { dnaListening.save(song, 0L, 0L) } }
                    catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { 0L }
                    var listened = 0L
                    var saved = 0L
                    var last = android.os.SystemClock.elapsedRealtime()
                    var wasPlaying = manager.playerState.value.isPlaying
                    var position = manager.playerState.value.position
                    while (true) {
                        delay(1000)
                        val now = android.os.SystemClock.elapsedRealtime()
                        val state = manager.playerState.value
                        if (wasPlaying && state.isPlaying && !state.isLoading)
                            listened += (now - last).coerceIn(0L, 1500L)
                        last = now; wasPlaying = state.isPlaying
                        if (listened - saved >= 5000) {
                            try { row = withContext(Dispatchers.IO) { dnaListening.save(song, row, listened) }; saved = listened }
                            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                            catch (_: Exception) { /* Playback survives unavailable history storage. */ }
                        }
                        if (position > song.duration - 5000 && state.position < 1000) {
                            row = 0L; listened = 0L; saved = 0L
                        }
                        position = state.position
                    }
                }
        }


 }
}
