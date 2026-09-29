/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#ifndef RFA_MATRIX_CORE_H
#define RFA_MATRIX_CORE_H

#include "rfa_core_types.h"

#ifdef __cplusplus
extern "C" {
#endif

enum {
    RFA_MATRIX_MAX_DIM = 16
};

typedef struct {
    rfa_i16 q15[RFA_MATRIX_MAX_DIM * RFA_MATRIX_MAX_DIM];
    int rows;
    int cols;
} rfa_matrix_q15;

void rfa_matrix_zero_q15(rfa_matrix_q15 *matrix, int rows, int cols);
int rfa_matrix_identity_q15(rfa_matrix_q15 *matrix, int dimension);
int rfa_matrix_apply_q15(const rfa_matrix_q15 *matrix,
                         const rfa_i32 *input_q15,
                         rfa_i32 *output_q15,
                         int output_capacity);

#ifdef __cplusplus
}
#endif

#endif
