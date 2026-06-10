# SJAS Structural-Only Proof Predicate

## Track 1 Slice

ADR-0073 Track 1, tableau proof checking and reflected procedure-call recovery.

## Problem

The formula-bearing structural checker no longer needed Proflog kernel proof
trace evidence, but the proof-predicate entry point still retained legacy
proof-trace machinery. In particular, the old agenda checker could accept
certificates shaped by kernel proof tags such as `conj`, `skip-true`, and
`false-close`, and a dead block of reflected-call helpers still encoded
sequence tags such as `eq-triggered-call`, `guarded-alt`, and guarded
sequence markers.

That was the wrong endpoint for Track 1. The SJAS-side predicate should accept
formula-bearing tableau trees and infer each rule from the decoded parent
formula, child formulas, and branch state.

## Change

- `sjas-proof-check-stateo` now selects the next formula from the branch agenda
  and delegates only to `sjas-structural-proof-check-stateo`.
- The legacy `sjas-proof-check-close-agendao` proof-trace checker was removed.
- Dead proof-trace reflected-call helper relations were removed.
- Source audits now reject the removed legacy checker/helper names.
- Tests that reintroduced generated kernel proof traces as SJAS certificates
  were removed or rewritten to use formula-bearing structural proof trees.
- Reflected-call tests now use encoded relation identities through
  `structural-neg-lit` and canonical formula-code proof nodes.

## Verification

Focused selectors passing after the change:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-right-first-conjunction-tableaux
lein test :only proflog.willard-sjas-test/sjas-proof-checker-rejects-legacy-proof-rule-tag-certificates
lein test :only proflog.willard-sjas-test/sjas-proof-predicates-ignore-external-runtime-clauses
lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-from-system-code
lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-without-symbol-registry
lein test :only proflog.willard-sjas-test/sjas-subst-prf-reconstructs-axiom-basis-without-system-registry
lein test :only proflog.willard-sjas-test/sjas-proof-code-decoder-checks-wide-formula-bearing-tableau-nodes
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-triggered-positive-calls
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-guarded-negative-reflected-bodies
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
git diff --check -- src/proflog/kernel/willard_sjas_profile.clj test/proflog/willard_sjas_test.clj LOG.md docs
```

One public end-to-end selector over the wide `tableau-proof/3` wrapper remains
a slow probe:

```text
lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-formula-bearing-true-theorem-certificates
```

It was relaunched as a durable background run:

```text
pid: test-runs/sjas-public-formula-bearing-true-theorem.pid
log: test-runs/sjas-public-formula-bearing-true-theorem-20260603T171016Z.log
```

Earlier foreground attempts at broader public proof-predicate selectors were
stopped after roughly 32 minutes because they were non-durable, consumed about
13 GB across three JVMs, and exceeded the focused-selector envelope. The
passing direct structural selectors now cover the intended reflected-call
invariant while the public wide-code route is treated as a separate durable
probe.

## Remaining Work

Track 1 is not complete. The next work is to continue replacing residual
proof-code grammar and self-consistency demonstration paths that still assume
generated kernel traces, then to make the public `tableau-proof/3` structural
certificate path tractable enough to verify end to end.
