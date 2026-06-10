# SJAS Canonical Quantifier Child Nodes

Date: 2026-06-03
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing structural quantifier nodes now support child nodes whose
formula payload mentions the introduced proof variable.

The previous quantifier fragment only covered closed bodies such as `false`.
That did not prove that an encoded child formula could refer to a variable
introduced by `forall` or `once-forall`. This matters for the remaining
equality and disequality work: non-rigid disequality storage and closure require
proof variables to appear in formula-bearing child nodes.

The new test encodes:

```text
forall v0. v0 != 0
```

with a child node encoded as canonical formula-code syntax:

```text
v0 != 0
```

The red failure showed that the child leaf could close directly, but the full
quantified proof did not. The structural checker was comparing each
formula-bearing node against the raw branch formula before applying the current
quantifier environment. It now compares the decoded node formula against the
branch-visible formula after applying `env`, while leaving the rule logic in
the ordinary raw-formula-plus-environment style.

The quantifier expansion also selects the next canonical code nom from
`code-nom-entries` according to the current proof-variable depth. This gives
formula-bearing child payloads a stable arithmetic encoding for introduced
variables: `v0`, `v1`, and so on.

## Verification

Focused red selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-quantifier-variable-children

FAIL in (sjas-proof-check-accepts-formula-bearing-quantifier-variable-children)
formula-bearing quantifier children should decode canonical v0 payloads into branch variables
actual: (not (successful? ()))
```

Focused green selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-quantifier-variable-children
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-quantifier-expansions
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-arithmetic-closures
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-complementary-literal-closures
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-continuations
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
```
