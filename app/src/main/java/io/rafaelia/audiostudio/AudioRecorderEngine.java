package io.rafaelia.audiostudio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

final class AudioRecorderEngine {
    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 1;

    private final Context context;
    private final File output;
    private volatile boolean running;
    private AudioRecord recorder;
    private Thread worker;

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

        int min = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) {
            throw new IOException("AudioRecord buffer indisponivel: " + min);
        }

        int bufferBytes = Math.max(min * 2, 8192);
        recorder = new AudioRecord(
                source,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferBytes);

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            recorder.release();
            recorder = null;
            throw new IOException("AudioRecord nao inicializado");
        }

        running = true;
        recorder.startRecording();

        worker = new Thread(() -> captureLoop(bufferBytes), "rafaelia-capture");
        worker.start();
    }

    private void captureLoop(int bufferBytes) {
        short[] samples = new short[bufferBytes / 2];
        byte[] bytes = new byte[bufferBytes];

        try (FileOutputStream out = new FileOutputStream(output, false)) {
            while (running) {
                int n = recorder.read(samples, 0, samples.length,
                        AudioRecord.READ_BLOCKING);
                if (n <= 0) continue;

                for (int i = 0, j = 0; i < n; i++) {
                    short s = samples[i];
                    bytes[j++] = (byte) (s & 0xff);
                    bytes[j++] = (byte) ((s >>> 8) & 0xff);
                }
                out.write(bytes, 0, n * 2);
            }
            out.flush();
        } catch (IOException ignored) {
            running = false;
        }
    }

    void stop() {
        running = false;
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (IllegalStateException ignored) {
            }
        }
        if (worker != null) {
            try {
                worker.join(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
        worker = null;
    }
}
