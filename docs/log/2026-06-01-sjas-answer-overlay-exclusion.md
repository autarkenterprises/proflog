# SJAS Answer Overlay Exclusion

Date: 2026-06-01
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This slice closes the answer-overlay proof-constructor boundary for SJAS theorem
proof predicates by exclusion. The answer overlay emits proof constructors such
as:

```clojure
query-pos-call
query-neg-call
query-neg-call-guarded-alt
guarded-call-seq-defer
```

These constructors describe query-entry and residual-deferral behavior for
answer export. They are not part of the semantic-tableau proof predicate over
encoded theorem/proof pairs. Track 1 therefore treats them as proof-code
constructors that may remain inspectable in public proof evidence but are
rejected by `tableau-proof/3` and `subst-prf/4` unless a future ADR deliberately
adds answer-overlay proof predicates.

The correspondence audit now classifies these symbols as `:excluded` with an
answer-overlay aspect. A focused SJAS regression confirms that a
`query-pos-call` certificate remains encoded as itself but is rejected by
`tableau-proof/3`.

## Red Evidence

Before implementation, the audit classified answer-overlay constructors as
unresolved:

```text
timeout -k 5s 120s lein test :only proflog.sjas-correspondence-test/answer-overlay-proof-forms-are-explicitly-excluded
  FAIL: query-pos-call, query-neg-call, query-neg-call-guarded-alt, and guarded-call-seq-defer were :unresolved
```

## Verification

```text
timeout -k 5s 120s lein test :only proflog.sjas-correspondence-test/answer-overlay-proof-forms-are-explicitly-excluded
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-exposes-relevant-and-unresolved-constructors
  Ran 1 tests containing 22 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 33 assertions.
  0 failures, 0 errors.

timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-answer-overlay-evidence
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-rejects-answer-overlay-query-certificates
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

git diff --check
  clean.

timeout -k 10s 1200s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.
  elapsed 2:05.43 maxrss 645000KB

timeout -k 10s 900s lein test-proflog-fast
  Ran 161 tests containing 610 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
