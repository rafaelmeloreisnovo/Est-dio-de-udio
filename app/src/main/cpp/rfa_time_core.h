/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#ifndef RFA_TIME_CORE_H
#define RFA_TIME_CORE_H

#include "rfa_core_types.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    rfa_u32 phase_q16;
    rfa_u32 step_q16;
} rfa_time_map_q16;

void rfa_time_map_reset_q16(rfa_time_map_q16 *state, rfa_u32 step_q16);
int rfa_time_resample_linear_q16(rfa_time_map_q16 *state,
                                 const rfa_i16 *input, int input_frames,
                                 int channels, rfa_i16 *output, int output_frames);

#ifdef __cplusplus
}
#endif

#endif
