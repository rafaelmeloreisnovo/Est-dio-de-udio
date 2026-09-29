/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */
#ifndef RFA_SINE_Q15_H
#define RFA_SINE_Q15_H

#include "rfa_core_types.h"

static const rfa_i16 rfa_sine_quarter_q15[65] = {
    0,804,1608,2410,3212,4011,4808,5602,6393,7179,7962,8739,9512,
    10278,11039,11793,12539,13279,14010,14732,15446,16151,16846,
    17530,18204,18868,19519,20159,20787,21403,22005,22594,23170,
    23731,24279,24811,25329,25832,26319,26790,27245,27683,28105,
    28510,28898,29268,29621,29956,30273,30571,30852,31113,31356,
    31580,31785,31971,32137,32285,32412,32521,32609,32678,32728,
    32757,32767
};

static inline rfa_i16 rfa_sine_lookup_q15(rfa_u32 phase) {
    rfa_u32 quadrant = phase >> 30;
    rfa_u32 offset = (phase >> 24) & 63U;
    rfa_u32 index;
    rfa_i16 value;

    if (quadrant == 0U) {
        index = offset;
        value = rfa_sine_quarter_q15[index];
    } else if (quadrant == 1U) {
        index = 64U - offset;
        value = rfa_sine_quarter_q15[index];
    } else if (quadrant == 2U) {
        index = offset;
        value = (rfa_i16)-rfa_sine_quarter_q15[index];
    } else {
        index = 64U - offset;
        value = (rfa_i16)-rfa_sine_quarter_q15[index];
    }
    return value;
}

#endif
