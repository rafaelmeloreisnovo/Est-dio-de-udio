package io.rafaelia.audiostudio;

final class NativeDsp {
    static final int PRESET_WHATSAPP_VOICE = 0;
    static final int PRESET_NATURAL_VOICE = 1;
    static final int PRESET_MUSIC_CLEAN = 2;

    static {
        System.loadLibrary("rafaelia_audio");
    }

    private NativeDsp() {}

    static native void nativeReset(int preset);
    static native void nativeProcess(short[] data, int count, int channels);
}
