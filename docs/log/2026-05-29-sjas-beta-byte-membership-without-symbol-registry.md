# SJAS Beta Byte Membership Without Symbol Registry

Date: 2026-05-29
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This Track 1 system-code reconstruction slice removes the source symbol table
from Group-2 beta axiom membership.

The beta block in `system-code` is a sequence of encoded formula byte strings.
To decide `axiom-member(system, formula)` for beta axioms, the proof profile
does not need to recover host names for application symbols. It needs to:

- prove each beta entry is a formula-shaped byte string so the scanner can
  advance to the next entry;
- compare the queried formula-code bytes to the current beta entry bytes.

The scanner now uses the syntax-only decoder, which keeps application heads as
numeric `(sym n)` terms. This preserves byte-boundary checking while avoiding
`:sjas/symbol-index-entries` for beta membership.

## Red/Green Evidence

The focused regression
`sjas-beta-axiom-member-decodes-application-codes-without-symbol-registry`
builds a system whose beta axiom is the application-bearing formula
`lt(1, 2)`, removes `:sjas/registry`, and asks `axiom-member` to cite that
beta axiom.

Before the change, this failed because the beta scanner called the
proof-facing formula decoder, which tried to resolve the `lt` symbol index
through the missing source registry:

```text
FAIL in (sjas-beta-axiom-member-decodes-application-codes-without-symbol-registry)
expected: (successful? proofs)
  actual: (not (successful? ()))

Ran 1 tests containing 3 assertions.
1 failures, 0 errors.
```

After the change, the same selector passes.

## Remaining Boundary

This does not complete proof-facing symbol-table internalization. Reflected
Group-2b formulas and procedure-call clauses still require relation identity,
and the proof checker still converts decoded formulas to ordinary Proflog AST
terms in several paths. Those paths still use `sjas-symbol-indexo` and remain
the next symbol-code Track 1 target.

## Verification

```text
timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-beta-axiom-member-decodes-application-codes-without-symbol-registry
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 100s lein test :only proflog.willard-sjas-test/sjas-system-does-not-generate-axiom-member-fact-registry
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.

timeout -k 5s 100s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-do-not-require-source-preprocessing-registry
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 130s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-axiom-citation-certificates
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.
```
