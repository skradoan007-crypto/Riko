package com.rikodev.music.android.dna.lab

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rikodev.music.android.audiofx.PcmAudioProcessor
import com.rikodev.music.android.audiofx.SpectrumBands
import com.rikodev.music.android.dna.core.SpectrumTripleBuffer
import com.rikodev.music.android.dna.core.SwitchableSource
import com.rikodev.music.android.dna.core.SyntheticSpectrum
import com.rikodev.music.android.dna.core.TripleBufferSource
import com.rikodev.music.android.dna.render.MdnaGlSurfaceView
import com.rikodev.music.android.dna.sync.MdnaSyncHub
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — LAB (debug builds only)
//
// Judge the look AND measure the pipeline. Plain Views on purpose (no Compose) so
// the numbers shown are the GL engine's own.
//
//   DIRECT → production path: audio-thread tap → history ring → latency-compensated
//            sampling at vsync (what the Visualizer's MDNA style uses).
//   LEGACY → P0 path: SpectrumFrame StateFlow (~30Hz, processor's fixed delay) →
//            main thread → newest frame. Kept ONLY for A/B comparison.
//   DEMO   → built-in 120BPM signal; works with no music playing.
//
// Start playback in RIKO first, then open this screen. Toggle DIRECT ⇄ LEGACY while
// a song plays: if P1 did its job, DIRECT lands on the beat and moves more smoothly.
// Exactly ONE thread feeds the legacy/demo buffer: the main thread.
// ─────────────────────────────────────────────────────────────────────────────
@AndroidEntryPoint
class MdnaLabActivity : ComponentActivity() {

    @Inject lateinit var pcm: PcmAudioProcessor
    @Inject lateinit var hub: MdnaSyncHub

    private enum class Mode(val label: String) { DIRECT("DIRECT"), LEGACY("LEGACY"), DEMO("DEMO") }

    private val bus = SpectrumTripleBuffer(SpectrumBands.COUNT)
    private val legacySource by lazy { TripleBufferSource(bus) }
    private val zeros = FloatArray(SpectrumBands.COUNT)
    private val demoBuf = FloatArray(SpectrumBands.COUNT)

    private lateinit var switchable: SwitchableSource
    private lateinit var glView: MdnaGlSurfaceView
    private lateinit var stats: TextView
    private lateinit var syncBar: SeekBar

