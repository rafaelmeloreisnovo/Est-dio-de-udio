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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileDescriptor;
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
                MediaFormat f = extractor.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    track = i;
                    format = f;
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

    static void masterPcm(File input, File output, int channels, int preset)
            throws IOException {
        if (channels < 1 || channels > 2) {
            throw new IOException("DSP suporta mono/estereo");
        }

        NativeDsp.nativeReset(preset);

        byte[] bytes = new byte[8192];
        short[] samples = new short[4096];

        try (FileInputStream in = new FileInputStream(input);
             FileOutputStream out = new FileOutputStream(output, false)) {
            int n;
            while ((n = in.read(bytes)) >= 0) {
                if (n == 0) continue;
                if ((n & 1) != 0) {
                    throw new IOException("PCM16 desalinhado");
                }

                int count = n / 2;
                for (int i = 0, j = 0; i < count; i++, j += 2) {
                    samples[i] = (short) ((bytes[j] & 0xff) |
                            ((bytes[j + 1] & 0xff) << 8));
                }

                NativeDsp.nativeProcess(samples, count, channels);

                for (int i = 0, j = 0; i < count; i++, j += 2) {
                    short s = samples[i];
                    bytes[j] = (byte) (s & 0xff);
                    bytes[j + 1] = (byte) ((s >>> 8) & 0xff);
                }
                out.write(bytes, 0, n);
            }
            out.flush();
        }
    }

    static void encodeOpusOgg(File pcm, FileDescriptor fd,
                              int sampleRate, int channels) throws IOException {
        if (sampleRate != 48000) {
            throw new IOException(
                    "Delta 1 requer PCM 48 kHz; recebido " + sampleRate +
                    ". Resampler ainda PENDING.");
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
                    channels == 1 ? 64000 : 96000);
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
                        ByteBuffer input = codec.getInputBuffer(inIndex);
                        if (input == null) throw new IOException("Encoder input null");
                        input.clear();

                        int capacity = input.remaining();
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
                            input.put(block, 0, aligned);
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
