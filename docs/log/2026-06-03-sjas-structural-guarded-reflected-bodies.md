# SJAS Structural Guarded Reflected Bodies

Date: 2026-06-03

## Context

Legacy guarded negative-call certificates use proof constructors such as
`neg-call-guarded-alt`, `guarded-alt`, `guarded-neg-alt`,
`guarded-neg-alt-saturated`, `guard-eq`, guarded sequence tags, and
`guarded-scope-exists`. Those constructors describe a proof-search strategy for
closing the negation of reflected bodies with guards, recursive calls, and
residuals.

Formula-bearing structural tableau proofs do not need to encode that strategy
when the reflected body itself can be decoded from `system-code`. The structural
negative-call rule can decode a reflected body, form its NNF negation, and then
close that formula with ordinary tableau rules.

## Finding

Added focused coverage for two guarded-shaped reflected bodies:

- a conjunction containing an equality guard and a residual true formula,
  whose negation closes as an ordinary disjunction of `1 != 1` and `false`;
- an existential reflected body, whose negation closes by ordinary structural
  `once-forall` expansion rather than `guarded-scope-exists`.

Both proof trees are formula-bearing and contain zero symbolic proof-rule
constructors. This demonstrates that the formula-bearing structural path can
avoid guarded proof tags for these guarded-body fragments by checking the
decoded negated body directly.

## Verification

Focused selectors:

- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-guarded-negative-reflected-bodies`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-guarded-scope-reflected-bodies`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-guarded-reflected-negative-call-from-system-code`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-saturated-guarded-reflected-negative-call-from-system-code`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-existential-guarded-scope-from-system-code`
- `lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route`
- `lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol`
- `git diff --check`

All passed.

## Remaining Work

This is structural coverage, not full retirement of the legacy guarded
certificate grammar. The public `tableau-proof/3` path still accepts guarded
proof-code constructors for compatibility with generated Proflog proof traces.
The next Track 1 audit must narrow that public surface or justify each remaining
legacy constructor as outside the final formula-bearing SJAS proof predicate.
