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

## `member(a, [a])`

Query:

```clojure
member(a, cons(a, null))
```

Proof term:

```clojure
(neg-call
 (once-univ
  (once-univ
   (split
    (neq-close (decompose (args (eq-bind) (args (eq-bind) ()))))
    (conj (refl-close))))))
```

The first disequality branch forces the list head to `a`, and the head-case
disjunct then closes reflexively.

## `member(a, [b, a])`

Query:

```clojure
member(a, cons(b, cons(a, null)))
```

This is the first non-trivial recursive `member` success. The proof descends
through the tail after the head-case disjunct fails on `b`, then closes on the
recursive call over `[a]`.

## `append([a], [b], [a, b])`

Query:

```clojure
append(cons(a, null), cons(b, null), cons(a, cons(b, null)))
```

Proof term:

```clojure
(neg-call
 (conj
  (split
   (neq-store
    (once-univ
     (once-univ
      (once-univ
       (split
        (neq-close (decompose (args (eq-bind) (args (eq-bind) ()))))
        (split
         (neq-close (decompose (args (decompose ()) (args (eq-bind) ()))))
         (neg-call (conj (split (refl-close) (refl-close))))))))))
   ...)))
```

Operationally:

1. the base-clause disequalities are stored because this is not the empty-list case,
2. the recursive clause instantiates `head = a`, `tail = []`, and `rest = [b]`,
3. the recursive call reduces to the base append case `append([], [b], [b])`.

## Wrong Result Example

The current suite also records:

```clojure
append([a], [b], [a]) => fails
member(c, [a, b])     => fails
```

So the recovered recursive list behavior now includes both positive and
negative one-step cases.

## `reverse([a], [a])`

This singleton reverse example now succeeds as well. It unfolds one recursive
step and then discharges the trailing append through the base case
`append([], [a], [a])`.

## Current Boundary

The greenfield list program itself already contains `member`, recursive
`append`, and recursive `reverse`, but the current committed namespace still
stops short of:

- inverse split enumeration,
- nested-list families.

The main remaining gaps after this recovery are deeper reverse cases such as
`reverse([a, b], [b, a])`, broader inverse split enumeration for
`append(x, y, [a, b, c])`, and the legacy nested/deeper list families.

This boundary is intentional for the committed baseline: the current slice now
covers base cases plus the first recovered recursive `member`/`append`/single
reverse behaviors without pretending that the deeper reverse and inverse list
families are already closed.
