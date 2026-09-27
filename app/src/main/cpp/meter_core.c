#include "meter_core.h"

typedef signed int i32;
typedef signed long long i64;
typedef unsigned int u32;
typedef unsigned long long u64;
typedef signed short i16;

#define WINDOW_FRAMES_100MS 4800
#define WINDOWS_PER_BLOCK 4
#define BLOCK_FRAMES_400MS 19200
#define ABS_GATE_ENERGY_Q36 8057ULL
#define ABS_GATE_BLOCK_Q36 (ABS_GATE_ENERGY_Q36 * BLOCK_FRAMES_400MS)

/*
 * ITU-R BS.1770-5 K-weighting coefficients at 48 kHz.
 * Quantized to Q29 so every coefficient fits signed 32-bit and
 * ARMv7 can use 32x32 -> 64-bit multiply without external helpers.
 */
typedef struct {
    i32 b0;
    i32 b1;
    i32 b2;
    i32 a1;
    i32 a2;
} biquad_coeff_q29;

typedef struct {
    i32 x1;
    i32 x2;
    i32 y1;
    i32 y2;
} biquad_state;

static const biquad_coeff_q29 K_STAGE1 = {
    824163883, -1445093388, 643382241, -907665797, 393247621
};

static const biquad_coeff_q29 K_STAGE2 = {
    536870912, -1073741824, 536870912, -1068398592, 531540992
};

/*
 * ITU-R BS.1770-5 Annex 2, order-48 4-phase FIR, exact Q16 values.
 * Rows are time taps; columns are interpolation phases.
 */
