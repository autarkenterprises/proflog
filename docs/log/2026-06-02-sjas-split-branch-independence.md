# SJAS Split Branch Independence

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

The SJAS-local tableau proof checker previously mirrored the ordinary Proflog
kernel's `split` bookkeeping by threading the left disjunct's output equality
substitution and disequality store into the right disjunct. That is an
implementation-level sequencing choice in the host proof kernel, not an
admissible semantic-tableau proof-predicate rule for `IS#_D(beta)`.

For the arithmeticized proof predicate, disjunction creates sibling branches.
The sibling branches share the incoming branch state and each must close under
that state, but equality or disequality updates produced while closing one
sibling cannot become evidence for closing the other sibling. Otherwise the
proof predicate can accept proof trees whose second branch closes only because
the first branch performed a unification.

The checker now validates the left and right split children from the same
incoming `sigma` and `neqs`, ignores sibling-local output state, and returns
the incoming state after both branches close. This makes the `split` constructor
an object-level semantic-tableau branch rule rather than a sequential host
proof-state shortcut.

## Red Evidence

The new focused regression constructs a direct proof-check target:

```clojure
(and true
     (exists x
       (or (and (= x 0) false)
           (!= x 0))))
```

and supplies the invalid certificate:

```clojure
(conj
  (witness
    (split
      (conj (eq-step (par-bind) (false-close)))
      (refl-close))))
```

Before the fix, the test failed because the proof checker accepted the proof:
the left branch bound the existential parameter to `0`, and the right branch
then used that sibling-local binding to close `x != 0` by reflexive
disequality.

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-keeps-split-branch-state-independent
  FAIL: expected empty result, actual (true)
```

## Verification

Focused green checks completed:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-keeps-split-branch-state-independent
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-truth-and-falsehood-constructors-without-kernel-validator
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-positive-equality-steps-without-kernel-validator
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 65 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-false-close-certificates
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-positive-equality-step-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-reflected-negative-call-alternative-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-exposes-relevant-and-unresolved-constructors
  Ran 1 tests containing 22 assertions.
  0 failures, 0 errors.
```

`sjas-tableau-proof-accepts-positive-equality-step-certificates` was long but
productive, staying in core.logic proof search with stable memory before
passing. That remains a performance hot spot, not a semantic acceptance
shortcut.
