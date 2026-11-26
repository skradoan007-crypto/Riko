package com.rikodev.sync.nexmesh.core.relay.ledger

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class RelayLedgerEntry(
    val entryId: String,
    val timestampEpochMs: Long,
    val deltaBytes: Long, // Positive = top-up credit, Negative = relay usage consumed
    val reason: String
)

@Serializable
data class CreditLedgerState(
    val remainingBytesAllowance: Long = 0L,
    val lifetimeRelayBytesUsed: Long = 0L,
    val entries: List<RelayLedgerEntry> = emptyList()
) {
    val remainingGb: Double get() = remainingBytesAllowance / (1024.0 * 1024.0 * 1024.0)
    val usedGb: Double get() = lifetimeRelayBytesUsed / (1024.0 * 1024.0 * 1024.0)
}

/**
 * Optional Pay-Per-GB Micro-Utility Credit Ledger (Zero-Ads, Zero-Monthly-Subscription).
 * Persists usage and prepaid byte credits atomically on disk; local LAN transfers are always 100% free.
 */
class UsageCreditLedger(private val ledgerFile: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<CreditLedgerState> = _state.asStateFlow()

    fun hasAllowance(requiredBytes: Long): Boolean = synchronized(lock) {
        _state.value.remainingBytesAllowance >= requiredBytes
    }

    fun grantPrepaidCredits(gigabytes: Double, referenceId: String) = synchronized(lock) {
        require(gigabytes > 0.0) { "Credit grant must be positive" }
        val bytesToAdd = (gigabytes * 1024.0 * 1024.0 * 1024.0).toLong()
        val cur = _state.value
        val entry = RelayLedgerEntry(
            entryId = referenceId,
            timestampEpochMs = System.currentTimeMillis(),
            deltaBytes = bytesToAdd,
            reason = String.format("Prepaid utility credit (+%.2f GB)", gigabytes)
        )
        val updated = cur.copy(
            remainingBytesAllowance = cur.remainingBytesAllowance + bytesToAdd,
            entries = (listOf(entry) + cur.entries).take(50)
        )
        persist(updated)
        _state.value = updated
    }

    fun consumeRelayBytes(bytes: Long, transferRef: String): Boolean = synchronized(lock) {
        if (bytes <= 0L) return true
        val cur = _state.value
        if (cur.remainingBytesAllowance < bytes) return false

        val entry = RelayLedgerEntry(
            entryId = transferRef,
            timestampEpochMs = System.currentTimeMillis(),
            deltaBytes = -bytes,
            reason = "Encrypted relay transit"
        )
        val updated = cur.copy(
            remainingBytesAllowance = cur.remainingBytesAllowance - bytes,
            lifetimeRelayBytesUsed = cur.lifetimeRelayBytesUsed + bytes,
            entries = (listOf(entry) + cur.entries).take(50)
        )
        persist(updated)
        _state.value = updated
        true
    }

    private fun load(): CreditLedgerState {
        if (!ledgerFile.exists()) return CreditLedgerState()
        return runCatching { json.decodeFromString<CreditLedgerState>(ledgerFile.readText()) }
            .getOrDefault(CreditLedgerState())
    }

    private fun persist(state: CreditLedgerState) {
        ledgerFile.parentFile?.mkdirs()
        val tmp = File(ledgerFile.parentFile, "${ledgerFile.name}.tmp")
        tmp.writeText(json.encodeToString(state))
        if (!tmp.renameTo(ledgerFile)) {
            ledgerFile.writeText(tmp.readText())
            tmp.delete()
        }
    }
}
