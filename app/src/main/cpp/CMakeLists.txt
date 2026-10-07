cmake_minimum_required(VERSION 3.22.1)
project(crate_fingerprint C CXX)

# Chromaprint 1.5.1 (LGPL-2.1+, vendored in chromaprint/) with its bundled
# KissFFT and resampler: no FFmpeg / FFTW needed. Audio is decoded in Kotlin
# with MediaCodec and fed here as 16-bit PCM.
set(CP ${CMAKE_CURRENT_SOURCE_DIR}/chromaprint)
file(GLOB CP_SOURCES ${CP}/src/*.cpp ${CP}/src/utils/*.cpp)
add_library(crate_fingerprint SHARED
    ${CP_SOURCES}
    ${CP}/src/avresample/resample2.c
    ${CP}/kissfft/kiss_fft.c
    ${CP}/kissfft/tools/kiss_fftr.c
    fingerprint_jni.cpp
)
target_include_directories(crate_fingerprint PRIVATE ${CP} ${CP}/src ${CP}/kissfft ${CP}/kissfft/tools)
target_compile_definitions(crate_fingerprint PRIVATE HAVE_CONFIG_H CHROMAPRINT_NODLL)
target_compile_options(crate_fingerprint PRIVATE -O2 -fvisibility=hidden)
target_link_options(crate_fingerprint PRIVATE -Wl,-z,max-page-size=16384)
