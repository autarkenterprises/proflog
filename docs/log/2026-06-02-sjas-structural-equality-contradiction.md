# SJAS Structural Equality Contradiction Closure

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing tableau leaves now support equality contradiction closure
without Proflog kernel proof-trace tags.

The red test required a structural proof node for the formula `0 = 1`, encoded
only as formula bytes and child count. Before this slice, the formula-bearing
checker could progress through satisfiable equality nodes but could not close an
impossible equality leaf unless the proof term supplied the old kernel
contradiction trace, such as `free-close` or `decompose`.

This slice adds a proof-free equality core for the structural SJAS checker. The
new helper walks terms through the branch substitution, detects proof-variable
occurs contradictions, distinct constructor heads, argument-list arity
mismatch, and recursive same-head argument contradictions, and exposes only the
semantic branch-closure result. It does not ask the encoded proof object for a
contradiction constructor and does not return `free-close`, `occurs-close`, or
`decompose` evidence.

The change advances ADR-0073 Track 1 by moving another equality closure case
from proof-directed kernel-trace validation to direct structural tableau
checking over the decoded formula-bearing proof tree.

## Remaining Boundary

This does not complete proof-free equality machinery. Other formula-bearing
branches still rely on existing equality helpers for branch-state computation,
including complementary literal unification and equality progression. Those
helpers do not require proof-code tags from the SJAS proof object, but they
still produce local kernel proof terms internally. Later Track 1 slices should
either replace those internal proof-producing helpers with proof-free cores or
prove that their local proof outputs are irrelevant and unreachable from the
encoded proof predicate.

## Verification

Focused red selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-contradiction-closures

FAIL in (sjas-proof-check-accepts-formula-bearing-equality-contradiction-closures)
formula-bearing equality leaves should close when the equation is impossible in the term algebra
actual: (not (successful? ()))
```

Focused green selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-contradiction-closures

Ran 1 tests containing 4 assertions.
0 failures, 0 errors.
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-continuations
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-rigid-disequality-continuations
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-reflexive-disequality-closures
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-arithmetic-closures
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-complementary-literal-closures
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-quantifier-expansions
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
git diff --check
```
