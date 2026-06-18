# AAR-0126: SJAS Total Multiplication Full SelfCons Target

- Date: 2026-06-18
- ADR: [ADR-0126](../adr/ADR-0126-sjas-total-mul-full-target.md)
- Branch: `adr-0126-sjas-total-mul-full-target`

## Outcome

ADR-0126 is complete as the full generated SelfCons target-construction stage
for the total-multiplication Workstream B variant.

The implementation adds:

- `selfcons-negation-target`, deriving `not(SelfCons_S)` from a system's
  generated Group-3 theorem formula;
- `selfcons-refutation-target`, constructing
  `AxiomConj(S) /\ not(SelfCons_S)`;
- `total-multiplication-full-target-report`, naming the ADR-0125 reduced
  witness system code, Group-3 theorem code, generated SelfCons formula,
  negated SelfCons formula, and full refutation target.

The Workstream B audit now marks total multiplication as
`:full-target-implemented`, with both required witness stages complete:
`:reduced-reflected-beta-witness` and
`:full-generated-selfcons-contradiction-target`.

This is not a contradiction proof. The audit and report keep
`:constructed-certificate` and `:proof-search-synthesis` open, and the report
marks future synthesis probes as requiring durable logs.

## Evidence

Initial red selectors failed as intended:

```text
boundary-failure-roadmap-keeps-witness-contract-open
expected :full-target-implemented, actual :reduced-witness-implemented
expected full target in completed stages, actual only reduced witness
expected open obligations without full target, actual still included full target

sjas-total-multiplication-full-target-names-generated-selfcons
No such var: sjas/total-multiplication-full-target-report
```

Focused green selectors:

```text
boundary-failure-roadmap-keeps-witness-contract-open
Ran 1 tests containing 9 assertions.
0 failures, 0 errors.

sjas-total-multiplication-full-target-names-generated-selfcons
Ran 1 tests containing 12 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 221 tests containing 1377 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1154 fail=0 error=0
```

## Follow-up

- Build the explicit constructed contradiction certificate against the
  ADR-0126 target.
- Add proof-search synthesis evidence for the same target, using durable
  `test-runs/` logs and PID files for long-running probes.
- Tab-2-or-stronger and Xtab/LEM-as-axiom variants remain open.
