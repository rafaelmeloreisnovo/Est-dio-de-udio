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
        System.out.println("COMPLIANCE_CLAIM=NOT_AUDITED");
        System.out.println("CLAIM_ALLOWED=false");
    }
}
