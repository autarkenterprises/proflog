# Query And Program Behavior

This file covers the current procedure-call, query-status, `P1`, `P2`, and
bounded-query examples from:

- `test/proflog/program_test.clj`
- `test/proflog/query_test.clj`
- `test/proflog/query_extended_test.clj`

## Clause Lookup Walkthrough

The simplest compiled program is:

```clojure
p(x) :- x = zero
```

Calling `program/call-clauseo` with the actual argument `succ(zero)` yields:

```clojure
env         = [[a_0 (app succ (app zero))]]
body        = (eq a_0 (app zero))
negated-body = (neq a_0 (app zero))
```

So the program layer does two things at once:

1. binds the compiled clause parameter to the actual argument,
2. exposes both the body and its negation for positive and negative
   subsidiary tableaux.

## Positive And Negative Procedure Calls

On the same program:

```clojure
p(x) :- x = zero
```

the current kernel shows the two closure directions directly.

### `p(one)` is false

Query:

```clojure
(pos (app p (app one)))
```

Proof term:

```clojure
(pos-call (free-close))
```

The positive call opens the clause body `one = zero`, which closes by
constructor clash.

### `not p(zero)` is false

Query:

```clojure
(neg (app p (app zero)))
```

Proof term:

```clojure
(neg-call (refl-close))
```

The negative call opens the negated body `zero != zero`, which closes
immediately.

The same pattern appears for the multi-argument example:

```clojure
pair-eq(x, y) :- x = y
```

- `pair-eq(zero, one)` closes as `(pos-call (free-close))`
- `not pair-eq(zero, zero)` closes as `(neg-call (refl-close))`

## Caller-Branch Isolation

The isolation example is:

```clojure
p(x) :- q(x)
```

The key point is negative:

- `p(zero) and not q(zero)` stays open
- `not p(zero) and q(zero)` stays open

The subsidiary tableau for `p` does not borrow unrelated caller literals.

## Query Status Walkthrough

For:

```clojure
p(x) :- x = zero
```

and declared-but-undefined relation `undef/1`, the public query API reports:

```clojure
p(0)      => :succeeds
p(1)      => :fails
undef(0)  => :unresolved
```

This is the operational distinction the greenfield query layer is currently
committed to preserve.

## Fitting `P1`

The current `P1` program is:

```clojure
even(x) :- x = zero
        or exists y. x = s(y) and odd(y)

odd(x)  :- forall y. (even(y) -> x != y)
```

### `even(0)` succeeds

Proof term:

```clojure
(neg-call (conj (refl-close)))
```

The base branch closes immediately because the negated body contains
`zero != zero`.

### `odd(1)` succeeds

Representative proof shape:

```clojure
(neg-call
 (witness
  (conj
   (savefml
    (eq-step
     (par-bind)
     (eq-triggered-call ...))))))
```

Operationally:

1. the negative call opens the negation of the `forall` body,
2. an existential witness is introduced,
3. equality binds that witness to `1`,
4. the saved equality unlocks the recursive `even` call,
5. the recursive branch eventually closes through `even(0)`.

### Deeper committed `P1` checks

The extended quantified suite also exercises:

```clojure
even(2) => succeeds
odd(0)  => fails
```

The `odd(0)` failure proof is compact:

```clojure
(pos-call
 (univ
  (split
   (neg-call (conj (neq-close (eq-bind))))
   (refl-close))))
```

The prover instantiates the universal, then closes both branches of the
disjunction.

## Fitting `P2` Inline Nim

The current inline Nim program is:

```clojure
win(x) :- exists y.
            (x = s(y) or x = s(s(y)))
            and not win(y)
```

Current direct ground pattern:

```clojure
win(0) => fails
win(1) => succeeds
win(2) => succeeds
win(3) => fails
win(4) => succeeds
win(5) => succeeds
```

### `win(3)` fails

Representative proof shape:

```clojure
(pos-call
 (witness
  (conj
   (split
    ...
    ...))))
```

The prover must show that every move from `3` lands in a winning position.
Both one-step and two-step move branches close.

### `win(1)` succeeds

Representative proof shape:

```clojure
(neg-call
 (once-univ
  (split
   (conj (neq-close (eq-bind)))
   (pos-call ...))))
```

The winning witness is `y = 0`, and the remaining obligation is to show that
`win(0)` fails.

## Bounded Query Helpers

The bounded helper tests deliberately separate operational time control from
semantic truth.

Current examples:

```clojure
(query-succeeds-within P2 win(0) 25ms) => ()
(query-fails-within    P2 win(1) 25ms) => ()
```

Both calls return the empty result when the time budget expires before a proof
arrives.

Easy non-recursive proofs still surface within budget:

```clojure
(query-succeeds-within status-program p(0) 500ms) => non-empty
(query-fails-within    status-program p(1) 500ms) => non-empty
```
