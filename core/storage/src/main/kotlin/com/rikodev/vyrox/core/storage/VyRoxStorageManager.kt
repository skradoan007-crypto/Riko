package com.rikodev.vyrox.core.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VyRoxStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val projectsDir: File
        get() = File(context.filesDir, "projects").apply { mkdirs() }

    val modelsDir: File
        get() = File(context.filesDir, "models").apply { mkdirs() }

    val proxyCacheDir: File
        get() = File(context.cacheDir, "proxies").apply { mkdirs() }

    val thumbCacheDir: File
        get() = File(context.cacheDir, "thumbs").apply { mkdirs() }

    val renderTmpDir: File
        get() = File(context.cacheDir, "render_tmp").apply { mkdirs() }

    fun projectDocumentFile(projectId: String): File {
        val dir = File(projectsDir, projectId).apply { mkdirs() }
        return File(dir, "project.vxproj.json")
    }

    /** Atomic temp-write + rename to prevent corrupted project JSON on crash. */
    suspend fun writeAtomicText(target: File, content: String) = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    suspend fun deleteProjectFiles(projectId: String) = withContext(Dispatchers.IO) {
        File(projectsDir, projectId).deleteRecursively()
    }

    suspend fun computeCacheBytes(): Long = withContext(Dispatchers.IO) {
        listOf(proxyCacheDir, thumbCacheDir, renderTmpDir).sumOf { dir ->
            dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
    }

    suspend fun purgeRenderTemp() = withContext(Dispatchers.IO) {
        renderTmpDir.listFiles()?.forEach { it.deleteRecursively() }
    }
}
