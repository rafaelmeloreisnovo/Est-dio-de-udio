/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#include "rfa_wave_core.h"
#include "rfa_sine_q15.h"

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
    return rfa_sine_lookup_q15(phase);
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
