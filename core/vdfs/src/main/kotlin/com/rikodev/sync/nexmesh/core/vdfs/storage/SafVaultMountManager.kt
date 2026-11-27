package com.rikodev.sync.nexmesh.core.vdfs.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StatFs
import androidx.documentfile.provider.DocumentFile
import com.rikodev.sync.nexmesh.core.protocol.crypto.Base64UrlCompat
import com.rikodev.sync.nexmesh.core.protocol.model.RemoteVaultFileItem
import com.rikodev.sync.nexmesh.core.protocol.model.RemoteVaultListResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream

data class MountedSafVault(
    val treeUri: Uri,
    val displayName: String,
    val freeBytes: Long,
    val totalBytes: Long
)

/**
 * Manages persistent SAF Folder Mounting via ACTION_OPEN_DOCUMENT_TREE + takePersistableUriPermission().
 * Supports real listing and streaming read/write of files inside the mounted SAF folder.
 */
class SafVaultMountManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("nexmesh_saf_vault", Context.MODE_PRIVATE)

    private val _mountedVault = MutableStateFlow<MountedSafVault?>(loadPersistedVault())
    val mountedVault: StateFlow<MountedSafVault?> = _mountedVault.asStateFlow()

    fun mountTreeUri(treeUri: Uri): MountedSafVault? {
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        prefs.edit().putString(KEY_TREE_URI, treeUri.toString()).apply()
        val vault = inspectUri(treeUri)
        _mountedVault.value = vault
        return vault
    }

    fun unmountVault() {
        prefs.edit().remove(KEY_TREE_URI).apply()
        _mountedVault.value = null
    }

    /**
     * Lists all files inside the Node's mounted SAF folder so a paired Controller can browse them.
     */
    fun listMountedVaultFiles(): RemoteVaultListResponse {
        val vault = _mountedVault.value ?: return RemoteVaultListResponse(mounted = false)
        val rootDoc = DocumentFile.fromTreeUri(appContext, vault.treeUri)
            ?: return RemoteVaultListResponse(mounted = false)

        val items = rootDoc.listFiles()
            .filter { it.isFile && it.canRead() }
            .sortedByDescending { it.lastModified() }
            .map { doc ->
                val encodedUri = Base64UrlCompat.encode(doc.uri.toString().toByteArray(Charsets.UTF_8))
                RemoteVaultFileItem(
                    fileId = encodedUri,
                    name = doc.name ?: "unnamed",
                    sizeBytes = doc.length().coerceAtLeast(0L),
                    mimeType = doc.type ?: "application/octet-stream",
                    isDirectory = doc.isDirectory,
                    lastModifiedMs = doc.lastModified()
                )
            }

        return RemoteVaultListResponse(
            mounted = true,
            vaultName = vault.displayName,
            files = items
        )
    }

    /**
     * Opens a Zero-OOM InputStream for a specific fileId from the Node's mounted SAF folder.
     */
    fun openMountedFileInputStream(fileId: String): Pair<String, InputStream>? {
        return runCatching {
            val uriStr = String(Base64UrlCompat.decode(fileId), Charsets.UTF_8)
            val uri = Uri.parse(uriStr)
            val doc = DocumentFile.fromSingleUri(appContext, uri)
            val name = doc?.name ?: "file.bin"
            val stream = appContext.contentResolver.openInputStream(uri) ?: return null
            name to stream
        }.getOrNull()
    }

    /**
     * Creates a new file inside the local mounted SAF folder for saving downloaded remote files.
     */
    fun createOutputStreamInMountedVault(fileName: String, mimeType: String = "application/octet-stream"): Pair<String, OutputStream>? {
        val vault = _mountedVault.value ?: return null
        val rootDoc = DocumentFile.fromTreeUri(appContext, vault.treeUri) ?: return null
        rootDoc.findFile(fileName)?.delete()
        val created = rootDoc.createFile(mimeType, fileName) ?: return null
        val os = appContext.contentResolver.openOutputStream(created.uri) ?: return null
        return "${vault.displayName}/$fileName" to os
    }

    private fun loadPersistedVault(): MountedSafVault? {
        val raw = prefs.getString(KEY_TREE_URI, null) ?: return null
        val uri = Uri.parse(raw)
        val hasPerm = appContext.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }
        return if (hasPerm) inspectUri(uri) else null
    }

    private fun inspectUri(uri: Uri): MountedSafVault? {
        val doc = DocumentFile.fromTreeUri(appContext, uri) ?: return null
        val stat = StatFs(appContext.filesDir.absolutePath)
        return MountedSafVault(
            treeUri = uri,
            displayName = doc.name ?: "Mounted SAF Vault",
            freeBytes = stat.availableBytes,
            totalBytes = stat.totalBytes
        )
    }

    companion object {
        private const val KEY_TREE_URI = "mounted_tree_uri"
    }
}
