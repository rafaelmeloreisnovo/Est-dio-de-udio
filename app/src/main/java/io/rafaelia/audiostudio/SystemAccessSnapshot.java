/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

package io.rafaelia.audiostudio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;

final class SystemAccessSnapshot {
    private SystemAccessSnapshot() {}

    static String describe(Context context) {
        SensorManager sm =
                (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        String mic = granted(context, Manifest.permission.RECORD_AUDIO) ?
                "MIC=GRANTED" : "MIC=ASK_ON_USE";
        String sensorEvidence = "SENSOR_MUDELTA=EXPLICIT_PROOF_ACTION";
        String sensorAccess =
                "SENSOR_ACCESS=NO_RUNTIME_PERMISSION_REQUIRED_ACCEL_MAG_CURRENT_PROFILE";
        String sensorRate =
                "SENSOR_RATE=PLATFORM_BOUNDED_NO_HIGH_RATE_PERMISSION";

        String accel = sensor(sm, Sensor.TYPE_ACCELEROMETER) ?
                "ACCEL=PRESENT" : "ACCEL=TOKEN_VAZIO_SENSOR_NOT_PRESENT";
        String mag = sensor(sm, Sensor.TYPE_MAGNETIC_FIELD) ?
                "MAG=PRESENT" : "MAG=TOKEN_VAZIO_SENSOR_NOT_PRESENT";
        String light = sensor(sm, Sensor.TYPE_LIGHT) ?
                "LIGHT=PRESENT" : "LIGHT=TOKEN_VAZIO_SENSOR_NOT_PRESENT";
        String prox = sensor(sm, Sensor.TYPE_PROXIMITY) ?
                "PROX=PRESENT" : "PROX=TOKEN_VAZIO_SENSOR_NOT_PRESENT";

        return mic + " | " + sensorAccess + " | " + sensorRate + " | " +
                sensorEvidence + " | " + accel + " | " + mag + " | " +
                light + " | " + prox;
    }

    static String signatureState() {
        return "MODE=" + BuildConfig.SIGNING_MODE +
                " | SIGNER=" + BuildConfig.SIGNER_ID +
                " | EXPECTED_CERT=" + BuildConfig.EXPECTED_CERT_SHA256;
    }

    static String originState() {
        return "PROJECT=RAFAELIA | PLATFORM=ANDROID | TOOLCHAIN=EXTERNAL_PINNED";
    }

    private static boolean granted(Context context, String permission) {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean sensor(SensorManager manager, int type) {
        return manager != null && manager.getDefaultSensor(type) != null;
    }
}
