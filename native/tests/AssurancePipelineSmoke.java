/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

public final class AssurancePipelineSmoke {
    private AssurancePipelineSmoke() {}

    private static void expect(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void expectFalse(boolean value, String message) {
        if (value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        expect(
                "TOKEN_VAZIO_MATERIAL",
                AssurancePipelineModel.starState(false, 0, 0, false, false, false));
        expect(
                "TOKEN_VAZIO_MATERIALIZATION",
                AssurancePipelineModel.starState(true, 0, 0, true, true, true));
        expect(
                "OBSERVED_UNPROMOTED_PROVENANCE_GAP",
                AssurancePipelineModel.starState(true, 2, 0, true, true, false));
        expect(
                "OBSERVED_UNPROMOTED_GAPS",
                AssurancePipelineModel.starState(true, 2, 1, true, true, true));
        expect(
                "PASS_PACKAGE_STRUCTURE_SCOPED",
                AssurancePipelineModel.starState(true, 2, 0, true, true, true));

        expect(
                "NOT_RUN",
                AssurancePipelineModel.metricState(false, false, true, false, false));
        expect(
                "FAIL_METRIC_CONTRACT",
                AssurancePipelineModel.metricState(true, false, true, false, false));
        expect(
                "OBSERVED_UNCALIBRATED",
                AssurancePipelineModel.metricState(true, true, true, true, false));
        expect(
                "OBSERVED_METRIC_SCOPED",
                AssurancePipelineModel.metricState(true, true, true, false, false));

        expect(
                "TOKEN_VAZIO_SENSOR_UNAVAILABLE",
                AssurancePipelineModel.sensorMetricState(
                        "TOKEN_VAZIO_SENSOR_UNAVAILABLE", 0, true, true));
        expect(
                "TOKEN_VAZIO_SENSOR_UNAVAILABLE",
                AssurancePipelineModel.sensorMetricState(
                        "TOKEN_VAZIO_SENSOR_UNAVAILABLE_NOT_PRESENT", 0, true, true));
        expect(
                "TOKEN_VAZIO_SENSOR_UNAVAILABLE",
                AssurancePipelineModel.sensorMetricState(
                        "TOKEN_VAZIO_SENSOR_UNAVAILABLE_SERVICE_UNAVAILABLE", 0, true, true));
        expect(
                "TOKEN_VAZIO_SENSOR_UNAVAILABLE",
                AssurancePipelineModel.sensorMetricState(
                        "TOKEN_VAZIO_SENSOR_UNAVAILABLE_LISTENER_REGISTRATION_FAILED", 0, true, true));
        expect(
                "INSUFFICIENT_EVIDENCE",
                AssurancePipelineModel.sensorMetricState(
                        "INSUFFICIENT_EVIDENCE", 1, true, true));
        expect(
                "FAIL_METRIC_CONTRACT",
                AssurancePipelineModel.sensorMetricState(
                        "OBSERVED_UNPROMOTED", 16, false, true));
        expect(
                "OBSERVED_METRIC_SCOPED",
                AssurancePipelineModel.sensorMetricState(
                        "OBSERVED_UNPROMOTED", 16, true, true));
        expectTrue(
                AssurancePipelineModel.isSensorUnavailable(
                        "TOKEN_VAZIO_SENSOR_UNAVAILABLE"),
                "generic unavailable sensor state must remain explicit");
        expectTrue(
                AssurancePipelineModel.isSensorUnavailable(
                        "TOKEN_VAZIO_SENSOR_UNAVAILABLE_NOT_PRESENT"),
                "missing hardware must remain an unavailable sensor state");
        expectTrue(
                AssurancePipelineModel.isSensorUnavailable(
                        "TOKEN_VAZIO_SENSOR_UNAVAILABLE_SERVICE_UNAVAILABLE"),
                "missing platform service must remain an unavailable sensor state");
        expectTrue(
                AssurancePipelineModel.isSensorUnavailable(
                        "TOKEN_VAZIO_SENSOR_UNAVAILABLE_LISTENER_REGISTRATION_FAILED"),
                "registration failure must remain an unavailable sensor state");
        expectFalse(
                AssurancePipelineModel.isObservedSensorState(
                        "TOKEN_VAZIO_SENSOR_UNAVAILABLE_NOT_PRESENT", 0),
                "unavailable sensor cannot become observed evidence");
        expectTrue(
                AssurancePipelineModel.isObservedSensorState(
                        "OBSERVED_UNPROMOTED", 16),
                "sufficient observed sensor samples must remain observable");

        expect(
                "NOT_AUDITED",
                AssurancePipelineModel.claimState(true, true, true, false));
        expect(
                "PROVABLE_SCOPED",
                AssurancePipelineModel.claimState(true, true, false, false));

        if (!AssurancePipelineModel.isConcreteIdentity(
                "30f4456508be29cdfc256a6973b35ad8774c64dd")) {
            throw new AssertionError("exact SHA must be a concrete identity");
        }
        if (AssurancePipelineModel.isConcreteIdentity("TOKEN_VAZIO")) {
            throw new AssertionError("TOKEN_VAZIO cannot become identity evidence");
        }
        if (AssurancePipelineModel.isConcreteIdentity("UNAVAILABLE_LOCAL_BUILD")) {
            throw new AssertionError("unavailable build coordinate cannot become evidence");
        }

        System.out.println("ASSURANCE_PIPELINE_SMOKE=PASS");
        System.out.println("SENSOR_UNAVAILABLE_SEMANTICS=PASS");
        System.out.println("SENSOR_ACCESS_CONTRACT=NO_RUNTIME_PERMISSION_REQUIRED_CURRENT_PROFILE");
        System.out.println("COMPLIANCE_CLAIM=NOT_AUDITED");
        System.out.println("CLAIM_ALLOWED=false");
    }
}
