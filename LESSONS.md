# Lessons

## 2026-04-18

- In the greenfield kernel, positive equality and disequality do not have symmetric operational rules.
- `eq(t, u)` is modeled by occurs-checking unification over the current branch substitution. If the terms can be unified, the equality can extend branch state; if they structurally clash, the branch closes.
- `neq(t, u)` is not "unification fails." It is a constraint saying the terms must remain distinct.
- A disequality literal is only contradictory when the current branch state already forces its two sides equal. If the branch does not yet force equality, the disequality must be stored symbolically and rechecked after later equality bindings.
- Counterexample: `neq(succ(x), succ(a))` is satisfiable because `x` may be any term other than `a`. A `neq` rule that closes by finding a unifier would incorrectly choose `x = a` and treat a satisfiable formula as contradictory.
- Operational summary:
  equality -> unification
  disequality -> symbolic disequality constraint / disunification discipline

## 2026-04-22

- The failed `sorted2([1])` probe exposed an implementation bug in equality contradiction search, not in quantified sortedness itself.
- In [src/proflog/equality.clj](./src/proflog/equality.clj), `eq-contradiction-term*o` must propagate substitutions learned from earlier arguments into later argument comparisons.
- Counterexample: `cons(1, null) = cons(a, cons(b, t))` is contradictory only after first binding `a = 1` and then discovering `null = cons(b, t)`.
- Before the fix, same-head contradiction checking recursed on later arguments under the old substitution, so it missed constructor clashes that became visible only after an earlier `par-bind` or `eq-bind`.
- The consequence was broader than `sorted2`: any proof depending on decomposition plus later contradiction under a refined substitution could stay spuriously open.
- The regression that now locks this down is [test/proflog/equality_test.clj](./test/proflog/equality_test.clj), `decomposition-can-bind-earlier-arguments-before-finding-a-later-clash`.
