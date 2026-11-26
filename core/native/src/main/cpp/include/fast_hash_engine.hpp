#pragma once

#include "native_kernel.hpp"
#include <array>
#include <cstddef>
#include <cstdint>

namespace nexmesh::native {

/**
 * Streaming xxHash64 + Hardware-aware CRC32C fast integrity engine.
 * Complements Android's hardware SHA-256 for rapid per-frame pre-validation.
 */
class FastHashEngine final : public INativeKernel {
public:
    explicit FastHashEngine(uint64_t seed = 0) noexcept;
    ~FastHashEngine() override = default;

    const char* name() const noexcept override { return "nexmesh-xxhash64-crc32c-v1"; }

    void reset(uint64_t seed = 0) noexcept;
    KernelStatus update(const uint8_t* data, size_t length) noexcept;
    uint64_t digestXxHash64() const noexcept;
    uint32_t digestCrc32c() const noexcept { return crc32c_ ^ 0xFFFFFFFFu; }

    static uint32_t computeCrc32cBlock(uint32_t prevCrc, const uint8_t* data, size_t length) noexcept;

private:
    static constexpr uint64_t kPrime1 = 11400714785074694791ULL;
    static constexpr uint64_t kPrime2 = 14029467366897019727ULL;
    static constexpr uint64_t kPrime3 = 1609587929392839161ULL;
    static constexpr uint64_t kPrime4 = 9650029242287828579ULL;
    static constexpr uint64_t kPrime5 = 2870177450012600261ULL;

    uint64_t totalLen_{0};
    uint64_t v1_{0}, v2_{0}, v3_{0}, v4_{0};
    std::array<uint8_t, 32> mem_{};
    size_t memSize_{0};
    uint32_t crc32c_{0xFFFFFFFFu};

    static inline uint64_t rotl64(uint64_t x, int r) noexcept {
        return (x << r) | (x >> (64 - r));
    }

    static inline uint64_t readU64(const uint8_t* p) noexcept {
        uint64_t v = 0;
        for (int i = 7; i >= 0; --i) {
            v = (v << 8) | p[i];
        }
        return v;
    }

    static inline uint64_t round(uint64_t acc, uint64_t input) noexcept {
        acc += input * kPrime2;
        acc = rotl64(acc, 31);
        acc *= kPrime1;
        return acc;
    }

    static inline uint64_t mergeRound(uint64_t acc, uint64_t val) noexcept {
        val = round(0, val);
        acc ^= val;
        acc = acc * kPrime1 + kPrime4;
        return acc;
    }
};

} // namespace nexmesh::native
