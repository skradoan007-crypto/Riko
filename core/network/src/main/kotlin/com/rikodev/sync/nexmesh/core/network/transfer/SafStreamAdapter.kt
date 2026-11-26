package com.rikodev.sync.nexmesh.core.network.transfer

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream
import java.io.OutputStream

data class SafFileDescriptor(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String
)

/**
 * Storage Access Framework (SAF) stream bridge using ACTION_OPEN_DOCUMENT_TREE
 * and persistable URI permissions — zero MANAGE_EXTERNAL_STORAGE usage.
 */
class SafStreamAdapter(private val context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun describeFile(uri: Uri): SafFileDescriptor? {
        val doc = DocumentFile.fromSingleUri(context.applicationContext, uri) ?: return null
        return SafFileDescriptor(
            uri = uri,
            displayName = doc.name ?: "unnamed.bin",
            sizeBytes = doc.length().coerceAtLeast(0L),
            mimeType = doc.type ?: "application/octet-stream"
        )
    }

    fun openInputStream(uri: Uri): InputStream? = resolver.openInputStream(uri)

    fun createOrReplaceOutputInTree(treeUri: Uri, fileName: String, mimeType: String = "application/octet-stream"): OutputStream? {
        val root = DocumentFile.fromTreeUri(context.applicationContext, treeUri) ?: return null
        root.findFile(fileName)?.delete()
        val created = root.createFile(mimeType, fileName) ?: return null
        return resolver.openOutputStream(created.uri)
    }
}
