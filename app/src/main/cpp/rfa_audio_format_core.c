/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

#include "rfa_audio_format_core.h"

static void rfa_put_ascii4(rfa_u8 *p, rfa_u8 a, rfa_u8 b, rfa_u8 c, rfa_u8 d) {
    p[0] = a;
    p[1] = b;
    p[2] = c;
    p[3] = d;
}

static void rfa_put_u16le(rfa_u8 *p, rfa_u32 v) {
    p[0] = (rfa_u8)(v & 255u);
    p[1] = (rfa_u8)((v >> 8) & 255u);
}

static void rfa_put_u16be(rfa_u8 *p, rfa_u32 v) {
    p[0] = (rfa_u8)((v >> 8) & 255u);
    p[1] = (rfa_u8)(v & 255u);
}

static void rfa_put_u32le(rfa_u8 *p, rfa_u32 v) {
    p[0] = (rfa_u8)(v & 255u);
    p[1] = (rfa_u8)((v >> 8) & 255u);
    p[2] = (rfa_u8)((v >> 16) & 255u);
    p[3] = (rfa_u8)((v >> 24) & 255u);
}

static void rfa_put_u32be(rfa_u8 *p, rfa_u32 v) {
    p[0] = (rfa_u8)((v >> 24) & 255u);
    p[1] = (rfa_u8)((v >> 16) & 255u);
    p[2] = (rfa_u8)((v >> 8) & 255u);
    p[3] = (rfa_u8)(v & 255u);
}

static void rfa_put_u64be(rfa_u8 *p, rfa_u64 v) {
    p[0] = (rfa_u8)((v >> 56) & 255u);
    p[1] = (rfa_u8)((v >> 48) & 255u);
    p[2] = (rfa_u8)((v >> 40) & 255u);
    p[3] = (rfa_u8)((v >> 32) & 255u);
    p[4] = (rfa_u8)((v >> 24) & 255u);
    p[5] = (rfa_u8)((v >> 16) & 255u);
    p[6] = (rfa_u8)((v >> 8) & 255u);
    p[7] = (rfa_u8)(v & 255u);
}

static rfa_u32 rfa_floor_log2_u32(rfa_u32 v) {
    rfa_u32 n = 0u;
    while (v > 1u) {
        v >>= 1;
        n += 1u;
    }
    return n;
}

static void rfa_put_extended80_rate(rfa_u8 *p, rfa_u32 sample_rate) {
    rfa_u32 exponent = rfa_floor_log2_u32(sample_rate);
    rfa_u32 biased = exponent + 16383u;
    rfa_u64 mantissa = ((rfa_u64)sample_rate) << (63u - exponent);
    rfa_put_u16be(p, biased);
    rfa_put_u64be(p + 2, mantissa);
}

static rfa_u64 rfa_double_bits_from_u32(rfa_u32 value) {
    rfa_u32 exponent = rfa_floor_log2_u32(value);
    rfa_u64 biased = (rfa_u64)(exponent + 1023u);
    rfa_u64 base = ((rfa_u64)1u) << exponent;
    rfa_u64 remainder = (rfa_u64)value - base;
    rfa_u64 fraction = remainder << (52u - exponent);
    return (biased << 52) | (fraction & 0x000fffffffffffffull);
}

rfa_u32 rfa_audio_format_header_size(rfa_u32 format) {
    switch (format) {
        case RFA_AUDIO_FORMAT_RAW_PCM16: return 0u;
        case RFA_AUDIO_FORMAT_WAV_PCM16: return 44u;
        case RFA_AUDIO_FORMAT_AIFF_PCM16: return 54u;
        case RFA_AUDIO_FORMAT_AU_PCM16: return 28u;
        case RFA_AUDIO_FORMAT_CAF_PCM16: return 68u;
        default: return RFA_AUDIO_FORMAT_INVALID_SIZE;
    }
}