    @Volatile private var mode = Mode.DIRECT
    private var legacyFrames = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        switchable = SwitchableSource(hub.source)
        glView = MdnaGlSurfaceView(this, switchable, refreshRate())

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(
            glView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        root.addView(
            buildPanel(),
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )
        setContentView(root)

        // LEGACY feed: SpectrumFrame StateFlow → buffer (main thread = the one producer).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                pcm.spectrum.collect { frame ->
                    if (mode != Mode.LEGACY) return@collect
                    if (frame.isActive && frame.bands.size == bus.size) {
                        legacyFrames++
                        bus.publish(frame.bands, System.nanoTime())
                    } else {
                        bus.publish(zeros, System.nanoTime())
                    }
                }
            }
        }

        // DEMO feed: 30Hz synthetic frames (same cadence as the legacy tap).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val t0 = System.nanoTime()
                while (isActive) {
                    if (mode == Mode.DEMO) {
                        SyntheticSpectrum.fill(demoBuf, (System.nanoTime() - t0) / 1_000_000_000f)
                        bus.publish(demoBuf, System.nanoTime())
                    }
                    delay(33)
                }
            }
        }

        // Stats ticker.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    stats.text = statsText()
                    delay(500)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hub.attach()
        glView.onResume()
        // attach() re-detects speaker vs Bluetooth: show the offset for the CURRENT output.
        syncBar.progress = hub.offsetMs - MdnaSyncHub.MIN_OFFSET_MS
    }

    override fun onPause() {
        glView.onPause()
        hub.detach()
        super.onPause()
    }

    @Suppress("DEPRECATION")
    private fun refreshRate(): Float {
        val hz = windowManager.defaultDisplay.refreshRate
        return if (hz in 30f..240f) hz else 60f
    }

    private fun statsText(): String {
        val r = glView.renderer
        val ms = r.frameMsAvg.coerceAtLeast(0.1f)
        val line1 = "%.0f fps  %.1f ms  %d pts  q=%s%s  bloom=%s".format(
            1000f / ms, ms, r.drawnPoints, r.quality.label,
            if (r.autoDowngrades > 0) " (auto↓${r.autoDowngrades})" else "",
            if (r.bloomActive) "on" else "off"
        )
        val src = when (mode) {
            Mode.DIRECT -> "DIRECT  latency=${hub.baseLatencyMs}${if (hub.offsetMs >= 0) "+" else ""}${hub.offsetMs} ms" +
                "  ${hub.outputLabel}  live=${r.liveData}"
            Mode.LEGACY -> "LEGACY  frames=$legacyFrames"
            Mode.DEMO -> "DEMO"
        }
        val err = r.error
        val tplErr = r.templateError
        val tpl = "template=${r.template.label}" + if (tplErr != null) "  (FAILED: $tplErr)" else ""
        return if (err != null) "$line1\n$tpl\n$src\nERROR: $err" else "$line1\n$tpl\n$src"
    }

    // ── minimal control panel (plain Views) ──

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun buildPanel(): View {
        val r = glView.renderer
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(150, 0, 0, 0))
            setPadding(dp(12), dp(8), dp(12), dp(12))
        }
        stats = TextView(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.MONOSPACE
            text = "…"
        }
        panel.addView(stats)

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val buttons2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun button(row: LinearLayout, label: String, onClick: (Button) -> Unit): Button = Button(this).apply {
            text = label
            setAllCaps(false)
            setOnClickListener { onClick(this) }
            row.addView(this, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        button(buttons, "Q: ${r.quality.label}") { b ->
            r.quality = r.quality.next()
            b.text = "Q: ${r.quality.label}"
        }
        button(buttons, "Auto-Q: on") { b ->
            r.autoQuality = !r.autoQuality
            b.text = "Auto-Q: ${if (r.autoQuality) "on" else "off"}"
        }
        button(buttons, "Bloom: on") { b ->
            r.bloomEnabled = !r.bloomEnabled
            b.text = "Bloom: ${if (r.bloomEnabled) "on" else "off"}"
        }
        button(buttons, "Src: DIRECT") { b ->
            mode = Mode.entries[(mode.ordinal + 1) % Mode.entries.size]
            switchable.delegate = if (mode == Mode.DIRECT) hub.source else legacySource
            if (mode != Mode.DIRECT) bus.publish(zeros, System.nanoTime())
            b.text = "Src: ${mode.label}"
        }
        button(buttons, "Spin: on") { b ->
            r.autoRotate = !r.autoRotate
            b.text = "Spin: ${if (r.autoRotate) "on" else "off"}"
        }
        button(buttons2, "Tpl: ${r.template.label}") { b ->
            r.template = r.template.next()
            b.text = "Tpl: ${r.template.label}"
        }
        panel.addView(buttons)
        panel.addView(buttons2)

        addSlider(panel, max = 100, initial = 12) { v, _ ->
            r.attackSec = v / 1000f
            "Attack $v ms"
        }
        addSlider(panel, max = 390, initial = 80) { v, _ ->
            r.releaseSec = (v + 10) / 1000f
            "Release ${v + 10} ms"
        }
        addSlider(panel, max = 250, initial = 100) { v, _ ->
            r.bloomIntensity = v / 100f
            "Bloom $v%"
        }
        syncBar = addSlider(
            panel,
            max = MdnaSyncHub.MAX_OFFSET_MS - MdnaSyncHub.MIN_OFFSET_MS,
            initial = 0 - MdnaSyncHub.MIN_OFFSET_MS
        ) { v, fromUser ->
            val ms = v + MdnaSyncHub.MIN_OFFSET_MS
            // Only a real drag may change the saved offset — building/restoring the bar must not.
            if (fromUser) hub.setOffsetMs(ms)
            "Sync offset ${if (ms > 0) "+" else ""}$ms ms (${hub.outputLabel})"
        }
        return panel
    }

    private fun addSlider(
        parent: LinearLayout,
        max: Int,
        initial: Int,
        onChange: (value: Int, fromUser: Boolean) -> String
    ): SeekBar {
        val label = TextView(this).apply {
            setTextColor(Color.LTGRAY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        val bar = SeekBar(this).apply {
            this.max = max
            progress = initial
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    label.text = onChange(progress, fromUser)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) = Unit
                override fun onStopTrackingTouch(sb: SeekBar?) = Unit
            })
        }
        label.text = onChange(initial, false)
        parent.addView(label)
        parent.addView(bar)
        return bar
    }
}
