/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 *
 * Freestanding integer helpers. Header-only by design so no cross-object
 * runtime dependency is introduced on ARMv7.
 */
#ifndef RFA_INT_MATH_H
#define RFA_INT_MATH_H

#include "rfa_core_types.h"

static inline rfa_i64 rfa_div_i64_u64_shift(rfa_i64 numerator,
                                             rfa_u64 denominator) {
    rfa_u64 magnitude;
    rfa_u64 quotient = 0ULL;
    rfa_u64 remainder = 0ULL;
    int negative;
    int bit;

    if (denominator == 0ULL || numerator == 0LL) return 0LL;

    negative = numerator < 0LL;
    if (negative) {
        /* Avoid signed overflow for INT64_MIN. */
        magnitude = (rfa_u64)(-(numerator + 1LL)) + 1ULL;
    } else {
        magnitude = (rfa_u64)numerator;
    }

    for (bit = 63; bit >= 0; --bit) {
        remainder = (remainder << 1) | ((magnitude >> bit) & 1ULL);
        if (remainder >= denominator) {
            remainder -= denominator;
            quotient |= 1ULL << bit;
        }
    }

    if (!negative) return (rfa_i64)quotient;
    if (quotient == (1ULL << 63)) {
        return (-9223372036854775807LL - 1LL);
    }
    return -(rfa_i64)quotient;
}

#endif
