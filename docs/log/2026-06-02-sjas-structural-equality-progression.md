# SJAS Structural Equality Progression

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing tableau nodes now support equality progression without the
Proflog `eq-step` proof-rule tag.

The structural proof node carries the equality formula and one child node. The
checker:

1. decodes the formula-bearing node;
2. applies the current branch environment;
3. unifies the two equality terms using the existing relational equality
   engine;
4. checks that stored disequalities remain stable under the updated
   substitution; and
5. continues with the child node on the remaining agenda.

The equality proof trace returned by the equality engine is not adjoined to the
tableau certificate. It is local predicate work, not part of the encoded proof
object for this fragment.

## Verification

Focused red/green selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-continuations
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-arithmetic-closures
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-complementary-literal-closures
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
git diff --check
```
