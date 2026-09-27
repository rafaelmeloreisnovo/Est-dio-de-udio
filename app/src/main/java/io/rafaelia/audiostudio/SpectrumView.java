package io.rafaelia.audiostudio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

final class SpectrumView extends View {
    private static final String[] LABELS = {
            "80","125","200","315","500","800","1.25k","2k",
            "3.15k","5k","8k","10k","12.5k","16k","20k","22k"
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final long[] bands = new long[16];

    SpectrumView(Context context) {
        super(context);
        paint.setTextSize(20f * getResources().getDisplayMetrics().scaledDensity / 2f);
        setMinimumHeight((int) (180 * getResources().getDisplayMetrics().density));
    }

    void setBands(long[] values) {
        int n = Math.min(values.length, bands.length);
        for (int i = 0; i < n; i++) bands[i] = values[i];
        for (int i = n; i < bands.length; i++) bands[i] = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long max = 1;
        for (long v : bands) if (v > max) max = v;

        float w = getWidth();
        float h = getHeight();
        float baseline = h - 28f;
        float slot = w / bands.length;
        paint.setStrokeWidth(Math.max(2f, slot * 0.55f));

        for (int i = 0; i < bands.length; i++) {
            long root = isqrt(bands[i]);
            long maxRoot = isqrt(max);
            float ratio = maxRoot == 0 ? 0f : (float) root / (float) maxRoot;
            float x = slot * i + slot * 0.5f;
            float top = baseline - ratio * (baseline - 10f);
            canvas.drawLine(x, baseline, x, top, paint);
        }

        paint.setStrokeWidth(1f);
        for (int i = 0; i < LABELS.length; i += 3) {
            canvas.drawText(LABELS[i], slot * i + 2f, h - 6f, paint);
        }
    }

    private static long isqrt(long x) {
        if (x <= 0) return 0;
        long result = 0;
        long bit = 1L << 62;
        while (bit > x) bit >>>= 2;
        while (bit != 0) {
            if (x >= result + bit) {
                x -= result + bit;
                result = (result >>> 1) + bit;
            } else {
                result >>>= 1;
            }
            bit >>>= 2;
        }
        return result;
    }
}
