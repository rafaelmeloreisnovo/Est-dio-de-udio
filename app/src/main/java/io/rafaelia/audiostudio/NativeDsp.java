package io.rafaelia.audiostudio;

final class NativeDsp {
    static final int PRESET_WHATSAPP_VOICE = 0;
    static final int PRESET_NATURAL_VOICE = 1;
    static final int PRESET_MUSIC_CLEAN = 2;

    static final int CONTAINER_ZRF = 1;
    static final int CONTAINER_CFR = 2;
    static final long CHUNK_PCM = 0x204d4350L;

    static final long TARGET_EBU_R128_Q36 = 403_812_580L;    // -23 LUFS
    static final long TARGET_NARRATION_Q36 = 1_276_967_499L; // -18 LUFS custom
    static final long TARGET_MOBILE_Q36 = 2_023_857_096L;    // -16 LUFS custom
    static final long TRUE_PEAK_MINUS_1DB_Q16 = 1_913_946_816L;

    static {
        System.loadLibrary("rafaelia_audio");
    }

    private NativeDsp() {}

    static native void nativeReset(int preset);
    static native void nativeProcess(short[] data, int count, int channels);
    static native void nativeApplyGain(short[] data, int count, long gainQ30);

    static native void nativeMeterReset(int channels, long gateBlockQ36);
    static native void nativeMeterPush(short[] data, int count, int channels);
    static native void nativeMeterResult(long[] output);
    static native long nativeMeterRelativeGate();
    static native long nativeMeterGain(long targetEnergyQ36, long ceilingQ16);

    static native void nativeSpectrum(
            short[] data, int count, int channels, long[] output);

    static native byte[] nativeContainerHeader(
            int kind, int channels, int matrixDim, int sampleRate,
            int waveCount, int flags, int chunkCount,
            long payloadBytes, int blockSamples);

    static native byte[] nativeChunkHeader(
            long type, long flags, long payloadBytes, long itemCount);
}

