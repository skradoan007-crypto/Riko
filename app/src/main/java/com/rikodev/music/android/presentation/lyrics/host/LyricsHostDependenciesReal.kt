/*
 * RIKO EXPERIENCE OS — HOST INTEGRATION TIER (B5.5)
 * Lyrics Engine — E3
 *
 * LyricsHostDependenciesReal.kt — replaces LyricsHostDependenciesStub.
 * Same real patterns as Queue (E1) / Audio FX (E2): real repository
 * lookups, real PlayerManager seek, real Android share/open-url
 * intents, real Vibrator/Toast/Log, real ConnectivityManager check.
 *
 * © RIKO Experience OS — Lyrics Host Tier
 */

package com.rikodev.music.android.presentation.lyrics.host

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import com.rikodev.music.android.core.util.RikoLog
import com.rikodev.music.android.data.repository.MusicRepository
import com.rikodev.music.android.service.PlayerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RikoLyricsHost"

@Singleton
class LyricsHostDependenciesReal @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val playerManager: PlayerManager,
) : LyricsHostDependencies {

    override suspend fun getSongTitle(songId: Long): String? =
        musicRepository.getSongById(songId)?.title

    override suspend fun getArtistName(songId: Long): String? =
        musicRepository.getSongById(songId)?.artist

    override suspend fun getAlbumName(songId: Long): String? =
        musicRepository.getSongById(songId)?.album

    override suspend fun getSongDurationMs(songId: Long): Long? =
        musicRepository.getSongById(songId)?.duration

    override suspend fun seekPlayback(positionMs: Long) {
        withContext(Dispatchers.Main) { playerManager.seekTo(positionMs) }
    }

    override fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "openUrl failed: ${e.message}")
        }
    }

    override fun shareText(text: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, "Share lyrics").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.w(TAG, "shareText failed: ${e.message}")
        }
    }

    override fun performHaptic() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        }
    }

    override suspend fun showSnackbar(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun logEvent(event: String, params: Map<String, Any?>) {
        RikoLog.d(TAG) {
            buildString {
                append(event)
                if (params.isNotEmpty()) {
                    append(" ")
                    append(params.entries.joinToString(", ") { (k, v) -> "$k=$v" })
                }
            }
        }
    }

    override fun isOffline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = cm.activeNetwork ?: return true
        val capabilities = cm.getNetworkCapabilities(network) ?: return true
        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
