# Path B Proof Attempt: Extended `D_SJAS` Apparatus

Date: 2026-06-13

ADR: [ADR-0103](../adr/ADR-0103-sjas-proof-attempts-a-b.md)

Predecessor: [Path B target](2026-06-13-sjas-path-b-extended-dsjas.md)

## Result

Path B has been advanced to an executable candidate apparatus inventory, but the
proof is not complete.

The inventory records every structural checker branch under candidate
`D_SJAS` rule families. This is necessary but not sufficient: Path B still must
show that the selected apparatus is admissible for Willard-style
self-verification, and must repair the `sjas-axiom` proof-size issue exposed by
ADR-0102.

## Candidate Rule Families

The executable inventory currently covers these families:

- `:base-tableau`;
- `:branch-bookkeeping`;
- `:truth-normalization`;
- `:quantifier`;
- `:equality-theory`;
- `:arithmetic-profile`;
- `:axiom-membership`;
- `:reflected-call`;
- `:recursive-proof`;
- `:substitution-proof`.

This is the current best candidate for `D_SJAS`: literal tableau rules plus
explicit SJAS theory/profile proof steps.

## Open Obligations

The audit deliberately keeps the main blockers visible:

- `:sjas-axiom-size-accounting`;
- `:recursive-proof-well-foundedness`;
- `:literature-admissibility`;
- `:equality-theory-admissibility`;
- `:reflected-call-admissibility`.

The first three are global blockers. Equality and reflected calls are local
apparatus blockers: they may be admitted as selected primitives or bounded
macros, but that choice still needs a proof.

## Executable Evidence

The audit API is:

```text
correspondence/audit-dsjas-rule-inventory
```

The focused test is:

```text
path-b-dsjas-rule-inventory-covers-extended-apparatus
```

It passed as part of:

```text
lein test proflog.sjas-correspondence-test
Ran 28 tests containing 397 assertions.
0 failures, 0 errors.
```

## Next Proof Work

Path B now needs a mathematical definition of `D_SJAS` matching the inventory,
not merely an implementation table. The next non-negotiable choice is the
proof-object accounting for bare `sjas-axiom` citations:

1. exclude citations from the `5J` size theorem;
2. replace citations with formula-bearing axiom leaves;
3. count a combined proof object containing the necessary theorem/system
   payload.

Only after that choice can the recursive `tableau-proof/3` and `subst-prf/4`
well-foundedness proof be stated cleanly.
