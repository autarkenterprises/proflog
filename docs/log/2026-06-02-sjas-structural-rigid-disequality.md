# SJAS Structural Rigid Disequality Progression

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing tableau nodes now support rigid disequality progression without
the Proflog `neq-rigid` proof-rule tag.

For a disequality formula whose two terms are already rigidly different under
the branch substitution, the structural checker continues to the child node and
remaining agenda without changing equality or disequality state. The proof
object carries only the formula node and child subtree; the rigid-difference
test is computed by the proof predicate.

## Verification

Focused red/green selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-rigid-disequality-continuations
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-continuations
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-reflexive-disequality-closures
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
git diff --check
```
