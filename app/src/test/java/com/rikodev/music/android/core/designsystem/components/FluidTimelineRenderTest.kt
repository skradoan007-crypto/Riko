package com.rikodev.music.android.core.designsystem.components

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.audiofx.WaveformData
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import java.io.File
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Production composable, not a surrogate painter. Fixtures below are explicit
 * deterministic test audio envelopes; the production loading path never uses them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 34], qualifiers = "w480dp-h900dp-xxhdpi", application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FluidTimelineRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val backdrop = Color(0.045f, 0.04f, 0.08f)
    private fun fixture() = WaveformData(
        FloatArray(1024) { i -> when (i) { in 120..310 -> 0.9f; in 550..700 -> 0.65f; else -> 0.18f } },
        FloatArray(1024) { i -> when (i) { in 120..310 -> 0.65f; in 550..700 -> 0.4f; else -> 0.08f } }, 1024, 180_000_000L)

    @Test fun realAudioBodyPersistsOnPauseAndNeverLeaksBeyondTheSeekCircle() {
        val progress = mutableStateOf(0.5f)
        val playing = mutableStateOf(false)
        val data = fixture()
        val oldScale = RikoMotion.motionScale
        RikoMotion.motionScale = 0f
        compose.mainClock.autoAdvance = false
        try {
            compose.setContent {
                Box(Modifier.width(360.dp).background(backdrop).testTag("timeline")) {
                    WaveformTimeline(progress = progress.value, isPlaying = playing.value,
                        artworkColor = Color(0xFF188BEF), artworkSecondary = Color.Cyan, waveformData = data)
                }
            }
            for (f in listOf(0f, 0.25f, 0.50f, 0.75f, 1f)) {
                compose.runOnIdle { progress.value = f }
                compose.mainClock.advanceTimeBy(64)
                val bitmap = captureTimeline()
                save("progress-${(f * 100).toInt()}", bitmap)
                val pixels = bitmap.asImageBitmap().toPixelMap()
                val d = bitmap.width / 360f
                val baseline = bitmap.height * 0.58f
                val head = (12f + f * 336f) * d
                // Exclude only the intentional localized seek-circle bloom.
                for (x in (head + 23f * d).toInt() until bitmap.width - (12f * d).toInt()) {
                    for (above in 4..22) {
                        val c = pixels[x, (baseline - above * d).toInt()]
                        assertTrue("Wave leaked at progress $f", close(c, backdrop))
                    }
                }
                if (f == 0f) for (x in (36f * d).toInt() until bitmap.width - (12f*d).toInt())
                    assertTrue(close(pixels[x, (baseline - 5f*d).toInt()], backdrop))
            }
            compose.runOnIdle { progress.value = 0.75f; playing.value = true }
            compose.mainClock.advanceTimeBy(64)
            val before = captureTimeline().asImageBitmap().toPixelMap()
            compose.runOnIdle { playing.value = false }
            compose.mainClock.advanceTimeBy(640)
            val after = captureTimeline().asImageBitmap().toPixelMap()
            var visible = 0
            val d = before.width / 360f
            for (x in (36f*d).toInt()..(220f*d).toInt()) for (above in 4..20) {
                val y = (before.height * 0.58f - above*d).toInt()
                assertTrue("Pause mutated the musical shape", close(before[x,y], after[x,y]))
                if (!close(before[x,y], backdrop)) visible++
            }
            assertTrue("Real envelope did not render", visible > 150)
            save("paused-real-audio", captureTimeline())
        } finally { RikoMotion.motionScale = oldScale }
    }

    @Test fun unavailableAnalysisShowsOnlyTheRailAndNewTrackDropsOldData() {
        val waveform = mutableStateOf<WaveformData?>(fixture())
        val id = mutableStateOf(1L)
        compose.setContent {
            Box(Modifier.width(360.dp).background(backdrop).testTag("timeline")) {
                WaveformTimeline(progress = 0.75f, isPlaying = false, artworkColor = Color.Cyan,
                    waveformData = waveform.value, trackId = id.value)
            }
        }
        compose.runOnIdle { id.value = 2L; waveform.value = null }
        val image = captureTimeline().asImageBitmap().toPixelMap()
        val d = image.width / 360f
        for (x in (24f*d).toInt()..(220f*d).toInt()) for (above in 4..22)
            assertTrue("Loading manufactured a wave", close(image[x, (image.height*0.58f-above*d).toInt()], backdrop))
    }

    @Test fun tapDragAndAccessibilityCommitOneSeekEachAndUnknownDurationDisablesThem() {
        val enabled = mutableStateOf(true)
        var starts = 0
        val commits = mutableListOf<Float>()
        compose.setContent {
            Box(Modifier.width(360.dp).testTag("timeline")) {
                WaveformTimeline(progress = 0.2f, isPlaying = false, artworkColor = Color.Cyan,
                    enabled = enabled.value, onSeekStart = { starts++ }, onSeekEnd = { commits += it })
            }
        }
        val node = compose.onNodeWithContentDescription("Playback timeline")
        node.performTouchInput { click(Offset(width * 0.5f, height * 0.58f)) }
        compose.runOnIdle { assertEquals(1, commits.size); assertEquals(0.5f, commits.last(), 0.02f) }
        node.performTouchInput { swipe(Offset(width*0.25f,height*0.58f), Offset(width*0.8f,height*0.58f), 250) }
        compose.runOnIdle { assertEquals(2, commits.size); assertTrue(commits.last() in 0.79f..0.85f) }
        node.performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(1f)) }
        compose.runOnIdle { assertEquals(3, commits.size); assertEquals(1f, commits.last(), 0f); assertEquals(3, starts) }
        compose.runOnIdle { enabled.value = false }
        node.performSemanticsAction(SemanticsActions.SetProgress) { assertFalse(it(0.3f)) }
        node.performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(3, commits.size) }
    }
    @Test fun livePcmDrawsBeforeOfflineAnalysisAndSilenceDoesNotFakeAWave() {
        val playing = mutableStateOf(true)
        val level = mutableStateOf(0f)
        val oldScale = RikoMotion.motionScale
        RikoMotion.motionScale = 1f
        compose.mainClock.autoAdvance = false
        try {
            compose.setContent {
                Box(Modifier.width(360.dp).background(backdrop).testTag("timeline")) {
                    WaveformTimeline(progress = 0.75f, isPlaying = playing.value,
                        artworkColor = Color(0xFF188BEF), sampleAmplitude = { level.value },
                        waveformData = null)
                }
            }
            compose.mainClock.advanceTimeBy(500)
            val empty = captureTimeline().asImageBitmap().toPixelMap()
            compose.runOnIdle { level.value = 0.6f }
            compose.mainClock.advanceTimeBy(1200)
            val liveBitmap = captureTimeline()
            save("live-pcm-before-analysis", liveBitmap)
            val live = liveBitmap.asImageBitmap().toPixelMap()
            val d = live.width/360f
            var changed = 0
            for (x in (24f*d).toInt()..(235f*d).toInt()) for (above in 4..20) {
                val y = (live.height*0.58f-above*d).toInt()
                if (!close(empty[x,y],live[x,y])) changed++
            }
            assertTrue("PCM response still waits for offline analysis", changed > 60)
            compose.runOnIdle { playing.value = false }
            compose.mainClock.advanceTimeBy(250)
            val paused = captureTimeline().asImageBitmap().toPixelMap()
            compose.mainClock.advanceTimeBy(1000)
            val settled = captureTimeline().asImageBitmap().toPixelMap()
            for (x in (24f*d).toInt()..(235f*d).toInt()) for (above in 4..20) {
                val y = (live.height*0.58f-above*d).toInt()
                assertTrue("Paused flow kept moving", close(paused[x,y],settled[x,y]))
            }
        } finally { RikoMotion.motionScale = oldScale }
    }
    private fun close(a: Color, b: Color) = abs(a.red-b.red)<0.025f && abs(a.green-b.green)<0.025f && abs(a.blue-b.blue)<0.025f
    private fun save(name: String, bitmap: Bitmap) {
        val file = File("build/reports/fluid-timeline/$name-api${android.os.Build.VERSION.SDK_INT}.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    private fun captureTimeline(): Bitmap {
        val bounds = compose.onNodeWithTag("timeline").fetchSemanticsNode().boundsInWindow
        return compose.runOnIdle {
            val content = compose.activity.findViewById<View>(android.R.id.content)
            val location = IntArray(2); content.getLocationInWindow(location)
            val full = Bitmap.createBitmap(content.width, content.height, Bitmap.Config.ARGB_8888)
            full.density = content.resources.displayMetrics.densityDpi
            content.draw(Canvas(full))
            val crop = Bitmap.createBitmap(full, (bounds.left-location[0]).toInt(), (bounds.top-location[1]).toInt(),
                bounds.width.toInt(), bounds.height.toInt())
            if (crop !== full) full.recycle()
            crop
        }
    }
}
