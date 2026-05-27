# SJAS Reflexive Disequality Proof Check

Date: 2026-05-27

## Context

The next direct equality certificate after positive equality progress was
negative equality closure. Ordinary Proflog proves the SJAS-definable theorem
`eq(code-1(0), code-1(0))` by closing the negated theorem
`neq(code-1(0), code-1(0))` with `(refl-close)`.

Before this slice, `refl-close` was encodable but remained only an unresolved
audit item; the SJAS-local proof checker did not consume it.

## Change

`sjas-proof-check-close-agendao` now accepts `(refl-close)` when the selected
formula is a negative equality and `equality/same-termo` confirms the two terms
are identical under the current equality substitution. The rule preserves the
current equality substitution and saved disequality list while closing the
branch. It does not call the host proof kernel.

The correspondence audit now classifies `refl-close` as relevant closure
evidence rather than unresolved equality machinery.

## Verification

- Red: `timeout 45s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-reflexive-disequality-closures-without-kernel-validator`
- Green: `timeout 45s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-reflexive-disequality-closures-without-kernel-validator`
- Green: `timeout 90s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-reflexive-disequality-closure-certificates`
- Green: `timeout 45s lein test proflog.sjas-correspondence-test`
- Green: `git diff --check`
- Green: `lein test-proflog-fast`
- Green: `lein test-proflog-extended`
