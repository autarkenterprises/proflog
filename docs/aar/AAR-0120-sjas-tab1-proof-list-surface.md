# AAR-0120: SJAS Tab-1 Proof-List Surface

- Date: 2026-06-18
- ADR: [ADR-0120](../adr/ADR-0120-sjas-tab1-proof-list-surface.md)
- Branch: `adr-0120-sjas-tab1-proof-list`

## Outcome

ADR-0120 is complete.

The implementation adds the first Tab-1 surface without claiming full Tab-1
proof checking:

- `:willard-sjas-tab1` is a stable SJAS profile identity in source/system code.
- Public `tab1-proof/3` and measured `dsjas-tab1-proof/3` relation builders are
  declared.
- `tab1-proof-list-object` encodes `H = [(t1,p1), ..., (tn,pn)]` as theorem /
  proof-code byte pairs.
- `dsjas-tab1-proof-object` encodes the measured `(S,F,H)` object that generated
  Tab-1 SelfCons quantifies.
- Tab-1 Group-3 uses `dsjas-tab1-proof`, `pi-star-1-code`, and `neg-pair`, not
  the Level-1 `dsjas-subst-prf` relation.
- The correspondence audit records the Rank-1* / `U1*` terminology mapping to
  local `Pi*_1` / `Sigma*_1` classifiers, and explicitly marks arithmeticized
  entry validation and theorem-reuse proof search as deferred.

The new proof-code and formula-code symbols were appended to stable tables, so
existing encoded symbols keep their previous indexes.

## Evidence

Initial red selectors failed as intended:

```text
No such var: sjas/tab1-proof-list-object
No such var: correspondence/audit-tab1-proof-list-roadmap
```

Focused green selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-tab1-profile-group-three-uses-tab1-proof-list-vocabulary
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/tab1-proof-list-object-encodes-theorem-proof-pairs
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.

lein test :only proflog.sjas-correspondence-test/tab1-proof-list-accounting-records-measured-object
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.

lein test :only proflog.sjas-correspondence-test/tab1-roadmap-audit-reconciles-rank1-terminology
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
```

Nearby invariant selectors also passed:

```text
proof-symbol-audit-classifies-every-encoded-certificate-symbol
proof-symbol-audit-exposes-relevant-and-unresolved-constructors
dsjas-composite-tableau-proof-object-carries-measured-components
dsjas-composite-subst-prf-object-carries-measured-components
```

Final gates:

```text
lein test-proflog-fast
Ran 219 tests containing 1357 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1092 fail=0 error=0
```

## Follow-up

- ADR-0121 should implement arithmeticized Tab-1 proof-list entry validation:
  each `pi` must prove `ti` from beta plus earlier `tj`, and every intermediate
  `tj` must be admitted by the public Level-1 class restriction.
- Public `tab1-proof/3` / `dsjas-tab1-proof/3` proof-search semantics are still
  deferred. Querying the `:willard-sjas-tab1` proof profile should not be
  treated as a completed Tab-1 checker.
- Workstream B and Workstream C from ADR-0119 remain open.
