/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

#include "rfa_verbo_core.h"

static rfa_verbo_input valid_input(void) {
    rfa_verbo_input in = {
        1u, 1u, 1u, 1u, 1u, 1u,
        1u, 1u, 1u, 1u, 1u, 1u
    };
    return in;
}

int main(void) {
    rfa_verbo_input in = valid_input();
    rfa_verbo_result out;
    unsigned int *gates[11];
    const unsigned int bits[11] = {
        RFA_VERBO_GAP_SOURCE,
        RFA_VERBO_GAP_ARTIFACT,
        RFA_VERBO_GAP_EXECUTION,
        RFA_VERBO_GAP_EVIDENCE,
        RFA_VERBO_GAP_AUTHORITY,
        RFA_VERBO_GAP_CONSENT,
        RFA_VERBO_GAP_PRIVACY,
        RFA_VERBO_GAP_DIGNITY,
        RFA_VERBO_GAP_CHILD_SAFETY,
        RFA_VERBO_GAP_HUMAN_OVERSIGHT,
        RFA_VERBO_GAP_REVERSIBILITY
    };
    int i;

    gates[0] = &in.source;
    gates[1] = &in.artifact;
    gates[2] = &in.execution;
    gates[3] = &in.evidence;
    gates[4] = &in.authority;
    gates[5] = &in.consent;
    gates[6] = &in.privacy;
    gates[7] = &in.dignity;
    gates[8] = &in.child_safety;
    gates[9] = &in.human_oversight;
    gates[10] = &in.reversible;

    out = rfa_verbo_evaluate(in);
    if (out.claim_allowed != 1u || out.gap_mask != 0u) return 1;
    if (out.body != RFA_VERBO_PASS || out.soul != RFA_VERBO_PASS ||
        out.spirit != RFA_VERBO_PASS) return 2;

    for (i = 0; i < 11; ++i) {
        in = valid_input();
        *gates[i] = 0u;
        out = rfa_verbo_evaluate(in);
        if (out.claim_allowed != 0u || (out.gap_mask & bits[i]) == 0u) return 10 + i;
        if ((i == 0 || i == 4) && out.soul != RFA_VERBO_AUDIT) return 30 + i;
        if (i >= 5 && out.spirit != RFA_VERBO_BLOCKED) return 40 + i;
    }

    in = valid_input();
    in.claim_requested = 0u;
    out = rfa_verbo_evaluate(in);
    if (out.claim_allowed != 0u || out.gap_mask != 0u) return 60;

    in = valid_input();
    in.artifact = 0u;
    in.consent = 0u;
    out = rfa_verbo_evaluate(in);
    if (out.claim_allowed != 0u) return 61;
    if ((out.gap_mask & (RFA_VERBO_GAP_ARTIFACT | RFA_VERBO_GAP_CONSENT)) !=
        (RFA_VERBO_GAP_ARTIFACT | RFA_VERBO_GAP_CONSENT)) return 62;

    return 0;
}
