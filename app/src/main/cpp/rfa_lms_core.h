/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#ifndef RFA_LMS_CORE_H
#define RFA_LMS_CORE_H

#include "rfa_core_types.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    rfa_i16 *weights_q15;
    rfa_i16 *history_q15;
    int taps;
    int position;
    rfa_i32 mu_q15;
} rfa_lms_q15;

typedef struct {
    rfa_i16 *weights_q15;
    rfa_i16 *history_q15;
    int taps;
    int position;
    rfa_i32 mu_q15;
    rfa_u64 epsilon_q30;
} rfa_nlms_q15;

int rfa_lms_bind_q15(rfa_lms_q15 *state, rfa_i16 *weights_q15,
                     rfa_i16 *history_q15, int taps, rfa_i32 mu_q15);
void rfa_lms_reset_q15(rfa_lms_q15 *state);
rfa_i16 rfa_lms_cancel_sample_q15(rfa_lms_q15 *state,
                                  rfa_i16 primary, rfa_i16 reference);
void rfa_lms_cancel_block_q15(rfa_lms_q15 *state,
                              const rfa_i16 *primary,
                              const rfa_i16 *reference,
                              rfa_i16 *output, int count);

/*
 * Normalized LMS variant. The caller supplies a real reference signal.
 * `adapt` is a fail-closed adaptation gate: 0 freezes coefficients while
 * continuing to estimate/cancel with the current filter; nonzero adapts.
 * A VAD or other policy may drive this gate, but VAD semantics are not
 * claimed by this primitive.
 */
int rfa_nlms_bind_q15(rfa_nlms_q15 *state, rfa_i16 *weights_q15,
                      rfa_i16 *history_q15, int taps, rfa_i32 mu_q15,
                      rfa_u64 epsilon_q30);
void rfa_nlms_reset_q15(rfa_nlms_q15 *state);
rfa_i16 rfa_nlms_cancel_sample_q15(rfa_nlms_q15 *state,
                                   rfa_i16 primary, rfa_i16 reference,
                                   int adapt);
void rfa_nlms_cancel_block_q15(rfa_nlms_q15 *state,
                               const rfa_i16 *primary,
                               const rfa_i16 *reference,
                               rfa_i16 *output, int count, int adapt);

#ifdef __cplusplus
}
#endif

#endif
