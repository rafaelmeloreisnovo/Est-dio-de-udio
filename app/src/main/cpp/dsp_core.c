#include "dsp_core.h"

typedef signed int i32;
typedef signed long long i64;
typedef signed short i16;

_Static_assert(sizeof(i16) == 2, "requires 16-bit short");
_Static_assert(sizeof(i32) == 4, "requires 32-bit int");
_Static_assert(sizeof(i64) == 8, "requires 64-bit long long");

#define Q31_ONE 2147483647
#define Q31_LIMIT_M1DB 1913946815

typedef struct {
    i32 prev_x;
    i32 hp_y;
    i32 env;
    i32 gate_gain;
    i32 level_gain;
} channel_state;

static channel_state g_state[2];
static int g_preset;

static i32 clamp_q31(i64 x) {
    if (x > (i64)Q31_ONE) return Q31_ONE;
    if (x < (i64)-2147483647 - 1) return (i32)(-2147483647 - 1);
    return (i32)x;
}

static i32 mul_q31(i32 a, i32 b) {
    return (i32)(((i64)a * (i64)b) >> 31);
}

static i32 abs_q31(i32 x) {
    if (x == (i32)(-2147483647 - 1)) return Q31_ONE;
    return x < 0 ? -x : x;
}

static i32 hp_alpha_for_preset(void) {
    if (g_preset == DSP_PRESET_MUSIC_CLEAN) return 2140467510;
    if (g_preset == DSP_PRESET_NATURAL_VOICE) return 2130683411;
    return 2127896177;
}

static i32 gate_threshold_for_preset(void) {
    if (g_preset == DSP_PRESET_MUSIC_CLEAN) return 0;
    if (g_preset == DSP_PRESET_NATURAL_VOICE) return 2703522;
    return 5394235;
}

static i32 level_threshold_for_preset(void) {
    if (g_preset == DSP_PRESET_MUSIC_CLEAN) return 1076291388;
    if (g_preset == DSP_PRESET_NATURAL_VOICE) return 539423503;
    return 428479319;
}

static i32 level_target(i32 env) {
    i32 t = level_threshold_for_preset();

    if ((i64)env > (i64)t * 3) {
        return g_preset == DSP_PRESET_WHATSAPP_VOICE
                ? 1245540515
                : 1610612735;
    }
    if ((i64)env > (i64)t * 2) {
        return g_preset == DSP_PRESET_WHATSAPP_VOICE
                ? 1546188226
                : 1825361100;
    }
    if (env > t) {
        return g_preset == DSP_PRESET_WHATSAPP_VOICE
                ? 1889785609
                : 1932735282;
    }
    return Q31_ONE;
}

static i32 apply_limiter(i32 x) {
    i32 a = abs_q31(x);
    if (a <= Q31_LIMIT_M1DB) return x;

    i32 excess = a - Q31_LIMIT_M1DB;
    i32 limited = Q31_LIMIT_M1DB + (excess >> 3);
    if (limited > Q31_ONE) limited = Q31_ONE;
    return x < 0 ? -limited : limited;
}

void dsp_reset(int preset) {
    g_preset = preset;
    for (int c = 0; c < 2; c++) {
        g_state[c].prev_x = 0;
        g_state[c].hp_y = 0;
        g_state[c].env = 0;
        g_state[c].gate_gain = Q31_ONE;
        g_state[c].level_gain = Q31_ONE;
    }
}

void dsp_process(i16 *samples, int count, int channels) {
    if (samples == (void *)0 || count <= 0) return;
    if (channels < 1 || channels > 2) return;

    const i32 alpha = hp_alpha_for_preset();
    const i32 gate_threshold = gate_threshold_for_preset();

    for (int i = 0; i < count; i++) {
        int c = i % channels;
        channel_state *st = &g_state[c];

        i32 x = (i32)((i64)samples[i] * 65536);

        i64 hp_input = (i64)st->hp_y + (i64)x - (i64)st->prev_x;
        i32 hp = mul_q31(alpha, clamp_q31(hp_input));
        st->prev_x = x;
        st->hp_y = hp;

        i32 a = abs_q31(hp);
        int env_shift = a > st->env ? 5 : 11;
        st->env += (a - st->env) >> env_shift;

        i32 gate_target = Q31_ONE;
        if (gate_threshold > 0 && st->env < gate_threshold) {
            gate_target = 429496729;
        }
        st->gate_gain += (gate_target - st->gate_gain) >> 7;

        i32 lev_target = level_target(st->env);
        st->level_gain += (lev_target - st->level_gain) >> 8;

        i32 y = mul_q31(hp, st->gate_gain);
        y = mul_q31(y, st->level_gain);
        y = apply_limiter(y);

        i32 out = y >> 16;
        if (out > 32767) out = 32767;
        if (out < -32768) out = -32768;
        samples[i] = (i16)out;
    }
}
