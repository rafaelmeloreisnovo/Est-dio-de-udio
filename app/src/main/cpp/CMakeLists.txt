cmake_minimum_required(VERSION 3.22.1)
project(rafaelia_audio C)

add_library(dsp_core STATIC dsp_core.c)
set_target_properties(dsp_core PROPERTIES POSITION_INDEPENDENT_CODE ON)

target_compile_options(dsp_core PRIVATE
    -O3
    -ffreestanding
    -fno-builtin
    -ffunction-sections
    -fdata-sections
    -Wall
    -Wextra
    -Werror
)

add_library(rafaelia_audio SHARED jni_bridge.c)

target_compile_options(rafaelia_audio PRIVATE
    -O3
    -ffunction-sections
    -fdata-sections
    -Wall
    -Wextra
    -Werror
)

target_link_libraries(rafaelia_audio PRIVATE dsp_core)

target_link_options(rafaelia_audio PRIVATE
    -Wl,--gc-sections
    -Wl,--exclude-libs,ALL
)
