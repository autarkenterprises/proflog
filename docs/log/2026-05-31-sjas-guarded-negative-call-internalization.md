# SJAS Guarded Negative Call Internalization

Date: 2026-05-31
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This slice internalizes the fallback guarded negative-call proof constructors
used by the Proflog kernel for multi-clause procedure calls. Before this work,
`neg-call-guarded-alt` and its guarded subproofs could appear in ordinary
kernel evidence, but the SJAS proof predicate did not validate that proof shape
from encoded system data. The proof-code alphabet also omitted the terminal
guarded proof markers such as `guarded-scope-done`, so such evidence could not
be represented as an SJAS certificate.

The implementation now:

- extends the proof-code alphabet with the guarded terminal markers used by the
  kernel;
- reconstructs matching reflected guarded alternatives by scanning the
  reflected Group-2b records in `system-code`;
- derives the fallback guarded negative sequence from the decoded reflected
  clause body rather than from the compiled guarded-clause host table; and
- validates `(neg-call-guarded-alt (guarded-alt ...))` evidence in
  `sjas-proof-check-stateo`.

This is still a bounded Track 1 slice. It covers the fallback
`guarded-neg-alt` sequence path. The saturated guard-first path and answer
overlay query/residual variants remain separate proof-constructor families to
internalize or explicitly exclude before Track 1 can be considered complete.

## Red Evidence

The proof-code regression failed before the alphabet extension:

```text
timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-guarded-negative-call-evidence
  ERROR: Unsupported proof symbol in SJAS certificate {:symbol guarded-scope-done}
```

The proof-checker regression also failed before the guarded checker branch:

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-guarded-reflected-negative-call-from-system-code
  FAIL: decoded tableau proof checking must recover guarded reflected negative calls from system-code
```

## Verification

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-guarded-negative-call-evidence
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-guarded-reflected-negative-call-from-system-code
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

timeout -k 5s 300s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-guarded-reflected-negative-call-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-exposes-relevant-and-unresolved-constructors
  Ran 1 tests containing 22 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 32 assertions.
  0 failures, 0 errors.

timeout -k 5s 300s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-reflected-negative-call-alternative-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.
  elapsed 1:17.28 maxrss 663628KB

git diff --check
  clean.

timeout -k 10s 900s lein test-proflog-fast
  Ran 159 tests containing 598 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
