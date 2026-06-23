# AAR-0134: SJAS Xtab / LEM Full SelfCons Target

- Date: 2026-06-18
- ADR: [ADR-0134](../adr/ADR-0134-sjas-xtab-lem-full-target.md)
- Branch: `adr-0134-sjas-xtab-lem-full-target`

## Outcome

ADR-0134 is complete as the full generated SelfCons target-construction stage
for the Xtab/LEM-as-axiom Workstream B variant.

The implementation adds `xtab-lem-full-target-report`, naming the ADR-0133
reduced-witness system code, generated Group-3 theorem code, SelfCons formula,
negated SelfCons formula, full refutation target, and target formula code.

The Workstream B audit now marks `:xtab-or-lem-axiom` as
`:full-target-implemented`, with both required witness stages complete:
`:reduced-reflected-beta-witness` and
`:full-generated-selfcons-contradiction-target`.

This is not a contradiction proof. The audit and report keep
`:constructed-certificate` and `:proof-search-synthesis` open, and the report
marks future synthesis probes as requiring durable logs.

## Evidence

Initial red selectors failed as intended:

```text
sjas-xtab-lem-full-target-names-generated-selfcons
No such var: sjas/xtab-lem-full-target-report

xtab-lem-full-target-records-generated-selfcons-stage
expected :full-target-implemented, actual :reduced-witness-implemented
expected full target in completed stages, actual only reduced witness
```

Focused green selectors:

```text
sjas-xtab-lem-full-target-names-generated-selfcons
Ran 1 tests containing 13 assertions.
0 failures, 0 errors.

xtab-lem-full-target-records-generated-selfcons-stage
Ran 1 tests containing 5 assertions.
0 failures, 0 errors.

boundary-failure-roadmap-keeps-witness-contract-open
Ran 1 tests containing 19 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 228 tests containing 1445 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1212 fail=0 error=0
```

## Follow-up

- Build constructed certificates and proof-search synthesis evidence for the
  total-multiplication and Xtab/LEM generated targets.
- Add a reduced reflected-beta witness for the Tab-2-or-stronger variant.
