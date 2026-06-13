# D_SJAS Track 2c Kickoff

Date: 2026-06-13

ADR: [ADR-0104](../adr/ADR-0104-dsjas-track2c.md)

## Result

Track 2c is complete under its own ADR branch.

The final ADR-0104 artifacts establish:

- selected `D_SJAS` rule-family specification;
- combined proof-object accounting for `sjas-axiom` citations;
- combined size lower-bound audit;
- recursive proof/substitution well-foundedness proof;
- literature-admissibility proof for the selected `IS#_{D_SJAS}(beta)` variant.

## D_SJAS Specification

The selected apparatus is not literal Willard `D`. It contains the ordinary
tableau core plus selected object-language extensions:

- base semantic-tableau rules;
- branch bookkeeping and truth normalization;
- quantifier and bounded-quantifier rules;
- equality/disequality theory;
- arithmetic/profile closure;
- decoded axiom membership;
- reflected-call expansion from encoded `system-code`;
- recursive `tableau-proof/3`;
- recursive `subst-prf/4`;
- structural substitution and fixed-point support.

The executable API is:

```text
correspondence/audit-dsjas-track2c-specification
```

## Citation Accounting Repair

ADR-0102 refuted measuring bare `sjas-axiom` citations by `P` alone. ADR-0104
selects combined proof-object accounting for citation leaves:

```text
size_D_SJAS(S,F,P) = size(S) + size(F) + size(P)
```

The exact final measure still needs a fully stated inequality, but the hidden
payload problem is repaired in kind: the cited formula and finite system basis
are now measured because they are part of the proof object.

The executable API is:

```text
correspondence/audit-dsjas-proof-object-accounting
```

## Combined Size Lower Bound

The first lower-bound audit is now explicit:

```text
correspondence/audit-dsjas-combined-size-lower-bound
```

It covers both currently selected proof-object kinds:

- `:sjas-axiom-citation`;
- `:formula-bearing-structural-tree`.

For citation objects, the measured object is the combined `(S,F,P)` tuple, so
the source of the `J` measure is the theorem-code payload rather than only the
fixed citation marker. For formula-bearing structural trees, the source remains
the proof-code formula-node payloads. The theorem is recorded as proved under
the existing code-injectivity and byte-inspectability assumptions from the
coding ADRs.

## Recursive Well-Foundedness

The recursive well-foundedness audit now uses the selected `D_SJAS` semantics:
recursive proof checks are interpreted as a least fixed point over finite,
acyclic proof-call graphs. Runtime fuel is explicitly not the proof measure; the
formula-bearing checker preserves fuel while validating fixed certificates.

Both structural recursive branches read the object proof-code argument and invoke
the structural checker on the decoded finite payload:

- `tableau-proof/3` structural branch;
- `subst-prf/4` structural branch.

The proof is by induction on proof-call graph height, with ordinary structural
induction on each decoded formula-bearing proof tree. Non-subtree proof-code
references are handled by graph height rather than child-node descent. Same-code
self-calls and mutual proof-code cycles have no finite least-fixed-point
derivation, so they are not accepted proof objects under the selected `D_SJAS`
relation.

The executable API is:

```text
correspondence/audit-dsjas-recursive-well-foundedness
```

## Literature Admissibility

The audit is now complete for the selected variant apparatus:

- natural tree coding;
- bounded object relations;
- semantic-tableau shape;
- selected-apparatus labeling;
- combined proof-size discipline;
- recursive proof well-foundedness;
- Willard `D`-parameterized proof-predicate instantiation;
- system-code reconstruction;
- primitive-or-bounded-macro classification for every non-literal-Willard rule
  family.

```text
:status :proved-for-selected-dsjas-variant
:apparatus-label :IS#_D_SJAS_beta
```

This is not a proof that `D_SJAS` is literal Willard `D`. It is a proof that the
extended apparatus is admissible as the selected proof predicate for the
explicitly labeled `IS#_{D_SJAS}(beta)` variant, under the recorded standing
assumptions for code injectivity, beta truth, and least-fixed-point recursive
semantics.

The executable API is:

```text
correspondence/audit-dsjas-literature-admissibility
```

## Evidence

Red:

```text
lein test proflog.sjas-correspondence-test
Syntax error compiling at (proflog/sjas_correspondence_test.clj:504:16).
No such var: correspondence/audit-dsjas-track2c-specification
Tests failed.
```

Green:

```text
lein test proflog.sjas-correspondence-test
Ran 34 tests containing 422 assertions.
0 failures, 0 errors.

lein test proflog.sjas-correspondence-test
Ran 35 tests containing 427 assertions.
0 failures, 0 errors.

lein test proflog.sjas-correspondence-test
Ran 35 tests containing 432 assertions.
0 failures, 0 errors.

lein test proflog.sjas-correspondence-test
Ran 35 tests containing 436 assertions.
0 failures, 0 errors.

lein test-proflog-fast
Ran 205 tests containing 1071 assertions.
0 failures, 0 errors.

lein test-proflog-fast
Ran 206 tests containing 1085 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 73 tests containing 219 assertions.
0 failures, 0 errors.
```

## Remaining Work

None for ADR-0104. Future work should keep the distinction between literal
`IS#_D(beta)` and the selected-variant `IS#_{D_SJAS}(beta)` explicit.
