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

## Decomposition Can Need An Earlier Binding First

Query:

```clojure
exists a. exists b. exists t.
  [1] = cons(a, cons(b, t))
```

Representative proof term:

```clojure
(witness
 (witness
  (witness
   (decompose
    (args
     (par-bind)
     (free-close))))))
```

This is the shape that blocked `sorted2([1])` before the fix:

1. the existential witnesses are introduced as branch-local parameters,
2. decomposition first binds `a = 1`,
3. only then does the tail comparison become `null = cons(b, t)`,
4. that later constructor clash closes the branch.

The important point is that contradiction is not always visible at the root.
Sometimes the kernel must carry an earlier equality binding through the
decomposition before the later constructor mismatch appears.

## Boundary Cases Also Covered

The namespace now also records two complementary non-trivial boundaries:

- a same-head disequality such as `pair(x, a) != pair(x, b)` stays open until
  some later equality forces a contradiction,
- nested occurs-check failures such as `x = f(g(x))` still close, even when
  the cycle is buried under multiple constructors.
