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
- the kernel now prunes saved disequalities that have already become false
  under the current substitution, so stale artifacts such as `neq(0, 0)` do
  not survive as residual constraints,
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
answer is no longer starved by earlier duplicate proof paths, and already-false
disequalities no longer leak out as residuals.

## Diagnostics: Raw Proof Growth Versus Unique Answers

The answer layer now also exposes a diagnostics helper:

```clojure
query-answer-diagnostics
```

For the duplicated `dup(x)` program above, the snapshots for raw limits
`[1 2 4]` show the difference between raw proof paths and unique exported
answers:

```clojure
{:raw-limit 1, :raw-count 1, :unique-count 1}
{:raw-limit 2, :raw-count 2, :unique-count 1}
{:raw-limit 4, :raw-count 3, :unique-count 2}
```

So the second raw proof is still just a duplicate witness for `x = 0`, and the
later distinct answer `x = 1` only appears once the raw stream is allowed to
grow past those duplicates.

## Staged Deepening Policy

`query-answers` no longer bets everything on one fully unfolded query shape.
Instead, it searches call-depth stages `0`, `1`, `2`, ... up to the requested
budget and keeps the deepest stage that still produces exportable answers.

That gives two useful behaviors:

- deeper productive stages override shallower ones,
- but if a deeper stage goes dry, the API falls back to the last productive
  frontier instead of returning `[]`.

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

## Diagnostics: Reverse Frontier

For the open list query:

```clojure
reverse([a, b], r)
```

the diagnostics helper is useful because the first exported symbolic answer is
not yet the final concrete reverse. At `call-depth 1`, the first snapshot is:

```clojure
{:raw-limit 1
 :raw-count 1
 :unique-count 1
 :sample-records
 [{:bindings [[r []]]
   :residuals
   [[a, b] != []
    not append(a_3, [a], [])
    not reverse([b], a_3)]}]}
```

This means the prover has exposed the first recursive frontier:

- it has recognized that `reverse([a, b], r)` cannot be using the empty-list
  base case,
- it has introduced an intermediate accumulator-like value `a_3`,
- and it has deferred the deeper obligations `reverse([b], a_3)` and
  `append(a_3, [a], [])`.

So the current greenfield behavior is not “no semantics at all.” It can export
the first symbolic frontier. The remaining gap is that deeper unfolding and
answer search still do not cheaply drive that frontier to the concrete answer
`r = [b, a]`.

With the staged fallback policy, asking for `call-depth 2` now keeps this
depth-1 frontier rather than dropping to an empty result set when the deeper
stage fails to produce an answer. On the current branch, that `call-depth 2`
query returns the same two symbolic frontier records in about `35.9 s`.

On the current `adr-0009` branch, the measured diagnostics are:

```clojure
call-depth 1, raw-limit 1:
  expansion ~= 1 ms
  search    ~= 1815.80 ms
  raw-count = 1
  unique-count = 1

call-depth 2, raw-limit 1:
  expansion ~= 0.34 ms
  search    ~= 54681.94 ms
  raw-count = 0
  unique-count = 0
```

So the operational cliff is not eager unfolding itself. The step from
`call-depth 1` to `call-depth 2` changes the search problem enough that the
first raw proof state disappears under the same fuel slice.

## Diagnostics: Inverse Append Frontiers

For:

```clojure
append(a, b, [a, b, c])
```

with both `a` and `b` free, the current diagnostics show:

```clojure
raw-limit 1:
  search ~= 28915.46 ms
  raw-count = 1
  unique-count = 1

raw-limit 2:
  search ~= 41559.38 ms
  raw-count = 2
  unique-count = 2

raw-limit 4:
  search ~= 53524.49 ms
  raw-count = 3
  exported-count = 3
  unique-count = 2
```

So the current greenfield engine can recover the first two split families:

- `a = []`, `b = [a,b,c]`
- `a = [a]`, `b = [b,c]`

But by the time the raw stream is exhausted at this fuel slice, the deeper
`[a,b]` / `[c]` and `[a,b,c]` / `[]` splits still have not surfaced. The third
raw proof is only a duplicate witness for the second unique answer.

At the main `query-answers` API, the staged deepening policy now uses the
depth-2 stage for this query because it is still productive. The current
result is:

```clojure
{:bindings [[a []] [b [a,b,c]]]
 :residuals []}

{:bindings [[a [a]] [b [b,c]]]
 :residuals [[a,b,c] != [b,c], [a] != []]}
```

So the answer layer now reaches the first recursive split family through a
deeper productive stage. On the current branch, that `call-depth 2` query
returns those two records in about `35.3 s`. It is still below legacy parity because the
deeper `[a,b]` / `[c]` and `[a,b,c]` / `[]` solutions have not surfaced yet.

## Bounded Ground Materialization

The non-generic helper can still materialize a concrete answer when requested.

Current example:

```clojure
query-ground-answers win(x) => [1]
```

So the first small winning Nim position recovered by bounded materialization is
`1`.
