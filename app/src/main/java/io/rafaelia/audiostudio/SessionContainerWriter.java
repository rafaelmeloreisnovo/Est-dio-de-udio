/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 *
 * Platform I/O edge only. Binary header semantics are produced by the C core.
 */
package io.rafaelia.audiostudio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

final class SessionContainerWriter {
    private SessionContainerWriter() {}

    static File wrapRawPcmAsZrf(
            File rawPcm, File target, int sampleRate, int channels) throws IOException {
        if (rawPcm == null || !rawPcm.exists()) {
            throw new IOException("raw PCM source missing");
        }
        if (channels <= 0) throw new IOException("invalid channel count");

        long pcmBytes = rawPcm.length();
        long payloadBytes = 16L + pcmBytes;
        if (pcmBytes > 0xfffffff0L || payloadBytes > 0xffffffffL) {
            throw new IOException("ZRF v1 payload exceeds 32-bit bounded container");
        }

        long frameBytes = 2L * channels;
        long frames = frameBytes == 0L ? 0L : pcmBytes / frameBytes;

        byte[] header = NativeDsp.nativeContainerHeader(
                NativeDsp.CONTAINER_ZRF,
                channels,
                0,
                sampleRate,
                0,
                0,
                1,
                payloadBytes,
                512);
        byte[] chunk = NativeDsp.nativeChunkHeader(
                NativeDsp.CHUNK_PCM,
                0L,
                pcmBytes,
                frames);

        if (header == null || chunk == null) {
            throw new IOException("native ZRF header gate rejected descriptor");
        }

        byte[] buffer = new byte[4096];
        try (FileInputStream in = new FileInputStream(rawPcm);
             FileOutputStream out = new FileOutputStream(target, false)) {
            out.write(header);
            out.write(chunk);
            int n;
            while ((n = in.read(buffer)) >= 0) {
                if (n != 0) out.write(buffer, 0, n);
            }
            out.flush();
        }
        return target;
    }
}
