/*
 * RIKO EXPERIENCE OS — HOST INTEGRATION TIER (B5.5)
 * Lyrics Engine — E3
 *
 * Id3UsltReader.kt
 * --------------------------------------------------------------------
 * Minimal, defensive ID3v2.3/2.4 USLT (unsynchronized lyrics) frame
 * reader. LrcParser.kt's Id3LyricsExtractor already converts a decoded
 * (language, text) pair into a real LyricsSet — it explicitly says the
 * raw byte-level extraction "happens in the audio metadata reader (not
 * in this package)." This is that reader.
 *
 * SCOPE (intentional, documented — not a silent gap)
 * ----------------------------------------------------
 * - Supports ID3v2.3 and ID3v2.4 (4-char frame IDs) — the vast majority
 *   of real-world MP3 files.
 * - Does NOT support ID3v2.2 (3-char frame IDs, pre-2000s files).
 * - Does NOT support SYLT (synchronized lyrics — a materially more
 *   complex binary timing format, genuinely rare in practice).
 * - Any parse failure (malformed tag, no USLT frame, truncated file)
 *   returns null. Never throws past this boundary, never fabricates a
 *   result — the caller's fallback chain (local .lrc → embedded →
 *   providers → empty) already handles null correctly.
 *
 * © RIKO Experience OS — Lyrics Host Tier
 */

package com.rikodev.music.android.presentation.lyrics.host

import java.io.File
import java.io.RandomAccessFile

object Id3UsltReader {

    // Defensive cap — real ID3v2 tags are almost always well under 1MB;
    // this guards against a malformed/hostile size field causing a huge
    // read attempt.
    private const val MAX_TAG_SIZE = 5 * 1024 * 1024

    /** @return (languageCode, lyricsText), or null if no USLT frame is found. */
    fun readUslt(file: File): Pair<String, String>? {
        if (!file.exists() || !file.canRead()) return null
        return try {
            RandomAccessFile(file, "r").use { raf -> readUsltFrom(raf) }
        } catch (e: Exception) {
            null
        }
    }

    private fun readUsltFrom(raf: RandomAccessFile): Pair<String, String>? {
        val header = ByteArray(10)
        if (raf.read(header) != 10) return null
        if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
            return null // no ID3v2 tag at file start
        }
        val majorVersion = header[3].toInt()
        if (majorVersion != 3 && majorVersion != 4) return null // out of scope, see header

        val tagSize = synchsafeToInt(header, 6)
        if (tagSize <= 0 || tagSize > MAX_TAG_SIZE) return null

        var remaining = tagSize
        while (remaining > 10) {
            val frameHeader = ByteArray(10)
            if (raf.read(frameHeader) != 10) break
            remaining -= 10

            val frameId = String(frameHeader, 0, 4, Charsets.ISO_8859_1)
            val frameSize = if (majorVersion == 4) synchsafeToInt(frameHeader, 4) else beInt(frameHeader, 4)
            if (frameSize <= 0 || frameSize > remaining) break

            if (frameId == "USLT") {
                val body = ByteArray(frameSize)
                if (raf.read(body) != frameSize) return null
                return parseUsltBody(body)
            }
            raf.seek(raf.filePointer + frameSize)
            remaining -= frameSize
        }
        return null
    }

    private fun parseUsltBody(body: ByteArray): Pair<String, String>? {
        if (body.size < 5) return null
        val encoding = body[0].toInt() and 0xFF
        val language = String(body, 1, 3, Charsets.ISO_8859_1).trim().ifBlank { "und" }

        val charset = when (encoding) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16   // BOM-prefixed, Java's UTF_16 decoder handles the BOM
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
        val terminatorWidth = if (encoding == 1 || encoding == 2) 2 else 1

        // Skip the content descriptor: null-terminated in the frame's own encoding.
        var descEnd = 4
        while (descEnd + terminatorWidth <= body.size) {
            val isTerminator = if (terminatorWidth == 1) {
                body[descEnd] == 0.toByte()
            } else {
                body[descEnd] == 0.toByte() && body[descEnd + 1] == 0.toByte()
            }
            if (isTerminator) break
            descEnd++
        }
        val textStart = (descEnd + terminatorWidth).coerceAtMost(body.size)
        if (textStart >= body.size) return null

        val text = String(body, textStart, body.size - textStart, charset)
            .trim('\u0000', ' ', '\n', '\r')
        return if (text.isBlank()) null else language to text
    }

    private fun synchsafeToInt(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)

    private fun beInt(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
}
