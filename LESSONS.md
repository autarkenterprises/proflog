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
- Generic `sorted/1` is naturally first-order in Proflog when written against a fixed comparator relation such as `le/2`.
- A comparator cannot currently be passed as an ordinary argument because the language and procedure-call rule are first-order: relation symbols are resolved from the declared program, while ordinary term arguments are not callable predicates.
- If the frontend ever wants syntax like `sorted_by(le, xs)`, that should compile to a first-order specialized or inlined form rather than pretending the kernel already supports higher-order predicate arguments.
- Compare this boundary explicitly against higher-order miniKanren designs before extending the language: the right question is not just ergonomics, but what semantic and operational commitments Proflog would inherit by admitting predicate-valued arguments or meta-calls.
- A residual like `neq(0, 0)` was not a meaningful exported constraint. It came from a saved disequality that the kernel had failed to prune after a later binding-producing close step forced its two sides equal.
- In answer mode this can happen because requested answer variables are allowed to behave like proof-time variables during `neq-close`, so a branch can bind an answer variable while older saved disequalities still mention it.
- The right invariant is: saved disequalities should remain genuinely open under the current substitution. Binding-producing kernel steps must therefore prune already-false disequalities instead of carrying them forward into `neqs-out`.
- The exporter still keeps a defensive top-level filter for obviously impossible residuals such as `neq(t, t)`, but that is a guardrail, not the main semantic fix.
- This still does not prove that every exported residual set is globally satisfiable. It closes the direct stale-disequality leak, not all possible multi-formula inconsistency patterns.
