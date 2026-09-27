#ifndef RAFAELIA_METER_CORE_H
#define RAFAELIA_METER_CORE_H

#ifdef __cplusplus
extern "C" {
#endif

typedef unsigned long long rfa_u64;

enum {
    METER_SPECTRUM_BANDS = 16
};

void meter_reset(int channels, rfa_u64 gate_block_q36);
void meter_push(const signed short *samples, int count, int channels);
void meter_result(rfa_u64 out_values[4]);
rfa_u64 meter_relative_gate_block(void);
rfa_u64 meter_gain_q30(rfa_u64 target_energy_q36,
                       rfa_u64 true_peak_ceiling_q16);
void meter_spectrum16(const signed short *samples, int count, int channels,
                      rfa_u64 out_bands[METER_SPECTRUM_BANDS]);

#ifdef __cplusplus
}
#endif

#endif
