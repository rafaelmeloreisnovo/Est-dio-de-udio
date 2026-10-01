/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 * SOURCE != EXECUTION != EVIDENCE != CLAIM.
 */

#include "rfa_measure_core.h"
#include "rfa_sine_q15.h"

#define RFA_DECAY_MINUS_5DB_Q30 339546978U
#define RFA_DECAY_MINUS_10DB_Q30 107374182U
#define RFA_DECAY_MINUS_25DB_Q30 3395470U
#define RFA_DECAY_MINUS_35DB_Q30 339547U

static rfa_i16 rfa_measure_clip_i16(rfa_i64 value) {
    if (value > 32767) return (rfa_i16)32767;
    if (value < -32768) return (rfa_i16)-32768;
    return (rfa_i16)value;
}

static rfa_u64 rfa_measure_udiv64(rfa_u64 n, rfa_u64 d) {
    rfa_u64 q = 0ULL;
    rfa_u64 r = 0ULL;
    int bit;
    if (d == 0ULL) return 0ULL;
    for (bit = 63; bit >= 0; --bit) {
        r = (r << 1) | ((n >> bit) & 1ULL);
        if (r >= d) {
            r -= d;
            q |= 1ULL << bit;
        }
    }
    return q;
}

static rfa_u32 rfa_measure_fraction_q30(rfa_u64 n, rfa_u64 d) {
    rfa_u64 r;
    rfa_u32 q = 0U;
    int bit;
    if (d == 0ULL || n == 0ULL) return 0U;
    if (n >= d) return 1U << 30;
    r = n;
    for (bit = 0; bit < 30; ++bit) {
        q <<= 1;
        if (r >= d - r) {
            r = r - (d - r);
            q |= 1U;
        } else {
            r <<= 1;
        }
    }
    return q;
}

static rfa_u32 rfa_measure_ratio_q20(rfa_u64 n, rfa_u64 d) {
    rfa_u64 whole;
    rfa_u64 consumed;
    rfa_u64 remainder;
    rfa_u64 r;
    rfa_u32 fraction = 0U;
    int bit;

    if (d == 0ULL || n == 0ULL) return 0U;
    whole = rfa_measure_udiv64(n, d);
    if (whole >= 4095ULL) return 0xffffffffU;
    consumed = whole * d;
    remainder = n - consumed;
    r = remainder;
    for (bit = 0; bit < 20; ++bit) {
        fraction <<= 1;
        if (r >= d - r) {
            r = r - (d - r);
            fraction |= 1U;
        } else {
            r <<= 1;
        }
    }
    return (rfa_u32)((whole << 20) | (rfa_u64)fraction);
}

void rfa_exp_sweep_reset_q31(rfa_exp_sweep_q31 *state,
                             rfa_u32 start_step_q32,
                             rfa_u32 ratio_q31,
                             rfa_i32 gain_q15,
                             int frames) {
    if (state == (rfa_exp_sweep_q31 *)0) return;
    if (gain_q15 > 32767) gain_q15 = 32767;
    if (gain_q15 < -32768) gain_q15 = -32768;
    state->phase = 0U;
    state->step_q32 = start_step_q32;
    state->ratio_q31 = ratio_q31 < 2147483648U ? 2147483648U : ratio_q31;
    state->gain_q15 = gain_q15;
    state->frames_left = frames < 0 ? 0 : frames;
}

int rfa_exp_sweep_render_q15(rfa_exp_sweep_q31 *state,
                             rfa_i16 *output, int frames, int channels) {
    int produced = 0;
    if (state == (rfa_exp_sweep_q31 *)0 || output == (rfa_i16 *)0) return 0;
    if (frames <= 0 || channels <= 0 || channels > 2) return 0;

    while (produced < frames && state->frames_left > 0) {
        rfa_i16 sine = rfa_sine_lookup_q15(state->phase);
        rfa_i64 scaled = ((rfa_i64)sine * (rfa_i64)state->gain_q15) >> 15;
        int channel;
        rfa_u64 next_step;

        for (channel = 0; channel < channels; ++channel) {
            output[produced * channels + channel] = rfa_measure_clip_i16(scaled);
        }

        state->phase += state->step_q32;
        next_step = ((rfa_u64)state->step_q32 * (rfa_u64)state->ratio_q31) >> 31;
        if (next_step > 0xffffffffULL) next_step = 0xffffffffULL;
        state->step_q32 = (rfa_u32)next_step;
        --state->frames_left;
        ++produced;
    }
    return produced;
}

