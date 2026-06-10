# SJAS Proof-Free Complementary Literal Closure

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing complementary literal closure now uses proof-free atom
unification.

The structural literal slice already removed `savefml` and `close` tags from
the encoded proof object, but the closure core still delegated to
`equality/atom-unifyo`, which computes a kernel proof trace payload while
unifying atom arguments. That payload was local, not encoded in the formula
bearing proof tree, but it was unnecessary for the SJAS structural tableau
predicate.

This slice adds `sjas-atom-unify-coreo`, a proof-free relation that checks
matching atom heads and unifies their argument lists using the SJAS profile's
proof-free term unifier. Complementary literal closure now exposes only the
branch-state effect needed to close the tableau branch.

## Verification

Focused red selector:

```text
lein test :only proflog.willard-sjas-test/sjas-complementary-literal-closure-uses-proof-free-atom-unifier

FAIL in (sjas-complementary-literal-closure-uses-proof-free-atom-unifier)
structural complementary literal closure should compute atom unification without kernel proof-trace output
actual: (not (str/includes? source "sjas-atom-unify-coreo atom opposite sigma sigma-out"))

FAIL in (sjas-complementary-literal-closure-uses-proof-free-atom-unifier)
formula-bearing complementary literal closure must not call the proof-producing kernel atom unifier
actual: (not (not true))
```

Focused green selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-complementary-literal-closure-uses-proof-free-atom-unifier
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-complementary-literal-closures
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-continuations
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-contradiction-closures
lein test :only proflog.willard-sjas-test/sjas-structural-proof-checker-uses-proof-free-equality-progression
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
```
