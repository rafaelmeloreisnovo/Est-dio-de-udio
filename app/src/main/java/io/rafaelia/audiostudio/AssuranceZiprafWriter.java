/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */
package io.rafaelia.audiostudio;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds a bounded ZIPRAF evidence package from one real in-app execution.
 *
 * ZIPRAF here is an integrity/custody container. It is not encryption,
 * authenticity, certification, accreditation, or standards compliance.
 */
final class AssuranceZiprafWriter {
    static final class Result {
        final Uri uri;
        final String displayName;
        final String starState;
        final int relationCount;
        final int gapCount;

        Result(Uri uri, String displayName, String starState,
               int relationCount, int gapCount) {
            this.uri = uri;
            this.displayName = displayName;
            this.starState = starState;
            this.relationCount = relationCount;
            this.gapCount = gapCount;
        }
    }

    private static final String NL = "\n";

    private AssuranceZiprafWriter() {}

    static Result write(
            Context context,
            EvidenceBundleWriter.Result rawEvidence,
            MicroDeltaVibrationProbe.Result vibration,
            MicroDeltaMagnetometerProbe.Result magnetometer,
            File lastZrf,
            File lastCfr,
            File lastMasteredPcm,
            RelativeCalibrationEngine.Result lastCalibration) throws Exception {
        if (rawEvidence == null || rawEvidence.uri == null) {
            throw new IllegalArgumentException("raw evidence is required");
        }

        final long generatedEpochMs = System.currentTimeMillis();
        final byte[] rawEvidenceBytes = readUriBytes(context, rawEvidence.uri);
        final String rawEvidenceSha = sha256Bytes(rawEvidenceBytes);
        final String apkSha = sha256File(new File(context.getApplicationInfo().sourceDir));
        final String zrfSha = sha256FileOrToken(lastZrf);
        final String cfrSha = sha256FileOrToken(lastCfr);
        final String pcmSha = sha256FileOrToken(lastMasteredPcm);

        final boolean sourceBound = AssurancePipelineModel.isConcreteIdentity(BuildConfig.SOURCE_SHA);
        final boolean ciBound = AssurancePipelineModel.isConcreteIdentity(BuildConfig.CI_RUN_ID);
        final boolean provenanceResolved = sourceBound && ciBound;

        final String vibrationSourceState = vibration == null ? "NOT_RUN" : vibration.state;
        final String magneticSourceState = magnetometer == null ? "NOT_RUN" : magnetometer.state;
        final boolean vibrationUnavailable =
                AssurancePipelineModel.isSensorUnavailable(vibrationSourceState);
        final boolean magneticUnavailable =
                AssurancePipelineModel.isSensorUnavailable(magneticSourceState);
        final boolean vibrationRan = vibration != null &&
                AssurancePipelineModel.isObservedSensorState(
                        vibration.state, vibration.samples);
        final boolean magneticRan = magnetometer != null &&
                AssurancePipelineModel.isObservedSensorState(
                        magnetometer.state, magnetometer.samples);
        final boolean behaviorObserved = vibrationRan || magneticRan || lastCalibration != null;

        final boolean vibrationContract = vibration == null || vibrationUnavailable ||
                (vibration.samples > 1 &&
                        AssurancePipelineModel.isFinite(vibration.rmsDeltaMs2) &&
                        AssurancePipelineModel.isFinite(vibration.peakDeltaMs2));
        final boolean magneticContract = magnetometer == null || magneticUnavailable ||
                (magnetometer.samples > 1 &&
                        AssurancePipelineModel.isFinite(magnetometer.rmsDeltaUt) &&
                        AssurancePipelineModel.isFinite(magnetometer.peakDeltaUt));
        final boolean metricContractResolved = vibrationContract && magneticContract;

        int materializedCount = 2; // installed APK + raw evidence
        if (!zrfSha.startsWith("TOKEN_VAZIO")) ++materializedCount;
        if (!cfrSha.startsWith("TOKEN_VAZIO")) ++materializedCount;
        if (!pcmSha.startsWith("TOKEN_VAZIO")) ++materializedCount;

        int gapCount = 3; // physical SPL reference + independent reproduction + external audit
        if (!sourceBound) ++gapCount;
        if (!ciBound) ++gapCount;
        if (!vibrationContract || !magneticContract) ++gapCount;

        final String starState = AssurancePipelineModel.starState(
                apkSha.length() == 64,
                materializedCount,
                gapCount,
                behaviorObserved,
                metricContractResolved,
                provenanceResolved);

        final int relationCount = 9;
        final TreeMap<String, byte[]> entries = new TreeMap<>();

        put(entries, "00_manifest.json", manifestJson(
                generatedEpochMs, rawEvidence.displayName, rawEvidenceSha,
                apkSha, materializedCount, relationCount, gapCount, starState));
        put(entries, "10_material.json", materialJson(
                apkSha, zrfSha, cfrSha, pcmSha));
        put(entries, "20_materialized.json", materializedJson(
                rawEvidence.displayName, rawEvidenceSha,
                zrfSha, cfrSha, pcmSha, materializedCount));
        put(entries, "30_metrics.json", metricsJson(
                vibration, magnetometer, lastCalibration));
        put(entries, "40_relations.json", relationsJson(
                apkSha, rawEvidenceSha, zrfSha, cfrSha, pcmSha,
                sourceBound, ciBound, vibrationSourceState, magneticSourceState));
        put(entries, "50_gaps.json", gapsJson(
                sourceBound, ciBound, vibrationContract, magneticContract, gapCount));
        put(entries, "60_claims.json", claimsJson(
                apkSha, sourceBound, ciBound,
                vibrationSourceState, vibration == null ? 0 : vibration.samples,
                magneticSourceState, magnetometer == null ? 0 : magnetometer.samples,
                lastCalibration));
        put(entries, "70_receipt.txt", receiptText(
                generatedEpochMs, materializedCount, relationCount, gapCount, starState));
        entries.put("80_raw/evidence.txt", rawEvidenceBytes);

        StringBuilder sums = new StringBuilder(4096);
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            sums.append(sha256Bytes(entry.getValue()))
                    .append("  ")
                    .append(entry.getKey())
                    .append(NL);
        }
        put(entries, "99_SHA256SUMS.txt", sums.toString());

