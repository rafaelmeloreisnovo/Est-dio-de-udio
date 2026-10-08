/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** Visualizer only: PCM bands and phone pose; never claims acoustic DOA. */
final class WaveLabPlotView extends View {
    private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final double[] binsDb = new double[WaveLabCore.BANDS_HZ.length];
    private float yaw = Float.NaN;
    private float ax, ay, az;

    WaveLabPlotView(Context context) {
        super(context);
        for (int i = 0; i < binsDb.length; ++i) binsDb[i] = Double.NaN;
    }

    void setSpectrum(double[] newBins) {
        if (newBins == null || newBins.length != binsDb.length) return;
        System.arraycopy(newBins, 0, binsDb, 0, binsDb.length);
        invalidate();
    }

    void setPose(float angleDegrees, float x, float y, float z) {
        yaw = angleDegrees; ax = x; ay = y; az = z;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float w = getWidth(), h = getHeight();
        canvas.drawColor(Color.rgb(12, 19, 25));
        pen.setColor(Color.rgb(215, 232, 242));
        pen.setTextSize(29f);
        pen.setStrokeWidth(2f);
        canvas.drawText("PCM / RELATIVE SPECTRAL BINS", 12, 35, pen);
        final float top = 57f;
        final float baseline = h * 0.48f;
        final float width = (w - 24f) / binsDb.length;
        for (int i = 0; i < binsDb.length; ++i) {
            double db = binsDb[i];
            float x = 14f + i * width;
            if (Double.isFinite(db)) {
                float normalized = (float) Math.min(1.0, Math.max(0.0, (db + 100.0) / 100.0));
                pen.setColor(Color.rgb(91, 184, 203));
                canvas.drawRect(x + width * .15f,
                        baseline - normalized * (baseline - top),
                        x + width * .78f, baseline, pen);
            }
            pen.setTextSize(20f);
            pen.setColor(Color.rgb(203, 218, 231));
            canvas.drawText(String.valueOf(WaveLabCore.BANDS_HZ[i]), x + 3f,
                    baseline + 25f, pen);
        }
        pen.setColor(Color.rgb(215, 232, 242));
        pen.setTextSize(26f);
        canvas.drawText("DEVICE POSE / NOT SOUND DIRECTION", 12, baseline + 62f, pen);
        float cx = w * .5f;
        float cy = (baseline + 72f + h) / 2f;
        float radius = Math.min(w * .28f, (h - baseline - 95f) * .40f);
        if (radius < 20f) return;
        pen.setColor(Color.rgb(63, 94, 110));
        pen.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 8; ++i) {
            double a = 2.0 * Math.PI * i / 8.0;
            canvas.drawLine(cx, cy,
                    cx + radius * (float) Math.sin(a),
                    cy - radius * (float) Math.cos(a), pen);
        }
        canvas.drawCircle(cx, cy, radius, pen);
        pen.setStyle(Paint.Style.FILL);
        if (!Float.isNaN(yaw)) {
            double a = Math.toRadians(yaw);
            pen.setColor(Color.rgb(227, 183, 93));
            pen.setStrokeWidth(7f);
            canvas.drawLine(cx, cy, cx + radius * .85f * (float)Math.sin(a),
                    cy - radius * .85f * (float)Math.cos(a), pen);
        }
        pen.setColor(Color.rgb(220, 230, 239));
        pen.setTextSize(20f);
        canvas.drawText("XYZ m/s2: " + brief(ax) + " / " + brief(ay) + " / " + brief(az),
                12, h - 12, pen);
    }

    private static String brief(float value) {
        if (Float.isNaN(value)) return "TOKEN_VAZIO";
        return String.valueOf(Math.round(value * 100f) / 100f);
    }
}
