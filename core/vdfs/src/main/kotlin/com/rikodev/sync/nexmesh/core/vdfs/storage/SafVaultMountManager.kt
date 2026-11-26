package com.rikodev.sync.nexmesh.core.vdfs.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StatFs
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MountedSafVault(
    val treeUri: Uri,
    val displayName: String,
    val freeBytes: Long,
    val totalBytes: Long
)

/**
 * Manages persistent SAF Folder Mounting via ACTION_OPEN_DOCUMENT_TREE + takePersistableUriPermission().
 * Zero MANAGE_EXTERNAL_STORAGE usage.
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

    private fun loadPersistedVault(): MountedSafVault? {
        val raw = prefs.getString(KEY_TREE_URI, null) ?: return null
        val uri = Uri.parse(raw)
        val hasPerm = appContext.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
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
