/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#ifndef RFA_RAC1_CORE_H
#define RFA_RAC1_CORE_H

#include "rfa_core_types.h"

#ifdef __cplusplus
extern "C" {
#endif

enum {
    RFA_RAC1_MAX_CHANNELS = 2
};

int rfa_rac1_bound_bytes(int frames, int channels);

int rfa_rac1_encode_i16(
        const rfa_i16 *samples,
        int frames,
        int channels,
        rfa_u8 *output,
        int capacity);

int rfa_rac1_decode_i16(
        const rfa_u8 *encoded,
        int encoded_bytes,
        int frames,
        int channels,
        rfa_i16 *output,
        int output_samples);

#ifdef __cplusplus
}
#endif

#endif
