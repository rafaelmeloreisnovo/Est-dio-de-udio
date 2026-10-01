/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 *
 * Android hardware-I/O edge only. Sweep generation and correlation live in
 * freestanding C cores. Absolute SPL is intentionally not claimed here.
 */
package io.rafaelia.audiostudio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import java.io.File;
import java.io.IOException;

final class RelativeCalibrationEngine {
    static final int SAMPLE_RATE = 48000;
    static final int CHANNELS = 1;
    static final int BLOCK = 512;
    static final int SYNC_FRAMES = 1024;
    static final int GUARD_FRAMES = 1024;
    static final int SWEEP_FRAMES = 96000;
    static final int PRE_FRAMES = 4096;
    static final int POST_FRAMES = 8192;
    static final int MAX_LAG = 16384;
    static final int DECAY_NOISE_TAIL_FRAMES = 2048;

    static final long SWEEP_START_STEP_Q32 = 1_789_570L;   // 20 Hz @ 48 kHz
    static final long SWEEP_RATIO_Q31 = 2_147_638_177L;    // ~20 Hz -> 20 kHz / 2 s
    static final int EXCITATION_GAIN_Q15 = 8192;
    static final long SYNC_SEED = 0x51f15e5dL;

    private RelativeCalibrationEngine() {}

    static final class Result {
        final File cfrFile;
        final int capturedFrames;
        final int bestLag;
        final long correlation;
        final long referenceEnergy;
        final long responseEnergy;
        final String inputSource;
        final long[] sweepProfile;
        final long[] decayProfile;

        Result(
                File cfrFile,
                int capturedFrames,
                int bestLag,
                long correlation,
                long referenceEnergy,
                long responseEnergy,
                String inputSource,
                long[] sweepProfile,
                long[] decayProfile) {
            this.cfrFile = cfrFile;
            this.capturedFrames = capturedFrames;
            this.bestLag = bestLag;
            this.correlation = correlation;
            this.referenceEnergy = referenceEnergy;
            this.responseEnergy = responseEnergy;
            this.inputSource = inputSource;
            this.sweepProfile = sweepProfile;
            this.decayProfile = decayProfile;
        }

        long latencyMicros() {
            return ((long) bestLag * 1_000_000L) / SAMPLE_RATE;
        }

        int validTransferBands() {
            if (sweepProfile == null ||
                    sweepProfile.length < NativeDsp.SWEEP_PROFILE_OUTPUT_LONGS) return 0;
            return (int) sweepProfile[NativeDsp.SWEEP_PROFILE_BANDS * 3];
        }

        long[] powerRatiosQ20() {
            long[] ratios = new long[NativeDsp.SWEEP_PROFILE_BANDS];
            if (sweepProfile == null) return ratios;
            int base = NativeDsp.SWEEP_PROFILE_BANDS * 2;
            for (int i = 0; i < ratios.length; ++i) {
                if (base + i < sweepProfile.length) ratios[i] = sweepProfile[base + i];
            }
            return ratios;
        }

        long decayFlags() {
            return decayProfile != null && decayProfile.length >= 13 ? decayProfile[12] : 0L;
        }

        int preferredRt60Frames() {
            if (decayProfile == null || decayProfile.length < 13) return -1;
            long flags = decayFlags();
            if ((flags & NativeDsp.DECAY_FLAG_T30) != 0L) return (int) decayProfile[11];
            if ((flags & NativeDsp.DECAY_FLAG_T20) != 0L) return (int) decayProfile[10];
            if ((flags & NativeDsp.DECAY_FLAG_EDT) != 0L) return (int) decayProfile[9];
            return -1;
        }

        long preferredRt60Millis() {
            int frames = preferredRt60Frames();
            return frames < 0 ? -1L : ((long) frames * 1000L) / SAMPLE_RATE;
        }

        String decayMethod() {
            long flags = decayFlags();
            if ((flags & NativeDsp.DECAY_FLAG_T30) != 0L) return "T30_RELATIVE";
            if ((flags & NativeDsp.DECAY_FLAG_T20) != 0L) return "T20_RELATIVE";
            if ((flags & NativeDsp.DECAY_FLAG_EDT) != 0L) return "EDT_RELATIVE";
            return "NOT_ENOUGH_DYNAMIC_RANGE";
        }
    }

