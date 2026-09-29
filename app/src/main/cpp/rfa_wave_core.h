/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#ifndef RFA_WAVE_CORE_H
#define RFA_WAVE_CORE_H

#include "rfa_core_types.h"

#ifdef __cplusplus
extern "C" {
#endif

enum {
    RFA_WAVE_MAX_VOICES = 16
};

typedef struct {
    rfa_u32 phase;
    rfa_u32 phase_step;
    rfa_i32 gain_q15;
} rfa_wave_voice;

typedef struct {
    rfa_wave_voice voice[RFA_WAVE_MAX_VOICES];
    int voice_count;
} rfa_wave_bank;

void rfa_wave_bank_reset(rfa_wave_bank *bank);
int rfa_wave_bank_set(rfa_wave_bank *bank, int index,
                      rfa_u32 phase, rfa_u32 phase_step, rfa_i32 gain_q15);
rfa_i16 rfa_wave_sine_q15(rfa_u32 phase);
void rfa_wave_render(rfa_wave_bank *bank, rfa_i16 *out,
                     int frames, int channels);

#ifdef __cplusplus
}
#endif

#endif
