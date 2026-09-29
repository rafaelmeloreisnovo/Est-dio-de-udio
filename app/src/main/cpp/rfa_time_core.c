/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#include "rfa_time_core.h"

static rfa_i16 rfa_time_clip_i16(rfa_i64 value) {
    if (value > 32767) return (rfa_i16)32767;
    if (value < -32768) return (rfa_i16)-32768;
    return (rfa_i16)value;
}

void rfa_time_map_reset_q16(rfa_time_map_q16 *state, rfa_u32 step_q16) {
    if (state == (rfa_time_map_q16 *)0) return;
    state->phase_q16 = 0U;
    state->step_q16 = step_q16 == 0U ? 65536U : step_q16;
}

int rfa_time_resample_linear_q16(rfa_time_map_q16 *state,
                                 const rfa_i16 *input, int input_frames,
                                 int channels, rfa_i16 *output, int output_frames) {
    int produced = 0;
    if (state == (rfa_time_map_q16 *)0 || input == (const rfa_i16 *)0 ||
        output == (rfa_i16 *)0) return 0;
    if (input_frames < 2 || output_frames <= 0 || channels <= 0 || channels > 2) return 0;

    while (produced < output_frames) {
        rfa_u32 index = state->phase_q16 >> 16;
        rfa_u32 frac = state->phase_q16 & 0xffffU;
        int channel;
        if (index + 1U >= (rfa_u32)input_frames) break;

        for (channel = 0; channel < channels; ++channel) {
            rfa_i32 a = input[(int)index * channels + channel];
            rfa_i32 b = input[((int)index + 1) * channels + channel];
            rfa_i64 delta = (rfa_i64)(b - a) * (rfa_i64)frac;
            rfa_i64 value = (rfa_i64)a + (delta >> 16);
            output[produced * channels + channel] = rfa_time_clip_i16(value);
        }
        state->phase_q16 += state->step_q16;
        ++produced;
    }
    return produced;
}
