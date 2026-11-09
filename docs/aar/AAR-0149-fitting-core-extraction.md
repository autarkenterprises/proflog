# AAR-0149: Fitting's Proflog as One core.logic Relation

- Date: 2026-11-09
- ADR: [ADR-0149](../adr/ADR-0149-fitting-core-extraction.md) (revision 6)
- Branch: `adr-0149-fitting-core-extraction`
- Code: [`fitting/`](../../fitting/README.md)
- Status: Layer 1 (the language) complete; Layer 2 (§8 engine) deferred

## Outcome

The owner's goal (2026-11-09) was to implement only and exactly the Proflog
language that Fitting defines, as a logic programming language using
core.logic, as near to a direct translation of the paper as possible.
`fitting/` delivers §2–§7:

- **The relation.** `proflog.fitting.tableau/tableauo` is one relation in
  core.logic's pure subset. With a fresh certificate it proves; with a ground
  one it checks.
- **Coverage.** It transcribes Table 1 as four `defne` tables, Table 2,
  §5's four equality rules and §6's Procedure Call Rule, arranged as §7's
  systematic construction.
- **The query API.** `proflog.fitting.query` implements Definition 6.1 and
  returns every answer with a closed tableau that the relation has re-checked.
- **The paper's own proof.** The §6 tableau for `win(s(s(s(0))))`, written
  by hand as a certificate from the paper's figure, is accepted as written.
  Prove mode finds exactly that certificate (`query_test.clj`,
  `program-p2`).

| | Lines |
|---|---|
| `syntax.clj` (L, Def 2.1, queries, tables) | 316 |
| `tableau.clj` (the relation) | 828 |
| `query.clj` (Def 6.1, search control) | 182 |
| `programs.clj` (P1, P2, §8 `move`) | 62 |
| tests | 605 |

About half of `tableau.clj` is comments quoting the paper. The root
project's Fitting core, by comparison, was about 4,700 lines spread across
eleven namespaces (ADR-0149, Appendix A).

## Test results

- **Default suite (`lein test`):** 57 tests, 178 assertions, all green, in
  25s (after the review).
  - syntax and Def 2.1;
  - substitution and shadowing;
  - each row of Table 1;
  - the paper's certificate, and a deterministic check of it;
  - check-mode falsifiers, one per rule;
  - P1 and P2 values;
  - the factored `move` program (⊥);
  - the D1–D5 cases against the ground calculus;
  - the binder-capture falsifier with a positive control;
  - the D5 falsifier;
  - namespace closure and purity.
- **`lein test :slow`:** all green in 2m17s, maximum RSS 1.9 GB:
  - `even(s(s(0)))` succeeds and `odd(s(s(0)))` fails;
  - `win(s⁴(0))` succeeds at fuel 20.

## Measurements that shaped the design

All measurements are wall time on the project machine (4-core i5).

1. **The first prove mode was slow.** Exhausting fuel 5 for
   `win(s(s(s(0))))` (which needs 7) took 70s. Most of the work re-derived
   equalities already on the branch: `(= p p)`, `(= sss0 sss0)`, and so on,
   each of which opened a new branch of the search. Two systematic-
   construction restrictions brought fuel 5 down to 0.14s, and the proof to
   4.1s:
   - never add a literal that is already on the branch;
   - never add t = t.

   Both are pure on ground syntax through a structural `differento` built
   from the symbol tables.
2. **Exhaustive search on the losing side.** The impossible side,
   `¬win(s(s(s(0))))`, still took 18.7s at fuel 6, because a failed search
   must be exhaustive. Two more restrictions brought fuel 7 on that side down
   to 0.38s:
   - each γ term is used once per γ sentence on a branch;
   - every closure uses a literal that is new since the last saturation
     point.

   The second also removes most duplicate certificates for a branch. Without
   it, miniKanren re-searches a right β sibling once for each way the left
   sibling closes.
