#ifndef RAFAELIA_DSP_CORE_H
#define RAFAELIA_DSP_CORE_H

#include "rfa_core_types.h"

#ifdef __cplusplus
extern "C" {
#endif

enum {
    DSP_PRESET_WHATSAPP_VOICE = 0,
    DSP_PRESET_NATURAL_VOICE = 1,
    DSP_PRESET_MUSIC_CLEAN = 2
};

typedef struct {
    rfa_i32 prev_x;
    rfa_i32 hp_y;
    rfa_i32 env;
    rfa_i32 gate_gain;
    rfa_i32 level_gain;
} dsp_channel_state;

typedef struct {
    dsp_channel_state channel[2];
    int preset;
} dsp_state;

void dsp_state_reset(dsp_state *state, int preset);
void dsp_state_process(dsp_state *state, rfa_i16 *samples, int count, int channels);
void dsp_apply_gain_q30(rfa_i16 *samples, int count, rfa_u64 gain_q30);

#ifdef __cplusplus
}
#endif

#endif
