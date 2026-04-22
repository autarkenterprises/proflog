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

### Non-base ground truths

The current extended slice also proves:

```clojure
plus(1, 0, 1)
plus(1, 1, 2)
plus(2, 1, 3)
plus(2, 3, 5)
```

The first two recursive truths already have the same outer proof shape as the
base case, but the proof objects are larger because they must peel one or more
successor layers before returning to the base branch.

The two-step example `plus(2, 1, 3)` is representative:

```clojure
(neg-call
 (conj
  (split
   (neq-store ...)
   (neq-store ...))))
```

Operationally the proof:

1. rejects the base branch because `2 != 0`,
2. opens the recursive clause,
3. strips one successor layer from the first and third arguments,
4. recurses until the base clause closes.

### Wrong sums are refuted directly

The current failure slice proves:

```clojure
plus(1, 1, 1) => false
plus(0, 1, 0) => false
plus(1, 2, 2) => false
```

The shortest wrong-sum example is:

```clojure
plus(0, 1, 0)
```

with proof term:

```clojure
(pos-call
 (split
  (conj (eq-step (decompose ()) (free-close)))
  (witness (witness (conj (free-close))))))
```

The positive call closes both branches:

1. the base branch reduces to `1 = 0`, which closes,
2. the recursive branch reduces to `0 = s(_)`, which also closes.

## Current Boundary

This namespace intentionally stops short of:

- recursive `tc` negative cases such as `tc(c, a)` or `tc(a, a)`.

The `plus/3` family now goes well beyond its original base-case placeholder and
should be read together with the open and partial `plus` examples in
`worked-examples/synthesis-modes.md`.

Those are phase-2 closure items on `ADR-0009`.
