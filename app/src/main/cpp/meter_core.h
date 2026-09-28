#ifndef RAFAELIA_METER_CORE_H
#define RAFAELIA_METER_CORE_H

#include "rfa_core_types.h"

#ifdef __cplusplus
extern "C" {
#endif

enum {
    METER_SPECTRUM_BANDS = 16,
    METER_WINDOW_SLOTS = 4,
    METER_TRUE_PEAK_TAPS = 12
};

typedef struct {
    rfa_i32 x1;
    rfa_i32 x2;
    rfa_i32 y1;
    rfa_i32 y2;
} meter_biquad_state;

typedef struct {
    meter_biquad_state k1[2];
    meter_biquad_state k2[2];
    rfa_i32 tp_hist[2][METER_TRUE_PEAK_TAPS];
    rfa_u64 window_energy;
    rfa_u64 windows[METER_WINDOW_SLOTS];
    int window_pos;
    int windows_seen;
    int window_frames;
    rfa_u64 gate_block;
    rfa_u64 block_sum;
    rfa_u64 block_count;
    rfa_u64 true_peak_q16;
    rfa_u64 sample_peak_q16;
    int channels;
} meter_state;

void meter_state_reset(meter_state *state, int channels, rfa_u64 gate_block_q36);
void meter_state_push(meter_state *state, const rfa_i16 *samples, int count, int channels);
void meter_state_result(const meter_state *state, rfa_u64 out_values[4]);
rfa_u64 meter_state_relative_gate_block(const meter_state *state);
rfa_u64 meter_state_gain_q30(const meter_state *state,
                             rfa_u64 target_energy_q36,
                             rfa_u64 true_peak_ceiling_q16);
void meter_spectrum16(const rfa_i16 *samples, int count, int channels,
                      rfa_u64 out_bands[METER_SPECTRUM_BANDS]);

#ifdef __cplusplus
}
#endif

#endif
