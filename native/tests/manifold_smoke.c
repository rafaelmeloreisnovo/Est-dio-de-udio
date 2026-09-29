/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

#include "rfa_wave_core.h"
#include "rfa_matrix_core.h"
#include "rfa_block_core.h"
#include "rfa_container_core.h"

static int same_i16(const rfa_i16 *a, const rfa_i16 *b, int count) {
    int i;
    for (i = 0; i < count; ++i) if (a[i] != b[i]) return 0;
    return 1;
}

int main(void) {
    rfa_wave_bank bank;
    rfa_i16 wave[512];
    rfa_matrix_q15 matrix;
    rfa_i32 input[7] = {32767, 16384, -8192, 4096, 0, 1024, -1024};
    rfa_i32 output[7];
    rfa_i16 ring_storage[512];
    rfa_i16 ring_out[128];
    rfa_ring_i16 ring;
    rfa_container_desc desc;
    rfa_container_desc decoded;
    rfa_u8 header_bytes[RFA_CONTAINER_HEADER_BYTES];
    rfa_u8 chunk_bytes[RFA_CONTAINER_CHUNK_HEADER_BYTES];
    rfa_u32 chunk_type;
    rfa_u32 chunk_flags;
    rfa_u32 chunk_payload;
    rfa_u32 chunk_items;
    int i;

    rfa_wave_bank_reset(&bank);
    if (!rfa_wave_bank_set(&bank, 0, 0U, 89478485U, 16384)) return 10;
    if (!rfa_wave_bank_set(&bank, 1, 0U, 178956971U, 8192)) return 11;
    rfa_wave_render(&bank, wave, 512, 1);
    {
        int nonzero = 0;
        for (i = 0; i < 512; ++i) nonzero |= (wave[i] != 0);
        if (!nonzero) return 12;
    }

    if (!rfa_matrix_identity_q15(&matrix, 7)) return 20;
    if (rfa_matrix_apply_q15(&matrix, input, output, 7) != 7) return 21;
    for (i = 0; i < 7; ++i) {
        rfa_i32 delta = output[i] - input[i];
        if (delta < 0) delta = -delta;
        if (delta > 2) return 22;
    }

    if (rfa_block_samples(0) != 128) return 30;
    if (rfa_block_samples(1) != 512) return 31;
    if (rfa_block_samples(2) != 4096) return 32;
    if (!rfa_ring_bind(&ring, ring_storage, 512U)) return 33;
    if (rfa_ring_write(&ring, wave, 128U) != 128U) return 34;
    if (rfa_ring_read(&ring, ring_out, 128U) != 128U) return 35;
    if (!same_i16(wave, ring_out, 128)) return 36;

    desc.kind = RFA_CONTAINER_ZRF;
    desc.version = 1;
    desc.channels = 2;
    desc.matrix_dim = 14;
    desc.sample_rate = 48000U;
    desc.wave_count = 2U;
    desc.flags = 0U;
    desc.chunk_count = 4U;
    desc.payload_bytes = 4096U;
    desc.block_samples = 512U;

    if (rfa_container_write_header(header_bytes,
            RFA_CONTAINER_HEADER_BYTES, &desc) != RFA_CONTAINER_HEADER_BYTES) return 40;
    if (!rfa_container_read_header(header_bytes,
            RFA_CONTAINER_HEADER_BYTES, &decoded)) return 41;
    if (decoded.kind != RFA_CONTAINER_ZRF) return 42;
    if (decoded.channels != 2 || decoded.matrix_dim != 14) return 43;
    if (decoded.sample_rate != 48000U || decoded.block_samples != 512U) return 44;

    if (rfa_container_write_chunk_header(chunk_bytes,
            RFA_CONTAINER_CHUNK_HEADER_BYTES, RFA_CHUNK_PCM,
            0U, 2048U, 1024U) != RFA_CONTAINER_CHUNK_HEADER_BYTES) return 45;
    if (!rfa_container_read_chunk_header(chunk_bytes,
            RFA_CONTAINER_CHUNK_HEADER_BYTES, &chunk_type, &chunk_flags,
            &chunk_payload, &chunk_items)) return 46;
    if (chunk_type != RFA_CHUNK_PCM || chunk_flags != 0U ||
        chunk_payload != 2048U || chunk_items != 1024U) return 47;

    header_bytes[8] ^= 1U;
    if (rfa_container_read_header(header_bytes,
            RFA_CONTAINER_HEADER_BYTES, &decoded)) return 48;

    return 0;
}
