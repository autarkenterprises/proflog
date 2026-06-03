# SJAS Structural Branch Binder Names

Date: 2026-06-03

## Context

The first formula-bearing quantifier fragment chose existential parameters from
the active proof-variable depth. That worked for a single existential at a
given depth, but it reused the same canonical payload for nested existential
parameters when no new proof variable had been introduced. This left the
structural proof format unable to distinguish two parameters such as `par v0`
and `par v1` in the same branch.

## Change

Replaced the proof-variable-depth selector with a branch-environment-depth
selector for structural quantifier payloads. `forall`, `once-forall`, and
`exists` now choose the next canonical code nom from the number of binders
already present in the structural branch environment.

The checker still maintains `proof-vars` for equality binding obligations, but
formula-bearing node payloads now follow the full branch binder depth. Nested
existentials therefore allocate distinct canonical parameters.

## Verification

Red selector before the change:

- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-distinct-nested-existential-parameters`

It failed because the second existential reused `par v0`.

Green selectors after the change:

- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-distinct-nested-existential-parameters`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-quantifier-variable-children`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-quantifier-expansions`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-disequality-storage`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-guarded-scope-reflected-bodies`
- `lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route`
- `lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol`
- `git diff --check`

All passed.

## Remaining Work

This fixes canonical branch binder allocation for the covered structural
quantifier fragment. The public proof-code boundary still accepts legacy
proof-rule tags, and broader gates remain to be run before Track 1 can be
considered complete.
