package com.rikodev.ibd3d.android

import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.InstallErrorCode
import com.rikodev.ibd3d.android.updates.PlayUpdates
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PlayUpdatesTest {
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun flexibleDownloadResumeAndComplete() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
        val manager = FakeAppUpdateManager(activity)
        var updates = PlayUpdates(activity, manager)
        manager.setUpdateAvailable(2)
        updates.check()
        idle()
        assertTrue(manager.isConfirmationDialogVisible)
        assertFalse(manager.isImmediateFlowVisible)
        manager.userAcceptsUpdate()
        manager.downloadStarts()
        idle()
        assertTrue(updates.state.value.busy)
        manager.downloadCompletes()
        idle()
        assertTrue(updates.state.value.downloaded)
        updates.close()
        updates = PlayUpdates(activity, manager)
        updates.resume()
        idle()
        assertTrue(updates.state.value.downloaded)
        updates.complete()
        idle()
        assertTrue(manager.isInstallSplashScreenVisible)
        manager.installCompletes()
        idle()
        assertFalse(updates.state.value.busy)
        updates.close()
    }

    @Test
    fun noUpdateAndFailureAllowRetry() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
        val manager = FakeAppUpdateManager(activity)
        val updates = PlayUpdates(activity, manager)
        manager.setUpdateNotAvailable()
        updates.check()
        idle()
        assertEquals("No update available on Google Play", updates.state.value.text)
        manager.setInstallErrorCode(InstallErrorCode.ERROR_INTERNAL_ERROR)
        updates.check()
        idle()
        assertFalse(updates.state.value.busy)
        assertTrue(updates.state.value.text.startsWith("Could not check"))
        manager.setInstallErrorCode(InstallErrorCode.NO_ERROR)
        manager.setUpdateAvailable(2)
        updates.check()
        idle()
        manager.userAcceptsUpdate()
        manager.downloadFails()
        idle()
        assertEquals("Download failed. Please retry.", updates.state.value.text)
        updates.close()
    }
}
