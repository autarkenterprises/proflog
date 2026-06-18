# AAR-0124: SJAS Boundary Variant Surface

- Date: 2026-06-18
- ADR: [ADR-0124](../adr/ADR-0124-sjas-boundary-variant-surface.md)
- Branch: `adr-0124-sjas-boundary-variant-surface`

## Outcome

ADR-0124 is complete as a bounded Workstream B surface slice.

The total-multiplication negative-variant surface now exists:

- baseline SJAS still excludes `mul/2`;
- `total-multiplication-functions` declares the opt-in `mul/2` function;
- `mul-term` constructs total multiplication terms;
- `total-multiplication-seed-axioms` returns two finite reflected beta laws
  involving `mul/2`;
- those seed laws are Pi*1-admissible and become Group-2 records;
- the seed records are visible through decoded `axiom-member/2` and citeable by
  `tableau-proof/3` with `sjas-axiom`;
- adding the seed beta fragment changes encoded system identity and regenerated
  Group-3/SelfCons code compared with the same function signature and no seed
  beta;
- the Workstream B audit records total multiplication as `:surface-implemented`
  while keeping reduced/full SelfCons contradiction witnesses, constructed
  certificate, and proof-search synthesis as open obligations.

This deliberately does not claim a completed Goedel-boundary failure. The
surface makes the first negative variant concrete without replacing the later
diagonal witness work with a trivial inconsistent-beta fixture.

## Evidence

Initial red selectors failed as intended:

```text
boundary-failure-roadmap-keeps-witness-contract-open
No such var: correspondence/audit-boundary-failure-roadmap

sjas-total-multiplication-boundary-surface-is-reflected
No such var: sjas/total-multiplication-functions
```

Focused green selectors:

```text
boundary-failure-roadmap-keeps-witness-contract-open
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.

sjas-total-multiplication-boundary-surface-is-reflected
Ran 1 tests containing 11 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 221 tests containing 1376 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1130 fail=0 error=0
```

## Follow-up

- A later Workstream B ADR must replace the seed fragment, or build on it, with
  a reduced reflected-beta contradiction witness.
- Completion still requires the full generated SelfCons contradiction target
  plus both an explicit constructed certificate and proof-search synthesis
  evidence.
- Tab-2-or-stronger and Xtab/LEM-as-axiom surfaces remain unstarted.
