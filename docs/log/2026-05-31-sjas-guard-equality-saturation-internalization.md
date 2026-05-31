# SJAS Guard Equality Saturation Internalization

Date: 2026-05-31
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This slice extends saturated guarded negative-call checking from the empty
guard case to non-empty equality guard saturation. The SJAS proof checker now
reconstructs guarded alternatives from reflected Group-2b records as structured
data:

```clojure
(guarded-alternative guards negated-residuals fallback-negated-conjuncts)
```

That lets the fallback `guarded-neg-alt` path keep proving the negation of all
conjuncts in order, while the saturated `guarded-neg-alt-saturated` path
saturates equality guards first and then checks the residual sequence.

The implementation adds `sjas-saturate-eq-guardso`, an object-level analogue of
the kernel's equality guard saturation relation. It consumes explicit
`guard-eq` evidence, uses the existing relational equality machinery, and does
not consult compiled guarded-clause tables.

This still leaves non-empty guarded call sequences, existential guarded scope,
non-equality guard behavior, and answer-overlay guarded/query constructors as
future Track 1 slices.

## Red Evidence

Before implementation, the focused checker and public proof-predicate tests
failed:

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-guard-equality-saturation-from-system-code
  FAIL: decoded tableau proof checking must recover non-empty reflected equality guards from system-code

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-guard-equality-saturation-certificates
  FAIL: tableau-proof must validate encoded guard saturation certificates from system-code
```

The proof-code regression already passed because `guard-eq` and
`guard-saturation-done` had been added with the previous guarded slice.

## Verification

```text
timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-guard-equality-saturation-evidence
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-guard-equality-saturation-from-system-code
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-guard-equality-saturation-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-saturated-guarded-reflected-negative-call-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-guarded-reflected-negative-call-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 32 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-exposes-relevant-and-unresolved-constructors
  Ran 1 tests containing 22 assertions.
  0 failures, 0 errors.

git diff --check
  clean.

timeout -k 10s 1200s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.
  elapsed 2:00.85 maxrss 639268KB

timeout -k 10s 900s lein test-proflog-fast
  Ran 159 tests containing 598 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
