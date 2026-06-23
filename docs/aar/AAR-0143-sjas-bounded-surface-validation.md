# AAR-0143: SJAS Bounded Surface Validation

- Date: 2026-06-22
- ADR: [ADR-0143](../adr/ADR-0143-sjas-bounded-surface-validation.md)
- Branch: `adr-0143-sjas-bounded-surface-validation`

## Outcome

Completed. Shared language validation now accepts `bounded-forall` and
`bounded-exists`, validates each bound term and body recursively, and preserves
the existing undeclared-symbol diagnostics. Supported bounded SJAS formulas can
therefore reach normalization and structural proof checking through public
query paths.

The change is intentionally confined to `proflog.language/validate-formula`.
The AST, normalizer, substitution layer, SJAS encoders/decoders, and structural
checker already implemented bounded quantifiers and required no semantic
changes.

## Impact

The original failure occurred in `sjas/query-succeeds` for
`total-multiplication-complete-system`. Its theorem wrapper includes the whole
generated axiom conjunction, including V4/V5 bounded existentials. Generic
surface validation rejected that conjunction before proof search.

The same omission affected every caller of `validate-query` and
`validate-clause` whose surface formula retained a bounded quantifier. The
repair is therefore shared rather than SJAS-specific. After the change, the
public multiplication-system query reaches the kernel and proves its reflected
Q6 axiom.

## Red-Green Evidence

Before implementation:

```text
lein test :only proflog.language-test/language-validates-bounded-quantifier-bounds-and-bodies
Ran 1 tests containing 4 assertions.
2 failures, 2 errors.

lein test :only proflog.willard-sjas-test/sjas-public-query-accepts-generated-bounded-axiom-basis
Ran 1 tests containing 1 assertions.
0 failures, 1 errors.
```

All failures were `ExceptionInfo: Malformed formula` at
`language.clj:validate-formula`, before bound/body validation or SJAS proof
search.

After implementation:

```text
language bounded validator: 1 test / 4 assertions, green
SJAS public bounded-basis query: 1 test / 1 assertion, green
language namespace: 6 tests / 21 assertions, green
bounded normalizer: 1 test / 8 assertions, green
bounded and negated-bounded structural checker: pass=36 fail=0 error=0
```

## Broad Gates

```text
lein test-proflog-fast
Ran 233 tests containing 1544 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.
```

`lein test-proflog-sjas-focused` progressed through the new public-query test
and both existing bounded structural-checker tests successfully. It was stopped
at the unrelated existing
`sjas-subst-prf-checks-selfcons-fixed-point-certificate` var after that var
exceeded its documented 45-minute runtime envelope while remaining CPU-active.
No complete focused-SJAS summary is claimed. The repository already records
that var as exceeding 25 and 45 minute envelopes; ADR-0143 did not change its
proof-checking path.

## Coverage

The new tests cover:

- successful `bounded-forall` validation;
- successful `bounded-exists` validation;
- rejection of an undeclared function inside a bound;
- rejection of an undeclared relation inside a bounded body;
- a generated SJAS axiom conjunction containing nested bounded existentials;
- public proof of a reflected multiplication axiom after validation.

These cases exercise every new validator branch and both recursive validation
edges. Existing normalization and structural-checker tests cover lowering and
proof semantics after validation.

## Follow-Up Boundary

ADR-0143 fixes syntax admission only. It does not establish the ADR-0142
Theorem 2.3 derivation, a Robinson Q interpretation, or the correctness of the
current `SemPrfK` bound. Those remain governed by the
[ADR-0142 interdeveloper review](../interdev/2026-06-22-adr-0142-review-and-corrections.md).
