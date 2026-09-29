/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#ifndef RFA_BLOCK_CORE_H
#define RFA_BLOCK_CORE_H

#include "rfa_core_types.h"

#ifdef __cplusplus
extern "C" {
#endif

enum {
    RFA_BLOCK_128 = 128,
    RFA_BLOCK_512 = 512,
    RFA_BLOCK_4096 = 4096
};

typedef struct {
    rfa_i16 *storage;
    rfa_u32 capacity;
    rfa_u32 mask;
    rfa_u32 read_index;
    rfa_u32 write_index;
} rfa_ring_i16;

int rfa_block_samples(int profile);
int rfa_ring_bind(rfa_ring_i16 *ring, rfa_i16 *storage, rfa_u32 capacity);
rfa_u32 rfa_ring_available(const rfa_ring_i16 *ring);
rfa_u32 rfa_ring_space(const rfa_ring_i16 *ring);
rfa_u32 rfa_ring_write(rfa_ring_i16 *ring, const rfa_i16 *source, rfa_u32 count);
rfa_u32 rfa_ring_read(rfa_ring_i16 *ring, rfa_i16 *target, rfa_u32 count);

#ifdef __cplusplus
}
#endif

#endif
