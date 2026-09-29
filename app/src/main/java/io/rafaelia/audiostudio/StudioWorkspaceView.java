/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 *
 * Platform edge only: this View renders state produced by the freestanding cores.
 */
package io.rafaelia.audiostudio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

final class StudioWorkspaceView extends View {
    private static final String[] TABS = {
            "REC", "EDIT", "CAL", "SPEC", "ROOM", "VOICE", "MASTER", "EXPORT"
    };
    private static final String[] SCREEN_TITLES = {
            "Recorder / transport",
            "Timeline / waveform",
            "Electroacoustic calibration",
            "Spectrum / transfer",
            "Impulse / room correction",
            "Voice / phoneme / timing",
            "DSP rack / A-B",
            "ZRF / CFR / render"
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thin = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final short[] waveMin = new short[128];
    private final short[] waveMax = new short[128];
    private final long[] spectrum = new long[16];

    private int screen;
    private int waveBins;
    private int peakPct;
    private int rmsPct;
    private long clipped;
    private long capturedSamples;
    private String inputSource = "TOKEN_VAZIO";
    private String masterState = "MASTER=PENDING";
    private String calibrationState = "CAL=RELATIVE_ONLY";
    private String containerState = "ZRF/CFR=IMPLEMENTED_UNTESTED";

    StudioWorkspaceView(Context context) {
        super(context);
        paint.setTypeface(android.graphics.Typeface.MONOSPACE);
        thin.setTypeface(android.graphics.Typeface.MONOSPACE);
        setMinimumHeight(dp(500));
        setFocusable(true);
    }

    void setCaptureStats(int peak, int rms, long clip, long samples, String source) {
        peakPct = peak;
        rmsPct = rms;
        clipped = clip;
        capturedSamples = samples;
        inputSource = source == null ? "TOKEN_VAZIO" : source;
        invalidate();
    }

    void setWaveform(short[] samples, int count) {
        int bins = waveMin.length;
        int i;
        if (samples == null || count <= 0) {
            waveBins = 0;
            invalidate();
            return;
        }
        if (count > samples.length) count = samples.length;
        if (count < bins) bins = count;
        for (i = 0; i < bins; ++i) {
            int start = (int) (((long)i * count) / bins);
            int end = (int) (((long)(i + 1) * count) / bins);
            int j;
            short lo = 32767;
            short hi = -32768;
            if (end <= start) end = start + 1;
            if (end > count) end = count;
            for (j = start; j < end; ++j) {
                short v = samples[j];
                if (v < lo) lo = v;
                if (v > hi) hi = v;
            }
            waveMin[i] = lo;
            waveMax[i] = hi;
        }
        waveBins = bins;
        invalidate();
    }

    void setSpectrum(long[] values) {
        int i;
        if (values == null) return;
        for (i = 0; i < spectrum.length; ++i) {
            spectrum[i] = i < values.length ? values[i] : 0L;
        }
        invalidate();
    }

    void setMasterState(String state) {
        masterState = state == null ? "MASTER=TOKEN_VAZIO" : state;
        invalidate();
    }

    void setCalibrationState(String state) {
        calibrationState = state == null ? "CAL=TOKEN_VAZIO" : state;
        invalidate();
    }

    void setContainerState(String state) {
        containerState = state == null ? "ZRF/CFR=TOKEN_VAZIO" : state;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float tabH = dp(50);
        float bodyTop = tabH + dp(8);

        paint.setStyle(Paint.Style.FILL);
        paint.setARGB(255, 16, 18, 22);
        canvas.drawRect(0, 0, w, h, paint);

        drawTabs(canvas, w, tabH);
        drawHeader(canvas, bodyTop, w);

        if (screen == 0) drawRecorder(canvas, w, h);
        else if (screen == 1) drawEditor(canvas, w, h);
        else if (screen == 2) drawCalibration(canvas, w, h);
        else if (screen == 3) drawSpectrum(canvas, w, h);
        else if (screen == 4) drawRoom(canvas, w, h);
        else if (screen == 5) drawVoice(canvas, w, h);
        else if (screen == 6) drawMaster(canvas, w, h);
        else drawExport(canvas, w, h);
    }

    private void drawTabs(Canvas canvas, float width, float tabH) {
        float slot = width / TABS.length;
        int i;
        paint.setTextSize(dp(11));
        paint.setTextAlign(Paint.Align.CENTER);
        for (i = 0; i < TABS.length; ++i) {
            paint.setARGB(255, i == screen ? 52 : 28, i == screen ? 66 : 31,
                    i == screen ? 82 : 36);
            rect.set(slot * i, 0, slot * (i + 1), tabH);
            canvas.drawRect(rect, paint);
            paint.setARGB(255, 235, 239, 244);
            canvas.drawText(TABS[i], slot * (i + 0.5f), dp(31), paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawHeader(Canvas canvas, float top, float width) {
        paint.setTextSize(dp(15));
        paint.setARGB(255, 236, 240, 244);
        canvas.drawText(SCREEN_TITLES[screen], dp(12), top + dp(18), paint);
        paint.setTextSize(dp(11));
        paint.setARGB(255, 150, 162, 176);
        canvas.drawText("48 kHz | PCM16 | " + inputSource,
                dp(12), top + dp(38), paint);

        long seconds = capturedSamples / 48000L;
        long millis = ((capturedSamples % 48000L) * 1000L) / 48000L;
        String time = two(seconds / 60L) + ":" + two(seconds % 60L) + "." + three(millis);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(dp(18));
        paint.setARGB(255, 236, 240, 244);
        canvas.drawText(time, width - dp(12), top + dp(28), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawRecorder(Canvas canvas, float w, float h) {
        float top = dp(112);
        drawWave(canvas, dp(12), top, w - dp(12), top + dp(150));
        drawMeter(canvas, dp(12), top + dp(170), w - dp(12), peakPct, rmsPct);
        drawText(canvas, "Peak " + peakPct + "%FS | RMS " + rmsPct +
                "%FS | clipped " + clipped, dp(12), top + dp(220), 12);
        drawPills(canvas, top + dp(248), new String[]{
                "ARM", "REC", "PAUSE", "STOP", "MARK", "MONITOR"
        });
        drawText(canvas, "Raw PCM preserved before DSP.", dp(12), h - dp(20), 11);
    }

    private void drawEditor(Canvas canvas, float w, float h) {
        float top = dp(112);
        drawTimeline(canvas, dp(12), top, w - dp(12));
        drawWave(canvas, dp(12), top + dp(32), w - dp(12), top + dp(205));
        drawPills(canvas, top + dp(230), new String[]{
                "SELECT", "ZOOM", "SCRUB", "MARKER", "WARP", "A/B"
        });
        drawText(canvas, "time-warp W(t): monotonic | pitch/formant independently gated",
                dp(12), h - dp(20), 11);
    }

    private void drawCalibration(Canvas canvas, float w, float h) {
        float top = dp(120);
        drawText(canvas, calibrationState, dp(12), top, 13);
        drawText(canvas, "DIGITAL: dBFS / peak / RMS / noise / latency", dp(12), top + dp(28), 12);
        drawText(canvas, "RELATIVE: transfer / phase / coherence / L-R match", dp(12), top + dp(52), 12);
        drawText(canvas, "ABS SPL: TOKEN_VAZIO until physical reference exists", dp(12), top + dp(76), 12);
        drawPills(canvas, top + dp(108), new String[]{
                "SILENCE", "WHITE", "PINK", "SWEEP", "STEP", "REF MIC"
        });
        drawMiniResponse(canvas, dp(12), top + dp(160), w - dp(12), top + dp(310));
        drawText(canvas, "speaker -> room -> mic -> ADC is measured as one chain unless referenced",
                dp(12), h - dp(20), 11);
    }

    private void drawSpectrum(Canvas canvas, float w, float h) {
        float top = dp(120);
        drawSpectrumBars(canvas, dp(12), top, w - dp(12), top + dp(220));
        drawPills(canvas, top + dp(244), new String[]{
                "SPECTRUM", "SPECTROGRAM", "PHASE", "COHERENCE", "THD", "IR"
        });
        drawText(canvas, "16-band Goertzel exists; calibrated FFT/STFT remains separately gated.",
                dp(12), h - dp(20), 11);
    }

    private void drawRoom(Canvas canvas, float w, float h) {
        float top = dp(120);
        drawText(canvas, "IR -> ETC/EDC -> EDT/T20/T30 -> constrained correction", dp(12), top, 12);
        drawMiniResponse(canvas, dp(12), top + dp(24), w - dp(12), top + dp(176));
        drawPills(canvas, top + dp(202), new String[]{
                "CAPTURE IR", "CONVOLVE", "RT", "TARGET", "CORRECT", "BYPASS"
        });
        drawText(canvas, "regularized inverse only; deep spatial nulls are not blindly boosted",
                dp(12), h - dp(20), 11);
    }

    private void drawVoice(Canvas canvas, float w, float h) {
        float top = dp(120);
        drawText(canvas, "f0 | formants | voicing | vibrato | phoneme | timing confidence",
                dp(12), top, 12);
        drawWave(canvas, dp(12), top + dp(28), w - dp(12), top + dp(150));
        drawPills(canvas, top + dp(178), new String[]{
                "PITCH", "FORMANT", "TIME", "PHONEME", "BREATH", "UNVOICED"
        });
        drawText(canvas, "phonetics is an acoustic feature layer; neuroscience/quantum claims remain experimental",
                dp(12), h - dp(20), 11);
    }

    private void drawMaster(Canvas canvas, float w, float h) {
        float top = dp(120);
        drawText(canvas, masterState, dp(12), top, 13);
        drawPills(canvas, top + dp(28), new String[]{
                "HPF", "GATE", "DENOISE", "EQ-N", "LEVEL", "LIMIT", "NORM", "ROOM"
        });
        drawText(canvas, "per-module: ENABLE | BYPASS | SOLO | A/B | reset", dp(12), top + dp(92), 12);
        drawMiniResponse(canvas, dp(12), top + dp(120), w - dp(12), top + dp(280));
        drawText(canvas, "SOURCE is immutable; preview state is separate from final render.",
                dp(12), h - dp(20), 11);
    }

    private void drawExport(Canvas canvas, float w, float h) {
        float top = dp(120);
        drawText(canvas, containerState, dp(12), top, 13);
        drawText(canvas, "ZRF: session/wave/matrix chunks | CFR: calibration/response chunks",
                dp(12), top + dp(30), 12);
        drawText(canvas, "PCM | WAVE | MATR | CAL | IR | SPEC | PHON | ROOM | RCPT",
                dp(12), top + dp(56), 12);
        drawPills(canvas, top + dp(90), new String[]{
                "RAW PCM", "ZRF", "CFR", "OGG/OPUS", "RECEIPT"
        });
        drawText(canvas, "Custom formats: project-local, versioned, fail-closed; not external standards.",
                dp(12), h - dp(20), 11);
    }

    private void drawWave(Canvas canvas, float left, float top, float right, float bottom) {
        thin.setStrokeWidth(1f);
        thin.setARGB(255, 58, 67, 78);
        float mid = (top + bottom) * 0.5f;
        canvas.drawLine(left, mid, right, mid, thin);
        if (waveBins <= 0) {
            drawText(canvas, "waveform buffer: TOKEN_VAZIO", left + dp(8), mid, 11);
            return;
        }
        thin.setARGB(255, 128, 210, 184);
        int i;
        float width = right - left;
        float half = (bottom - top) * 0.46f;
        for (i = 0; i < waveBins; ++i) {
            float x = left + width * i / (float)Math.max(1, waveBins - 1);
            float y1 = mid - (waveMax[i] / 32768f) * half;
            float y2 = mid - (waveMin[i] / 32768f) * half;
            canvas.drawLine(x, y1, x, y2, thin);
        }
    }

    private void drawTimeline(Canvas canvas, float left, float top, float right) {
        thin.setARGB(255, 90, 100, 112);
        canvas.drawLine(left, top, right, top, thin);
        int i;
        for (i = 0; i <= 10; ++i) {
            float x = left + (right - left) * i / 10f;
            canvas.drawLine(x, top, x, top + dp(i % 5 == 0 ? 12 : 6), thin);
        }
    }

    private void drawMeter(Canvas canvas, float left, float top, float right, int peak, int rms) {
        float width = right - left;
        paint.setARGB(255, 45, 50, 58);
        canvas.drawRect(left, top, right, top + dp(18), paint);
        paint.setARGB(255, 104, 185, 151);
        canvas.drawRect(left, top, left + width * Math.min(100, peak) / 100f,
                top + dp(7), paint);
        paint.setARGB(255, 103, 145, 203);
        canvas.drawRect(left, top + dp(11), left + width * Math.min(100, rms) / 100f,
                top + dp(18), paint);
    }

    private void drawSpectrumBars(Canvas canvas, float left, float top, float right, float bottom) {
        long max = 1L;
        int i;
        for (i = 0; i < spectrum.length; ++i) if (spectrum[i] > max) max = spectrum[i];
        float slot = (right - left) / spectrum.length;
        paint.setARGB(255, 112, 172, 210);
        for (i = 0; i < spectrum.length; ++i) {
            float ratio = (float)Math.sqrt((double)spectrum[i] / (double)max);
            float height = (bottom - top) * ratio;
            canvas.drawRect(left + i * slot + dp(2), bottom - height,
                    left + (i + 1) * slot - dp(2), bottom, paint);
        }
    }

    private void drawMiniResponse(Canvas canvas, float left, float top, float right, float bottom) {
        thin.setARGB(255, 66, 75, 86);
        canvas.drawRect(left, top, right, bottom, thin);
        float mid = (top + bottom) * 0.5f;
        canvas.drawLine(left, mid, right, mid, thin);
        thin.setARGB(255, 186, 150, 96);
        float px = left;
        float py = mid;
        int i;
        for (i = 1; i <= 48; ++i) {
            float x = left + (right - left) * i / 48f;
            float y = mid + (float)Math.sin(i * 0.47) * (bottom - top) * 0.12f
                    + (float)Math.sin(i * 0.13) * (bottom - top) * 0.08f;
            canvas.drawLine(px, py, x, y, thin);
            px = x;
            py = y;
        }
    }

    private void drawPills(Canvas canvas, float y, String[] labels) {
        float x = dp(12);
        int i;
        paint.setTextSize(dp(10));
        for (i = 0; i < labels.length; ++i) {
            float width = paint.measureText(labels[i]) + dp(20);
            if (x + width > getWidth() - dp(12)) {
                x = dp(12);
                y += dp(34);
            }
            paint.setARGB(255, 42, 49, 58);
            rect.set(x, y, x + width, y + dp(26));
            canvas.drawRoundRect(rect, dp(5), dp(5), paint);
            paint.setARGB(255, 220, 226, 232);
            canvas.drawText(labels[i], x + dp(10), y + dp(18), paint);
            x += width + dp(6);
        }
    }

    private void drawText(Canvas canvas, String text, float x, float y, int sizeSp) {
        paint.setTextSize(dp(sizeSp));
        paint.setARGB(255, 197, 205, 214);
        canvas.drawText(text, x, y, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && event.getY() <= dp(56)) {
            float slot = getWidth() / (float)TABS.length;
            int target = slot <= 0f ? 0 : (int)(event.getX() / slot);
            if (target < 0) target = 0;
            if (target >= TABS.length) target = TABS.length - 1;
            screen = target;
            invalidate();
            performClick();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
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
