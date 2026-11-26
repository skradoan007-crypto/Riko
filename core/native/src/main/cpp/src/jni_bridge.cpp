#include <jni.h>
#include <cstdint>
#include <memory>
#include <mutex>
#include <unordered_map>

#include "lz4_stream_engine.hpp"
#include "fast_hash_engine.hpp"

using namespace nexmesh::native;

namespace {

struct NativeSession {
    std::unique_ptr<Lz4StreamEngine> lz4;
    std::unique_ptr<FastHashEngine> hasher;
};

std::mutex gSessionMutex;
std::unordered_map<int64_t, std::shared_ptr<NativeSession>> gSessions;
std::atomic<int64_t> gNextHandle{1};

std::shared_ptr<NativeSession> findSession(int64_t handle) {
    std::lock_guard<std::mutex> lock(gSessionMutex);
    auto it = gSessions.find(handle);
    return (it != gSessions.end()) ? it->second : nullptr;
}

} // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_rikodev_sync_nexmesh_core_nativeengine_NativeEngineBridge_nativeCreateSession(
    JNIEnv*,
    jobject,
    jlong hashSeed
) {
    auto session = std::make_shared<NativeSession>();
    session->lz4 = std::make_unique<Lz4StreamEngine>();
    session->hasher = std::make_unique<FastHashEngine>(static_cast<uint64_t>(hashSeed));

    const int64_t handle = gNextHandle.fetch_add(1, std::memory_order_relaxed);
    {
        std::lock_guard<std::mutex> lock(gSessionMutex);
        gSessions.emplace(handle, std::move(session));
    }
    return static_cast<jlong>(handle);
}

JNIEXPORT void JNICALL
Java_com_rikodev_sync_nexmesh_core_nativeengine_NativeEngineBridge_nativeCancelSession(
    JNIEnv*,
    jobject,
    jlong handle
) {
    if (auto session = findSession(static_cast<int64_t>(handle))) {
        session->lz4->requestCancel();
        session->hasher->requestCancel();
    }
}

JNIEXPORT void JNICALL
Java_com_rikodev_sync_nexmesh_core_nativeengine_NativeEngineBridge_nativeDestroySession(
    JNIEnv*,
    jobject,
    jlong handle
) {
    std::lock_guard<std::mutex> lock(gSessionMutex);
    gSessions.erase(static_cast<int64_t>(handle));
}

JNIEXPORT jint JNICALL
Java_com_rikodev_sync_nexmesh_core_nativeengine_NativeEngineBridge_nativeCompressDirect(
    JNIEnv* env,
    jobject,
    jlong handle,
    jobject srcBuffer,
    jint srcLength,
    jobject dstBuffer,
    jint dstCapacity
) {
    auto session = findSession(static_cast<int64_t>(handle));
    if (!session || !srcBuffer || !dstBuffer || srcLength < 0 || dstCapacity < 0) {
        return static_cast<jint>(KernelStatus::ERR_INVALID_ARGS);
    }

    auto* srcPtr = static_cast<const uint8_t*>(env->GetDirectBufferAddress(srcBuffer));
    auto* dstPtr = static_cast<uint8_t*>(env->GetDirectBufferAddress(dstBuffer));
    const jlong srcCap = env->GetDirectBufferCapacity(srcBuffer);
    const jlong dstCap = env->GetDirectBufferCapacity(dstBuffer);

    if (!srcPtr || !dstPtr || srcLength > srcCap || dstCapacity > dstCap) {
        return static_cast<jint>(KernelStatus::ERR_INVALID_ARGS);
    }

    size_t outSize = 0;
    const KernelStatus status = session->lz4->compressBlock(
        srcPtr,
        static_cast<size_t>(srcLength),
        dstPtr,
        static_cast<size_t>(dstCapacity),
        outSize
    );
    if (status != KernelStatus::OK) {
        return static_cast<jint>(status);
    }
    return static_cast<jint>(outSize);
}

JNIEXPORT jint JNICALL
Java_com_rikodev_sync_nexmesh_core_nativeengine_NativeEngineBridge_nativeDecompressDirect(
    JNIEnv* env,
    jobject,
    jlong handle,
    jobject srcBuffer,
    jint srcLength,
    jobject dstBuffer,
    jint dstCapacity
) {
    auto session = findSession(static_cast<int64_t>(handle));
    if (!session || !srcBuffer || !dstBuffer || srcLength < 0 || dstCapacity < 0) {
        return static_cast<jint>(KernelStatus::ERR_INVALID_ARGS);
    }

    auto* srcPtr = static_cast<const uint8_t*>(env->GetDirectBufferAddress(srcBuffer));
    auto* dstPtr = static_cast<uint8_t*>(env->GetDirectBufferAddress(dstBuffer));
    const jlong srcCap = env->GetDirectBufferCapacity(srcBuffer);
    const jlong dstCap = env->GetDirectBufferCapacity(dstBuffer);

    if (!srcPtr || !dstPtr || srcLength > srcCap || dstCapacity > dstCap) {
        return static_cast<jint>(KernelStatus::ERR_INVALID_ARGS);
    }

    size_t outSize = 0;
    const KernelStatus status = session->lz4->decompressBlock(
        srcPtr,
        static_cast<size_t>(srcLength),
        dstPtr,
        static_cast<size_t>(dstCapacity),
        outSize
    );
    if (status != KernelStatus::OK) {
        return static_cast<jint>(status);
    }
    return static_cast<jint>(outSize);
}

JNIEXPORT jint JNICALL
Java_com_rikodev_sync_nexmesh_core_nativeengine_NativeEngineBridge_nativeHashUpdateDirect(
    JNIEnv* env,
    jobject,
    jlong handle,
    jobject dataBuffer,
    jint length
) {
    auto session = findSession(static_cast<int64_t>(handle));
    if (!session || !dataBuffer || length < 0) {
        return static_cast<jint>(KernelStatus::ERR_INVALID_ARGS);
    }

    auto* dataPtr = static_cast<const uint8_t*>(env->GetDirectBufferAddress(dataBuffer));
    const jlong cap = env->GetDirectBufferCapacity(dataBuffer);
    if (!dataPtr || length > cap) {
        return static_cast<jint>(KernelStatus::ERR_INVALID_ARGS);
    }

    return static_cast<jint>(session->hasher->update(dataPtr, static_cast<size_t>(length)));
}

JNIEXPORT jlong JNICALL
Java_com_rikodev_sync_nexmesh_core_nativeengine_NativeEngineBridge_nativeHashDigestXx64(
    JNIEnv*,
    jobject,
    jlong handle
) {
    auto session = findSession(static_cast<int64_t>(handle));
    if (!session) return 0L;
    return static_cast<jlong>(session->hasher->digestXxHash64());
}

JNIEXPORT jint JNICALL
Java_com_rikodev_sync_nexmesh_core_nativeengine_NativeEngineBridge_nativeHashDigestCrc32c(
    JNIEnv*,
    jobject,
    jlong handle
) {
    auto session = findSession(static_cast<int64_t>(handle));
    if (!session) return 0;
    return static_cast<jint>(session->hasher->digestCrc32c());
}

JNIEXPORT jstring JNICALL
Java_com_rikodev_sync_nexmesh_core_nativeengine_NativeEngineBridge_nativeEngineInfo(
    JNIEnv* env,
    jobject
) {
#if defined(__aarch64__)
    const char* abi = "arm64-v8a (ARMv8+CRC32C)";
#elif defined(__arm__)
    const char* abi = "armeabi-v7a (NEON)";
#else
    const char* abi = "host-native";
#endif
    return env->NewStringUTF(abi);
}

} // extern "C"
