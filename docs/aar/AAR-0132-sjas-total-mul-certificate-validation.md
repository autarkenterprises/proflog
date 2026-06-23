# AAR-0132: SJAS Total-Multiplication Certificate Validation

- Date: 2026-06-18
- ADR: [ADR-0132](../adr/ADR-0132-sjas-total-mul-certificate-validation.md)
- Branch: `adr-0132-sjas-total-mul-certificate-validation`

## Outcome

ADR-0132 is complete as the target-specific proof-validation bridge for
total-multiplication constructed-certificate evidence.

The implementation adds:

- `total-multiplication-constructed-certificate-validation`, which validates a
  supplied proof code through the public `tableau-proof/3` predicate against
  the total-multiplication reduced-witness system's generated Group-3 SelfCons
  theorem code;
- `:target-code` in the ADR-0126 target report and validation records, so
  independently rebuilt alpha-equivalent target formulas can be compared by
  stable code;
- certificate-kind classification for supplied proof codes;
- ADR-0131 verifier hardening that rejects mismatched candidate and validation
  certificate kinds;
- Workstream B audit metadata naming the validation helper.

The public `sjas-axiom` Group-3 citation validates as an SJAS proof, but it is
classified as `:sjas-axiom`; the verifier therefore rejects it when presented
as a constructed structural-tableau boundary certificate. No concrete
contradiction certificate or proof-search synthesis evidence is claimed.

## Evidence

Initial red selectors failed as intended:

```text
sjas-total-multiplication-certificate-validation-targets-generated-selfcons
No such var: sjas/total-multiplication-constructed-certificate-validation

boundary-constructed-certificate-verifier-requires-screen-and-proof-validation
expected #{:wrong-certificate-kind}, actual #{}
```

Focused green selectors:

```text
sjas-total-multiplication-certificate-validation-targets-generated-selfcons
Ran 1 tests containing 15 assertions.
0 failures, 0 errors.

sjas-total-multiplication-full-target-names-generated-selfcons
Ran 1 tests containing 12 assertions.
0 failures, 0 errors.

boundary-constructed-certificate-verifier-requires-screen-and-proof-validation
Ran 1 tests containing 13 assertions.
0 failures, 0 errors.

boundary-failure-roadmap-keeps-witness-contract-open
Ran 1 tests containing 16 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 226 tests containing 1431 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1188 fail=0 error=0
```

## Follow-up

- Build a concrete nontrivial structural-tableau certificate for the
  total-multiplication generated SelfCons target.
- Add proof-search synthesis evidence for the same target with durable
  `test-runs/` logs and PID files.
- Add reduced and full targets for the Tab-2-or-stronger and Xtab/LEM variants.
