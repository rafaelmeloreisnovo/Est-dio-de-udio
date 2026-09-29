/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#include "rfa_fir_core.h"

static rfa_i16 rfa_fir_clip_i16(rfa_i64 value) {
    if (value > 32767) return (rfa_i16)32767;
    if (value < -32768) return (rfa_i16)-32768;
    return (rfa_i16)value;
}

int rfa_fir_bind_q15(rfa_fir_q15 *state, const rfa_i16 *coeff_q15,
                     rfa_i16 *history, int taps) {
    if (state == (rfa_fir_q15 *)0 || coeff_q15 == (const rfa_i16 *)0 ||
        history == (rfa_i16 *)0) return 0;
    if (taps <= 0 || taps > 4096) return 0;
    state->coeff_q15 = coeff_q15;
    state->history = history;
    state->taps = taps;
    state->position = 0;
    rfa_fir_reset_q15(state);
    return 1;
}

void rfa_fir_reset_q15(rfa_fir_q15 *state) {
    int i;
    if (state == (rfa_fir_q15 *)0 || state->history == (rfa_i16 *)0) return;
    for (i = 0; i < state->taps; ++i) state->history[i] = 0;
    state->position = 0;
}

rfa_i16 rfa_fir_process_sample_q15(rfa_fir_q15 *state, rfa_i16 sample) {
    rfa_i64 acc = 0;
    int tap;
    int index;
    if (state == (rfa_fir_q15 *)0 || state->history == (rfa_i16 *)0 ||
        state->coeff_q15 == (const rfa_i16 *)0 || state->taps <= 0) return sample;

    state->history[state->position] = sample;
    index = state->position;
    for (tap = 0; tap < state->taps; ++tap) {
        acc += (rfa_i64)state->coeff_q15[tap] * (rfa_i64)state->history[index];
        --index;
        if (index < 0) index = state->taps - 1;
    }
    ++state->position;
    if (state->position >= state->taps) state->position = 0;
    return rfa_fir_clip_i16(acc >> 15);
}

void rfa_fir_process_block_q15(rfa_fir_q15 *state,
                               const rfa_i16 *input, rfa_i16 *output, int count) {
    int i;
    if (state == (rfa_fir_q15 *)0 || input == (const rfa_i16 *)0 ||
        output == (rfa_i16 *)0 || count <= 0) return;
    for (i = 0; i < count; ++i) {
        output[i] = rfa_fir_process_sample_q15(state, input[i]);
    }
}
