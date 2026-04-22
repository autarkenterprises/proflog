# Integration Families

This file covers `test/proflog/integration_families_test.clj`.

## Transitive Closure

The current integration graph is:

```clojure
edge(a, b)
edge(b, c)
tc(x, y) :- edge(x, y)
         or exists z. edge(x, z) and tc(z, y)
```

The committed greenfield baseline currently checks only the direct edges.

### `tc(a, b)` succeeds

Proof term:

```clojure
(neg-call
 (conj
  (neg-call (conj (split (refl-close) (refl-close))))))
```

The direct `edge(a, b)` branch closes immediately.

### `tc(b, c)` succeeds

Proof term:

```clojure
(neg-call
 (conj
  (neg-call
   (conj
    (split
     (neq-store ...)
     (neq-store ...))))))
```

The proof is slightly larger because it first eliminates the `a -> b` base
branch before closing the `b -> c` branch.

## Peano Addition

The current arithmetic relation is:

```clojure
plus(x, y, z) :- x = zero and z = y
              or exists x1 z1.
                   x = s(x1)
                   and z = s(z1)
                   and plus(x1, y, z1)
```

The committed baseline currently checks the zero-left base case.

### `plus(0, 2, 2)` succeeds

Proof term:

```clojure
(neg-call (conj (split (refl-close) (refl-close))))
```

The base branch closes because both equalities become reflexive.

## Current Boundary

This namespace intentionally stops short of:

- recursive `tc(a, c)` success,
- negative `tc` cases such as `tc(c, a)` or `tc(a, a)`,
- non-base `plus` proofs.

Those are phase-2 closure items on `ADR-0009`.
