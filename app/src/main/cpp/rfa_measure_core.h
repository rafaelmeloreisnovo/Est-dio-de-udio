/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#ifndef RFA_MEASURE_CORE_H
#define RFA_MEASURE_CORE_H

#include "rfa_core_types.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    rfa_u32 phase;
    rfa_u32 step_q32;
    rfa_u32 ratio_q31;
    rfa_i32 gain_q15;
    int frames_left;
} rfa_exp_sweep_q31;

typedef struct {
    rfa_i64 correlation;
    rfa_u64 reference_energy;
    rfa_u64 response_energy;
    int best_lag;
    int analyzed;
} rfa_relative_transfer;

void rfa_exp_sweep_reset_q31(rfa_exp_sweep_q31 *state,
                             rfa_u32 start_step_q32,
                             rfa_u32 ratio_q31,
                             rfa_i32 gain_q15,
                             int frames);
int rfa_exp_sweep_render_q15(rfa_exp_sweep_q31 *state,
                             rfa_i16 *output, int frames, int channels);
int rfa_sync_sequence_q15(rfa_i16 *output, int count,
                          rfa_u32 seed, rfa_i32 gain_q15);
int rfa_relative_transfer_search(const rfa_i16 *reference, int reference_count,
                                 const rfa_i16 *response, int response_count,
                                 int min_lag, int max_lag,
                                 rfa_relative_transfer *result);

#ifdef __cplusplus
}
#endif

#endif
