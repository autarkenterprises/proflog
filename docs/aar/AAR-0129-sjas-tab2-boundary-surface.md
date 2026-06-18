# AAR-0129: SJAS Tab-2 Boundary Surface

- Date: 2026-06-18
- ADR: [ADR-0129](../adr/ADR-0129-sjas-tab2-boundary-surface.md)
- Branch: `adr-0129-sjas-tab2-boundary-surface`

## Outcome

ADR-0129 is complete as the Tab-2-or-stronger Workstream B boundary-surface
slice.

The implementation adds:

- `tab2-or-stronger-boundary-surface`, an executable audit record identifying
  the negative proof-list variant;
- `audit-tab2-or-stronger-boundary-surface`, a public audit helper;
- Workstream B roadmap updates marking `:tab-2-or-stronger` as
  `:surface-implemented`;
- `:variant-surfaces` metadata that records total multiplication and Tab-2 as
  implemented variant surfaces.

Only the `:variant-surface` obligation is closed for
`:tab-2-or-stronger`. The reduced witness, generated SelfCons contradiction
target, constructed certificate, and proof-search synthesis evidence remain
open.

## Evidence

Initial red selectors failed as intended because no public Tab-2 boundary audit
helper existed:

```text
No such var: correspondence/audit-tab2-or-stronger-boundary-surface
```

Focused green selectors:

```text
boundary-failure-roadmap-keeps-witness-contract-open
Ran 1 tests containing 12 assertions.
0 failures, 0 errors.

tab2-boundary-surface-distinguishes-negative-proof-list-variant
Ran 1 tests containing 9 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 224 tests containing 1405 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1173 fail=0 error=0
```

## Follow-up

- Add a reduced reflected-beta witness for the Tab-2-or-stronger variant.
- Add the generated SelfCons contradiction target for that variant.
- Build constructed certificates and proof-search synthesis evidence for
  Workstream B variants.
- Add the Xtab/LEM-as-axiom boundary surface.
