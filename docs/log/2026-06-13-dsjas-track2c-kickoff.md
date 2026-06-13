# D_SJAS Track 2c Kickoff

Date: 2026-06-13

ADR: [ADR-0104](../adr/ADR-0104-dsjas-track2c.md)

## Result

Track 2c has started under its own ADR branch.

This slice does not complete Track 2c. It establishes the governing ADR and the
first executable audit artifacts for the final proof:

- selected `D_SJAS` rule-family specification;
- combined proof-object accounting for `sjas-axiom` citations;
- recursive proof/substitution descent-measure audit;
- literature-admissibility status with explicit open criterion.

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

## Recursive Measure

The first recursive well-foundedness audit names the primary descent measure as
decoded proof-code payload. Both structural recursive branches read the object
proof-code argument and invoke the structural checker on the decoded finite
payload:

- `tableau-proof/3` structural branch;
- `subst-prf/4` structural branch.

This is not yet the final well-foundedness proof. It is the required finite
measure statement that the final proof must discharge.

The executable API is:

```text
correspondence/audit-dsjas-recursive-well-foundedness
```

## Literature Admissibility

The audit currently records partial support:

- natural tree coding;
- bounded object relations;
- semantic-tableau shape;
- selected-apparatus labeling.

The hard open criterion remains:

```text
:willard-style-self-verification-transfer
```

That criterion must be proved before Track 2c can be complete.

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

lein test-proflog-fast
Ran 205 tests containing 1071 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 73 tests containing 219 assertions.
0 failures, 0 errors.
```

## Remaining Work

- Strengthen the combined size measure into a concrete lower-bound theorem.
- Prove recursive `tableau-proof/3` and `subst-prf/4` well-foundedness, not
  merely the measure shape.
- Prove or refute Willard-style self-verification transfer for `D_SJAS`.
- Add final Track 2c correspondence tests and AAR only after those obligations
  are discharged.
