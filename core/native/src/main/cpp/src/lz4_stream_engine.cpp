#include "lz4_stream_engine.hpp"
#include <algorithm>
#include <cstring>

namespace nexmesh::native {

Lz4StreamEngine::Lz4StreamEngine() noexcept {
    hashTable_.fill(0);
}

KernelStatus Lz4StreamEngine::compressBlock(
    const uint8_t* src,
    size_t srcSize,
    uint8_t* dst,
    size_t dstCapacity,
    size_t& outCompressedSize
) noexcept {
    outCompressedSize = 0;
    if (!src || !dst) return KernelStatus::ERR_INVALID_ARGS;
    if (srcSize == 0) return KernelStatus::OK;
    if (srcSize > kMaxStreamBlockBytes) return KernelStatus::ERR_BLOCK_LIMIT_EXCEEDED;
    if (dstCapacity < maxCompressedSize(srcSize)) return KernelStatus::ERR_BUFFER_TOO_SMALL;

    hashTable_.fill(0);

    const uint8_t* const srcEnd = src + srcSize;
    const uint8_t* const matchLimit = (srcSize > 12) ? (srcEnd - 12) : src;
    const uint8_t* ip = src;
    const uint8_t* anchor = src;
    uint8_t* op = dst;
    uint8_t* const dstEnd = dst + dstCapacity;

    size_t iterCount = 0;

    while (ip < matchLimit) {
        if ((++iterCount & 0x3FFu) == 0 && isCancelled()) {
            return KernelStatus::ERR_CANCELLED;
        }

        const uint32_t seq = readU32(ip);
        const uint32_t h = hash4(seq);
        const uint32_t refIndex = hashTable_[h];
        const uint32_t curIndex = static_cast<uint32_t>(ip - src);
        hashTable_[h] = curIndex;

        const uint8_t* ref = src + refIndex;
        const uint32_t distance = curIndex - refIndex;

        if (distance == 0 || distance > kMaxDistance || refIndex >= curIndex || readU32(ref) != seq) {
            ++ip;
            continue;
        }

        // Extend match
        size_t matchLen = kMinMatch;
        const size_t maxMatch = static_cast<size_t>(srcEnd - 5 - ip);
        while (matchLen < maxMatch && ip[matchLen] == ref[matchLen]) {
            ++matchLen;
        }

        const size_t litLen = static_cast<size_t>(ip - anchor);
        if (op + 1 + (litLen / 255) + litLen + 2 + (matchLen / 255) >= dstEnd) {
            return KernelStatus::ERR_BUFFER_TOO_SMALL;
        }

        uint8_t* token = op++;
        if (litLen >= 15) {
            *token = 0xF0u;
            size_t rem = litLen - 15;
            while (rem >= 255) {
                *op++ = 255;
                rem -= 255;
            }
            *op++ = static_cast<uint8_t>(rem);
        } else {
            *token = static_cast<uint8_t>(litLen << 4);
        }

        if (litLen > 0) {
            std::memcpy(op, anchor, litLen);
            op += litLen;
        }

        // Write 16-bit little-endian offset
        *op++ = static_cast<uint8_t>(distance & 0xFFu);
        *op++ = static_cast<uint8_t>((distance >> 8) & 0xFFu);

        const size_t encodedMatchLen = matchLen - kMinMatch;
        if (encodedMatchLen >= 15) {
            *token |= 0x0Fu;
            size_t rem = encodedMatchLen - 15;
            while (rem >= 255) {
                *op++ = 255;
                rem -= 255;
            }
            *op++ = static_cast<uint8_t>(rem);
        } else {
            *token |= static_cast<uint8_t>(encodedMatchLen);
        }

        ip += matchLen;
        anchor = ip;
    }

    if (isCancelled()) {
        return KernelStatus::ERR_CANCELLED;
    }

    // Encode trailing literals
    const size_t lastLitLen = static_cast<size_t>(srcEnd - anchor);
    if (op + 1 + (lastLitLen / 255) + lastLitLen > dstEnd) {
        return KernelStatus::ERR_BUFFER_TOO_SMALL;
    }

    if (lastLitLen >= 15) {
        *op++ = 0xF0u;
        size_t rem = lastLitLen - 15;
        while (rem >= 255) {
            *op++ = 255;
            rem -= 255;
        }
        *op++ = static_cast<uint8_t>(rem);
    } else {
        *op++ = static_cast<uint8_t>(lastLitLen << 4);
    }

    if (lastLitLen > 0) {
        std::memcpy(op, anchor, lastLitLen);
        op += lastLitLen;
    }

    outCompressedSize = static_cast<size_t>(op - dst);
    return KernelStatus::OK;
}

KernelStatus Lz4StreamEngine::decompressBlock(
    const uint8_t* src,
    size_t srcSize,
    uint8_t* dst,
    size_t dstCapacity,
    size_t& outDecompressedSize
) noexcept {
    outDecompressedSize = 0;
    if (!src || !dst) return KernelStatus::ERR_INVALID_ARGS;
    if (srcSize == 0) return KernelStatus::OK;
    if (dstCapacity > kMaxStreamBlockBytes) return KernelStatus::ERR_BLOCK_LIMIT_EXCEEDED;

    const uint8_t* ip = src;
    const uint8_t* const srcEnd = src + srcSize;
    uint8_t* op = dst;
    uint8_t* const dstEnd = dst + dstCapacity;

    size_t iterCount = 0;

    while (ip < srcEnd) {
        if ((++iterCount & 0x3FFu) == 0 && isCancelled()) {
            return KernelStatus::ERR_CANCELLED;
        }

        const uint8_t token = *ip++;
        size_t litLen = static_cast<size_t>(token >> 4);
        if (litLen == 15) {
            uint8_t s = 0;
            do {
                if (ip >= srcEnd) return KernelStatus::ERR_CORRUPT_STREAM;
                s = *ip++;
                litLen += s;
            } while (s == 255);
        }

        if (ip + litLen > srcEnd) return KernelStatus::ERR_CORRUPT_STREAM;
        if (op + litLen > dstEnd) return KernelStatus::ERR_BUFFER_TOO_SMALL;

        if (litLen > 0) {
            std::memcpy(op, ip, litLen);
            ip += litLen;
            op += litLen;
        }

        if (ip >= srcEnd) {
            break; // End of block after final literals
        }

        if (ip + 2 > srcEnd) return KernelStatus::ERR_CORRUPT_STREAM;
        const uint32_t offset = static_cast<uint32_t>(ip[0]) | (static_cast<uint32_t>(ip[1]) << 8);
        ip += 2;

        if (offset == 0 || static_cast<size_t>(op - dst) < offset) {
            return KernelStatus::ERR_CORRUPT_STREAM;
        }

        size_t matchLen = static_cast<size_t>(token & 0x0Fu) + kMinMatch;
        if ((token & 0x0Fu) == 15) {
            uint8_t s = 0;
            do {
                if (ip >= srcEnd) return KernelStatus::ERR_CORRUPT_STREAM;
                s = *ip++;
                matchLen += s;
            } while (s == 255);
        }

        if (op + matchLen > dstEnd) return KernelStatus::ERR_BUFFER_TOO_SMALL;

        const uint8_t* matchPtr = op - offset;
        for (size_t i = 0; i < matchLen; ++i) {
            op[i] = matchPtr[i];
        }
        op += matchLen;
    }

    outDecompressedSize = static_cast<size_t>(op - dst);
    return KernelStatus::OK;
}

} // namespace nexmesh::native
