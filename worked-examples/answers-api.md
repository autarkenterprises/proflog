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

The diagnostics now also summarize proof families, not just answer records. For
the same `dup(x)` slice at raw limit `4`, one productive stage reports:

```clojure
{:duplicate-exported-count 1
 :distinct-proof-signature-count 3
 :common-proof-signatures
 [{:count 1, :steps [...]}
  ...]}
```

So the helper can now separate:

- repeated exported answers,
- genuinely distinct raw proof families,
- and identical proof signatures.

## Direct Entry Policy

`query-answers` now keeps the original negated query and enters top-level
literal program calls directly in the kernel. `call-depth` is spent only on
recursive descendants below that entry boundary.

Within that exact search:

- call deferral is still a relational kernel choice,
- but while descent budget remains, the kernel tries real recursive descent
  before exporting a residual call frontier,
- and the answer layer ranks later closed answers ahead of earlier symbolic
  frontiers when both survive export.

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
not yet the final concrete reverse. With the direct-entry policy, that first
frontier now appears already at `call-depth 0`:

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
the first symbolic frontier through direct kernel entry descent. The remaining
gap is that the next recursive stage still does not cheaply drive that frontier
to the concrete answer `r = [b, a]`.

## Worked Example: `query-answers reverse([a,b], r)`

At the main `query-answers` API, the current first exported record depends on
the requested `call-depth`. With:

```clojure
{:proof-limit 1
 :max-raw-proof-limit 16
 :fuel 32
 :call-depth 0}    ;; or 1
```

the first exported records are:

```clojure
call-depth 0:

{:bindings [[r []]]
 :residuals
 [[a,b] != []
  not append(a_3, [a], [])
  not reverse([b], a_3)]}

call-depth 1:

{:bindings [[r [a]]]
 :residuals
 [[a] != []
  [a,b] != []
  not reverse([b], [])
  not reverse([b], [])]}
```

These are not complete extensional answers. The only complete answer to
`reverse([a,b], r)` is still `r = [b,a]`. The exported record instead means:

- current binding frontier,
- plus residual obligations that must still be discharged.

So:

- `r = []` means "the current branch has matched the outer reverse clause in a
  way that binds `r` to `[]`, but it still owes a recursive `reverse/2` call and
  an `append/3` call,"
- `r = [a]` means "one deeper descent has refined the frontier, but a recursive
  obligation still remains, so this is still not a closed answer."

The duplicated `not reverse([b], [])` residual in the `call-depth 1` record is
real. It comes from distinct proof paths that collapse to the same exported
symbolic state.

## Parameter Meanings

For this worked example, the important `query-answers` controls are:

- `call-depth`: recursive procedure-call descent budget below the surface query
  boundary. The top-level `reverse/2` query entry itself does not spend this
  budget.
- `fuel`: global proof-search step budget. Kernel steps that actually descend or
  expand formulas consume fuel; when fuel runs out, deeper search stops.
- `proof-limit`: number of unique exported answer records requested.
- `max-raw-proof-limit`: cap on how many raw kernel proof states the collector is
  allowed to sample while looking for those exported answers.

The repository also uses the phrase "raw proof limit" in diagnostics. The
relationship is:

- `query-answer-diagnostics` takes an explicit `raw-limit` and reports exactly
  what that one raw slice produced,
- `query-answers` grows an internal raw limit up to `max-raw-proof-limit`,
  merges duplicate exported answers, ranks them by completion, and returns up to
  `proof-limit` unique records.

## Why These Bindings And Residuals Appear

At `call-depth 0`, the top-level query can enter `reverse([a,b], r)` directly,
but it cannot spend any recursive budget below that entry call. So the exporter
sees the first satisfiable symbolic frontier:

- `r` is bound to `[]`,
- `[a,b] != []` records that the non-empty input cannot be the reverse base
  case,
- `not reverse([b], a_3)` and `not append(a_3, [a], [])` are the remaining
  obligations from the recursive reverse clause.

At `call-depth 1`, the kernel may spend one recursive descent below the entry
call. That extra descent refines the frontier:

- the intermediate structure now forces `r = [a]`,
- `[a] != []` records that the refined result is still not the append base case,
- `[a,b] != []` remains from the outer reverse decomposition,
- `not reverse([b], [])` is now the still-open recursive obligation, surfaced
  twice because two proof paths collapse to the same exported residual state.

So the progression from `[]` to `[a]` is not unsoundness. It is the symbolic
answer API exposing progressively refined open proof frontiers rather than only
fully closed substitutions.

At the current boundary, deeper exact stages remain productive, but reverse is
still much harder than append:

```clojure
call-depth 0, raw-limit 1:
  raw-count = 1
  unique-count = 1

call-depth 1, raw-limit 1:
  raw-count = 1
  unique-count = 1
```

