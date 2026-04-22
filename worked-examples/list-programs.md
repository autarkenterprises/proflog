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

## `append([a, b], [c], [a, b, c])`

Query:

```clojure
append(cons(a, cons(b, null)),
       cons(c, null),
       cons(a, cons(b, cons(c, null))))
```

This is the next fully ground recursive append case beyond the earlier
one-step regression. Operationally, the proof:

1. rejects the base branch because the left input is non-empty,
2. binds `head = a` and reduces to
   `append([b], [c], [b, c])`,
3. repeats the same recursive clause once more,
4. closes on the base append case
   `append([], [c], [c])`.

The important point is semantic, not aesthetic: the greenfield kernel can now
carry the recursive append relation through two full constructor layers and
still close the proof with no residual obligations.

Recorded successful runtime for the final committed iteration:

```text
154219.489533 ms
```

## `reverse([a, b], [b, a])`

Query:

```clojure
reverse(cons(a, cons(b, null)),
        cons(b, cons(a, null)))
```

This is the first non-trivial ground reverse case beyond the singleton list.
Its proof path is:

1. decompose the input as `head = a`, `tail = [b]`,
2. recurse on `reverse([b], [b])`,
3. reduce the trailing append obligation to
   `append([b], [a], [b, a])`,
4. close that append through the recursive append chain.

This case matters because it confirms that recursive `reverse` is executable in
the greenfield stack, not just its base and singleton slices.

Recorded successful runtime for the final committed iteration:

```text
276769.773115 ms
```

## `append([[a]], [[b]], z)`

Open query:

```clojure
append(cons(cons(a, null), null),
       cons(cons(b, null), null),
       z)
```

Current exported answer record:

```clojure
{:bindings [[z (app cons
                    (app cons (app a) (app null))
                    (app cons
                     (app cons (app b) (app null))
                     (app null)))]]
 :residuals
 [(neq (app cons (app cons (app a) (app null))
             (app cons (app cons (app b) (app null)) (app null)))
       (app cons (app cons (app b) (app null)) (app null)))
  (neq (app cons (app cons (app a) (app null)) (app null))
       (app null))]}
```

So the answer exporter does recover the intended nested list:

```clojure
z = [[a], [b]]
```

but it does not yet fully normalize away the supporting disequalities introduced
while ruling out the wrong append branches. That is still a valid greenfield
answer record: the constructive binding is concrete, and the residuals make the
remaining proof obligations explicit instead of silently dropping them.

Recorded successful runtime for the final committed iteration:

```text
41655.620203 ms
```

## Current Boundary

The greenfield list program itself already contains `member`, recursive
`append`, and recursive `reverse`, but the current committed namespace still
stops short of:

- inverse split enumeration,
- nested-list families beyond the first forward case.

The main remaining gap inside the basic list family is inverse split
enumeration for `append(x, y, [a, b, c])`. In the latest long successful probe,
that query took:

```text
1372558.603771 ms
```

and still returned only the base split plus the first recursive split family.
So the semantic story is improving faster than the operational story: deeper
ground append and reverse now close, and the first nested forward append answer
binds correctly, but inverse list enumeration remains a serious performance
boundary.

This boundary is intentional for the committed baseline: the current slice now
covers base cases, recovered recursive `member`, one- and two-step ground
`append`, singleton and two-element ground `reverse`, and the first nested
forward append answer, without pretending that inverse enumeration and the
deeper nested list families are already closed.
