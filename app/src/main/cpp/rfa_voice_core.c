/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#include "rfa_voice_core.h"

int rfa_voice_analyze_mono(const rfa_i16 *samples, int count,
                           int min_period_samples, int max_period_samples,
                           rfa_voice_features *features) {
    int i;
    int lag;
    rfa_u64 energy = 0ULL;
    rfa_u32 zero_crossings = 0U;
    rfa_i64 best = 0LL;
    rfa_i64 second = 0LL;
    int best_period = 0;

    if (samples == (const rfa_i16 *)0 || features == (rfa_voice_features *)0) return 0;
    if (count < 4) return 0;
    if (min_period_samples < 1) min_period_samples = 1;
    if (max_period_samples >= count) max_period_samples = count - 1;
    if (max_period_samples < min_period_samples) return 0;

    for (i = 0; i < count; ++i) {
        rfa_i64 s = (rfa_i64)samples[i];
        energy += (rfa_u64)(s * s);
        if (i > 0) {
            int previous_negative = samples[i - 1] < 0;
            int current_negative = samples[i] < 0;
            if (previous_negative != current_negative) ++zero_crossings;
        }
    }

    for (lag = min_period_samples; lag <= max_period_samples; ++lag) {
        rfa_i64 corr = 0LL;
        for (i = lag; i < count; ++i) {
            corr += (rfa_i64)samples[i] * (rfa_i64)samples[i - lag];
        }
        if (corr > best) {
            second = best;
            best = corr;
            best_period = lag;
        } else if (corr > second) {
            second = corr;
        }
    }

    features->energy = energy;
    features->zero_crossings = zero_crossings;
    features->best_correlation = best;
    features->second_correlation = second;
    features->best_period_samples = best_period;
    features->analyzed_samples = count;
    return 1;
}
