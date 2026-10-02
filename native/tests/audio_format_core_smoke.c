#include "rfa_audio_format_core.h"

static int eq4(const rfa_u8 *p, rfa_u8 a, rfa_u8 b, rfa_u8 c, rfa_u8 d) {
    return p[0] == a && p[1] == b && p[2] == c && p[3] == d;
}

int main(void) {
    rfa_u8 out[80];
    rfa_u8 sample[4] = {0x34u, 0x12u, 0xccu, 0xedu};
    rfa_u32 written = 0u;

    if (rfa_audio_format_header_size(RFA_AUDIO_FORMAT_RAW_PCM16) != 0u) return 1;
    if (rfa_audio_format_header_size(RFA_AUDIO_FORMAT_WAV_PCM16) != 44u) return 2;
    if (rfa_audio_format_header_size(RFA_AUDIO_FORMAT_AIFF_PCM16) != 54u) return 3;
    if (rfa_audio_format_header_size(RFA_AUDIO_FORMAT_AU_PCM16) != 28u) return 4;
    if (rfa_audio_format_header_size(RFA_AUDIO_FORMAT_CAF_PCM16) != 68u) return 5;

    if (!rfa_audio_format_write_header(
            RFA_AUDIO_FORMAT_WAV_PCM16, 48000u, 1u, 4u, out, 80u, &written)) return 6;
    if (written != 44u || !eq4(out, 'R', 'I', 'F', 'F') || !eq4(out + 8, 'W', 'A', 'V', 'E')) return 7;

    if (!rfa_audio_format_write_header(
            RFA_AUDIO_FORMAT_AIFF_PCM16, 48000u, 1u, 4u, out, 80u, &written)) return 8;
    if (written != 54u || !eq4(out, 'F', 'O', 'R', 'M') || !eq4(out + 8, 'A', 'I', 'F', 'F')) return 9;
    if (out[28] != 0x40u || out[29] != 0x0eu || out[30] != 0xbbu || out[31] != 0x80u) return 10;

    if (!rfa_audio_format_write_header(
            RFA_AUDIO_FORMAT_AU_PCM16, 48000u, 1u, 4u, out, 80u, &written)) return 11;
    if (written != 28u || !eq4(out, '.', 's', 'n', 'd') || out[15] != 3u) return 12;

    if (!rfa_audio_format_write_header(
            RFA_AUDIO_FORMAT_CAF_PCM16, 48000u, 1u, 4u, out, 80u, &written)) return 13;
    if (written != 68u || !eq4(out, 'c', 'a', 'f', 'f') || !eq4(out + 28, 'l', 'p', 'c', 'm')) return 14;
    if (!eq4(out + 52, 'd', 'a', 't', 'a')) return 15;

    if (!rfa_pcm16_swap_pairs(sample, 4u)) return 16;
    if (sample[0] != 0x12u || sample[1] != 0x34u || sample[2] != 0xedu || sample[3] != 0xccu) return 17;
    if (rfa_pcm16_swap_pairs(sample, 3u) != RFA_AUDIO_FORMAT_FAIL) return 18;

    return 0;
}
