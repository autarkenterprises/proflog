# SJAS Structural Stored Disequality Closure

Date: 2026-06-03
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing structural equality leaves now close a branch when equality
progression falsifies a stored disequality.

The previous storage slice could retain unresolved parameter disequalities and
continue the branch. This slice adds the paired closure rule. The red test
encoded:

```text
exists v0. (((v0 != 0) and (v0 = 0)))
```

After structural `exists` expansion, the proof tree stores:

```text
(par v0) != 0
```

and then presents the formula-bearing equality leaf:

```text
(par v0) = 0
```

The checker unifies the equality terms with the proof-free unifier, detects
that one stored disequality is now reflexive under the updated branch
substitution, prunes contradictory disequalities, and closes the branch. The
proof object contains no `neq-store`, `eq-step`, or `neq-close` constructor.

## Verification

Focused red selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-stored-disequality-closures

FAIL in (sjas-proof-check-accepts-formula-bearing-stored-disequality-closures)
formula-bearing equality leaves should close when equality violates stored disequalities
actual: (not (successful? ()))
```

Focused green selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-stored-disequality-closures
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-disequality-storage
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-continuations
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-contradiction-closures
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-quantifier-variable-children
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
git diff --check
```
