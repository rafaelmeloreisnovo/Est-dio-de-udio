/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

/** Platform-free numerical operators. This Java class is hosted, not freestanding C. */
final class WaveLabCore {
    static final int SAMPLE_RATE = 48000;
    static final int[] BANDS_HZ = {125, 250, 500, 1000, 2000, 4000};
    private WaveLabCore() {}

    /** A bin-centred, finite-window Goertzel sample amplitude in dBFS. No SPL claim. */
    static double relativeDbfs(short[] samples, int n, int hz) {
        if (samples == null || n < 64 || n > samples.length ||
                hz <= 0 || hz >= SAMPLE_RATE / 2) return Double.NaN;
        int k = (int) Math.round((double) n * hz / SAMPLE_RATE);
        if (k <= 0 || k >= n / 2) return Double.NaN;
        double w = 2.0 * Math.PI * k / n;
        double coefficient = 2.0 * Math.cos(w);
        double q0, q1 = 0.0, q2 = 0.0;
        for (int i = 0; i < n; ++i) {
            q0 = samples[i] + coefficient * q1 - q2;
            q2 = q1;
            q1 = q0;
        }
        double power = q1 * q1 + q2 * q2 - coefficient * q1 * q2;
        if (!(power > 0.0) || !Double.isFinite(power)) return Double.NaN;
        double peak = 2.0 * Math.sqrt(power) / (n * 32768.0);
        return peak > 0.0 && Double.isFinite(peak)
                ? 20.0 * Math.log10(peak) : Double.NaN;
    }

    /** Device-pose azimuth bin, NOT source direction of arrival. */
    static int poseSector(double yawDegrees) {
        if (!Double.isFinite(yawDegrees)) return -1;
        double d = ((yawDegrees % 360.0) + 360.0) % 360.0;
        return ((int) Math.floor((d + 22.5) / 45.0)) % 8;
    }

    /** Only the geometric round-trip model, not a physical range measurement. */
    static double modeledEchoRangeM(double delaySeconds, double speedMps) {
        if (!Double.isFinite(delaySeconds) || delaySeconds < 0.0 ||
                !Double.isFinite(speedMps) || speedMps <= 0.0) return Double.NaN;
        return speedMps * delaySeconds / 2.0;
    }

    /** Great-circle estimate. Requires valid geodetic coordinates. */
    static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        if (!Double.isFinite(lat1) || !Double.isFinite(lon1) ||
                !Double.isFinite(lat2) || !Double.isFinite(lon2) ||
                Math.abs(lat1) > 90 || Math.abs(lat2) > 90 ||
                Math.abs(lon1) > 180 || Math.abs(lon2) > 180) return Double.NaN;
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double dp = p2 - p1, dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2) +
                Math.cos(p1) * Math.cos(p2) *
                        Math.sin(dl / 2) * Math.sin(dl / 2);
        a = Math.max(0.0, Math.min(1.0, a));
        return 6371008.8 * 2 * Math.asin(Math.sqrt(a));
    }

    /** Generic Poincare SECTION COUNTER only; no claim of periodic orbit or chaos. */
    static int sectionCrossing(double previous, double current) {
        if (!Double.isFinite(previous) || !Double.isFinite(current)) return 0;
        return previous < 0.0 && current >= 0.0 ? 1 : 0;
    }
}
