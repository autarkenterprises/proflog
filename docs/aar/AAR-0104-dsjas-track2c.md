# AAR-0104: D_SJAS Track 2c Correspondence Program

- Date: 2026-06-13
- ADR: [ADR-0104](../adr/ADR-0104-dsjas-track2c.md)
- Branch: `adr-0104-dsjas-track2c`

## Outcome

ADR-0104 is complete.

The branch defines `D_SJAS` as an explicit selected apparatus rather than as
"whatever the implementation accepts." The selected rule families are base
tableau rules, branch bookkeeping, truth normalization, quantifiers,
equality/disequality theory, arithmetic/profile closure, decoded axiom
membership, reflected calls, recursive `tableau-proof/3`, and recursive
`subst-prf/4`.

The ADR-0102 `sjas-axiom` citation counterexample is repaired by measuring
citation leaves as the combined inspectable object `(S,F,P)` instead of proof
code `P` alone. Formula-bearing structural proof trees keep the ordinary
proof-code measure. The combined lower-bound audit covers both proof-object
kinds under the recorded code-injectivity and byte-inspectability assumptions.

Recursive proof and substitution leaves are justified by the selected
least-fixed-point semantics over finite acyclic proof-call graphs. Runtime fuel
is not used as the proof measure. Same-proof-code self-calls and mutual
proof-code cycles have no finite derivation in the selected relation.

The literature-admissibility audit is complete for the selected variant
`IS#_{D_SJAS}(beta)`. This is not a claim that `D_SJAS` is literal Willard `D`.
It is the Track 2c result: Willard's `D`-parameterized proof-predicate framework
is instantiated with a named, bounded, inspectable, tableau-shaped extended
apparatus whose non-literal-Willard rules are classified as selected primitives
or bounded macros.

## Evidence

Red tests captured the missing APIs and later the insufficient proof statuses:

```text
lein test proflog.sjas-correspondence-test
No such var: correspondence/audit-dsjas-track2c-specification
Tests failed.

lein test proflog.sjas-correspondence-test
No such var: correspondence/audit-dsjas-combined-size-lower-bound
Tests failed.

lein test proflog.sjas-correspondence-test
expected: (= :proved-for-finite-acyclic-proof-call-graphs (:status audit))
  actual: (not (= :proved-for-finite-acyclic-proof-call-graphs :measure-specified))
Tests failed.

lein test proflog.sjas-correspondence-test
expected: (= :proved-for-selected-dsjas-variant (:status audit))
  actual: (not (= :proved-for-selected-dsjas-variant :in-progress))
Tests failed.
```

Focused green after the final literature-admissibility audit:

```text
lein test proflog.sjas-correspondence-test
Ran 35 tests containing 436 assertions.
0 failures, 0 errors.
```

Final broad gates:

```text
lein test-proflog-fast
Ran 206 tests containing 1085 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 73 tests containing 219 assertions.
0 failures, 0 errors.
```

## Follow-up

- Keep claims labeled `IS#_{D_SJAS}(beta)` unless a later ADR proves a literal
  `IS#_D(beta)` correspondence.
- Any future proof-predicate optimization must preserve the selected apparatus,
  the combined citation accounting, and the finite acyclic proof-call semantics.