    static Result run(Context context, File cfrTarget) throws Exception {
        int excitationFrames = SYNC_FRAMES + GUARD_FRAMES + SWEEP_FRAMES;
        int captureCapacity =
                PRE_FRAMES + excitationFrames + POST_FRAMES + MAX_LAG;

        short[] sync = new short[SYNC_FRAMES];
        short[] sweep = new short[SWEEP_FRAMES];
        short[] excitation = new short[excitationFrames];
        short[] response = new short[captureCapacity];

        if (NativeDsp.nativeSyncSequence(
                sync, SYNC_FRAMES, SYNC_SEED, EXCITATION_GAIN_Q15) != SYNC_FRAMES) {
            throw new IOException("sync generator gate failed");
        }

        NativeDsp.nativeSweepReset(
                SWEEP_START_STEP_Q32,
                SWEEP_RATIO_Q31,
                EXCITATION_GAIN_Q15,
                SWEEP_FRAMES);
        if (NativeDsp.nativeSweepRender(
                sweep, SWEEP_FRAMES, CHANNELS) != SWEEP_FRAMES) {
            throw new IOException("sweep generator gate failed");
        }

        int cursor = 0;
        for (int i = 0; i < SYNC_FRAMES; ++i) excitation[cursor++] = sync[i];
        for (int i = 0; i < GUARD_FRAMES; ++i) excitation[cursor++] = 0;
        for (int i = 0; i < SWEEP_FRAMES; ++i) excitation[cursor++] = sweep[i];

        InputHandle input = createInput();
        AudioTrack output = createOutput();
        int[] playbackError = new int[]{0};
        int captured = 0;

        try {
            input.record.startRecording();
            if (input.record.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                throw new IOException("AudioRecord did not enter RECORDSTATE_RECORDING");
            }

            Thread playbackThread = new Thread(() -> {
                try {
                    output.play();
                    output.setVolume(0.25f);
                    writeSilence(output, PRE_FRAMES);
                    writeAll(output, excitation, excitation.length);
                    writeSilence(output, POST_FRAMES);
                } catch (Throwable t) {
                    playbackError[0] = -1;
                } finally {
                    try {
                        output.stop();
                    } catch (Throwable ignored) {
                    }
                }
            }, "rafaelia-cal-playback");

            playbackThread.start();

            int idleReads = 0;
            while (captured < response.length) {
                int remaining = response.length - captured;
                int request = remaining > 4096 ? 4096 : remaining;
                int n = input.record.read(
                        response, captured, request, AudioRecord.READ_BLOCKING);
                if (n > 0) {
                    captured += n;
                    idleReads = 0;
                } else if (n == 0) {
                    ++idleReads;
                    if (idleReads > 64) {
                        throw new IOException("AudioRecord stalled");
                    }
                } else {
                    throw new IOException("AudioRecord error " + n);
                }
            }

            playbackThread.join(10000L);
            if (playbackThread.isAlive()) {
                playbackThread.interrupt();
                throw new IOException("calibration playback watchdog");
            }
            if (playbackError[0] != 0) {
                throw new IOException("AudioTrack playback failed");
            }
        } finally {
            try {
                input.record.stop();
            } catch (Throwable ignored) {
            }
            input.record.release();
            output.release();
        }

        long[] transfer = new long[5];
        int searchMax = PRE_FRAMES + MAX_LAG;
        if (searchMax >= captured) searchMax = captured - 1;
        if (searchMax < 0) {
            throw new IOException("calibration capture empty");
        }

        int transferOk = NativeDsp.nativeRelativeTransfer(
                sync,
                sync.length,
                response,
                captured,
                0,
                searchMax,
                transfer);
        if (transferOk == 0 || transfer[4] == 0L) {
            throw new IOException("relative transfer could not locate sync");
        }

        int bestLag = (int) transfer[3];
        int referenceSweepOffset = SYNC_FRAMES + GUARD_FRAMES;
        int responseSweepOffset = bestLag + SYNC_FRAMES + GUARD_FRAMES;
        long[] sweepProfile = new long[NativeDsp.SWEEP_PROFILE_OUTPUT_LONGS];
        int sweepProfileOk = NativeDsp.nativeSweepBandProfile(
                excitation, excitation.length, referenceSweepOffset,
                response, captured, responseSweepOffset,
                SWEEP_FRAMES, sweepProfile);
        if (sweepProfileOk == 0) {
            throw new IOException("relative sweep profile failed after sync alignment");
        }

        long[] decayProfile = new long[NativeDsp.DECAY_PROFILE_OUTPUT_LONGS];
        int responseTailOffset = responseSweepOffset + SWEEP_FRAMES;
        int responseTailFrames = captured - responseTailOffset;
        if (responseTailFrames <= 0) {
            throw new IOException("relative decay tail unavailable");
        }
        int decayOk = NativeDsp.nativeDecayProfile(
                response, responseTailOffset, responseTailFrames,
                DECAY_NOISE_TAIL_FRAMES, decayProfile);
        if (decayOk == 0) {
            // The capture remains valid even when the tail has insufficient
            // dynamic range. Keep an explicit empty decay profile in the CFR.
            for (int i = 0; i < decayProfile.length; ++i) decayProfile[i] = 0L;
            decayProfile[3] = responseTailFrames;
            decayProfile[4] = Math.min(DECAY_NOISE_TAIL_FRAMES, responseTailFrames);
            for (int i = 5; i <= 11; ++i) decayProfile[i] = -1L;
        }

        CalibrationContainerWriter.writeRelativeCfr(
                cfrTarget,
                excitation,
                excitation.length,
                response,
                captured,
                SAMPLE_RATE,
                SWEEP_START_STEP_Q32,
                SWEEP_RATIO_Q31,
                EXCITATION_GAIN_Q15,
                SYNC_FRAMES,
                PRE_FRAMES,
                POST_FRAMES,
                MAX_LAG,
                transfer,
                sweepProfile,
                decayProfile);

        return new Result(
                cfrTarget,
                captured,
                bestLag,
                transfer[0],
                transfer[1],
                transfer[2],
                input.sourceName,
                sweepProfile,
                decayProfile);
    }

