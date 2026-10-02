package io.rafaelia.audiostudio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class AudioInteropWriterSmoke {
    public static void main(String[] args) throws Exception {
        File pcm = Files.createTempFile("rafaelia-audio-interop", ".pcm").toFile();
        try (FileOutputStream out = new FileOutputStream(pcm)) {
            out.write(new byte[] {0x34, 0x12, (byte) 0xcc, (byte) 0xed});
        }

        check(AudioInteropWriter.Format.values().length == 5, "FORMAT_COUNT");
        checkRaw(pcm);
        checkWav(pcm);
        checkAiff(pcm);
        checkAu(pcm);
        checkCaf(pcm);

        if (!pcm.delete()) pcm.deleteOnExit();
        System.out.println("AUDIO_INTEROP_WRITER_SMOKE=PASS");
        System.out.println("AUTHORIAL_EXPORT_FORMATS=RAW_PCM16,WAV_PCM16,AIFF_PCM16,AU_PCM16,CAF_LPCM16");
        System.out.println("EXTERNAL_CODEC_LIBRARY=NONE");
    }

    private static void checkRaw(File pcm) throws Exception {
        byte[] bytes = write(AudioInteropWriter.Format.RAW_PCM, pcm);
        check(bytes.length == 4, "RAW_SIZE");
        check(bytes[0] == 0x34 && bytes[1] == 0x12, "RAW_ENDIAN");
    }

    private static void checkWav(File pcm) throws Exception {
        byte[] bytes = write(AudioInteropWriter.Format.WAV_PCM16, pcm);
        check(bytes.length == 48, "WAV_SIZE");
        checkAscii(bytes, 0, "RIFF", "WAV_RIFF");
        checkAscii(bytes, 8, "WAVE", "WAV_WAVE");
        checkAscii(bytes, 36, "data", "WAV_DATA");
        check(bytes[44] == 0x34 && bytes[45] == 0x12, "WAV_ENDIAN");
    }

    private static void checkAiff(File pcm) throws Exception {
        byte[] bytes = write(AudioInteropWriter.Format.AIFF_PCM16, pcm);
        check(bytes.length == 58, "AIFF_SIZE");
        checkAscii(bytes, 0, "FORM", "AIFF_FORM");
        checkAscii(bytes, 8, "AIFF", "AIFF_TYPE");
        checkAscii(bytes, 12, "COMM", "AIFF_COMM");
        checkAscii(bytes, 38, "SSND", "AIFF_SSND");
        byte[] ext80 = new byte[] {
                0x40, 0x0e, (byte) 0xbb, (byte) 0x80, 0, 0, 0, 0, 0, 0
        };
        for (int i = 0; i < ext80.length; ++i) {
            check(bytes[28 + i] == ext80[i], "AIFF_RATE80_" + i);
        }
        check(bytes[54] == 0x12 && bytes[55] == 0x34, "AIFF_ENDIAN");
    }

    private static void checkAu(File pcm) throws Exception {
        byte[] bytes = write(AudioInteropWriter.Format.AU_PCM16, pcm);
        check(bytes.length == 32, "AU_SIZE");
        checkAscii(bytes, 0, ".snd", "AU_MAGIC");
        check(u32be(bytes, 4) == 28L, "AU_OFFSET");
        check(u32be(bytes, 12) == 3L, "AU_ENCODING");
        check(bytes[28] == 0x12 && bytes[29] == 0x34, "AU_ENDIAN");
    }

    private static void checkCaf(File pcm) throws Exception {
        byte[] bytes = write(AudioInteropWriter.Format.CAF_PCM16, pcm);
        check(bytes.length == 72, "CAF_SIZE");
        checkAscii(bytes, 0, "caff", "CAF_MAGIC");
        checkAscii(bytes, 8, "desc", "CAF_DESC");
        checkAscii(bytes, 28, "lpcm", "CAF_LPCM");
        checkAscii(bytes, 52, "data", "CAF_DATA");
        check(bytes[68] == 0x34 && bytes[69] == 0x12, "CAF_ENDIAN");
    }

    private static byte[] write(AudioInteropWriter.Format format, File pcm) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long size = AudioInteropWriter.write(format, pcm, out, 48000, 1);
        byte[] bytes = out.toByteArray();
        check(size == bytes.length, format.name() + "_RETURN_SIZE");
        return bytes;
    }

    private static void checkAscii(byte[] bytes, int offset, String value, String label) {
        byte[] expected = value.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < expected.length; ++i) {
            check(bytes[offset + i] == expected[i], label + "_" + i);
        }
    }

    private static long u32be(byte[] bytes, int offset) {
        return ((long)(bytes[offset] & 0xff) << 24) |
                ((long)(bytes[offset + 1] & 0xff) << 16) |
                ((long)(bytes[offset + 2] & 0xff) << 8) |
                (long)(bytes[offset + 3] & 0xff);
    }

    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
