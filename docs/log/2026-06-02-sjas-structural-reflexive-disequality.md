# SJAS Structural Reflexive Disequality Closure

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing tableau leaves now support reflexive disequality closure
without the Proflog `refl-close` proof-rule tag.

The proof code supplies a formula-bearing leaf. The checker decodes that
formula, applies the current branch environment, recognizes a disequality
literal, and closes the branch when both sides are equal under the current
branch substitution.

This is another small move from proof-trace evidence toward the direct
semantic-tableau proof object: the closure rule is inferred from the formula
and branch state rather than asserted as a separate proof constructor.

## Verification

Focused red/green selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-reflexive-disequality-closures
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-quantifier-expansions
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-complementary-literal-closures
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
git diff --check
```
