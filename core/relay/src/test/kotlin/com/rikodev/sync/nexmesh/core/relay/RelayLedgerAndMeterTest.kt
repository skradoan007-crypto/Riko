package com.rikodev.sync.nexmesh.core.relay

import com.rikodev.sync.nexmesh.core.relay.ledger.UsageCreditLedger
import com.rikodev.sync.nexmesh.core.relay.meter.LocalBandwidthMeter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RelayLedgerAndMeterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun ledgerTracksPayPerGbCreditsAndBlocksOverdraft() {
        val ledger = UsageCreditLedger(File(tempFolder.root, "relay_ledger.json"))
        assertFalse(ledger.hasAllowance(1024))

        ledger.grantPrepaidCredits(1.0, "topup_001") // 1 GB
        assertTrue(ledger.hasAllowance(512L * 1024 * 1024))
        assertTrue(ledger.consumeRelayBytes(256L * 1024 * 1024, "tx_1"))
        assertEquals(0.75, ledger.state.value.remainingGb, 0.01)

        // Overdraft must be rejected
        assertFalse(ledger.consumeRelayBytes(1024L * 1024 * 1024, "tx_too_large"))
    }

    @Test
    fun bandwidthMeterSeparatesLanAndRelayTraffic() {
        val meter = LocalBandwidthMeter()
        meter.recordLanBytes(10L * 1024 * 1024)
        meter.recordRelaySent(2L * 1024 * 1024)
        meter.recordRelayReceived(3L * 1024 * 1024)

        val snap = meter.snapshot.value
        assertEquals(10.0, snap.lanMb, 0.01)
        assertEquals(5.0, snap.totalRelayMb, 0.01)
    }
}
