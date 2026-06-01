# SJAS Generic Sidecar Exclusion

Date: 2026-06-01
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This slice closes the generic optimized sidecar boundary for SJAS proof
predicates by exclusion rather than macro expansion. Proflog can emit proof
constructors such as:

```clojure
(profiled propositional ...)
(profiled first-order ...)
lem-close
skolemized
```

for ordinary optimized proof layers. Those layers are not part of the current
SJAS proof-predicate apparatus. The SJAS entrypoint already hides compiled
clauses from generic sidecar eligibility during proof-predicate validation, but
`sjas/proof-certificate` still stripped any three-field `(profiled kind proof)`
wrapper. That meant a generic sidecar certificate such as
`(profiled propositional valid-subproof)` could be encoded as if it were just
`valid-subproof`.

The wrapper erasure is now restricted to outer SJAS profile annotations:

```clojure
(profiled willard-sjas-tableau0 proof)
(profiled willard-sjas-level1 proof)
```

Generic sidecar wrappers remain in the proof-code tree. Since the SJAS proof
checker has no rule for those constructors, `tableau-proof/3` rejects them
rather than silently erasing the wrapper or using the optimized host sidecar.
The correspondence audit now classifies `lem-close`, `skolemized`,
`propositional`, and `first-order` as `:excluded`, and path-sensitive
`(profiled propositional ...)` / `(profiled first-order ...)` forms as
`:excluded`.

## Red Evidence

Before implementation, the proof-certificate regression failed because the
generic sidecar wrapper was stripped:

```text
timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-proof-certificates-preserve-generic-profiled-sidecar-evidence
  FAIL: expected encoded bytes for `(profiled propositional (conj (false-close)))`
```

The public proof-predicate regression also failed because the stripped
certificate was accepted as a valid ordinary proof:

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-rejects-generic-profiled-sidecar-certificates
  FAIL: SJAS proof predicates must reject generic optimized sidecar certificates rather than erasing the wrapper
```

The audit regression initially failed because generic sidecars were still
classified as unresolved/probably-excluded rather than explicitly excluded.

## Verification

```text
timeout -k 5s 120s lein test :only proflog.sjas-correspondence-test/generic-sidecar-proof-forms-are-explicitly-excluded
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.

timeout -k 5s 120s lein test :only proflog.sjas-correspondence-test/profile-wrapper-audit-is-path-sensitive
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-exposes-relevant-and-unresolved-constructors
  Ran 1 tests containing 22 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 33 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-certificates-preserve-generic-profiled-sidecar-evidence
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-rejects-generic-profiled-sidecar-certificates
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-false-close-certificates
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.

git diff --check
  clean.

timeout -k 10s 1200s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.
  elapsed 1:58.77 maxrss 585260KB

timeout -k 10s 900s lein test-proflog-fast
  Ran 160 tests containing 604 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
