/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#include "rfa_biquad_core.h"

static rfa_i32 rfa_bq_clip_i32(rfa_i64 value) {
    if (value > 2147483647LL) return (rfa_i32)2147483647;
    if (value < (-2147483647LL - 1LL)) return (rfa_i32)(-2147483647 - 1);
    return (rfa_i32)value;
}

static rfa_i16 rfa_bq_clip_i16(rfa_i32 value) {
    if (value > 32767) return (rfa_i16)32767;
    if (value < -32768) return (rfa_i16)-32768;
    return (rfa_i16)value;
}

void rfa_biquad_bank_reset_q30(rfa_biquad_bank_q30 *bank, int channels) {
    int s;
    int c;
    if (bank == (rfa_biquad_bank_q30 *)0) return;
    if (channels <= 0) channels = 1;
    if (channels > 2) channels = 2;
    bank->channels = channels;
    bank->sections = 0;
    for (s = 0; s < RFA_BIQUAD_MAX_SECTIONS; ++s) {
        bank->coeff[s].b0_q30 = (rfa_i32)(1U << 30);
        bank->coeff[s].b1_q30 = 0;
        bank->coeff[s].b2_q30 = 0;
        bank->coeff[s].a1_q30 = 0;
        bank->coeff[s].a2_q30 = 0;
        for (c = 0; c < 2; ++c) {
            bank->state[s][c].x1 = 0;
            bank->state[s][c].x2 = 0;
            bank->state[s][c].y1 = 0;
            bank->state[s][c].y2 = 0;
        }
    }
}

int rfa_biquad_bank_set_q30(rfa_biquad_bank_q30 *bank, int section,
                            const rfa_biquad_coeff_q30 *coeff) {
    int c;
    if (bank == (rfa_biquad_bank_q30 *)0 ||
        coeff == (const rfa_biquad_coeff_q30 *)0) return 0;
    if (section < 0 || section >= RFA_BIQUAD_MAX_SECTIONS) return 0;
    bank->coeff[section] = *coeff;
    for (c = 0; c < 2; ++c) {
        bank->state[section][c].x1 = 0;
        bank->state[section][c].x2 = 0;
        bank->state[section][c].y1 = 0;
        bank->state[section][c].y2 = 0;
    }
    if (bank->sections <= section) bank->sections = section + 1;
    return 1;
}

void rfa_biquad_bank_process_q30(rfa_biquad_bank_q30 *bank,
                                 rfa_i16 *samples, int count, int channels) {
    int i;
    if (bank == (rfa_biquad_bank_q30 *)0 || samples == (rfa_i16 *)0) return;
    if (count <= 0 || channels <= 0 || channels > 2) return;
    for (i = 0; i < count; ++i) {
        int channel = i % channels;
        rfa_i32 value = samples[i];
        int section;
        for (section = 0; section < bank->sections; ++section) {
            const rfa_biquad_coeff_q30 *k = &bank->coeff[section];
            rfa_biquad_state_q30 *st = &bank->state[section][channel];
            rfa_i64 acc = (rfa_i64)k->b0_q30 * value +
                          (rfa_i64)k->b1_q30 * st->x1 +
                          (rfa_i64)k->b2_q30 * st->x2 -
                          (rfa_i64)k->a1_q30 * st->y1 -
                          (rfa_i64)k->a2_q30 * st->y2;
            rfa_i32 y = rfa_bq_clip_i32(acc >> 30);
            st->x2 = st->x1;
            st->x1 = value;
            st->y2 = st->y1;
            st->y1 = y;
            value = y;
        }
        samples[i] = rfa_bq_clip_i16(value);
    }
}
