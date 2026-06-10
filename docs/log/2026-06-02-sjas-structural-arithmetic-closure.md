# SJAS Structural Arithmetic Closure

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing tableau leaves now support arithmetic/profile closure without
requiring either:

- the minimal `(arith-close)` proof-rule tag; or
- a full `(profiled willard-sjas-arithmetic ...)` proof trace.

The structural proof object supplies only the formula node. The checker decodes
that formula and runs the existing SJAS arithmeticized closure cores over the
branch state:

- arithmetic disequality closure through `sjas-neq-close-coreo`;
- negated arithmetic/profile relation closure through
  `sjas-neg-relation-close-coreo`.

The arithmetic evidence remains inside the predicate evaluation; it is not
adjoined to the tableau certificate as an extra Godel-encoded proof trace.

## Verification

Focused red/green selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-arithmetic-closures
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-reflexive-disequality-closures
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-minimal-arithmetic-close-certificates
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
git diff --check
```
