/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#include "rfa_matrix_core.h"

static rfa_i32 rfa_clip_i32(rfa_i64 value) {
    if (value > 2147483647LL) return (rfa_i32)2147483647;
    if (value < (-2147483647LL - 1LL)) return (rfa_i32)(-2147483647 - 1);
    return (rfa_i32)value;
}

void rfa_matrix_zero_q15(rfa_matrix_q15 *matrix, int rows, int cols) {
    int i;
    if (matrix == (rfa_matrix_q15 *)0) return;
    if (rows < 0) rows = 0;
    if (cols < 0) cols = 0;
    if (rows > RFA_MATRIX_MAX_DIM) rows = RFA_MATRIX_MAX_DIM;
    if (cols > RFA_MATRIX_MAX_DIM) cols = RFA_MATRIX_MAX_DIM;
    matrix->rows = rows;
    matrix->cols = cols;
    for (i = 0; i < RFA_MATRIX_MAX_DIM * RFA_MATRIX_MAX_DIM; ++i) {
        matrix->q15[i] = 0;
    }
}

int rfa_matrix_identity_q15(rfa_matrix_q15 *matrix, int dimension) {
    int i;
    if (matrix == (rfa_matrix_q15 *)0) return 0;
    if (dimension <= 0 || dimension > RFA_MATRIX_MAX_DIM) return 0;
    rfa_matrix_zero_q15(matrix, dimension, dimension);
    for (i = 0; i < dimension; ++i) {
        matrix->q15[i * RFA_MATRIX_MAX_DIM + i] = 32767;
    }
    return 1;
}

int rfa_matrix_apply_q15(const rfa_matrix_q15 *matrix,
                         const rfa_i32 *input_q15,
                         rfa_i32 *output_q15,
                         int output_capacity) {
    int row;
    if (matrix == (const rfa_matrix_q15 *)0 ||
        input_q15 == (const rfa_i32 *)0 ||
        output_q15 == (rfa_i32 *)0) return 0;
    if (matrix->rows <= 0 || matrix->cols <= 0) return 0;
    if (matrix->rows > RFA_MATRIX_MAX_DIM || matrix->cols > RFA_MATRIX_MAX_DIM) return 0;
    if (output_capacity < matrix->rows) return 0;

    for (row = 0; row < matrix->rows; ++row) {
        rfa_i64 acc = 0;
        int col;
        for (col = 0; col < matrix->cols; ++col) {
            rfa_i16 m = matrix->q15[row * RFA_MATRIX_MAX_DIM + col];
            acc += ((rfa_i64)m * (rfa_i64)input_q15[col]) >> 15;
        }
        output_q15[row] = rfa_clip_i32(acc);
    }
    return matrix->rows;
}
