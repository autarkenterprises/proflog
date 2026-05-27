# SJAS Positive Equality-Step Proof Check

Date: 2026-05-27

## Context

After direct and nested free-constructor equality closure were internalized, the
next reachable certificate gap was positive equality progress. Ordinary Proflog
proves the SJAS-definable theorem `forall x. x != 0 or x != 1` by closing the
negated branch `exists x. x = 0 and x = 1`. The proof introduces a witness
parameter, binds that parameter to `0`, then closes the second equality against
`1`:

```clojure
(conj
  (witness
    (conj
      (eq-step
        (par-bind)
        (free-close)))))
```

Before this slice, `par-bind` was not part of the SJAS proof-code alphabet and
the local proof checker had no `eq-step` continuation rule.

## Change

`par-bind` was appended to the proof-code symbol alphabet so existing proof
indices remain stable. It remains classified as unresolved equality machinery
in the correspondence audit because only this positive equality-step path has
been internalized so far.

`sjas-proof-check-close-agendao` now accepts `(eq-step step-proof subproof)` for
positive equality formulas when `equality/unify-termo` relationally produces
`step-proof`, saved disequalities remain stable, and the checker can continue
with the next pending formula under the updated equality substitution. The rule
recurses into `sjas-proof-check-stateo`; it does not invoke
`kernel/prove-programo`.

## Verification

- Red: `timeout 45s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-positive-equality-step-evidence`
- Red: `timeout 50s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-positive-equality-steps-without-kernel-validator`
- Green: `timeout 45s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-positive-equality-step-evidence`
- Green: `timeout 55s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-positive-equality-steps-without-kernel-validator`
- Green: `timeout 120s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-positive-equality-step-certificates`
- Green: `timeout 45s lein test proflog.sjas-correspondence-test`
- Green: `git diff --check`
- Green: `lein test-proflog-fast`
- Green: `lein test-proflog-extended`
