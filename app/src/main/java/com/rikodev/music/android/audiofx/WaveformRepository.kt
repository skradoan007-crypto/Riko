package com.rikodev.music.android.audiofx

import android.content.Context
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.rikodev.music.android.data.db.dao.WaveformDao
import com.rikodev.music.android.data.db.entity.WaveformCacheEntity
import com.rikodev.music.android.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed class WaveformState {
    data object Idle : WaveformState()
    data class Analyzing(val songId: Long) : WaveformState()
    data class Ready(val songId: Long, val data: WaveformData) : WaveformState()
    data class Unavailable(val songId: Long, val reason: String) : WaveformState()
}

/** One canonical offline-waveform owner. All metadata/Room/decoding work is off UI.
 * Generation checks are atomic with publication: cancelled old work can NEVER
 * overwrite a new track, even if a provider ignores coroutine cancellation.
 */
@Singleton
class WaveformRepository @Inject constructor(
    private val waveformDao: WaveformDao,
    private val audioFileAnalyzer: AudioFileAnalyzer,
    @ApplicationContext private val context: Context
) {
    private val _state = MutableStateFlow<WaveformState>(WaveformState.Idle)
    val state: StateFlow<WaveformState> = _state.asStateFlow()
    private val lock = Any()
    private var generation = 0L
    private var analysisJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val memory = LinkedHashMap<String, WaveformData>(16, 0.75f, true)

    fun loadWaveformFor(song: Song?) {
        synchronized(lock) {
            analysisJob?.cancel()
            val request = ++generation
            _state.value = if (song == null) WaveformState.Idle else WaveformState.Analyzing(song.id)
            if (song == null) return
            analysisJob = scope.launch {
                try {
                    val identity = identityFor(song)
                    currentCoroutineContext().ensureActive()
                    val resident = synchronized(lock) { memory[identity] }
                    val cached = resident ?: cacheOperation { waveformDao.getWaveform(song.id) }
                        ?.let { WaveformCacheCodec.decode(identity, it.bucketsCsv) }
                    val result = cached ?: audioFileAnalyzer.analyze(context, song.uri, durationHintMs = song.duration)
                    currentCoroutineContext().ensureActive()
                    if (result != null && cached == null && identityFor(song) != identity) {
                        synchronized(lock) {
                            if (request == generation) _state.value = WaveformState.Unavailable(song.id, "Audio file changed during analysis")
                        }
                        return@launch
                    }
                    synchronized(lock) {
                        if (request != generation) return@launch
                        if (result != null) {
                            memory[identity] = result
                            while (memory.size > 12) memory.remove(memory.keys.first())
                            _state.value = WaveformState.Ready(song.id, result)
                        } else {
                            _state.value = WaveformState.Unavailable(song.id, "Audio analysis unavailable")
                        }
                    }
                    if (result != null && cached == null) {
                        cacheOperation {
                            waveformDao.saveWaveform(WaveformCacheEntity(song.id, WaveformCacheCodec.encode(identity, result)))
                            waveformDao.trimToNewest(128)
                        }
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    synchronized(lock) {
                        if (request == generation) _state.value = WaveformState.Unavailable(song.id, "Audio analysis unavailable")
                    }
                }
            }
        }
    }

    private suspend fun <T> cacheOperation(block: suspend () -> T): T? = try { block() }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { null } // A broken/unwritable cache cannot break playback or fresh analysis.

    private fun identityFor(song: Song): String {
        val file = File(song.path)
        var size = song.size
        var modified = song.dateModified
        try {
            context.contentResolver.query(song.uri, arrayOf(OpenableColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED),
                null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val s = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val m = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                    if (s >= 0 && !cursor.isNull(s)) size = cursor.getLong(s)
                    if (m >= 0 && !cursor.isNull(m)) modified = cursor.getLong(m)
                }
            }
        } catch (_: Exception) { /* SAF providers may not expose modification time. */ }
        val source = listOf(WaveformCacheCodec.VERSION, song.id, song.uri, size, modified,
            song.duration, runCatching { file.length() }.getOrDefault(-1L),
            runCatching { file.lastModified() }.getOrDefault(-1L)).joinToString("\n")
        return MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
            .joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
    }
}
