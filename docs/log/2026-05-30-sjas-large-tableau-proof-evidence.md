# SJAS Large Tableau Proof Evidence

Date: 2026-05-30
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This Track 1 slice removes the large non-axiom `tableau-proof/3` reporting
shortcut. The previous implementation checked a large Group-3 theorem-code
proof through the SJAS close relation in truth mode, but returned a synthetic
`(profiled willard-sjas-proof-check)` marker instead of the decoded proof tree
that was supplied by the proof code.

That was semantically better than the old `kernel/prove-programo` validator
shortcut because acceptance still came from the SJAS proof-check relation, but
it was not a concrete implementation of the proof predicate's proof-object
surface. The public proof result now reports the decoded non-axiom proof tree
for large theorem codes just as it does for small theorem codes.

## Red/Green Evidence

The source audit now rejects:

```text
large-non-axiom-tableau-proof-query
large-tableau-proof-summary
reported-decoded-proof-o
```

Before the implementation, the audit failed on all three names. After removing
the shortcut it passed 32 assertions.

## Runtime Boundary

The focused selector
`sjas-tableau-proof-checks-structural-non-generated-theorem-codes` passes with
full proof evidence.

The larger selector
`sjas-selfcons-demonstration-uses-substantive-proof-targets` exceeded a 900s
envelope after this change. The timeout is now a proof-evidence reification
boundary, not a semantic acceptance shortcut: the previous truth-mode check
demonstrated that the SJAS close relation can accept the certificate, but the
full public proof value for the Group-3 self-consistency theorem is currently
too expensive to materialize in the Proflog runtime.

This is aligned with the active goal's priority ordering: the concrete
arithmeticized predicate surface is preferred over a tractable summary marker.
The runtime boundary remains a performance problem for later work.

## Verification

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Red before implementation: 3 failures on the large proof-summary names.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 32 assertions.
  0 failures, 0 errors.

timeout -k 5s 360s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-checks-structural-non-generated-theorem-codes
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 900s lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  Timed out while materializing full proof evidence.

git diff --check
  clean.

timeout -k 5s 900s lein test-proflog-fast
  Ran 159 tests containing 594 assertions.
  0 failures, 0 errors.

timeout -k 5s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
