#ifndef RAFAELIA_AUDIO_FORMAT_CORE_H
#define RAFAELIA_AUDIO_FORMAT_CORE_H

/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

#include "rfa_core_types.h"

enum {
    RFA_AUDIO_FORMAT_RAW_PCM16 = 0,
    RFA_AUDIO_FORMAT_WAV_PCM16 = 1,
    RFA_AUDIO_FORMAT_AIFF_PCM16 = 2,
    RFA_AUDIO_FORMAT_AU_PCM16 = 3,
    RFA_AUDIO_FORMAT_CAF_PCM16 = 4
};

enum {
    RFA_AUDIO_FORMAT_OK = 1,
    RFA_AUDIO_FORMAT_FAIL = 0,
    RFA_AUDIO_FORMAT_INVALID_SIZE = 0xffffffffu
};

rfa_u32 rfa_audio_format_header_size(rfa_u32 format);

rfa_u32 rfa_audio_format_write_header(
    rfa_u32 format,
    rfa_u32 sample_rate,
    rfa_u32 channels,
    rfa_u64 data_bytes,
    rfa_u8 *out,
    rfa_u32 out_capacity,
    rfa_u32 *written);

rfa_u32 rfa_pcm16_swap_pairs(rfa_u8 *bytes, rfa_u32 byte_count);

#endif
