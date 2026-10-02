/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */
package io.rafaelia.audiostudio;

/**
 * Pure-Java qualification model for the in-app assurance/ZIPRAF pipeline.
 *
 * This class deliberately does not turn symbolic composition into a fake score.
 * STAR is a state derived from evidence predicates; unknowns remain explicit.
 */
final class AssurancePipelineModel {
    static final String SCHEMA = "rafaelia.assurance-pipeline/v1";
    static final String STAR_EXPRESSION =
            "★={†[material]}×{‡([materialized^n])+∅×∆×§×¶}";

    static final String NOT_AUDITED = "NOT_AUDITED";
    static final String CLAIM_POLICY =
            "SOURCE!=ARTIFACT!=EXECUTION!=EVIDENCE!=CLAIM;" +
            "TOKEN_VAZIO!=0;IMPLEMENTED_UNTESTED!=PASS";

    private AssurancePipelineModel() {}

    static String starState(
            boolean materialBound,
            int materializedCount,
            int unresolvedCount,
            boolean deltaObserved,
            boolean metricContractResolved,
            boolean provenanceResolved) {
        if (!materialBound) return "TOKEN_VAZIO_MATERIAL";
        if (materializedCount <= 0) return "TOKEN_VAZIO_MATERIALIZATION";
        if (!provenanceResolved) return "OBSERVED_UNPROMOTED_PROVENANCE_GAP";
        if (!metricContractResolved) return "OBSERVED_UNPROMOTED_METRIC_CONTRACT";
        if (!deltaObserved) return "NOT_RUN_BEHAVIOR";
        if (unresolvedCount > 0) return "OBSERVED_UNPROMOTED_GAPS";
        return "PASS_PACKAGE_STRUCTURE_SCOPED";
    }

    static String metricState(
            boolean ran,
            boolean finite,
            boolean unitDefined,
            boolean requiresExternalReference,
            boolean externalReferencePresent) {
        if (!ran) return "NOT_RUN";
        if (!finite || !unitDefined) return "FAIL_METRIC_CONTRACT";
        if (requiresExternalReference && !externalReferencePresent) {
            return "OBSERVED_UNCALIBRATED";
        }
        return "OBSERVED_METRIC_SCOPED";
    }

    static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    static boolean isConcreteIdentity(String value) {
        if (value == null) return false;
        String v = value.trim();
        if (v.length() == 0) return false;
        return !(v.startsWith("TOKEN_VAZIO") ||
                v.startsWith("UNAVAILABLE") ||
                v.startsWith("PENDING") ||
                v.startsWith("NOT_RUN"));
    }

    static String claimState(
            boolean evidencePresent,
            boolean executionPresent,
            boolean externalAuditRequired,
            boolean externalAuditObserved) {
        if (!evidencePresent) return "TOKEN_VAZIO_EVIDENCE";
        if (!executionPresent) return "NOT_RUN";
        if (externalAuditRequired && !externalAuditObserved) return NOT_AUDITED;
        return "PROVABLE_SCOPED";
    }
}
