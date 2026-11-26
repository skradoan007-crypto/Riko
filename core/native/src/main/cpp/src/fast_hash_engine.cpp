#include "fast_hash_engine.hpp"
#include <cstring>

#if defined(__ARM_FEATURE_CRC32)
#include <arm_acle.h>
#endif

namespace nexmesh::native {

FastHashEngine::FastHashEngine(uint64_t seed) noexcept {
    reset(seed);
}

void FastHashEngine::reset(uint64_t seed) noexcept {
    totalLen_ = 0;
    v1_ = seed + kPrime1 + kPrime2;
    v2_ = seed + kPrime2;
    v3_ = seed;
    v4_ = seed - kPrime1;
    memSize_ = 0;
    crc32c_ = 0xFFFFFFFFu;
    resetCancel();
}

uint32_t FastHashEngine::computeCrc32cBlock(uint32_t prevCrc, const uint8_t* data, size_t length) noexcept {
    uint32_t crc = prevCrc;
    size_t i = 0;

#if defined(__ARM_FEATURE_CRC32)
    while (i + 8 <= length) {
        uint64_t chunk = 0;
        std::memcpy(&chunk, data + i, sizeof(chunk));
        crc = __crc32cd(crc, chunk);
        i += 8;
    }
    while (i < length) {
        crc = __crc32cb(crc, data[i]);
        ++i;
    }
#else
    constexpr uint32_t kPoly = 0x82F63B78u;
    while (i < length) {
        crc ^= data[i++];
        for (int b = 0; b < 8; ++b) {
            crc = (crc & 1u) ? ((crc >> 1) ^ kPoly) : (crc >> 1);
        }
    }
#endif
    return crc;
}

KernelStatus FastHashEngine::update(const uint8_t* data, size_t length) noexcept {
    if (!data && length > 0) return KernelStatus::ERR_INVALID_ARGS;
    if (length == 0) return KernelStatus::OK;
    if (length > kMaxStreamBlockBytes) return KernelStatus::ERR_BLOCK_LIMIT_EXCEEDED;
    if (isCancelled()) return KernelStatus::ERR_CANCELLED;

    crc32c_ = computeCrc32cBlock(crc32c_, data, length);

    const uint8_t* p = data;
    const uint8_t* const end = data + length;
    totalLen_ += length;

    if (memSize_ + length < 32) {
        std::memcpy(mem_.data() + memSize_, data, length);
        memSize_ += length;
        return KernelStatus::OK;
    }

    if (memSize_ > 0) {
        const size_t fill = 32 - memSize_;
        std::memcpy(mem_.data() + memSize_, p, fill);
        v1_ = round(v1_, readU64(mem_.data()));
        v2_ = round(v2_, readU64(mem_.data() + 8));
        v3_ = round(v3_, readU64(mem_.data() + 16));
        v4_ = round(v4_, readU64(mem_.data() + 24));
        p += fill;
        memSize_ = 0;
    }

    while (p + 32 <= end) {
        v1_ = round(v1_, readU64(p));
        v2_ = round(v2_, readU64(p + 8));
        v3_ = round(v3_, readU64(p + 16));
        v4_ = round(v4_, readU64(p + 24));
        p += 32;
    }

    if (p < end) {
        memSize_ = static_cast<size_t>(end - p);
        std::memcpy(mem_.data(), p, memSize_);
    }

    return KernelStatus::OK;
}

uint64_t FastHashEngine::digestXxHash64() const noexcept {
    uint64_t h64 = 0;
    if (totalLen_ >= 32) {
        h64 = rotl64(v1_, 1) + rotl64(v2_, 7) + rotl64(v3_, 12) + rotl64(v4_, 18);
        h64 = mergeRound(h64, v1_);
        h64 = mergeRound(h64, v2_);
        h64 = mergeRound(h64, v3_);
        h64 = mergeRound(h64, v4_);
    } else {
        h64 = v3_ + kPrime5;
    }

    h64 += totalLen_;

    const uint8_t* p = mem_.data();
    const uint8_t* const end = mem_.data() + memSize_;

    while (p + 8 <= end) {
        uint64_t k1 = round(0, readU64(p));
        h64 ^= k1;
        h64 = rotl64(h64, 27) * kPrime1 + kPrime4;
        p += 8;
    }

    if (p + 4 <= end) {
        uint32_t v = static_cast<uint32_t>(p[0]) |
                     (static_cast<uint32_t>(p[1]) << 8) |
                     (static_cast<uint32_t>(p[2]) << 16) |
                     (static_cast<uint32_t>(p[3]) << 24);
        h64 ^= static_cast<uint64_t>(v) * kPrime1;
        h64 = rotl64(h64, 23) * kPrime2 + kPrime3;
        p += 4;
    }

    while (p < end) {
        h64 ^= static_cast<uint64_t>(*p) * kPrime5;
        h64 = rotl64(h64, 11) * kPrime1;
        ++p;
    }

    h64 ^= h64 >> 33;
    h64 *= kPrime2;
    h64 ^= h64 >> 29;
    h64 *= kPrime3;
    h64 ^= h64 >> 32;
    return h64;
}

} // namespace nexmesh::native