Operationally:

- `call-depth 1` refines the first exported reverse frontier from `r = []` to
  `r = [a]`,
- inverse `append` can again prefer its first closed recursive split,
- but reverse still did not reach `r = [b, a]` in a longer `fuel 64`,
  `call-depth 3`, raw-`64` probe.

## Diagnostics: Inverse Append Frontiers

For:

```clojure
append(a, b, [a, b, c])
```

with both `a` and `b` free, the current greenfield engine can recover the first
two split families:

- `a = []`, `b = [a,b,c]`
- `a = [a]`, `b = [b,c]`

But the deeper `[a,b]` / `[c]` and `[a,b,c]` / `[]` splits still have not
surfaced.

At the main `query-answers` API, `call-depth 1` now merges the productive stage
`0` symbolic frontier with the deeper stage-`1` refinement. The current result is:

```clojure
{:bindings [[a []] [b [a,b,c]]]
 :residuals []}

{:bindings [[a [a]] [b [b,c]]]
 :residuals [[a,b,c] != [b,c], [a] != []]}
```

Stage `0` still exports a symbolic recursive cons-family for the second split,
while stage `1` refines that family into the concrete split `([a], [b,c])`.
The merged answer API therefore keeps the concrete refinement without throwing
away the shallower symbolic information entirely. It is still below legacy
parity because the deeper `[a,b]` / `[c]` and `[a,b,c]` / `[]` solutions have
not surfaced yet.

## Stage Diagnostics: Where Search Goes Dry

The stronger harness is:

```clojure
query-stage-diagnostics
```

It sweeps stage `0`, `1`, `2`, ... up to the requested `call-depth`, runs one
or more raw-limit slices at each stage, and reports whether that stage is still
productive at all. The diagnostics also report the searched formula, the
remaining `unfold-depth` (now `0` in the default path), and the kernel
`call-depth` used at that stage.

For `reverse([a,b], r)` at `fuel 32`, `raw-limit 1`, the current summary is:

```clojure
{:stage 0, :productive? true, :raw-count 1, :unique-count 1}
{:stage 1, :productive? false, :raw-count 0, :unique-count 0}
```

That matters because it shows the failure mode precisely:

- stage `0` is already the real direct-entry reverse frontier,
- stage `1` is simply dry for this fuel slice.

So the current reverse gap is not “the answer exporter ignored a deeper proof.”
At this stage and fuel, no first raw proof state surfaces at all.

For `append(a, b, [a,b,c])`, the staged picture is different:

- stage `0` already reaches the base split plus a symbolic recursive cons-family,
- stage `1` concretizes that symbolic family into the second split
  `([a],[b,c])`.

That is why the merged answer policy matters. The deeper stage contributes a
more concrete second answer, but the answer layer no longer assumes that deeper
productivity automatically subsumes every shallower symbolic family.

## Bounded Ground Materialization

The non-generic helper can still materialize a concrete answer when requested.

Current example:

```clojure
query-ground-answers win(x) => [1]
```

So the first small winning Nim position recovered by bounded materialization is
`1`.

## Closed-Answer Parity Mode

ADR-0012 adds a separate helper:

```clojure
query-parity-answers
```

This is intentionally not the same thing as `query-answers`.

- `query-answers` is the generic symbolic API. It keeps residual obligations and
  may export open proof frontiers such as `r = []` or `r = [a]`.
- `query-parity-answers` is a specialty closed-answer materializer for the
  legacy list-family parity questions. It returns only empty residuals and does
  not try to preserve the generic symbolic contract.

For the legacy reverse query:

```clojure
query-parity-answers reverse([a,b], r)
```

the current record is:

```clojure
{:bindings [[r [b,a]]]
 :residuals []
 :proofs []}
```

For inverse append:

```clojure
query-parity-answers append(a, b, [a,b,c])
```

the current records are:

```clojure
{:bindings [[a []] [b [a,b,c]]]
 :residuals []
 :proofs []}

{:bindings [[a [a]] [b [b,c]]]
 :residuals []
 :proofs []}

{:bindings [[a [a,b]] [b [c]]]
 :residuals []
 :proofs []}

{:bindings [[a [a,b,c]] [b []]]
 :residuals []
 :proofs []}
```

Two boundaries matter here:

- this mode now requires fully empty residuals for parity,
- and on the list-family fast path it leaves `:proofs` empty on purpose.

That is not a bug. The proof authority for these concrete list cases remains the
direct semantic regressions in `test/proflog/list_programs_test.clj`. The parity
mode is an isolated extensional answer layer used to compare greenfield against
legacy without redefining the generic symbolic answer API.
