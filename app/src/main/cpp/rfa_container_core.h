/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#ifndef RFA_CONTAINER_CORE_H
#define RFA_CONTAINER_CORE_H

#include "rfa_core_types.h"

#ifdef __cplusplus
extern "C" {
#endif

enum {
    RFA_CONTAINER_ZRF = 1,
    RFA_CONTAINER_CFR = 2,
    RFA_CONTAINER_HEADER_BYTES = 40,
    RFA_CONTAINER_CHUNK_HEADER_BYTES = 16
};

enum {
    RFA_CHUNK_PCM = 0x204d4350U,
    RFA_CHUNK_WAVE = 0x45564157U,
    RFA_CHUNK_MATRIX = 0x5254414dU,
    RFA_CHUNK_CAL = 0x204c4143U,
    RFA_CHUNK_IR = 0x20205249U,
    RFA_CHUNK_SPEC = 0x43455053U,
    RFA_CHUNK_PHON = 0x4e4f4850U,
    RFA_CHUNK_ROOM = 0x4d4f4f52U,
    RFA_CHUNK_RECEIPT = 0x54504352U
};

typedef struct {
    int kind;
    int version;
    int channels;
    int matrix_dim;
    rfa_u32 sample_rate;
    rfa_u32 wave_count;
    rfa_u32 flags;
    rfa_u32 chunk_count;
    rfa_u32 payload_bytes;
    rfa_u32 block_samples;
} rfa_container_desc;

rfa_u32 rfa_fnv1a32(const rfa_u8 *data, int count);
int rfa_container_write_header(rfa_u8 *target, int capacity,
                               const rfa_container_desc *desc);
int rfa_container_read_header(const rfa_u8 *source, int length,
                              rfa_container_desc *desc);
int rfa_container_write_chunk_header(rfa_u8 *target, int capacity,
                                     rfa_u32 type, rfa_u32 flags,
                                     rfa_u32 payload_bytes, rfa_u32 item_count);
int rfa_container_read_chunk_header(const rfa_u8 *source, int length,
                                    rfa_u32 *type, rfa_u32 *flags,
                                    rfa_u32 *payload_bytes, rfa_u32 *item_count);

#ifdef __cplusplus
}
#endif

#endif
