package io.rafaelia.audiostudio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

final class AudioPipeline {
    static final class DecodedAudio {
        final File pcmFile;
        final int sampleRate;
        final int channels;

        DecodedAudio(File pcmFile, int sampleRate, int channels) {
            this.pcmFile = pcmFile;
            this.sampleRate = sampleRate;
            this.channels = channels;
        }
    }

    static final class MasterResult {
        final File pcmFile;
        final long gainQ30;
        final long relativeGateBlockQ36;
        final long gatedBlocks;
        final long truePeakBeforeQ16;
        final long truePeakAfterQ16;
        final long samplePeakBeforeQ16;
        final long[] spectrum;

        MasterResult(File pcmFile, long gainQ30, long relativeGateBlockQ36,
                     long gatedBlocks, long truePeakBeforeQ16,
                     long truePeakAfterQ16, long samplePeakBeforeQ16,
                     long[] spectrum) {
            this.pcmFile = pcmFile;
            this.gainQ30 = gainQ30;
            this.relativeGateBlockQ36 = relativeGateBlockQ36;
            this.gatedBlocks = gatedBlocks;
            this.truePeakBeforeQ16 = truePeakBeforeQ16;
            this.truePeakAfterQ16 = truePeakAfterQ16;
            this.samplePeakBeforeQ16 = samplePeakBeforeQ16;
            this.spectrum = spectrum;
        }
    }

    private AudioPipeline() {}

    static DecodedAudio decodeToPcm(Context context, Uri uri, File output)
            throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec decoder = null;

