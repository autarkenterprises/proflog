# SJAS Large Proof Report Shortcut Removal

Date: 2026-06-01

## Context

ADR-0073 Track 1 requires `tableau-proof/3` to expose the object-level proof
evidence it checks. A prior performance-motivated path handled large direct
`tableau-proof/3` queries by first checking acceptance through
`direct-negated-profile-closeo` in truth mode, then returning a small synthetic
public report for the proof-code and theorem-code reads.

That was operationally useful, but it weakened the Track 1 invariant. The
public proof result was no longer necessarily the reified evidence produced by
the same object-level relation that accepted the certificate.

## Change

Removed the large-tableau report path and its truth-mode acceptance guard.
Direct SJAS profile predicates now use the same proof-reifying path for large
`tableau-proof/3` queries as for other direct profile relations:

```clojure
(run proof-limit [proof]
  (direct-negated-profile-closeo formula program fuel proof))
```

This preserves the correctness-first Track 1 direction: performance may be
worse for large public numerals, but accepted proof-predicate queries return
the proof evidence produced by the internalized checker itself.

## Test Evidence

Added source-audit assertions rejecting:

- `large-tableau-proof-report`
- `direct-profile-accepted?`

The focused audit was red before the implementation and green afterward.
Focused behavioral checks confirmed that the zero-limit path does not
materialize report evidence, raw large direct evidence still materializes, and
the public self-consistency demonstration still obtains a checked proof.

## Verification

- `lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route`
- `lein test :only proflog.willard-sjas-test/large-tableau-proof-zero-limit-does-not-materialize-report`
- `lein test :only proflog.willard-sjas-test/large-tableau-proof-raw-direct-evidence-materializes`
- `lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-simple-arithmetic-certificates-without-kernel-validator`
- `lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets`
- `git diff --check`

- `lein test-proflog-fast`: 164 tests, 653 assertions
- `lein test-proflog-extended`: 68 tests, 203 assertions
