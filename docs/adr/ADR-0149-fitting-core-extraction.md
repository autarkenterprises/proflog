# ADR-0149: Fitting-Core Extraction — Reference Tableau and Repaired Free-Variable Engine

- Status: proposed
- Date: 2026-11-07
- Branch: `adr-0149-fitting-core-extraction`
- Base: `origin/main` `f1d6ed0` (its `src/` is byte-identical to `849fd55`,
  the commit every probe below was run against)
- AAR: pending

## Context

The repository began as an implementation of Melvin Fitting, *Tableaus for
Logic Programming* (`LPTableaus.pdf`, 1993; "Proflog"). It has since grown to
roughly 36K lines of `src/`, of which the Fitting core is a minority. The
rest is SJAS work (about 17K lines), theory/profile sidecars (about 3.5K), an
open-query answer layer (about 4.2K), demos, probes, and a legacy `cljtap.*`
tree. Much of it was written by earlier model generations. The goal of this ADR
is an implementation of **only and exactly** Fitting's proposal.

### Spec boundary

| Paper | Content | Status in the extraction |
|---|---|---|
| §2 | Language L with `=`; formulas over ∧ ∨ ¬ ⊃ ∀ ∃ true false; one clause `R(x̄) ← φ(x̄)` per relation; a query is a ground literal of L | implemented |
| §3 | Weak Herbrand models, supervaluations, `Φ_P`, least fixed point `s_P` | **specification of expected values**, not engine code (see "Role of §3") |
| §4 | Tables 1–2: ¬¬, ¬true, ¬false, α, β, γ (any closed term of Lpar), δ (new parameter); closure on `false` or `A`/`¬A` | implemented |
| §5 | Reflexivity, Substitutivity, Free Closure (three cases, symbols of L only), One-One | implemented |
| §6 | Procedure Call Rule (positive and negative, atoms of L only); Def 6.1 succeeds/fails | implemented |
| §7 | `t_P = s_P`; systematic (fair) construction | the reference layer's search policy; the engine's completeness target |
| §8 | Free variables + unification, run-time Skolemization, keep call bindings in L, disunification for Free Closure, factored `move` non-example | the engine layer |

### Role of §3

`s_P` quantifies over every weak Herbrand model, including non-standard
domains (Def 3.1, 3.4). It is not a decision procedure: by Thm 7.2 the true and
false sets are only semi-decidable and ⊥ is undecidable. §3 therefore enters
the code base as the source of each test's expected value. Every acceptance
test states its `s_P` value and the §3 argument for it (a model that satisfies
or falsifies the body, or the reason no such model exists) in a comment. Small
fragments may also get mechanical oracles, as `test/proflog/oracle/herbrand_test.clj`
already does for ground equality.

### Defects in the current generic kernel (probe-confirmed on `849fd55`)

Probes are under
[`docs/log/2026-11-07-fitting-core-probes/`](../log/2026-11-07-fitting-core-probes/)
and run from a `main` checkout with
`lein run -m clojure.main <probe>.clj`. "Generic kernel" means
`proflog.kernel/profiled-closeo` was replaced by `fail`, so only the ordinary
rule clauses ran.

| # | Defect | Probe input | Fitting value | `main` result |
|---|---|---|---|---|
| D1 | **δ unsound.** δ binds a bare fresh `(par nom)` that does not depend on the γ-variables already on the branch, so a later unification can bind an earlier γ-variable to it. | `∀x∃y(y≠x)` as root (generic kernel and full `prove-program`) | satisfiable: no closed tableau | closes |
| | | `rr(x)←∃y¬(y=x)`, `t(z)←∀w rr(w)`; `query-status t(0)` | ⊥ | `:fails` |
| D2 | **β unsound.** The equality substitution σ is threaded from the left β branch into the right, so branch-local parameter bindings (Substitutivity facts) leak into the sibling. | `∃y((y=0∧0=s(0))∨y≠0)` as root | satisfiable: no closed tableau | closes |
| | | `r(x)←∃y((y=x∧x=s(0))∨¬(y=x))`; `query-fails r(0)` | `r(0)` true | returns a disproof |
| D3 | **γ incomplete under negative calls.** `normalize/negate-formula` turns every ¬∃ into `once-forall`, which is instantiated once and never re-enqueued. | `a(z)←a(z)`, `r(x)←∃y(¬a(y)∨a(s(y)))`, `r3(x)←∀y(a(y)∧¬a(s(y)))` | `r(0)` true, `r3(0)` false | `r(0)` `:unresolved`; `r3(0)` `:fails` — the **same** subsidiary formula `∀y(a(y)∧¬a(s(y)))` closes through the positive call and not through the negative one |
| D4 | **No `false` rule** in the generic kernel; only the first-order sidecar supplies it. | root formula `false` (generic kernel) | closed | no proof |
| D5 | **Closed-term γ switched off for any program with a call** (`gamma/program-call-free?`, `gamma.clj:133-149`), so §8 disunification-style Free Closure is absent, and answers depend on **unrelated** clauses. | `q0(w)←∀u(u=0)` alone; `q0(0)` | false (`u:=s(0)`) | `:fails` |
| | | same, plus unrelated `p(x)←false`, `q(z)←p(z)` | false | `:unresolved` |

