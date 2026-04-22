# List Programs

This file covers `test/proflog/list_programs_test.clj`.

List constructors:

```clojure
null
cons(head, tail)
```

Current relations:

```clojure
member(x, xs)
append(xs, ys, zs)
reverse(r1, r2)
```

## `append([], [a], [a])`

Query:

```clojure
append(null, cons(a, null), cons(a, null))
```

Proof term:

```clojure
(neg-call (conj (split (refl-close) (refl-close))))
```

The base branch fires directly:

- `xs = null`
- `zs = ys`

Both equalities become reflexive.

## `append([], [a], z)`

Open query:

```clojure
append(null, cons(a, null), z)
```

Exported answer record:

```clojure
{:bindings [[z (app cons (app a) (app null))]]
 :residuals []
 :proofs [(conj (split (refl-close) (neq-close (eq-bind))))]}
```

So the exporter returns the expected concrete binding:

```clojure
z = [a]
```

with no residual obligations.

## `reverse([], [])`

Query:

```clojure
reverse(null, null)
```

Proof term:

```clojure
(neg-call (conj (split (refl-close) (refl-close))))
```

Again the base clause closes immediately.

## Current Boundary

The greenfield list program itself already contains `member`, recursive
`append`, and recursive `reverse`, but the current committed namespace still
stops short of:

- `member` execution examples,
- non-empty recursive `append`/`reverse` proofs,
- inverse split enumeration,
- nested-list families.

Those move into phase 2 of `ADR-0009`.

This boundary is intentional for the committed baseline: the two current tests
pin down the executable empty-list branch and the answer-export shape without
pretending that the deeper recursive list families are already closed.
