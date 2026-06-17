# Fitting-Fidelity Audit of the Proflog Core

**Purpose.** The greenfield core under `src/proflog/` is an implementation of *Proflog* —
Melvin Fitting's logic-programming language whose operational semantics is the semantic-tableau
proof procedure (Fitting, *Tableaus for Logic Programming*, 1993; repo copy `LPTableaus.pdf`).
The SJAS/Willard layer that sits on top has been checked against *Willard's D*, but until now no
artifact systematically anchored the core back to **Fitting's own specification**. This document
is that anchor: a clause-by-clause map of Fitting §2–§8 onto the implementation.

**Method.** Every verdict is backed by **evidence** — a re-verified `file:line` and a passing (or,
for a divergence, a referenced failing) test — never an asserted "looks compliant." This
discipline exists because an early automated pass over-asserted compliance and hallucinated kernel
line numbers; anchors here were re-confirmed against the source.

**Scope.** The proof procedure + LP layer and `answer_overlay.clj`. Excluded: the `*_probe.clj`
experiments and the SJAS layer (consumers of this core).

**Companion tests:** `test/proflog/fitting_fidelity_test.clj` (gap-filling interrogations, in the fast
gate) and `test/proflog/proof_quorum_test.clj` (Phase 2b quorum, in the extended gate); plus the
pre-existing `test/proflog/equality_test.clj` and `test/proflog/fitting_programs_test.clj`, which
already establish large parts of §2/§5/§8.

**Status (2026-06-17).** §2–§8 fully dispositioned; the two headline results have ADRs — §6
free-variable calls ([ADR-0116](adr/ADR-0116-fitting-free-variable-procedure-call.md)) and quorum
proof-checking + proof-term adequacy ([ADR-0117](adr/ADR-0117-quorum-proof-checking.md)) — and the
secondary findings are dispositioned in [ADR-0118](adr/ADR-0118-fitting-audit-secondary-findings.md).
Interrogation + quorum suites are green and gated. Named deferrals: precise `⊥`, a first-order/equality
differential soundness test, first-order γ-envelope quantification, a line-level `answer_overlay` diff,
and reconciliation with `origin/main` (ADR-0112/0113/0114).

