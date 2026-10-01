/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 *
 * Platform file-I/O edge. Container framing is produced by the C core.
 */
package io.rafaelia.audiostudio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

final class CalibrationContainerWriter {
    private CalibrationContainerWriter() {}

    static File writeRelativeCfr(
            File target,
            short[] reference,
            int referenceFrames,
            short[] response,
            int responseFrames,
            int sampleRate,
            long startStepQ32,
            long ratioQ31,
            int gainQ15,
            int syncFrames,
            int preFrames,
            int postFrames,
            int maxLag,
            long[] transfer,
            long[] sweepProfile,
            long[] decayProfile) throws IOException {
        if (target == null || reference == null || response == null || transfer == null ||
                sweepProfile == null || decayProfile == null) {
            throw new IOException("CFR argument missing");
        }
        if (referenceFrames < 0 || referenceFrames > reference.length) {
            throw new IOException("invalid CFR reference length");
        }
        if (responseFrames < 0 || responseFrames > response.length) {
            throw new IOException("invalid CFR response length");
        }
        if (transfer.length < 5) {
            throw new IOException("relative-transfer result incomplete");
        }
        if (sweepProfile.length < NativeDsp.SWEEP_PROFILE_OUTPUT_LONGS) {
            throw new IOException("sweep profile incomplete");
        }
        if (decayProfile.length < NativeDsp.DECAY_PROFILE_OUTPUT_LONGS) {
            throw new IOException("decay profile incomplete");
        }

        final int waveBytes = 32;
        final int calBytes = 48;
        final int specBytes = 344;
        final int roomBytes = 72;
        long refBytes = (long) referenceFrames * 2L;
        long respBytes = (long) responseFrames * 2L;
        long payloadBytes =
                16L + waveBytes +
                16L + calBytes +
                16L + refBytes +
                16L + respBytes +
                16L + specBytes +
                16L + roomBytes;

        if (payloadBytes > 0xffffffffL) {
            throw new IOException("CFR v1 exceeds bounded 32-bit payload");
        }

        byte[] header = NativeDsp.nativeContainerHeader(
                NativeDsp.CONTAINER_CFR,
                1,
                0,
                sampleRate,
                1,
                NativeDsp.CFR_FLAG_RELATIVE,
                6,
                payloadBytes,
                512);
        byte[] waveHeader = NativeDsp.nativeChunkHeader(
                NativeDsp.CHUNK_WAVE, 0L, waveBytes, 1L);
        byte[] calHeader = NativeDsp.nativeChunkHeader(
                NativeDsp.CHUNK_CAL, NativeDsp.CFR_FLAG_RELATIVE, calBytes, 1L);
        byte[] refHeader = NativeDsp.nativeChunkHeader(
                NativeDsp.CHUNK_PCM, NativeDsp.CHUNK_FLAG_REFERENCE,
                refBytes, referenceFrames);
        byte[] respHeader = NativeDsp.nativeChunkHeader(
                NativeDsp.CHUNK_PCM, NativeDsp.CHUNK_FLAG_RESPONSE,
                respBytes, responseFrames);
        byte[] specHeader = NativeDsp.nativeChunkHeader(
                NativeDsp.CHUNK_SPEC, NativeDsp.CFR_FLAG_RELATIVE,
                specBytes, NativeDsp.SWEEP_PROFILE_BANDS);
        byte[] roomHeader = NativeDsp.nativeChunkHeader(
                NativeDsp.CHUNK_ROOM, NativeDsp.CFR_FLAG_RELATIVE,
                roomBytes, 1L);

        if (header == null || waveHeader == null || calHeader == null ||
                refHeader == null || respHeader == null ||
                specHeader == null || roomHeader == null) {
            throw new IOException("native CFR framing rejected descriptor");
        }

        try (FileOutputStream out = new FileOutputStream(target, false)) {
            out.write(header);

            out.write(waveHeader);
            writeU32(out, startStepQ32);
            writeU32(out, ratioQ31);
            writeI32(out, gainQ15);
            writeU32(out, referenceFrames);
            writeU32(out, preFrames);
            writeU32(out, postFrames);
            writeU32(out, maxLag);
            writeU32(out, syncFrames);

            out.write(calHeader);
            writeU32(out, 1L);
            writeU32(out, NativeDsp.CFR_FLAG_RELATIVE);
            writeI32(out, (int) transfer[3]);
            writeU32(out, responseFrames);
            writeI64(out, transfer[0]);
            writeU64(out, transfer[1]);
            writeU64(out, transfer[2]);
            writeU32(out, sampleRate);
            writeU32(out, 1L);

            out.write(refHeader);
            writePcm16Le(out, reference, referenceFrames);

            out.write(respHeader);
            writePcm16Le(out, response, responseFrames);

            out.write(specHeader);
            writeU32(out, 1L);
            writeU32(out, 1L); // equal-time windows over exponential sweep
            writeU32(out, NativeDsp.SWEEP_PROFILE_BANDS);
            writeU32(out, sweepProfile[48]);
            writeU32(out, sweepProfile[49]);
            writeU32(out, 20L); // power_ratio_q20 fractional bits
            for (int i = 0; i < NativeDsp.SWEEP_PROFILE_BANDS; ++i) {
                writeU64(out, sweepProfile[i]);
                writeU64(out, sweepProfile[NativeDsp.SWEEP_PROFILE_BANDS + i]);
                writeU32(out, sweepProfile[NativeDsp.SWEEP_PROFILE_BANDS * 2 + i]);
            }

            out.write(roomHeader);
            writeU32(out, 1L);
            writeU32(out, 1L); // backward-integrated relative decay
            writeU32(out, decayProfile[3]);
            writeU32(out, decayProfile[4]);
            writeU64(out, decayProfile[0]);
            writeU64(out, decayProfile[1]);
            writeU64(out, decayProfile[2]);
            for (int i = 5; i <= 11; ++i) {
                writeI32(out, (int) decayProfile[i]);
            }
            writeU32(out, decayProfile[12]);
            out.flush();
        }

        return target;
    }

    private static void writePcm16Le(
            FileOutputStream out, short[] samples, int count) throws IOException {
        byte[] buffer = new byte[4096];
        int offset = 0;
        while (offset < count) {
            int batch = count - offset;
            if (batch > 2048) batch = 2048;
            int p = 0;
            for (int i = 0; i < batch; ++i) {
                int value = samples[offset + i];
                buffer[p++] = (byte) (value & 0xff);
                buffer[p++] = (byte) ((value >>> 8) & 0xff);
            }
            out.write(buffer, 0, p);
            offset += batch;
        }
    }

    private static void writeI32(FileOutputStream out, int value) throws IOException {
        writeU32(out, value & 0xffffffffL);
    }

    private static void writeU32(FileOutputStream out, long value) throws IOException {
        out.write((int) (value & 0xffL));
        out.write((int) ((value >>> 8) & 0xffL));
        out.write((int) ((value >>> 16) & 0xffL));
        out.write((int) ((value >>> 24) & 0xffL));
    }

    private static void writeI64(FileOutputStream out, long value) throws IOException {
        writeU64(out, value);
    }

    private static void writeU64(FileOutputStream out, long value) throws IOException {
        for (int shift = 0; shift < 64; shift += 8) {
            out.write((int) ((value >>> shift) & 0xffL));
        }
    }
}