rfa_u32 rfa_audio_format_write_header(
    rfa_u32 format,
    rfa_u32 sample_rate,
    rfa_u32 channels,
    rfa_u64 data_bytes,
    rfa_u8 *out,
    rfa_u32 out_capacity,
    rfa_u32 *written) {
    rfa_u32 header_size = rfa_audio_format_header_size(format);
    rfa_u64 frame_bytes = (rfa_u64)channels * 2u;

    if (written == (rfa_u32 *)0) return RFA_AUDIO_FORMAT_FAIL;
    *written = 0u;
    if (header_size == RFA_AUDIO_FORMAT_INVALID_SIZE) return RFA_AUDIO_FORMAT_FAIL;
    if (sample_rate == 0u || channels == 0u || channels > 2u) return RFA_AUDIO_FORMAT_FAIL;
    if (frame_bytes == 0u || (data_bytes % frame_bytes) != 0u) return RFA_AUDIO_FORMAT_FAIL;
    if (header_size == 0u) return RFA_AUDIO_FORMAT_OK;
    if (out == (rfa_u8 *)0 || out_capacity < header_size) return RFA_AUDIO_FORMAT_FAIL;

    if (format == RFA_AUDIO_FORMAT_WAV_PCM16) {
        rfa_u32 block_align;
        rfa_u32 byte_rate;
        if (data_bytes > 0xffffffffull - 36ull) return RFA_AUDIO_FORMAT_FAIL;
        block_align = channels * 2u;
        if (sample_rate > 0xffffffffu / block_align) return RFA_AUDIO_FORMAT_FAIL;
        byte_rate = sample_rate * block_align;
        rfa_put_ascii4(out + 0, 'R', 'I', 'F', 'F');
        rfa_put_u32le(out + 4, (rfa_u32)(36ull + data_bytes));
        rfa_put_ascii4(out + 8, 'W', 'A', 'V', 'E');
        rfa_put_ascii4(out + 12, 'f', 'm', 't', ' ');
        rfa_put_u32le(out + 16, 16u);
        rfa_put_u16le(out + 20, 1u);
        rfa_put_u16le(out + 22, channels);
        rfa_put_u32le(out + 24, sample_rate);
        rfa_put_u32le(out + 28, byte_rate);
        rfa_put_u16le(out + 32, block_align);
        rfa_put_u16le(out + 34, 16u);
        rfa_put_ascii4(out + 36, 'd', 'a', 't', 'a');
        rfa_put_u32le(out + 40, (rfa_u32)data_bytes);
        *written = 44u;
        return RFA_AUDIO_FORMAT_OK;
    }

    if (format == RFA_AUDIO_FORMAT_AIFF_PCM16) {
        rfa_u64 frames = data_bytes / frame_bytes;
        if (frames > 0xffffffffull || data_bytes > 0xffffffffull - 46ull) {
            return RFA_AUDIO_FORMAT_FAIL;
        }
        rfa_put_ascii4(out + 0, 'F', 'O', 'R', 'M');
        rfa_put_u32be(out + 4, (rfa_u32)(46ull + data_bytes));
        rfa_put_ascii4(out + 8, 'A', 'I', 'F', 'F');
        rfa_put_ascii4(out + 12, 'C', 'O', 'M', 'M');
        rfa_put_u32be(out + 16, 18u);
        rfa_put_u16be(out + 20, channels);
        rfa_put_u32be(out + 22, (rfa_u32)frames);
        rfa_put_u16be(out + 26, 16u);
        rfa_put_extended80_rate(out + 28, sample_rate);
        rfa_put_ascii4(out + 38, 'S', 'S', 'N', 'D');
        rfa_put_u32be(out + 42, (rfa_u32)(8ull + data_bytes));
        rfa_put_u32be(out + 46, 0u);
        rfa_put_u32be(out + 50, 0u);
        *written = 54u;
        return RFA_AUDIO_FORMAT_OK;
    }

    if (format == RFA_AUDIO_FORMAT_AU_PCM16) {
        if (data_bytes > 0xffffffffull) return RFA_AUDIO_FORMAT_FAIL;
        rfa_put_ascii4(out + 0, '.', 's', 'n', 'd');
        rfa_put_u32be(out + 4, 28u);
        rfa_put_u32be(out + 8, (rfa_u32)data_bytes);
        rfa_put_u32be(out + 12, 3u);
        rfa_put_u32be(out + 16, sample_rate);
        rfa_put_u32be(out + 20, channels);
        rfa_put_u32be(out + 24, 0u);
        *written = 28u;
        return RFA_AUDIO_FORMAT_OK;
    }

    if (format == RFA_AUDIO_FORMAT_CAF_PCM16) {
        rfa_u32 bytes_per_packet = channels * 2u;
        rfa_put_ascii4(out + 0, 'c', 'a', 'f', 'f');
        rfa_put_u16be(out + 4, 1u);
        rfa_put_u16be(out + 6, 0u);
        rfa_put_ascii4(out + 8, 'd', 'e', 's', 'c');
        rfa_put_u64be(out + 12, 32u);
        rfa_put_u64be(out + 20, rfa_double_bits_from_u32(sample_rate));
        rfa_put_ascii4(out + 28, 'l', 'p', 'c', 'm');
        rfa_put_u32be(out + 32, 2u);
        rfa_put_u32be(out + 36, bytes_per_packet);
        rfa_put_u32be(out + 40, 1u);
        rfa_put_u32be(out + 44, channels);
        rfa_put_u32be(out + 48, 16u);
        rfa_put_ascii4(out + 52, 'd', 'a', 't', 'a');
        rfa_put_u64be(out + 56, data_bytes + 4u);
        rfa_put_u32be(out + 64, 0u);
        *written = 68u;
        return RFA_AUDIO_FORMAT_OK;
    }

    return RFA_AUDIO_FORMAT_FAIL;
}

rfa_u32 rfa_pcm16_swap_pairs(rfa_u8 *bytes, rfa_u32 byte_count) {
    rfa_u32 i = 0u;
    if (bytes == (rfa_u8 *)0 || (byte_count & 1u) != 0u) return RFA_AUDIO_FORMAT_FAIL;
    while (i < byte_count) {
        rfa_u8 lo = bytes[i];
        bytes[i] = bytes[i + 1u];
        bytes[i + 1u] = lo;
        i += 2u;
    }
    return RFA_AUDIO_FORMAT_OK;
}
