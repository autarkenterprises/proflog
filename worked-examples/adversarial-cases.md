# Adversarial Cases

This file covers `test/proflog/adversarial_test.clj`.

The namespace checks a specific semantic hazard: a procedure call may be saved
on the branch before the equality that makes its clause body contradictory.
The greenfield kernel must still notice that later equality and close the saved
call.

## Positive Call Unlocked Late

Program:

```clojure
r(x) :- x != zero
```

Query:

```clojure
r(x) and x = zero
```

Representative proof term:

```clojure
(conj
 (savefml
  (eq-step
   (eq-bind)
   (eq-triggered-call (refl-close)))))
```

Operationally:

1. `savefml` stores the positive call `r(x)` on the branch.
2. `eq-step (eq-bind)` later binds `x = zero`.
3. `eq-triggered-call` reopens the saved call under the new substitution.
4. `refl-close` closes the clause body because `zero != zero` is impossible.

## Negative Call Unlocked Late

Program:

```clojure
p(x) :- x = zero
```

Query:

```clojure
not p(x) and x = zero
```

Representative proof term:

```clojure
(conj
 (savefml
  (eq-step
   (eq-bind)
   (eq-triggered-neg-call (refl-close)))))
```

This is the same pattern on the negative side:

1. the branch saves `not p(x)`,
2. a later equality fixes `x = zero`,
3. the kernel revisits the saved negative call,
4. the clause body now succeeds, so the negated call closes.

## Why This Namespace Matters

These tests are not about raw speed. They guard a correctness property:
procedure-call closure must be order-insensitive with respect to equalities on
the same branch. If these regressions fail, the prover can miss real
contradictions simply because the equalities arrived "too late".
