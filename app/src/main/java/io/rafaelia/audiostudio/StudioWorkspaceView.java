/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 *
 * Platform edge only: renders executed/observed state from auditable cores.
 * Decorative placeholders are intentionally excluded from the primary surface.
 */
package io.rafaelia.audiostudio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

final class StudioWorkspaceView extends View {
    interface ActionListener {
        void onRecord();
        void onStopAndMaster();
        void onPlayMaster();
        void onRunRelativeCalibration();
        void onGenerateEvidence();
    }

    private static final int SECTION_DASHBOARD = 0;
    private static final int SECTION_MEASURE = 1;
    private static final int SECTION_SESSION = 2;
    private static final int SECTION_EVIDENCE = 3;
    private static final int SECTION_SYSTEM = 4;

    private static final String[] SECTIONS = {
            "DASH", "MEASURE", "SESSION", "EVIDENCE", "SYSTEM"
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thin = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final RectF[] navTargets = {
            new RectF(), new RectF(), new RectF(), new RectF(), new RectF()
    };

    private final RectF dashSignal = new RectF();
    private final RectF dashInput = new RectF();
    private final RectF dashCalibration = new RectF();
    private final RectF dashEvidence = new RectF();
    private final RectF dashWave = new RectF();

    private final RectF transportRecord = new RectF();
    private final RectF transportStop = new RectF();
    private final RectF transportPlay = new RectF();
    private final RectF transportCal = new RectF();
    private final RectF transportEvidence = new RectF();
    private final RectF calibrationAction = new RectF();
    private final RectF evidenceAction = new RectF();

    private final short[] waveMin = new short[128];
    private final short[] waveMax = new short[128];
    private final long[] spectrum = new long[16];
    private final long[] calibrationPowerRatioQ20 = new long[16];

    private int section = SECTION_DASHBOARD;
    private int waveBins;
    private int peakPct;
    private int rmsPct;
    private long clipped;
    private long capturedSamples;
    private int calibrationValidBands;

    private String inputSource = "INPUT_IDLE";
    private String masterState = "MASTER=PENDING";
    private String calibrationState = "CAL=RELATIVE_ONLY";
    private String roomAnalysisState = "ROOM=NOT_RUN";
    private String containerState = "ZRF/CFR=IMPLEMENTED_UNTESTED";
    private String systemState = "SENSORS=OBSERVE | PERMISSIONS=MINIMAL";
    private String originState = "ORIGIN=PROJECT+PLATFORM+TOOLCHAIN";
    private String signatureState = "SIGNATURE=TOKEN_VAZIO";

    private ActionListener actionListener;

    StudioWorkspaceView(Context context) {
        super(context);
        paint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.MONOSPACE,
                android.graphics.Typeface.NORMAL));
        thin.setTypeface(paint.getTypeface());
        thin.setStyle(Paint.Style.STROKE);
        setMinimumHeight(dp(500));
        setFocusable(true);
        setClickable(true);
        setContentDescription("RAFAELIA Audio measurement and evidence workspace");
    }

    void setActionListener(ActionListener listener) {
        actionListener = listener;
    }

    void setCaptureStats(int peak, int rms, long clip, long samples, String source) {
        peakPct = boundPercent(peak);
        rmsPct = boundPercent(rms);
        clipped = clip < 0L ? 0L : clip;
        capturedSamples = samples < 0L ? 0L : samples;
        inputSource = source == null ? "INPUT_UNAVAILABLE" : source;
        invalidate();
    }

    void setWaveform(short[] samples, int count) {
        if (samples == null || count <= 0) {
            waveBins = 0;
            invalidate();
            return;
        }
        if (count > samples.length) count = samples.length;
        int bins = Math.min(waveMin.length, count);
        int i;
        for (i = 0; i < bins; ++i) {
            int start = (int) (((long) i * count) / bins);
            int end = (int) (((long) (i + 1) * count) / bins);
            if (end <= start) end = start + 1;
            if (end > count) end = count;
            short lo = 32767;
            short hi = -32768;
            int j;
            for (j = start; j < end; ++j) {
                short value = samples[j];
                if (value < lo) lo = value;
                if (value > hi) hi = value;
            }
            waveMin[i] = lo;
            waveMax[i] = hi;
        }
        waveBins = bins;
        invalidate();
    }

    void setSpectrum(long[] values) {
        if (values == null) return;
        int i;
        for (i = 0; i < spectrum.length; ++i) {
            spectrum[i] = i < values.length && values[i] > 0L ? values[i] : 0L;
        }
        invalidate();
    }

    void setMasterState(String state) {
        masterState = state == null ? "MASTER=UNAVAILABLE" : state;
        invalidate();
    }

    void setCalibrationState(String state) {
        calibrationState = state == null ? "CAL=UNAVAILABLE" : state;
        invalidate();
    }

    void setCalibrationAnalysis(long[] powerRatiosQ20, int validBands, String roomState) {
        int i;
        for (i = 0; i < calibrationPowerRatioQ20.length; ++i) {
            calibrationPowerRatioQ20[i] =
                    powerRatiosQ20 != null && i < powerRatiosQ20.length && powerRatiosQ20[i] > 0L
                            ? powerRatiosQ20[i] : 0L;
        }
        calibrationValidBands = validBands < 0 ? 0
                : Math.min(validBands, calibrationPowerRatioQ20.length);
        roomAnalysisState = roomState == null ? "ROOM=UNAVAILABLE" : roomState;
        invalidate();
    }

    void setContainerState(String state) {
        containerState = state == null ? "ZRF/CFR=UNAVAILABLE" : state;
        invalidate();
    }

    void setSystemState(String state) {
        systemState = state == null ? "SENSORS=UNAVAILABLE" : state;
        invalidate();
    }

    void setOriginState(String state) {
        originState = state == null ? "ORIGIN=UNAVAILABLE" : state;
        invalidate();
    }

    void setSignatureState(String state) {
        signatureState = state == null ? "SIGNATURE=UNAVAILABLE" : state;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int wanted = width <= dp(360) ? dp(520) : dp(560);
        setMeasuredDimension(
                resolveSize(width, widthMeasureSpec),
                resolveSize(wanted, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float navH = dp(44);
        float headerH = dp(62);
        float transportH = dp(60);
        float contentTop = navH + headerH;
        float contentBottom = height - transportH;

        paint.setStyle(Paint.Style.FILL);
        paint.setARGB(255, 10, 13, 17);
        canvas.drawRect(0f, 0f, width, height, paint);

        drawPrimaryNavigation(canvas, width, navH);
        drawHeader(canvas, width, navH, headerH);

        if (section == SECTION_DASHBOARD) {
            drawDashboard(canvas, width, contentTop, contentBottom);
        } else if (section == SECTION_MEASURE) {
            drawMeasure(canvas, width, contentTop, contentBottom);
        } else if (section == SECTION_SESSION) {
            drawSession(canvas, width, contentTop, contentBottom);
        } else if (section == SECTION_EVIDENCE) {
            drawEvidence(canvas, width, contentTop, contentBottom);
        } else {
            drawSystem(canvas, width, contentTop, contentBottom);
        }

        drawTransport(canvas, width, height - transportH, transportH);
    }

    private void drawPrimaryNavigation(Canvas canvas, float width, float height) {
        float slot = width / SECTIONS.length;
        paint.setTextAlign(Paint.Align.CENTER);
        int i;
        for (i = 0; i < SECTIONS.length; ++i) {
            float left = i * slot;
            float right = left + slot;
            navTargets[i].set(left, 0f, right, height);
            paint.setARGB(255,
                    i == section ? 22 : 12,
                    i == section ? 37 : 17,
                    i == section ? 48 : 22);
            canvas.drawRect(left, 0f, right, height, paint);
            if (i == section) {
                paint.setARGB(255, 81, 177, 202);
                canvas.drawRect(left + dp(9), height - dp(2), right - dp(9), height, paint);
            }
            drawText(canvas, SECTIONS[i], left + slot * 0.5f, height * 0.63f,
                    width <= dp(360) ? 8 : 9,
                    i == section ? 235 : 146,
                    i == section ? 241 : 160,
                    i == section ? 245 : 171);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawHeader(Canvas canvas, float width, float top, float height) {
        String title;
        String subtitle;
        if (section == SECTION_DASHBOARD) {
            title = "RAFAELIA AUDIO";
            subtitle = "MEASUREMENT · EVIDENCE · DEVICE QA";
        } else if (section == SECTION_MEASURE) {
            title = "MEASUREMENT";
            subtitle = "SPECTRUM · RELATIVE CAL · ROOM";
        } else if (section == SECTION_SESSION) {
            title = "SESSION";
            subtitle = "CAPTURE · SOURCE · MASTER";
        } else if (section == SECTION_EVIDENCE) {
            title = "EVIDENCE";
            subtitle = "ZRF · CFR · RECEIPT · PROVENANCE";
        } else {
            title = "SYSTEM";
            subtitle = "DEVICE · SENSORS · ORIGIN";
        }

        drawText(canvas, title, dp(14), top + dp(24), 14, 236, 242, 247);
        drawText(canvas, subtitle, dp(14), top + dp(44), 8, 124, 145, 160);

        paint.setTextAlign(Paint.Align.RIGHT);
        drawText(canvas, sessionTime(), width - dp(14), top + dp(26), 14, 224, 231, 236);
        paint.setTextAlign(Paint.Align.LEFT);

        paint.setARGB(255, 22, 28, 34);
        canvas.drawRect(0f, top + height - dp(1), width, top + height, paint);
    }

    private void drawDashboard(Canvas canvas, float width, float top, float bottom) {
        float gap = dp(8);
        float margin = dp(10);
        float cardW = (width - margin * 2f - gap) * 0.5f;
        float cardH = dp(108);
        float x1 = margin;
        float x2 = margin + cardW + gap;
        float y1 = top + dp(8);
        float y2 = y1 + cardH + gap;

        dashSignal.set(x1, y1, x1 + cardW, y1 + cardH);
        drawCard(canvas, dashSignal, "SIGNAL", true);
        drawBigMetric(canvas, peakPct + "%", "PEAK FS", x1 + dp(10), y1 + dp(48));
        drawSmallMetric(canvas, "RMS", rmsPct + "%", x1 + dp(10), y1 + dp(77));
        drawSmallMetric(canvas, "CLIP", Long.toString(clipped), x1 + cardW * 0.54f, y1 + dp(77));

        dashInput.set(x2, y1, x2 + cardW, y1 + cardH);
        drawCard(canvas, dashInput, "INPUT", true);
        drawStateLine(canvas, inputSource, x2 + dp(10), y1 + dp(45), cardW - dp(20), 9);
        drawStateLine(canvas, "48 kHz · PCM16", x2 + dp(10), y1 + dp(69), cardW - dp(20), 9);
        drawStatus(canvas, waveBins > 0 ? "LIVE" : "READY",
                x2 + dp(10), y1 + dp(82), waveBins > 0);

        dashCalibration.set(x1, y2, x1 + cardW, y2 + cardH);
        drawCard(canvas, dashCalibration, "CALIBRATION", true);
        drawStateLine(canvas, calibrationState, x1 + dp(10), y2 + dp(45), cardW - dp(20), 8);
        drawSmallMetric(canvas, "BANDS", Integer.toString(calibrationValidBands),
                x1 + dp(10), y2 + dp(79));
        drawStatus(canvas, calibrationValidBands > 0 ? "OBSERVED" : "RELATIVE",
                x1 + cardW * 0.47f, y2 + dp(69), calibrationValidBands > 0);

        dashEvidence.set(x2, y2, x2 + cardW, y2 + cardH);
        drawCard(canvas, dashEvidence, "EVIDENCE", true);
        drawStateLine(canvas, containerState, x2 + dp(10), y2 + dp(45), cardW - dp(20), 8);
        drawStateLine(canvas, signatureState, x2 + dp(10), y2 + dp(68), cardW - dp(20), 8);
        drawStatus(canvas, containsGap(signatureState) ? "OPEN GAP" : "BOUND",
                x2 + dp(10), y2 + dp(82), !containsGap(signatureState));

        float waveTop = y2 + cardH + dp(10);
        float waveBottom = bottom - dp(10);
        dashWave.set(margin, waveTop, width - margin, waveBottom);
        if (waveBottom - waveTop > dp(62)) {
            drawPanel(canvas, dashWave);
            drawText(canvas, "LIVE WAVEFORM   › SESSION",
                    margin + dp(10), waveTop + dp(20), 8, 126, 148, 164);
            drawWave(canvas,
                    margin + dp(10), waveTop + dp(30),
                    width - margin - dp(10), waveBottom - dp(10));
        }
    }

    private void drawMeasure(Canvas canvas, float width, float top, float bottom) {
        float margin = dp(10);
        float y = top + dp(8);
        float chartH = dp(144);
        RectF chart = new RectF(margin, y, width - margin, y + chartH);
        drawPanel(canvas, chart);
        drawText(canvas, "16-BAND SPECTRUM · RELATIVE", margin + dp(10), y + dp(20),
                8, 126, 148, 164);
        if (hasSpectrum()) {
            drawSpectrumBars(canvas,
                    margin + dp(10), y + dp(32),
                    width - margin - dp(10), y + chartH - dp(10));
        } else {
            drawText(canvas, "AVAILABLE AFTER MASTER", margin + dp(10), y + dp(82),
                    9, 132, 148, 159);
        }

        y += chartH + dp(8);
        float calH = dp(146);
        RectF cal = new RectF(margin, y, width - margin, y + calH);
        drawPanel(canvas, cal);
        drawText(canvas, "RELATIVE SWEEP PROFILE", margin + dp(10), y + dp(20),
                8, 126, 148, 164);
        drawCalibrationProfile(canvas,
                margin + dp(10), y + dp(32),
                width - margin - dp(10), y + calH - dp(34));
        drawStateLine(canvas, roomAnalysisState,
                margin + dp(10), y + calH - dp(13), width - margin * 2f - dp(20), 8);

        y += calH + dp(8);
        calibrationAction.set(margin, y, width - margin, Math.min(bottom - dp(8), y + dp(42)));
        drawActionButton(canvas, calibrationAction,
                "RUN RELATIVE CAL · SAFE LEVEL", 34, 64, 78);
    }

    private void drawSession(Canvas canvas, float width, float top, float bottom) {
        float margin = dp(10);
        float y = top + dp(8);
        float waveH = dp(164);
        RectF wave = new RectF(margin, y, width - margin, y + waveH);
        drawPanel(canvas, wave);
        drawText(canvas, "SOURCE WAVEFORM · RAW PRESERVED", margin + dp(10), y + dp(20),
                8, 126, 148, 164);
        drawWave(canvas,
                margin + dp(10), y + dp(32),
                width - margin - dp(10), y + waveH - dp(12));

        y += waveH + dp(8);
        RectF state = new RectF(margin, y, width - margin, y + dp(104));
        drawPanel(canvas, state);
        drawText(canvas, "SESSION STATE", margin + dp(10), y + dp(20),
                8, 126, 148, 164);
        drawStateLine(canvas, inputSource, margin + dp(10), y + dp(45),
                width - margin * 2f - dp(20), 9);
        drawStateLine(canvas, masterState, margin + dp(10), y + dp(68),
                width - margin * 2f - dp(20), 8);
        drawSmallMetric(canvas, "SAMPLES", Long.toString(capturedSamples),
                margin + dp(10), y + dp(92));

        y += dp(112);
        RectF flow = new RectF(margin, y, width - margin, Math.min(bottom - dp(8), y + dp(70)));
        drawPanel(canvas, flow);
        drawText(canvas, "CAPTURE → RAW → MASTER → EXPORT",
                margin + dp(10), y + dp(32), 9, 205, 214, 221);
        drawText(canvas, "source bytes remain distinct from derived output",
                margin + dp(10), y + dp(53), 8, 124, 145, 160);
    }

    private void drawEvidence(Canvas canvas, float width, float top, float bottom) {
        float margin = dp(10);
        float y = top + dp(8);

        RectF custody = new RectF(margin, y, width - margin, y + dp(128));
        drawPanel(canvas, custody);
        drawText(canvas, "CUSTODY", margin + dp(10), y + dp(20), 8, 126, 148, 164);
        drawStateLine(canvas, containerState, margin + dp(10), y + dp(48),
                width - margin * 2f - dp(20), 8);
        drawStateLine(canvas, originState, margin + dp(10), y + dp(72),
                width - margin * 2f - dp(20), 8);
        drawStateLine(canvas, signatureState, margin + dp(10), y + dp(96),
                width - margin * 2f - dp(20), 8);
        drawStatus(canvas, containsGap(signatureState) ? "SIGNATURE GAP" : "SIGNATURE PRESENT",
                margin + dp(10), y + dp(105), !containsGap(signatureState));

        y += dp(136);
        RectF policy = new RectF(margin, y, width - margin, y + dp(108));
        drawPanel(canvas, policy);
        drawText(canvas, "EVIDENCE POLICY", margin + dp(10), y + dp(20),
                8, 126, 148, 164);
        drawText(canvas, "SOURCE ≠ ARTIFACT ≠ EXECUTION",
                margin + dp(10), y + dp(46), 9, 212, 221, 228);
        drawText(canvas, "EXECUTION ≠ EVIDENCE ≠ CLAIM",
                margin + dp(10), y + dp(67), 9, 212, 221, 228);
        drawText(canvas, "TOKEN_VAZIO ≠ 0 · UNTESTED ≠ PASS",
                margin + dp(10), y + dp(89), 8, 164, 180, 191);

        y += dp(116);
        evidenceAction.set(margin, y, width - margin, Math.min(bottom - dp(8), y + dp(42)));
        drawActionButton(canvas, evidenceAction, "VALIDATE + GENERATE ZIPRAF", 34, 64, 78);
    }

    private void drawSystem(Canvas canvas, float width, float top, float bottom) {
        float margin = dp(10);
        float y = top + dp(8);

        RectF device = new RectF(margin, y, width - margin, y + dp(142));
        drawPanel(canvas, device);
        drawText(canvas, "DEVICE / SENSOR STATE", margin + dp(10), y + dp(20),
                8, 126, 148, 164);
        drawStateLine(canvas, systemState, margin + dp(10), y + dp(48),
                width - margin * 2f - dp(20), 9);
        drawStateLine(canvas, "MIC · RECORD_AUDIO explicit",
                margin + dp(10), y + dp(76), width - margin * 2f - dp(20), 8);
        drawStateLine(canvas, "ACCEL / MAG / LIGHT / PROX · hardware availability",
                margin + dp(10), y + dp(99), width - margin * 2f - dp(20), 8);
        drawStateLine(canvas, "μ∆ observation runs only during explicit PROOF",
                margin + dp(10), y + dp(122), width - margin * 2f - dp(20), 8);

        y += dp(150);
        RectF origin = new RectF(margin, y, width - margin, Math.min(bottom - dp(8), y + dp(126)));
        drawPanel(canvas, origin);
        drawText(canvas, "ORIGIN / SIGNATURE", margin + dp(10), y + dp(20),
                8, 126, 148, 164);
        drawStateLine(canvas, originState, margin + dp(10), y + dp(50),
                width - margin * 2f - dp(20), 8);
        drawStateLine(canvas, signatureState, margin + dp(10), y + dp(76),
                width - margin * 2f - dp(20), 8);
        drawStatus(canvas, containsGap(signatureState) ? "TOKEN_VAZIO" : "BOUND",
                margin + dp(10), y + dp(91), !containsGap(signatureState));
    }

    private void drawTransport(Canvas canvas, float width, float top, float height) {
        paint.setARGB(255, 7, 9, 12);
        canvas.drawRect(0f, top, width, top + height, paint);
        paint.setARGB(255, 28, 34, 40);
        canvas.drawRect(0f, top, width, top + dp(1), paint);

        float gap = dp(5);
        float margin = dp(7);
        float slot = (width - margin * 2f - gap * 4f) / 5f;
        float y1 = top + dp(8);
        float y2 = top + height - dp(8);

        transportRecord.set(margin, y1, margin + slot, y2);
        transportStop.set(transportRecord.right + gap, y1,
                transportRecord.right + gap + slot, y2);
        transportPlay.set(transportStop.right + gap, y1,
                transportStop.right + gap + slot, y2);
        transportCal.set(transportPlay.right + gap, y1,
                transportPlay.right + gap + slot, y2);
        transportEvidence.set(transportCal.right + gap, y1,
                transportCal.right + gap + slot, y2);

        drawTransportButton(canvas, transportRecord, "REC", 87, 38, 43);
        drawTransportButton(canvas, transportStop, "STOP/MASTER", 42, 49, 57);
        drawTransportButton(canvas, transportPlay, "PLAY", 31, 62, 51);
        drawTransportButton(canvas, transportCal, "CAL", 33, 62, 75);
        drawTransportButton(canvas, transportEvidence, "PROOF", 38, 54, 69);
    }

    private void drawTransportButton(
            Canvas canvas, RectF target, String label, int r, int g, int b) {
        paint.setARGB(255, r, g, b);
        canvas.drawRoundRect(target, dp(7), dp(7), paint);
        paint.setTextAlign(Paint.Align.CENTER);
        drawText(canvas, label, target.centerX(), target.centerY() + dp(4),
                label.length() > 7 ? 7 : 9, 238, 243, 246);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawCard(Canvas canvas, RectF target, String title, boolean navigable) {
        paint.setARGB(255, 17, 22, 27);
        canvas.drawRoundRect(target, dp(9), dp(9), paint);
        thin.setStrokeWidth(dp(1));
        thin.setARGB(255, 31, 42, 50);
        canvas.drawRoundRect(target, dp(9), dp(9), thin);
        drawText(canvas, title + (navigable ? "   ›" : ""),
                target.left + dp(10), target.top + dp(20), 8, 111, 137, 153);
    }

    private void drawPanel(Canvas canvas, RectF target) {
        paint.setARGB(255, 15, 20, 25);
        canvas.drawRoundRect(target, dp(9), dp(9), paint);
        thin.setStrokeWidth(dp(1));
        thin.setARGB(255, 28, 38, 46);
        canvas.drawRoundRect(target, dp(9), dp(9), thin);
    }

    private void drawBigMetric(Canvas canvas, String value, String label, float x, float y) {
        drawText(canvas, value, x, y, 20, 232, 239, 243);
        drawText(canvas, label, x, y + dp(18), 8, 114, 139, 154);
    }

    private void drawSmallMetric(Canvas canvas, String label, String value, float x, float y) {
        drawText(canvas, label + "  " + value, x, y, 9, 176, 190, 199);
    }

    private void drawStatus(Canvas canvas, String label, float x, float y, boolean positive) {
        paint.setTextSize(dp(8));
        float width = paint.measureText(label) + dp(14);
        paint.setARGB(255,
                positive ? 24 : 49,
                positive ? 66 : 47,
                positive ? 52 : 31);
        rect.set(x, y, x + width, y + dp(20));
        canvas.drawRoundRect(rect, dp(10), dp(10), paint);
        drawText(canvas, label, x + dp(7), y + dp(14), 8,
                positive ? 151 : 219,
                positive ? 220 : 181,
                positive ? 184 : 120);
    }

    private void drawActionButton(Canvas canvas, RectF target, String label, int r, int g, int b) {
        if (target.bottom <= target.top) return;
        paint.setARGB(255, r, g, b);
        canvas.drawRoundRect(target, dp(8), dp(8), paint);
        paint.setTextAlign(Paint.Align.CENTER);
        drawText(canvas, label, target.centerX(), target.centerY() + dp(4),
                9, 236, 242, 246);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawStateLine(
            Canvas canvas, String value, float x, float y, float maxWidth, int size) {
        String text = value == null ? "TOKEN_VAZIO" : value;
        drawText(canvas, fitText(text, maxWidth, size), x, y, size, 185, 197, 205);
    }

    private String fitText(String text, float maxWidth, int size) {
        if (text == null) return "TOKEN_VAZIO";
        paint.setTextSize(dp(size));
        if (paint.measureText(text) <= maxWidth) return text;
        String suffix = "…";
        int end = text.length();
        while (end > 1 && paint.measureText(text.substring(0, end) + suffix) > maxWidth) {
            --end;
        }
        return text.substring(0, end) + suffix;
    }

    private void drawText(
            Canvas canvas, String text, float x, float y, int size,
            int r, int g, int b) {
        paint.setTextSize(dp(size));
        paint.setARGB(255, r, g, b);
        canvas.drawText(text == null ? "TOKEN_VAZIO" : text, x, y, paint);
    }

    private void drawWave(Canvas canvas, float left, float top, float right, float bottom) {
        float mid = (top + bottom) * 0.5f;
        thin.setStrokeWidth(dp(1));
        thin.setARGB(255, 38, 50, 59);
        canvas.drawLine(left, mid, right, mid, thin);
        if (waveBins <= 0) {
            drawText(canvas, "waveform · waiting for capture", left + dp(6), mid + dp(4),
                    8, 112, 132, 145);
            return;
        }
        thin.setARGB(255, 91, 202, 174);
        float width = right - left;
        float half = (bottom - top) * 0.44f;
        int i;
        for (i = 0; i < waveBins; ++i) {
            int denominator = waveBins > 1 ? waveBins - 1 : 1;
            float x = left + width * i / (float) denominator;
            float y1 = mid - (waveMax[i] / 32768f) * half;
            float y2 = mid - (waveMin[i] / 32768f) * half;
            canvas.drawLine(x, y1, x, y2, thin);
        }
    }

    private void drawSpectrumBars(Canvas canvas, float left, float top, float right, float bottom) {
        long max = 1L;
        int i;
        for (i = 0; i < spectrum.length; ++i) {
            if (spectrum[i] > max) max = spectrum[i];
        }
        float slot = (right - left) / spectrum.length;
        paint.setARGB(255, 84, 173, 205);
        for (i = 0; i < spectrum.length; ++i) {
            float ratio = (float) ((double) spectrum[i] / (double) max);
            float barH = (bottom - top) * ratio;
            canvas.drawRect(
                    left + i * slot + dp(2), bottom - barH,
                    left + (i + 1) * slot - dp(2), bottom, paint);
        }
    }

    private void drawCalibrationProfile(
            Canvas canvas, float left, float top, float right, float bottom) {
        float mid = (top + bottom) * 0.5f;
        thin.setStrokeWidth(dp(1));
        thin.setARGB(255, 38, 50, 59);
        canvas.drawLine(left, mid, right, mid, thin);
        if (calibrationValidBands <= 0) {
            drawText(canvas, "relative profile · run calibration to populate",
                    left + dp(6), mid + dp(4), 8, 112, 132, 145);
            return;
        }

        float slot = (right - left) / (calibrationPowerRatioQ20.length - 1);
        paint.setARGB(255, 91, 180, 212);
        boolean haveLast = false;
        float lastX = left;
        float lastY = mid;
        int i;
        for (i = 0; i < calibrationPowerRatioQ20.length; ++i) {
            long raw = calibrationPowerRatioQ20[i];
            if (raw <= 0L) {
                haveLast = false;
                continue;
            }
            double powerRatio = (double) raw / 1048576.0;
            double db = 10.0 * Math.log10(powerRatio);
            if (db > 24.0) db = 24.0;
            if (db < -24.0) db = -24.0;
            float x = left + slot * i;
            float y = mid - (float) db * (bottom - top) / 48.0f;
            if (haveLast) canvas.drawLine(lastX, lastY, x, y, paint);
            canvas.drawCircle(x, y, dp(2), paint);
            lastX = x;
            lastY = y;
            haveLast = true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float x = event.getX();
        float y = event.getY();

        int i;
        for (i = 0; i < navTargets.length; ++i) {
            if (navTargets[i].contains(x, y)) {
                switchSection(i);
                return true;
            }
        }

        if (section == SECTION_DASHBOARD) {
            if (dashSignal.contains(x, y) || dashWave.contains(x, y)) {
                switchSection(SECTION_SESSION);
                return true;
            }
            if (dashInput.contains(x, y)) {
                switchSection(SECTION_SYSTEM);
                return true;
            }
            if (dashCalibration.contains(x, y)) {
                switchSection(SECTION_MEASURE);
                return true;
            }
            if (dashEvidence.contains(x, y)) {
                switchSection(SECTION_EVIDENCE);
                return true;
            }
        }

        if (transportRecord.contains(x, y)) {
            if (actionListener != null) actionListener.onRecord();
            performClick();
            return true;
        }
        if (transportStop.contains(x, y)) {
            if (actionListener != null) actionListener.onStopAndMaster();
            performClick();
            return true;
        }
        if (transportPlay.contains(x, y)) {
            if (actionListener != null) actionListener.onPlayMaster();
            performClick();
            return true;
        }
        if (transportCal.contains(x, y) ||
                (section == SECTION_MEASURE && calibrationAction.contains(x, y))) {
            if (actionListener != null) actionListener.onRunRelativeCalibration();
            performClick();
            return true;
        }
        if (transportEvidence.contains(x, y) ||
                (section == SECTION_EVIDENCE && evidenceAction.contains(x, y))) {
            if (actionListener != null) actionListener.onGenerateEvidence();
            performClick();
            return true;
        }
        return true;
    }

    private void switchSection(int target) {
        section = target;
        invalidate();
        performClick();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private String sessionTime() {
        long seconds = capturedSamples / 48000L;
        long millis = ((capturedSamples % 48000L) * 1000L) / 48000L;
        return two(seconds / 60L) + ":" + two(seconds % 60L) + "." + three(millis);
    }

    private boolean containsGap(String value) {
        return value == null || value.contains("TOKEN_VAZIO") || value.contains("PENDING");
    }

    private boolean hasSpectrum() {
        int i;
        for (i = 0; i < spectrum.length; ++i) {
            if (spectrum[i] > 0L) return true;
        }
        return false;
    }

    private int boundPercent(int value) {
        if (value < 0) return 0;
        if (value > 100) return 100;
        return value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static String two(long value) {
        return value < 10L ? "0" + value : Long.toString(value);
    }

    private static String three(long value) {
        if (value < 10L) return "00" + value;
        if (value < 100L) return "0" + value;
        return Long.toString(value);
    }
}
