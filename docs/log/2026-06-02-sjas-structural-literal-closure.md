# SJAS Structural Literal Closure

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing tableau nodes now support ordinary complementary literal
closure without proof-trace tags. A literal node with one child saves the
current literal into the branch context and continues with the next agenda
formula. A literal leaf closes when it unifies with a saved complementary
literal on the same branch.

This removes the need for `savefml` and `close` proof-rule tags in this
fragment. The encoded proof object supplies formula bytes and child-tree
structure; the proof predicate computes the branch-context transition and
closure relation.

## Decoder Correction

This slice also corrected formula-bearing node decoding. Formula-code bytes
first decode into the SJAS internal syntax used by formula-code predicates,
where numerals appear as internal numeric syntax such as `num`. Tableau branch
checking, however, operates over AST terms such as `(app 0)`. Formula-bearing
nodes now pass decoded formulas through `sjas-internal-formula-asto`, matching
the theorem-code decoding path.

## Verification

Focused red/green selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-complementary-literal-closures
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-disjunction-tableaux
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-and-true-false-tableaux
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
git diff --check
```
