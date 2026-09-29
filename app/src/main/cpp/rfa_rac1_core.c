/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#include "rfa_rac1_core.h"

static rfa_u32 rfa_rac1_zigzag(rfa_i32 value) {
    rfa_u32 magnitude;
    if (value < 0) {
        magnitude = (rfa_u32)(-(value + 1)) + 1U;
        return (magnitude << 1) - 1U;
    }
    return ((rfa_u32)value) << 1;
}

static rfa_i32 rfa_rac1_unzigzag(rfa_u32 value) {
    rfa_u32 magnitude = value >> 1;
    if ((value & 1U) != 0U) return -(rfa_i32)(magnitude + 1U);
    return (rfa_i32)magnitude;
}

static int rfa_rac1_put_varint(rfa_u32 value, rfa_u8 *out, int capacity) {
    int used = 0;
    do {
        rfa_u8 byte = (rfa_u8)(value & 0x7fU);
        value >>= 7;
        if (value != 0U) byte = (rfa_u8)(byte | 0x80U);
        if (used >= capacity) return 0;
        out[used++] = byte;
    } while (value != 0U);
    return used;
}

static int rfa_rac1_get_varint(
        const rfa_u8 *in,
        int available,
        rfa_u32 *value_out) {
    rfa_u32 value = 0U;
    int shift = 0;
    int used = 0;

    if (in == (const rfa_u8 *)0 || value_out == (rfa_u32 *)0) return 0;

    while (used < available && used < 3) {
        rfa_u8 byte = in[used++];
        value |= ((rfa_u32)(byte & 0x7fU)) << shift;
        if ((byte & 0x80U) == 0U) {
            *value_out = value;
            return used;
        }
        shift += 7;
    }
    return 0;
}

int rfa_rac1_bound_bytes(int frames, int channels) {
    if (frames < 0 || channels <= 0 || channels > RFA_RAC1_MAX_CHANNELS) return 0;
    if (frames > 715827882 / channels) return 0;
    return frames * channels * 3;
}

int rfa_rac1_encode_i16(
        const rfa_i16 *samples,
        int frames,
        int channels,
        rfa_u8 *output,
        int capacity) {
    rfa_i32 previous[RFA_RAC1_MAX_CHANNELS] = {0, 0};
    int frame;
    int position = 0;

    if (samples == (const rfa_i16 *)0 || output == (rfa_u8 *)0) return 0;
    if (frames < 0 || channels <= 0 || channels > RFA_RAC1_MAX_CHANNELS) return 0;

    for (frame = 0; frame < frames; ++frame) {
        int channel;
        for (channel = 0; channel < channels; ++channel) {
            int index = frame * channels + channel;
            rfa_i32 current = (rfa_i32)samples[index];
            rfa_i32 delta = current - previous[channel];
            rfa_u32 code = rfa_rac1_zigzag(delta);
            int used = rfa_rac1_put_varint(
                    code, output + position, capacity - position);
            if (used <= 0) return 0;
            position += used;
            previous[channel] = current;
        }
    }
    return position;
}

int rfa_rac1_decode_i16(
        const rfa_u8 *encoded,
        int encoded_bytes,
        int frames,
        int channels,
        rfa_i16 *output,
        int output_samples) {
    rfa_i32 previous[RFA_RAC1_MAX_CHANNELS] = {0, 0};
    int frame;
    int position = 0;
    int required;

    if (encoded == (const rfa_u8 *)0 || output == (rfa_i16 *)0) return 0;
    if (frames < 0 || channels <= 0 || channels > RFA_RAC1_MAX_CHANNELS) return 0;
    if (frames > 1073741823 / channels) return 0;
    required = frames * channels;
    if (output_samples < required) return 0;

    for (frame = 0; frame < frames; ++frame) {
        int channel;
        for (channel = 0; channel < channels; ++channel) {
            rfa_u32 code;
            rfa_i32 delta;
            rfa_i32 current;
            int used;

            if (position >= encoded_bytes) return 0;
            used = rfa_rac1_get_varint(
                    encoded + position, encoded_bytes - position, &code);
            if (used <= 0) return 0;
            position += used;

            delta = rfa_rac1_unzigzag(code);
            current = previous[channel] + delta;
            if (current < -32768 || current > 32767) return 0;
            output[frame * channels + channel] = (rfa_i16)current;
            previous[channel] = current;
        }
    }

    if (position != encoded_bytes) return 0;
    return required;
}
