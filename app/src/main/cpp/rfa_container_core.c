/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#include "rfa_container_core.h"

static void rfa_put_u16le(rfa_u8 *p, rfa_u32 value) {
    p[0] = (rfa_u8)(value & 0xffU);
    p[1] = (rfa_u8)((value >> 8) & 0xffU);
}

static void rfa_put_u32le(rfa_u8 *p, rfa_u32 value) {
    p[0] = (rfa_u8)(value & 0xffU);
    p[1] = (rfa_u8)((value >> 8) & 0xffU);
    p[2] = (rfa_u8)((value >> 16) & 0xffU);
    p[3] = (rfa_u8)((value >> 24) & 0xffU);
}

static rfa_u32 rfa_get_u16le(const rfa_u8 *p) {
    return (rfa_u32)p[0] | ((rfa_u32)p[1] << 8);
}

static rfa_u32 rfa_get_u32le(const rfa_u8 *p) {
    return (rfa_u32)p[0] |
           ((rfa_u32)p[1] << 8) |
           ((rfa_u32)p[2] << 16) |
           ((rfa_u32)p[3] << 24);
}

rfa_u32 rfa_fnv1a32(const rfa_u8 *data, int count) {
    rfa_u32 hash = 2166136261U;
    int i;
    if (data == (const rfa_u8 *)0 || count <= 0) return hash;
    for (i = 0; i < count; ++i) {
        hash ^= (rfa_u32)data[i];
        hash *= 16777619U;
    }
    return hash;
}

int rfa_container_write_header(rfa_u8 *target, int capacity,
                               const rfa_container_desc *desc) {
    int i;
    rfa_u32 checksum;
    if (target == (rfa_u8 *)0 || desc == (const rfa_container_desc *)0) return 0;
    if (capacity < RFA_CONTAINER_HEADER_BYTES) return 0;
    if (desc->kind != RFA_CONTAINER_ZRF && desc->kind != RFA_CONTAINER_CFR) return 0;
    if (desc->channels <= 0 || desc->channels > 8) return 0;
    if (desc->matrix_dim < 0 || desc->matrix_dim > 16) return 0;

    for (i = 0; i < RFA_CONTAINER_HEADER_BYTES; ++i) target[i] = 0U;
    target[0] = desc->kind == RFA_CONTAINER_ZRF ? (rfa_u8)'Z' : (rfa_u8)'C';
    target[1] = (rfa_u8)(desc->kind == RFA_CONTAINER_ZRF ? 'R' : 'F');
    target[2] = (rfa_u8)(desc->kind == RFA_CONTAINER_ZRF ? 'F' : 'R');
    target[3] = (rfa_u8)'1';
    target[4] = (rfa_u8)desc->version;
    target[5] = (rfa_u8)desc->kind;
    target[6] = (rfa_u8)desc->channels;
    target[7] = (rfa_u8)desc->matrix_dim;
    rfa_put_u32le(target + 8, desc->sample_rate);
    rfa_put_u16le(target + 12, desc->wave_count);
    rfa_put_u16le(target + 14, desc->flags);
    rfa_put_u32le(target + 16, desc->chunk_count);
    rfa_put_u32le(target + 20, desc->payload_bytes);
    rfa_put_u32le(target + 24, desc->block_samples);
    rfa_put_u32le(target + 28, RFA_CONTAINER_HEADER_BYTES);
    checksum = rfa_fnv1a32(target, 32);
    rfa_put_u32le(target + 32, checksum);
    rfa_put_u32le(target + 36, 0U);
    return RFA_CONTAINER_HEADER_BYTES;
}

int rfa_container_read_header(const rfa_u8 *source, int length,
                              rfa_container_desc *desc) {
    int kind;
    rfa_u32 checksum;
    if (source == (const rfa_u8 *)0 || desc == (rfa_container_desc *)0) return 0;
    if (length < RFA_CONTAINER_HEADER_BYTES) return 0;

    if (source[0] == (rfa_u8)'Z' && source[1] == (rfa_u8)'R' &&
        source[2] == (rfa_u8)'F' && source[3] == (rfa_u8)'1') {
        kind = RFA_CONTAINER_ZRF;
    } else if (source[0] == (rfa_u8)'C' && source[1] == (rfa_u8)'F' &&
               source[2] == (rfa_u8)'R' && source[3] == (rfa_u8)'1') {
        kind = RFA_CONTAINER_CFR;
    } else {
        return 0;
    }

    if ((int)source[5] != kind) return 0;
    if (rfa_get_u32le(source + 28) != RFA_CONTAINER_HEADER_BYTES) return 0;
    checksum = rfa_fnv1a32(source, 32);
    if (checksum != rfa_get_u32le(source + 32)) return 0;

    desc->kind = kind;
    desc->version = (int)source[4];
    desc->channels = (int)source[6];
    desc->matrix_dim = (int)source[7];
    desc->sample_rate = rfa_get_u32le(source + 8);
    desc->wave_count = rfa_get_u16le(source + 12);
    desc->flags = rfa_get_u16le(source + 14);
    desc->chunk_count = rfa_get_u32le(source + 16);
    desc->payload_bytes = rfa_get_u32le(source + 20);
    desc->block_samples = rfa_get_u32le(source + 24);

    if (desc->channels <= 0 || desc->channels > 8) return 0;
    if (desc->matrix_dim < 0 || desc->matrix_dim > 16) return 0;
    return 1;
}
