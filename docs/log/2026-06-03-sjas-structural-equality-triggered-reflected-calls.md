# SJAS Structural Equality-Triggered Reflected Calls

Date: 2026-06-03

## Context

The legacy SJAS proof-code path used `eq-step` followed by
`eq-triggered-call` or `eq-triggered-neg-call` when an equality made a saved
reflected call ground. ADR-0073 Track 1 removes those proof-trace constructors
from formula-bearing structural tableau proofs.

## Change

Extended `sjas-structural-proof-check-stateo` so an equality node with one
child can:

- compute the branch substitution with `sjas-unify-termo-coreo`;
- walk saved positive or negative literals through that substitution;
- require the walked call arguments to be L-ground;
- decode the matching reflected clause from encoded `system-code`; and
- check the child subtree against the reflected body or its NNF negation.

The proof tree now carries the equality formula and the reflected body formula
as ordinary formula-bearing nodes. It no longer needs `savefml`, `eq-step`,
`eq-triggered-call`, or `eq-triggered-neg-call` tags for this fragment.

## Verification

Focused selectors:

- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-triggered-positive-calls`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-triggered-negative-calls`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-triggered-literal-closures`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-equality-triggered-positive-calls-without-kernel-validator`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-equality-triggered-negative-calls-without-kernel-validator`
- `lein test :only proflog.willard-sjas-test/sjas-structural-proof-checker-uses-proof-free-equality-progression`
- `lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route`
- `lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol`
- `git diff --check`

All passed.

## Remaining Work

Guarded negative-call alternatives still use proof-directed constructors. The
public `tableau-proof/3` and substitution-proof surfaces also still accept the
legacy proof-code grammar until the remaining structural fragments are covered
and the proof-code boundary is retired or narrowed.
