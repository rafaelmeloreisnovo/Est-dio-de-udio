#include "dsp_core.h"

static int same16(const signed short *a, const signed short *b, int n) {
    for (int i = 0; i < n; i++) {
        if (a[i] != b[i]) return 0;
    }
    return 1;
}

int main(void) {
    signed short silence[32] = {0};
    dsp_reset(DSP_PRESET_WHATSAPP_VOICE);
    dsp_process(silence, 32, 1);
    for (int i = 0; i < 32; i++) {
        if (silence[i] != 0) return 10;
    }

    signed short a[16] = {
        0, 1000, -1000, 3000, -3000, 10000, -10000, 30000,
        -30000, 32767, -32768, 12000, -12000, 2000, -2000, 0
    };
    signed short b[16];
    for (int i = 0; i < 16; i++) b[i] = a[i];

    dsp_reset(DSP_PRESET_WHATSAPP_VOICE);
    dsp_process(a, 16, 1);
    dsp_reset(DSP_PRESET_WHATSAPP_VOICE);
    dsp_process(b, 16, 1);

    if (!same16(a, b, 16)) return 20;
    return 0;
}
