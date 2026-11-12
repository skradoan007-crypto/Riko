package com.rikodev.music.android.dna.data

import android.app.ActivityManager
import android.app.job.*
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.rikodev.music.android.data.repository.MusicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** One resumable batch, one decoder (shared PcmSource mutex), no playback ownership. */
@AndroidEntryPoint
class DnaAnalysisJob : JobService() {
    @Inject lateinit var music: MusicRepository
    @Inject lateinit var dna: DnaRepository
    private var task: Job? = null
    override fun onStartJob(params: JobParameters): Boolean {
        task = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            val monitor = launch {
                while (true) {
                    delay(5000)
                    val power = getSystemService(POWER_SERVICE) as PowerManager
                    val memory = ActivityManager.MemoryInfo()
                    (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
                    if (memory.lowMemory || power.isPowerSaveMode || (Build.VERSION.SDK_INT >= 29 && power.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE)) {
                        jobFinished(params, true); task?.cancel(); break
                    }
                }
            }
            var retry = false
            try {
                val songs = withTimeout(30_000) { music.getAllSongs().first { it.isNotEmpty() } }.sortedBy { it.id }
                val prefs = getSharedPreferences("dna_analysis", MODE_PRIVATE)
                var cursor = prefs.getInt("cursor", 0).coerceIn(0, songs.size)
                var processed = 0
                while (cursor < songs.size && processed < 8) {
                    ensureActive()
                    val memory = ActivityManager.MemoryInfo()
                    (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
                    val power = getSystemService(POWER_SERVICE) as PowerManager
                    if (memory.lowMemory || power.isPowerSaveMode ||
                        (Build.VERSION.SDK_INT >= 29 && power.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE)) {
                        retry = true; break
                    }
                    dna.genome(songs[cursor])
                    cursor++; processed++
                    prefs.edit().putInt("cursor", cursor).apply()
                }
                if (cursor == songs.size) prefs.edit().putInt("cursor", 0).apply()
                retry = retry || cursor < songs.size
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { retry = true }
            finally { monitor.cancel(); if (isActive) jobFinished(params, retry) }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { task?.cancel(); task = null; return true }
    override fun onDestroy() { task?.cancel(); super.onDestroy() }
    companion object {
        private const val ID = 0x4D444E41
        fun schedule(context: Context): Int = (context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler).schedule(
            JobInfo.Builder(ID, ComponentName(context, DnaAnalysisJob::class.java))
                .setRequiresCharging(true).setRequiresBatteryNotLow(true).setRequiresStorageNotLow(true)
                .setPersisted(true).setBackoffCriteria(60_000, JobInfo.BACKOFF_POLICY_EXPONENTIAL).build())
        fun cancel(context: Context) { (context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler).cancel(ID) }
    }
}
