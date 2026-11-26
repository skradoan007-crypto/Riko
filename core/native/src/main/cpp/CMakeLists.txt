cmake_minimum_required(VERSION 3.22.1)
project(nexmesh_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(nexmesh_native SHARED
    src/lz4_stream_engine.cpp
    src/fast_hash_engine.cpp
    src/jni_bridge.cpp
)

target_include_directories(nexmesh_native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Android 15+ 16KB ELF page-size alignment for ARM64 & safe optimization flags
if(ANDROID)
    target_link_options(nexmesh_native PRIVATE "-Wl,-z,max-page-size=16384")
    if(ANDROID_ABI STREQUAL "arm64-v8a")
        target_compile_options(nexmesh_native PRIVATE -march=armv8-a+crc)
    elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
        target_compile_options(nexmesh_native PRIVATE -mfpu=neon)
    endif()
    find_library(log-lib log)
    target_link_libraries(nexmesh_native PRIVATE ${log-lib})
endif()
