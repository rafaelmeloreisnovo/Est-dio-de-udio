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
    unsigned int authority_ok = !!in.authority;
    unsigned int execution_ok = !!in.execution;
    unsigned int evidence_ok = !!in.evidence;
    unsigned int privacy_ok = !!in.privacy;
    unsigned int dignity_ok = !!in.dignity;
    unsigned int child_ok = !!in.child_safety;
    unsigned int oversight_ok = !!in.human_oversight;
    unsigned int reversible_ok = !!in.reversible;
    unsigned int technical_ok = source_ok & authority_ok & execution_ok & evidence_ok;
    unsigned int ethical_ok = privacy_ok & dignity_ok & child_ok & oversight_ok & reversible_ok;
    unsigned int promotable = technical_ok & ethical_ok;

    out.gap_mask =
        ((!source_ok) * RFA_VERBO_GAP_SOURCE) |
        ((!authority_ok) * RFA_VERBO_GAP_AUTHORITY) |
        ((!execution_ok) * RFA_VERBO_GAP_EXECUTION) |
        ((!evidence_ok) * RFA_VERBO_GAP_EVIDENCE) |
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
