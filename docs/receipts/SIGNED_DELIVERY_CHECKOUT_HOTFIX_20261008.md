# Signed-delivery source checkout regression (2026-10-08)

- Root cause run: https://github.com/rafaelmeloreisnovo/EstudioAudio/actions/runs/37727206394
- Source head observed: `214bf48da2adf89af6d707e6cacca35437461b66`
- `gate / build`: SUCCESS; `delivery / signed + store`: FAIL.
- Observed failure: `bash: ci/provider-enforcement-gate.sh: No such file or directory` (exit 127). The source script exists; job invoked it before its checkout step.
- Independent provider failure: https://github.com/rafaelmeloreisnovo/EstudioAudio/actions/runs/37726762798, `main.protected=false`, FAIL expected and retained.
- Delta: exact SHA checkout precedes source script, which remains before signing/bootstrap/publication. Dedicated host source contract asserts order.
- NOT CLAIMED: provider protection configured; signed release produced; physical APK executed; acoustic conformity.
- Rollback: revert this PR; historical run records retained.
- Expected next execution when signed is explicitly requested and `main.protected=false`: fail-closed `MAIN_PROTECTION=false expected=true`, **not** missing script.
- R3: F_ok=source-order defect fixed in PR, F_gap=CI exact-head result and GitHub provider protection, F_next=PR CI then owner-authorized protection configuration/readback and physical receipt.
