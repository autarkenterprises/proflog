# Equality And Disequality

This file covers the expanded regressions in `test/proflog/equality_test.clj`.

## Transitive Equality Can Break A Later Disequality

Query:

```clojure
x = y and y = a and x != a
```

Representative proof term:

```clojure
(conj
 (eq-step
  (eq-bind)
  (conj
   (eq-step
    (eq-bind)
    (refl-close)))))
```

Operationally:

1. the first equality links `x` and `y`,
2. the second equality binds that chain to `a`,
3. the remaining disequality collapses to `a != a`,
4. `refl-close` finishes the contradiction.

This is the regression that guards transitive propagation through a branch,
not just one-step substitution.

## Same-Head Equalities Decompose Recursively

Query:

```clojure
pair(a, b) = pair(a, c)
```

Representative proof term:

```clojure
(decompose (free-close))
```

The prover does not stop at the outer `pair/2` constructor. It decomposes the
equality into argument equalities and then discovers the inner clash `b = c`,
which closes by constructor mismatch.

## Boundary Cases Also Covered

The namespace now also records two complementary non-trivial boundaries:

- a same-head disequality such as `pair(x, a) != pair(x, b)` stays open until
  some later equality forces a contradiction,
- nested occurs-check failures such as `x = f(g(x))` still close, even when
  the cycle is buried under multiple constructors.
