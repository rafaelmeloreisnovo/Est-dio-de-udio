/*
 * Deterministic defensive fuzz harness for ZRF/CFR headers and RAC1.
 * Host sanitizer evidence only; not a physical Android test.
 */
#include "rfa_container_core.h"
#include "rfa_rac1_core.h"
#include "rfa_int_math.h"

static rfa_u32 fuzz_next(rfa_u32 *state) {
    rfa_u32 x = *state;
    x ^= x << 13;
    x ^= x >> 17;
    x ^= x << 5;
    *state = x;
    return x;
}

static void copy_u8(rfa_u8 *dst, const rfa_u8 *src, int count) {
    int i;
    for (i = 0; i < count; ++i) dst[i] = src[i];
}

static int same_i16(const rfa_i16 *a, const rfa_i16 *b, int count) {
    int i;
    for (i = 0; i < count; ++i) if (a[i] != b[i]) return 0;
    return 1;
}

int main(void) {
    rfa_container_desc desc;
    rfa_container_desc decoded;
    rfa_u8 header[RFA_CONTAINER_HEADER_BYTES];
    rfa_u8 mutated[RFA_CONTAINER_HEADER_BYTES];
    rfa_u8 chunk[RFA_CONTAINER_CHUNK_HEADER_BYTES];
    rfa_u32 type;
    rfa_u32 flags;
    rfa_u32 payload;
    rfa_u32 items;
    rfa_i16 samples[64];
    rfa_i16 restored[64];
    rfa_u8 encoded[192];
    rfa_i16 guarded[66];
    rfa_u8 random_bytes[64];
    rfa_u32 rng = 0x51f15e5dU;
    int encoded_bytes;
    int i;

    desc.kind = RFA_CONTAINER_ZRF;
    desc.version = 1;
    desc.channels = 2;
    desc.matrix_dim = 14;
    desc.sample_rate = 48000U;
    desc.wave_count = 2U;
    desc.flags = 0U;
    desc.chunk_count = 3U;
    desc.payload_bytes = 4096U;
    desc.block_samples = 512U;

    if (rfa_container_write_header(
            header, RFA_CONTAINER_HEADER_BYTES, &desc) != RFA_CONTAINER_HEADER_BYTES) return 10;
    if (!rfa_container_read_header(header, RFA_CONTAINER_HEADER_BYTES, &decoded)) return 11;

    for (i = -2; i < RFA_CONTAINER_HEADER_BYTES; ++i) {
        if (rfa_container_read_header(header, i, &decoded)) return 12;
    }

    /* Any single-bit mutation in protected bytes/checksum must invalidate header. */
    for (i = 0; i < 36; ++i) {
        copy_u8(mutated, header, RFA_CONTAINER_HEADER_BYTES);
        mutated[i] ^= 1U;
        if (rfa_container_read_header(mutated, RFA_CONTAINER_HEADER_BYTES, &decoded)) return 13;
    }

    if (rfa_container_write_chunk_header(
            chunk, RFA_CONTAINER_CHUNK_HEADER_BYTES,
            RFA_CHUNK_PCM, 0U, 1024U, 512U) != RFA_CONTAINER_CHUNK_HEADER_BYTES) return 20;
    for (i = -2; i < RFA_CONTAINER_CHUNK_HEADER_BYTES; ++i) {
        if (rfa_container_read_chunk_header(
                chunk, i, &type, &flags, &payload, &items)) return 21;
    }
    if (!rfa_container_read_chunk_header(
            chunk, RFA_CONTAINER_CHUNK_HEADER_BYTES,
            &type, &flags, &payload, &items)) return 22;

    for (i = 0; i < 64; ++i) {
        int v = ((i * 997) & 65535) - 32768;
        samples[i] = (rfa_i16)v;
    }
    encoded_bytes = rfa_rac1_encode_i16(samples, 32, 2, encoded, 192);
    if (encoded_bytes <= 0) return 30;
    if (rfa_rac1_decode_i16(
            encoded, encoded_bytes, 32, 2, restored, 64) != 64) return 31;
    if (!same_i16(samples, restored, 64)) return 32;

    for (i = 0; i < encoded_bytes; ++i) {
        if (rfa_rac1_decode_i16(encoded, i, 32, 2, restored, 64) != 0) return 33;
    }

    /* Bounded mutation corpus. ASan/UBSan watches every call; sentinels catch overwrite. */
    for (i = 0; i < 8192; ++i) {
        int j;
        int encoded_len = (int)(fuzz_next(&rng) & 63U);
        int frames = (int)(fuzz_next(&rng) % 33U);
        int channels = (int)(fuzz_next(&rng) & 1U) + 1;
        int header_len = (int)(fuzz_next(&rng) % 43U) - 2;

        for (j = 0; j < 64; ++j) random_bytes[j] = (rfa_u8)fuzz_next(&rng);

        (void)rfa_container_read_header(random_bytes, header_len, &decoded);
        (void)rfa_container_read_chunk_header(
                random_bytes, header_len, &type, &flags, &payload, &items);

        guarded[0] = (rfa_i16)-23456;
        guarded[65] = (rfa_i16)23456;
        for (j = 1; j < 65; ++j) guarded[j] = (rfa_i16)0x5a5a;

        (void)rfa_rac1_decode_i16(
                random_bytes, encoded_len, frames, channels, guarded + 1, 64);
        if (guarded[0] != (rfa_i16)-23456) return 40;
        if (guarded[65] != (rfa_i16)23456) return 41;
    }

    /* Shared division helper: sign, zero-denominator, and INT64_MIN boundary. */
    if (rfa_div_i64_u64_shift(100LL, 4ULL) != 25LL) return 50;
    if (rfa_div_i64_u64_shift(-100LL, 4ULL) != -25LL) return 51;
    if (rfa_div_i64_u64_shift(7LL, 3ULL) != 2LL) return 52;
    if (rfa_div_i64_u64_shift(-7LL, 3ULL) != -2LL) return 53;
    if (rfa_div_i64_u64_shift(123LL, 0ULL) != 0LL) return 54;
    if (rfa_div_i64_u64_shift((-9223372036854775807LL - 1LL), 1ULL) !=
        (-9223372036854775807LL - 1LL)) return 55;

    return 0;
}
