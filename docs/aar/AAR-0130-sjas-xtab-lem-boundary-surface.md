# AAR-0130: SJAS Xtab / LEM Boundary Surface

- Date: 2026-06-18
- ADR: [ADR-0130](../adr/ADR-0130-sjas-xtab-lem-boundary-surface.md)
- Branch: `adr-0130-sjas-xtab-lem-boundary-surface`

## Outcome

ADR-0130 is complete as the Xtab/LEM-as-axiom Workstream B
boundary-surface slice.

The implementation adds:

- `xtab-or-lem-boundary-surface`, an executable audit record identifying the
  axiom-schema boundary variant;
- `audit-xtab-or-lem-boundary-surface`, a public audit helper;
- Workstream B roadmap updates marking `:xtab-or-lem-axiom` as
  `:surface-implemented`;
- `:variant-surfaces` metadata that records total multiplication,
  Tab-2-or-stronger, and Xtab/LEM-as-axiom surfaces.

Only the `:variant-surface` obligation is closed for
`:xtab-or-lem-axiom`. The reduced witness, generated SelfCons contradiction
target, constructed certificate, and proof-search synthesis evidence remain
open.

## Evidence

Initial red selectors failed as intended because no public Xtab/LEM boundary
audit helper existed:

```text
No such var: correspondence/audit-xtab-or-lem-boundary-surface
```

Focused green selectors:

```text
boundary-failure-roadmap-keeps-witness-contract-open
Ran 1 tests containing 14 assertions.
0 failures, 0 errors.

xtab-lem-boundary-surface-distinguishes-axiom-schema-variant
Ran 1 tests containing 9 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 225 tests containing 1416 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1173 fail=0 error=0
```

## Follow-up

- Add a reduced reflected-beta witness for the Xtab/LEM-as-axiom variant.
- Add the generated SelfCons contradiction target for that variant.
- Build constructed certificates and proof-search synthesis evidence for the
  Workstream B negative variants.
