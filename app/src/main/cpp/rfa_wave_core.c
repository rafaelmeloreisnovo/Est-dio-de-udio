/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#include "rfa_wave_core.h"

/* Quarter-wave Q15 table, 0..pi/2 inclusive. No libm/runtime lookup. */
static const rfa_i16 rfa_sin_quarter_q15[65] = {
    0,804,1608,2410,3212,4011,4808,5602,6393,7179,7962,8739,9512,
    10278,11039,11793,12539,13279,14010,14732,15446,16151,16846,
    17530,18204,18868,19519,20159,20787,21403,22005,22594,23170,
    23731,24279,24811,25329,25832,26319,26790,27245,27683,28105,
    28510,28898,29268,29621,29956,30273,30571,30852,31113,31356,
    31580,31785,31971,32137,32285,32412,32521,32609,32678,32728,
    32757,32767
};

static rfa_i16 rfa_clip_i16(rfa_i64 value) {
    if (value > 32767) return (rfa_i16)32767;
    if (value < -32768) return (rfa_i16)-32768;
    return (rfa_i16)value;
}

void rfa_wave_bank_reset(rfa_wave_bank *bank) {
    int i;
    if (bank == (rfa_wave_bank *)0) return;
    bank->voice_count = 0;
    for (i = 0; i < RFA_WAVE_MAX_VOICES; ++i) {
        bank->voice[i].phase = 0U;
        bank->voice[i].phase_step = 0U;
        bank->voice[i].gain_q15 = 0;
    }
}

int rfa_wave_bank_set(rfa_wave_bank *bank, int index,
                      rfa_u32 phase, rfa_u32 phase_step, rfa_i32 gain_q15) {
    if (bank == (rfa_wave_bank *)0) return 0;
    if (index < 0 || index >= RFA_WAVE_MAX_VOICES) return 0;
    if (gain_q15 > 32767) gain_q15 = 32767;
    if (gain_q15 < -32768) gain_q15 = -32768;
    bank->voice[index].phase = phase;
    bank->voice[index].phase_step = phase_step;
    bank->voice[index].gain_q15 = gain_q15;
    if (bank->voice_count <= index) bank->voice_count = index + 1;
    return 1;
}

rfa_i16 rfa_wave_sine_q15(rfa_u32 phase) {
    rfa_u32 quadrant = phase >> 30;
    rfa_u32 offset = (phase >> 24) & 63U;
    rfa_u32 index;
    rfa_i16 value;

    if (quadrant == 0U) {
        index = offset;
        value = rfa_sin_quarter_q15[index];
    } else if (quadrant == 1U) {
        index = 64U - offset;
        value = rfa_sin_quarter_q15[index];
    } else if (quadrant == 2U) {
        index = offset;
        value = (rfa_i16)-rfa_sin_quarter_q15[index];
    } else {
        index = 64U - offset;
        value = (rfa_i16)-rfa_sin_quarter_q15[index];
    }
    return value;
}

void rfa_wave_render(rfa_wave_bank *bank, rfa_i16 *out,
                     int frames, int channels) {
    int frame;
    if (bank == (rfa_wave_bank *)0 || out == (rfa_i16 *)0) return;
    if (frames <= 0 || channels <= 0 || channels > 2) return;

    for (frame = 0; frame < frames; ++frame) {
        rfa_i64 mixed = 0;
        int voice;
        int channel;
        for (voice = 0; voice < bank->voice_count; ++voice) {
            rfa_wave_voice *v = &bank->voice[voice];
            rfa_i32 s = (rfa_i32)rfa_wave_sine_q15(v->phase);
            mixed += ((rfa_i64)s * (rfa_i64)v->gain_q15) >> 15;
            v->phase += v->phase_step;
        }
        for (channel = 0; channel < channels; ++channel) {
            out[frame * channels + channel] = rfa_clip_i16(mixed);
        }
    }
}
