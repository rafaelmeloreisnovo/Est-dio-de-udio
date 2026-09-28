#include "meter_core.h"

#define WINDOW_FRAMES_100MS 4800
#define WINDOWS_PER_BLOCK 4
#define BLOCK_FRAMES_400MS 19200
#define ABS_GATE_ENERGY_Q36 8057ULL
#define ABS_GATE_BLOCK_Q36 (ABS_GATE_ENERGY_Q36 * BLOCK_FRAMES_400MS)

typedef struct {
    rfa_i32 b0;
    rfa_i32 b1;
    rfa_i32 b2;
    rfa_i32 a1;
    rfa_i32 a2;
} biquad_coeff_q29;

static const biquad_coeff_q29 K_STAGE1 = {
    824163883, -1445093388, 643382241, -907665797, 393247621
};

static const biquad_coeff_q29 K_STAGE2 = {
    536870912, -1073741824, 536870912, -1068398592, 531540992
};

static const rfa_i32 TP_FIR_Q16[METER_TRUE_PEAK_TAPS][4] = {
    {   112,  -1912,  -1240,   -544},
    {   720,   1920,   2168,    976},
    { -1288,  -3392,  -3816,  -1744},
    {  2176,   5840,   6656,   3120},
    { -3896, -10912, -13128,  -6704},
    {  9000,  30480,  51104,  63712},
    { 63712,  51104,  30480,   9000},
    { -6704, -13128, -10912,  -3896},
    {  3120,   6656,   5840,   2176},
    { -1744,  -3816,  -3392,  -1288},
    {   976,   2168,   1920,    720},
    {  -544,  -1240,  -1912,    112}
};

static const rfa_i32 SPECTRUM_COEFF_Q30[METER_SPECTRUM_BANDS] = {
    2147365900, 2147196181, 2146747759, 2145658338,
    2142885721, 2135719508, 2118800422, 2074309917,
    1967498656, 1703713325, 1073741824,  555809667,
    -140452151,-1073741824,-1859775393,-2074309917
};

static rfa_i32 clamp_i32(rfa_i64 x) {
    if (x > 2147483647LL) return 2147483647;
    if (x < -2147483647LL - 1LL) return (rfa_i32)(-2147483647LL - 1LL);
    return (rfa_i32)x;
}

static rfa_u64 abs_i64_u(rfa_i64 x) {
    if (x >= 0) return (rfa_u64)x;
    return (rfa_u64)(-(x + 1LL)) + 1ULL;
}

static rfa_u32 abs_i32_u(rfa_i32 x) {
    if (x >= 0) return (rfa_u32)x;
    return (rfa_u32)(-(x + 1)) + 1U;
}

static rfa_u64 udiv64(rfa_u64 n, rfa_u64 d) {
    if (d == 0ULL) return 0ULL;
    rfa_u64 q = 0ULL;
    rfa_u64 r = 0ULL;
    for (int bit = 63; bit >= 0; --bit) {
        r = (r << 1) | ((n >> bit) & 1ULL);
        if (r >= d) {
            r -= d;
            q |= 1ULL << bit;
        }
    }
    return q;
}

static rfa_u64 ufrac_q30(rfa_u64 n, rfa_u64 d) {
    if (d == 0ULL || n == 0ULL) return 0ULL;
    if (n >= d) return 1ULL << 30;

    rfa_u64 r = n;
    rfa_u64 q = 0ULL;
    for (int bit = 0; bit < 30; ++bit) {
        q <<= 1;
        if (r >= d - r) {
            r = r - (d - r);
            q |= 1ULL;
        } else {
            r <<= 1;
        }
    }
    return q;
}

static rfa_u64 isqrt64(rfa_u64 x) {
    rfa_u64 result = 0ULL;
    rfa_u64 bit = 1ULL << 62;
    while (bit > x) bit >>= 2;
    while (bit != 0ULL) {
        if (x >= result + bit) {
            x -= result + bit;
            result = (result >> 1) + bit;
        } else {
            result >>= 1;
        }
        bit >>= 2;
    }
    return result;
}

static rfa_i32 biquad_step(rfa_i32 x, meter_biquad_state *st,
                           const biquad_coeff_q29 *c) {
    rfa_i64 acc = (rfa_i64)c->b0 * (rfa_i64)x;
    acc += (rfa_i64)c->b1 * (rfa_i64)st->x1;
    acc += (rfa_i64)c->b2 * (rfa_i64)st->x2;
    acc -= (rfa_i64)c->a1 * (rfa_i64)st->y1;
    acc -= (rfa_i64)c->a2 * (rfa_i64)st->y2;

    rfa_i32 y = clamp_i32(acc >> 29);
    st->x2 = st->x1;
    st->x1 = x;
    st->y2 = st->y1;
    st->y1 = y;
    return y;
}

