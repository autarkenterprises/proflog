# SJAS Recursive Guarded Call Sequence Internalization

Date: 2026-06-01
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This slice internalizes recursive guarded-call sequence checking for reflected
negative-call proofs. Reflected guarded alternatives reconstructed from
`system-code` now preserve the same guard/call/residual partition used by the
ordinary guarded-call kernel path:

```clojure
(guarded-alternative
  scope
  guards
  negated-calls
  negated-residuals
  fallback-negated-conjuncts)
```

The call partition is not recovered from compiled guarded-clause tables. It is
recognized by reading reflected relation indexes from the encoded finite system
bytes and requiring a well-formed reflected clause record for the relation. The
saturated guarded alternative checker now consumes `guarded-call-seq-step`
evidence by recursively resolving the negated call through
`sjas-system-reflected-guarded-call-alternativeso`, checking the selected nested
guarded alternative, and then continuing with the remaining guarded-call
sequence before residuals are checked.

This covers recursive guarded calls whose selected recursive relation is present
in the reflected Group-2b system-code records. `guarded-call-seq-defer`,
answer-overlay query constructors, non-equality guard saturation, and generic
optimized layer/profile wrappers remain Track 1 gaps.

## Red Evidence

Before implementation, the focused checker and public proof-predicate tests
failed:

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-recursive-guarded-call-sequence-from-system-code
  FAIL: decoded tableau proof checking must recover recursive guarded call sequences from system-code

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-recursive-guarded-call-sequence-certificates
  FAIL: tableau-proof must validate encoded guarded-call sequence certificates from system-code
```

The proof-code grammar regression already passed because the proof-code alphabet
contained `guarded-call-seq-step`, but the checker did not yet consume that
constructor on the saturated guarded path.

## Verification

```text
timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-recursive-guarded-call-sequence-evidence
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-recursive-guarded-call-sequence-from-system-code
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

timeout -k 5s 300s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-recursive-guarded-call-sequence-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-existential-guarded-scope-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-guard-equality-saturation-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-saturated-guarded-reflected-negative-call-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-guarded-reflected-negative-call-certificates
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
  elapsed 1:57.17 maxrss 531008KB

timeout -k 10s 900s lein test-proflog-fast
  Ran 159 tests containing 598 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
