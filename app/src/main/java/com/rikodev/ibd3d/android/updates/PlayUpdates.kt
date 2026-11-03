package com.rikodev.ibd3d.android.updates

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UpdateState(
    val text: String = "Updates are delivered by Google Play",
    val busy: Boolean = false,
    val downloaded: Boolean = false,
)

/** One Activity owns the result launcher and listener; release them when it is destroyed. */
class PlayUpdates(
    activity: ComponentActivity,
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(activity),
) {
    private val mutable = MutableStateFlow(UpdateState())
    val state = mutable.asStateFlow()
    private val launcher =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            result ->
            if (result.resultCode != Activity.RESULT_OK) {
                mutable.value =
                    UpdateState(
                        if (result.resultCode == Activity.RESULT_CANCELED)
                            "Update cancelled. Check again anytime."
                        else "Could not start the update."
                    )
            }
        }
    private val listener = InstallStateUpdatedListener {
        showInstallState(it.installStatus(), it.bytesDownloaded(), it.totalBytesToDownload())
    }

    init {
        manager.registerListener(listener)
    }

    private fun showInstallState(status: Int, bytes: Long = 0, total: Long = 0): Boolean {
        mutable.value =
            when (status) {
                InstallStatus.PENDING -> UpdateState("Download pending", busy = true)
                InstallStatus.DOWNLOADING ->
                    UpdateState(
                        "Downloading update" + (if (total > 0) " ${100*bytes/total}%" else ""),
                        busy = true,
                    )
                InstallStatus.DOWNLOADED ->
                    UpdateState("Update ready. Restart to install.", downloaded = true)
                InstallStatus.INSTALLING -> UpdateState("Installing update…", busy = true)
                InstallStatus.INSTALLED -> UpdateState("Update installed")
                InstallStatus.FAILED -> UpdateState("Download failed. Please retry.")
                InstallStatus.CANCELED -> UpdateState("Update cancelled.")
                else -> return false
            }
        return true
    }

    fun check() {
        if (mutable.value.busy) return
        mutable.value = UpdateState("Checking Google Play…", busy = true)
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                when {
                    info.installStatus() in
                        listOf(
                            InstallStatus.PENDING,
                            InstallStatus.DOWNLOADING,
                            InstallStatus.DOWNLOADED,
                            InstallStatus.INSTALLING,
                        ) ->
                        showInstallState(
                            info.installStatus(),
                            info.bytesDownloaded(),
                            info.totalBytesToDownload(),
                        )
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                        mutable.value = UpdateState("Waiting for Play confirmation", busy = true)
                        // A fresh AppUpdateInfo is requested on each retry. False is also a start
                        // failure.
                        val started =
                            runCatching {
                                    manager.startUpdateFlowForResult(
                                        info,
                                        launcher,
                                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                                    )
                                }
                                .getOrDefault(false)
                        if (!started)
                            mutable.value = UpdateState("Could not open update flow. Please retry.")
                    }
                    info.updateAvailability() == UpdateAvailability.UPDATE_NOT_AVAILABLE ->
                        mutable.value = UpdateState("No update available on Google Play")
                    else -> mutable.value = UpdateState("Updates unavailable for this installation")
                }
            }
            .addOnFailureListener {
                mutable.value =
                    UpdateState(
                        "Could not check. Use a Play-installed release and an internet connection."
                    )
            }
    }

    fun resume() {
        manager.appUpdateInfo.addOnSuccessListener {
            showInstallState(it.installStatus(), it.bytesDownloaded(), it.totalBytesToDownload())
        }
    }

    fun complete() {
        if (!mutable.value.downloaded) return
        mutable.value = UpdateState("Installing update…", busy = true)
        manager.completeUpdate().addOnFailureListener {
            mutable.value = UpdateState("Could not restart. Please retry.", downloaded = true)
        }
    }

    fun close() {
        manager.unregisterListener(listener)
    }
}