### Verdict legend
- ✅ **faithful** — realizes Fitting's element directly.
- ⚖️ **representational-divergence (justified)** — different mechanism, same meaning; argued + tested.
- ➕ **extension-beyond-Fitting** — goes past the paper (usually into Fitting's own §8 "future" space).
- ⏳ **pending** — interrogation not yet written / anchor not yet line-verified.
- ❓ **open question** — a genuine fidelity question to resolve (may become an ADR).

---

## §2 Syntax & Programs

| Spec element (Fitting ref) | Impl anchor | Verdict | Evidence |
|---|---|---|---|
| Language L = constants/functions/relations; equality always a relation (p.2) | `language/language` | ✅ | `fitting/peano-language` (`fitting_programs.clj:53`) |
| L-clause `R(x̄) ← φ(x̄)`; **≤ 1 clause per relation, except equality** (Def 2.1) | `compile-program` (`language.clj:438`); `clause-group->core-clause` (`:378`) | ⚖️ | multiple *surface* clauses are sugar; compilation disjoins them into ONE defining formula per relation (`:clauses` keyed one-per-relation), explicitly recovering Def 2.1. "Guarded alternatives" are disjuncts of that one body. Evidence: `sec2-multiple-surface-clauses-compile-to-one-defining-formula` |
| Parameters cannot appear in query answers (p.2) | `l-ground` filter on answer export (`answers.clj`, `answer_overlay.clj`) | ⏳ | guard exists (`l-ground-term*o`); dedicated answer-export interrogation PENDING |
| Fixed L; a program may use a subset (avoids "irrelevant axioms") | `language/validate-query` (`query.clj:49,62`) | ✅ | queries validated against `(:language program)` |
| Query = any ground literal of L (p.3) | `query/query-succeeds`/`-fails`/`-status` | ✅ | `query.clj`; `fitting_programs_test.clj` |
| Worked program **P₁** even/odd, incl. `odd(x) ← ∀y[even(y)⊃¬(x=y)]` | `fitting/p1-program` (`fitting_programs.clj:73`) | ✅ | verbatim clause; `fitting_programs_test.clj` (`p1-even-0`, `p1-odd-1`, `p1-odd-0-fails`) |
| Worked program **P₂** `win(x) ← ∃y[(x=s(y)∨x=s(s(y)))∧¬win(y)]` | `fitting/p2-program` (`fitting_programs.clj:97`) | ✅ | `win(4)` succeeds, `win(3)` fails per Fitting's pp.8–9 tableau |

## §3 Semantics (supervaluation / weak Herbrand)

| Spec element | Impl anchor | Verdict | Evidence |
|---|---|---|---|
| Computes operational valuation t_P (Def 7.1), **not** Φ_P / s_P | `query.clj` two-direction search; no fixpoint operator in core | ✅ by-design | `query.clj` docstring; Theorem 7.2 is the (Fitting) bridge, not computed |
| Weak Herbrand equality: identity-only, injective, disjoint ranges (Def 3.1) | `equality.clj` free-constructor engine | ✅ | see §5 |
| `s_∅` occurs subtlety: ground `t=f(t)` **false**, but `(∃x)(x=f(x))` is **⊥** (p.6) | gamma-var occurs ⇒ `(occurs-close)` (`equality.clj:350`); delta-param occurs ⇒ `absent-paro` fails *and* no clash ⇒ open | ✅ | `fitting_fidelity_test/sec3-occurs-ground-and-variable-close-but-existential-stays-open` |
| `(∀x)(even∨¬even)` true; `(∀x)(even∨odd)` ⊥ under P₁ (p.6) | propositional closure vs non-ground call non-firing | ✅ | `sec3-p1-classical-tautology-succeeds`, `sec3-p1-even-or-odd-is-undefined` (`:unresolved`) |
| Three-valued **⊥** vs the impl's **`:unresolved`** (budget-exhausted) | `query/query-status` | ⚖️❓ | `:unresolved` *over-approximates* ⊥ (correct for the tested cases); the general reporting boundary is a documented soundness obligation — candidate ADR |

## §4 Tableau rules (α/β/γ/δ, ¬¬/¬true/¬false)

| Spec element (Table 1/2) | Impl anchor | Verdict | Evidence |
|---|---|---|---|
| α (X∧Y) | `kernel.clj:822` | ✅ | proof tag `conj` |
| β (X∨Y) | `kernel.clj:846` | ✅ | proof tag `split` |
| δ (∃ → **new** parameter p) | `kernel.clj:1017` | ✅ (freshness ⏳ to re-confirm) | proof tag `witness`; `(par …)` fresh nom |
| γ (∀ → **any** closed term of L^par) | `kernel.clj:885`; `gamma.clj` candidate enumeration; fair re-enqueue (ADR-0016) | ➕❓ | bounded fresh-var + closed-term enumeration; completeness **envelope** is a §8 compromise — candidate ADR (ties §8) |
| ¬¬Z→Z, ¬true→false, ¬false→true, and the α/β/γ/δ negation duals | `normalize/negate-formula` + `to-nnf` (`normalize.clj:37,86`) | ✅ | `sec4-nnf-realizes-fitting-negation-duals` (incl. ¬∃→`once-forall`, ¬¬→Z) |
| `once-forall` = the γ from a negated clause-body ∃ (single-use, no re-enqueue) | `normalize.clj:63`; `kernel.clj:966` | ✅ | `normalize` `exists`→`once-forall-form`; comment + `sec4` test |

## §5 Equality rules

| Spec element | Impl anchor | Verdict | Evidence |
|---|---|---|---|
| **Reflexivity** (t=t) | `unify-termo` `eq-refl` branch (`equality.clj:431`) | ✅ | `equality_test` |
| **Substitutivity** (left-right; right-left derivable) | explicit `sigma` substitution + `walko`/`unify-termo` rather than rule applications | ⚖️ | unification-under-σ realizes substitutivity closure; `equality_test/equality-bindings-compose-through-transitive-chains`, `…-atom-congruence-on-the-branch` |
| **Free Closure**: `c=d`; `f(…)=c`; `f(…)=g(…)` (all 3 clauses) | `eq-contradictiono` distinct-head + arity-mismatch (`equality.clj:358,373`), tag `(free-close)` | ✅ | the 3 clauses collapse to distinct-constructor-head/arity; `equality_test/free-constructor-clash-closes-equality`, `decomposition-finds-inner-constructor-clashes` |
| **One-One** (`f(t̄)=f(ū) ⊢ tᵢ=uᵢ`) | same-head `decompose` in `unify-termo:459` / `eq-contradiction-term*o:373` | ✅ | `equality_test/injectivity-and-eq-neq-closure-work-together` |
| **Occurs check** asymmetry (var closes, param ⊥) | `absent-termo:197` / `absent-paro:229`; `occurs-close` var-only | ✅ | `equality_test/cyclic-open-…`, `unresolved-parameters-do-not-close-…`; `sec3-occurs-…` |

## §6 Procedure Call Rule

| Spec element | Impl anchor | Verdict | Evidence |
|---|---|---|---|
| Fires only on a **ground atom of L** (pos) / **¬ ground atom of L** (neg) (Def 6.1) | `kernel.clj:1188`/`:1246`; guard `l-ground-termo` (`kernel_support.clj:313`) | ➕ extension | **core admits free `(var …)`**, rejecting only `(par …)` — broader than §6's *ground* atoms. This is Fitting's §8 free-variable (Prolog-style) call, with `l-ground` as his §8 "keep-the-unifier-in-L" mechanism. Evidence: `sec6-l-ground-guard-admits-variables-but-rejects-parameters` → **[ADR-0116](adr/ADR-0116-fitting-free-variable-procedure-call.md)** |
| Atom must be of L, not L^par | `l-ground-term*o` | ✅ | rejects parameters; indirectly evidenced by `sec3-p1-even-or-odd-is-undefined` (non-ground call does not fire) |
| Negative call uses closed tableau for ¬φ(t̄) (via NNF + `once-forall`) | `normalize/negate-formula`; `kernel.clj:1246` | ✅ | `fitting_programs_test` `win(3)` fails (Fitting pp.8–9) |

## §7 Soundness & Completeness

| Spec element | Impl anchor | Verdict | Evidence |
|---|---|---|---|
| Soundness: never close a v-satisfiable branch (Lemmas 7.4/7.5) | whole kernel | ✅ propositional / ⏳ FO | `sec7-query-status-is-never-inconsistent…`; **`sec7-propositional-validity-matches-truth-tables`** — kernel validity == truth-table tautology over 200 random formulas (no spurious closure, and complete) for the propositional core; first-order/equality soundness remains characterized but not differentially tested |
| Completeness bounded by §8 compromises; classical-FOL engine benchmark | `pelletier_test` (cross-ref) | ✅ (engine) | Pelletier ≠ full t_P-completeness (supervaluational) — documented bound |
| Relation to the project's Willard-D correspondence (ADR-0100, first fragment, not machine-checked) | `docs/log/2026-06-13-sjas-tableau-correspondence-proof.md` | note | this audit supplies the Fitting-side ground truth that correspondence presupposes |

## §8 Implementation boundary (compromises Fitting predicted)

| Spec element | Impl anchor | Verdict | Evidence |
|---|---|---|---|
| `move`-factoring non-example: factored `move` ≠ inlined (non-standard moves) | `fitting/factored-move-program` (`fitting_programs.clj:115`) | ✅ | `fitting_programs_test`: factored `win(1)` `:unresolved`, `:invalid-auxiliary-relation-factoring` |
| γ-choice / keep unifier a term of L, not the Skolem enlargement ("serious complication") | `l-ground` discipline + proof-var handling | ❓ | ties §4 γ; **PENDING** — the deepest correctness question |
| Disunification / disequality ("most intractable issue"): symbolic `neqs` + delayed check | `kernel_support` `neqs`; `equality/neq-violatedo:499` | ⏳ | soundness partly tested (`equality_test` disequality cases); free-variable disunifier completeness PENDING (`existential_disequality_test`, ADR-0018) |

## answer_overlay.clj (Fitting §8 free-variable layer — in depth)

| Concern | Verdict | Evidence |
|---|---|---|
| Residuals / `call-depth` / `existentials-as-vars?` | ✅ inspection | `answer_overlay.clj:46` (`*defer-residual-calls*`), `:100` (saved-call w/ residual export), `:830-869` (δ exports `(var …)` when `existentials-as-vars?`, else `(par …)`) |
| Answer soundness (residuals = unresolved, never proven; no `par` in answers) | ✅ inspection + existing tests | residuals exported as distinctly-tagged obligations (`eq-triggered-residual-call` `:147`), never folded into closures; `answers/admissible-term?`/`ground-no-par?` (`answers.clj:127,540`) keep `par` out of exports; behaviourally covered by `answers_test`/`parity_test` |
| Faithful rule-by-rule mirror of the kernel (no rule weakened) | ⚖️ inspection | overlay rules (`answer_overlay.clj:690-870`) mirror kernel `:885-1045` with added `residuals`/`call-depth` threading and identical proof tags (validated by the ADR-0117 quorum, which accepts P2 `win(4)` certificates). A line-level diff of all ~2.5k LOC is deferred |

## Quorum proof-checking (Phase 2b) — RESOLVED ([ADR-0117](adr/ADR-0117-quorum-proof-checking.md))

| Oracle | Verdict | Evidence |
|---|---|---|
| Kernel-as-prover (search) | ✅ | `kernel/prove`, `query/*` |
| Kernel-as-checker (proof bound, relational) | ✅ | `proof-quorum-test/kernel-accepts?` — accepts genuine, rejects garbage; binding `proof` fixes the rule per node but re-searches witnesses (**check-determinism** finding) |
| Independent non-relational `proof_check.clj` | ✅ | structural grammar validator (`rule-arities`), no shared code / no core.logic |
| Quorum + mutation harness | ✅ | `proof_quorum_test.clj` — **5 tests / 58 assertions**; all oracles accept genuine (incl. P2 `win(4)` guarded-alt), reject garbage + arity-truncated mutants |
| **Proof-term adequacy** | finding | certificates are pure tag trees (no formulas/witnesses) → independent oracle is structural-only; kernel-as-checker supplies semantic re-validation → [ADR-0117](adr/ADR-0117-quorum-proof-checking.md) |

---

## Open interrogations / candidate ADRs (carried forward)
1. **§6 call-firing — RESOLVED.** The *core* fires on variable-bearing (L-ground, non-ground) atoms
   (`l-ground-termo` admits `(var …)`, rejects `(par …)`): an `extension-beyond-Fitting` realizing
   Fitting's §8 free-variable calls. It is **not** a core/overlay split — the extension is in the core.
   → **[ADR-0116](adr/ADR-0116-fitting-free-variable-procedure-call.md)** (headline). Soundness rests on
   the `l-ground` guard + `proof-bindingso`; the completeness envelope is Fitting's §8 disunification caveat.
2. **⊥ vs `:unresolved`** — reporting-soundness boundary (`:unresolved` over-approximates ⊥).
3. **γ-instantiation envelope** — bounded enumeration vs Fitting's "any closed term of L^par"; the
   §8 Skolemization/keep-in-L "serious complication".
4. **Proof-term adequacy — RESOLVED.** Certificates are pure tag trees recording the rule at each node
   but no δ-witnesses / γ-instantiations / unifiers, so they cannot be replayed without re-search; the
   independent checker is structural-only and the kernel-as-checker supplies semantic re-validation.
   → [ADR-0117](adr/ADR-0117-quorum-proof-checking.md).
5. **§2 one-clause-per-relation — RESOLVED.** Multiple surface clauses compile to one disjunctive
   defining formula per relation (`clause-group->core-clause`); Def 2.1 is preserved at the core (⚖️).
6. **answer_overlay rule-mirror** — no kernel rule weakened in the overlay.

## Status of the interrogation suite (this pass)
`lein test proflog.fitting-fidelity-test` → **8 tests, 222 assertions, 0 failures** (verifying §2, §3,
§4, §6, §7 rows; the §7 propositional differential is 200 of those assertions).
`lein test proflog.proof-quorum-test` → **5 tests, 58 assertions, 0 failures** (Phase 2b quorum).
§5 rows rest on the pre-existing `equality_test.clj`; §8 on `fitting_programs_test.clj`; answer-overlay
on `answers_test.clj`/`parity_test.clj`.

## Literature anchors (cross-references)

Fitting's paper situates Proflog against the sources this audit also leans on:

- **R. M. Smullyan, *First-Order Logic* (Springer, 1968)** — Fitting [10]: the uniform-notation
  α/β/γ/δ tableau rules (Tables 1–2) the §4 row maps onto `kernel.clj` + `normalize.clj`.
- **M. Fitting, *First-Order Logic and Automated Theorem Proving* (Springer, 1990)** — Fitting [4]:
  the systematic tableau construction behind §7 completeness, and the free-variable +
  run-time-Skolemization discussion that §8 / [ADR-0116] / [ADR-0118] build on.
- **B. C. van Fraassen, *Singular Terms, Truth-Value Gaps, and Free Logic* (J. Philosophy 63, 1966)**
  — Fitting [11]: the supervaluation semantics whose operational image `t_P` the §3 rows characterize.
- **M. Fitting, Kripke/Kleene (JLP 2, 1985) [2]; Partial models and logic programming (TCS 48, 1987)
  [3]** — the semantic lineage of the smallest supervaluation model `s_P`.

Repo secondary tableau sources for reconciliation: `sjas/lit/tableau-def.pdf`,
`sjas/lit/tableauchapter.pdf`. The project's own correspondence to *Willard's D* (ADR-0100, first
fragment) sits atop this Fitting anchor; `origin/main`'s literature golden suite (ADR-0112) adds
external corpus coverage to reconcile with at audit close.

[ADR-0116]: adr/ADR-0116-fitting-free-variable-procedure-call.md
[ADR-0118]: adr/ADR-0118-fitting-audit-secondary-findings.md
