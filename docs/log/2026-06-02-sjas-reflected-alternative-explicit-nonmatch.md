# SJAS Reflected Alternative Explicit Nonmatch

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`
ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Context

The reflected negative-call proof paths reconstruct alternative lists from the
encoded Group-2b reflected clauses inside `system-code`. This is the right
Track 1 boundary: the proof checker must not consult compiled host clause
tables for `neg-call-alt` or guarded negative-call proof constructors.

The remaining issue was that the collectors used `conda` as an if-then-else:
if the current encoded reflected clause matched the focused call, include it in
the alternative list; otherwise skip it. Replacing this with plain `conde`
would be unsound because the skip branch could omit a matching alternative.
The in-principle arithmeticized relation needs an explicit proof that a skipped
encoded clause does not match the focused call.

## Change

Added finite structural relations over encoded reflected-clause headers:

- `reflected-atom-relation-indexo` recovers the relation index for a focused
  decoded call atom, either from reserved SJAS vocabulary or structural
  `(sym idx)` user symbols.
- `reflected-atom-arity-byteo` relates the focused call's argument list length
  to the reflected-clause arity byte.
- `reflected-call-header-matcho` proves that an encoded reflected-clause
  header matches the focused call by relation index and arity byte.
- `reflected-call-header-nonmatcho` proves a finite nonmatch: relation-index
  mismatch, or same relation-index with arity-byte mismatch.

`reflected-call-alternatives-in-clauseso` and
`reflected-call-guarded-alternatives-in-clauseso` now use ordinary `conde`
between the explicit match and explicit nonmatch branches. Matching clauses are
included; skipped clauses must carry an object-level nonmatch proof.

## Red Evidence

The source audit was extended first and failed on both committed-choice
collectors:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route

FAIL reflected negative-call alternative collection must use explicit encoded-clause match/nonmatch relations
FAIL guarded reflected alternative collection must use explicit encoded-clause match/nonmatch relations
```

## Verification

Completed current-source checks:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 72 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/reflected-call-header-match-and-nonmatch-are-explicit-relations
  Ran 1 tests containing 5 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-reflected-negative-call-alternatives-from-system-code
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-guarded-reflected-negative-call-from-system-code
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.
```

`git diff --check` was clean after the implementation.
