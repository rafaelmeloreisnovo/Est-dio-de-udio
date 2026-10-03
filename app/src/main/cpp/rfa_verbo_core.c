/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

#include "rfa_verbo_core.h"

/*
 * SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
 * TOKEN_VAZIO != 0
 * IMPLEMENTED_UNTESTED != PASS
 *
 * Branch-light bitwise evaluation. VERBO can block a claim; it cannot create
 * execution or evidence that was not observed by the surrounding system.
 */
rfa_verbo_result rfa_verbo_evaluate(rfa_verbo_input in) {
    rfa_verbo_result out;
    unsigned int source_ok = !!in.source;
    unsigned int artifact_ok = !!in.artifact;
    unsigned int authority_ok = !!in.authority;
    unsigned int execution_ok = !!in.execution;
    unsigned int evidence_ok = !!in.evidence;
    unsigned int consent_ok = !!in.consent;
    unsigned int privacy_ok = !!in.privacy;
    unsigned int dignity_ok = !!in.dignity;
    unsigned int child_ok = !!in.child_safety;
    unsigned int oversight_ok = !!in.human_oversight;
    unsigned int reversible_ok = !!in.reversible;
    unsigned int technical_ok = source_ok & authority_ok & artifact_ok & execution_ok & evidence_ok;
    unsigned int ethical_ok = consent_ok & privacy_ok & dignity_ok & child_ok & oversight_ok & reversible_ok;
    unsigned int promotable = technical_ok & ethical_ok;

    out.gap_mask =
        ((!source_ok) * RFA_VERBO_GAP_SOURCE) |
        ((!authority_ok) * RFA_VERBO_GAP_AUTHORITY) |
        ((!artifact_ok) * RFA_VERBO_GAP_ARTIFACT) |
        ((!execution_ok) * RFA_VERBO_GAP_EXECUTION) |
        ((!evidence_ok) * RFA_VERBO_GAP_EVIDENCE) |
        ((!consent_ok) * RFA_VERBO_GAP_CONSENT) |
        ((!privacy_ok) * RFA_VERBO_GAP_PRIVACY) |
        ((!dignity_ok) * RFA_VERBO_GAP_DIGNITY) |
        ((!child_ok) * RFA_VERBO_GAP_CHILD_SAFETY) |
        ((!oversight_ok) * RFA_VERBO_GAP_HUMAN_OVERSIGHT) |
        ((!reversible_ok) * RFA_VERBO_GAP_REVERSIBILITY);

    out.body = (rfa_verbo_state)(RFA_VERBO_IMPLEMENTED_UNTESTED +
        promotable * (RFA_VERBO_PASS - RFA_VERBO_IMPLEMENTED_UNTESTED));
    out.soul = (rfa_verbo_state)(RFA_VERBO_AUDIT +
        (source_ok & authority_ok) * (RFA_VERBO_PASS - RFA_VERBO_AUDIT));
    out.spirit = (rfa_verbo_state)(RFA_VERBO_BLOCKED +
        ethical_ok * (RFA_VERBO_PASS - RFA_VERBO_BLOCKED));

    out.claim_allowed = (!!in.claim_requested) & promotable;
    return out;
}
