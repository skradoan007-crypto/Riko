package com.rikodev.ibd3d.android

import com.rikodev.ibd3d.android.files.ModelFiles
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelFilesTest {
    @get:Rule val temp = TemporaryFolder()

    @Test
    fun glbHeaderAndTruncation() {
        val text = "{\"asset\":{\"version\":\"2.0\"}}"
        val json = text.padEnd((text.length + 3) / 4 * 4).toByteArray()
        val bytes =
            ByteBuffer.allocate(20 + json.size)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put("glTF".toByteArray())
                .putInt(2)
                .putInt(20 + json.size)
                .putInt(json.size)
                .putInt(0x4e4f534a)
                .put(json)
                .array()
        val file = temp.newFile("mesh.glb").apply { writeBytes(bytes) }
        assertEquals("GLB 2", ModelFiles.format(file))
        file.writeBytes(bytes.copyOf(bytes.size - 1))
        assertTrue(runCatching { ModelFiles.format(file) }.isFailure)
    }

    @Test
    fun unityHeaderDespiteGlbExtension() {
        val out = ByteArrayOutputStream()
        val data = DataOutputStream(out)
        data.write("UnityFS\u0000".toByteArray())
        data.writeInt(6)
        data.write("5.x.x\u00002021.3\u0000".toByteArray())
        val offset = out.size()
        data.writeLong(0)
        repeat(3) { data.writeInt(0) }
        val bytes = out.toByteArray()
        ByteBuffer.wrap(bytes).putLong(offset, bytes.size.toLong())
        val file = temp.newFile("bundle.glb").apply { writeBytes(bytes) }
        assertEquals("Unity AssetBundle", ModelFiles.format(file))
        file.appendBytes(byteArrayOf(0))
        assertTrue(runCatching { ModelFiles.format(file) }.isFailure)
    }

    @Test
    fun errorPageRejected() {
        val file =
            temp.newFile("error.glb").apply { writeText("<html>download unavailable</html>") }
        assertTrue(runCatching { ModelFiles.format(file) }.isFailure)
    }

    @Test
    fun displayNameHidesStorageId() {
        assertEquals("My car.glb", ModelFiles.displayName(temp.newFile("My car.glb__id.glb")))
    }
}
