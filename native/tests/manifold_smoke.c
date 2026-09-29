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
#include "rfa_fir_core.h"
#include "rfa_time_core.h"
#include "rfa_lms_core.h"
#include "rfa_biquad_core.h"
#include "rfa_voice_core.h"
#include "rfa_measure_core.h"

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
    rfa_i16 fir_coeff[1] = {32767};
    rfa_i16 fir_history[1];
    rfa_i16 fir_out[16];
    rfa_fir_q15 fir;
    rfa_time_map_q16 time_map;
    rfa_i16 time_in[8] = {0,1000,2000,3000,4000,5000,6000,7000};
    rfa_i16 time_out[8];
    rfa_i16 lms_weights[4];
    rfa_i16 lms_history[4];
    rfa_i16 lms_primary[8] = {100,200,300,400,500,600,700,800};
    rfa_i16 lms_reference[8] = {20,20,20,20,20,20,20,20};
    rfa_i16 lms_out[8];
    rfa_lms_q15 lms;
    rfa_biquad_bank_q30 bq;
    rfa_biquad_coeff_q30 identity_bq;
    rfa_i16 bq_data[8] = {-1000,-500,0,500,1000,500,0,-500};
    rfa_i16 voice_data[32];
    rfa_voice_features voice_features;
    rfa_exp_sweep_q31 sweep;
    rfa_i16 sweep_data[64];
    rfa_i16 delayed_data[72];
    rfa_relative_transfer transfer;
    rfa_i16 sync_a[32];
    rfa_i16 sync_b[32];
    rfa_i16 sync_inverted[40];
    rfa_relative_transfer inverted_transfer;
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

    if (!rfa_fir_bind_q15(&fir, fir_coeff, fir_history, 1)) return 50;
    rfa_fir_process_block_q15(&fir, time_in, fir_out, 8);
    for (i = 0; i < 8; ++i) {
        int delta = fir_out[i] - time_in[i];
        if (delta < 0) delta = -delta;
        if (delta > 1) return 51;
    }

    rfa_time_map_reset_q16(&time_map, 65536U);
    if (rfa_time_resample_linear_q16(&time_map, time_in, 8, 1, time_out, 7) != 7) return 52;
    for (i = 0; i < 7; ++i) if (time_out[i] != time_in[i]) return 53;

    if (!rfa_lms_bind_q15(&lms, lms_weights, lms_history, 4, 512)) return 54;
    rfa_lms_cancel_block_q15(&lms, lms_primary, lms_reference, lms_out, 8);
    if (lms_out[0] != lms_primary[0]) return 55;

    rfa_biquad_bank_reset_q30(&bq, 1);
    identity_bq.b0_q30 = (rfa_i32)(1U << 30);
    identity_bq.b1_q30 = 0;
    identity_bq.b2_q30 = 0;
    identity_bq.a1_q30 = 0;
    identity_bq.a2_q30 = 0;
    if (!rfa_biquad_bank_set_q30(&bq, 0, &identity_bq)) return 56;
    rfa_biquad_bank_process_q30(&bq, bq_data, 8, 1);
    if (bq_data[0] != -1000 || bq_data[4] != 1000) return 57;

    for (i = 0; i < 32; ++i) {
        int phase = i & 3;
        voice_data[i] = phase == 0 ? 1000 : (phase == 2 ? -1000 : 0);
    }
    if (!rfa_voice_analyze_mono(voice_data, 32, 2, 8, &voice_features)) return 58;
    if (voice_features.best_period_samples != 4) return 59;
    if (voice_features.energy == 0ULL || voice_features.zero_crossings == 0U) return 60;

    if (rfa_sync_sequence_q15(sync_a, 32, 0x51f15e5dU, 8192) != 32) return 61;
    if (rfa_sync_sequence_q15(sync_b, 32, 0x51f15e5dU, 8192) != 32) return 62;
    for (i = 0; i < 32; ++i) {
        if (sync_a[i] != sync_b[i]) return 63;
        if (sync_a[i] != 8192 && sync_a[i] != -8192) return 64;
    }
    for (i = 0; i < 40; ++i) sync_inverted[i] = 0;
    for (i = 0; i < 32; ++i) sync_inverted[i + 3] = (rfa_i16)-sync_a[i];
    if (!rfa_relative_transfer_search(
            sync_a, 32, sync_inverted, 40, 0, 6, &inverted_transfer)) return 71;
    if (inverted_transfer.best_lag != 3) return 69;
    if (inverted_transfer.correlation >= 0LL) return 70;

    rfa_exp_sweep_reset_q31(&sweep, 89478485U, 2149631132U, 16384, 64);
    if (rfa_exp_sweep_render_q15(&sweep, sweep_data, 64, 1) != 64) return 65;
    for (i = 0; i < 72; ++i) delayed_data[i] = 0;
    for (i = 0; i < 64; ++i) delayed_data[i + 5] = sweep_data[i];
    if (!rfa_relative_transfer_search(
            sweep_data, 64, delayed_data, 72, 0, 8, &transfer)) return 66;
    if (transfer.best_lag != 5) return 67;
    if (transfer.reference_energy == 0ULL || transfer.response_energy == 0ULL) return 68;

    return 0;
}
