/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

package io.rafaelia.audiostudio;

import android.app.ActivityManager;
import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.AudioDeviceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

final class EvidenceBundleWriter {
    static final class Result {
        final Uri uri;
        final String displayName;

        Result(Uri uri, String displayName) {
            this.uri = uri;
            this.displayName = displayName;
        }
    }

    private EvidenceBundleWriter() {}

    static Result write(
            Context context,
            MicroDeltaVibrationProbe.Result vibration,
            MicroDeltaMagnetometerProbe.Result magnetometer,
            File lastZrf,
            File lastCfr,
            File lastMasteredPcm,
            RelativeCalibrationEngine.Result lastCalibration) throws Exception {
        String displayName = "rafaelia_evidence_" +
                System.currentTimeMillis() + ".txt";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, displayName);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
        values.put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/RafaeliaAudio/Evidence");
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri uri = context.getContentResolver().insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IllegalStateException("evidence MediaStore insert failed");

        boolean success = false;
        try (OutputStream out = context.getContentResolver().openOutputStream(uri, "w")) {
            if (out == null) throw new IllegalStateException("evidence stream unavailable");
            String text = buildText(
                    context, vibration, magnetometer, lastZrf, lastCfr,
                    lastMasteredPcm, lastCalibration);
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.flush();
            success = true;
        } finally {
            ContentValues done = new ContentValues();
            done.put(MediaStore.Downloads.IS_PENDING, success ? 0 : 1);
            context.getContentResolver().update(uri, done, null, null);
        }

