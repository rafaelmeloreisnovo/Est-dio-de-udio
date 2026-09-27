#ifndef RAFAELIA_DSP_CORE_H
#define RAFAELIA_DSP_CORE_H

#ifdef __cplusplus
extern "C" {
#endif

enum {
    DSP_PRESET_WHATSAPP_VOICE = 0,
    DSP_PRESET_NATURAL_VOICE = 1,
    DSP_PRESET_MUSIC_CLEAN = 2
};

void dsp_reset(int preset);
void dsp_process(signed short *samples, int count, int channels);

#ifdef __cplusplus
}
#endif

#endif
