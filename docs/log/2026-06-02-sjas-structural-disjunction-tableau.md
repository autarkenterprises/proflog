# SJAS Structural Disjunction Tableau Nodes

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

The formula-bearing tableau node checker now covers disjunction. A structural
node for:

```clojure
(or left right)
```

must have exactly two child proof nodes. The checker infers the beta/tableau
branching rule from the decoded parent formula and validates each child against
the corresponding disjunct.

This continues the move away from proof-trace tags. The certificate does not
need the Proflog `split` tag for this fragment; it carries the parent formula
bytes and the two child formula-bearing subtrees.

## Branch State

The structural disjunction rule preserves sibling independence. Both child
branches receive the same incoming literals, environment, equality
substitution, and disequality store. Equality/disequality changes produced
while closing one child remain local to that child and are not threaded into the
other child.

## Verification

Focused red/green selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-disjunction-tableaux
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-and-true-false-tableaux
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
git diff --check
```
