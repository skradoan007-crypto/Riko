package com.rikodev.ibd3d.android.files

import android.content.*
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.rikodev.ibd3d.android.BuildConfig
import java.io.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ModelFiles {
    const val LIMIT = 256L * 1024 * 1024
    private val lock = Mutex()
    private suspend fun copy(source: InputStream, dest: OutputStream) {
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val n = source.read(buffer)
            if (n < 0) break
            total += n
            require(total <= LIMIT) { "Maximum model size is 256 MB." }
            dest.write(buffer, 0, n)
        }
    }
    fun displayName(file: File) = file.name.substringBeforeLast("__", file.name)
    fun format(file: File): String = RandomAccessFile(file, "r").use { input ->
        require(file.length() in 12L..LIMIT) { "Model must be between 12 bytes and 256 MB." }
        val magic = ByteArray(8); input.readFully(magic)
        when {
            String(magic, Charsets.US_ASCII) == "UnityFS\u0000" -> {
                input.readInt()
                repeat(2) {
                    var count = 0
                    while (input.readByte().toInt() != 0) require(++count < 128) { "Invalid bundle header." }
                }
                require(input.readLong() == file.length()) { "Incomplete Unity AssetBundle." }
                "Unity AssetBundle"
            }
            String(magic.copyOf(4), Charsets.US_ASCII) == "glTF" -> {
                input.seek(4)
                require(Integer.reverseBytes(input.readInt()) == 2) { "Only GLB 2 is supported." }
                require(Integer.reverseBytes(input.readInt()).toLong() == file.length()) { "Incomplete GLB." }
                "GLB 2"
            }
            else -> error("Choose a GLB 2 or UnityFS AssetBundle file.")
        }
    }
    suspend fun bundled(context: Context, asset: String): File = withContext(Dispatchers.IO) {
        lock.withLock {
            require(asset.startsWith("models/") && !asset.contains(".."))
            val dir = File(context.cacheDir, "exports/v${BuildConfig.VERSION_CODE}").apply { mkdirs() }
            val file = File(dir, asset.substringAfterLast('/'))
            if (!file.exists()) {
                val part = File(dir, file.name + ".part")
                try {
                    val paths = try {
                        context.assets.open(asset + ".parts").bufferedReader().use { it.readLines() }
                    } catch (_: FileNotFoundException) { listOf(asset) }
                    part.outputStream().use { output ->
                        paths.forEach { path ->
                            require(path.startsWith("models/") && !path.contains(".."))
                            context.assets.open(path).use { copy(it, output) }
                        }
                    }
                    format(part)
                    check(part.renameTo(file)) { "Could not prepare model." }
                } finally { part.delete() }
            }
            format(file)
            file
        }
    }
    suspend fun importModel(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "custom_models").apply { mkdirs() }
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "Custom model.glb"
        val safe = name.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").take(80).trim().ifBlank { "Custom model.glb" }
        val file = File(dir, "${safe}__${UUID.randomUUID()}.glb")
        val part = File(dir, file.name + ".part")
        try {
            context.contentResolver.openInputStream(uri)?.use { source ->
                part.outputStream().use { copy(source, it) }
            } ?: error("Could not read selected file.")
            format(part)
            check(part.renameTo(file))
            file
        } finally { part.delete() }
    }
    suspend fun export(context: Context, file: File, uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { output -> file.inputStream().use { copy(it, output) } }
            ?: error("Could not write selected location.")
    }
    fun loadInGame(context: Context, file: File): Boolean {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/octet-stream")
            .setPackage("com.Rohit.IndianBikes").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (intent.resolveActivity(context.packageManager) == null) return false
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }
}
