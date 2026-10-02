/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

package io.rafaelia.audiostudio;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
    private static final int REQ_ACTIVITY = 102;

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
    private File lastZrf;
    private File lastCfr;
    private RelativeCalibrationEngine.Result lastCalibrationResult;
    private int lastMasteredRate = 48000;
    private int lastMasteredChannels = 1;
    private Uri lastOutput;
    private Uri lastEvidenceUri;

    private boolean pendingNarration;
    private boolean pendingCalibration;
    private volatile boolean calibrationRunning;
    private volatile boolean evidenceRunning;
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
        root.setFillViewport(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(10);
        box.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("RAFAELIA AUDIO · μ∆");
        title.setTextSize(20f);
        box.addView(title);

        status = new TextView(this);
        status.setTextSize(13f);
        box.addView(status);
        refreshReadyState();

        studioWorkspaceView = new StudioWorkspaceView(this);
        studioWorkspaceView.setCalibrationState(
                "CAL=DIGITAL+RELATIVE | ABS_SPL=PENDING_PHYSICAL_REFERENCE");
        studioWorkspaceView.setContainerState(
                "ZRF=RECORDER_WIRED | CFR=RELATIVE_READY");
        studioWorkspaceView.setActionListener(new StudioWorkspaceView.ActionListener() {
            @Override public void onRecord() {
                ensurePermissionAndRecord(false);
            }
            @Override public void onStopAndMaster() {
                stopAndMaster();
            }
            @Override public void onPlayMaster() {
                playMaster();
            }
            @Override public void onRunRelativeCalibration() {
                ensurePermissionAndCalibrate();
            }
            @Override public void onGenerateEvidence() {
                generateEvidenceBundle();
            }
            @Override public void onRequestSensorAccess() {
                requestOptionalSensorAccess();
            }
        });
        refreshSystemPanel();
        boolean landscape =
                getResources().getConfiguration().orientation ==
                        Configuration.ORIENTATION_LANDSCAPE;
        int studioHeight = landscape ? dp(430) : dp(560);
        box.addView(studioWorkspaceView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout transport = row();
        Button rawVoice = button("● REC");
        rawVoice.setOnClickListener(v -> ensurePermissionAndRecord(false));
        Button stop = button("■ MASTER");
        stop.setOnClickListener(v -> stopAndMaster());
        Button play = button("▶ PLAY");
        play.setOnClickListener(v -> playMaster());
        Button stopPlay = button("Ⅱ STOP");
        stopPlay.setOnClickListener(v -> playback.stop());
        transport.addView(rawVoice, weight());
        transport.addView(stop, weight());
        transport.addView(play, weight());
        transport.addView(stopPlay, weight());
        box.addView(transport);

        LinearLayout evidenceRow = row();
        Button evidence = button("★ VALIDAR + ZIPRAF");
        evidence.setOnClickListener(v -> generateEvidenceBundle());
        Button shareEvidence = button("COMPARTILHAR");
        shareEvidence.setOnClickListener(v -> shareEvidenceBundle());
        evidenceRow.addView(evidence, weight());
        evidenceRow.addView(shareEvidence, weight());
        box.addView(evidenceRow);

        TextView toolsTitle = new TextView(this);
        toolsTitle.setText("FERRAMENTAS · toque somente no que precisar");
        toolsTitle.setTextSize(12f);
        box.addView(toolsTitle);

        LinearLayout tools = row();
        Button wizard = button("PRÉ-VOO");
        wizard.setOnClickListener(v -> StudioWizard.show(this, this::refreshReadyState));
        Button narration = button("NARRAÇÃO");
        narration.setOnClickListener(v -> ensurePermissionAndRecord(true));
        Button importAudio = button("IMPORTAR");
        importAudio.setOnClickListener(v -> pickAudio());
        Button share = button("EXPORTAR");
        share.setOnClickListener(v -> shareLast());
        tools.addView(wizard, weight());
        tools.addView(narration, weight());
        tools.addView(importAudio, weight());
        tools.addView(share, weight());
        box.addView(tools);

        LinearLayout interop = row();
        Button rawExport = button("RAW PCM");
        rawExport.setOnClickListener(v -> exportMasterRaw());
        Button wavExport = button("WAV PCM16");
        wavExport.setOnClickListener(v -> exportMasterWav());
        Button opusShare = button("OPUS / SHARE");
        opusShare.setOnClickListener(v -> shareLast());
        interop.addView(rawExport, weight());
        interop.addView(wavExport, weight());
        interop.addView(opusShare, weight());
        box.addView(interop);

        profileSpinner = new Spinner(this);
        ArrayAdapter<String> profiles = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, PROFILES);
        profiles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileSpinner.setAdapter(profiles);
        box.addView(profileSpinner);

        scriptEdit = new EditText(this);
        scriptEdit.setHint("Roteiro / teleprompter");
        scriptEdit.setMinLines(2);
        scriptEdit.setMaxLines(5);
        scriptEdit.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        box.addView(scriptEdit);

        LinearLayout promptButtons = row();
        Button loadScript = button("CARREGAR TEXTO");
        loadScript.setOnClickListener(v -> loadPrompter());
        Button promptStart = button("▶ TEXTO");
        promptStart.setOnClickListener(v -> startPrompter());
        Button promptStop = button("■ TEXTO");
        promptStop.setOnClickListener(v -> stopPrompter());
        promptButtons.addView(loadScript, weight());
        promptButtons.addView(promptStart, weight());
        promptButtons.addView(promptStop, weight());
        box.addView(promptButtons);

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
        prompterText.setTextSize(22f);
        prompterText.setLineSpacing(8f, 1.10f);
        prompterText.setPadding(dp(8), dp(8), dp(8), dp(30));
        prompterText.setText("Teleprompter");
        prompterScroll.addView(prompterText);
        box.addView(prompterScroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(150)));

        spectrumView = new SpectrumView(this);
        box.addView(spectrumView);

        TextView note = new TextView(this);
        note.setText(
                "CORE: C freestanding/fixed-point. ANDROID EDGE: tela, toque, áudio, " +
                "sensor, armazenamento e carregamento do core. " +
                "TOKEN_VAZIO é preservado quando a evidência física não existe. " +
                "AUDITORIA NORMATIVA EXTERNA=NOT_AUDITED.");
        note.setTextSize(11f);
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
        refreshSystemPanel();
    }

    private void refreshSystemPanel() {
        if (studioWorkspaceView == null) return;
        studioWorkspaceView.setSystemState(SystemAccessSnapshot.describe(this));
        studioWorkspaceView.setOriginState(SystemAccessSnapshot.originState());
        studioWorkspaceView.setSignatureState(SystemAccessSnapshot.signatureState());
    }

    private void requestOptionalSensorAccess() {
        if (android.os.Build.VERSION.SDK_INT >= 29 &&
                checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    REQ_ACTIVITY);
            return;
        }

        refreshSystemPanel();
        status.setText(
                "Sensores locais: permissões mínimas reconciliadas. " +
                "Acelerômetro/magnetômetro/luz/proximidade não recebem permissões inventadas.");
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

    private void generateEvidenceBundle() {
        if (evidenceRunning) {
            status.setText("Geração de provas/ZIPRAF já está em execução.");
            return;
        }

        evidenceRunning = true;
        status.setText(
                "★ ASSURANCE — coletando material, execução, métricas, relações e gaps…");

        MicroDeltaVibrationProbe.run(this, 2200L, vibration -> {
            MicroDeltaMagnetometerProbe.run(this, 2200L, magnetometer -> {
                new Thread(() -> {
                    try {
                        EvidenceBundleWriter.Result rawResult = EvidenceBundleWriter.write(
                                this,
                                vibration,
                                magnetometer,
                                lastZrf,
                                lastCfr,
                                lastMasteredPcm,
                                lastCalibrationResult);
                        AssuranceZiprafWriter.Result result = AssuranceZiprafWriter.write(
                                this,
                                rawResult,
                                vibration,
                                magnetometer,
                                lastZrf,
                                lastCfr,
                                lastMasteredPcm,
                                lastCalibrationResult);
                        lastEvidenceUri = result.uri;
                        runOnUiThread(() -> status.setText(
                                "★ ZIPRAF GERADO — " + result.displayName +
                                "\nstate=" + result.starState +
                                " | relations=" + result.relationCount +
                                " | gaps=" + result.gapCount +
                                "\nraw evidence + hashes + métricas + relações + claims" +
                                "\nNORMAS=NOT_AUDITED | claim_allowed=false" +
                                "\nSalvo em Downloads/RafaeliaAudio/Assurance."));
                    } catch (Exception e) {
                        runOnUiThread(() ->
                                status.setText("Falha ao gerar assurance ZIPRAF: " + e.getMessage()));
                    } finally {
                        evidenceRunning = false;
                    }
                }, "rafaelia-assurance-zipraf-writer").start();
            });
        });
    }

    private void shareEvidenceBundle() {
        if (lastEvidenceUri == null) {
            status.setText("Gere o ZIPRAF de assurance antes de compartilhar.");
            return;
        }

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/zip");
        send.putExtra(Intent.EXTRA_STREAM, lastEvidenceUri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Compartilhar ZIPRAF Rafaelia"));
    }

    private void ensurePermissionAndRecord(boolean narration) {
        pendingCalibration = false;
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

    private void ensurePermissionAndCalibrate() {
        pendingNarration = false;
        pendingCalibration = true;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        pendingCalibration = false;
        runRelativeCalibration();
    }

    private void runRelativeCalibration() {
        if (calibrationRunning) {
            status.setText("Calibração relativa já está em execução.");
            return;
        }
        if (recorder != null) {
            status.setText("Pare a gravação antes da calibração acústica.");
            return;
        }

        calibrationRunning = true;
        playback.stop();
        status.setText(
                "CALIBRAÇÃO RELATIVA — preparando sync + sweep em nível conservador…");
        if (studioWorkspaceView != null) {
            studioWorkspaceView.setCalibrationState(
                    "CAL=RUNNING_RELATIVE | ABS_SPL=PENDING_PHYSICAL_REFERENCE");
        }

        new Thread(() -> {
            try {
                File cfr = new File(
                        getCacheDir(),
                        "rafaelia_cal_" + System.currentTimeMillis() + ".cfr");
                RelativeCalibrationEngine.Result result =
                        RelativeCalibrationEngine.run(this, cfr);
                lastCfr = result.cfrFile;
                lastCalibrationResult = result;

                runOnUiThread(() -> {
                    long rtMs = result.preferredRt60Millis();
                    String roomState = "ROOM=" + result.decayMethod() +
                            (rtMs >= 0L ? " | RT60_REL~" + rtMs + " ms" :
                                    " | RT60_REL=TOKEN_VAZIO_DYNAMIC_RANGE");
                    if (studioWorkspaceView != null) {
                        studioWorkspaceView.setCalibrationState(
                                "CAL=RELATIVE_ANALYZED | input=" + result.inputSource +
                                " | lag=" + result.bestLag + " samples");
                        studioWorkspaceView.setCalibrationAnalysis(
                                result.powerRatiosQ20(), result.validTransferBands(), roomState);
                        studioWorkspaceView.setContainerState(
                                "CFR=RECORDED+SPEC16+ROOM_RELATIVE | ABS_SPL=PENDING_PHYSICAL_REFERENCE");
                    }
                    status.setText(
                            "CFR RELATIVO ANALISADO" +
                            "\ninput=" + result.inputSource +
                            " | lag=" + result.bestLag + " samples" +
                            " | latency=" + result.latencyMicros() + " us" +
                            "\npolarity=" + (result.correlation < 0L ? "INVERTED" : "NORMAL") +
                            " | captured=" + result.capturedFrames + " frames" +
                            " | bands=" + result.validTransferBands() + "/16" +
                            "\n" + roomState +
                            " | EXTERNAL_STANDARD_AUDIT=NOT_AUDITED" +
                            "\nABS_SPL=PENDING_PHYSICAL_REFERENCE — referência acústica física necessária." +
                            "\nCFR: " + result.cfrFile.getAbsolutePath());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    lastCalibrationResult = null;
                    if (studioWorkspaceView != null) {
                        studioWorkspaceView.setCalibrationState(
                                "CAL=FAIL | ABS_SPL=TOKEN_VAZIO");
                        studioWorkspaceView.setCalibrationAnalysis(
                                null, 0, "ROOM=NOT_RUN_AFTER_CAL_FAIL");
                    }
                    status.setText("Falha na calibração relativa: " + e.getMessage());
                });
            } finally {
                calibrationRunning = false;
            }
        }, "rafaelia-relative-calibration").start();
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
            File zrf = new File(
                    getCacheDir(), "rafaelia_session_" +
                    System.currentTimeMillis() + ".zrf");
            Uri outputUri = null;

            try {
                try {
                    lastZrf = SessionContainerWriter.wrapRawPcmAsZrf(
                            input, zrf, sampleRate, channels);
                    runOnUiThread(() -> {
                        if (studioWorkspaceView != null) {
                            studioWorkspaceView.setContainerState(
                                    "ZRF=RECORDED | CFR=FORMAT_READY_CAPTURE_PENDING");
                        }
                    });
                } catch (Exception containerError) {
                    lastZrf = null;
                    runOnUiThread(() -> {
                        if (studioWorkspaceView != null) {
                            studioWorkspaceView.setContainerState(
                                    "ZRF=FAIL | CFR=FORMAT_READY_CAPTURE_PENDING");
                        }
                    });
                }

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
                            "\nAuditoria normativa externa: NOT_AUDITED; vetores formais permanecem pendentes.");
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

    private void exportMasterRaw() {
        exportMasterInterop(false);
    }

    private void exportMasterWav() {
        exportMasterInterop(true);
    }

    private void exportMasterInterop(boolean wav) {
        if (lastMasteredPcm == null || !lastMasteredPcm.isFile()) {
            status.setText("Ainda não há master PCM para exportar.");
            return;
        }

        String stamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.US).format(new Date());
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,
                "Rafaelia_Master_" + stamp + (wav ? ".wav" : ".pcm"));
        values.put(MediaStore.MediaColumns.MIME_TYPE,
                wav ? "audio/wav" : "application/octet-stream");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                wav ? Environment.DIRECTORY_MUSIC + "/RafaeliaAudio" :
                        Environment.DIRECTORY_DOWNLOADS + "/RafaeliaAudio/Raw");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri collection = wav ?
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI :
                MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        Uri uri = getContentResolver().insert(collection, values);
        if (uri == null) {
            status.setText("Falha ao criar destino " + (wav ? "WAV." : "RAW."));
            return;
        }

        new Thread(() -> {
            try (java.io.OutputStream out =
                         getContentResolver().openOutputStream(uri, "w")) {
                if (out == null) throw new IllegalStateException("stream indisponível");
                long bytes = wav ?
                        AudioInteropWriter.writeWav16(
                                lastMasteredPcm, out,
                                lastMasteredRate, lastMasteredChannels) :
                        AudioInteropWriter.writeRaw(lastMasteredPcm, out);

                ContentValues done = new ContentValues();
                done.put(MediaStore.MediaColumns.IS_PENDING, 0);
                getContentResolver().update(uri, done, null, null);
                runOnUiThread(() -> status.setText(
                        (wav ? "WAV PCM16" : "RAW PCM") +
                        " exportado: " + bytes + " bytes"));
            } catch (Exception e) {
                getContentResolver().delete(uri, null, null);
                runOnUiThread(() ->
                        status.setText("Falha no export: " + e.getMessage()));
            }
        }, wav ? "rafaelia-wav-export" : "rafaelia-raw-export").start();
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
            if (pendingCalibration) {
                pendingCalibration = false;
                runRelativeCalibration();
            } else if (pendingNarration) {
                countdown(3);
            } else {
                startRecording(false);
            }
        } else if (requestCode == REQ_AUDIO) {
            pendingCalibration = false;
            status.setText("Permissão de microfone negada.");
        } else if (requestCode == REQ_ACTIVITY) {
            refreshSystemPanel();
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                status.setText(
                        "ACTIVITY_RECOGNITION concedida para sensores de movimento compatíveis.");
            } else {
                status.setText(
                        "ACTIVITY_RECOGNITION negada; sensores que não exigem essa permissão continuam disponíveis.");
            }
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
