# Robinson Q Proof Profile Example

This example documents `test/proflog/robinson_q_test.clj`, ADR-0048 through
ADR-0052. It shows Robinson arithmetic Q in two forms:

- ordinary first-order assumptions: `Q1 and ... and Q7 -> theorem`;
- an opt-in proof profile: Robinson-Q theory rules are bound into the ordinary
  tableau kernel and may close a branch when proof search reaches a relevant
  disequality.

Run the focused regression:

```text
lein test-proflog-robinson-q
```

Current result:

```text
Ran 12 tests containing 88 assertions.
0 failures, 0 errors.
real 22.24 s
```

Run the timing comparison:

```text
lein probe-proflog-robinson-q
```

The comparison probe passed in `real 11.37 s` on 2026-05-09.

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
`proflog.proof-profile`. The `:robinson-q` method binds
`proflog.kernel/*theory-profile-closeo*` to
`proflog.kernel.robinson-q-profile/robinson-q-theory-closeo`, then calls the
normal kernel relation. That hook is tried inside `kernel/close-agendao` next
to the other branch-closing rules.

The Q theory rule uses a miniKanren normal-form relation over terms. It can
close a disequality branch when both sides are equal modulo these conversions:

```text
add(x, zero) -> x
add(x, s(y)) -> s(add(x, y))
mul(x, zero) -> zero
mul(x, s(y)) -> add(mul(x, y), x)
```

For Q7 itself, the profiled proof contains ordinary quantifier steps followed
by an explicit theory closure:

```clojure
(witness
  (witness
    (profiled robinson-q
      (q-convert-close
        (q-normal-mul
          (q-normal-par)
          (q-normal-s (q-normal-par))
          (q-rewrite :mul-succ
            (app mul (par a_0) (app s (par a_1)))
            (app add (app mul (par a_0) (par a_1)) (par a_0)))
          ...)
        ...))))
```

That is the semantic difference. Under ordinary Q, Q7 is an assumption. Under
`:robinson-q`, Q7 is proved by ordinary tableau universal refutation followed
by conversion closure on the exposed disequality.

Q3 is not a conversion. It is a trusted predecessor-equality branch rule. The
direct theorem is proved by closing the negated theorem shape:

```text
exists x. x != zero and once-forall y. x != s(y)
```

The profiled proof shows the ordinary kernel work around the theory step:

```clojure
(witness
  (conj
    (neq-store
      (once-univ
        (profiled robinson-q
          (q3-predecessor-equality
            predecessor-or-zero
            (par a_0)
            (var a_1)
            (par-bind)
            (q-convert-close
              (q-normal-par)
              (q-normal-s (q-normal-var))))))))))
```

Under ordinary Q, the same formula is proved as `Q1 and ... and Q7 -> Q3`, so
Q3 is available as an assumption. Under the profile, Q3 is a trusted
Robinson-Q theory rule of the selected proof system. The important point is
that the Q3 equality fires only after `witness`, `neq-store`, and `once-univ`
have exposed the nonzero premise and the active universal proof variable.

The same proof tag also handles larger refutations. The theorem:

```text
forall x. x != zero -> exists y. add(y, s(zero)) = x
```

negates to:

```text
exists x. x != zero and once-forall y. add(y, s(zero)) != x
```

The profile now closes that branch by storing `x != zero`, instantiating the
single-use universal with a proof variable, normalizing `add(y, s(zero))` to
`s(y)`, and using Q3 to choose that proof variable as `x`'s predecessor:

```clojure
(witness
  (conj
    (neq-store
      (once-univ
        (profiled robinson-q
          (q3-predecessor-equality
            predecessor-or-zero
            (par a_0)
            (var a_1)
            (par-bind)
            (q-convert-close
              (q-normal-add
                (q-normal-var)
                (q-normal-s (q-normal-zero))
                (q-rewrite :add-succ ...)
                (q-normal-s
                  (q-normal-add
                    (q-normal-var)
                    (q-normal-zero)
                    (q-rewrite :add-zero ...))))
              (q-normal-par))))))))
```

ADR-0052 adds the contextual theorem that exposed the final gap in the
incremental implementation:

```text
forall x. x != zero -> exists y. s(add(y, s(zero))) = s(x)
```

Its profiled proof uses the same Q3 marker. The difference is that Q3 supplies
`x = s(y)` under an outer successor context, so Q conversion must expose
`s(s(y))` on the left and `s(x)` on the right:

