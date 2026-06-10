# SJAS Minimal Arithmetic Close Certificate

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

The previous SJAS proof-certificate grammar reserved and accepted arithmetic
closure through Proflog-style evidence:

```clojure
(profiled willard-sjas-arithmetic ...)
```

That evidence is useful for debugging and for a future Track 2 correspondence
argument about Proflog proof traces, but it is stronger than the minimal
semantic-tableau proof object required by the direct SJAS proof predicate. A
closed tableau leaf only needs to state that the selected branch formula closes
by an admissible arithmetic/profile relation; the arithmeticized predicate
itself should evaluate the relation over the branch state.

This slice adds a minimal proof-tree constructor:

```clojure
(arith-close)
```

`arith-close` is a tableau leaf certificate. It is not a host-side shortcut and
does not assert that a decoded Proflog arithmetic trace is valid. During proof
checking, the SJAS predicate selects the branch formula and runs the same
object-level arithmetic closure cores used by the traced arithmetic evidence
path. The proof code therefore remains a tableau tree, while the arithmetic
work remains inside the proof predicate.

## Result

- `proof-symbols` now includes `arith-close`.
- The correspondence classifier marks `arith-close` as a relevant tableau
  symbol rather than leaving it as an unclassified proof alphabet extension.
- The branch-close checker accepts `(arith-close)` by internally checking
  arithmetic disequality/refuted-relation closure.
- The older traced `(profiled willard-sjas-arithmetic ...)` path remains
  available for existing generated certificates, but it is no longer the only
  way to express arithmetic branch closure in an SJAS proof code.

## Verification

Focused red/green selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-minimal-arithmetic-close-certificates
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-minimal-arithmetic-close-certificates
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
git diff --check
```

All passed after implementation.

## Remaining Boundary

This does not remove all Proflog-kernel proof-trace tags from the SJAS proof
alphabet. It establishes the first explicit split: minimal tableau evidence is
the preferred Track 1 grammar, while proof-trace evidence should be either
macro-expanded, classified as debugging/Track 2-only evidence, or excluded from
the final direct SJAS proof predicate.
