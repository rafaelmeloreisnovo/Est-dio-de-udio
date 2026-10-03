/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

#ifndef RFA_VERBO_CORE_H
#define RFA_VERBO_CORE_H

#include "rfa_core_types.h"

/* VERBO is a deterministic governance gate, not a source of evidence. */
typedef enum {
    RFA_VERBO_TOKEN_VAZIO = 0,
    RFA_VERBO_NOT_RUN = 1,
    RFA_VERBO_PENDING = 2,
    RFA_VERBO_AUDIT = 3,
    RFA_VERBO_IMPLEMENTED_UNTESTED = 4,
    RFA_VERBO_PASS = 5,
    RFA_VERBO_FAIL = 6,
    RFA_VERBO_BLOCKED = 7
} rfa_verbo_state;

typedef struct {
    unsigned int source;
    unsigned int artifact;
    unsigned int execution;
    unsigned int evidence;
    unsigned int authority;
    unsigned int consent;
    unsigned int privacy;
    unsigned int dignity;
    unsigned int child_safety;
    unsigned int human_oversight;
    unsigned int reversible;
    unsigned int claim_requested;
} rfa_verbo_input;

typedef struct {
    rfa_verbo_state body;
    rfa_verbo_state soul;
    rfa_verbo_state spirit;
    unsigned int claim_allowed;
    unsigned int gap_mask;
} rfa_verbo_result;

enum {
    RFA_VERBO_GAP_SOURCE = 1u << 0,
    RFA_VERBO_GAP_AUTHORITY = 1u << 1,
    RFA_VERBO_GAP_EXECUTION = 1u << 2,
    RFA_VERBO_GAP_EVIDENCE = 1u << 3,
    RFA_VERBO_GAP_PRIVACY = 1u << 4,
    RFA_VERBO_GAP_DIGNITY = 1u << 5,
    RFA_VERBO_GAP_CHILD_SAFETY = 1u << 6,
    RFA_VERBO_GAP_HUMAN_OVERSIGHT = 1u << 7,
    RFA_VERBO_GAP_REVERSIBILITY = 1u << 8,
    RFA_VERBO_GAP_ARTIFACT = 1u << 9,
    RFA_VERBO_GAP_CONSENT = 1u << 10
};

rfa_verbo_result rfa_verbo_evaluate(rfa_verbo_input in);

#endif
