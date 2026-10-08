/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

package io.rafaelia.audiostudio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

final class AudioRecorderEngine {
    static final class CaptureStats {
        final int peak;
        final int rms;
        final long samples;
        final long clipped;
        final String source;

        CaptureStats(int peak, int rms, long samples, long clipped, String source) {
            this.peak = peak;
            this.rms = rms;
            this.samples = samples;
            this.clipped = clipped;
            this.source = source;
        }
    }

    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 1;

    private final Context context;
    private final File output;
    private volatile boolean running;
    private volatile boolean captureFailed;
    private AudioRecord recorder;
    private Thread worker;

    private AutomaticGainControl agc;
    private NoiseSuppressor ns;
    private AcousticEchoCanceler aec;

    private volatile int peak;
    private volatile long energyScaled;
    private volatile long sampleCount;
    private volatile long clipped;
    private final short[] latest = new short[512];
    private volatile int latestCount;
    private String sourceName = "NOT_STARTED";

    AudioRecorderEngine(Context context, File output) {
        this.context = context.getApplicationContext();
        this.output = output;
    }

    int getSampleRate() {
        return SAMPLE_RATE;
    }

    int getChannels() {
        return CHANNELS;
    }

    CaptureStats getStats() {
        long count = sampleCount;
        long meanScaled = count == 0 ? 0 : energyScaled / count;
        long rmsSquare = meanScaled << 8;
        int rms = (int) isqrt(rmsSquare);
        return new CaptureStats(peak, rms, count, clipped, sourceName);
    }

    int copyLatest(short[] out) {
        if (out == null) return 0;
        int count = latestCount;
        if (count > out.length) count = out.length;
        for (int i = 0; i < count; ++i) out[i] = latest[i];
        return count;
    }

    void start() throws IOException {
        if (running) return;

        AudioManager audioManager =
                (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        boolean rawSupported = audioManager != null &&
                "true".equals(audioManager.getProperty(
                        AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED));

        int source = rawSupported
                ? MediaRecorder.AudioSource.UNPROCESSED
                : MediaRecorder.AudioSource.VOICE_RECOGNITION;
        sourceName = rawSupported ? "UNPROCESSED" : "VOICE_RECOGNITION";

        int min = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) {
            throw new IOException("AudioRecord buffer indisponivel: " + min);
        }

        int bufferBytes = Math.max(min * 2, 8192);
        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build();

        try {
            recorder = new AudioRecord.Builder()
                    .setAudioSource(source)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferBytes)
                    .build();
        } catch (UnsupportedOperationException e) {
            throw new IOException("Configuração de captura não suportada", e);
        }

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            recorder.release();
            recorder = null;
            throw new IOException("AudioRecord nao inicializado");
        }

        disablePlatformPreprocessors(recorder.getAudioSessionId());

        peak = 0;
        energyScaled = 0;
        sampleCount = 0;
        clipped = 0;
        latestCount = 0;
        captureFailed = false;
        running = true;
        recorder.startRecording();

        worker = new Thread(() -> captureLoop(bufferBytes), "rafaelia-capture");
        worker.start();
    }

    private void disablePlatformPreprocessors(int sessionId) {
        try {
            if (AutomaticGainControl.isAvailable()) {
                agc = AutomaticGainControl.create(sessionId);
                if (agc != null) agc.setEnabled(false);
            }
        } catch (RuntimeException ignored) {
            agc = null;
        }

        try {
            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(sessionId);
                if (ns != null) ns.setEnabled(false);
            }
        } catch (RuntimeException ignored) {
            ns = null;
        }

        try {
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(sessionId);
                if (aec != null) aec.setEnabled(false);
            }
        } catch (RuntimeException ignored) {
            aec = null;
        }
    }

    private void captureLoop(int bufferBytes) {
        short[] samples = new short[bufferBytes / 2];
        byte[] bytes = new byte[bufferBytes];

        try (FileOutputStream out = new FileOutputStream(output, false)) {
            while (running) {
                int n = recorder.read(samples, 0, samples.length,
                        AudioRecord.READ_BLOCKING);
                if (n < 0) {
                    captureFailed = true;
                    running = false;
                    break;
                }
                if (n == 0) continue;

                int localPeak = peak;
                long localEnergy = energyScaled;
                long localClipped = clipped;

                for (int i = 0, j = 0; i < n; i++) {
                    short s = samples[i];
                    int a = s == Short.MIN_VALUE ? 32768 : Math.abs((int) s);
                    if (a > localPeak) localPeak = a;
                    if (a >= 32767) ++localClipped;
                    localEnergy += ((long) s * (long) s) >> 8;

                    bytes[j++] = (byte) (s & 0xff);
                    bytes[j++] = (byte) ((s >>> 8) & 0xff);
                }

                peak = localPeak;
                energyScaled = localEnergy;
                clipped = localClipped;
                sampleCount += n;

                int copy = n < latest.length ? n : latest.length;
                int start = n - copy;
                for (int i = 0; i < copy; ++i) latest[i] = samples[start + i];
                latestCount = copy;

                out.write(bytes, 0, n * 2);
            }
            out.flush();
        } catch (IOException | RuntimeException error) {
            captureFailed = true;
            running = false;
        }
    }

    /**
     * True only when the capture worker is closed and its PCM output was flushed.
     * A timed-out worker may still own the file and AudioRecord native buffers:
     * do not publish its bytes or release those resources prematurely.
     */
    boolean stop() {
        running = false;
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (IllegalStateException ignored) {
                captureFailed = true;
            }
        }
        final Thread captureThread = worker;
        if (captureThread != null) {
            try {
                captureThread.join(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                captureFailed = true;
            }
            if (captureThread.isAlive()) {
                Thread cleanup = new Thread(() -> {
                    try {
                        captureThread.join();
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    // Failure to drain remains a strict NO for the caller.
                    // Keep platform resources until no native capture can use them.
                    if (!captureThread.isAlive()) releaseOwnedRecorder();
                }, "rafaelia-capture-drain");
                cleanup.setDaemon(true);
                cleanup.start();
                return false;
            }
        }
        releaseOwnedRecorder();
        return !captureFailed;
    }

    private void releaseOwnedRecorder() {
        releaseEffects();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
        worker = null;
    }

    private void releaseEffects() {
        if (agc != null) {
            agc.release();
            agc = null;
        }
        if (ns != null) {
            ns.release();
            ns = null;
        }
        if (aec != null) {
            aec.release();
            aec = null;
        }
    }

    private static long isqrt(long x) {
        if (x <= 0) return 0;
        long result = 0;
        long bit = 1L << 62;
        while (bit > x) bit >>>= 2;
        while (bit != 0) {
            if (x >= result + bit) {
                x -= result + bit;
                result = (result >>> 1) + bit;
            } else {
                result >>>= 1;
            }
            bit >>>= 2;
        }
        return result;
    }
}
