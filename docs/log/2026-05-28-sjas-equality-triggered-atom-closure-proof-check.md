# SJAS Equality-Triggered Atom Closure Proof Check

Date: 2026-05-28

## Context

After disequality closure, the remaining positive-equality closure gap was the
kernel branch where a new equality step makes saved complementary atoms close.
The kernel represents this as:

```clojure
(eq-step step-proof
  (atom-close arg-proof))
```

The focused regression saves `color(x)` and `not color(0)` on the branch, then
uses `x = 0` to close the saved pair. The host kernel validator is disabled, so
success requires the SJAS-side checker to consume the decoded proof tree.

## Change

`atom-close` and `eq-refl` were appended to the proof-code symbol alphabet so
existing proof indices remain stable. `sjas-proof-check-close-agendao` now
accepts the saved-atom form of `(eq-step step-proof branch-proof)` by using the
same relational `equality/contradictory-atomso` predicate as the kernel.

`atom-close` is classified as relevant closure evidence. `eq-refl` remains in
the unresolved equality bucket because it is generic equality evidence that can
also appear outside this closure case.

## Verification

- Red: `timeout 45s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-equality-triggered-atom-closure-evidence`
- Red: `timeout 70s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-equality-triggered-atom-closures-without-kernel-validator`
- Green: `timeout 50s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-equality-triggered-atom-closure-evidence`
- Green: `timeout 60s lein test :only proflog.willard-sjas-test/sjas-proof-code-decoder-round-trips-equality-triggered-atom-closure-evidence`
- Green: `timeout 80s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-equality-triggered-atom-closures-without-kernel-validator`
- Green: `timeout 45s lein test proflog.sjas-correspondence-test`
- Green: `git diff --check`
- Green: `lein test-proflog-fast`
- Green: `lein test-proflog-extended`

## Follow-Up

A public `tableau-proof/3` regression over relation-heavy atom formulas timed
out while reifying the theorem-code read proof, even though proof-code decoding
and the local checker both succeeded. That is a separate focused performance
problem in theorem-code proof witness reification, not evidence that
`atom-close` checking still requires `kernel/prove-programo`.