        return new Result(uri, displayName);
    }

    private static String buildText(
            Context context,
            MicroDeltaVibrationProbe.Result vibration,
            MicroDeltaMagnetometerProbe.Result magnetometer,
            File lastZrf,
            File lastCfr,
            File lastMasteredPcm,
            RelativeCalibrationEngine.Result lastCalibration) throws Exception {
        StringBuilder b = new StringBuilder(16384);
        PackageManager pm = context.getPackageManager();
        PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
        AudioManager audio =
                (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        SensorManager sensors =
                (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        line(b, "schema", BuildConfig.EVIDENCE_SCHEMA);
        line(b, "kind", "installation_hardware_development_evidence");
        line(b, "generated_epoch_ms", Long.toString(System.currentTimeMillis()));
        line(b, "claim_policy",
                "SOURCE!=ARTIFACT!=EXECUTION!=EVIDENCE!=CLAIM;TOKEN_VAZIO!=0");

        section(b, "installed_application");
        line(b, "package", context.getPackageName());
        line(b, "version_name", pi.versionName == null ? "UNAVAILABLE_NOT_REPORTED" : pi.versionName);
        line(b, "version_code", Long.toString(pi.getLongVersionCode()));
        line(b, "first_install_epoch_ms", Long.toString(pi.firstInstallTime));
        line(b, "last_update_epoch_ms", Long.toString(pi.lastUpdateTime));
        line(b, "source_sha", BuildConfig.SOURCE_SHA);
        line(b, "ci_run_id", BuildConfig.CI_RUN_ID);
        line(b, "ci_run_number", BuildConfig.CI_RUN_NUMBER);
        String apkPath = context.getApplicationInfo().sourceDir;
        line(b, "installed_apk_sha256", sha256File(new File(apkPath)));
        line(b, "signing_mode", BuildConfig.SIGNING_MODE);
        line(b, "signer_id", BuildConfig.SIGNER_ID);
        line(b, "expected_signing_cert_sha256", BuildConfig.EXPECTED_CERT_SHA256);
        String installedCert = installedSigningCertificateSha256(context);
        line(b, "installed_signing_cert_sha256", installedCert);
        line(b, "signing_cert_matches_expected",
                signingMatch(installedCert, BuildConfig.EXPECTED_CERT_SHA256));

        section(b, "embedded_ci_provenance");
        appendAssetOrToken(context, b, "ci_provenance_v1.txt");

        section(b, "component_origin");
        appendAssetOrToken(context, b, "component_origin_v1.txt");

        section(b, "permission_contract");
        appendAssetOrToken(context, b, "permission_contract_v1.txt");

        section(b, "device_runtime");
        line(b, "manufacturer", safe(Build.MANUFACTURER));
        line(b, "model", safe(Build.MODEL));
        line(b, "device", safe(Build.DEVICE));
        line(b, "android_release", safe(Build.VERSION.RELEASE));
        line(b, "sdk_int", Integer.toString(Build.VERSION.SDK_INT));
        line(b, "supported_abis", join(Build.SUPPORTED_ABIS));

        section(b, "permissions_and_audio");
        line(b, "record_audio_permission",
                context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED ? "GRANTED" : "NOT_GRANTED");
        line(b, "activity_recognition_permission",
                "NOT_DECLARED_NO_STEP_ACTIVITY_FEATURE");
        line(b, "sensor_mudelta_policy",
                "EXPLICIT_PROOF_ACTION");
        line(b, "access_network_state_permission",
                context.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) ==
                        PackageManager.PERMISSION_GRANTED ? "GRANTED" : "NOT_GRANTED");
        line(b, "high_sampling_rate_sensor_permission",
                "NOT_REQUESTED_CURRENT_PROFILE");
        line(b, "microphone_feature",
                pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE) ? "PRESENT" : "ABSENT");
        line(b, "audio_output_sample_rate",
                audio == null ? "UNAVAILABLE_SERVICE" :
                        safeOrUnavailable(audio.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)));
        line(b, "audio_output_frames_per_buffer",
                audio == null ? "UNAVAILABLE_SERVICE" :
                        safeOrUnavailable(audio.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)));

        section(b, "audio_input_devices");
        appendAudioInputDevices(b, audio);

        section(b, "hardware_acceleration");
        appendGraphicsCapabilities(context, b, pm);
        line(b, "npu_generic_query", "UNAVAILABLE_STANDARD_ANDROID_QUERY");
        line(b, "numa_generic_query", "UNAVAILABLE_STANDARD_ANDROID_QUERY");

        section(b, "micro_delta_vibration");
        if (vibration == null) {
            line(b, "state", "NOT_RUN");
        } else {
            line(b, "state", vibration.state);
            line(b, "definition",
                    "mu_delta=temporal acceleration delta operator; not SI micro-unit");
            line(b, "sensor", safe(vibration.sensorName));
            line(b, "vendor", safe(vibration.vendor));
            line(b, "sensor_version", Integer.toString(vibration.sensorVersion));
            line(b, "samples", Integer.toString(vibration.samples));
            line(b, "delta_samples", Integer.toString(vibration.deltaSamples));
            line(b, "effective_hz", f6(vibration.effectiveHz));
            line(b, "rms_delta_m_s2", f6(vibration.rmsDeltaMs2));
            line(b, "peak_delta_m_s2", f6(vibration.peakDeltaMs2));
            line(b, "x_min_m_s2", f6(vibration.minX));
            line(b, "x_max_m_s2", f6(vibration.maxX));
            line(b, "y_min_m_s2", f6(vibration.minY));
            line(b, "y_max_m_s2", f6(vibration.maxY));
            line(b, "z_min_m_s2", f6(vibration.minZ));
            line(b, "z_max_m_s2", f6(vibration.maxZ));
            line(b, "interpretation",
                    "OBSERVATION_ONLY; vibration/movement is not a hardware-fault diagnosis");
        }

        section(b, "micro_delta_magnetic");
        if (magnetometer == null) {
            line(b, "state", "NOT_RUN");
        } else {
            line(b, "state", magnetometer.state);
            line(b, "definition",
                    "mu_delta=temporal magnetic-field delta operator; unit=microtesla");
            line(b, "sensor", safe(magnetometer.sensorName));
            line(b, "vendor", safe(magnetometer.vendor));
            line(b, "sensor_version", Integer.toString(magnetometer.sensorVersion));
            line(b, "samples", Integer.toString(magnetometer.samples));
            line(b, "delta_samples", Integer.toString(magnetometer.deltaSamples));
            line(b, "effective_hz", f6(magnetometer.effectiveHz));
            line(b, "rms_delta_uT", f6(magnetometer.rmsDeltaUt));
            line(b, "peak_delta_uT", f6(magnetometer.peakDeltaUt));
            line(b, "x_min_uT", f6(magnetometer.minX));
            line(b, "x_max_uT", f6(magnetometer.maxX));
            line(b, "y_min_uT", f6(magnetometer.minY));
            line(b, "y_max_uT", f6(magnetometer.maxY));
            line(b, "z_min_uT", f6(magnetometer.minZ));
            line(b, "z_max_uT", f6(magnetometer.maxZ));
            line(b, "interpretation",
                    "OBSERVATION_ONLY; magnetic change is not causal attribution");
        }

        section(b, "passive_connectivity");
        PassiveRadioObservation.Result radio = PassiveRadioObservation.observe(context);
        line(b, "state", radio.state);
        line(b, "active_transports", radio.transports);
        line(b, "telephony_feature", radio.telephonyFeature ? "PRESENT" : "NOT_REPORTED");
        line(b, "policy",
                "PASSIVE_PLATFORM_METADATA_ONLY; no scan/injection/modem/power/frequency control");

        section(b, "audio_codec_inventory");
        CodecCapabilityProbe.Result codecs = CodecCapabilityProbe.observe();
        line(b, "encoders", codecs.audioEncoders);
        line(b, "decoders", codecs.audioDecoders);
        line(b, "policy",
                "RUNTIME_REPORTED_CAPABILITY; presence does not imply Rafaelia implementation");

        section(b, "sensor_inventory");
        if (sensors == null) {
            line(b, "sensors", "UNAVAILABLE_SERVICE");
        } else {
            List<Sensor> all = sensors.getSensorList(Sensor.TYPE_ALL);
            line(b, "sensor_count", Integer.toString(all.size()));
            int index = 0;
            for (Sensor sensor : all) {
                String prefix = "sensor." + index + ".";
                line(b, prefix + "type", Integer.toString(sensor.getType()));
                line(b, prefix + "name", safe(sensor.getName()));
                line(b, prefix + "vendor", safe(sensor.getVendor()));
                line(b, prefix + "version", Integer.toString(sensor.getVersion()));
                line(b, prefix + "resolution", f6(sensor.getResolution()));
                line(b, prefix + "max_range", f6(sensor.getMaximumRange()));
                line(b, prefix + "min_delay_us", Integer.toString(sensor.getMinDelay()));
                ++index;
            }
        }

        section(b, "project_artifacts");
        appendFileEvidence(b, "last_zrf", lastZrf);
        appendFileEvidence(b, "last_cfr", lastCfr);
        appendFileEvidence(b, "last_mastered_pcm", lastMasteredPcm);

        section(b, "relative_calibration_analysis");
        if (lastCalibration == null) {
            line(b, "state", "NOT_RUN");
        } else {
            line(b, "state", "OBSERVED_RELATIVE_UNCALIBRATED");
            line(b, "input_source", safe(lastCalibration.inputSource));
            line(b, "best_lag_samples", Integer.toString(lastCalibration.bestLag));
            line(b, "latency_us", Long.toString(lastCalibration.latencyMicros()));
            line(b, "valid_sweep_bands", Integer.toString(lastCalibration.validTransferBands()));
            line(b, "decay_method", lastCalibration.decayMethod());
            long rtMs = lastCalibration.preferredRt60Millis();
            line(b, "rt60_relative_ms", rtMs >= 0L ? Long.toString(rtMs) :
                    "TOKEN_VAZIO_DYNAMIC_RANGE");
            line(b, "interpretation",
                    "RELATIVE_PATH_ONLY; ABS_SPL_NOT_ESTABLISHED; ISO3382_NOT_CLAIMED; IR_DECONVOLUTION_NOT_CLAIMED");
        }

        section(b, "capability_claims");
        line(b, "freestanding_core",
                "BUILT_IN; runtime proof depends on embedded CI provenance");
        line(b, "cfr_relative_capture", "AVAILABLE_IN_APP");
        line(b, "cfr_relative_sweep_profile", "AVAILABLE_IN_APP_16_BANDS");
        line(b, "relative_decay_metrics", "AVAILABLE_IN_APP_NONSTANDARD_RELATIVE");
        line(b, "absolute_spl", "PENDING_PHYSICAL_REFERENCE");
        line(b, "sensor_vibration", "OBSERVED_UNPROMOTED");
        line(b, "sensor_magnetic", "OBSERVED_UNPROMOTED");
        line(b, "passive_radio_metadata", "PLATFORM_OBSERVATION_ONLY");
        line(b, "rac1_codec", "CORE_IMPLEMENTED_UNPROMOTED");
        line(b, "hardware_diagnosis", "NOT_CLAIMED");
        line(b, "installation_proof",
                "package metadata + installed APK SHA-256 + source/CI provenance when embedded");

        return b.toString();
    }

    private static void appendAudioInputDevices(
            StringBuilder b, AudioManager audio) {
        if (audio == null) {
            line(b, "state", "UNAVAILABLE_SERVICE");
            return;
        }
        AudioDeviceInfo[] devices = audio.getDevices(AudioManager.GET_DEVICES_INPUTS);
        line(b, "count", Integer.toString(devices.length));
        int i;
        for (i = 0; i < devices.length; ++i) {
            AudioDeviceInfo d = devices[i];
            String prefix = "input." + i + ".";
            line(b, prefix + "id", Integer.toString(d.getId()));
            line(b, prefix + "type", Integer.toString(d.getType()));
            CharSequence product = d.getProductName();
            line(b, prefix + "product",
                    product == null ? "UNAVAILABLE_NOT_REPORTED" : product.toString());
            line(b, prefix + "sample_rates", joinInts(d.getSampleRates()));
            line(b, prefix + "channel_counts", joinInts(d.getChannelCounts()));
            line(b, prefix + "channel_masks", joinInts(d.getChannelMasks()));
            line(b, prefix + "encodings", joinInts(d.getEncodings()));
        }
    }

    private static void appendGraphicsCapabilities(
            Context context, StringBuilder b, PackageManager pm) {
        ActivityManager activity =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activity == null) {
            line(b, "gles_required_version", "UNAVAILABLE_SERVICE");
        } else {
            ConfigurationInfo info = activity.getDeviceConfigurationInfo();
            if (info == null) {
                line(b, "gles_required_version", "UNAVAILABLE_NOT_REPORTED");
            } else {
                line(b, "gles_required_version",
                        "0x" + Integer.toHexString(info.reqGlEsVersion));
            }
        }
        line(b, "vulkan_feature",
                pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) ?
                        "PRESENT" : "NOT_REPORTED");
    }

    private static String joinInts(int[] values) {
        if (values == null || values.length == 0) return "UNAVAILABLE_NOT_REPORTED";
        StringBuilder b = new StringBuilder();
        int i;
        for (i = 0; i < values.length; ++i) {
            if (i != 0) b.append(',');
            b.append(values[i]);
        }
        return b.toString();
    }

    private static String safeOrUnavailable(String value) {
        return value == null || value.length() == 0 ?
                "UNAVAILABLE_NOT_REPORTED" : value;
    }

    private static void appendAssetOrToken(
            Context context, StringBuilder b, String assetName) {
        try (InputStream in = context.getAssets().open(assetName)) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) > 0) {
                b.append(new String(buffer, 0, n, StandardCharsets.UTF_8));
            }
            if (b.length() == 0 || b.charAt(b.length() - 1) != '\n') b.append('\n');
        } catch (Exception e) {
            line(b, "ci_provenance", "UNAVAILABLE_LOCAL_OR_NONCANONICAL_BUILD");
        }
    }

    private static void appendFileEvidence(StringBuilder b, String key, File file)
            throws Exception {
        if (file == null || !file.isFile()) {
            line(b, key + ".state", "NOT_CREATED_IN_SESSION");
            return;
        }
        line(b, key + ".state", "PRESENT");
        line(b, key + ".name", safe(file.getName()));
        line(b, key + ".bytes", Long.toString(file.length()));
        line(b, key + ".sha256", sha256File(file));
    }

    private static String installedSigningCertificateSha256(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNING_CERTIFICATES);
            if (pi.signingInfo == null) return "TOKEN_VAZIO_SIGNING_INFO";
            Signature[] signatures = pi.signingInfo.hasMultipleSigners() ?
                    pi.signingInfo.getApkContentsSigners() :
                    pi.signingInfo.getSigningCertificateHistory();
            if (signatures == null || signatures.length == 0) {
                return "TOKEN_VAZIO_SIGNING_CERT";
            }
            LowSha256 sha = new LowSha256();
            byte[] cert = signatures[0].toByteArray();
            sha.update(cert, 0, cert.length);
            return hex(sha.finish());
        } catch (Exception e) {
            return "UNAVAILABLE_SIGNING_QUERY";
        }
    }

    private static String signingMatch(String installed, String expected) {
        String e = normalizeHex(expected);
        String i = normalizeHex(installed);
        if (e.length() != 64 || i.length() != 64) return "TOKEN_VAZIO";
        return e.equals(i) ? "PASS" : "FAIL";
    }

    private static String normalizeHex(String value) {
        if (value == null) return "";
        StringBuilder b = new StringBuilder(value.length());
        int i;
        for (i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') b.append(c);
            else if (c >= 'a' && c <= 'f') b.append(c);
            else if (c >= 'A' && c <= 'F') b.append((char)(c + ('a' - 'A')));
        }
        return b.toString();
    }

    private static String sha256File(File file) throws Exception {
        LowSha256 sha = new LowSha256();
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[16384];
            int n;
            while ((n = in.read(buffer)) > 0) sha.update(buffer, 0, n);
        }
        return hex(sha.finish());
    }

    private static String hex(byte[] digest) {
        char[] out = new char[digest.length * 2];
        final char[] alphabet = "0123456789abcdef".toCharArray();
        int p = 0;
        for (byte value : digest) {
            int v = value & 0xff;
            out[p++] = alphabet[v >>> 4];
            out[p++] = alphabet[v & 15];
        }
        return new String(out);
    }

    private static void section(StringBuilder b, String name) {
        b.append("\n[").append(name).append("]\n");
    }

    private static void line(StringBuilder b, String key, String value) {
        b.append(key).append('=').append(sanitize(value)).append('\n');
    }

    private static String sanitize(String value) {
        if (value == null || value.length() == 0) return "UNAVAILABLE_NOT_REPORTED";
        return value.replace('\n', ' ').replace('\r', ' ');
    }

    private static String safe(String value) {
        return value == null || value.length() == 0 ? "UNAVAILABLE_NOT_REPORTED" : value;
    }

    private static String join(String[] values) {
        if (values == null || values.length == 0) return "UNAVAILABLE_NOT_REPORTED";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < values.length; ++i) {
            if (i != 0) b.append(',');
            b.append(values[i]);
        }
        return b.toString();
    }

    private static String f6(double value) {
        boolean negative = value < 0.0;
        double positive = negative ? -value : value;
        long scaled = (long)(positive * 1000000.0 + 0.5);
        long whole = scaled / 1000000L;
        long fraction = scaled - whole * 1000000L;
        String frac = Long.toString(fraction);
        StringBuilder b = new StringBuilder(24);
        if (negative) b.append('-');
        b.append(whole).append('.');
        int pad;
        for (pad = frac.length(); pad < 6; ++pad) b.append('0');
        b.append(frac);
        return b.toString();
    }
}
