# SJAS Structural Negative Reflected Calls

Date: 2026-06-03

## Context

ADR-0073 Track 1 is removing proof-trace constructors and host-side proof
machinery from the SJAS proof predicate. Formula-bearing tableau nodes now
carry the object formula for the node, so a proof of closure can be checked by
structural tableau progression instead of by replaying a separate Proflog proof
trace.

Positive reflected calls already recovered a reflected clause from encoded
`system-code` and checked the child body subtree without a `pos-call` tag. The
corresponding single-clause negative reflected-call case still needed a
formula-bearing structural path.

## Change

Added a structural negative reflected-call branch to
`sjas-structural-proof-check-stateo`. When the current node formula is a
negated ground reflected application, the checker now:

- substitutes the branch environment into the visible formula;
- walks the atom through the branch substitution;
- requires ground arguments;
- recovers the matching reflected clause from encoded `system-code`; and
- checks the child subtree against the negated reflected body.

The encoded proof node therefore needs only the parent formula code and one
child subtree. It does not need a `neg-call` proof constructor for the
single-clause case.

## Verification

Focused selectors:

- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-negative-reflected-calls`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-positive-reflected-calls`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-guarded-reflected-negative-call-from-system-code`
- `lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route`
- `lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol`
- `git diff --check`

All passed.

## Remaining Work

This does not yet remove the structural gaps for multi-clause negative-call
alternatives, equality-triggered reflected calls, or the parameter-counter
limitations in formula-bearing quantifier expansion. Those remain separate
ADR-0073 Track 1 slices.
