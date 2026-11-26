package com.rikodev.sync.nexmesh.core.network

import com.rikodev.sync.nexmesh.core.network.transfer.TransferCheckpoint
import com.rikodev.sync.nexmesh.core.network.transfer.TransferCheckpointStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TransferCheckpointStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun checkpointPersistsAndResumesInterruptedChunkProgress() {
        val store = TransferCheckpointStore(tempFolder.newFolder("chk"))
        val initial = TransferCheckpoint(
            transferId = "tx_resume_01",
            fileName = "backup.mp4",
            totalSizeBytes = 20L * 1024L * 1024L,
            logicalChunkSizeBytes = 8 * 1024 * 1024,
            totalChunks = 3,
            expectedFileSha256 = "abc123",
            compressedWithLz4 = true
        )
        store.save(initial)

        val afterChunk0 = store.markChunkCompleted("tx_resume_01", 0)
        assertNotNull(afterChunk0)
        assertEquals(8L * 1024L * 1024L, afterChunk0!!.completedBytes)
        assertFalse(afterChunk0.isComplete)

        store.markChunkCompleted("tx_resume_01", 1)
        val afterAll = store.markChunkCompleted("tx_resume_01", 2)!!
        assertEquals(20L * 1024L * 1024L, afterAll.completedBytes)
        assertTrue(afterAll.isComplete)
    }
}
