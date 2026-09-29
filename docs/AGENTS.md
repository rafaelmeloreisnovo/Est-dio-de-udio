# AGENTS.md — docs/

Documentation is part of the evidence architecture.

## Document classes

- **contract**: invariants and interfaces;
- **guide**: operational instructions;
- **reference**: schemas/formats/APIs;
- **protocol**: repeatable test procedure;
- **receipt**: append-only executed evidence;
- **brief**: public/product communication.

Do not convert a brief into authority for technical truth. Claims must trace back to code, a contract, or an executed receipt.

## Required labels

Use explicit states when material:

`PASS | FAIL | NOT_RUN | PENDING | AUDIT | TOKEN_VAZIO | IMPLEMENTED_UNTESTED | OBSERVED_UNPROMOTED`.

## Publication rule

Public-facing documents may simplify language, but cannot strengthen the claim.
Example: “supports relative calibration workflow” is allowed when implemented; “laboratory-grade calibrated SPL meter” is not allowed without the corresponding reference and validation.
