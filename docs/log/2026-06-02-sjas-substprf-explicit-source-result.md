# SJAS SubstPrf Explicit Source Result

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This slice removes the source-only `subst-prf/4` shortcut left by ADR-0069.
That helper only decoded the substitution source code and relied on the
metatheorem that diagonal substitution is total on decoded formula syntax.
That was not a host decoder, but it was still too weak for the current
in-principle arithmeticization target: Willard's `SubstPrf(g,t,p)` factors
through an existential sentence `h` such that `Subst(g,h)` and the proof checks
`t` from beta plus `h`.

The new relation, `sjas-subst-source-result-antecedento`, decodes `g`, builds the
quoted code term for `g`, computes the diagonal substituted formula `h`, and
converts `h` into the proof-antecedent AST form consumed by the local tableau
checker. It deliberately does not synthesize a public code term for `h`, but the
substituted source sentence is now computed as object-level formula structure
instead of being replaced by a well-formedness check.

`subst-prf/4` now uses this result in both relevant paths:

- `sjas-axiom` theorem citations still allow `t` to be a system axiom, but they
  carry explicit substituted-source evidence proving that the existential
  `Subst(g,h)` witness was computed.
- Non-`sjas-axiom` certificates now check the decoded proof against
  `(system axioms and h) and not(t)`, rather than against `system axioms and
  not(t)`.

Two proof-code symbols were added and classified as relevant SJAS profile
evidence:

- `willard-sjas-subst-source-result`
- `willard-sjas-subst-exprf`

## Red Evidence

The source audit was extended before implementation and failed on the old
source-only relation:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  FAIL subst-prf must compute the substituted source witness instead of using a source-only well-formedness shortcut
```

## Verification

Focused current-source checks:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 69 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-subst-source-result-computes-explicit-proof-antecedent
  Ran 1 tests containing 5 assertions.
  0 failures, 0 errors.

lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

lein test :only proflog.sjas-correspondence-test/implemented-sjas-profile-layer-markers-are-classified
  Ran 1 tests containing 14 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-proof-code-decoder-round-trips-byte-payload-evidence
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

lein test-proflog-fast
  Ran 165 tests containing 656 assertions.
  0 failures, 0 errors.
  elapsed 4:00.13 maxrss 413672KB

lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
  elapsed 9:21.33 maxrss 548692KB
```

Current-source long `subst-prf` selectors were relaunched as durable background
runs under `test-runs/` with timestamp `20260602T230615Z`:

- `sjas-subst-prf-uses-substitution-code-independently-of-theorem-code`
- `sjas-subst-prf-reconstructs-axiom-basis-without-system-registry`
- `sjas-subst-prf-checks-identity-substitution-certificates`

At the time of this log, they were still running and had not produced exit
files. Their outcomes should be checked from their `.log` and `.exit` files
before treating them as current-source SJAS soundness evidence.
