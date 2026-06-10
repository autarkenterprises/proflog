# SJAS Stored Disequality Proof Check

Date: 2026-05-28

## Context

The next Track 1 equality gap after rigid disequality progress was delayed
disequality. Ordinary Proflog proves the SJAS-definable theorem
`forall x. x = 0 or x != 0` by closing the negated branch
`exists x. x != 0 and x = 0` with this proof shape:

```clojure
(conj
  (witness
    (conj
      (neq-store
        (eq-step
          (par-bind)
          (neq-close))))))
```

Before this slice, the SJAS-local proof checker had no rule for storing a
symbolic disequality and no `eq-step` branch for closing against saved
disequalities after equality progress.

## Change

`sjas-proof-check-close-agendao` now accepts `(neq-store subproof)` by storing
the selected negative equality in the branch disequality list and continuing on
the next pending formula. It also accepts the saved-disequality closure form of
`(eq-step step-proof branch-proof)`: after relational unification with
`equality/unify-termo`, `equality/neq-violatedo` must show that a saved
disequality has collapsed, and the branch closes with that evidence.

`neq-store` is now classified as relevant branch-state/progress evidence. The
bare `neq-close` symbol remains unresolved in the audit because the same symbol
also appears in the still-unimplemented negative-equality proof-variable
closure form `(neq-close step-proof)`.

## Verification

- Red: `timeout 55s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-stored-disequality-closures-without-kernel-validator`
- Red: `timeout 120s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-stored-disequality-closure-certificates`
- Green: `timeout 60s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-stored-disequality-closures-without-kernel-validator`
- Green: `timeout 130s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-stored-disequality-closure-certificates`
- Green: `timeout 45s lein test proflog.sjas-correspondence-test`
- Green: `git diff --check`
- Green: `lein test-proflog-fast`
- Green: `lein test-proflog-extended`
