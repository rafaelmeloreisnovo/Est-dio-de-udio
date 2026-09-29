/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */
package io.rafaelia.audiostudio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

final class AudioInteropWriter {
    private AudioInteropWriter() {}

    static long writeRaw(File pcm, OutputStream out) throws IOException {
        return copy(pcm, out);
    }

    static long writeWav16(
            File pcm, OutputStream out, int sampleRate, int channels) throws IOException {
        if (pcm == null || !pcm.isFile()) throw new IOException("PCM source missing");
        if (out == null) throw new IOException("output stream missing");
        if (sampleRate <= 0) throw new IOException("invalid sample rate");
        if (channels <= 0 || channels > 2) throw new IOException("invalid channel count");

        long dataBytes = pcm.length();
        if (dataBytes > 0xffffffffL - 36L) {
            throw new IOException("WAV RIFF v1 exceeds 32-bit chunk size");
        }
        int blockAlign = channels * 2;
        long byteRate = (long) sampleRate * blockAlign;

        writeAscii(out, "RIFF");
        writeU32Le(out, 36L + dataBytes);
        writeAscii(out, "WAVE");

        writeAscii(out, "fmt ");
        writeU32Le(out, 16L);
        writeU16Le(out, 1);
        writeU16Le(out, channels);
        writeU32Le(out, sampleRate);
        writeU32Le(out, byteRate);
        writeU16Le(out, blockAlign);
        writeU16Le(out, 16);

        writeAscii(out, "data");
        writeU32Le(out, dataBytes);
        copy(pcm, out);
        out.flush();
        return dataBytes + 44L;
    }

    private static long copy(File source, OutputStream out) throws IOException {
        if (source == null || !source.isFile()) throw new IOException("source missing");
        if (out == null) throw new IOException("output stream missing");
        byte[] buffer = new byte[16384];
        long total = 0L;
        try (FileInputStream in = new FileInputStream(source)) {
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
                total += n;
            }
        }
        out.flush();
        return total;
    }

    private static void writeAscii(OutputStream out, String value) throws IOException {
        for (int i = 0; i < value.length(); ++i) out.write((byte) value.charAt(i));
    }

    private static void writeU16Le(OutputStream out, int value) throws IOException {
        out.write(value & 0xff);
        out.write((value >>> 8) & 0xff);
    }

    private static void writeU32Le(OutputStream out, long value) throws IOException {
        out.write((int)(value & 0xffL));
        out.write((int)((value >>> 8) & 0xffL));
        out.write((int)((value >>> 16) & 0xffL));
        out.write((int)((value >>> 24) & 0xffL));
    }
}
