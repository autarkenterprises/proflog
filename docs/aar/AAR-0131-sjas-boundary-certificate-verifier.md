# AAR-0131: SJAS Boundary Constructed-Certificate Verifier

- Date: 2026-06-18
- ADR: [ADR-0131](../adr/ADR-0131-sjas-boundary-certificate-verifier.md)
- Branch: `adr-0131-sjas-boundary-certificate-verifier`

## Outcome

ADR-0131 is complete as the constructed-certificate verifier contract for
Workstream B boundary evidence.

The implementation adds:

- `verify-boundary-constructed-certificate`, a public verifier that runs the
  ADR-0127 screen before considering proof-validation metadata;
- proof-validation checks for variant, system code, generated SelfCons code,
  target formula, proof code, and success status;
- Workstream B audit metadata under
  `:evidence-verifiers :total-multiplication :constructed-certificate`.

A successful verifier result marks only the candidate's
`:constructed-certificate` obligation as intermediate evidence and keeps
`:proof-search-synthesis` open. The roadmap still keeps both
total-multiplication final obligations open because this ADR does not provide a
concrete certificate.

## Evidence

Initial red selectors failed as intended because the public verifier did not
exist:

```text
No such var: correspondence/verify-boundary-constructed-certificate
```

Focused green selectors:

```text
boundary-failure-roadmap-keeps-witness-contract-open
Ran 1 tests containing 15 assertions.
0 failures, 0 errors.

boundary-constructed-certificate-verifier-requires-screen-and-proof-validation
Ran 1 tests containing 11 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 226 tests containing 1428 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1173 fail=0 error=0
```

## Follow-up

- Build a concrete total-multiplication constructed certificate that produces a
  matching successful proof-validation record.
- Add proof-search synthesis evidence for the total-multiplication target with
  durable `test-runs/` logs and PID files.
- Add reduced and full targets for the Tab-2-or-stronger and Xtab/LEM variants.
