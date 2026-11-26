package com.rikodev.sync.nexmesh.core.protocol.identity

import android.content.Context
import com.rikodev.sync.nexmesh.core.protocol.model.TrustedPeerRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Atomic file-backed registry of explicitly user-approved trusted swarm peers.
 * No peer can join or transfer files unless its public key is pinned here after SAS approval.
 */
class TrustedPeerStore(context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val storeFile = File(context.applicationContext.noBackupFilesDir, "nexmesh_trusted_peers.json")
    private val lock = Any()

    private val _peers = MutableStateFlow<List<TrustedPeerRecord>>(loadFromDisk())
    val peers: StateFlow<List<TrustedPeerRecord>> = _peers.asStateFlow()

    fun isTrusted(nodeId: String, publicKeyB64Url: String): Boolean = synchronized(lock) {
        _peers.value.any { it.nodeId == nodeId && it.publicKeyB64Url == publicKeyB64Url }
    }

    fun getPeer(nodeId: String): TrustedPeerRecord? = synchronized(lock) {
        _peers.value.firstOrNull { it.nodeId == nodeId }
    }

    fun saveTrustedPeer(record: TrustedPeerRecord) = synchronized(lock) {
        val updated = _peers.value.filterNot { it.nodeId == record.nodeId } + record
        persistAtomically(updated)
        _peers.value = updated
    }

    fun revokePeer(nodeId: String) = synchronized(lock) {
        val updated = _peers.value.filterNot { it.nodeId == nodeId }
        persistAtomically(updated)
        _peers.value = updated
    }

    private fun loadFromDisk(): List<TrustedPeerRecord> {
        if (!storeFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<TrustedPeerRecord>>(storeFile.readText())
        }.getOrDefault(emptyList())
    }

    private fun persistAtomically(records: List<TrustedPeerRecord>) {
        val tmp = File(storeFile.parentFile, "${storeFile.name}.tmp")
        tmp.writeText(json.encodeToString(records))
        if (!tmp.renameTo(storeFile)) {
            storeFile.writeText(tmp.readText())
            tmp.delete()
        }
    }
}
