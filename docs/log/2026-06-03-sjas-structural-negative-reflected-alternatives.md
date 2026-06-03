# SJAS Structural Negative Reflected Alternatives

Date: 2026-06-03

## Context

The legacy SJAS proof-code path has a `neg-call-alt` constructor whose child
`alt` proof selects one negated body from the reflected alternatives for a
multi-clause call. ADR-0073 Track 1 aims to avoid proof-trace selectors when a
formula-bearing tableau tree can expose the same structure directly.

## Finding

The structural negative reflected-call branch added for single-clause calls
already ranges over matching reflected clauses in encoded `system-code`. For a
multi-clause reflected relation, the formula-bearing child node identifies the
selected negated body by its formula code. No additional `neg-call-alt` or
`alt` proof constructor is needed for this structural fragment.

This is not a host-side shortcut: the matcher still decodes reflected clause
records from `system-code`, matches the encoded relation index and arity, builds
the call environment, negates the decoded body, and checks the child subtree
with the same structural proof predicate.

## Verification

Focused selectors:

- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-negative-reflected-alternatives`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-negative-reflected-calls`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-reflected-negative-call-alternatives-from-system-code`
- `lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol`
- `git diff --check`

All passed.

## Remaining Work

Guarded negative alternatives still have their own proof-directed constructors.
Equality-triggered reflected calls also remain tagged and need a structural
formula-bearing path.
