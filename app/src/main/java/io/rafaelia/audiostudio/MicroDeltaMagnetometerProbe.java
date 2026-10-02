/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

package io.rafaelia.audiostudio;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

final class MicroDeltaMagnetometerProbe implements SensorEventListener {
    interface Callback {
        void onComplete(Result result);
    }

    static final class Result {
        final String state;
        final String sensorName;
        final String vendor;
        final int sensorVersion;
        final int samples;
        final int deltaSamples;
        final double effectiveHz;
        final double rmsDeltaUt;
        final double peakDeltaUt;
        final float minX, maxX, minY, maxY, minZ, maxZ;

        Result(String state, String sensorName, String vendor, int sensorVersion,
               int samples, int deltaSamples, double effectiveHz,
               double rmsDeltaUt, double peakDeltaUt,
               float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
            this.state = state;
            this.sensorName = sensorName;
            this.vendor = vendor;
            this.sensorVersion = sensorVersion;
            this.samples = samples;
            this.deltaSamples = deltaSamples;
            this.effectiveHz = effectiveHz;
            this.rmsDeltaUt = rmsDeltaUt;
            this.peakDeltaUt = peakDeltaUt;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        static Result unavailable(String reason) {
            return new Result(
                    "TOKEN_VAZIO_SENSOR_UNAVAILABLE_" + reason,
                    "TOKEN_VAZIO", "TOKEN_VAZIO", 0,
                    0, 0, 0.0, 0.0, 0.0,
                    0f, 0f, 0f, 0f, 0f, 0f);
        }
    }

    private final SensorManager manager;
    private final Sensor sensor;
    private final Callback callback;
    private final Handler main = new Handler(Looper.getMainLooper());

    private int samples;
    private int deltaSamples;
    private boolean havePrevious;
    private float previousX, previousY, previousZ;
    private double sumDeltaSq;
    private double peakDeltaSq;
    private long firstTimestampNs;
    private long lastTimestampNs;
    private float minX = Float.POSITIVE_INFINITY;
    private float maxX = Float.NEGATIVE_INFINITY;
    private float minY = Float.POSITIVE_INFINITY;
    private float maxY = Float.NEGATIVE_INFINITY;
    private float minZ = Float.POSITIVE_INFINITY;
    private float maxZ = Float.NEGATIVE_INFINITY;
    private boolean finished;

    private MicroDeltaMagnetometerProbe(
            SensorManager manager, Sensor sensor, Callback callback) {
        this.manager = manager;
        this.sensor = sensor;
        this.callback = callback;
    }

    static void run(Context context, long durationMs, Callback callback) {
        SensorManager manager =
                (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (manager == null) {
            new Handler(Looper.getMainLooper()).post(
                    () -> callback.onComplete(Result.unavailable("SERVICE_UNAVAILABLE")));
            return;
        }

        Sensor sensor = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (sensor == null) {
            new Handler(Looper.getMainLooper()).post(
                    () -> callback.onComplete(Result.unavailable("NOT_PRESENT")));
            return;
        }

        if (durationMs < 500L) durationMs = 500L;
        if (durationMs > 5000L) durationMs = 5000L;

        MicroDeltaMagnetometerProbe probe =
                new MicroDeltaMagnetometerProbe(manager, sensor, callback);
        boolean registered = manager.registerListener(
                probe, sensor, SensorManager.SENSOR_DELAY_GAME);
        if (!registered) {
            callback.onComplete(Result.unavailable("LISTENER_REGISTRATION_FAILED"));
            return;
        }
        probe.main.postDelayed(probe::finish, durationMs);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (finished || event == null || event.values == null ||
                event.values.length < 3) return;
        if (samples >= 4096) {
            finish();
            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        if (samples == 0) firstTimestampNs = event.timestamp;
        lastTimestampNs = event.timestamp;

        if (x < minX) minX = x;
        if (x > maxX) maxX = x;
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
        if (z < minZ) minZ = z;
        if (z > maxZ) maxZ = z;

        if (havePrevious) {
            double dx = (double)x - previousX;
            double dy = (double)y - previousY;
            double dz = (double)z - previousZ;
            double d2 = dx * dx + dy * dy + dz * dz;
            sumDeltaSq += d2;
            if (d2 > peakDeltaSq) peakDeltaSq = d2;
            ++deltaSamples;
        }

        previousX = x;
        previousY = y;
        previousZ = z;
        havePrevious = true;
        ++samples;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void finish() {
        if (finished) return;
        finished = true;
        manager.unregisterListener(this);

        double seconds = lastTimestampNs > firstTimestampNs ?
                (lastTimestampNs - firstTimestampNs) / 1000000000.0 : 0.0;
        double hz = seconds > 0.0 && samples > 1 ?
                (samples - 1) / seconds : 0.0;
        double rms = deltaSamples > 0 ?
                sqrtLocal(sumDeltaSq / deltaSamples) : 0.0;
        double peak = sqrtLocal(peakDeltaSq);

        Result result = new Result(
                samples > 1 ? "OBSERVED_UNPROMOTED" : "INSUFFICIENT_EVIDENCE",
                sensor.getName(), sensor.getVendor(), sensor.getVersion(),
                samples, deltaSamples, hz, rms, peak,
                finiteOrZero(minX), finiteOrZero(maxX),
                finiteOrZero(minY), finiteOrZero(maxY),
                finiteOrZero(minZ), finiteOrZero(maxZ));
        main.post(() -> callback.onComplete(result));
    }

    private static double sqrtLocal(double value) {
        if (!(value > 0.0)) return 0.0;
        double x = value >= 1.0 ? value : 1.0;
        int i;
        for (i = 0; i < 12; ++i) x = 0.5 * (x + value / x);
        return x;
    }

    private static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0f;
    }
}
