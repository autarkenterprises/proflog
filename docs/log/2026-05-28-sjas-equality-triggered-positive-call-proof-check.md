# SJAS Equality-Triggered Positive Call Proof Check

Date: 2026-05-28

## Context

The kernel can wake a saved positive procedure atom after a positive equality
step makes the atom object-language ground. The proof term records this as:

```clojure
(eq-step step-proof
  (eq-triggered-call subproof))
```

Before this slice, the SJAS-local proof checker had immediate `pos-call`
support over reflected system-code clauses, but no saved-call analogue.

The focused regression uses the reflected demo clause `demo(x) :- x = 1`.
The branch first saves `demo(p)` while `p` is a delta parameter, then expands
`p = 0`. After the equality step, the saved atom walks to `demo(0)`, the
reflected clause body becomes `0 = 1`, and the subproof closes by
`free-close`.

## Change

Added `sjas-saved-positive-call-closeso`, the SJAS proof-checker analogue of
the kernel's positive `saved-call-closeso` branch. The relation walks saved
positive atoms through the current equality substitution, checks object-language
groundness, resolves the call body from reflected clauses decoded out of
`system-code`, and continues proof checking on the reflected body with
`(eq-triggered-call subproof)`.

This does not consult the compiled runtime clause table and does not call
`kernel/prove-programo`.

## Verification

- Red: `timeout 80s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-equality-triggered-positive-calls-without-kernel-validator`
- Green: `timeout 80s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-equality-triggered-positive-calls-without-kernel-validator`
- Green: `timeout 50s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-equality-triggered-positive-call-evidence`
- Green: `timeout 45s lein test proflog.sjas-correspondence-test`
- Green: `git diff --check`
- Green: `lein test-proflog-fast`
- Green: `lein test-proflog-extended`
