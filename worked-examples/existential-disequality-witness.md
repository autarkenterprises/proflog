# Existential Disequality Witness

This example records the semantic split covered by
`test/proflog/legacy_impurity_test.clj` and promoted by ADR-0018.

## Program

Use a language with exactly the declared constants `a` and `b`, and relation
`p/1`:

```prolog
p(x) :- exists y. x != y
```

Over this language, the intended ground meaning is finite and direct:

- `p(a)` should succeed, witnessed by `y = b`.
- `p(b)` should succeed, witnessed by `y = a`.
- `p(x)` should export exactly the object-language answers `a` and `b`.
- No public answer should contain an internal delta parameter `(par ...)`.

## Legacy Execution

The legacy probe asks for an open answer:

```clojure
(legacy/proveo
  ['pos ['app 'p answer]]
  '()
  '()
  '()
  program
  proof
  4)
```

The first result binds `answer` to a term whose head is `par`.
`legacy_impurity_test.clj` intentionally asserts only the shape of that result,
because the printed nominal identity is not stable:

```clojure
(legacy-par-term? (first results)) => true
```

Operationally, legacy reaches that result as follows:

1. The procedure-call guard runs `l-ground-termo` on the call argument.
2. Legacy `l-ground-termo` uses `project`, so an unbound logic variable is
   inspected host-side and admitted because it does not yet contain `(par ...)`.
3. The clause call maps formal `x` to the still-open answer variable.
4. The existential `y` introduces a fresh internal delta parameter `(par p)`.
5. The disequality branch can then close after the open answer variable is
   unified with that same delta parameter.

That is a useful negative reference, not a valid result. The answer is outside
the declared language `L`; it is an implementation artifact from `L^par`.
Accepting it would let a proof-local witness escape as a user-level program
answer.

## Greenfield Execution

The same program is compiled through the greenfield language boundary:

```clojure
(language/language
  {:constants ['a 'b]
   :relations {'p 1}})
```

Current greenfield behavior, measured on 2026-04-26, is:

```text
query-status p(a), timeout 1000 ms  => :unresolved
query-status p(b), timeout 1000 ms  => :unresolved
query-succeeds p(a), fuel 8         => ()
query-fails p(a), fuel 8            => ()
query-answers p(answer), fuel 8     => []
query-ground-answers p(answer),
  max-depth 0, fuel 8               => []
```

Greenfield is correct on the boundary that legacy violates:

- `kernel/l-ground-termo` walks terms structurally instead of using `project`.
- unresolved `(par ...)` terms are rejected at the procedure-call boundary.
- user answer variables are kept distinct from proof-local delta parameters.
- the public answer exporters refuse to emit `(par ...)`.

Greenfield is still incomplete for this program. It avoids legacy's unsound
internal-parameter answer, but it does not yet discover the available
object-language witnesses `a` and `b`.

## Target Behavior

ADR-0018 treats this as a gatekeeping example. A correct greenfield evaluation
must satisfy all of the following:

- `p(a)` returns `:succeeds`.
- `p(b)` returns `:succeeds`.
- `p(a)` and `p(b)` do not also return failure proofs.
- open answer evaluation returns exactly `a` and `b` under the documented
  finite-language or bounded-Herbrand policy.
- no query, answer record, residual, or public proof witness exports `(par ...)`
  as a user-level answer.

The practical point is that rejecting legacy's `(par ...)` answer is necessary
but not sufficient. The implementation must replace that artifact with the
real object-language witnesses that make the program true.
