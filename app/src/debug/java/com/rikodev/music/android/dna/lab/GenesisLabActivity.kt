package com.rikodev.music.android.dna.lab

import android.app.ActivityManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rikodev.music.android.dna.core.GenesisDemoSource
import com.rikodev.music.android.dna.core.SwitchableSource
import com.rikodev.music.android.dna.render.MdnaGlSurfaceView
import com.rikodev.music.android.dna.render.MdnaQuality
import com.rikodev.music.android.dna.render.MdnaTemplate
import com.rikodev.music.android.dna.sync.MdnaSyncHub
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/** P0 only. Dedicated launcher; does not modify production navigation or database. */
@AndroidEntryPoint
class GenesisLabActivity : ComponentActivity() {
    @Inject lateinit var hub: MdnaSyncHub
    private val demo = GenesisDemoSource()
    private lateinit var source: SwitchableSource
    private var surface: MdnaGlSurfaceView? = null
    private var atmosphere: GenesisAtmosphere? = null
    private lateinit var stats: TextView
    private var demoMode = false
    private var frozen = false
    private var attached = false
    private var agslEnabled = true
    private lateinit var offset: SeekBar
    private lateinit var offsetLabel: TextView
    private lateinit var qualityButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demoMode = savedInstanceState?.getBoolean("demo") ?: false
        frozen = savedInstanceState?.getBoolean("frozen") ?: false
        agslEnabled = savedInstanceState?.getBoolean("agsl") ?: true
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (manager.deviceConfigurationInfo.reqGlEsVersion < 0x30000) {
            setContentView(label("Genesis needs OpenGL ES 3.0 on this GPU.\nAudio playback remains available.", 18f))
            return
        }
        source = SwitchableSource(if (demoMode) demo else hub.source)
        @Suppress("DEPRECATION")
        val refresh = windowManager.defaultDisplay.refreshRate.coerceIn(30f, 240f)
        val gl = MdnaGlSurfaceView(this, source, refresh).also { surface = it }
        gl.renderer.apply {
            template = MdnaTemplate.GENESIS_ORB
            quality = if (manager.isLowRamDevice) MdnaQuality.LOW else MdnaQuality.MID
            savedInstanceState?.getString("quality")?.let { name ->
                MdnaQuality.entries.firstOrNull { it.name == name }?.let { quality = it }
            }
            bloomIntensity = 0.35f
            animationEnabled = !frozen
            autoQuality = savedInstanceState?.getBoolean("autoQuality", true) ?: true
            bloomEnabled = savedInstanceState?.getBoolean("bloom", true) ?: true
        }
        demo.paused = frozen
        gl.contentDescription = "Genesis organism. Drag to inspect; pinch to zoom."
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(5, 6, 14))
            fitsSystemWindows = true
        }
        root.addView(label("RIKO  /  GENESIS", 14f).apply {
            letterSpacing = 0.16f; setTextColor(Color.rgb(123, 220, 249))
        })
        root.addView(label("One song. A living organism.", 23f))
        val viewport = FrameLayout(this)
        viewport.addView(gl, FrameLayout.LayoutParams(-1, -1))
        if (Build.VERSION.SDK_INT >= 33) {
            atmosphere = GenesisAtmosphere(this, gl.renderer).also {
                viewport.addView(it, FrameLayout.LayoutParams(-1, -1))
            }
        }
        root.addView(viewport, LinearLayout.LayoutParams(-1, 0, 1f))
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(12), dp(8))
        }
        stats = label("Waiting for the first frame…", 11f).apply {
            typeface = Typeface.MONOSPACE
            setPadding(dp(4), dp(4), dp(4), dp(4))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        controls.addView(stats)
        val row = LinearLayout(this)
        button(row, if (demoMode) "DEMO" else "LIVE") { b ->
            demoMode = !demoMode
            demo.resumeClock()
            source.delegate = if (demoMode) demo else hub.source
            b.text = if (demoMode) "DEMO" else "LIVE"
            reconcileTap()
        }
        button(row, if (frozen) "Resume visual" else "Pause visual") { b ->
            frozen = !frozen
            demo.paused = frozen
            gl.renderer.animationEnabled = !frozen
            b.text = if (frozen) "Resume visual" else "Pause visual"
        }
        qualityButton = button(row, "Quality: ${gl.renderer.quality.label}") { _ ->
            gl.renderer.quality = gl.renderer.quality.next()
        }
        controls.addView(row)
        val row2 = LinearLayout(this)
        button(row2, "Auto quality: ${if (gl.renderer.autoQuality) "on" else "off"}") { b ->
            gl.renderer.autoQuality = !gl.renderer.autoQuality
            b.text = "Auto quality: ${if (gl.renderer.autoQuality) "on" else "off"}"
        }
        button(row2, "Glow: ${if (gl.renderer.bloomEnabled) "on" else "off"}") { b ->
            gl.renderer.bloomEnabled = !gl.renderer.bloomEnabled
            b.text = "Glow: ${if (gl.renderer.bloomEnabled) "on" else "off"}"
        }
        button(row2, if (Build.VERSION.SDK_INT >= 33) "AGSL: ${if (agslEnabled) "on" else "off"}" else "GLES only") { b ->
            if (Build.VERSION.SDK_INT >= 33) {
                agslEnabled = !agslEnabled
                atmosphere?.active = agslEnabled
                atmosphere?.invalidate()
                b.text = "AGSL: ${if (agslEnabled) "on" else "off"}"
            }
        }
        controls.addView(row2)
        offsetLabel = label("", 12f)
        controls.addView(offsetLabel)
        offset = SeekBar(this).apply {
            max = MdnaSyncHub.MAX_OFFSET_MS - MdnaSyncHub.MIN_OFFSET_MS
            contentDescription = "Audio visual synchronization offset. Positive delays the picture."
            minimumHeight = dp(48)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) hub.setOffsetMs(progress + MdnaSyncHub.MIN_OFFSET_MS)
                    updateOffset()
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        }
        controls.addView(offset)
        val scroll = ScrollView(this).apply { addView(controls) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, dp(250)))
        setContentView(root)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    val r = gl.renderer
                    r.sampleRateHz = if (demoMode) 44100 else hub.sampleRate.takeIf { it > 0 } ?: 44100
                    val age = if (hub.lastFrameNanos == 0L) -1L else (System.nanoTime()-hub.lastFrameNanos)/1_000_000L
                    stats.text = String.format(Locale.US,
                        "%s · %s\n%.0f fps / %.1f ms avg / %.1f max\nCPU submit %.2f ms · %,d points · auto↓ %d\nBass %.2f  Mid %.2f  High %.2f\nPCM age %d ms · %.2fx · %d ms\n%s",
                        if (demoMode) "DEMO · synthetic 120 BPM" else "LIVE · playback PCM",
                        if (frozen) "VISUAL PAUSED" else if (r.liveData) "SIGNAL" else "waiting / paused",
                        1000f/r.frameMsAvg.coerceAtLeast(1f), r.frameMsAvg, r.frameMsMax,
                        r.cpuSubmitMs, r.drawnPoints, r.autoDowngrades,
                        r.bassLevel, r.midLevel, r.trebleLevel, if (demoMode) -1 else age,
                        hub.playbackSpeed, hub.positionMs,
                        r.error ?: r.templateError ?: atmosphere?.failure?.let { "AGSL disabled: $it" }
                            ?: "Timing estimate; device measurement pending")
                    qualityButton.text = "Quality: ${r.quality.label}"
                    updateOffset()
                    delay(500)
                }
            }
        }
    }
    private fun updateOffset() {
        offsetLabel.text = "AV offset ${hub.offsetMs} ms · ${hub.outputLabel}\nPositive = later picture; effective ${(hub.baseLatencyMs + hub.offsetMs).coerceAtLeast(0)} ms (estimate)"
        val progress = hub.offsetMs - MdnaSyncHub.MIN_OFFSET_MS
        if (offset.progress != progress) offset.progress = progress
    }
    private fun reconcileTap() {
        val shouldAttach = !demoMode && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (shouldAttach && !attached) { hub.attach(); attached = true }
        if (!shouldAttach && attached) { hub.detach(); attached = false }
    }
    override fun onResume() {
        super.onResume()
        if (surface == null) return
        // Activity.onResume can precede Lifecycle's RESUMED event.
        if (!demoMode && !attached) { hub.attach(); attached = true }
        demo.resumeClock()
        surface?.onResume()
        if (Build.VERSION.SDK_INT >= 33) {
            atmosphere?.active = agslEnabled
            atmosphere?.invalidate()
        }
        updateOffset()
    }
    override fun onPause() {
        if (Build.VERSION.SDK_INT >= 33) atmosphere?.active = false
        surface?.onPause()
        if (attached) { hub.detach(); attached = false }
        super.onPause()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("demo", demoMode)
        outState.putBoolean("frozen", frozen)
        outState.putBoolean("agsl", agslEnabled)
        outState.putBoolean("autoQuality", surface?.renderer?.autoQuality ?: true)
        outState.putBoolean("bloom", surface?.renderer?.bloomEnabled ?: true)
        outState.putString("quality", surface?.renderer?.quality?.name)
        super.onSaveInstanceState(outState)
    }
    private fun dp(value: Int) = (value*resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float) = TextView(this).apply {
        text = value; textSize = size; setTextColor(Color.rgb(231, 235, 247))
        setPadding(dp(16), dp(6), dp(16), dp(6))
    }
    private fun button(row: LinearLayout, title: String, action: (Button) -> Unit) = Button(this).apply {
        text = title; textSize = 11f; isAllCaps = false; minHeight = dp(48)
        setOnClickListener { action(this) }
        row.addView(this, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    }
}