The first-order and equality-fragment sidecars close the D1 formulas as well,
so removing them removes further unsound paths, not only code. The 2026-06
fidelity audit (`docs/FITTING_FIDELITY_AUDIT.md`) left exactly these points
open ("δ freshness ⏳ to re-confirm"; first-order soundness "not differentially
tested").

### Why `once-forall` is removed, not kept

`once-forall` was introduced in `6373687` (2026-03-04) with this rationale
(DESIGN.md, "AV4"): *"a single LVar instantiation always suffices because the
β-rule shares the substitution map … If the two branches require different
bindings, neither `once-forall` nor genuine `forall` can close (it's a
fundamental tableau limitation)."* The second sentence is false. Re-applying γ
creates a second, independent variable; that is what γ reuse is for. Fitting
lists `¬(∃x)Φ` as a γ sentence in Table 1 alongside `(∀x)Φ`, and Table 2's
rules are branch *extension* rules: the γ sentence stays on the branch and may
be instantiated with any number of closed terms. §6 puts `¬φ(t̄)` on a
subsidiary tableau where all §4 rules apply; nothing in §6 distinguishes
procedure-call bodies.

`once-forall` is sound (each use is a legitimate γ step) but incomplete. D3
shows the incompleteness with a paired probe. ADR-0024 and ADR-0025 had already
found single use wrong for theorem proving and re-enabled re-enqueueing only
inside the first-order sidecar, on the unstated assumption that procedure-call
bodies differ. They do not. §8 does allow "an incomplete but efficient
mechanism … provided the sources of the incompleteness are sufficiently well
understood". `once-forall` was never offered as such a compromise; it was
offered as complete. Any future search compromise must be named, documented,
and measured against the reference layer.

## Decision

### 1. Location: a self-contained subproject on this branch

The extraction lives in a new Leiningen project at `fitting/` on branch
`adr-0149-fitting-core-extraction`, with namespaces `proflog.fitting.*`.

- The root `project.clj` puts `vendor/core.logic-1.0.1/src` ahead of the Maven
  jar, so stock core.logic cannot be used inside the root project. Every vendor
  patch on `main` was motivated by SJAS work. `fitting/project.clj` depends on
  `org.clojure/clojure 1.11.1` and Maven `org.clojure/core.logic 1.0.1` only,
  with no `-Xss256m`.
- Physical isolation makes "only and exactly" checkable: SJAS namespaces are
  not on the subproject's classpath.
- The root project is not modified apart from documentation, so the branch can
  merge into `main` without regressing anything.

### 2. Layer 1 — reference tableau (§2, §4–§7, literally)

Plain Clojure, deliberately **not** core.logic, so that it is independent of
both the engine and core.logic.

- `proflog.fitting.syntax`: language L, terms, parameters, formulas with ¬, ⊃,
  `true`, `false` kept as written (no NNF); programs with at most one clause per
  relation; queries as ground literals of L.
- `proflog.fitting.tableau`: the certificate format and `check`. A certificate
  is a tableau tree whose nodes carry the formula, the rule used, and
  references to the premise nodes above it on the same branch. `check`
  accepts iff every node is the root or follows from earlier nodes on its
  branch by one rule of Table 2 or §5, every δ parameter is new to its branch,
  and every leaf branch is closed by `false`, `A`/`¬A`, a Free Closure case on
  symbols of L, or a Procedure Call whose atom is of L and whose nested
  certificate closes `φ(t̄)` or `¬φ(t̄)`.
- `proflog.fitting.reference`: a fair, fuel-bounded systematic prover in the
  §7 style (γ over closed terms of Lpar in a fair enumeration) that returns a
  certificate or nothing. It is slow by design.

### 3. Layer 2 — free-variable engine (§8), core.logic

Extracted from the keep-list in Appendix A, with these changes:

- **D1:** run-time Skolemization. A δ step binds `(sk i v₁ … vₙ)`, where `i`
  is unique to that δ application and `v₁ … vₙ` are the free variables
  introduced on the branch so far (tracked explicitly in branch state). The
  occurs check then blocks binding an earlier variable to its own witness.
  Skolem terms are outside L: they cannot be called on and do not take part
  in Free Closure.
- **D2:** split state. γ-variable bindings form one rigid substitution shared
  by the whole tableau and threaded through β, as in the guided checker pinned
  by ADR-0147. Branch-local equality facts, meaning parameter and Skolem bindings
  from Substitutivity plus the disequality store, are restored to the parent's
  value at each β split.
- **D3:** delete `once-forall`. ¬∃ normalizes to ordinary ∀, re-enqueued under
  the fair agenda.
- **D4:** `false` closes a branch and `true` is discharged.
- **D5:** remove the program-wide call-free gate. Free Closure on a positive
  equality with free variables follows §8: it closes when the two sides can be
  made to differ on a function symbol of L. This is recorded as a global
  constraint and discharged when the certificate is grounded (see 4). The
  mechanism is settled during implementation; the acceptance tests fix its
  semantics, including a falsifier for naive infinite-domain `!=`.
- **Free-variable calls (§8):** an atom with free variables may be called
  only if its final grounding lies in L (no parameters or Skolem terms).
  Grounding enforces this, and the certificate checker detects any violation.

### 4. Proof-carrying answers

The engine returns certificates, not bare statuses. After a closure, the final
rigid substitution is applied. Each distinct ground Skolem term becomes a fresh
parameter, and each unbound variable becomes a closed L-term consistent with
the D5 constraints. The result is emitted as a Layer 1 certificate.

- `query-status` returns `:succeeds` or `:fails` **only if `tableau/check`
  accepts the certificate**. A rejected certificate is reported as an engine
  defect, never as an answer. The search is not trusted; the checker is.
- ⊥ has no certificate. `:unresolved` means no certificate was found within
  fuel.
- Both succeeding and failing is impossible by Lemma 7.5 and Thm 7.2. The API
  throws instead of returning the current `:inconsistent` status.
- The current proof terms are bare tag trees (ADR-0117) that can only be
  checked by re-running the kernel. They are not carried over.

### 5. Scope: excluded

SJAS (all `willard-sjas*`, `sjas-*`, Robinson Q); profile and sidecar layers
(`proof-profile`, `formula-profile`, `kernel.first-order`, equality-fragment,
constructor-recursive, tabling, guarded alternatives); `answers` and
`answer-overlay` (open free-variable queries, which go beyond Def 6.1 and may
be a later layer); demos and probes; the vendored core.logic; `cljtap.*`.
The `frontend` macros are optional sugar and are deferred.

### 6. The root kernel is not repaired here

D1–D5 remain in the root `proflog.kernel`. Repairing it would change behaviour
SJAS work depends on and needs its own ADR. This ADR records them as known
debt.

## Consequences

- There are two implementations of one specification. That is the point:
  Layer 1 is the oracle and certificate checker for Layer 2, and its own
  correctness is reviewable rule by rule against Tables 1–2 and §5–§6.
- Removing `once-forall` and the call-free γ gate will make some searches
  slower. Fuel turns this into `:unresolved`, never into a wrong answer.
- Rejected alternatives:
  - *Trim the root project in place.* Rejected because it breaks SJAS on the
    branch and cannot drop the vendored core.logic.
  - *New repository.* Rejected at the owner's direction (branch + ADR).
  - *Keep `once-forall` as a §8 compromise.* Rejected; see Context.
  - *Engine only, no reference.* Rejected because the existing audit shows that
    assertions of fidelity without an independent oracle let D1–D5 through.

## Test Obligations

Red first, in this order. Each test states its `s_P` value and the §3 argument.

1. Subproject skeleton: a namespace-closure test asserting that the transitive
   requires of `proflog.fitting.*` are within `clojure.*`, `clojure.core.logic*`,
   and `proflog.fitting.*`.
2. Checker, positive: hand-written certificates for paper examples are
   accepted.
3. Checker, falsifiers (each must be rejected):
   - a δ parameter already on the branch;
   - a call on an atom containing a parameter;
   - a γ instance that is not a closed term of Lpar;
   - an open leaf;
   - a wrong α or β component;
   - Free Closure applied to `p = c` with `p` a parameter.
4. Reference prover: the values stated in the paper for P1 and P2, and the §8
   factored-`move` non-example.
5. Engine, copied unchanged from Appendix A: the D1–D5 table above, reproduced
   red. Required results:
   - D1: no closure; `t(0)` is not `:fails`.
   - D2: no closure; `r(0)` is `:succeeds`.
   - D3: `r(0)` is `:succeeds`; `r3(0)` is `:fails` (control).
   - D4: `false` closes; `r(x)←false` gives `:fails`; `r2(x)←true` gives
     `:succeeds`.
   - D5: `q0(0)` is `:fails` with and without the unrelated clauses.
   - **D5 falsifier:** with L = {0} and no function symbols, `q0(w)←∀u(u=0)`
     gives a ⊥ value for `q0(0)`. The model `{0}` satisfies the body, and a
     model with one extra element falsifies it. The result must not be
     `:fails`; naive core.logic `!=` disunification, which assumes an infinite
     domain, would return `:fails`.
6. Differential: engine against reference on the test corpus.

## Exit Criteria

- All obligations above are green in both layers, with the reference within
  its fuel budget.
- Every definite engine answer in the suite carries a certificate that
  `tableau/check` accepts.
- Engine and reference never give opposite definite answers on the corpus.
- The namespace-closure test passes.
- An AAR records line counts, performance relative to the root kernel on
  shared cases, and any named §8 compromises.

Halting conditions:

- If the engine cannot pass the soundness obligations (D1, D2, the D5
  falsifier), it is not shipped. Layer 1 alone is the deliverable, and the AAR
  records why.
- The checker is never weakened to accept engine output.

## Appendix A — keep / trim map from root `src/` (`849fd55`)

| Root namespace | Lines | Fitting role | Trim |
|---|---|---|---|
| `ast` | 304 | §2 terms, formulas | drop bounded quantifiers and `once-forall` |
| `language` | 474 | §2 language, validation, clause grouping (multiple surface clauses are combined into one disjunctive body, recovering one clause per relation) | drop guarded-alternative IR (`language.clj:251-377`) |
| `normalize` | 126 | NNF, ¬¬/¬true/¬false | drop SJAS bounded-quantifier lowering; ¬∃ becomes ∀ |
| `subst` | 288 | substitution | drop `acyclic-unifyo` (occurs check off, SJAS) |
| `program` | 166 | §6 lookup and argument binding | keep `call-clauseo` and its helpers only |
| `equality` | 532 | §5 Free Closure, One-One, occurs check, disequality store | keep |
| `kernel_support` | 445 | complementary closure, "atom of L" guard, fair selection, fuel | drop `call-free-formulao` |
| `gamma` | 325 | closed-term enumeration | drop the `program-call-free?` gate |
| `kernel` | 1480 | §4–§6 rules | drop sidecar handoff (`:141-370`), guarded alternatives (`:477-790`), `neg-call-alt`, theory and tabling hooks, `acyclic-unifyo`, profile dispatch in `prove`/`prove-program` |
| `query` | 123 | Def 6.1 status | call the kernel directly (not `proof-profile`, which requires `willard-sjas-profile`) |
| `fitting_programs` | 446 | P1, P2, factored-`move` | keep those three only |

Candidate tests to port, trimmed of non-core requires: `ast`, `language`,
`normalize`, `subst`, `equality`, `program`, `kernel`, `gamma`,
`closed-term-gamma`, `adversarial`, `oracle/herbrand`, `fitting-fidelity`,
`fitting-programs`, `query`, `proof-quorum`, `literature-tableau-golden`,
`pelletier`. Ported tests that encode `once-forall` behaviour are rewritten,
not kept.
