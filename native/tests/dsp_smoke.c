#include "dsp_core.h"

static int same16(const rfa_i16 *a, const rfa_i16 *b, int n) {
    for (int i = 0; i < n; i++) {
        if (a[i] != b[i]) return 0;
    }
    return 1;
}

int main(void) {
    dsp_state state_a;
    dsp_state state_b;

    rfa_i16 silence[32] = {0};
    dsp_state_reset(&state_a, DSP_PRESET_WHATSAPP_VOICE);
    dsp_state_process(&state_a, silence, 32, 1);
    for (int i = 0; i < 32; i++) {
        if (silence[i] != 0) return 10;
    }

    rfa_i16 a[16] = {
        0, 1000, -1000, 3000, -3000, 10000, -10000, 30000,
        -30000, 32767, -32768, 12000, -12000, 2000, -2000, 0
    };
    rfa_i16 b[16];
    for (int i = 0; i < 16; i++) b[i] = a[i];

    dsp_state_reset(&state_a, DSP_PRESET_WHATSAPP_VOICE);
    dsp_state_reset(&state_b, DSP_PRESET_WHATSAPP_VOICE);
    dsp_state_process(&state_a, a, 16, 1);
    dsp_state_process(&state_b, b, 16, 1);

    if (!same16(a, b, 16)) return 20;

    /* Independent contexts must not leak mutable state into each other. */
    rfa_i16 probe_a[4] = {1000, 1000, 1000, 1000};
    rfa_i16 probe_b[4] = {1000, 1000, 1000, 1000};
    dsp_state_reset(&state_a, DSP_PRESET_WHATSAPP_VOICE);
    dsp_state_reset(&state_b, DSP_PRESET_WHATSAPP_VOICE);
    dsp_state_process(&state_a, probe_a, 4, 1);
    dsp_state_process(&state_b, probe_b, 4, 1);
    if (!same16(probe_a, probe_b, 4)) return 30;

    return 0;
}
