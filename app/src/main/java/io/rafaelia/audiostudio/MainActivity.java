package io.rafaelia.audiostudio;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int REQ_AUDIO = 100;
    private static final int REQ_PICK = 101;

    private static final String[] PROFILES = {
            "WhatsApp / voz — -16 LUFS workflow",
            "Narração — -18 LUFS workflow",
            "Broadcast — -23 LUFS EBU R128",
            "Música clean — -18 LUFS conservador"
    };

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final PcmPlaybackEngine playback = new PcmPlaybackEngine();

    private TextView status;
    private EditText scriptEdit;
    private TextView prompterText;
    private ScrollView prompterScroll;
    private SeekBar speedSeek;
    private TextView speedLabel;
    private Spinner profileSpinner;
    private SpectrumView spectrumView;
    private StudioWorkspaceView studioWorkspaceView;
    private final short[] liveWave = new short[512];

    private AudioRecorderEngine recorder;
    private File recordedPcm;
    private File lastMasteredPcm;
    private int lastMasteredRate = 48000;
    private int lastMasteredChannels = 1;
    private Uri lastOutput;

    private boolean pendingNarration;
    private boolean prompterRunning;
    private boolean telemetryRunning;

    private final Runnable prompterTick = new Runnable() {
        @Override
        public void run() {
            if (!prompterRunning) return;
            int wpm = currentWpm();
            int pixels = Math.max(1, wpm / 45);
            prompterScroll.scrollBy(0, pixels);
            ui.postDelayed(this, 100);
        }
    };

    private final Runnable telemetryTick = new Runnable() {
        @Override
        public void run() {
            if (!telemetryRunning || recorder == null) return;
            AudioRecorderEngine.CaptureStats s = recorder.getStats();
            int peakPct = (int) ((long) s.peak * 100L / 32768L);
            int rmsPct = (int) ((long) s.rms * 100L / 32768L);
            status.setText(
                    "GRAVANDO 48 kHz PCM16 — " + s.source +
                    "\nPeak amostral: " + peakPct + "% FS" +
                    " | RMS bruto: " + rmsPct + "% FS" +
                    " | clipping samples: " + s.clipped);
            if (studioWorkspaceView != null) {
                int liveCount = recorder.copyLatest(liveWave);
                studioWorkspaceView.setCaptureStats(
                        peakPct, rmsPct, s.clipped, s.samples, s.source);
                studioWorkspaceView.setWaveform(liveWave, liveCount);
            }
            ui.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildUi());

        if (StudioWizard.shouldShow(this)) {
            ui.postDelayed(() -> StudioWizard.show(this, this::refreshReadyState), 250);
        }
    }

    private View buildUi() {
        ScrollView root = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        box.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Rafaelia Audio Studio — Delta 2");
        title.setTextSize(25f);
        box.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(
                "Narration workstation • 48 kHz • freestanding DSP core • Ogg/Opus");
        box.addView(subtitle);

        Button wizard = button("Abrir Wizard / pré-voo");
        wizard.setOnClickListener(v -> StudioWizard.show(this, this::refreshReadyState));
        box.addView(wizard);

        status = new TextView(this);
        status.setTextSize(16f);
        box.addView(status);
        refreshReadyState();

        studioWorkspaceView = new StudioWorkspaceView(this);
        studioWorkspaceView.setCalibrationState(
                "CAL=DIGITAL+RELATIVE | ABS_SPL=TOKEN_VAZIO");
        studioWorkspaceView.setContainerState(
                "ZRF/CFR CORE=IMPLEMENTED_UNTESTED | RECORDING_WIRE=PENDING");
        box.addView(studioWorkspaceView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(520)));

        TextView profileTitle = new TextView(this);
        profileTitle.setText("\nPerfil de remasterização / normalização");
        box.addView(profileTitle);

        profileSpinner = new Spinner(this);
        ArrayAdapter<String> profiles = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, PROFILES);
        profiles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileSpinner.setAdapter(profiles);
        box.addView(profileSpinner);

        TextView scriptTitle = new TextView(this);
        scriptTitle.setText("\nRoteiro da narração");
        scriptTitle.setTextSize(19f);
        box.addView(scriptTitle);

        scriptEdit = new EditText(this);
        scriptEdit.setHint(
                "Cole ou escreva aqui o texto. O original permanece editável.");
        scriptEdit.setMinLines(5);
        scriptEdit.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        box.addView(scriptEdit);

        Button loadScript = button("Carregar texto no teleprompter");
        loadScript.setOnClickListener(v -> loadPrompter());
        box.addView(loadScript);

        speedLabel = new TextView(this);
        box.addView(speedLabel);

        speedSeek = new SeekBar(this);
        speedSeek.setMax(160);
        speedSeek.setProgress(60);
        speedSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                updateSpeedLabel();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        box.addView(speedSeek);
        updateSpeedLabel();

        prompterScroll = new ScrollView(this);
        prompterScroll.setFillViewport(true);
        prompterText = new TextView(this);
        prompterText.setTextSize(26f);
        prompterText.setLineSpacing(10f, 1.15f);
        prompterText.setPadding(dp(12), dp(16), dp(12), dp(80));
        prompterText.setText(
                "O teleprompter aparecerá aqui. Carregue o roteiro acima.");
        prompterScroll.addView(prompterText);
        box.addView(prompterScroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(300)));

        LinearLayout promptButtons = row();
        Button promptStart = button("▶ Rolar");
        promptStart.setOnClickListener(v -> startPrompter());
        Button promptStop = button("■ Parar texto");
        promptStop.setOnClickListener(v -> stopPrompter());
        promptButtons.addView(promptStart, weight());
        promptButtons.addView(promptStop, weight());
        box.addView(promptButtons);

        Button narration = button("🎙 Gravar narração + roteiro (3s)");
        narration.setOnClickListener(v -> ensurePermissionAndRecord(true));
        box.addView(narration);

        Button rawVoice = button("Gravar voz sem teleprompter");
        rawVoice.setOnClickListener(v -> ensurePermissionAndRecord(false));
        box.addView(rawVoice);

        Button stop = button("Parar → remasterizar → normalizar");
        stop.setOnClickListener(v -> stopAndMaster());
        box.addView(stop);

        Button importAudio = button("Importar áudio / WhatsApp / Ogg Opus");
        importAudio.setOnClickListener(v -> pickAudio());
        box.addView(importAudio);

        LinearLayout playbackRow = row();
        Button play = button("▶ Ouvir master");
        play.setOnClickListener(v -> playMaster());
        Button stopPlay = button("■ Parar áudio");
        stopPlay.setOnClickListener(v -> playback.stop());
        playbackRow.addView(play, weight());
        playbackRow.addView(stopPlay, weight());
        box.addView(playbackRow);

        Button share = button("Compartilhar último Ogg/Opus");
        share.setOnClickListener(v -> shareLast());
        box.addView(share);

        TextView spectrumTitle = new TextView(this);
        spectrumTitle.setText("\nEspectrometria relativa — 16 centros, 80 Hz…22 kHz");
        box.addView(spectrumTitle);

        spectrumView = new SpectrumView(this);
        box.addView(spectrumView);

        TextView note = new TextView(this);
        note.setText(
                "\nGates: -23 LUFS é EBU R128. -18/-16 são targets de workflow. " +
                "True-peak usa o FIR 4× do BS.1770-5, mas conformidade formal " +
                "permanece PENDING até vetores normativos. O APK usa Android para I/O; " +
                "o DSP/medidor C é o componente freestanding.");
        box.addView(note);

        root.addView(box);
        return root;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        return b;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void refreshReadyState() {
        status.setText(
                "READY — fonte preservada, DSP pós-captura, normalização gated.\n" +
                "Escolha um perfil, carregue o roteiro ou importe um áudio.");
    }

    private int currentWpm() {
        return 80 + speedSeek.getProgress();
    }

    private void updateSpeedLabel() {
        if (speedLabel != null && speedSeek != null) {
            speedLabel.setText(
                    "Velocidade aproximada do teleprompter: " + currentWpm() + " WPM");
        }
    }

    private void loadPrompter() {
        String text = scriptEdit.getText().toString().trim();
        if (text.length() == 0) {
            status.setText("Roteiro vazio: escreva ou cole um texto.");
            return;
        }
        prompterText.setText(text + "\n\n");
        prompterScroll.scrollTo(0, 0);
        status.setText("Roteiro carregado. Ajuste WPM e faça o pré-voo.");
    }

    private void startPrompter() {
        if (prompterRunning) return;
        prompterRunning = true;
        ui.removeCallbacks(prompterTick);
        ui.post(prompterTick);
    }

    private void stopPrompter() {
        prompterRunning = false;
        ui.removeCallbacks(prompterTick);
    }

    private void pickAudio() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, REQ_PICK);
    }

    private void ensurePermissionAndRecord(boolean narration) {
        pendingNarration = narration;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }

        if (narration) {
            countdown(3);
        } else {
            startRecording(false);
        }
    }

    private void countdown(int value) {
        if (value <= 0) {
            startRecording(true);
            return;
        }
        status.setText("Narração começa em " + value + "…");
        ui.postDelayed(() -> countdown(value - 1), 1000);
    }

    private void startRecording(boolean narration) {
        if (recorder != null) {
            status.setText("Já existe gravação ativa.");
            return;
        }

        try {
            String stamp = new SimpleDateFormat(
                    "yyyyMMdd_HHmmss", Locale.US).format(new Date());
            recordedPcm = new File(
                    getCacheDir(), "rafaelia_raw_" + stamp + "_48k.pcm");
            recorder = new AudioRecorderEngine(this, recordedPcm);
            recorder.start();

            telemetryRunning = true;
            ui.removeCallbacks(telemetryTick);
            ui.post(telemetryTick);

            if (narration) {
                if (scriptEdit.getText().toString().trim().length() > 0 &&
                        prompterText.getText().toString().startsWith("O teleprompter")) {
                    loadPrompter();
                }
                prompterScroll.scrollTo(0, 0);
                startPrompter();
            }
        } catch (Exception e) {
            recorder = null;
            telemetryRunning = false;
            status.setText("Falha ao iniciar gravação: " + e.getMessage());
        }
    }

    private void stopAndMaster() {
        if (recorder == null) {
            status.setText("Nenhuma gravação ativa.");
            return;
        }

        AudioRecorderEngine.CaptureStats capture = recorder.getStats();
        int sampleRate = recorder.getSampleRate();
        int channels = recorder.getChannels();

        telemetryRunning = false;
        ui.removeCallbacks(telemetryTick);
        stopPrompter();
        recorder.stop();
        recorder = null;

        status.setText(
                "CAPTURA ENCERRADA — " + capture.source +
                "\npeak=" + capture.peak +
                " rms=" + capture.rms +
                " clipped_samples=" + capture.clipped +
                "\nRemasterizando…");

        runPipelineFromPcm(recordedPcm, sampleRate, channels, "microfone");
    }

    private int selectedPreset() {
        int p = profileSpinner.getSelectedItemPosition();
        if (p == 0) return NativeDsp.PRESET_WHATSAPP_VOICE;
        if (p == 3) return NativeDsp.PRESET_MUSIC_CLEAN;
        return NativeDsp.PRESET_NATURAL_VOICE;
    }

    private long selectedTargetEnergy() {
        int p = profileSpinner.getSelectedItemPosition();
        if (p == 0) return NativeDsp.TARGET_MOBILE_Q36;
        if (p == 2) return NativeDsp.TARGET_EBU_R128_Q36;
        return NativeDsp.TARGET_NARRATION_Q36;
    }

    private String selectedTargetLabel() {
        int p = profileSpinner.getSelectedItemPosition();
        if (p == 0) return "-16 LUFS workflow";
        if (p == 2) return "-23 LUFS EBU R128";
        return "-18 LUFS workflow";
    }

    private void runPipelineFromPcm(
            File input, int sampleRate, int channels, String origin) {
        status.setText(
                "PROCESSANDO " + origin + " — " + selectedTargetLabel() + "…");

        final int preset = selectedPreset();
        final long target = selectedTargetEnergy();
        final String targetLabel = selectedTargetLabel();

        new Thread(() -> {
            File mastered = new File(getCacheDir(), "rafaelia_mastered_48k.pcm");
            Uri outputUri = null;

            try {
                AudioPipeline.MasterResult result =
                        AudioPipeline.masterAndNormalize(
                                input, mastered, sampleRate, channels,
                                preset, target);

                outputUri = createPendingOutput();
                if (outputUri == null) {
                    throw new IllegalStateException("MediaStore insert retornou null");
                }

                try (ParcelFileDescriptor pfd =
                             getContentResolver().openFileDescriptor(outputUri, "w")) {
                    if (pfd == null) {
                        throw new IllegalStateException("FD de saída indisponível");
                    }
                    AudioPipeline.encodeOpusOgg(
                            result.pcmFile,
                            pfd.getFileDescriptor(),
                            sampleRate,
                            channels);
                }

                publishOutput(outputUri);
                lastOutput = outputUri;
                lastMasteredPcm = result.pcmFile;
                lastMasteredRate = sampleRate;
                lastMasteredChannels = channels;

                int gainPct = (int) ((result.gainQ30 * 100L) >> 30);
                long fullScaleQ16 = 32768L << 16;
                long tpPermille = result.truePeakAfterQ16 <= 0 ? 0 :
                        result.truePeakAfterQ16 * 1000L / fullScaleQ16;

                final Uri finalOutputUri = outputUri;
                runOnUiThread(() -> {
                    spectrumView.setBands(result.spectrum);
                    if (studioWorkspaceView != null) {
                        studioWorkspaceView.setSpectrum(result.spectrum);
                        studioWorkspaceView.setMasterState(
                                "MASTER=" + targetLabel +
                                " | blocks=" + result.gatedBlocks +
                                " | gain=" + gainPct + "%");
                    }
                    status.setText(
                            "MASTER GERADO — " + targetLabel +
                            "\nGanho final: " + gainPct + "%" +
                            " | gated blocks: " + result.gatedBlocks +
                            "\nTrue-peak estimado pós-gain: " + tpPermille +
                            "‰ FS (ceiling -1 dBTP)" +
                            "\nOgg/Opus: " + finalOutputUri +
                            "\nConformidade normativa: PENDING test vectors.");
                });
            } catch (Exception e) {
                if (outputUri != null) {
                    getContentResolver().delete(outputUri, null, null);
                }
                runOnUiThread(() ->
                        status.setText("Falha no pipeline: " + e.getMessage()));
            }
        }, "rafaelia-mastering").start();
    }

    private void playMaster() {
        if (lastMasteredPcm == null || !lastMasteredPcm.exists()) {
            status.setText("Ainda não há master PCM para reprodução.");
            return;
        }
        try {
            playback.play(
                    lastMasteredPcm, lastMasteredRate, lastMasteredChannels);
            status.setText("REPRODUZINDO master PCM.");
        } catch (Exception e) {
            status.setText("Falha de reprodução: " + e.getMessage());
        }
    }

    private Uri createPendingOutput() {
        String stamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.US).format(new Date());

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME,
                "Rafaelia_Master_" + stamp + ".opus");
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/ogg");
        values.put(MediaStore.Audio.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MUSIC + "/RafaeliaAudio");
        values.put(MediaStore.Audio.Media.IS_PENDING, 1);

        return getContentResolver().insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void publishOutput(Uri uri) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.IS_PENDING, 0);
        getContentResolver().update(uri, values, null, null);
    }

    private void shareLast() {
        if (lastOutput == null) {
            status.setText("Ainda não há saída processada.");
            return;
        }

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("audio/ogg");
        send.putExtra(Intent.EXTRA_STREAM, lastOutput);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Compartilhar áudio"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK && resultCode == RESULT_OK &&
                data != null && data.getData() != null) {
            Uri uri = data.getData();
            status.setText("DECODIFICANDO arquivo selecionado…");

            new Thread(() -> {
                try {
                    File pcm = new File(getCacheDir(), "rafaelia_imported.pcm");
                    AudioPipeline.DecodedAudio decoded =
                            AudioPipeline.decodeToPcm(this, uri, pcm);
                    runOnUiThread(() -> runPipelineFromPcm(
                            decoded.pcmFile,
                            decoded.sampleRate,
                            decoded.channels,
                            "arquivo importado"));
                } catch (Exception e) {
                    runOnUiThread(() ->
                            status.setText("Falha ao decodificar: " + e.getMessage()));
                }
            }, "rafaelia-decode").start();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (pendingNarration) countdown(3);
            else startRecording(false);
        } else if (requestCode == REQ_AUDIO) {
            status.setText("Permissão de microfone negada.");
        }
    }

    @Override
    protected void onDestroy() {
        telemetryRunning = false;
        stopPrompter();
        ui.removeCallbacks(telemetryTick);
        playback.stop();
        if (recorder != null) {
            recorder.stop();
            recorder = null;
        }
        super.onDestroy();
    }
}
