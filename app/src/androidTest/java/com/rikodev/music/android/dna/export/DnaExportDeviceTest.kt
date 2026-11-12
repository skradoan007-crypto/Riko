package com.rikodev.music.android.dna.export
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.rikodev.music.android.dna.genome.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
class DnaExportDeviceTest {
    @Test fun pngIsDecodableAndVideoRequiresAnalyzedTimeline() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val f = DnaFeatures.fromNormalized(.5,.3,.2,2000.0,.3,120.0,.8,20.0,DoubleArray(12))
        val genome = DnaGenomeMapper.map(f,1L,DnaAlgorithmVersion.GENESIS_V1)
        val file = withTimeout(30000) { DnaExporter.export(context,genome,null,false) {} }
        try {
            val bitmap = requireNotNull(BitmapFactory.decodeFile(file.path))
            assertEquals(1440,bitmap.width);assertEquals(1440,bitmap.height);bitmap.recycle()
        } finally { file.delete() }
        try { DnaExporter.export(context,genome,null,true) {}; fail("Timeline required") }
        catch (_: IllegalArgumentException) { }
    }
}
