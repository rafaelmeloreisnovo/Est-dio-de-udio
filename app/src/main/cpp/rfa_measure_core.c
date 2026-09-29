/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#include "rfa_measure_core.h"
#include "rfa_wave_core.h"

static rfa_i16 rfa_measure_clip_i16(rfa_i64 value) {
    if (value > 32767) return (rfa_i16)32767;
    if (value < -32768) return (rfa_i16)-32768;
    return (rfa_i16)value;
}

void rfa_exp_sweep_reset_q31(rfa_exp_sweep_q31 *state,
                             rfa_u32 start_step_q32,
                             rfa_u32 ratio_q31,
                             rfa_i32 gain_q15,
                             int frames) {
    if (state == (rfa_exp_sweep_q31 *)0) return;
    if (gain_q15 > 32767) gain_q15 = 32767;
    if (gain_q15 < -32768) gain_q15 = -32768;
    state->phase = 0U;
    state->step_q32 = start_step_q32;
    state->ratio_q31 = ratio_q31 < 2147483648U ? 2147483648U : ratio_q31;
    state->gain_q15 = gain_q15;
    state->frames_left = frames < 0 ? 0 : frames;
}

int rfa_exp_sweep_render_q15(rfa_exp_sweep_q31 *state,
                             rfa_i16 *output, int frames, int channels) {
    int produced = 0;
    if (state == (rfa_exp_sweep_q31 *)0 || output == (rfa_i16 *)0) return 0;
    if (frames <= 0 || channels <= 0 || channels > 2) return 0;

    while (produced < frames && state->frames_left > 0) {
        rfa_i16 sine = rfa_wave_sine_q15(state->phase);
        rfa_i64 scaled = ((rfa_i64)sine * (rfa_i64)state->gain_q15) >> 15;
        int channel;
        rfa_u64 next_step;

        for (channel = 0; channel < channels; ++channel) {
            output[produced * channels + channel] = rfa_measure_clip_i16(scaled);
        }

        state->phase += state->step_q32;
        next_step = ((rfa_u64)state->step_q32 * (rfa_u64)state->ratio_q31) >> 31;
        if (next_step > 0xffffffffULL) next_step = 0xffffffffULL;
        state->step_q32 = (rfa_u32)next_step;
        --state->frames_left;
        ++produced;
    }
    return produced;
}

int rfa_sync_sequence_q15(rfa_i16 *output, int count,
                          rfa_u32 seed, rfa_i32 gain_q15) {
    rfa_u32 state;
    int i;
    if (output == (rfa_i16 *)0 || count <= 0) return 0;
    if (gain_q15 < 0) gain_q15 = -gain_q15;
    if (gain_q15 > 32767) gain_q15 = 32767;
    state = seed == 0U ? 0x6d2b79f5U : seed;
    for (i = 0; i < count; ++i) {
        state ^= state << 13;
        state ^= state >> 17;
        state ^= state << 5;
        output[i] = (state & 1U) != 0U ?
                (rfa_i16)gain_q15 : (rfa_i16)-gain_q15;
    }
    return count;
}

int rfa_relative_transfer_search(const rfa_i16 *reference, int reference_count,
                                 const rfa_i16 *response, int response_count,
                                 int min_lag, int max_lag,
                                 rfa_relative_transfer *result) {
    int lag;
    int best_lag = 0;
    rfa_i64 best_corr = 0LL;
    rfa_u64 best_ref_energy = 0ULL;
    rfa_u64 best_resp_energy = 0ULL;

    if (reference == (const rfa_i16 *)0 ||
        response == (const rfa_i16 *)0 ||
        result == (rfa_relative_transfer *)0) return 0;
    if (reference_count <= 0 || response_count <= 0) return 0;
    if (min_lag < 0) min_lag = 0;
    if (max_lag >= response_count) max_lag = response_count - 1;
    if (max_lag < min_lag) return 0;

    for (lag = min_lag; lag <= max_lag; ++lag) {
        int available = response_count - lag;
        int count = reference_count < available ? reference_count : available;
        int i;
        rfa_i64 corr = 0LL;
        rfa_u64 ref_energy = 0ULL;
        rfa_u64 resp_energy = 0ULL;

        if (count <= 0) continue;
        for (i = 0; i < count; ++i) {
            rfa_i64 a = reference[i];
            rfa_i64 b = response[i + lag];
            corr += a * b;
            ref_energy += (rfa_u64)(a * a);
            resp_energy += (rfa_u64)(b * b);
        }
        if (corr > best_corr) {
            best_corr = corr;
            best_lag = lag;
            best_ref_energy = ref_energy;
            best_resp_energy = resp_energy;
        }
    }

    result->correlation = best_corr;
    result->reference_energy = best_ref_energy;
    result->response_energy = best_resp_energy;
    result->best_lag = best_lag;
    result->analyzed = best_corr != 0LL ? 1 : 0;
    return result->analyzed;
}
