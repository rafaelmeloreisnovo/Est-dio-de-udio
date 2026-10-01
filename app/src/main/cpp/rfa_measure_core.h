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

enum {
    RFA_SWEEP_PROFILE_BANDS = 16,
    RFA_DECAY_FLAG_EDT = 1,
    RFA_DECAY_FLAG_T20 = 2,
    RFA_DECAY_FLAG_T30 = 4
};

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

typedef struct {
    rfa_u64 reference_energy[RFA_SWEEP_PROFILE_BANDS];
    rfa_u64 response_energy[RFA_SWEEP_PROFILE_BANDS];
    rfa_u32 power_ratio_q20[RFA_SWEEP_PROFILE_BANDS];
    int valid_bands;
    int analyzed_frames;
} rfa_sweep_band_profile;

typedef struct {
    rfa_u64 raw_energy;
    rfa_u64 noise_energy_per_sample;
    rfa_u64 corrected_initial_energy;
    int analyzed_frames;
    int noise_tail_frames;
    int t5_frames;
    int t10_frames;
    int t25_frames;
    int t35_frames;
    int edt60_frames;
    int t20_rt60_frames;
    int t30_rt60_frames;
    rfa_u32 flags;
} rfa_decay_profile;

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

/*
 * Divide a logarithmic sweep into 16 equal-time bands and compare aligned
 * reference/response energy. This is a bounded relative sweep profile, not a
 * substitute for a calibrated FFT or an impulse-response deconvolution.
 */
int rfa_sweep_band_profile_q20(
        const rfa_i16 *reference, int reference_count, int reference_offset,
        const rfa_i16 *response, int response_count, int response_offset,
        int sweep_frames, rfa_sweep_band_profile *profile);

/*
 * Backward integrated relative decay with a tail-noise estimate. Threshold
 * crossings are returned in frames. T20/T30/EDT are relative decay estimates;
 * ISO 3382 conformance requires separately controlled acquisition/evidence.
 */
int rfa_decay_profile_q30(const rfa_i16 *samples, int count,
                          int noise_tail_frames, rfa_decay_profile *profile);

#ifdef __cplusplus
}
#endif

#endif
