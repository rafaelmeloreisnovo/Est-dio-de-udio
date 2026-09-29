/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#include "rfa_block_core.h"

static int rfa_power_of_two(rfa_u32 value) {
    return value >= 2U && (value & (value - 1U)) == 0U;
}

int rfa_block_samples(int profile) {
    if (profile <= 0) return RFA_BLOCK_128;
    if (profile == 1) return RFA_BLOCK_512;
    return RFA_BLOCK_4096;
}

int rfa_ring_bind(rfa_ring_i16 *ring, rfa_i16 *storage, rfa_u32 capacity) {
    if (ring == (rfa_ring_i16 *)0 || storage == (rfa_i16 *)0) return 0;
    if (!rfa_power_of_two(capacity)) return 0;
    ring->storage = storage;
    ring->capacity = capacity;
    ring->mask = capacity - 1U;
    ring->read_index = 0U;
    ring->write_index = 0U;
    return 1;
}

rfa_u32 rfa_ring_available(const rfa_ring_i16 *ring) {
    if (ring == (const rfa_ring_i16 *)0 || ring->storage == (rfa_i16 *)0) return 0U;
    return ring->write_index - ring->read_index;
}

rfa_u32 rfa_ring_space(const rfa_ring_i16 *ring) {
    rfa_u32 available;
    if (ring == (const rfa_ring_i16 *)0 || ring->storage == (rfa_i16 *)0) return 0U;
    available = rfa_ring_available(ring);
    if (available >= ring->capacity) return 0U;
    return ring->capacity - available;
}

rfa_u32 rfa_ring_write(rfa_ring_i16 *ring, const rfa_i16 *source, rfa_u32 count) {
    rfa_u32 written = 0U;
    rfa_u32 space;
    if (ring == (rfa_ring_i16 *)0 || source == (const rfa_i16 *)0) return 0U;
    space = rfa_ring_space(ring);
    if (count > space) count = space;
    while (written < count) {
        ring->storage[ring->write_index & ring->mask] = source[written];
        ++ring->write_index;
        ++written;
    }
    return written;
}

rfa_u32 rfa_ring_read(rfa_ring_i16 *ring, rfa_i16 *target, rfa_u32 count) {
    rfa_u32 read = 0U;
    rfa_u32 available;
    if (ring == (rfa_ring_i16 *)0 || target == (rfa_i16 *)0) return 0U;
    available = rfa_ring_available(ring);
    if (count > available) count = available;
    while (read < count) {
        target[read] = ring->storage[ring->read_index & ring->mask];
        ++ring->read_index;
        ++read;
    }
    return read;
}
