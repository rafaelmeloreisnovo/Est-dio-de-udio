package io.rafaelia.audiostudio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

final class PcmPlaybackEngine {
    private volatile boolean playing;
    private AudioTrack track;
    private Thread worker;

    boolean isPlaying() {
        return playing;
    }

    void play(File pcm, int sampleRate, int channels) throws IOException {
        stop();

        int channelMask = channels == 2
                ? AudioFormat.CHANNEL_OUT_STEREO
                : AudioFormat.CHANNEL_OUT_MONO;

        int min = AudioTrack.getMinBufferSize(
                sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) throw new IOException("AudioTrack buffer indisponivel: " + min);

        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(channelMask)
                .build();

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();

        track = new AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setBufferSizeInBytes(Math.max(min * 2, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build();

        if (track.getState() != AudioTrack.STATE_INITIALIZED) {
            track.release();
            track = null;
            throw new IOException("AudioTrack nao inicializado");
        }

        playing = true;
        track.play();
        worker = new Thread(() -> playbackLoop(pcm), "rafaelia-playback");
        worker.start();
    }

    private void playbackLoop(File pcm) {
        byte[] buffer = new byte[8192];
        try (FileInputStream in = new FileInputStream(pcm)) {
            int n;
            while (playing && (n = in.read(buffer)) >= 0) {
                if (n == 0) continue;
                int offset = 0;
                while (playing && offset < n) {
                    int written = track.write(
                            buffer, offset, n - offset, AudioTrack.WRITE_BLOCKING);
                    if (written <= 0) {
                        playing = false;
                        break;
                    }
                    offset += written;
                }
            }
        } catch (IOException ignored) {
        } finally {
            releaseTrack();
        }
    }

    void stop() {
        playing = false;
        if (track != null) {
            try {
                track.pause();
                track.flush();
            } catch (IllegalStateException ignored) {
            }
        }
        if (worker != null && worker != Thread.currentThread()) {
            try {
                worker.join(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        releaseTrack();
    }

    private void releaseTrack() {
        AudioTrack local = track;
        track = null;
        worker = null;
        playing = false;
        if (local != null) {
            try {
                local.stop();
            } catch (IllegalStateException ignored) {
            }
            local.release();
        }
    }
}