3. **Iterative deepening was the remaining cost.** For `even(s(0))`:

   | Fuel | Search | Time |
   |---|---|---|
   | 4 | exhausted | 7.2s |
   | 5 | exhausted | 127s |
   | 6 | proof found | 47ms |

   For `odd(s(0))`, exhausting fuel 7 took 115s, and fuel 8 then found the
   proof in 175ms. Time to the first proof barely changes once the fuel is
   enough. At fuel 8 to 16, `win(s(s(s(0))))` takes 1.4–2.8s and `even(s(0))`
   about 80ms. The query API therefore:
   - runs each side once at a fuel bound (default 16);
   - races the two sides;
   - cancels the loser by stepping core.logic's stream itself, with the same
     loop as `run` plus an interrupt check.
4. **The remaining cost is the ground γ.** `odd(s(s(0)))` needs γ with
   s(s(0)) followed by the whole `even(s(s(0)))` proof. Each wrong γ term
   leads to a call whose subsidiary search must be exhausted. This is the
   inefficiency Fitting calls "virtually built in". §8's free variables are
   the remedy, and they belong to the engine.

## Deviations from the ADR as proposed

- **Notation.** Variables are bare symbols and clauses are `(<- (R x…) ϕ)`.
  The ADR had proposed `(var x)` and `(R [x…] ϕ)`. This is closer to the
  paper, which the owner's 2026-11-08 criterion favours.
- **Certificate format.** Certificates do not name premise positions, δ
  parameters or rule components, because the systematic construction
  determines them. They do name literals, which carry no binders. Two
  falsifiers were changed to match (see the ADR's test-obligation status).
- **Rejection diagnostics** (`stepo`) were not built.
- **Layer 2,** the §8 engine, is deferred to a follow-up ADR. So are the
  binder-escape falsifier and the engine/reference differential.

## Independent review

A fresh subagent, given only the paper and the code, audited soundness,
completeness, fidelity and tests. It wrote and ran its own probes.

- **Soundness: no defect found.** It checked δ freshness, γ terms, call
  polarity and "atom of L", the Free Closure cases (including the c = f(t̄)
  orientation), One-One, Substitutivity, substitution and shadowing, and
  true/false.
- **Completeness: one real defect, now fixed.** γ applies to the head of the
  γ queue, and each term is used once. With no function symbols in L there
  are finitely many terms, so the head γ sentence could use them all up and
  block every γ sentence behind it. Examples:
  - `(∀x)(∀y)r(x,y) ∧ ¬r(a,a)` with L = {a} never closed;
  - the function-free query `p(a)`, whose s_P value is false, came out
    `:unresolved`.

  Function-free (Datalog-style) programs are common, so this mattered. The
  fix lets γ use a new parameter whenever L has no function symbol. That is
  still "any closed term of Lpar", and it keeps δ fresh. The four
  counterexamples are now tests: they failed first and pass now.
- **Fragile invariant, now fixed.** One-One marked as new even component
  equations it did not add. Now only the added ones are marked, so `new`
  stays a subset of the branch's literals.
- **Fidelity fixes in the documentation:**
  - The checker accepts the systematic construction's normal form, not
    every closed tableau. The README, the namespace docstring and the ADR
    now say so, and that a future engine must emit this normal form.
  - "Oldest γ sentence" is now "head of the queue".
  - The Reflexivity comment now explains why the derivation of symmetry is
    not needed.
  - The docstring of the one-argument `free-variables` is corrected.
  - The One-One fuel cost is stated.
- **Test gaps, now filled** (default suite 45 → 57 tests):
  - δ freshness;
  - call polarity and calls on relations with no clause;
  - One-One on a parameter;
  - Substitutivity without its equation on the branch;
  - Free Closure on equal symbols;
  - the two normal-form rules (γ term reuse, closing on old literals);
  - substitution of free occurrences only when a call is instantiated;
  - 0-ary relations and equality queries;
  - the `:both` option.

  The review also noted that the ⊥ tests over function-free languages (the
  binder-capture and D5 falsifiers) could have come out `:unresolved`
  because of the γ defect. They are now run with the fix in place, and the
  binder-capture test has a positive control.

## Follow-ups

- A follow-up ADR for the §8 free-variable engine, using Layer 1 as its
  certificate checker. It needs its own forms of the search restrictions,
  because those use structural difference on ground terms.
- Optional: rejection diagnostics for hand-written certificates.
