# Robinson Q Proof Profile Example

This example documents `test/proflog/robinson_q_test.clj`, ADR-0048, and
ADR-0049. It
shows Robinson arithmetic Q in two forms:

- ordinary first-order assumptions: `Q1 and ... and Q7 -> theorem`;
- an opt-in proof profile: visible Q arithmetic terms are converted before the
  existing kernel checks equality, and Q3 closes by a recorded
  predecessor-or-zero case split.

Run the focused regression:

```text
lein test-proflog-robinson-q
```

Current result:

```text
Ran 6 tests containing 48 assertions.
0 failures, 0 errors.
wall 8.89 s
```

Run the timing comparison:

```text
lein probe-proflog-robinson-q
```

The comparison probe passed in `wall 7.83 s` on 2026-05-08.

## Hand-Written Theory

Robinson Q is written over terms, not procedures:

```text
zero
s(x)
add(x, y)
mul(x, y)
```

The relevant equations are:

```text
Q1. forall x. s(x) != zero
Q2. forall x y. s(x) = s(y) -> x = y
Q3. forall x. x != zero -> exists y. x = s(y)
Q4. forall x. add(x, zero) = x
Q5. forall x y. add(x, s(y)) = s(add(x, y))
Q6. forall x. mul(x, zero) = zero
Q7. forall x y. mul(x, s(y)) = add(mul(x, y), x)
```

There are no relation clauses here. `add` and `mul` are function symbols that
construct terms. They do not call a Proflog procedure.

## Frontend Shape

The ordinary and profiled language declarations differ only by proof-profile
metadata:

```clojure
(pf/language
  (constants zero)
  (functions (s 1)
             (add 2)
             (mul 2))
  (relations))

(pf/language
  (constants zero)
  (functions (s 1)
             (add 2)
             (mul 2))
  (relations)
  (proof-profile :robinson-q))
```

`proflog.robinson-q` exposes the backend equivalents as `rq/language` and
`rq/profile-language`.

## Backend AST

The language compiles to a function-only signature:

```clojure
{:constants #{zero}
 :functions {zero 0, s 1, add 2, mul 2}
 :relations {}}
```

With the profile selected, the language also carries:

```clojure
{:proof-profile :robinson-q}
```

The term `mul(2, 2)` is a nested object-language term:

```clojure
(app mul
  (app s (app s (app zero)))
  (app s (app s (app zero))))
```

The theorem `mul(2, 2) = 4` is an equality formula:

```clojure
(eq
  (app mul
    (app s (app s (app zero)))
    (app s (app s (app zero))))
  (app s (app s (app s (app s (app zero))))))
```

## Ordinary Q-As-Antecedent Evaluation

The ordinary path proves a theorem by asking whether Q entails it:

```clojure
(query/query-succeeds
  rq/ordinary-program
  (rq/q-implies
    (rq/eq (rq/mul (rq/numeral 2) (rq/numeral 2))
           (rq/numeral 4)))
  1
  96)
```

The query layer negates the implication and asks the existing program kernel to
close:

```text
Q1 and ... and Q7 and mul(2,2) != 4
```

This is ordinary proof from assumptions. Q7 is available because it is part of
the antecedent. The proof is not marked `robinson-q`, and the tests assert that
the ordinary path does not silently use the deduction-modulo profile.

## Deduction-Modulo Profile Evaluation

The profiled path leaves Q4-Q7 out of the query and selects a proof profile on
the language:

```clojure
(query/query-succeeds
  rq/profile-program
  (rq/eq (rq/mul (rq/numeral 2) (rq/numeral 2))
         (rq/numeral 4))
  1
  16)
```

The query still descends to the ordinary proof kernel, but first passes through
`proflog.proof-profile`. The `:robinson-q` method normalizes visible arithmetic
terms by these conversion rules:

```text
add(x, zero) -> x
add(x, s(y)) -> s(add(x, y))
mul(x, zero) -> zero
mul(x, s(y)) -> add(mul(x, y), x)
```

For Q7 itself, the profiled proof contains an explicit conversion marker:

```clojure
(profiled robinson-q
  ((q-rewrite :mul-succ
     (app mul (var x) (app s (var y)))
     (app add (app mul (var x) (var y)) (var x))))
  ...)
```

That is the semantic difference. Under ordinary Q, Q7 is an assumption. Under
`:robinson-q`, Q7 is proved by conversion plus equality closure.

Q3 is not a conversion. It is proved by closing the negated theorem shape:

```text
exists x. x != zero and once-forall y. x != s(y)
```

The profiled proof records that theory step directly:

```clojure
(profiled robinson-q
  ((q3-case-split predecessor-or-zero (var x) (var y)))
  (q3-close))
```

Under ordinary Q, the same formula is proved as `Q1 and ... and Q7 -> Q3`, so
Q3 is available as an assumption. Under the profile, Q3 is a trusted
Robinson-Q case-split rule of the selected proof system.

## Correctness And Performance

Focused test:

```text
lein test-proflog-robinson-q
Ran 6 tests containing 48 assertions.
0 failures, 0 errors.
wall 8.89 s
```

Comparison probe:

| Formula | Ordinary Q fuel | Ordinary elapsed | Profile fuel | Profile elapsed |
|---|---:|---:|---:|---:|
| `Q3` | 32 | `8.527 ms` | 32 | `2.278 ms` |
| `Q7` | 32 | `2.931 ms` | 16 | `1.631 ms` |
| `add(1, zero) = 1` | 48 | `2.457 ms` | 16 | `2.394 ms` |
| `mul(2, zero) = zero` | 48 | `2.732 ms` | 16 | `0.776 ms` |
| `add(1, 2) = 3` | 64 | `2.469 ms` | 16 | `0.716 ms` |
| `mul(2, 2) = 4` | 96 | `3.089 ms` | 16 | `1.074 ms` |

The elapsed values above are the in-process row timings printed by
`lein probe-proflog-robinson-q`; the full Leiningen process took `wall 7.83 s`.

## Shortcomings

The `:robinson-q` profile is a trusted conversion layer, not a derivation of Q
from weaker arithmetic principles. Proof records make that explicit by wrapping
the kernel proof in `profiled robinson-q` and listing `q-rewrite` or
`q3-case-split` steps.

Q3 is included only as the exact predecessor-or-zero branch closure needed to
prove Q3 itself. It is not a general predecessor-synthesis engine, and it is not
a rewrite rule.

The initial conversion happens before ordinary kernel proof search. It handles
visible Q arithmetic terms in formulas, including terms under quantifiers, but
it is not yet a branch-local congruence engine that renormalizes after every
later equality substitution.
