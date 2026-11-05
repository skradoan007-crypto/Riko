package com.rikodev.ibd3d.android

import android.app.Application
import android.app.NotificationManager
import android.os.Looper
import com.rikodev.ibd3d.android.notifications.CopyReminder
import java.time.Duration
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], application = Application::class)
class CopyReminderTest {
    private val context: Application get() = RuntimeEnvironment.getApplication()
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private fun advance(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    @Before fun reset() { CopyReminder.enabled = true; CopyReminder.foreground(context) }
    @Test fun copyInsideAppDoesNotPost() {
        CopyReminder.copied("Defender", "0002"); advance(2000)
        assertTrue(manager.activeNotifications.isEmpty())
    }
    @Test fun latestCopyUsesSingleIdAndExpires() {
        CopyReminder.copied("Old", "123"); CopyReminder.copied("Defender", "0002"); CopyReminder.background(context)
        val first = manager.activeNotifications.single()
        assertEquals("Defender • 0002", first.notification.extras.getCharSequence("android.title"))
        assertEquals(7000L, first.notification.timeoutAfter)
        advance(2000)
        assertEquals(first.id, manager.activeNotifications.single().id)
        assertEquals(5000L, manager.activeNotifications.single().notification.timeoutAfter)
        advance(2000)
        assertEquals(3000L, manager.activeNotifications.single().notification.timeoutAfter)
        advance(3000); assertTrue(manager.activeNotifications.isEmpty())
    }
    @Test fun returningCancelsUpdates() {
        CopyReminder.copied("Defender", "0002"); CopyReminder.background(context); CopyReminder.foreground(context)
        advance(8000); assertTrue(manager.activeNotifications.isEmpty())
    }
    @Test fun dismissalIsRespected() {
        CopyReminder.copied("Defender", "0002"); CopyReminder.background(context)
        manager.cancel(manager.activeNotifications.single().id)
        advance(2500); assertTrue(manager.activeNotifications.isEmpty())
    }
    @Test fun staleCopyDoesNotPost() {
        CopyReminder.copied("Defender", "0002"); advance(10001); CopyReminder.background(context)
        assertTrue(manager.activeNotifications.isEmpty())
    }
    @Test fun disabledDoesNotPost() {
        CopyReminder.enabled = false; CopyReminder.copied("Defender", "0002"); CopyReminder.background(context)
        assertTrue(manager.activeNotifications.isEmpty())
    }
}
