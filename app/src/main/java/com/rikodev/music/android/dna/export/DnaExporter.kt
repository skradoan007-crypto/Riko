package com.rikodev.music.android.dna.export

import android.content.Context
import android.graphics.*
import android.media.*
import android.opengl.*
import android.view.Surface
import com.rikodev.music.android.dna.analysis.DnaTimeline
import com.rikodev.music.android.dna.core.BandSource
import com.rikodev.music.android.dna.genome.*
import com.rikodev.music.android.dna.render.MdnaQuality
import com.rikodev.music.android.dna.render.visual.BaseVisualRenderer
import com.rikodev.music.android.dna.render.visual.RikoVisuals
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.ByteBuffer

/** Fixed settings and fixed-step offline frames; encoded bytes vary by codec/device. */
object DnaExporter {
    private val gate = Mutex()
    suspend fun export(context: Context, genome: DnaGenome, timeline: DnaTimeline?, video: Boolean,
                       progress: (Int) -> Unit): File = gate.withLock {
        withContext(Dispatchers.IO) {
            require(!video || timeline != null) { "Analyze this song before video export" }
            val job = currentCoroutineContext()
            // Leave recently shared URIs readable; reclaim only our exports older than a day.
            val cutoff = System.currentTimeMillis() - 86_400_000L
            context.cacheDir.listFiles { entry -> entry.name.startsWith("riko-dna-") &&
                (entry.extension == "png" || entry.extension == "mp4") && entry.lastModified() < cutoff
            }?.forEach { it.delete() }
            val file = File.createTempFile("riko-dna-", if (video) ".mp4" else ".png", context.cacheDir)
            var codec: MediaCodec? = null
            var muxer: MediaMuxer? = null
            var input: Surface? = null
            var egl: Session? = null
            var renderer: BaseVisualRenderer? = null
            var success = false
            var started = false
            try {
                val size = if (video) 720 else 1440
                if (video) {
                    codec = MediaCodec.createEncoderByType("video/avc")
                    val format = MediaFormat.createVideoFormat("video/avc", size, size).apply {
                        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                        setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000)
                        setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                    }
                    codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    input = codec.createInputSurface(); codec.start()
                    muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                }
                egl = Session(size, input)
                var mediaUs = 0L
                val source = object : BandSource {
                    override val bandCount = 40
                    override fun sample(nowNanos: Long, out: FloatArray): Boolean {
                        for (i in out.indices) {
                            val bassEnd = com.rikodev.music.android.audiofx.SpectrumBands.bandIndex(250f,44100)
                            val midEnd = com.rikodev.music.android.audiofx.SpectrumBands.bandIndex(4000f,44100)
                            val column = if (i < bassEnd) 0 else if (i < midEnd) 1 else 2
                            val count = if (i < bassEnd) bassEnd else if (i < midEnd) midEnd-bassEnd else out.size-midEnd
                            if (timeline == null || timeline.frameCount == 0) { out[i] = 0f; continue }
                            val rms = com.rikodev.music.android.dna.analysis.DnaTimelineSampler.sample(timeline,mediaUs,column)
                            out[i] = kotlin.math.sqrt(rms / kotlin.math.sqrt(count.coerceAtLeast(1).toFloat()))
                        }
                        return true
                    }
                }
                renderer = RikoVisuals.ALL.first().factory(source, 30f) as BaseVisualRenderer
                renderer.apply {
                    seed = ((genome.seed xor (genome.seed ushr 32)) and 65535L) / 65535f
                    autoQuality = false
                    quality = MdnaQuality.HIGH
                }
                renderer.onSurfaceCreated(null,null); renderer.onSurfaceChanged(null,size,size)
                var track = -1
                val info = MediaCodec.BufferInfo()
                fun drain(end: Boolean) {
                    val encoder = codec ?: return
                    val limit = System.nanoTime() + 10_000_000_000L
                    while (true) {
                        job.ensureActive()
                        val index = encoder.dequeueOutputBuffer(info, 10_000)
                        when {
                            index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                check(!started); track = requireNotNull(muxer).addTrack(encoder.outputFormat)
                                muxer!!.start(); started = true
                            }
                            index >= 0 -> {
                                try {
                                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                                    if (info.size > 0) {
                                        check(started)
                                        val buffer = requireNotNull(encoder.getOutputBuffer(index))
                                        buffer.position(info.offset); buffer.limit(info.offset + info.size)
                                        muxer!!.writeSampleData(track, buffer, info)
                                    }
                                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                                } finally { encoder.releaseOutputBuffer(index, false) }
                            }
                            !end -> return
                            System.nanoTime() > limit -> error("Encoder did not finish")
                        }
                    }
                }
                val count = if (video) 300 else 1
                for (frame in 0 until count) {
                    job.ensureActive(); mediaUs = frame * 1_000_000L / 30
                    renderer.displayFrameNanos = 1_000_000_000L + frame * 1_000_000_000L / 30
                    renderer.onDrawFrame(null)
                    check(renderer.error == null) { "GPU render failed" }
                    if (video) {
                        EGLExt.eglPresentationTimeANDROID(egl.display, egl.surface, frame * 1_000_000_000L / 30)
                        check(EGL14.eglSwapBuffers(egl.display, egl.surface))
                        drain(false)
                    } else {
                        val pixels = ByteBuffer.allocateDirect(size*size*4)
                        GLES20.glReadPixels(0,0,size,size,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,pixels)
                        check(GLES20.glGetError() == GLES20.GL_NO_ERROR)
                        val colors = IntArray(size*size)
                        for (y in 0 until size) for (x in 0 until size) {
                            val offset = ((size-1-y)*size+x)*4
                            colors[y*size+x] = Color.rgb(pixels.get(offset).toInt() and 255,
                                pixels.get(offset+1).toInt() and 255,pixels.get(offset+2).toInt() and 255)
                        }
                        val bitmap = Bitmap.createBitmap(colors,size,size,Bitmap.Config.ARGB_8888)
                        try {
                            val canvas = Canvas(bitmap)
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE;textSize=48f;typeface=Typeface.DEFAULT_BOLD }
                            canvas.drawText("RIKO · MUSIC DNA",64f,96f,paint)
                            paint.textSize=26f
                            canvas.drawText("${genome.archetype} · DNA v${genome.algorithmVersion.id} · ${genome.seed.toULong().toString(16)}",64f,size-100f,paint)
                            canvas.drawText("DOT SPECTRUM · ${genome.rhythmMilliBpm/1000} BPM · Stored DNA identity",64f,size-52f,paint)
                            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
                        } finally { bitmap.recycle() }
                    }
                    progress((frame+1)*100/count)
                }
                if (video) { codec!!.signalEndOfInputStream(); drain(true); check(started); muxer!!.stop(); started=false }
                success = true; file
            } finally {
                runCatching { renderer?.release() }
                runCatching { egl?.close() }
                runCatching { input?.release() }
                runCatching { codec?.stop() }; runCatching { codec?.release() }
                runCatching { if(started) muxer?.stop() }; runCatching { muxer?.release() }
                if (!success) file.delete()
            }
        }
    }
    private class Session(size: Int, input: Surface?) {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        var surface: EGLSurface = EGL14.EGL_NO_SURFACE
        private var context: EGLContext = EGL14.EGL_NO_CONTEXT
        init {
          try {
            check(EGL14.eglInitialize(display, IntArray(2),0,IntArray(2),1))
            val configs = arrayOfNulls<EGLConfig>(1)
            val attributes = intArrayOf(EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_SURFACE_TYPE,
                if(input==null) EGL14.EGL_PBUFFER_BIT else EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,
                0x3142,if(input==null) 0 else 1,EGL14.EGL_NONE)
            check(EGL14.eglChooseConfig(display,attributes,0,configs,0,1,IntArray(1),0))
            val config = requireNotNull(configs[0])
            context=EGL14.eglCreateContext(display,config,EGL14.EGL_NO_CONTEXT,intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE),0)
            surface=if(input==null) EGL14.eglCreatePbufferSurface(display,config,intArrayOf(EGL14.EGL_WIDTH,size,EGL14.EGL_HEIGHT,size,EGL14.EGL_NONE),0)
                else EGL14.eglCreateWindowSurface(display,config,input,intArrayOf(EGL14.EGL_NONE),0)
            check(EGL14.eglMakeCurrent(display,surface,surface,context))
          } catch (error: Throwable) { close(); throw error }
        }
        fun close() {
            EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display,surface); EGL14.eglDestroyContext(display,context); EGL14.eglTerminate(display)
        }
    }
}
