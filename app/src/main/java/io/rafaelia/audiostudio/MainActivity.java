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
import android.content.res.ColorStateList;
import android.graphics.Color;
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
            "Mobile voice · target energy",
            "Narration · target energy",
            "Broadcast · target energy · NOT_AUDITED",
            "Music clean · target energy"
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
    // JNI DSP/meter states are platform-owned and currently shared: serialize sessions.
    private final AudioProcessingGate processingGate = new AudioProcessingGate();
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
            AudioRecorderEngine.CaptureStats capture = recorder.getStats();
            int peakPct = (int) ((long) capture.peak * 100L / 32768L);
            int rmsPct = (int) ((long) capture.rms * 100L / 32768L);
            status.setText(
                    "RECORDING · 48 kHz PCM16 · " + capture.source +
                    "\nPeak " + peakPct + "% FS · RMS " + rmsPct +
                    "% FS · clipped " + capture.clipped);
            if (studioWorkspaceView != null) {
                int liveCount = recorder.copyLatest(liveWave);
                studioWorkspaceView.setCaptureStats(
                        peakPct, rmsPct, capture.clipped, capture.samples, capture.source);
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
        int pad = dp(8);
        box.setPadding(pad, pad, pad, dp(16));
        box.setBackgroundColor(Color.rgb(10, 13, 17));

        status = new TextView(this);
        status.setTextSize(11f);
        status.setTextColor(Color.rgb(188, 200, 209));
        status.setPadding(dp(6), dp(3), dp(6), dp(8));
        box.addView(status);

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
        });
        refreshSystemPanel();
        box.addView(studioWorkspaceView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView profileLabel = sectionLabel("PROCESSING PROFILE");
        box.addView(profileLabel);

        profileSpinner = new Spinner(this);
        ArrayAdapter<String> profiles = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, PROFILES);
        profiles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileSpinner.setAdapter(profiles);
        box.addView(profileSpinner);

        LinearLayout utilityRow = row();
        Button narrationToggle = button("Narration");
        Button importAudio = button("Import audio");
        Button moreToggle = button("More tools");
        utilityRow.addView(narrationToggle, weight());
        utilityRow.addView(importAudio, weight());
        utilityRow.addView(moreToggle, weight());
        box.addView(utilityRow);

        LinearLayout narrationPanel = verticalPanel();
        narrationPanel.setVisibility(View.GONE);
        buildNarrationPanel(narrationPanel);
        box.addView(narrationPanel);

        LinearLayout advancedPanel = verticalPanel();
        advancedPanel.setVisibility(View.GONE);
        buildAdvancedPanel(advancedPanel);
        box.addView(advancedPanel);

        narrationToggle.setOnClickListener(v ->
                togglePanel(narrationPanel, narrationToggle, "Narration", "Close narration"));
        importAudio.setOnClickListener(v -> pickAudio());
        moreToggle.setOnClickListener(v ->
                togglePanel(advancedPanel, moreToggle, "More tools", "Close tools"));

        root.addView(box);
        refreshReadyState();
        return root;
    }

    private void buildNarrationPanel(LinearLayout panel) {
        panel.addView(sectionLabel("NARRATION / TELEPROMPTER"));

        scriptEdit = new EditText(this);
        scriptEdit.setHint("Script / teleprompter text");
        scriptEdit.setMinLines(2);
        scriptEdit.setMaxLines(5);
        scriptEdit.setTextColor(Color.rgb(222, 229, 234));
        scriptEdit.setHintTextColor(Color.rgb(120, 137, 149));
        scriptEdit.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        panel.addView(scriptEdit);

        LinearLayout controls = row();
        Button loadScript = button("Load text");
        loadScript.setOnClickListener(v -> loadPrompter());
        Button recordNarration = button("Record narration");
        recordNarration.setOnClickListener(v -> ensurePermissionAndRecord(true));
        Button stopText = button("Stop text");
        stopText.setOnClickListener(v -> stopPrompter());
        controls.addView(loadScript, weight());
        controls.addView(recordNarration, weight());
        controls.addView(stopText, weight());
        panel.addView(controls);

        speedLabel = new TextView(this);
        speedLabel.setTextColor(Color.rgb(164, 180, 191));
        speedLabel.setTextSize(10f);
        panel.addView(speedLabel);

        speedSeek = new SeekBar(this);
        speedSeek.setMax(160);
        speedSeek.setProgress(60);
        speedSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSpeedLabel();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        panel.addView(speedSeek);
        updateSpeedLabel();

        prompterScroll = new ScrollView(this);
        prompterScroll.setFillViewport(true);
        prompterText = new TextView(this);
        prompterText.setTextSize(22f);
        prompterText.setTextColor(Color.rgb(232, 238, 242));
        prompterText.setLineSpacing(8f, 1.10f);
        prompterText.setPadding(dp(8), dp(8), dp(8), dp(30));
        prompterText.setText("Teleprompter");
        prompterScroll.addView(prompterText);
        panel.addView(prompterScroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(150)));
    }

    private void buildAdvancedPanel(LinearLayout panel) {
        panel.addView(sectionLabel("EXECUTABLE TOOLS"));

        LinearLayout first = row();
        Button preflight = button("Pre-flight");
        preflight.setOnClickListener(v -> StudioWizard.show(this, this::refreshReadyState));
        Button stopPlayback = button("Stop playback");
        stopPlayback.setOnClickListener(v -> playback.stop());
        first.addView(preflight, weight());
        first.addView(stopPlayback, weight());
        panel.addView(first);

        panel.addView(sectionLabel("PCM EXPORT FORMATS"));

        LinearLayout exportPrimary = row();
        Button rawExport = button("RAW");
        rawExport.setOnClickListener(v -> exportMasterRaw());
        Button wavExport = button("WAV");
        wavExport.setOnClickListener(v -> exportMasterWav());
        Button aiffExport = button("AIFF");
        aiffExport.setOnClickListener(v -> exportMasterAiff());
        exportPrimary.addView(rawExport, weight());
        exportPrimary.addView(wavExport, weight());
        exportPrimary.addView(aiffExport, weight());
        panel.addView(exportPrimary);

        LinearLayout exportSecondary = row();
        Button auExport = button("AU / SND");
        auExport.setOnClickListener(v -> exportMasterAu());
        Button cafExport = button("CAF / LPCM");
        cafExport.setOnClickListener(v -> exportMasterCaf());
        exportSecondary.addView(auExport, weight());
        exportSecondary.addView(cafExport, weight());
        panel.addView(exportSecondary);

        LinearLayout share = row();
        Button shareAudio = button("Share processed audio");
        shareAudio.setOnClickListener(v -> shareLast());
        Button shareEvidence = button("Share ZIPRAF");
        shareEvidence.setOnClickListener(v -> shareEvidenceBundle());
        share.addView(shareAudio, weight());
        share.addView(shareEvidence, weight());
        panel.addView(share);
    }

    private LinearLayout verticalPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(4), dp(8), dp(4), dp(8));
        return panel;
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

    private TextView sectionLabel(String label) {
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(9f);
        text.setTextColor(Color.rgb(124, 145, 160));
        text.setPadding(dp(5), dp(10), dp(5), dp(4));
        return text;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(11f);
        button.setTextColor(Color.rgb(235, 241, 245));
        button.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(35, 49, 60)));
        return button;
    }

    private void togglePanel(
            View panel, Button trigger, String closedLabel, String openLabel) {
        boolean opening = panel.getVisibility() != View.VISIBLE;
        panel.setVisibility(opening ? View.VISIBLE : View.GONE);
        trigger.setText(opening ? openLabel : closedLabel);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void refreshReadyState() {
        if (status == null) return;
        status.setText(
                "READY · source preserved · 48 kHz PCM16 · " +
                "tap dashboard cards/graphs to inspect");
        refreshSystemPanel();
    }

    private void refreshSystemPanel() {
        if (studioWorkspaceView == null) return;
        studioWorkspaceView.setSystemState(SystemAccessSnapshot.describe(this));
        studioWorkspaceView.setOriginState(SystemAccessSnapshot.originState());
        studioWorkspaceView.setSignatureState(SystemAccessSnapshot.signatureState());
    }

    private int currentWpm() {
        return 80 + speedSeek.getProgress();
    }

    private void updateSpeedLabel() {
        if (speedLabel != null && speedSeek != null) {
            speedLabel.setText("Teleprompter speed · " + currentWpm() + " WPM");
        }
    }

    private void loadPrompter() {
        String text = scriptEdit.getText().toString().trim();
        if (text.length() == 0) {
            status.setText("Script empty · write or paste text first.");
            return;
        }
        prompterText.setText(text + "\n\n");
        prompterScroll.scrollTo(0, 0);
        status.setText("Narration script loaded.");
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
        if (processingGate.busy() || calibrationRunning || recorder != null) {
            status.setText("PROOF_BLOCKED — audio session is still active");
            return;
        }
        if (evidenceRunning) {
            status.setText("Evidence / ZIPRAF generation is already running.");
            return;
        }

        evidenceRunning = true;
        // Do not contaminate microphone/sensor evidence with app playback.
        playback.stop();
        status.setText(
                "PROOF · ação explícita: coletando μ∆ local · bounded sensor window + evidence…");

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
                                "ZIPRAF GENERATED · " + result.displayName +
                                "\nstate=" + result.starState +
                                " · relations=" + result.relationCount +
                                " · gaps=" + result.gapCount +
                                "\nNORMATIVE_AUDIT=NOT_AUDITED · claim_allowed=false"));
                    } catch (Exception e) {
                        runOnUiThread(() ->
                                status.setText("ZIPRAF generation failed · " + e.getMessage()));
                    } finally {
                        evidenceRunning = false;
                    }
                }, "rafaelia-assurance-zipraf-writer").start();
            });
        });
    }

    private void shareEvidenceBundle() {
        if (lastEvidenceUri == null) {
            status.setText("Generate a ZIPRAF bundle before sharing.");
            return;
        }

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/zip");
        send.putExtra(Intent.EXTRA_STREAM, lastEvidenceUri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Share Rafaelia ZIPRAF"));
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
        if (processingGate.busy() || evidenceRunning) {
            status.setText("CAL_BLOCKED — master or ZIPRAF is active");
            return;
        }
        if (calibrationRunning) {
            status.setText("Relative calibration is already running.");
            return;
        }
        if (recorder != null) {
            status.setText("Stop recording before acoustic calibration.");
            return;
        }

        calibrationRunning = true;
        playback.stop();
        status.setText("RELATIVE CAL · sync + sweep at conservative level…");
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
                            "RELATIVE CFR ANALYZED" +
                            "\ninput=" + result.inputSource +
                            " · latency=" + result.latencyMicros() + " us" +
                            " · bands=" + result.validTransferBands() + "/16" +
                            "\n" + roomState +
                            " · EXTERNAL_STANDARD_AUDIT=NOT_AUDITED");
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
                    status.setText("Relative calibration failed · " + e.getMessage());
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
        status.setText("Narration starts in " + value + "…");
        ui.postDelayed(() -> countdown(value - 1), 1000);
    }

    private void startRecording(boolean narration) {
        if (processingGate.busy() || calibrationRunning || evidenceRunning) {
            status.setText("REC_BLOCKED — mastering, calibration or proof is active");
            return;
        }
        if (recorder != null) {
            status.setText("Recording is already active.");
            return;
        }

        try {
            // Prevent the previous master from contaminating a new microphone capture.
            playback.stop();
            // Every capture owns its PCM; second-resolution names could overwrite earlier evidence.
            recordedPcm = File.createTempFile("rafaelia_raw_", "_48k.pcm", getCacheDir());
            recorder = new AudioRecorderEngine(this, recordedPcm);
            recorder.start();

            telemetryRunning = true;
            ui.removeCallbacks(telemetryTick);
            ui.post(telemetryTick);

            if (narration) {
                if (scriptEdit.getText().toString().trim().length() > 0 &&
                        "Teleprompter".contentEquals(prompterText.getText())) {
                    loadPrompter();
                }
                prompterScroll.scrollTo(0, 0);
                startPrompter();
            }
        } catch (Exception e) {
            if (recorder != null) recorder.stop();
            recorder = null;
            telemetryRunning = false;
            status.setText("Recording start failed · " + e.getMessage());
        }
    }

    private void stopAndMaster() {
        if (recorder == null) {
            status.setText("No active recording.");
            return;
        }

        AudioRecorderEngine.CaptureStats capture = recorder.getStats();
        int sampleRate = recorder.getSampleRate();
        int channels = recorder.getChannels();

        telemetryRunning = false;
        ui.removeCallbacks(telemetryTick);
        stopPrompter();
        boolean captureComplete = recorder.stop();
        recorder = null;
        if (!captureComplete) {
            status.setText("CAPTURE_INVALID — read/write failure or worker not drained. " +
                    "Raw PCM preserved for diagnostics; mastering blocked.");
            return;
        }

        status.setText(
                "CAPTURE CLOSED · " + capture.source +
                "\npeak=" + capture.peak +
                " · rms=" + capture.rms +
                " · clipped=" + capture.clipped +
                "\nMastering…");

        runPipelineFromPcm(recordedPcm, sampleRate, channels, "microphone");
    }

    private int selectedPreset() {
        int position = profileSpinner.getSelectedItemPosition();
        if (position == 0) return NativeDsp.PRESET_WHATSAPP_VOICE;
        if (position == 3) return NativeDsp.PRESET_MUSIC_CLEAN;
        return NativeDsp.PRESET_NATURAL_VOICE;
    }

    private long selectedTargetEnergy() {
        int position = profileSpinner.getSelectedItemPosition();
        if (position == 0) return NativeDsp.TARGET_MOBILE_Q36;
        if (position == 2) return NativeDsp.TARGET_EBU_R128_Q36;
        return NativeDsp.TARGET_NARRATION_Q36;
    }

    private String selectedTargetLabel() {
        int position = profileSpinner.getSelectedItemPosition();
        if (position == 0) return "MOBILE_VOICE_TARGET";
        if (position == 1) return "NARRATION_TARGET";
        if (position == 2) return "BROADCAST_TARGET_NOT_AUDITED";
        return "MUSIC_CLEAN_TARGET";
    }

    private void runPipelineFromPcm(
            File input, int sampleRate, int channels, String origin) {
        // Single-flight bounds the platform JNI global DSP and meter state.
        // Reject parallel master/calibration/proof; never overwrite the previous good master.
        if (recorder != null || calibrationRunning || evidenceRunning ||
                !processingGate.tryEnter()) {
            if ("imported audio".equals(origin) && input != null) input.delete();
            status.setText("MASTER_BLOCKED — another audio/proof session is active");
            return;
        }
        final int preset = selectedPreset();
        final long target = selectedTargetEnergy();
        final String targetLabel = selectedTargetLabel();
        status.setText("PROCESSING " + origin + " · " + targetLabel + "…");

        try {
            new Thread(() -> {
                File mastered = null;
                File zrf = null;
                Uri outputUri = null;
                boolean committed = false;

                try {
                    mastered = File.createTempFile("rafaelia_mastered_", "_48k.pcm", getCacheDir());
                    zrf = File.createTempFile("rafaelia_session_", ".zrf", getCacheDir());
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
                    if (zrf != null) zrf.delete();
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
                    throw new IllegalStateException("MediaStore insert returned null");
                }

                try (ParcelFileDescriptor pfd =
                             getContentResolver().openFileDescriptor(outputUri, "w")) {
                    if (pfd == null) {
                        throw new IllegalStateException("Output file descriptor unavailable");
                    }
                    AudioPipeline.encodeOpusOgg(
                            result.pcmFile,
                            pfd.getFileDescriptor(),
                            sampleRate,
                            channels);
                }

                publishOutput(outputUri);
                final Uri publishedUri = outputUri;
                final File completeMaster = result.pcmFile;
                committed = true;
                runOnUiThread(() -> {
                    lastOutput = publishedUri;
                    lastMasteredPcm = completeMaster;
                    lastMasteredRate = sampleRate;
                    lastMasteredChannels = channels;
                });

                int gainPct = (int) ((result.gainQ30 * 100L) >> 30);
                long fullScaleQ16 = 32768L << 16;
                long tpPermille = result.truePeakAfterQ16 <= 0 ? 0 :
                        result.truePeakAfterQ16 * 1000L / fullScaleQ16;

                final Uri finalOutputUri = outputUri;
                runOnUiThread(() -> {
                    if (studioWorkspaceView != null) {
                        studioWorkspaceView.setSpectrum(result.spectrum);
                        studioWorkspaceView.setMasterState(
                                "MASTER=" + targetLabel +
                                " | blocks=" + result.gatedBlocks +
                                " | gain=" + gainPct + "%");
                    }
                    status.setText(
                            "MASTER GENERATED · " + targetLabel +
                            "\ngain=" + gainPct + "% · gated_blocks=" + result.gatedBlocks +
                            " · estimated_true_peak=" + tpPermille + "‰ FS" +
                            "\nOgg/Opus=" + finalOutputUri +
                            "\nEXTERNAL_STANDARD_AUDIT=NOT_AUDITED");
                });
                } catch (Exception e) {
                    if (outputUri != null) {
                        getContentResolver().delete(outputUri, null, null);
                    }
                    runOnUiThread(() ->
                            status.setText("Pipeline failed · " + e.getMessage()));
                } finally {
                    if (!committed && mastered != null) mastered.delete();
                    if ("imported audio".equals(origin) && input != null) input.delete();
                    processingGate.leave();
                }
            }, "rafaelia-mastering").start();
        } catch (RuntimeException e) {
            processingGate.leave();
            status.setText("MASTER_START_FAILED — " + e.getClass().getSimpleName());
        }
    }

    private void playMaster() {
        if (recorder != null || calibrationRunning || evidenceRunning || processingGate.busy()) {
            status.setText("PLAYBACK_BLOCKED — capture, calibration, proof or master active");
            return;
        }
        if (lastMasteredPcm == null || !lastMasteredPcm.exists()) {
            status.setText("No master PCM is available for playback yet.");
            return;
        }
        try {
            playback.play(
                    lastMasteredPcm, lastMasteredRate, lastMasteredChannels);
            status.setText("PLAYING · master PCM");
        } catch (Exception e) {
            status.setText("Playback failed · " + e.getMessage());
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
        exportMasterInterop(AudioInteropWriter.Format.RAW_PCM);
    }

    private void exportMasterWav() {
        exportMasterInterop(AudioInteropWriter.Format.WAV_PCM16);
    }

    private void exportMasterAiff() {
        exportMasterInterop(AudioInteropWriter.Format.AIFF_PCM16);
    }

    private void exportMasterAu() {
        exportMasterInterop(AudioInteropWriter.Format.AU_PCM16);
    }

    private void exportMasterCaf() {
        exportMasterInterop(AudioInteropWriter.Format.CAF_PCM16);
    }

    private void exportMasterInterop(AudioInteropWriter.Format format) {
        if (lastMasteredPcm == null || !lastMasteredPcm.isFile()) {
            status.setText("No master PCM is available to export yet.");
            return;
        }

        boolean raw = format == AudioInteropWriter.Format.RAW_PCM;
        String stamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.US).format(new Date());
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,
                "Rafaelia_Master_" + stamp + format.extension);
        values.put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                raw ? Environment.DIRECTORY_DOWNLOADS + "/RafaeliaAudio/Raw" :
                        Environment.DIRECTORY_MUSIC + "/RafaeliaAudio");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri collection = raw ?
                MediaStore.Downloads.EXTERNAL_CONTENT_URI :
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri uri = getContentResolver().insert(collection, values);
        if (uri == null) {
            status.setText("Export destination could not be created.");
            return;
        }

        new Thread(() -> {
            try (java.io.OutputStream out =
                         getContentResolver().openOutputStream(uri, "w")) {
                if (out == null) throw new IllegalStateException("Output stream unavailable");
                long bytes = AudioInteropWriter.write(
                        format,
                        lastMasteredPcm,
                        out,
                        lastMasteredRate,
                        lastMasteredChannels);

                ContentValues done = new ContentValues();
                done.put(MediaStore.MediaColumns.IS_PENDING, 0);
                getContentResolver().update(uri, done, null, null);
                runOnUiThread(() -> status.setText(
                        format.label + " exported · " + bytes + " bytes"));
            } catch (Exception e) {
                getContentResolver().delete(uri, null, null);
                runOnUiThread(() ->
                        status.setText(format.label + " export failed · " + e.getMessage()));
            }
        }, "rafaelia-" + format.name().toLowerCase(Locale.US) + "-export").start();
    }

    private void shareLast() {
        if (lastOutput == null) {
            status.setText("No processed output is available to share yet.");
            return;
        }

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("audio/ogg");
        send.putExtra(Intent.EXTRA_STREAM, lastOutput);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Share processed audio"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK && resultCode == RESULT_OK &&
                data != null && data.getData() != null) {
            Uri uri = data.getData();
            status.setText("DECODING selected audio…");

            new Thread(() -> {
                try {
                    File pcm = File.createTempFile("rafaelia_imported_", ".pcm", getCacheDir());
                    AudioPipeline.DecodedAudio decoded =
                            AudioPipeline.decodeToPcm(this, uri, pcm);
                    runOnUiThread(() -> runPipelineFromPcm(
                            decoded.pcmFile,
                            decoded.sampleRate,
                            decoded.channels,
                            "imported audio"));
                } catch (Exception e) {
                    runOnUiThread(() ->
                            status.setText("Decode failed · " + e.getMessage()));
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
            pendingNarration = false;
            status.setText("Microphone permission denied.");
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