int rfa_sync_sequence_q15(rfa_i16 *output, int count,
                          rfa_u32 seed, rfa_i32 gain_q15) {
    rfa_u32 state;
    int i;
    if (output == (rfa_i16 *)0 || count <= 0) return 0;
    if (gain_q15 < 0) {
        gain_q15 = gain_q15 < -32767 ? 32767 : -gain_q15;
    }
    if (gain_q15 > 32767) gain_q15 = 32767;
    state = seed == 0U ? 0x6d2b79f5U : seed;
    for (i = 0; i < count; ++i) {
        state ^= state << 13;
        state ^= state >> 17;
        state ^= state << 5;
        output[i] = (state & 1U) != 0U ?
                (rfa_i16)gain_q15 : (rfa_i16)-gain_q15;
    }
    return count;
}

int rfa_relative_transfer_search(const rfa_i16 *reference, int reference_count,
                                 const rfa_i16 *response, int response_count,
                                 int min_lag, int max_lag,
                                 rfa_relative_transfer *result) {
    int lag;
    int best_lag = 0;
    rfa_i64 best_corr = 0LL;
    rfa_u64 best_abs_corr = 0ULL;
    rfa_u64 best_ref_energy = 0ULL;
    rfa_u64 best_resp_energy = 0ULL;

    if (reference == (const rfa_i16 *)0 ||
        response == (const rfa_i16 *)0 ||
        result == (rfa_relative_transfer *)0) return 0;
    if (reference_count <= 0 || response_count <= 0) return 0;
    if (min_lag < 0) min_lag = 0;
    if (max_lag >= response_count) max_lag = response_count - 1;
    if (max_lag < min_lag) return 0;

    for (lag = min_lag; lag <= max_lag; ++lag) {
        int available = response_count - lag;
        int count = reference_count < available ? reference_count : available;
        int i;
        rfa_i64 corr = 0LL;
        rfa_u64 ref_energy = 0ULL;
        rfa_u64 resp_energy = 0ULL;

        if (count <= 0) continue;
        for (i = 0; i < count; ++i) {
            rfa_i64 a = reference[i];
            rfa_i64 b = response[i + lag];
            corr += a * b;
            ref_energy += (rfa_u64)(a * a);
            resp_energy += (rfa_u64)(b * b);
        }
        {
            rfa_u64 abs_corr = corr < 0LL ?
                    (rfa_u64)(-(corr + 1LL)) + 1ULL : (rfa_u64)corr;
            if (abs_corr > best_abs_corr) {
                best_abs_corr = abs_corr;
                best_corr = corr;
                best_lag = lag;
                best_ref_energy = ref_energy;
                best_resp_energy = resp_energy;
            }
        }
    }

    result->correlation = best_corr;
    result->reference_energy = best_ref_energy;
    result->response_energy = best_resp_energy;
    result->best_lag = best_lag;
    result->analyzed = best_abs_corr != 0ULL ? 1 : 0;
    return result->analyzed;
}

int rfa_sweep_band_profile_q20(
        const rfa_i16 *reference, int reference_count, int reference_offset,
        const rfa_i16 *response, int response_count, int response_offset,
        int sweep_frames, rfa_sweep_band_profile *profile) {
    int available_reference;
    int available_response;
    int frames;
    int base_frames;
    int remainder_frames;
    int cursor = 0;
    int band;

    if (reference == (const rfa_i16 *)0 || response == (const rfa_i16 *)0 ||
        profile == (rfa_sweep_band_profile *)0) return 0;
    if (reference_count <= 0 || response_count <= 0 || sweep_frames <= 0) return 0;
    if (reference_offset < 0 || response_offset < 0) return 0;
    if (reference_offset >= reference_count || response_offset >= response_count) return 0;

    profile->valid_bands = 0;
    profile->analyzed_frames = 0;
    for (band = 0; band < RFA_SWEEP_PROFILE_BANDS; ++band) {
        profile->reference_energy[band] = 0ULL;
        profile->response_energy[band] = 0ULL;
        profile->power_ratio_q20[band] = 0U;
    }

    available_reference = reference_count - reference_offset;
    available_response = response_count - response_offset;
    frames = sweep_frames;
    if (frames > available_reference) frames = available_reference;
    if (frames > available_response) frames = available_response;
    if (frames < RFA_SWEEP_PROFILE_BANDS) return 0;

    base_frames = frames >> 4;
    remainder_frames = frames & 15;

    for (band = 0; band < RFA_SWEEP_PROFILE_BANDS; ++band) {
        int band_frames = base_frames + (band < remainder_frames ? 1 : 0);
        int i;
        rfa_u64 ref_energy = 0ULL;
        rfa_u64 resp_energy = 0ULL;
        for (i = 0; i < band_frames; ++i) {
            rfa_i64 a = reference[reference_offset + cursor + i];
            rfa_i64 b = response[response_offset + cursor + i];
            ref_energy += (rfa_u64)(a * a);
            resp_energy += (rfa_u64)(b * b);
        }
        profile->reference_energy[band] = ref_energy;
        profile->response_energy[band] = resp_energy;
        if (ref_energy != 0ULL) {
            profile->power_ratio_q20[band] =
                    rfa_measure_ratio_q20(resp_energy, ref_energy);
            ++profile->valid_bands;
        }
        cursor += band_frames;
    }

    profile->analyzed_frames = cursor;
    return profile->valid_bands != 0 ? 1 : 0;
}

