cmake_minimum_required(VERSION 3.22.1)
project(rafaelia_audio C)

add_library(dsp_core STATIC dsp_core.c)
add_library(meter_core STATIC meter_core.c)
add_library(rfa_manifold_core STATIC
    rfa_wave_core.c
    rfa_matrix_core.c
    rfa_block_core.c
    rfa_container_core.c
    rfa_fir_core.c
    rfa_time_core.c
    rfa_lms_core.c
    rfa_biquad_core.c
    rfa_voice_core.c
    rfa_measure_core.c
    rfa_rac1_core.c
)

set_target_properties(dsp_core meter_core rfa_manifold_core PROPERTIES
    POSITION_INDEPENDENT_CODE ON
)

set(RFA_CORE_FLAGS
    -O3
    -ffreestanding
    -fno-builtin
    -nostdinc
    -fno-stack-protector
    -fno-unwind-tables
    -fno-asynchronous-unwind-tables
    -fno-common
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -Wall
    -Wextra
    -Werror
)

target_compile_options(dsp_core PRIVATE ${RFA_CORE_FLAGS})
target_compile_options(meter_core PRIVATE ${RFA_CORE_FLAGS})
target_compile_options(rfa_manifold_core PRIVATE ${RFA_CORE_FLAGS})

add_library(rafaelia_audio SHARED jni_bridge.c)

target_compile_options(rafaelia_audio PRIVATE
    -O3
    -ffunction-sections
    -fdata-sections
    -Wall
    -Wextra
    -Werror
)

target_link_libraries(rafaelia_audio PRIVATE dsp_core meter_core rfa_manifold_core)

target_link_options(rafaelia_audio PRIVATE
    -Wl,--gc-sections
    -Wl,--exclude-libs,ALL
)
