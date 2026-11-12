package com.rikodev.music.android.dna.data

import androidx.room.withTransaction
import com.rikodev.music.android.data.db.RikoDatabase
import com.rikodev.music.android.data.db.entity.PlayHistoryEntity
import com.rikodev.music.android.domain.model.Song
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

fun Song.dnaKey(): String = MessageDigest.getInstance("SHA-256")
    .digest("$uri|$size|$dateModified".toByteArray(Charsets.UTF_8))
    .joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
fun Song.meaningfulMs(): Long = (duration / 2).coerceIn(30_000L, 240_000L)

/** Duration and evolution cross the threshold in one transaction, exactly once per row. */
@Singleton
class DnaListening @Inject constructor(private val db: RikoDatabase) {
    suspend fun save(song: Song, id: Long, listenedMs: Long): Long = db.withTransaction {
        val dao = db.playHistoryDao()
        if (id == 0L) dao.clearOldHistory(System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000)
        val old = if (id > 0) dao.duration(id) ?: 0L else 0L
        val row = if (id == 0L) dao.insertHistory(PlayHistoryEntity(songId = song.id, playDuration = listenedMs))
            else { dao.updateDuration(id, listenedMs); id }
        if (old < song.meaningfulMs() && listenedMs >= song.meaningfulMs()) db.dnaProfileDao().incrementGeneration(song.dnaKey())
        row
    }
}
