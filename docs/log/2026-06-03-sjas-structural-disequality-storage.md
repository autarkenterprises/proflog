# SJAS Structural Disequality Storage

Date: 2026-06-03
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing structural tableau nodes now support unresolved disequality
storage without a `neq-store` proof-rule tag.

The new test encodes an existential branch whose introduced parameter appears
in an unresolved disequality:

```text
exists v0. ((v0 != 0) and false)
```

After existential expansion the child proof node carries canonical formula-code
syntax for:

```text
(par v0) != 0
```

The structural checker now selects a deterministic canonical parameter nom for
the `exists` branch and stores unresolved disequality pairs in the branch
`neqs` state before continuing to the next agenda formula. The proof object
contains only formula-bearing nodes; it does not contain `witness` or
`neq-store`.

## Remaining Boundary

The current deterministic parameter selector is sufficient for the first
formula-bearing parameter fragment: the next parameter is keyed to the active
proof-variable depth. Branches that introduce multiple same-depth existential
parameters still need an explicit parameter counter or parameter-scope state so
their proof-code payloads can distinguish `par v0`, `par v1`, and later
parameters without host-side choice.

This slice covers storage and continuation. Closing a stored disequality after
later equality progression remains a separate structural rule to implement.

## Verification

Focused red selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-disequality-storage

FAIL in (sjas-proof-check-accepts-formula-bearing-disequality-storage)
formula-bearing unresolved disequalities should be stored structurally and the branch should continue
actual: (not (successful? ()))
```

Focused green selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-disequality-storage
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-quantifier-variable-children
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-quantifier-expansions
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-rigid-disequality-continuations
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-reflexive-disequality-closures
```
