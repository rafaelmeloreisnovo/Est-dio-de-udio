/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#ifndef RFA_VOICE_CORE_H
#define RFA_VOICE_CORE_H

#include "rfa_core_types.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    rfa_u64 energy;
    rfa_u32 zero_crossings;
    rfa_i64 best_correlation;
    rfa_i64 second_correlation;
    int best_period_samples;
    int analyzed_samples;
} rfa_voice_features;

int rfa_voice_analyze_mono(const rfa_i16 *samples, int count,
                           int min_period_samples, int max_period_samples,
                           rfa_voice_features *features);

#ifdef __cplusplus
}
#endif

#endif