static const i32 TP_FIR_Q16[12][4] = {
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

/* Precomputed Goertzel recurrence coefficients for 48 kHz, Q30. */
static const i32 SPECTRUM_COEFF_Q30[METER_SPECTRUM_BANDS] = {
    2147365900, 2147196181, 2146747759, 2145658338,
    2142885721, 2135719508, 2118800422, 2074309917,
    1967498656, 1703713325, 1073741824,  555809667,
    -140452151,-1073741824,-1859775393,-2074309917
};

static biquad_state g_k1[2];
static biquad_state g_k2[2];
static i32 g_tp_hist[2][12];
static u64 g_window_energy;
static u64 g_windows[WINDOWS_PER_BLOCK];
static int g_window_pos;
static int g_windows_seen;
static int g_window_frames;
static u64 g_gate_block;
static u64 g_block_sum;
static u64 g_block_count;
static u64 g_true_peak_q16;
static u64 g_sample_peak_q16;
static int g_channels;

static i32 clamp_i32(i64 x) {
    if (x > 2147483647LL) return 2147483647;
    if (x < -2147483647LL - 1LL) return (i32)(-2147483647LL - 1LL);
    return (i32)x;
}

static u64 abs_i64_u(i64 x) {
    if (x >= 0) return (u64)x;
    return (u64)(-(x + 1LL)) + 1ULL;
}

static u32 abs_i32_u(i32 x) {
    if (x >= 0) return (u32)x;
    return (u32)(-(x + 1)) + 1U;
}

static u64 udiv64(u64 n, u64 d) {
    if (d == 0ULL) return 0ULL;
    u64 q = 0ULL;
    u64 r = 0ULL;
    for (int bit = 63; bit >= 0; --bit) {
        r = (r << 1) | ((n >> bit) & 1ULL);
        if (r >= d) {
            r -= d;
            q |= 1ULL << bit;
        }
    }
    return q;
}

static u64 ufrac_q30(u64 n, u64 d) {
    if (d == 0ULL || n == 0ULL) return 0ULL;
    if (n >= d) return 1ULL << 30;

    u64 r = n;
    u64 q = 0ULL;
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

static u64 isqrt64(u64 x) {
    u64 result = 0ULL;
    u64 bit = 1ULL << 62;
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

static i32 biquad_step(i32 x, biquad_state *st,
                       const biquad_coeff_q29 *c) {
    i64 acc = (i64)c->b0 * (i64)x;
    acc += (i64)c->b1 * (i64)st->x1;
    acc += (i64)c->b2 * (i64)st->x2;
    acc -= (i64)c->a1 * (i64)st->y1;
    acc -= (i64)c->a2 * (i64)st->y2;

    i32 y = clamp_i32(acc >> 29);
    st->x2 = st->x1;
    st->x1 = x;
    st->y2 = st->y1;
    st->y1 = y;
    return y;
}

static void true_peak_step(i16 sample, int channel) {
    i32 *h = g_tp_hist[channel];
    for (int k = 11; k > 0; --k) h[k] = h[k - 1];
    h[0] = (i32)sample;

    u64 sample_abs = (u64)abs_i32_u((i32)sample) << 16;
    if (sample_abs > g_sample_peak_q16) g_sample_peak_q16 = sample_abs;

    for (int phase = 0; phase < 4; ++phase) {
        i64 acc = 0LL;
        for (int k = 0; k < 12; ++k) {
            acc += (i64)h[k] * (i64)TP_FIR_Q16[k][phase];
        }
        u64 a = abs_i64_u(acc);
        if (a > g_true_peak_q16) g_true_peak_q16 = a;
    }
}

static void finish_window(void) {
    g_windows[g_window_pos] = g_window_energy;
    g_window_pos = (g_window_pos + 1) & 3;
    if (g_windows_seen < WINDOWS_PER_BLOCK) ++g_windows_seen;

    if (g_windows_seen == WINDOWS_PER_BLOCK) {
        u64 block = g_windows[0] + g_windows[1] +
                    g_windows[2] + g_windows[3];
        if (block > g_gate_block) {
            g_block_sum += block;
            ++g_block_count;
        }
    }

    g_window_energy = 0ULL;
    g_window_frames = 0;
}

void meter_reset(int channels, u64 gate_block_q36) {
    g_channels = channels == 2 ? 2 : 1;
    g_window_energy = 0ULL;
    g_window_pos = 0;
    g_windows_seen = 0;
    g_window_frames = 0;
    g_gate_block = gate_block_q36 < ABS_GATE_BLOCK_Q36
            ? ABS_GATE_BLOCK_Q36 : gate_block_q36;
    g_block_sum = 0ULL;
    g_block_count = 0ULL;
    g_true_peak_q16 = 0ULL;
    g_sample_peak_q16 = 0ULL;

    for (int c = 0; c < 2; ++c) {
        g_k1[c].x1 = g_k1[c].x2 = g_k1[c].y1 = g_k1[c].y2 = 0;
        g_k2[c].x1 = g_k2[c].x2 = g_k2[c].y1 = g_k2[c].y2 = 0;
        for (int k = 0; k < 12; ++k) g_tp_hist[c][k] = 0;
    }
    for (int w = 0; w < WINDOWS_PER_BLOCK; ++w) g_windows[w] = 0ULL;
}

void meter_push(const i16 *samples, int count, int channels) {
    if (samples == (const i16 *)0 || count <= 0) return;
    if (channels != 1 && channels != 2) return;
    if (channels != g_channels) return;

    int frames = channels == 1 ? count : (count >> 1);
    for (int frame = 0; frame < frames; ++frame) {
        u64 frame_energy = 0ULL;

        for (int c = 0; c < channels; ++c) {
            i16 s = samples[frame * channels + c];
            true_peak_step(s, c);

            /* PCM16 -> Q27 leaves >4 bits of headroom through K weighting. */
            i32 x_q27 = (i32)s * 4096;
            i32 y1 = biquad_step(x_q27, &g_k1[c], &K_STAGE1);
            i32 y2 = biquad_step(y1, &g_k2[c], &K_STAGE2);

            i64 yy = (i64)y2 * (i64)y2;
            frame_energy += (u64)(yy >> 18); /* Q36 normalized power. */
        }

        g_window_energy += frame_energy;
        ++g_window_frames;
        if (g_window_frames == WINDOW_FRAMES_100MS) finish_window();
    }
}

void meter_result(u64 out_values[4]) {
    if (out_values == (u64 *)0) return;
    out_values[0] = g_block_sum;
    out_values[1] = g_block_count;
    out_values[2] = g_true_peak_q16;
    out_values[3] = g_sample_peak_q16;
}

u64 meter_relative_gate_block(void) {
    if (g_block_count == 0ULL) return ABS_GATE_BLOCK_Q36;
    u64 mean_block = udiv64(g_block_sum, g_block_count);
    u64 relative = udiv64(mean_block, 10ULL); /* -10 LU in power domain. */
    return relative > ABS_GATE_BLOCK_Q36 ? relative : ABS_GATE_BLOCK_Q36;
}

u64 meter_gain_q30(u64 target_energy_q36, u64 true_peak_ceiling_q16) {
    if (g_block_count == 0ULL || g_block_sum == 0ULL) return 1ULL << 30;

    u64 measured_block = udiv64(g_block_sum, g_block_count);
    u32 target32 = target_energy_q36 > 4294967295ULL
            ? 4294967295U : (u32)target_energy_q36;
    u64 target_block = (u64)target32 * (u64)(u32)BLOCK_FRAMES_400MS;

    u64 ratio_q30;
    if (target_block >= measured_block) {
        u64 whole = udiv64(target_block, measured_block);
        if (whole >= 16ULL) {
            ratio_q30 = (16ULL << 30) - 1ULL;
        } else {
            u64 consumed = 0ULL;
            for (u32 k = 0; k < (u32)whole; ++k) consumed += measured_block;
            u64 rem = target_block - consumed;
            ratio_q30 = (whole << 30) +
                    ufrac_q30(rem, measured_block);
        }
    } else {
        ratio_q30 = ufrac_q30(target_block, measured_block);
    }

    u64 gain_q30 = isqrt64(ratio_q30 << 30);
    if (gain_q30 > 4294967295ULL) gain_q30 = 4294967295ULL;

    if (g_true_peak_q16 != 0ULL && true_peak_ceiling_q16 != 0ULL) {
        u64 peak_cap_q30;
        if (true_peak_ceiling_q16 >= g_true_peak_q16) {
            u64 whole = udiv64(true_peak_ceiling_q16, g_true_peak_q16);
            if (whole >= 4ULL) {
                peak_cap_q30 = 4294967295ULL;
            } else {
                u64 consumed = 0ULL;
                for (u32 k = 0; k < (u32)whole; ++k) consumed += g_true_peak_q16;
                u64 rem = true_peak_ceiling_q16 - consumed;
                peak_cap_q30 = (whole << 30) +
                        ufrac_q30(rem, g_true_peak_q16);
            }
        } else {
            peak_cap_q30 =
                    ufrac_q30(true_peak_ceiling_q16, g_true_peak_q16);
        }
        if (peak_cap_q30 < gain_q30) gain_q30 = peak_cap_q30;
    }

    return gain_q30;
}

void meter_spectrum16(const i16 *samples, int count, int channels,
                      u64 out_bands[METER_SPECTRUM_BANDS]) {
    if (out_bands == (u64 *)0) return;
    for (int b = 0; b < METER_SPECTRUM_BANDS; ++b) out_bands[b] = 0ULL;
    if (samples == (const i16 *)0 || count <= 0) return;
    if (channels != 1 && channels != 2) return;

    int frames = channels == 1 ? count : (count >> 1);
    if (frames > 2048) frames = 2048;

    for (int b = 0; b < METER_SPECTRUM_BANDS; ++b) {
        i32 s1 = 0;
        i32 s2 = 0;
        i32 coeff = SPECTRUM_COEFF_Q30[b];

        for (int i = 0; i < frames; ++i) {
            i32 x;
            if (channels == 1) {
                x = samples[i];
            } else {
                x = ((i32)samples[i * 2] + (i32)samples[i * 2 + 1]) >> 1;
            }

            i64 prod = (i64)coeff * (i64)s1;
            i64 next = (i64)x + (prod >> 30) - (i64)s2;
            i32 s0 = clamp_i32(next);
            s2 = s1;
            s1 = s0;
        }

        i64 p1 = (i64)s1 * (i64)s1;
        i64 p2 = (i64)s2 * (i64)s2;
        i32 weighted_s1 = (i32)(((i64)coeff * (i64)s1) >> 30);
        i64 cross = (i64)weighted_s1 * (i64)s2;
        i64 power = p1 + p2 - cross;
        out_bands[b] = power > 0 ? (u64)power : 0ULL;
    }
}