        try (ParcelFileDescriptor pfd =
                     context.getContentResolver().openFileDescriptor(uri, "r");
             FileOutputStream pcmOut = new FileOutputStream(output, false)) {

            if (pfd == null) throw new IOException("Nao foi possivel abrir URI");
            extractor.setDataSource(pfd.getFileDescriptor());

            int track = -1;
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat candidate = extractor.getTrackFormat(i);
                String mime = candidate.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    track = i;
                    format = candidate;
                    break;
                }
            }

            if (track < 0 || format == null) {
                throw new IOException("Arquivo sem faixa de audio");
            }

            extractor.selectTrack(track);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime == null) throw new IOException("MIME de audio ausente");

            int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            if (channels < 1 || channels > 2) {
                throw new IOException("Somente mono/estereo: canais=" + channels);
            }

            format.setInteger(MediaFormat.KEY_PCM_ENCODING,
                    AudioFormat.ENCODING_PCM_16BIT);

            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, null, null, 0);
            decoder.start();

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inputDone = false;
            boolean outputDone = false;

            while (!outputDone) {
                if (!inputDone) {
                    int inIndex = decoder.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer in = decoder.getInputBuffer(inIndex);
                        if (in == null) throw new IOException("Buffer decoder null");
                        int size = extractor.readSampleData(in, 0);
                        if (size < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            long pts = extractor.getSampleTime();
                            decoder.queueInputBuffer(inIndex, 0, size, pts, 0);
                            extractor.advance();
                        }
                    }
                }

                int outIndex = decoder.dequeueOutputBuffer(info, 10000);
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat outFormat = decoder.getOutputFormat();
                    sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    channels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    if (outFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        int enc = outFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
                        if (enc != AudioFormat.ENCODING_PCM_16BIT) {
                            throw new IOException("Decoder nao entregou PCM16: " + enc);
                        }
                    }
                } else if (outIndex >= 0) {
                    ByteBuffer out = decoder.getOutputBuffer(outIndex);
                    if (out != null && info.size > 0) {
                        ByteBuffer copy = out.duplicate();
                        copy.position(info.offset);
                        copy.limit(info.offset + info.size);
                        byte[] block = new byte[info.size];
                        copy.get(block);
                        pcmOut.write(block);
                    }

                    outputDone =
                            (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    decoder.releaseOutputBuffer(outIndex, false);
                }
            }

            pcmOut.flush();
            return new DecodedAudio(output, sampleRate, channels);
        } finally {
            if (decoder != null) {
                try {
                    decoder.stop();
                } catch (IllegalStateException ignored) {
                }
                decoder.release();
            }
            extractor.release();
        }
    }

    static MasterResult masterAndNormalize(
            File input, File output, int sampleRate, int channels,
            int preset, long targetEnergyQ36) throws IOException {

        if (sampleRate != 48000) {
            throw new IOException(
                    "Meter/DSP Delta 2 requer 48 kHz; recebido " + sampleRate +
                    ". Resampler de alta qualidade permanece PENDING.");
        }
        if (channels < 1 || channels > 2) {
            throw new IOException("DSP suporta apenas mono/estereo");
        }

        File stage = new File(output.getParentFile(), output.getName() + ".stage");
        try {
            dspAndAbsoluteGatePass(input, stage, channels, preset);

            long relativeGate = NativeDsp.nativeMeterRelativeGate();

            NativeDsp.nativeMeterReset(channels, relativeGate);
            long[] spectrum = new long[16];
            meterPass(stage, channels, spectrum);

            long[] meter = new long[4];
            NativeDsp.nativeMeterResult(meter);
            long gainQ30 = NativeDsp.nativeMeterGain(
                    targetEnergyQ36,
                    NativeDsp.TRUE_PEAK_MINUS_1DB_Q16);

            applyGainPass(stage, output, channels, gainQ30);

            long predictedTruePeak = multiplyQ30(meter[2], gainQ30);
            return new MasterResult(
                    output,
                    gainQ30,
                    relativeGate,
                    meter[1],
                    meter[2],
                    predictedTruePeak,
                    meter[3],
                    spectrum);
        } finally {
            if (stage.exists() && !stage.delete()) {
                stage.deleteOnExit();
            }
        }
    }

    private static void dspAndAbsoluteGatePass(
            File input, File stage, int channels, int preset) throws IOException {
        NativeDsp.nativeReset(preset);
        NativeDsp.nativeMeterReset(channels, 0L);

        transformPcm(input, stage, channels, (samples, count) -> {
            NativeDsp.nativeProcess(samples, count, channels);
            NativeDsp.nativeMeterPush(samples, count, channels);
        });
    }

    private static void meterPass(File input, int channels, long[] spectrum)
            throws IOException {
        byte[] bytes = new byte[channels == 2 ? 8192 : 4096];
        short[] samples = new short[bytes.length / 2];

        try (FileInputStream in = new FileInputStream(input)) {
            int carry = 0;
            int frameBytes = channels * 2;

            while (true) {
                int n = in.read(bytes, carry, bytes.length - carry);
                if (n < 0) break;
                n += carry;
                int aligned = n - (n % frameBytes);

                if (aligned > 0) {
                    int count = bytesToSamples(bytes, aligned, samples);
                    NativeDsp.nativeMeterPush(samples, count, channels);

                    long[] blockSpectrum = new long[16];
                    NativeDsp.nativeSpectrum(
                            samples, count, channels, blockSpectrum);
                    for (int i = 0; i < spectrum.length; i++) {
                        spectrum[i] = saturatingAdd(
                                spectrum[i], blockSpectrum[i]);
                    }
                }

                carry = n - aligned;
                if (carry > 0) {
                    System.arraycopy(bytes, aligned, bytes, 0, carry);
                }
            }

            if (carry != 0) {
                throw new IOException("PCM16/frame desalinhado");
            }
        }
    }

    private static void applyGainPass(
            File input, File output, int channels, long gainQ30) throws IOException {
        transformPcm(input, output, channels, (samples, count) ->
                NativeDsp.nativeApplyGain(samples, count, gainQ30));
    }

    private interface PcmTransform {
        void apply(short[] samples, int count);
    }

    private static void transformPcm(
            File input, File output, int channels, PcmTransform transform)
            throws IOException {

        int frameBytes = channels * 2;
        byte[] bytes = new byte[8192];
        short[] samples = new short[4096];

        try (FileInputStream in = new FileInputStream(input);
             FileOutputStream out = new FileOutputStream(output, false)) {

            int carry = 0;
            while (true) {
                int n = in.read(bytes, carry, bytes.length - carry);
                if (n < 0) break;
                n += carry;
                int aligned = n - (n % frameBytes);

                if (aligned > 0) {
                    int count = bytesToSamples(bytes, aligned, samples);
                    transform.apply(samples, count);
                    samplesToBytes(samples, count, bytes);
                    out.write(bytes, 0, count * 2);
                }

                carry = n - aligned;
                if (carry > 0) {
                    System.arraycopy(bytes, aligned, bytes, 0, carry);
                }
            }

            if (carry != 0) {
                throw new IOException("PCM16/frame desalinhado");
            }
            out.flush();
        }
    }

    private static int bytesToSamples(byte[] bytes, int byteCount, short[] samples) {
        int count = byteCount / 2;
        for (int i = 0, j = 0; i < count; i++, j += 2) {
            samples[i] = (short) ((bytes[j] & 0xff) |
                    ((bytes[j + 1] & 0xff) << 8));
        }
        return count;
    }

    private static void samplesToBytes(short[] samples, int count, byte[] bytes) {
        for (int i = 0, j = 0; i < count; i++, j += 2) {
            short s = samples[i];
            bytes[j] = (byte) (s & 0xff);
            bytes[j + 1] = (byte) ((s >>> 8) & 0xff);
        }
    }

    private static long saturatingAdd(long a, long b) {
        if (b > 0 && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }

    private static long multiplyQ30(long value, long gainQ30) {
        if (value <= 0 || gainQ30 <= 0) return 0;
        long hi = value >>> 15;
        long lo = value & 0x7fffL;
        long result = (hi * gainQ30) >>> 15;
        result += (lo * gainQ30) >>> 30;
        return result;
    }

    static void encodeOpusOgg(File pcm, FileDescriptor fd,
                              int sampleRate, int channels) throws IOException {
        if (sampleRate != 48000) {
            throw new IOException(
                    "Export Ogg/Opus requer 48 kHz neste Delta; recebido " +
                    sampleRate);
        }
        if (channels < 1 || channels > 2) {
            throw new IOException("Opus export suporta mono/estereo");
        }

        MediaCodec codec = null;
        MediaMuxer muxer = null;
        boolean muxerStarted = false;

        try (FileInputStream in = new FileInputStream(pcm)) {
            MediaFormat format = MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_OPUS, sampleRate, channels);
            format.setInteger(MediaFormat.KEY_BIT_RATE,
                    channels == 1 ? 64000 : 128000);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS);
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();

            muxer = new MediaMuxer(fd, MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG);

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            byte[] block = new byte[16384];
            int track = -1;
            boolean inputDone = false;
            boolean outputDone = false;
            long framesQueued = 0;
            int frameBytes = channels * 2;

            while (!outputDone) {
                if (!inputDone) {
                    int inIndex = codec.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer inputBuffer = codec.getInputBuffer(inIndex);
                        if (inputBuffer == null) {
                            throw new IOException("Encoder input null");
                        }
                        inputBuffer.clear();

                        int capacity = inputBuffer.remaining();
                        int request = Math.min(capacity, block.length);
                        request -= request % frameBytes;
                        int read = in.read(block, 0, request);

                        long ptsUs = (framesQueued * 1_000_000L) / sampleRate;
                        if (read < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, ptsUs,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            int aligned = read - (read % frameBytes);
                            inputBuffer.put(block, 0, aligned);
                            codec.queueInputBuffer(inIndex, 0, aligned, ptsUs, 0);
                            framesQueued += aligned / frameBytes;
                        }
                    }
                }

                int outIndex = codec.dequeueOutputBuffer(info, 10000);
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (muxerStarted) {
                        throw new IOException("Formato do encoder mudou duas vezes");
                    }
                    track = muxer.addTrack(codec.getOutputFormat());
                    muxer.start();
                    muxerStarted = true;
                } else if (outIndex >= 0) {
                    ByteBuffer encoded = codec.getOutputBuffer(outIndex);
                    if (encoded != null && info.size > 0 &&
                            (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        if (!muxerStarted) {
                            throw new IOException("Muxer ainda nao iniciado");
                        }
                        encoded.position(info.offset);
                        encoded.limit(info.offset + info.size);
                        muxer.writeSampleData(track, encoded, info);
                    }

                    outputDone =
                            (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    codec.releaseOutputBuffer(outIndex, false);
                }
            }
        } finally {
            if (codec != null) {
                try {
                    codec.stop();
                } catch (IllegalStateException ignored) {
                }
                codec.release();
            }
            if (muxer != null) {
                if (muxerStarted) {
                    try {
                        muxer.stop();
                    } catch (IllegalStateException ignored) {
                    }
                }
                muxer.release();
            }
        }
    }
}
