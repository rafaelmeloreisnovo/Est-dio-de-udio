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
    enum Format {
        RAW_PCM("RAW PCM16", ".pcm", "application/octet-stream", false),
        WAV_PCM16("WAV PCM16", ".wav", "audio/wav", false),
        AIFF_PCM16("AIFF PCM16", ".aiff", "audio/aiff", true),
        AU_PCM16("AU/SND PCM16", ".au", "audio/basic", true),
        CAF_PCM16("CAF LPCM16", ".caf", "audio/x-caf", false);

        final String label;
        final String extension;
        final String mimeType;
        final boolean bigEndianPayload;

        Format(String label, String extension, String mimeType, boolean bigEndianPayload) {
            this.label = label;
            this.extension = extension;
            this.mimeType = mimeType;
            this.bigEndianPayload = bigEndianPayload;
        }
    }

    private AudioInteropWriter() {}

    static long write(
            Format format,
            File pcm,
            OutputStream out,
            int sampleRate,
            int channels) throws IOException {
        if (format == null) throw new IOException("format missing");
        switch (format) {
            case RAW_PCM:
                return writeRaw(pcm, out);
            case WAV_PCM16:
                return writeWav16(pcm, out, sampleRate, channels);
            case AIFF_PCM16:
                return writeAiff16(pcm, out, sampleRate, channels);
            case AU_PCM16:
                return writeAu16(pcm, out, sampleRate, channels);
            case CAF_PCM16:
                return writeCaf16(pcm, out, sampleRate, channels);
            default:
                throw new IOException("unsupported format");
        }
    }

    static long writeRaw(File pcm, OutputStream out) throws IOException {
        return copy(pcm, out, false);
    }

    static long writeWav16(
            File pcm, OutputStream out, int sampleRate, int channels) throws IOException {
        long dataBytes = validatePcm16(pcm, out, sampleRate, channels);
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
        copy(pcm, out, false);
        out.flush();
        return dataBytes + 44L;
    }

    static long writeAiff16(
            File pcm, OutputStream out, int sampleRate, int channels) throws IOException {
        long dataBytes = validatePcm16(pcm, out, sampleRate, channels);
        long frameBytes = (long) channels * 2L;
        long frames = dataBytes / frameBytes;
        if (frames > 0xffffffffL || dataBytes > 0xffffffffL - 46L) {
            throw new IOException("AIFF exceeds 32-bit FORM/frame size");
        }

        writeAscii(out, "FORM");
        writeU32Be(out, 46L + dataBytes);
        writeAscii(out, "AIFF");

        writeAscii(out, "COMM");
        writeU32Be(out, 18L);
        writeU16Be(out, channels);
        writeU32Be(out, frames);
        writeU16Be(out, 16);
        writeExtended80PositiveInteger(out, sampleRate);

        writeAscii(out, "SSND");
        writeU32Be(out, 8L + dataBytes);
        writeU32Be(out, 0L);
        writeU32Be(out, 0L);
        copy(pcm, out, true);
        out.flush();
        return dataBytes + 54L;
    }

    static long writeAu16(
            File pcm, OutputStream out, int sampleRate, int channels) throws IOException {
        long dataBytes = validatePcm16(pcm, out, sampleRate, channels);
        if (dataBytes > 0xffffffffL) {
            throw new IOException("AU exceeds 32-bit data size");
        }

        writeAscii(out, ".snd");
        writeU32Be(out, 28L);
        writeU32Be(out, dataBytes);
        writeU32Be(out, 3L);
        writeU32Be(out, sampleRate);
        writeU32Be(out, channels);
        writeU32Be(out, 0L);
        copy(pcm, out, true);
        out.flush();
        return dataBytes + 28L;
    }

    static long writeCaf16(
            File pcm, OutputStream out, int sampleRate, int channels) throws IOException {
        long dataBytes = validatePcm16(pcm, out, sampleRate, channels);
        if (dataBytes > Long.MAX_VALUE - 4L) {
            throw new IOException("CAF data size overflow");
        }
        int bytesPerFrame = channels * 2;

        writeAscii(out, "caff");
        writeU16Be(out, 1);
        writeU16Be(out, 0);

        writeAscii(out, "desc");
        writeU64Be(out, 32L);
        writeU64Be(out, Double.doubleToLongBits((double) sampleRate));
        writeAscii(out, "lpcm");
        writeU32Be(out, 2L);
        writeU32Be(out, bytesPerFrame);
        writeU32Be(out, 1L);
        writeU32Be(out, channels);
        writeU32Be(out, 16L);

        writeAscii(out, "data");
        writeU64Be(out, dataBytes + 4L);
        writeU32Be(out, 0L);
        copy(pcm, out, false);
        out.flush();
        return dataBytes + 68L;
    }

    private static long validatePcm16(
            File pcm, OutputStream out, int sampleRate, int channels) throws IOException {
        if (pcm == null || !pcm.isFile()) throw new IOException("PCM source missing");
        if (out == null) throw new IOException("output stream missing");
        if (sampleRate <= 0) throw new IOException("invalid sample rate");
        if (channels <= 0 || channels > 2) throw new IOException("invalid channel count");
        long dataBytes = pcm.length();
        long frameBytes = (long) channels * 2L;
        if ((dataBytes & 1L) != 0L || dataBytes % frameBytes != 0L) {
            throw new IOException("PCM16 source is not frame aligned");
        }
        return dataBytes;
    }

    private static long copy(File source, OutputStream out, boolean swap16) throws IOException {
        if (source == null || !source.isFile()) throw new IOException("source missing");
        if (out == null) throw new IOException("output stream missing");
        byte[] buffer = new byte[16384];
        byte[] converted = swap16 ? new byte[16384] : null;
        long total = 0L;
        int pending = -1;
        try (FileInputStream in = new FileInputStream(source)) {
            int n;
            while ((n = in.read(buffer)) > 0) {
                total += n;
                if (!swap16) {
                    out.write(buffer, 0, n);
                    continue;
                }

                int i = 0;
                int j = 0;
                if (pending >= 0) {
                    converted[j++] = buffer[0];
                    converted[j++] = (byte) pending;
                    pending = -1;
                    i = 1;
                }
                while (i + 1 < n) {
                    converted[j++] = buffer[i + 1];
                    converted[j++] = buffer[i];
                    i += 2;
                }
                if (i < n) pending = buffer[i] & 0xff;
                if (j > 0) out.write(converted, 0, j);
            }
        }
        if (pending >= 0) throw new IOException("PCM16 source ended on partial sample");
        out.flush();
        return total;
    }

    private static void writeExtended80PositiveInteger(OutputStream out, int value)
            throws IOException {
        if (value <= 0) throw new IOException("invalid extended80 value");
        int exponent = 31 - Integer.numberOfLeadingZeros(value);
        int biased = exponent + 16383;
        long mantissa = ((long) value) << (63 - exponent);
        writeU16Be(out, biased);
        writeU64Be(out, mantissa);
    }

    private static void writeAscii(OutputStream out, String value) throws IOException {
        for (int i = 0; i < value.length(); ++i) out.write((byte) value.charAt(i));
    }

    private static void writeU16Le(OutputStream out, int value) throws IOException {
        out.write(value & 0xff);
        out.write((value >>> 8) & 0xff);
    }

    private static void writeU16Be(OutputStream out, int value) throws IOException {
        out.write((value >>> 8) & 0xff);
        out.write(value & 0xff);
    }

    private static void writeU32Le(OutputStream out, long value) throws IOException {
        out.write((int)(value & 0xffL));
        out.write((int)((value >>> 8) & 0xffL));
        out.write((int)((value >>> 16) & 0xffL));
        out.write((int)((value >>> 24) & 0xffL));
    }

    private static void writeU32Be(OutputStream out, long value) throws IOException {
        out.write((int)((value >>> 24) & 0xffL));
        out.write((int)((value >>> 16) & 0xffL));
        out.write((int)((value >>> 8) & 0xffL));
        out.write((int)(value & 0xffL));
    }

    private static void writeU64Be(OutputStream out, long value) throws IOException {
        out.write((int)((value >>> 56) & 0xffL));
        out.write((int)((value >>> 48) & 0xffL));
        out.write((int)((value >>> 40) & 0xffL));
        out.write((int)((value >>> 32) & 0xffL));
        out.write((int)((value >>> 24) & 0xffL));
        out.write((int)((value >>> 16) & 0xffL));
        out.write((int)((value >>> 8) & 0xffL));
        out.write((int)(value & 0xffL));
    }
}
