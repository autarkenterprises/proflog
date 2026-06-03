# SJAS Proof-Free Structural Arithmetic Closure

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing structural arithmetic leaves now close through proof-free
arithmetic readers and relation checks.

The earlier structural arithmetic slice removed `arith-close` and profiled
arithmetic proof tags from the encoded proof object, but the structural checker
still delegated to arithmetic closure cores that returned local read/relation
proof payloads. This slice separates the semantic branch-closing relation from
that trace-producing evidence.

The new proof-free helpers read SJAS numeral terms to bit lists, bind delayed
object variables to canonical numerals, compare normalized numerals, and check
`mult`, `leq`, and `lt` relation leaves without constructing proof evidence.
The formula-bearing structural checker now calls:

```text
sjas-neq-close-structural-coreo
sjas-neg-relation-close-structural-coreo
```

instead of the proof-producing arithmetic cores.

## Remaining Boundary

This change affects structural formula-bearing leaves only. Legacy
proof-directed paths, including minimal `(arith-close)` and profiled arithmetic
certificates, still use the proof-producing arithmetic helpers because those
certificate fragments have not yet been fully retired.

## Verification

Focused red selector:

```text
lein test :only proflog.willard-sjas-test/sjas-structural-arithmetic-closure-uses-proof-free-arithmetic

FAIL in (sjas-structural-arithmetic-closure-uses-proof-free-arithmetic)
structural arithmetic disequality closure should not require arithmetic proof payloads

FAIL in (sjas-structural-arithmetic-closure-uses-proof-free-arithmetic)
structural arithmetic relation closure should not require arithmetic proof payloads

FAIL in (sjas-structural-arithmetic-closure-uses-proof-free-arithmetic)
formula-bearing structural arithmetic closure must not call the proof-producing disequality core

FAIL in (sjas-structural-arithmetic-closure-uses-proof-free-arithmetic)
formula-bearing structural arithmetic closure must not call the proof-producing relation core
```

Focused green selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-structural-arithmetic-closure-uses-proof-free-arithmetic
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-arithmetic-closures
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-minimal-arithmetic-close-certificates
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-complementary-literal-closures
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-continuations
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
```
