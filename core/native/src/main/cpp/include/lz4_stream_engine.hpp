#pragma once

#include "native_kernel.hpp"
#include <array>
#include <cstddef>
#include <cstdint>

namespace nexmesh::native {

/**
 * Zero-allocation, bounded-buffer LZ4-style block compressor/decompressor.
 * Uses a fixed 16KB hash table inside the RAII object—zero heap allocations per block.
 * Fully compatible with Zero-OOM streaming (max 256KB per block).
 */
class Lz4StreamEngine final : public INativeKernel {
public:
    static constexpr size_t kHashLog = 12; // 4096 entries * 4 bytes = 16KB fixed footprint
    static constexpr size_t kHashTableSize = 1u << kHashLog;
    static constexpr uint32_t kMinMatch = 4;
    static constexpr uint32_t kMaxDistance = 65535;

    Lz4StreamEngine() noexcept;
    ~Lz4StreamEngine() override = default;

    const char* name() const noexcept override { return "nexmesh-lz4-stream-v1"; }

    static size_t maxCompressedSize(size_t inputSize) noexcept {
        return inputSize + (inputSize / 255) + 16;
    }

    KernelStatus compressBlock(
        const uint8_t* src,
        size_t srcSize,
        uint8_t* dst,
        size_t dstCapacity,
        size_t& outCompressedSize
    ) noexcept;

    KernelStatus decompressBlock(
        const uint8_t* src,
        size_t srcSize,
        uint8_t* dst,
        size_t dstCapacity,
        size_t& outDecompressedSize
    ) noexcept;

private:
    std::array<uint32_t, kHashTableSize> hashTable_{};

    static inline uint32_t readU32(const uint8_t* p) noexcept {
        return static_cast<uint32_t>(p[0]) |
               (static_cast<uint32_t>(p[1]) << 8) |
               (static_cast<uint32_t>(p[2]) << 16) |
               (static_cast<uint32_t>(p[3]) << 24);
    }

    static inline uint32_t hash4(uint32_t v) noexcept {
        return (v * 2654435761u) >> (32 - kHashLog);
    }
};

} // namespace nexmesh::native
