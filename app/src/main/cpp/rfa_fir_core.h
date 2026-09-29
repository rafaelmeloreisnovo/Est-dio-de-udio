/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#ifndef RFA_FIR_CORE_H
#define RFA_FIR_CORE_H

#include "rfa_core_types.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    const rfa_i16 *coeff_q15;
    rfa_i16 *history;
    int taps;
    int position;
} rfa_fir_q15;

int rfa_fir_bind_q15(rfa_fir_q15 *state, const rfa_i16 *coeff_q15,
                     rfa_i16 *history, int taps);
void rfa_fir_reset_q15(rfa_fir_q15 *state);
rfa_i16 rfa_fir_process_sample_q15(rfa_fir_q15 *state, rfa_i16 sample);
void rfa_fir_process_block_q15(rfa_fir_q15 *state,
                               const rfa_i16 *input, rfa_i16 *output, int count);

#ifdef __cplusplus
}
#endif

#endif
