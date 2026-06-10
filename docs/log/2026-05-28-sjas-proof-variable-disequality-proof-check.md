# SJAS Proof-Variable Disequality Proof Check

Date: 2026-05-28

## Context

After stored disequality closure, the remaining negative-equality closure form
was `(neq-close step-proof)`: a branch containing a disequality over a
gamma-introduced proof variable can close by binding that proof variable so the
disequality becomes false.

The focused public regression uses an inert unary function `f/1` declared in
the demo SJAS system, so the theorem remains SJAS-definable without being
absorbed by the U-grounding arithmetic interpreter:

```clojure
exists x. f(x) = f(0)
```

The negated theorem is checked with this certificate:

```clojure
(conj
  (once-univ
    (neq-close
      (decompose
        (args
          (eq-bind)
          ())))))
```

## Change

`eq-bind` was appended to the proof-code symbol alphabet so existing proof
indices remain stable. `sjas-proof-check-close-agendao` now accepts
`(neq-close step-proof)` when relational unification can make the selected
negative equality false and the new equality bindings are all over
proof-introduced variables. This mirrors the kernel's `support/proof-bindingso`
guard and prevents user-level answer variables from being bound merely to close
a proof-predicate branch.

`neq-close` is now classified as relevant closure evidence. `eq-bind` remains
unresolved in the audit because it also appears as equality-progress evidence
outside this negative-equality closure rule.

## Verification

- Red: `timeout 45s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-proof-variable-disequality-closure-evidence`
- Red: `timeout 70s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-proof-variable-disequality-closures-without-kernel-validator`
- Green: `timeout 50s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-proof-variable-disequality-closure-evidence`
- Green: `timeout 80s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-proof-variable-disequality-closures-without-kernel-validator`
- Green: `timeout 140s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-proof-variable-disequality-closure-certificates`
- Green: `timeout 45s lein test proflog.sjas-correspondence-test`
- Green: `git diff --check`
- Green: `lein test-proflog-fast`
- Green: `lein test-proflog-extended`