        String displayName = "rafaelia_assurance_" + generatedEpochMs + ".zipraf";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, displayName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/zip");
        values.put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/RafaeliaAudio/Assurance");
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri uri = context.getContentResolver().insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IllegalStateException("ZIPRAF MediaStore insert failed");

        boolean success = false;
        try (OutputStream raw = context.getContentResolver().openOutputStream(uri, "w")) {
            if (raw == null) throw new IllegalStateException("ZIPRAF output stream unavailable");
            try (ZipOutputStream zip = new ZipOutputStream(raw)) {
                for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    writeStoredEntry(zip, entry.getKey(), entry.getValue());
                }
                zip.finish();
            }
            success = true;
        } finally {
            if (success) {
                ContentValues done = new ContentValues();
                done.put(MediaStore.Downloads.IS_PENDING, 0);
                context.getContentResolver().update(uri, done, null, null);
            } else {
                context.getContentResolver().delete(uri, null, null);
            }
        }

        return new Result(uri, displayName, starState, relationCount, gapCount);
    }

    private static String manifestJson(
            long epochMs,
            String rawEvidenceName,
            String rawEvidenceSha,
            String apkSha,
            int materializedCount,
            int relationCount,
            int gapCount,
            String starState) {
        return "{\n" +
                "  \"schema\":\"" + AssurancePipelineModel.SCHEMA + "\",\n" +
                "  \"container\":\"ZIPRAF_INTEGRITY_CUSTODY\",\n" +
                "  \"generated_epoch_ms\":" + epochMs + ",\n" +
                "  \"claim_allowed\":false,\n" +
                "  \"claim_policy\":\"" + json(AssurancePipelineModel.CLAIM_POLICY) + "\",\n" +
                "  \"star_expression\":\"" + json(AssurancePipelineModel.STAR_EXPRESSION) + "\",\n" +
                "  \"star_numeric_score\":\"TOKEN_VAZIO_NOT_DEFINED\",\n" +
                "  \"star_state\":\"" + json(starState) + "\",\n" +
                "  \"raw_evidence_name\":\"" + json(rawEvidenceName) + "\",\n" +
                "  \"raw_evidence_sha256\":\"" + rawEvidenceSha + "\",\n" +
                "  \"installed_apk_sha256\":\"" + apkSha + "\",\n" +
                "  \"materialized_count\":" + materializedCount + ",\n" +
                "  \"relation_count\":" + relationCount + ",\n" +
                "  \"gap_count\":" + gapCount + ",\n" +
                "  \"external_standard_audit\":\"NOT_AUDITED\",\n" +
                "  \"integrity_note\":\"SHA-256 binds bytes; ZIPRAF does not by itself prove meaning, authenticity, scientific validity, or standards conformity\"\n" +
                "}\n";
    }

    private static String materialJson(
            String apkSha, String zrfSha, String cfrSha, String pcmSha) {
        return "{\n" +
                "  \"dagger_material\":{\n" +
                "    \"installed_apk_sha256\":\"" + apkSha + "\",\n" +
                "    \"source_sha\":\"" + json(BuildConfig.SOURCE_SHA) + "\",\n" +
                "    \"ci_run_id\":\"" + json(BuildConfig.CI_RUN_ID) + "\",\n" +
                "    \"last_zrf_sha256\":\"" + zrfSha + "\",\n" +
                "    \"last_cfr_sha256\":\"" + cfrSha + "\",\n" +
                "    \"last_mastered_pcm_sha256\":\"" + pcmSha + "\"\n" +
                "  }\n" +
                "}\n";
    }

    private static String materializedJson(
            String rawName, String rawSha,
            String zrfSha, String cfrSha, String pcmSha,
            int count) {
        return "{\n" +
                "  \"double_dagger_materialized_n\":" + count + ",\n" +
                "  \"raw_evidence\":{\"name\":\"" + json(rawName) +
                "\",\"sha256\":\"" + rawSha + "\"},\n" +
                "  \"zrf_sha256\":\"" + zrfSha + "\",\n" +
                "  \"cfr_sha256\":\"" + cfrSha + "\",\n" +
                "  \"mastered_pcm_sha256\":\"" + pcmSha + "\"\n" +
                "}\n";
    }

    private static String metricsJson(
            MicroDeltaVibrationProbe.Result vibration,
            MicroDeltaMagnetometerProbe.Result magnetometer,
            RelativeCalibrationEngine.Result calibration) {
        String vibState = AssurancePipelineModel.sensorMetricState(
                vibration == null ? null : vibration.state,
                vibration == null ? 0 : vibration.samples,
                vibration != null &&
                        AssurancePipelineModel.isFinite(vibration.rmsDeltaMs2) &&
                        AssurancePipelineModel.isFinite(vibration.peakDeltaMs2),
                true);
        String magState = AssurancePipelineModel.sensorMetricState(
                magnetometer == null ? null : magnetometer.state,
                magnetometer == null ? 0 : magnetometer.samples,
                magnetometer != null &&
                        AssurancePipelineModel.isFinite(magnetometer.rmsDeltaUt) &&
                        AssurancePipelineModel.isFinite(magnetometer.peakDeltaUt),
                true);

        String vib = vibration == null ?
                "{\"state\":\"NOT_RUN\"}" :
                "{\"state\":\"" + vibState +
                        "\",\"source_state\":\"" + json(vibration.state) +
                        "\",\"samples\":" + vibration.samples +
                        ",\"effective_hz\":" + f6(vibration.effectiveHz) +
                        ",\"rms_delta\":" + f6(vibration.rmsDeltaMs2) +
                        ",\"peak_delta\":" + f6(vibration.peakDeltaMs2) +
                        ",\"unit\":\"m/s^2\",\"scale\":\"ANDROID_SENSOR_REPORTED\"}";
        String mag = magnetometer == null ?
                "{\"state\":\"NOT_RUN\"}" :
                "{\"state\":\"" + magState +
                        "\",\"source_state\":\"" + json(magnetometer.state) +
                        "\",\"samples\":" + magnetometer.samples +
                        ",\"effective_hz\":" + f6(magnetometer.effectiveHz) +
                        ",\"rms_delta\":" + f6(magnetometer.rmsDeltaUt) +
                        ",\"peak_delta\":" + f6(magnetometer.peakDeltaUt) +
                        ",\"unit\":\"uT\",\"scale\":\"ANDROID_SENSOR_REPORTED\"}";

        String cal;
        if (calibration == null) {
            cal = "{\"state\":\"NOT_RUN\",\"absolute_spl\":\"PENDING_PHYSICAL_REFERENCE\"}";
        } else {
            long rt = calibration.preferredRt60Millis();
            cal = "{\"state\":\"OBSERVED_RELATIVE_UNCALIBRATED\"" +
                    ",\"latency_us\":" + calibration.latencyMicros() +
                    ",\"best_lag_samples\":" + calibration.bestLag +
                    ",\"valid_transfer_bands\":" + calibration.validTransferBands() +
                    ",\"decay_method\":\"" + json(calibration.decayMethod()) + "\"" +
                    ",\"rt60_relative_ms\":\"" +
                    (rt >= 0L ? Long.toString(rt) : "TOKEN_VAZIO_DYNAMIC_RANGE") + "\"" +
                    ",\"absolute_spl\":\"PENDING_PHYSICAL_REFERENCE\"}";
        }

        return "{\n" +
                "  \"delta_behavior\":{\n" +
                "    \"vibration\":" + vib + ",\n" +
                "    \"magnetic\":" + mag + ",\n" +
                "    \"relative_room\":" + cal + "\n" +
                "  },\n" +
                "  \"metric_validity_policy\":\"availability+unit+execution+finite-value+required-reference; unavailable sensor is not a metric failure; no external standard audit inferred\"\n" +
                "}\n";
    }

    private static String relationsJson(
            String apkSha,
            String evidenceSha,
            String zrfSha,
            String cfrSha,
            String pcmSha,
            boolean sourceBound,
            boolean ciBound,
            String vibrationState,
            String magneticState) {
        return "{\n" +
                "  \"paragraph_relations\":[\n" +
                edge("source_sha", "BUILDS", "installed_apk", sourceBound ? "BOUND" : "TOKEN_VAZIO") + ",\n" +
                edge("ci_run_id", "EXECUTES_BUILD_GATE", "installed_apk", ciBound ? "BOUND" : "TOKEN_VAZIO") + ",\n" +
                edge("installed_apk:" + apkSha, "EXECUTES", "raw_evidence:" + evidenceSha, "OBSERVED") + ",\n" +
                edge("raw_evidence", "OBSERVES", "vibration_delta", sensorRelationState(vibrationState)) + ",\n" +
                edge("raw_evidence", "OBSERVES", "magnetic_delta", sensorRelationState(magneticState)) + ",\n" +
                edge("zrf:" + zrfSha, "MATERIALIZES", "capture_path", relationState(zrfSha)) + ",\n" +
                edge("cfr:" + cfrSha, "MATERIALIZES", "relative_calibration_path", relationState(cfrSha)) + ",\n" +
                edge("pcm:" + pcmSha, "MATERIALIZES", "master_path", relationState(pcmSha)) + ",\n" +
                edge("metrics", "BOUNDS", "claims", "CLAIM_ALLOWED_FALSE") + "\n" +
                "  ]\n" +
                "}\n";
    }

    private static String gapsJson(
            boolean sourceBound,
            boolean ciBound,
            boolean vibrationContract,
            boolean magneticContract,
            int gapCount) {
        return "{\n" +
                "  \"empty_gap_count\":" + gapCount + ",\n" +
                "  \"gaps\":[\n" +
                gap("absolute_spl_reference", "PENDING_PHYSICAL_REFERENCE", "provide traceable physical acoustic reference") + ",\n" +
                gap("independent_reproduction", "TOKEN_VAZIO", "repeat on independently controlled installation/device") + ",\n" +
                gap("external_standard_audit", "NOT_AUDITED", "scoped external audit evidence required before any conformity claim") +
                (sourceBound ? "" : ",\n" + gap("source_sha", "TOKEN_VAZIO", "build through source-bound CI")) +
                (ciBound ? "" : ",\n" + gap("ci_run_id", "TOKEN_VAZIO", "build through CI with run provenance")) +
                ((vibrationContract && magneticContract) ? "" : ",\n" +
                        gap("metric_contract", "FAIL_METRIC_CONTRACT", "rerun available probes with finite values and sufficient samples")) +
                "\n  ]\n" +
                "}\n";
    }

    private static String claimsJson(
            String apkSha,
            boolean sourceBound,
            boolean ciBound,
            String vibrationState,
            int vibrationSamples,
            String magneticState,
            int magneticSamples,
            RelativeCalibrationEngine.Result calibration) {
        return "{\n" +
                "  \"claim_allowed\":false,\n" +
                "  \"claims\":[\n" +
                claim("installed_apk_byte_identity",
                        apkSha.length() == 64 ? "PROVABLE_SCOPED" : "TOKEN_VAZIO_EVIDENCE") + ",\n" +
                claim("source_ci_binding",
                        (sourceBound && ciBound) ? "PROVABLE_SCOPED" : "TOKEN_VAZIO_PROVENANCE") + ",\n" +
                claim("vibration_behavior_observation",
                        sensorClaimState(vibrationState, vibrationSamples)) + ",\n" +
                claim("magnetic_behavior_observation",
                        sensorClaimState(magneticState, magneticSamples)) + ",\n" +
                claim("relative_room_path",
                        calibration != null ? "OBSERVED_RELATIVE_UNCALIBRATED" : "NOT_RUN") + ",\n" +
                claim("absolute_spl", "TOKEN_VAZIO_PHYSICAL_REFERENCE") + ",\n" +
                claim("external_standards_conformity", AssurancePipelineModel.NOT_AUDITED) + ",\n" +
                claim("scientific_causality", "NOT_CLAIMED") + "\n" +
                "  ]\n" +
                "}\n";
    }

    private static String receiptText(
            long epochMs,
            int materializedCount,
            int relationCount,
            int gapCount,
            String starState) {
        return "schema=" + AssurancePipelineModel.SCHEMA + NL +
                "generated_epoch_ms=" + epochMs + NL +
                "star_expression=" + AssurancePipelineModel.STAR_EXPRESSION + NL +
                "dagger_material=BOUND_TO_INSTALLED_APK" + NL +
                "double_dagger_materialized_n=" + materializedCount + NL +
                "empty_gap_count=" + gapCount + NL +
                "delta_behavior=RUNTIME_PROBE_OR_NOT_RUN" + NL +
                "section_metric_contract=AVAILABILITY_THEN_EXPLICIT_UNITS_AND_REFERENCE_GATES" + NL +
                "paragraph_relations=" + relationCount + NL +
                "star_numeric_score=TOKEN_VAZIO_NOT_DEFINED" + NL +
                "star_state=" + starState + NL +
                "external_standard_audit=NOT_AUDITED" + NL +
                "claim_allowed=false" + NL +
                "zipraf_encryption=NO" + NL +
                "zipraf_authenticity=NOT_PROVEN_BY_CONTAINER" + NL +
                "source_artifact_execution_evidence_claim=SEPARATE" + NL;
    }

    private static String sensorRelationState(String state) {
        if (state == null || "NOT_RUN".equals(state)) return "NOT_RUN";
        if (AssurancePipelineModel.isSensorUnavailable(state)) return state;
        if ("INSUFFICIENT_EVIDENCE".equals(state)) return state;
        return "SCOPED";
    }

    private static String sensorClaimState(String state, int samples) {
        if (state == null || "NOT_RUN".equals(state)) return "NOT_RUN";
        if (AssurancePipelineModel.isSensorUnavailable(state)) return state;
        if (!AssurancePipelineModel.isObservedSensorState(state, samples)) {
            return "INSUFFICIENT_EVIDENCE";
        }
        return "OBSERVED_UNPROMOTED";
    }

    private static String edge(String from, String relation, String to, String state) {
        return "    {\"from\":\"" + json(from) + "\",\"relation\":\"" +
                json(relation) + "\",\"to\":\"" + json(to) +
                "\",\"state\":\"" + json(state) + "\"}";
    }

    private static String gap(String id, String state, String exitCriterion) {
        return "    {\"id\":\"" + json(id) + "\",\"state\":\"" +
                json(state) + "\",\"exit_criterion\":\"" +
                json(exitCriterion) + "\"}";
    }

    private static String claim(String id, String state) {
        return "    {\"id\":\"" + json(id) + "\",\"state\":\"" +
                json(state) + "\"}";
    }

    private static String relationState(String hash) {
        return hash.startsWith("TOKEN_VAZIO") ? "TOKEN_VAZIO" : "BOUND_SHA256";
    }

    private static void put(TreeMap<String, byte[]> entries, String name, String text) {
        entries.put(name, text.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeStoredEntry(
            ZipOutputStream zip, String name, byte[] bytes) throws Exception {
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, bytes.length);
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(bytes.length);
        entry.setCompressedSize(bytes.length);
        entry.setCrc(crc.getValue());
        entry.setTime(0L);
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private static byte[] readUriBytes(Context context, Uri uri) throws Exception {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("raw evidence stream unavailable");
            ByteArrayOutputStream out = new ByteArrayOutputStream(16384);
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) >= 0) {
                if (n > 0) out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static String sha256FileOrToken(File file) throws Exception {
        if (file == null || !file.isFile()) return "TOKEN_VAZIO_NOT_MATERIALIZED";
        return sha256File(file);
    }

    private static String sha256File(File file) throws Exception {
        LowSha256 digest = new LowSha256();
        byte[] buffer = new byte[8192];
        try (InputStream in = new FileInputStream(file)) {
            int n;
            while ((n = in.read(buffer)) >= 0) {
                if (n > 0) digest.update(buffer, 0, n);
            }
        }
        return hex(digest.finish());
    }

    private static String sha256Bytes(byte[] bytes) {
        LowSha256 digest = new LowSha256();
        digest.update(bytes, 0, bytes.length);
        return hex(digest.finish());
    }

    private static String hex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        final char[] alphabet = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; ++i) {
            int v = bytes[i] & 0xff;
            out[i * 2] = alphabet[v >>> 4];
            out[i * 2 + 1] = alphabet[v & 15];
        }
        return new String(out);
    }

    private static String f6(double value) {
        if (!AssurancePipelineModel.isFinite(value)) return "null";
        long scaled = Math.round(value * 1000000.0);
        long whole = scaled / 1000000L;
        long frac = Math.abs(scaled % 1000000L);
        String digits = Long.toString(frac);
        StringBuilder b = new StringBuilder();
        b.append(whole).append('.');
        for (int i = digits.length(); i < 6; ++i) b.append('0');
        b.append(digits);
        return b.toString();
    }

    private static String json(String value) {
        if (value == null) return "TOKEN_VAZIO_NULL";
        StringBuilder b = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            if (c == '\\' || c == '"') b.append('\\').append(c);
            else if (c == '\n') b.append("\\n");
            else if (c == '\r') b.append("\\r");
            else if (c == '\t') b.append("\\t");
            else if (c < 0x20) b.append('?');
            else b.append(c);
        }
        return b.toString();
    }
}