int rfa_decay_profile_q30(const rfa_i16 *samples, int count,
                          int noise_tail_frames, rfa_decay_profile *profile) {
    rfa_u64 raw_energy = 0ULL;
    rfa_u64 noise_energy = 0ULL;
    rfa_u64 noise_per_sample;
    rfa_u64 corrected_initial;
    rfa_u64 remaining_energy;
    int i;

    if (samples == (const rfa_i16 *)0 || profile == (rfa_decay_profile *)0) return 0;
    if (count <= 0) return 0;
    if (noise_tail_frames <= 0) noise_tail_frames = 1;
    if (noise_tail_frames > count) noise_tail_frames = count;

    profile->raw_energy = 0ULL;
    profile->noise_energy_per_sample = 0ULL;
    profile->corrected_initial_energy = 0ULL;
    profile->analyzed_frames = count;
    profile->noise_tail_frames = noise_tail_frames;
    profile->t5_frames = -1;
    profile->t10_frames = -1;
    profile->t25_frames = -1;
    profile->t35_frames = -1;
    profile->edt60_frames = -1;
    profile->t20_rt60_frames = -1;
    profile->t30_rt60_frames = -1;
    profile->flags = 0U;

    for (i = 0; i < count; ++i) {
        rfa_i64 x = samples[i];
        raw_energy += (rfa_u64)(x * x);
    }
    for (i = count - noise_tail_frames; i < count; ++i) {
        rfa_i64 x = samples[i];
        noise_energy += (rfa_u64)(x * x);
    }

    noise_per_sample = rfa_measure_udiv64(
            noise_energy, (rfa_u64)(rfa_u32)noise_tail_frames);
    if (raw_energy > noise_per_sample * (rfa_u64)(rfa_u32)count) {
        corrected_initial = raw_energy -
                noise_per_sample * (rfa_u64)(rfa_u32)count;
    } else {
        corrected_initial = 0ULL;
    }

    profile->raw_energy = raw_energy;
    profile->noise_energy_per_sample = noise_per_sample;
    profile->corrected_initial_energy = corrected_initial;
    if (corrected_initial == 0ULL) return 0;

    remaining_energy = raw_energy;
    for (i = 0; i < count; ++i) {
        int remaining_frames = count - i;
        rfa_u64 expected_noise = noise_per_sample *
                (rfa_u64)(rfa_u32)remaining_frames;
        rfa_u64 corrected = remaining_energy > expected_noise ?
                remaining_energy - expected_noise : 0ULL;
        rfa_u32 ratio_q30 = rfa_measure_fraction_q30(
                corrected, corrected_initial);
        rfa_i64 x = samples[i];
        rfa_u64 x2 = (rfa_u64)(x * x);

        if (profile->t5_frames < 0 && ratio_q30 <= RFA_DECAY_MINUS_5DB_Q30) {
            profile->t5_frames = i;
        }
        if (profile->t10_frames < 0 && ratio_q30 <= RFA_DECAY_MINUS_10DB_Q30) {
            profile->t10_frames = i;
        }
        if (profile->t25_frames < 0 && ratio_q30 <= RFA_DECAY_MINUS_25DB_Q30) {
            profile->t25_frames = i;
        }
        if (profile->t35_frames < 0 && ratio_q30 <= RFA_DECAY_MINUS_35DB_Q30) {
            profile->t35_frames = i;
        }

        remaining_energy = remaining_energy > x2 ? remaining_energy - x2 : 0ULL;
    }

    if (profile->t10_frames > 0) {
        profile->edt60_frames = profile->t10_frames * 6;
        profile->flags |= RFA_DECAY_FLAG_EDT;
    }
    if (profile->t5_frames >= 0 && profile->t25_frames > profile->t5_frames) {
        profile->t20_rt60_frames =
                (profile->t25_frames - profile->t5_frames) * 3;
        profile->flags |= RFA_DECAY_FLAG_T20;
    }
    if (profile->t5_frames >= 0 && profile->t35_frames > profile->t5_frames) {
        profile->t30_rt60_frames =
                (profile->t35_frames - profile->t5_frames) * 2;
        profile->flags |= RFA_DECAY_FLAG_T30;
    }
    return 1;
}
