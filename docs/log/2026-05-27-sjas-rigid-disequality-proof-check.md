# SJAS Rigid Disequality Proof Check

Date: 2026-05-27

## Context

After reflexive disequality closure, the next reachable negative-equality
certificate was rigid disequality progress. In Proflog's tableau proof terms,
`(neq-rigid subproof)` discharges a constructor disequality that is already true
in the free term algebra and continues with the remaining pending formula.

The proof-directed public regression uses a theorem whose negated theorem is

```clojure
(and
  (neq (code-1 0) (code-1 1))
  (eq 0 1))
```

and supplies the certificate:

```clojure
(conj
  (conj
    (neq-rigid
      (free-close))))
```

## Change

`sjas-proof-check-close-agendao` now accepts `(neq-rigid subproof)` when the
selected negative equality is rigidly different under the current equality
substitution. The rule leaves the equality substitution and saved disequalities
unchanged and recurses into `sjas-proof-check-stateo` on the next pending
formula.

The public `tableau-proof/3` regression also exposed a tractability issue: once
the proof predicate target expands `(and system-axioms negated-theorem)`, the
generic checker could spend time trying to match the supplied inner `conj`
proof against the reconstructed axiom basis before selecting the negated
theorem. `sjas-proof-check-programo` now has a proof-directed object-level fast
path for certificates that close the negated theorem directly after the top
conjunction. This is a tableau-valid branch closure and still recurses into the
SJAS-local checker rather than the host kernel.

The correspondence audit now classifies `neq-rigid` as relevant closure/progress
evidence rather than unresolved equality machinery.

## Verification

- Red: `timeout 45s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-rigid-disequality-progress-without-kernel-validator`
- Green: `timeout 55s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-rigid-disequality-progress-without-kernel-validator`
- Green: `timeout 100s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-rigid-disequality-progress-certificates`
- Green: `timeout 45s lein test proflog.sjas-correspondence-test`
- Green: `git diff --check`
- Green: `lein test-proflog-fast`
- Green: `lein test-proflog-extended`