```clojure
(q3-predecessor-equality
  predecessor-or-zero
  (par a_0)
  (var a_1)
  (par-bind)
  (q-convert-close
    (q-normal-s
      (q-normal-add
        (q-normal-var)
        (q-normal-s (q-normal-zero))
        (q-rewrite :add-succ ...)
        (q-normal-s
          (q-normal-add
            (q-normal-var)
            (q-normal-zero)
            (q-rewrite :add-zero ...)))))
    (q-normal-s (q-normal-par))))
```

There is now one current Q3 proof marker. Direct Q3, add-one Q3, and contextual
Q3 all use `q3-predecessor-equality`.

## Kernel Rule Shape

The theory hook receives the same branch-local state as ordinary close rules:

- `fml`: the selected formula currently being expanded or closed;
- `unexpanded`: the rest of the branch agenda;
- `lits`: saved positive and negative atoms;
- `env`: bound-variable substitution introduced by quantifier rules;
- `proof-vars`: gamma-introduced proof variables;
- `sigma` / `sigma-out`: incoming and outgoing equality substitution;
- `neqs` / `neqs-out`: incoming and outgoing delayed disequalities;
- `prog`: the compiled program, here empty except for language metadata;
- `gamma-terms`: bounded closed-term candidates for gamma rules;
- `fuel`: bounded search fuel;
- `proof`: the proof term produced by the branch rule.

ADR-0050 deliberately keeps the Q normalizer directional from a known branch
term to a normal form. It is relational proof machinery, not a host-side
formula transformer, but it is not a general reverse arithmetic synthesizer.

## Correctness And Performance

Focused test:

```text
lein test-proflog-robinson-q
Ran 12 tests containing 88 assertions.
0 failures, 0 errors.
real 22.24 s
```

Comparison probe:

| Formula | Ordinary Q fuel | Ordinary elapsed | Profile fuel | Profile elapsed |
|---|---:|---:|---:|---:|
| `Q3` | 32 | `8.389 ms` | 32 | `1957.861 ms` |
| `Q7` | 32 | `3.704 ms` | 16 | `297.713 ms` |
| `add(1, zero) = 1` | 48 | `2.491 ms` | 16 | `11.719 ms` |
| `mul(2, zero) = zero` | 48 | `3.396 ms` | 16 | `11.465 ms` |
| `add(1, 2) = 3` | 64 | `2.256 ms` | 16 | `46.196 ms` |
| `mul(2, 2) = 4` | 96 | `3.221 ms` | 16 | `232.891 ms` |
| `forall x. x != zero -> exists y. add(y, s(zero)) = x` | 64 | `2.266 ms` | 48 | `545.513 ms` |
| `forall x. x != zero -> exists y. s(add(y, s(zero))) = s(x)` | 16 | `2.035 ms` | 16 | `762.420 ms` |

The elapsed values above are the in-process row timings printed by
`lein probe-proflog-robinson-q`; the full Leiningen process took `real 11.37 s`.

## Shortcomings

The `:robinson-q` profile is a trusted theory layer, not a derivation of Q from
weaker arithmetic principles. Proof records make that explicit by wrapping the
branch closure in `profiled robinson-q` and listing `q-rewrite` or
`q3-predecessor-equality` evidence.

Q3 is still not a rewrite rule. The unified Q3 rule is relevance controlled: it
requires a saved nonzero premise, an active proof-local universal variable, and
an active disequality that becomes reflexive after temporarily applying the Q3
equality and Q conversion. It does not introduce arbitrary predecessors without
a branch obligation that can immediately use them.

The ADR-0050 profile is slower than the old host preprocessor because
conversion now participates in kernel proof search. That cost is intentional for
the current demonstration: the tests assert that ordinary kernel steps and Q
theory steps are interleaved in one proof object.

The Q normalizer is still intentionally narrow. It rewrites known branch terms
over `zero`, `s`, `add`, and `mul`, and leaves symbolic right arguments neutral
when no Q root rule applies. It is not yet a full congruence-closure or
deduction-modulo framework for arbitrary user theories.

The profile intentionally does not prove:

```text
forall x. x != zero -> exists y. x = s(s(y))
```

That formula is false in the standard model because `s(zero)` is nonzero but
has no double predecessor. The regression suite keeps this as a focused guard
against using Q3 as unrestricted successor unification.
