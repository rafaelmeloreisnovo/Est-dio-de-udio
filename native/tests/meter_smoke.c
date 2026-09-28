#include "meter_core.h"

static void fill_alt(rfa_i16 *x, int n, rfa_i16 a) {
    for (int i = 0; i < n; ++i) x[i] = (i & 1) ? (rfa_i16)-a : a;
}

int main(void) {
    meter_state state_a;
    meter_state state_b;
    rfa_i16 block[480];
    fill_alt(block, 480, 12000);

    meter_state_reset(&state_a, 1, 0ULL);
    for (int i = 0; i < 50; ++i) meter_state_push(&state_a, block, 480, 1);

    rfa_u64 first[4] = {0,0,0,0};
    meter_state_result(&state_a, first);

    if (first[1] == 0ULL) return 10;
    if (first[2] == 0ULL) return 11;
    if (first[3] != ((rfa_u64)12000 << 16)) return 12;

    rfa_u64 relative = meter_state_relative_gate_block(&state_a);
    if (relative == 0ULL) return 20;

    meter_state_reset(&state_b, 1, relative);
    for (int i = 0; i < 50; ++i) meter_state_push(&state_b, block, 480, 1);

    rfa_u64 second[4] = {0,0,0,0};
    meter_state_result(&state_b, second);
    if (second[1] == 0ULL) return 30;

    rfa_u64 gain = meter_state_gain_q30(
            &state_b,
            403812580ULL,
            1913946816ULL);
    if (gain == 0ULL || gain > 4294967295ULL) return 31;

    rfa_u64 spectrum[16];
    meter_spectrum16(block, 480, 1, spectrum);
    rfa_u64 total = 0ULL;
    for (int i = 0; i < 16; ++i) total |= spectrum[i];
    if (total == 0ULL) return 40;

    return 0;
}