static void true_peak_step(meter_state *state, rfa_i16 sample, int channel) {
    rfa_i32 *h = state->tp_hist[channel];
    for (int k = METER_TRUE_PEAK_TAPS - 1; k > 0; --k) h[k] = h[k - 1];
    h[0] = (rfa_i32)sample;

    rfa_u64 sample_abs = (rfa_u64)abs_i32_u((rfa_i32)sample) << 16;
    if (sample_abs > state->sample_peak_q16) state->sample_peak_q16 = sample_abs;

    for (int phase = 0; phase < 4; ++phase) {
        rfa_i64 acc = 0LL;
        for (int k = 0; k < METER_TRUE_PEAK_TAPS; ++k) {
            acc += (rfa_i64)h[k] * (rfa_i64)TP_FIR_Q16[k][phase];
        }
        rfa_u64 a = abs_i64_u(acc);
        if (a > state->true_peak_q16) state->true_peak_q16 = a;
    }
}

static void finish_window(meter_state *state) {
    state->windows[state->window_pos] = state->window_energy;
    state->window_pos = (state->window_pos + 1) & 3;
    if (state->windows_seen < WINDOWS_PER_BLOCK) ++state->windows_seen;

    if (state->windows_seen == WINDOWS_PER_BLOCK) {
        rfa_u64 block = state->windows[0] + state->windows[1] +
                        state->windows[2] + state->windows[3];
        if (block > state->gate_block) {
            state->block_sum += block;
            ++state->block_count;
        }
    }

    state->window_energy = 0ULL;
    state->window_frames = 0;
}

void meter_state_reset(meter_state *state, int channels, rfa_u64 gate_block_q36) {
    if (state == (meter_state *)0) return;

    state->channels = channels == 2 ? 2 : 1;
    state->window_energy = 0ULL;
    state->window_pos = 0;
    state->windows_seen = 0;
    state->window_frames = 0;
    state->gate_block = gate_block_q36 < ABS_GATE_BLOCK_Q36
            ? ABS_GATE_BLOCK_Q36 : gate_block_q36;
    state->block_sum = 0ULL;
    state->block_count = 0ULL;
    state->true_peak_q16 = 0ULL;
    state->sample_peak_q16 = 0ULL;

    for (int c = 0; c < 2; ++c) {
        state->k1[c].x1 = state->k1[c].x2 =
                state->k1[c].y1 = state->k1[c].y2 = 0;
        state->k2[c].x1 = state->k2[c].x2 =
                state->k2[c].y1 = state->k2[c].y2 = 0;
        for (int k = 0; k < METER_TRUE_PEAK_TAPS; ++k) state->tp_hist[c][k] = 0;
    }
    for (int w = 0; w < METER_WINDOW_SLOTS; ++w) state->windows[w] = 0ULL;
}

void meter_state_push(meter_state *state, const rfa_i16 *samples,
                      int count, int channels) {
    if (state == (meter_state *)0 || samples == (const rfa_i16 *)0 || count <= 0) return;
    if (channels != 1 && channels != 2) return;
    if (channels != state->channels) return;

    int frames = channels == 1 ? count : (count >> 1);
    for (int frame = 0; frame < frames; ++frame) {
        rfa_u64 frame_energy = 0ULL;

        for (int c = 0; c < channels; ++c) {
            rfa_i16 s = samples[frame * channels + c];
            true_peak_step(state, s, c);

            rfa_i32 x_q27 = (rfa_i32)s * 4096;
            rfa_i32 y1 = biquad_step(x_q27, &state->k1[c], &K_STAGE1);
            rfa_i32 y2 = biquad_step(y1, &state->k2[c], &K_STAGE2);

            rfa_i64 yy = (rfa_i64)y2 * (rfa_i64)y2;
            frame_energy += (rfa_u64)(yy >> 18);
        }

        state->window_energy += frame_energy;
        ++state->window_frames;
        if (state->window_frames == WINDOW_FRAMES_100MS) finish_window(state);
    }
}

void meter_state_result(const meter_state *state, rfa_u64 out_values[4]) {
    if (state == (const meter_state *)0 || out_values == (rfa_u64 *)0) return;
    out_values[0] = state->block_sum;
    out_values[1] = state->block_count;
    out_values[2] = state->true_peak_q16;
    out_values[3] = state->sample_peak_q16;
}

rfa_u64 meter_state_relative_gate_block(const meter_state *state) {
    if (state == (const meter_state *)0 || state->block_count == 0ULL) {
        return ABS_GATE_BLOCK_Q36;
    }
    rfa_u64 mean_block = udiv64(state->block_sum, state->block_count);
    rfa_u64 relative = udiv64(mean_block, 10ULL);
    return relative > ABS_GATE_BLOCK_Q36 ? relative : ABS_GATE_BLOCK_Q36;
}

