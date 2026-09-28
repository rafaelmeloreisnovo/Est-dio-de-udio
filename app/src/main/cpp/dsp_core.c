#include "dsp_core.h"

#define Q31_ONE 2147483647
#define Q31_LIMIT_M1DB 1913946815

static rfa_i32 clamp_q31(rfa_i64 x) {
    if (x > (rfa_i64)Q31_ONE) return Q31_ONE;
    if (x < (rfa_i64)-2147483647 - 1) return (rfa_i32)(-2147483647 - 1);
    return (rfa_i32)x;
}

static rfa_i32 mul_q31(rfa_i32 a, rfa_i32 b) {
    return (rfa_i32)(((rfa_i64)a * (rfa_i64)b) >> 31);
}

static rfa_i32 abs_q31(rfa_i32 x) {
    if (x == (rfa_i32)(-2147483647 - 1)) return Q31_ONE;
    return x < 0 ? -x : x;
}

static rfa_i32 hp_alpha_for_preset(int preset) {
    if (preset == DSP_PRESET_MUSIC_CLEAN) return 2140467510;
    if (preset == DSP_PRESET_NATURAL_VOICE) return 2130683411;
    return 2127896177;
}

static rfa_i32 gate_threshold_for_preset(int preset) {
    if (preset == DSP_PRESET_MUSIC_CLEAN) return 0;
    if (preset == DSP_PRESET_NATURAL_VOICE) return 2703522;
    return 5394235;
}

static rfa_i32 level_threshold_for_preset(int preset) {
    if (preset == DSP_PRESET_MUSIC_CLEAN) return 1076291388;
    if (preset == DSP_PRESET_NATURAL_VOICE) return 539423503;
    return 428479319;
}

static rfa_i32 level_target(int preset, rfa_i32 env) {
    rfa_i32 t = level_threshold_for_preset(preset);

    if ((rfa_i64)env > (rfa_i64)t * 3) {
        return preset == DSP_PRESET_WHATSAPP_VOICE
                ? 1245540515
                : 1610612735;
    }
    if ((rfa_i64)env > (rfa_i64)t * 2) {
        return preset == DSP_PRESET_WHATSAPP_VOICE
                ? 1546188226
                : 1825361100;
    }
    if (env > t) {
        return preset == DSP_PRESET_WHATSAPP_VOICE
                ? 1889785609
                : 1932735282;
    }
    return Q31_ONE;
}

static rfa_i32 apply_limiter(rfa_i32 x) {
    rfa_i32 a = abs_q31(x);
    if (a <= Q31_LIMIT_M1DB) return x;

    rfa_i32 excess = a - Q31_LIMIT_M1DB;
    rfa_i32 limited = Q31_LIMIT_M1DB + (excess >> 3);
    if (limited > Q31_ONE) limited = Q31_ONE;
    return x < 0 ? -limited : limited;
}

void dsp_state_reset(dsp_state *state, int preset) {
    if (state == (dsp_state *)0) return;
    state->preset = preset;
    for (int c = 0; c < 2; c++) {
        state->channel[c].prev_x = 0;
        state->channel[c].hp_y = 0;
        state->channel[c].env = 0;
        state->channel[c].gate_gain = Q31_ONE;
        state->channel[c].level_gain = Q31_ONE;
    }
}

void dsp_state_process(dsp_state *state, rfa_i16 *samples, int count, int channels) {
    if (state == (dsp_state *)0 || samples == (rfa_i16 *)0 || count <= 0) return;
    if (channels < 1 || channels > 2) return;

    const rfa_i32 alpha = hp_alpha_for_preset(state->preset);
    const rfa_i32 gate_threshold = gate_threshold_for_preset(state->preset);

    for (int i = 0; i < count; i++) {
        int c = channels == 1 ? 0 : (i & 1);
        dsp_channel_state *st = &state->channel[c];

        rfa_i32 x = (rfa_i32)((rfa_i64)samples[i] * 65536);

        rfa_i64 hp_input = (rfa_i64)st->hp_y + (rfa_i64)x - (rfa_i64)st->prev_x;
        rfa_i32 hp = mul_q31(alpha, clamp_q31(hp_input));
        st->prev_x = x;
        st->hp_y = hp;

        rfa_i32 a = abs_q31(hp);
        int env_shift = a > st->env ? 5 : 11;
        st->env += (a - st->env) >> env_shift;

        rfa_i32 gate_target = Q31_ONE;
        if (gate_threshold > 0 && st->env < gate_threshold) {
            gate_target = 429496729;
        }
        st->gate_gain += (gate_target - st->gate_gain) >> 7;

        rfa_i32 lev_target = level_target(state->preset, st->env);
        st->level_gain += (lev_target - st->level_gain) >> 8;

        rfa_i32 y = mul_q31(hp, st->gate_gain);
        y = mul_q31(y, st->level_gain);
        y = apply_limiter(y);

        rfa_i32 out = y >> 16;
        if (out > 32767) out = 32767;
        if (out < -32768) out = -32768;
        samples[i] = (rfa_i16)out;
    }
}

void dsp_apply_gain_q30(rfa_i16 *samples, int count, rfa_u64 gain_q30) {
    if (samples == (rfa_i16 *)0 || count <= 0) return;
    if (gain_q30 > 4294967295ULL) gain_q30 = 4294967295ULL;
    rfa_u32 gain32 = (rfa_u32)gain_q30;

    for (int i = 0; i < count; ++i) {
        rfa_i32 s = (rfa_i32)samples[i];
        rfa_u32 mag = s < 0 ? (rfa_u32)(-s) : (rfa_u32)s;
        rfa_u64 scaled = (rfa_u64)mag * (rfa_u64)gain32;
        rfa_i32 out = (rfa_i32)(scaled >> 30);
        if (s < 0) out = -out;
        if (out > 32767) out = 32767;
        if (out < -32768) out = -32768;
        samples[i] = (rfa_i16)out;
    }
}
