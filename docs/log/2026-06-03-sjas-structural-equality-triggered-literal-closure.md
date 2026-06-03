# SJAS Structural Equality-Triggered Literal Closure

Date: 2026-06-03
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing structural equality leaves now close saved complementary
literals after equality makes their atoms unifiable.

The red test encoded a branch equivalent to:

```text
forall v0. (wff(v0) and (not wff(0) and v0 = 0))
```

The literal nodes store `wff(v0)` and `not wff(0)` structurally. The equality
leaf then unifies `v0` with `0`; after that substitution, the saved literals
are complementary and the branch closes. The proof object carries only
formula-bearing nodes and contains no `savefml`, `eq-step`, or `close` tags.

The implementation adds a proof-free saved-literal contradiction relation that
uses the existing structural atom-unification core rather than the
proof-producing kernel atom unifier.

## Verification

Focused red selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-triggered-literal-closures

FAIL in (sjas-proof-check-accepts-formula-bearing-equality-triggered-literal-closures)
formula-bearing equality leaves should close saved complementary literals after unification
actual: (not (successful? ()))
```

Focused green selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-triggered-literal-closures
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-complementary-literal-closures
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-continuations
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-stored-disequality-closures
lein test :only proflog.willard-sjas-test/sjas-complementary-literal-closure-uses-proof-free-atom-unifier
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
git diff --check
```
