# SJAS Existential Guarded Scope Internalization

Date: 2026-05-31
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This slice internalizes leading existential guarded scope for reflected guarded
negative-call proof checking. Reflected guarded alternatives reconstructed from
`system-code` now carry decoded scope:

```clojure
(guarded-alternative scope guards negated-residuals fallback-negated-conjuncts)
```

The SJAS checker uses an object-level guarded-scope opener that consumes
`guarded-scope-exists` evidence and extends the branch environment with
fresh proof variables before checking either the fallback guarded sequence or
the saturated guard/residual path.

This covers leading existential scope. Other leading quantifier forms,
recursive guarded call sequences, non-equality guard behavior, and
answer-overlay guarded/query constructors remain Track 1 gaps.

## Red Evidence

Before implementation, the focused checker and public proof-predicate tests
failed:

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-existential-guarded-scope-from-system-code
  FAIL: decoded tableau proof checking must recover existential guarded scope from system-code

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-existential-guarded-scope-certificates
  FAIL: tableau-proof must validate encoded guarded-scope certificates from system-code
```

The proof-code regression already passed because `guarded-scope-exists` and
`guarded-scope-done` were encoded in the earlier guarded slice.

## Verification

```text
timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-existential-guarded-scope-evidence
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-existential-guarded-scope-from-system-code
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-existential-guarded-scope-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-guard-equality-saturation-certificates
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
  elapsed 2:01.54 maxrss 661416KB

timeout -k 10s 900s lein test-proflog-fast
  Ran 159 tests containing 598 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
