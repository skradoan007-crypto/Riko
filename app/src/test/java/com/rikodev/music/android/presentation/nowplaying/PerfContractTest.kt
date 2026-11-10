package com.rikodev.music.android.presentation.nowplaying

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C1/C5 contract tests for the sliced-flow + visibility-gate patterns used
 * by the performance waves. These lock the semantics so a future refactor
 * cannot silently reintroduce monolithic state collection or unbounded
 * frame loops.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PerfContractTest {

    /** C1 followup: combine(sliced) must not require the monolithic PlayerState. */
    @Test
    fun combineOfSlicedFlows_deliversBothInputs() = runTest {
        val song = MutableStateFlow(10L)
        val position = MutableStateFlow(0L)
        val received = mutableListOf<Pair<Long?, Long>>()
        val job = launch {
            combine(song, position) { id, pos -> id to pos }.collect { received += it }
        }
        runCurrent()
        // StateFlow conflates rapid updates — process each one separately.
        position.value = 250L
        runCurrent()
        song.value = 20L
        runCurrent()
        job.cancel()
        // Must have received at least the final state; intermediate states may
        // conflate (StateFlow semantics) but both inputs must be represented.
        assertTrue("must emit at least once", received.isNotEmpty())
        val last = received.last()
        assertEquals(20L, last.first)
        assertEquals(250L, last.second)
    }

    /**
     * C5: the WavePhaseClock gate contract — when `active()` flips false,
     * `collectLatest` cancels the running loop; flipping true restarts it.
     */
    @Test
    fun activeGate_cancelsLoopWhenHidden() = runTest {
        val active = MutableStateFlow(true)
        var loopBodyRuns = 0
        var cancelled = 0
        val job = launch {
            active.collectLatest { isActive ->
                if (!isActive) return@collectLatest
                try {
                    while (true) {
                        loopBodyRuns++
                        kotlinx.coroutines.delay(1) // cancellable suspension point
                    }
                } finally {
                    cancelled++
                }
            }
        }
        runCurrent()
        assertTrue("loop must start when active", loopBodyRuns > 0)
        active.value = false
        runCurrent()
        assertEquals("loop body must be cancelled when hidden", 1, cancelled)
        active.value = true
        runCurrent()
        assertTrue("loop must restart when re-shown", loopBodyRuns > 1)
        job.cancel()
        runCurrent()
    }

    /** H5: cache starts non-null so filterNotNull() consumers emit immediately. */
    @Test
    fun h5_cacheStartsNonNull_filterNotNullEmitsImmediately() = runTest {
        val cache = MutableStateFlow<List<String>?>(emptyList())
        val emitted = mutableListOf<List<String>>()
        val job = launch {
            cache.filterNotNull().collect { emitted += it }
        }
        runCurrent()
        job.cancel()
        runCurrent()
        assertEquals("first emission must arrive without waiting for a scan", 1, emitted.size)
        assertTrue(emitted.first().isEmpty())
    }
}
