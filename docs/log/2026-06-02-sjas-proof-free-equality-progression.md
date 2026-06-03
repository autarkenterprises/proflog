# SJAS Proof-Free Equality Progression

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing equality progression now uses the SJAS profile's proof-free
unification core instead of the kernel's proof-producing `equality/unify-termo`
helper.

The earlier structural equality slice removed the `eq-step` constructor from
the encoded proof object, but the structural checker still called a helper that
materialized local proof trace payloads such as `eq-refl`, `eq-bind`,
`par-bind`, and `decompose`. Those payloads were not visible in the proof code,
but they were still unnecessary for the structural tableau predicate.

This slice adds a guard around `sjas-structural-proof-check-stateo` requiring
that the structural fragment call `sjas-unify-termo-coreo` and not
`equality/unify-termo`. The equality progression branch now computes only the
branch substitution needed to continue the tableau.

## Verification

Focused red selector:

```text
lein test :only proflog.willard-sjas-test/sjas-structural-proof-checker-uses-proof-free-equality-progression

FAIL in (sjas-structural-proof-checker-uses-proof-free-equality-progression)
structural equality progression should compute branch substitutions without kernel proof-trace output
actual: (not (str/includes? structural-source "sjas-unify-termo-coreo"))

FAIL in (sjas-structural-proof-checker-uses-proof-free-equality-progression)
formula-bearing structural equality progression must not call the proof-producing kernel unifier
actual: (not (not true))
```

Focused green selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-structural-proof-checker-uses-proof-free-equality-progression
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-continuations
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-contradiction-closures
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-rigid-disequality-continuations
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
```
