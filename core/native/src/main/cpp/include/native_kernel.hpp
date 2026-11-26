#pragma once

#include <atomic>
#include <cstddef>
#include <cstdint>

namespace nexmesh::native {

// Bounded Zero-OOM ceiling: never allow a single native stream call to exceed 256KB
constexpr size_t kMaxStreamBlockBytes = 256 * 1024;
constexpr size_t kDefaultStreamBlockBytes = 128 * 1024;

enum class KernelStatus : int32_t {
    OK = 0,
    ERR_INVALID_ARGS = -1,
    ERR_BUFFER_TOO_SMALL = -2,
    ERR_BLOCK_LIMIT_EXCEEDED = -3,
    ERR_CORRUPT_STREAM = -4,
    ERR_CANCELLED = -5,
    ERR_INTERNAL = -6
};

/**
 * Extensible RAII base interface for all Phase 1 (LZ4, xxHash64)
 * and Phase 3 (distributed compute) native kernels.
 */
class INativeKernel {
public:
    virtual ~INativeKernel() = default;

    virtual const char* name() const noexcept = 0;

    void requestCancel() noexcept {
        cancelled_.store(true, std::memory_order_release);
    }

    void resetCancel() noexcept {
        cancelled_.store(false, std::memory_order_release);
    }

    bool isCancelled() const noexcept {
        return cancelled_.load(std::memory_order_acquire);
    }

protected:
    std::atomic<bool> cancelled_{false};
};

} // namespace nexmesh::native
