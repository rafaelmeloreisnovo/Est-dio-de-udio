/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#ifndef RFA_BIQUAD_CORE_H
#define RFA_BIQUAD_CORE_H

#include "rfa_core_types.h"

#ifdef __cplusplus
extern "C" {
#endif

enum {
    RFA_BIQUAD_MAX_SECTIONS = 16
};

typedef struct {
    rfa_i32 b0_q30;
    rfa_i32 b1_q30;
    rfa_i32 b2_q30;
    rfa_i32 a1_q30;
    rfa_i32 a2_q30;
} rfa_biquad_coeff_q30;

typedef struct {
    rfa_i32 x1;
    rfa_i32 x2;
    rfa_i32 y1;
    rfa_i32 y2;
} rfa_biquad_state_q30;

typedef struct {
    rfa_biquad_coeff_q30 coeff[RFA_BIQUAD_MAX_SECTIONS];
    rfa_biquad_state_q30 state[RFA_BIQUAD_MAX_SECTIONS][2];
    int sections;
    int channels;
} rfa_biquad_bank_q30;

void rfa_biquad_bank_reset_q30(rfa_biquad_bank_q30 *bank, int channels);
int rfa_biquad_bank_set_q30(rfa_biquad_bank_q30 *bank, int section,
                            const rfa_biquad_coeff_q30 *coeff);
void rfa_biquad_bank_process_q30(rfa_biquad_bank_q30 *bank,
                                 rfa_i16 *samples, int count, int channels);

#ifdef __cplusplus
}
#endif

#endif
