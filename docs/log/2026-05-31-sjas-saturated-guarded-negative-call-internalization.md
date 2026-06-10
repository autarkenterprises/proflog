# SJAS Saturated Guarded Negative Call Internalization

Date: 2026-05-31
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This slice extends the guarded negative-call internalization to the saturated
guarded proof shape:

```clojure
(guarded-neg-alt-saturated
  (guarded-scope-done)
  (guard-saturation-done)
  (guarded-call-seq-done)
  residual-proof)
```

The implemented case is the no-scope/no-guard/no-recursive-call macro path.
For that case, the SJAS checker reconstructs the reflected body from
`system-code`, derives the negated guarded fallback sequence from the decoded
body, requires empty guard saturation and empty guarded call sequence, and
checks the remaining formulas through `guarded-residual-seq-*` constructors.

This does not complete all guarded machinery. Recursive
`guarded-call-seq-step`, non-empty `guard-eq`, existential guarded scope, and
answer-overlay query/residual variants remain separate Track 1 gaps.

## Red Evidence

Before implementation, the new saturated checker tests failed:

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-saturated-guarded-reflected-negative-call-from-system-code
  FAIL: decoded tableau proof checking must recover saturated guarded reflected negative calls from system-code

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-saturated-guarded-reflected-negative-call-certificates
  FAIL: tableau-proof must validate encoded saturated guarded negative-call certificates from system-code
```

## Verification

```text
timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-saturated-guarded-negative-call-evidence
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-saturated-guarded-reflected-negative-call-from-system-code
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-saturated-guarded-reflected-negative-call-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 300s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-guarded-reflected-negative-call-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 32 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-exposes-relevant-and-unresolved-constructors
  Ran 1 tests containing 22 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.
  elapsed 1:57.27 maxrss 644380KB

timeout -k 10s 900s lein test-proflog-fast
  Ran 159 tests containing 598 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