    private static final class InputHandle {
        final AudioRecord record;
        final String sourceName;

        InputHandle(AudioRecord record, String sourceName) {
            this.record = record;
            this.sourceName = sourceName;
        }
    }

    private static InputHandle createInput() throws IOException {
        int minBytes = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (minBytes < 0) {
            throw new IOException("AudioRecord minimum buffer unavailable: " + minBytes);
        }
        int bufferBytes = minBytes;
        if (bufferBytes < 16384) bufferBytes = 16384;

        AudioRecord record = tryBuildInput(
                MediaRecorder.AudioSource.UNPROCESSED, bufferBytes);
        if (record != null) {
            return new InputHandle(record, "UNPROCESSED");
        }

        record = tryBuildInput(MediaRecorder.AudioSource.MIC, bufferBytes);
        if (record != null) {
            return new InputHandle(record, "MIC_FALLBACK");
        }

        throw new IOException("no initialized 48 kHz mono input");
    }

    private static AudioRecord tryBuildInput(int source, int bufferBytes) {
        AudioRecord record = null;
        try {
            AudioFormat format = new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build();
            record = new AudioRecord.Builder()
                    .setAudioSource(source)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferBytes)
                    .build();
            if (record.getState() == AudioRecord.STATE_INITIALIZED) {
                return record;
            }
        } catch (Throwable ignored) {
        }
        if (record != null) {
            record.release();
        }
        return null;
    }

    private static AudioTrack createOutput() throws IOException {
        int minBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (minBytes < 0) {
            throw new IOException("AudioTrack minimum buffer unavailable: " + minBytes);
        }
        int bufferBytes = minBytes;
        if (bufferBytes < 16384) bufferBytes = 16384;

        AudioFormat format = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build();
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferBytes)
                .build();

        if (track.getState() != AudioTrack.STATE_INITIALIZED) {
            track.release();
            throw new IOException("AudioTrack did not initialize");
        }
        return track;
    }

    private static void writeSilence(AudioTrack track, int frames) throws IOException {
        short[] zeros = new short[BLOCK];
        int remaining = frames;
        while (remaining > 0) {
            int batch = remaining > zeros.length ? zeros.length : remaining;
            writeAll(track, zeros, batch);
            remaining -= batch;
        }
    }

    private static void writeAll(
            AudioTrack track, short[] data, int count) throws IOException {
        int offset = 0;
        int idleWrites = 0;
        while (offset < count) {
            int remaining = count - offset;
            int request = remaining > 4096 ? 4096 : remaining;
            int n = track.write(
                    data, offset, request, AudioTrack.WRITE_BLOCKING);
            if (n > 0) {
                offset += n;
                idleWrites = 0;
            } else if (n == 0) {
                ++idleWrites;
                if (idleWrites > 64) {
                    throw new IOException("AudioTrack stalled");
                }
            } else {
                throw new IOException("AudioTrack error " + n);
            }
        }
    }
}
