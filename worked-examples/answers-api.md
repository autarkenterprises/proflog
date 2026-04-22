# Answers API

This file covers `test/proflog/answers_test.clj`.

## Ground Enumeration

The bounded ground-term enumerator over the simple unary Peano language returns:

```clojure
[(app zero)
 (app s (app zero))
 (app s (app s (app zero)))
 (app s (app s (app s (app zero))))]
```

In decimal notation, that is:

```clojure
[0 1 2 3]
```

## Direct Symbolic Binding Export

For:

```clojure
p(x) :- x = zero
```

the open query:

```clojure
p(x)
```

exports:

```clojure
{:bindings [[x (app zero)]]
 :residuals []
 :proofs [(neq-close (eq-bind))]}
```

So the answer API returns the direct substitution `x = 0` without requiring
bounded ground enumeration.

## Residual Disequalities Survive Export

For the formula:

```clojure
x != 1 and 0 = 1
```

the proof closes through the contradiction `0 = 1`, but the exported answer
still preserves the surviving side condition:

```clojure
{:bindings [[x x]]
 :residuals [(neq x 1)]}
```

This is the current contract: symbolic answers keep residual constraints when
they remain semantically relevant.

## Duplicate Proofs Do Not Define Answer Cardinality

For the intentionally duplicated program:

```clojure
dup(x) :- x = zero
dup(x) :- x = zero
dup(x) :- x = s(zero)
```

the open query:

```clojure
dup(x)
```

should return the two unique answers `x = 0` and `x = 1`, even though the
first answer has two proof paths.

The current exporter now does two important things here:

- it keeps searching raw proof states until it has the requested number of
  unique answer records, rather than truncating first and merging afterward,
- it drops impossible residual artifacts such as `neq(0, 0)` instead of
  exporting them as if they were meaningful side conditions.

So the current records are:

```clojure
{:bindings [[x 0]]
 :residuals []}

{:bindings [[x 1]]
 :residuals [1 != 0, 1 != 0]}
```

The duplicated disequalities on the second answer are still redundant, but they
are semantically harmless. The important correction is that the later distinct
answer is no longer starved by earlier duplicate proof paths.

## Recursive Open Query: `even(x)`

Using the recursive parity program, the open query:

```clojure
even(x)
```

currently exports two families:

```clojure
{:bindings [[x 0]]
 :residuals []}

{:bindings [[x s(a_1)]]
 :residuals [s(a_1) != 0, not odd(a_1)]}
```

The first record is the direct base witness. The second is the recursive
symbolic family: numbers that are one successor above a value whose `odd`
branch still needs to fail.

## Open Query: `win(x)`

For the inline Nim program, the generic answer API returns symbolic winning
families rather than enumerating every numeral eagerly.

The first two current records are:

```clojure
{:bindings [[x s(a_1)]]
 :residuals [win(a_1)]}

{:bindings [[x s(s(a_1))]]
 :residuals [s(s(a_1)) != s(a_1), win(a_1)]}
```

So the answer API says:

- a one-step predecessor of a losing position is winning,
- a two-step predecessor is also winning, with the additional disequality
  witness recorded explicitly.

## Bounded Ground Materialization

The non-generic helper can still materialize a concrete answer when requested.

Current example:

```clojure
query-ground-answers win(x) => [1]
```

So the first small winning Nim position recovered by bounded materialization is
`1`.
