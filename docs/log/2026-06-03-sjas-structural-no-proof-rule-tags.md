# SJAS Structural Checker Proof-Rule Tag Audit

Date: 2026-06-03

## Context

ADR-0073 Track 1 now has a formula-bearing structural checker that validates
tableau nodes from decoded formula bytes and child subtrees. The broader
compatibility checker still contains legacy proof-trace branches, so a source
audit is needed to keep the structural path isolated from those tags.

## Change

Added a focused source audit over the body of
`sjas-structural-proof-check-stateo`. The audit rejects references to legacy
proof-rule tags such as `conj`, `witness`, `eq-step`, `savefml`, `pos-call`,
`neg-call`, `neg-call-alt`, guarded-call tags, and guarded sequence tags inside
the formula-bearing structural checker.

This does not remove the compatibility checker. It establishes that the
structural path infers local tableau rules from formula nodes and branch state
rather than matching symbolic Proflog proof-rule constructors.

## Verification

Focused selectors:

- `lein test :only proflog.willard-sjas-test/sjas-structural-proof-checker-has-no-proof-rule-tag-shortcuts`
- `lein test :only proflog.willard-sjas-test/sjas-structural-proof-checker-uses-proof-free-equality-progression`
- `lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route`
- `lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol`
- `git diff --check`

All passed.

## Running Gates

The foreground fast and extended suites passed before this slice:

- `lein test-proflog-fast`: 165 tests, 656 assertions, 0 failures/errors.
- `lein test-proflog-extended`: 68 tests, 203 assertions, 0 failures/errors.

`lein test-proflog-sjas-focused` is running productively on
`large-tableau-proof-full-evidence-materializes`; it has not produced a final
result yet.
