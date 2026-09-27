#include "meter_core.h"

static void fill_alt(signed short *x, int n, signed short a) {
    for (int i = 0; i < n; ++i) x[i] = (i & 1) ? (signed short)-a : a;
}

int main(void) {
    signed short block[480];
    fill_alt(block, 480, 12000);

    meter_reset(1, 0ULL);
    for (int i = 0; i < 50; ++i) meter_push(block, 480, 1);

    rfa_u64 first[4] = {0,0,0,0};
    meter_result(first);

    if (first[1] == 0ULL) return 10;
    if (first[2] == 0ULL) return 11;
    if (first[3] != ((rfa_u64)12000 << 16)) return 12;

    rfa_u64 relative = meter_relative_gate_block();
    if (relative == 0ULL) return 20;

    meter_reset(1, relative);
    for (int i = 0; i < 50; ++i) meter_push(block, 480, 1);

    rfa_u64 second[4] = {0,0,0,0};
    meter_result(second);
    if (second[1] == 0ULL) return 30;

    rfa_u64 gain = meter_gain_q30(
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
