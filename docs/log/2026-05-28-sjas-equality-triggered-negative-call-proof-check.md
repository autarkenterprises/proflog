# SJAS Equality-Triggered Negative Call Proof Check

Date: 2026-05-28

## Context

The positive saved-call slice still left the symmetric negative procedure-call
case unresolved. The kernel records this proof branch as:

```clojure
(eq-step step-proof
  (eq-triggered-neg-call subproof))
```

The focused regression uses the reflected demo clause `demo(x) :- x = 1`.
The branch saves `not demo(p)` while `p` is a delta parameter, then expands
`p = 1`. After equality, the saved atom walks to `not demo(1)`, the reflected
clause body negates to `1 != 1`, and the subproof closes by `refl-close`.

## Change

Added `sjas-saved-negative-call-closeso`, the SJAS proof-checker analogue of
the kernel's negative `saved-call-closeso` branch. It walks saved negative
atoms through the current equality substitution, verifies L-groundness, decodes
the matching reflected clause from `system-code`, and checks the NNF negation
of the reflected body under `(eq-triggered-neg-call subproof)`.

This keeps equality-triggered negative calls inside the proof checker relation
and avoids `kernel/prove-programo` and the compiled runtime clause table.

## Verification

- Red: `timeout 80s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-equality-triggered-negative-calls-without-kernel-validator`
- Green: `timeout 80s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-equality-triggered-negative-calls-without-kernel-validator`
- Green: `timeout 80s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-equality-triggered-positive-calls-without-kernel-validator`
- Green: `timeout 50s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-equality-triggered-negative-call-evidence`
- Green: `timeout 45s lein test proflog.sjas-correspondence-test`
- Green: `git diff --check`
- Green: `lein test-proflog-fast`
- Green: `lein test-proflog-extended`