rfa_u64 meter_state_gain_q30(const meter_state *state,
                             rfa_u64 target_energy_q36,
                             rfa_u64 true_peak_ceiling_q16) {
    if (state == (const meter_state *)0 ||
        state->block_count == 0ULL || state->block_sum == 0ULL) {
        return 1ULL << 30;
    }

    rfa_u64 measured_block = udiv64(state->block_sum, state->block_count);
    rfa_u32 target32 = target_energy_q36 > 4294967295ULL
            ? 4294967295U : (rfa_u32)target_energy_q36;
    rfa_u64 target_block = (rfa_u64)target32 * (rfa_u64)(rfa_u32)BLOCK_FRAMES_400MS;

    rfa_u64 ratio_q30;
    if (target_block >= measured_block) {
        rfa_u64 whole = udiv64(target_block, measured_block);
        if (whole >= 16ULL) {
            ratio_q30 = (16ULL << 30) - 1ULL;
        } else {
            rfa_u64 consumed = 0ULL;
            for (rfa_u32 k = 0; k < (rfa_u32)whole; ++k) consumed += measured_block;
            rfa_u64 rem = target_block - consumed;
            ratio_q30 = (whole << 30) + ufrac_q30(rem, measured_block);
        }
    } else {
        ratio_q30 = ufrac_q30(target_block, measured_block);
    }

    rfa_u64 gain_q30 = isqrt64(ratio_q30 << 30);
    if (gain_q30 > 4294967295ULL) gain_q30 = 4294967295ULL;

    if (state->true_peak_q16 != 0ULL && true_peak_ceiling_q16 != 0ULL) {
        rfa_u64 peak_cap_q30;
        if (true_peak_ceiling_q16 >= state->true_peak_q16) {
            rfa_u64 whole = udiv64(true_peak_ceiling_q16, state->true_peak_q16);
            if (whole >= 4ULL) {
                peak_cap_q30 = 4294967295ULL;
            } else {
                rfa_u64 consumed = 0ULL;
                for (rfa_u32 k = 0; k < (rfa_u32)whole; ++k) {
                    consumed += state->true_peak_q16;
                }
                rfa_u64 rem = true_peak_ceiling_q16 - consumed;
                peak_cap_q30 = (whole << 30) +
                        ufrac_q30(rem, state->true_peak_q16);
            }
        } else {
            peak_cap_q30 = ufrac_q30(true_peak_ceiling_q16,
                                     state->true_peak_q16);
        }
        if (peak_cap_q30 < gain_q30) gain_q30 = peak_cap_q30;
    }

    return gain_q30;
}

void meter_spectrum16(const rfa_i16 *samples, int count, int channels,
                      rfa_u64 out_bands[METER_SPECTRUM_BANDS]) {
    if (out_bands == (rfa_u64 *)0) return;
    for (int b = 0; b < METER_SPECTRUM_BANDS; ++b) out_bands[b] = 0ULL;
    if (samples == (const rfa_i16 *)0 || count <= 0) return;
    if (channels != 1 && channels != 2) return;

    int frames = channels == 1 ? count : (count >> 1);
    if (frames > 2048) frames = 2048;

    for (int b = 0; b < METER_SPECTRUM_BANDS; ++b) {
        rfa_i32 s1 = 0;
        rfa_i32 s2 = 0;
        rfa_i32 coeff = SPECTRUM_COEFF_Q30[b];

        for (int i = 0; i < frames; ++i) {
            rfa_i32 x;
            if (channels == 1) {
                x = samples[i];
            } else {
                x = ((rfa_i32)samples[i * 2] +
                     (rfa_i32)samples[i * 2 + 1]) >> 1;
            }

            rfa_i64 prod = (rfa_i64)coeff * (rfa_i64)s1;
            rfa_i64 next = (rfa_i64)x + (prod >> 30) - (rfa_i64)s2;
            rfa_i32 s0 = clamp_i32(next);
            s2 = s1;
            s1 = s0;
        }

        rfa_i64 p1 = (rfa_i64)s1 * (rfa_i64)s1;
        rfa_i64 p2 = (rfa_i64)s2 * (rfa_i64)s2;
        rfa_i32 weighted_s1 =
                (rfa_i32)(((rfa_i64)coeff * (rfa_i64)s1) >> 30);
        rfa_i64 cross = (rfa_i64)weighted_s1 * (rfa_i64)s2;
        rfa_i64 power = p1 + p2 - cross;
        out_bands[b] = power > 0 ? (rfa_u64)power : 0ULL;
    }
}
