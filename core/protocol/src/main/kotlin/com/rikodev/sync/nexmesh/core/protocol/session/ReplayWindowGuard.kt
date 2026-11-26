package com.rikodev.sync.nexmesh.core.protocol.session

/**
 * 64-slot sliding window replay guard (RFC 6479 style) protecting encrypted stream frames
 * against duplicate or out-of-window replayed packets.
 */
class ReplayWindowGuard {
    private var highestSeq: Long = -1L
    private var windowMask: Long = 0L

    @Synchronized
    fun accept(sequenceNumber: Long): Boolean {
        if (sequenceNumber < 0L) return false

        if (highestSeq < 0L) {
            highestSeq = sequenceNumber
            windowMask = 1L
            return true
        }

        if (sequenceNumber > highestSeq) {
            val shift = sequenceNumber - highestSeq
            windowMask = if (shift >= WINDOW_SIZE) 1L else (windowMask shl shift.toInt()) or 1L
            highestSeq = sequenceNumber
            return true
        }

        val diff = highestSeq - sequenceNumber
        if (diff >= WINDOW_SIZE) return false // Too old — outside replay window

        val bit = 1L shl diff.toInt()
        if ((windowMask and bit) != 0L) return false // Replay detected

        windowMask = windowMask or bit
        return true
    }

    @Synchronized
    fun reset() {
        highestSeq = -1L
        windowMask = 0L
    }

    companion object {
        const val WINDOW_SIZE = 64
    }
}
