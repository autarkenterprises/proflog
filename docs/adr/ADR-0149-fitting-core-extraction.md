# ADR-0149: Fitting-Core Extraction — Reference Tableau and Repaired Free-Variable Engine

- Status: accepted. Layer 1, the language, was implemented in revision 6
  (2026-11-09). Layer 2, the §8 engine, is deferred.
- Date: 2026-11-07
- Branch: `adr-0149-fitting-core-extraction`
- Base: `origin/main` `f1d6ed0` (its `src/` is byte-identical to `849fd55`,
  the commit every probe below was run against)
- AAR: pending

## Revision note (2026-11-08)

The first draft wrote Layer 1 in plain Clojure with a separate certificate
checker. The owner pointed out that this requires two artifacts, the prover
and the checker, to be correct and to stay in sync. Layer 1 is now a single
core.logic relation with a certificate argument: with the certificate fresh it
proves, and with the certificate ground it checks. The independence that
matters is between Layer 1 and the engine, and that is unchanged.

A second revision the same day restores core.logic's nominal logic to Layer 1.
The first revision had excluded it along with impure operators. It is
relational, and it is the natural tool for binders and for Fitting's "new
parameter". δ parameters are now bound by a tie in the certificate, replacing
the per-branch counter.

A third revision the same day excludes nominal logic again, because it is not
required. The owner's condition was that it could be excluded if that could be
justified. The justification is in Decision §2 (now "Why named variables, not
nominal logic or de Bruijn indices"):
every substitution in Fitting's calculus inserts a closed term, so de Bruijn
indices and a per-branch parameter counter do the same work in plain
`==`/`conde`/`fresh`, and certificates become plain data. The second revision
had weighed nominal logic only against `!=`-on-names, not against de Bruijn
indices.

A fourth revision the same day keeps nominal logic out and replaces de Bruijn
indices with named bound variables. The owner asked whether nominal logic
would bring the code's syntax closer to the paper's, so that each can be read
against the other. It would, compared with de Bruijn indices, but the paper's
own named variables are closer than both. Because every inserted term is
closed, Fitting's definition of substitution (§2) can be transcribed as
written, with no capture-avoidance. The third revision had counted
readability only as a cost to mitigate. See Decision §2, "Why named variables,
not nominal logic or de Bruijn indices".

A fifth revision the same day settles the engine's binder representation on
its merits rather than inheriting it from the root code. It also states what
Fitting requires of free-variable unification: the expected result and the
rules that achieve it. The engine now uses named variables as well. See
Decision §2, "With §8's free variables".

## Revision 6 (2026-11-09): Layer 1 implemented as the language

The owner set the goal: implement only and exactly the Proflog language
Fitting defines, as a logic programming language that uses core.logic to
simplify the implementation and is as near to a direct translation of the
paper as possible. Existing material, this ADR included, is not to be
over-indexed. Layer 1 now exists in `fitting/`. Building it settled the
following points, each decided from the paper. Decision §2,
"Implementation as built", has the details.

1. **The language is §2–§7.** §8 is Fitting's discussion of implementation
   ("Implementation and Other Issues"). Its free-variable engine (Layer 2,
   Decision §3) is not part of the language, so it is deferred to a
   follow-up ADR. The ground calculus runs the paper's examples within
   seconds (AAR-0149).
2. **Notation closer to the paper.**
   - Variables are bare symbols: `(forall x (P x))`, not
     `(forall x (P (var x)))`.
   - Clauses are written `(<- (R x1 … xn) ϕ)`.
   - `substo` is still §2's two cases. The names table now pairs each
     variable with every variable and constant.
3. **The relation is §7's systematic construction.** Branches expand their
   sentences in agenda order. Choices happen only at *saturation points*:
   which closure, which γ term, which equality steps. Certificates name
   every rule application. They contain literals and γ terms but no
   quantified formulas, so they read as the tableau they describe.
4. **Normal-form restrictions that keep completeness.** Each is the
   systematic construction's, not the paper's rules:
   - closure on A/¬A is checked on literals;
   - Substitutivity rewrites literals;
   - Reflexivity is used only to close against ¬(t = t);
   - γ terms are drawn from L's closed terms and the parameters already on
     the branch, and each is used at most once per γ sentence on a branch;
   - no literal is added twice, and no equation t = t is added;
   - every closure uses a literal that is new since the previous saturation
     point.

   Ground terms make every "different" test structural, so the
   restrictions are pure relations over the distinctness tables.
5. **The query API is search control only.** Each side is one prove-mode
   run at a fuel bound, and the two sides run in parallel. The first closed
   tableau wins, the other search is cancelled, and the certificate is
   re-checked in check mode. Iterative deepening was measured and rejected
   (see the AAR). An insufficient fuel level must be exhausted, which can
   take a hundred times longer than finding the proof one level up.
6. The {d, e} model in the binder example is now justified from §3 (Def
   3.1, 3.3–3.5, 3.7), as the owner asked on 2026-11-08.

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

### 2. Layer 1 — the language (§2, §4–§7, literally)

One core.logic relation, `proflog.fitting.tableau/tableauo`, transcribes
Tables 1–2, §5 and §6 over ground sentences of Lpar, arranged as §7's
systematic construction. Formulas keep ¬, ⊃, `true` and `false` as written,
with no NNF, so the rules are Fitting's and not those of a normal-form
variant. Following leanTAP and αleanTAP, the relation has a certificate
argument:

- with the certificate fresh, it is the prover;
- with a ground certificate, it is the checker.

Prover and checker are the same code, so they cannot disagree. Layer 1 is
the whole language. The query API (`proflog.fitting.query`, Definition 6.1)
only decides how the relation is run.

#### Implementation as built (revision 6)

- **Notation** (`proflog.fitting.syntax`). The notation is plain data in the
  paper's form:
  - `(∀x)P(x)` is `(forall x (P x))`, and variables are the symbols that
    are not symbols of L;
  - a parameter is `(par)`, `(par 1)`, `(par 1 1)`, … (unary);
  - a clause `R(x₁,…,xₙ) ← ϕ` is `(<- (R x₁ … xₙ) ϕ)`.

  There is no compile step: the data is the formula. The namespace checks
  well-formedness only:
  - Def 2.1: at most one clause per relation, distinct head variables, and
    every free variable of a body among its head variables, so that `ϕ(t̄)`
    is a sentence;
  - a query is a ground literal of L.
- **Systematic construction (§7).** A branch holds:
  - a queue of unexpanded sentences;
  - its γ sentences, each with the terms already given to it;
  - its literals, and which of them are new since the last saturation
    point;
  - its parameter count.

  The head of the queue is expanded by the one Table 2 rule its shape
  selects: ¬¬, ¬true, ¬false, α, β or δ. `false` closes the branch,
  `true` is dropped, a γ sentence is set aside, and a literal is recorded.
  When the queue is empty the branch is *saturated*, and it either:
  - closes: A/¬A, Reflexivity against ¬(t = t), Free Closure, or the
    Procedure Call Rule, possibly after One-One and Substitutivity steps
    that add literals;
  - or applies γ to its oldest γ sentence and continues. Rotating the γ
    sentences makes the construction fair.
- **Normal-form restrictions,** each sound (it only selects among Fitting's
  rule applications) and complete (it loses no closed tableau):
  - **closure on A/¬A is checked for literals.** A compound A is decomposed
    first;
  - **Substitutivity rewrites literals.** That suffices for ground equality;
  - **Reflexivity is used only to close against ¬(t = t),** its only use
    that can contribute to a closure. Equality has no clause, t = t cannot
    clash, and rewriting t to t changes nothing;
  - **γ terms are closed terms of L or parameters already on the branch,**
    each used at most once per γ sentence on a branch. This rests on the
    substitution lemma below. A repeated instance adds nothing. A new
    parameter is used only when no such term exists;
  - **no literal is added twice, and no t = t is added;**
  - **every closure uses a literal that is new since the previous saturation
    point,** directly or through the equality steps that derived it. A
    closure from older literals alone was available earlier with at least as
    much fuel. This is what stops a branch from closing again in the same
    way at every later saturation point.
- **Certificate format.** A certificate is the closed tableau as nested EDN
  vectors. There is one node per rule application, tagged:
  - Table 2: `:not-not`, `:not-true`, `:not-false`, `:alpha`, `:beta`
    (two subtrees), `:delta`, `:gamma t`;
  - closures: `:false`, `:close A`, `:reflexivity t`, `:free-closure e`;
  - §5 extensions: `:one-one e`, `:substitutivity e X X'`;
  - `:call A` with the subsidiary tableau's certificate.

  Certificates name literals and γ terms but no quantified formula, so they
  contain no binders. They can be written by hand, printed and read back.
  The paper's §6 tableau for `win(s(s(s(0))))` is in the test suite as
  written, and prove mode finds exactly that certificate.
- **Pure subset only:** `==`, `conde`, `fresh`, and relations built from
  them (`defne`, `conso`, `appendo`, `membero`) over finite tables. There is
  no nominal logic, `!=`, `clojure.core.logic.fd`, `conda`/`condu`/`onceo`
  or `project`. Side conditions are stated without negation:
  - **Binders** are named, as in the paper. `substo` transcribes Fitting's
    definition of `ϕ(t₁,…,tₙ)`, "substituting t₁,…,tₙ for all free
    occurrences of x₁,…,xₙ" (§2):
    - an occurrence of the variable being instantiated is replaced, and any
      other leaf is kept;
    - a quantifier that binds the same name stops substitution below it;
    - the inserted terms are closed, so nothing can be captured.

    Closure on `A`/`¬A` is `==` on sentences, the paper's syntactic
    identity.
  - **Name distinctness:** a finite table of (variable, leaf) pairs built
    from the program text, the root sentence and L's constants. No rule
    creates a name.
  - **δ freshness:** each branch counts the parameters it has introduced. δ
    introduces `(par . k)` and increments the count, so the parameter is new
    to the branch by construction.
  - **"Distinct symbols of L"** (Free Closure), and term and literal
    difference (the restrictions above), come from finite tables of distinct
    constant, function and relation symbol pairs. On ground syntax,
    difference is structural.
  - **"Atom of L"** for calls is a structural relation that admits no
    parameter.
- **Check mode is search-free.** Every clause unifies the certificate's rule
  tag, or the shape of the sentence being expanded, before anything else. A
  check of the paper's certificate has exactly one answer.
- **Fuel.** Each γ application, call, One-One step and Substitutivity step
  costs one unit, and a γ term costs one more unit per function symbol in
  it. The bound is per branch path, so each run is a finite search. In check
  mode the fuel is fresh.
- **Search control** (`proflog.fitting.query`):
  - one prove-mode run per side at the fuel bound;
  - the two sides in parallel, with the first closed tableau winning and
    the other search cancelled;
  - every certificate re-checked in check mode before it is reported.

  Cancellation steps core.logic's stream directly, with the same loop as
  `run`, and checks for thread interruption.
- **Not built:** rejection diagnostics, meaning a host walk that names the
  first failing node of a rejected certificate. Acceptance never depended on
  it.

#### Why named variables, not nominal logic or de Bruijn indices

Nominal logic earns its place when substitution can capture variables, or when
freshness cannot be made structural. Neither is the case in Fitting's
calculus. Without that need, the representation closest to the paper is the
paper's own: named variables.

1. **Every substitution inserts a closed term:** γ instantiates with a closed
   term of Lpar, δ with a parameter, the Procedure Call Rule with the ground
   arguments `t̄`, and Substitutivity replaces one closed term by another. A
   closed term has no free variables, so it cannot be captured. The only
   question substitution asks of a name is whether an occurrence is the
   variable being instantiated. That is a lookup in the finite
   name-distinctness table described above.
2. **Parameters enter a branch only through rule steps.** The root of the
   main tableau is a literal of L, and the root of each subsidiary tableau is
   `φ(t̄)` or `¬φ(t̄)` with `t̄` in L, because the Procedure Call Rule fires only
   on atoms of L. Neither contains parameters.
   - *Renaming lemma:* renaming each δ parameter to `(par k)` throughout the
     subtree below its δ node maps any closed Fitting tableau to a closed
     counter-form tableau. Free Closure and the Procedure Call Rule mention
     parameters only to exclude them, and the other rules ignore which
     parameter is which.
   - *Substitution lemma:* replace a γ-introduced parameter `q`, one not yet on
     the branch, by a closed term `t₀` over L and the parameters already on
     the branch, throughout the subtree. Closed tableaux stay closed.
     - No δ step below can introduce `q` or a parameter of `t₀`, since all of
       them are on the branch.
     - Existing calls are on atoms of L, which do not contain `q`.
     - Free Closure does not involve parameters.

     This justifies the γ-term restriction above.
3. **The code reads as the paper.** For each rule, the named form is the one
   whose code is the paper's sentence:
   - *`ϕ(t̄)`* (§2) is substitution for free occurrences. With named
     variables, `substo` is that definition's two cases. A nominal `substo`
     adds freshness side conditions (`nom/hash`) for a capture the calculus
     cannot produce, and a de Bruijn `substo` adds index arithmetic.
   - *Closure* (§4), "both A and ¬A for some sentence A", is syntactic
     identity, which is `==` on named sentences. Nominal unification and de
     Bruijn indices both identify alpha-variants. They would close
     `(∀x)P(x)` against `¬(∀y)P(y)` in one step. The paper closes that pair
     in three: δ, γ, then an atomic closure.
   - *δ* (§4): "a parameter that has not previously occurred on the branch"
     is the per-branch counter. `nom/fresh` gives a globally fresh name,
     which is a different notion.
   - *Text:* formulas and certificates print as written and read back as
     plain EDN. Paper-example certificates are written by hand, golden files
     are stored, and engine output is handed to the checker. Noms print as
     `<nom:y__14286>`, ties print as `[<nom:…>] body`, and neither can be
     read back.
4. **Independence.** Layer 1 and Layer 2 share no rule code: each has its own
   substitution and closure. Certificates contain rule tags, literals and
   terms, but no quantified formulas, so binders never cross between the
   layers.

The cost is that named substitution is correct only because the inserted
terms are closed. An open term would be captured silently, where nominal
logic would stay correct. The invariant is therefore enforced, not assumed.
γ terms, call arguments and Substitutivity terms must satisfy the
closed-term-of-Lpar relation, which has no case for a variable, and a
falsifier tests this (Test Obligation 3).

Neither lemma depends on the binder representation. Both are needed for any
finite reading of "any closed term of Lpar" and "a new parameter".

#### With §8's free variables

(This analysis was written before revision 6 and writes a bound occurrence
of y as `(var y)`. In revision 6's notation it is the bare symbol `y`.)

§8 replaces γ's "any closed term" with "a free variable, followed by later
unification to close branches". A free variable is therefore a placeholder
for a closed term of Lpar that is chosen later. A free-variable tableau is
correct only if every closure it makes is one the ground calculus of §4–§6
could make once the placeholders are filled. Grounding plus Layer 1 checks
exactly that (Decision §4).

**Substitution stays capture-free.** The terms §8 inserts are not closed:

- γ inserts a logic variable;
- run-time Skolemization inserts `(sk i v₁ … vₙ)`;
- a free-variable call inserts arguments that may contain logic variables.

None of them contains a bound occurrence `(var x)`, because logic variables
are a separate sort that no binder binds. The invariant becomes "an inserted
term contains no `(var x)`"; "closed" is its ground case.

**Unification under a binder is the one new hazard.** Unification fills
placeholders, so it is correct only if it fills each one with a term. Applied
to formulas that still contain quantifiers, it can instead fill a placeholder
with a bound occurrence.

- *Example.* Let S be
  `(∀x)[((∀y)R(x,y) ∧ ¬(∀y)R(y,y)) ∨ ((∀y)Q(x,y) ∧ ¬(∀y)Q(y,y))]`, with
  L = {0}, no function symbols, and no clauses for R or Q.
  - S is false in the weak Herbrand model whose domain is exactly the
    Herbrand universe {0}. At x = 0, each disjunct needs both R(0,0) (or
    Q(0,0)) and its negation.
  - S is true in the weak Herbrand model with domain {d, e}, 0ᴵ = d, and e
    non-standard. §3 drops domain closure ("there may be other
    'non-standard' items"), and Def 3.1 is vacuous for L = {0}. Take R false
    only at (d,d) and Q false only at (e,e).
- *Fitting's expected result.* S has no closed tableau. For the program
  `p(w) ← S`:
  - s_P(p(0)) = s_P(S) by Def 3.5.
  - R and Q have no clauses, so Φ_P gives every R and Q atom of L the value
    ⊥ at every stage (Def 3.7), and so does s_P. Both models therefore
    extend s_P (Def 3.3).
  - S is true in one of them and false in the other, so s_P(S) = ⊥ (Def
    3.4), and p(0) neither succeeds nor fails.

  The tableau side agrees. The δ parameter for ¬(∀y)Q(y,y) can name the
  non-standard element. It is not in L, so neither Free Closure nor a call
  can use it, and no rule can show that the domain is {0}.
- *The hazard.*
  - γ introduces the placeholder `v`, and β splits.
  - On the left, unifying `(∀y)R(v,y)` with `(∀y)R(y,y)` binds
    `v := (var y)`.
  - The binding is global, so on the right `(∀y)Q(v,y)` becomes
    `(∀y)Q(y,y)`. The escaped name is captured by another binder, and the
    branch closes against `¬(∀y)Q(y,y)`.
  - The result: S is refuted and p(0) fails. The probe
    `docs/log/2026-11-07-fitting-core-probes/binder-escape.clj` reproduces
    both steps with named `==`.
- *Why it is wrong in Fitting's terms.* No closed term `t` makes `(∀y)R(t,y)`
  identical to `(∀y)R(y,y)`, so the left closure has no ground counterpart.
  Grounding would find `v` bound to `(var y)`, which is not a closed term, and
  Layer 1 would reject the certificate. The engine must not produce such a
  certificate.

Nominal logic changes this only in part. Nominal unification also closes the
left branch, with `v := a₀`, because unknowns may be instantiated with names
by design. Names are globally distinct, so the right branch is not then
captured, and S is not refuted in the probe. The left closure is still
unjustified. Only an explicit freshness constraint `a # v`, stated for every
binder that `v` was introduced outside of, blocks it.

**What the engine must do,** whatever its representation, is unify only what
Fitting's free-variable calculus unifies:

- *Closure on literals only.* Free-variable tableaux, which §8 refers to [4]
  for, close a branch by unifying complementary atomic formulas. Nothing is
  lost: closure on a complementary compound pair is derivable from atomic
  closures after decomposition.
- *Equality on literals and terms only.* §5's Substitutivity rewrites "an
  occurrence of a closed term". In a free-variable tableau that means an
  occurrence containing no bound variable, and literals contain none.
- *Run-time Skolemization* (D1). Ahead-of-time Skolemization, which §8 also
  offers, inserts `f(x₁,…,xₙ)` under the binders of `x₁…xₙ` and would need
  capture-avoiding substitution.

Under these rules no unifier ever sees a binder. Nominal unification's
alpha-equivalence would never be exercised, so the engine uses named
variables like Layer 1 (Decision §3).

**Where nominal logic earns its place.** The tableau precedent is αleanTAP.
There, nominal logic replaces leanTAP's non-logical `copy_term` for
instantiating universals, and it lets the prover run on partly unknown
formulas. That second use is where unification under binders cannot be
avoided. In Proflog, formulas come from the program text and a ground query,
so only terms are ever unknown. Nominal logic would become relevant if
Proflog were run backward over formulas, for example to synthesize clauses
from certificates. That is outside Fitting's proposal.

### 3. Layer 2 — free-variable engine (§8), core.logic

**Deferred (revision 6).** §8 is Fitting's discussion of implementation, not
part of the language. Layer 1 runs the paper's programs on its own, so the
engine is deferred to a follow-up ADR, which starts from the design below.
That ADR must also give the engine its own forms of Layer 1's search
restrictions. Those use structural difference on ground terms, which a term
holding an unbound logic variable does not support.

The design as proposed: extracted from the keep-list in Appendix A, with
these changes:

- **Named binders.** The engine uses Layer 1's notation, `(forall x ϕ)` with
  occurrences `(var x)`, and has its own `substo` that inserts logic
  variables, Skolem terms and call arguments. This replaces the root code's
  nominal ties. The choice is made on its merits (Decision §2, "With §8's
  free variables"), not inherited.
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
- **Binder invariants (§8).** These follow from Fitting's free-variable
  calculus, not from any existing code; see "With §8's free variables" in
  Decision §2.
  - Skolemization happens only at run time (D1).
  - Unification applies only to literals and terms. This covers closure,
    Substitutivity, Free Closure and disunification, and call arguments. A
    literal on a branch has no binder above it, so it contains no bound
    occurrence. Every unifier therefore binds logic variables to terms
    without `(var x)`.
  - Closure on literals is complete, so nothing is lost.

### 4. Proof-carrying answers

Layer 1's query API already works this way. Every `:succeeds` or `:fails`
carries a certificate that the relation has re-checked in check mode. The
rest of this section is for the deferred engine.

The engine returns certificates, not bare statuses. After a closure, the final
rigid substitution is applied. Each distinct ground Skolem term becomes a fresh
parameter, and each unbound variable becomes a closed L-term consistent with
the D5 constraints. The result is emitted as a Layer 1 certificate.

- `query-status` returns `:succeeds` or `:fails` **only if `tableauo`
  accepts the certificate in check mode**. A rejected certificate is reported as an engine
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

- Revision 6: the language is delivered by Layer 1 alone. The points below
  about Layer 2 apply to the follow-up ADR.
- There are two implementations of one specification. That is the point:
  Layer 1 is the oracle and certificate checker for Layer 2, and its own
  correctness is reviewable rule by rule against Tables 1–2 and §5–§6.
- Layer 1 and Layer 2 both depend on stock core.logic. Layer 1 uses only its
  relational subset, and the hand-derived §3 values in the tests stay an
  external check on both layers.
- Layer 1 reads like the paper: `(forall x ϕ)`, substitution for free
  occurrences, and closure as syntactic identity. Its substitution is correct
  only for closed terms. That invariant is a structural relation with a
  falsifier, not an assumption.
- The engine's grounding step must emit certificates in Layer 1's format
  exactly. Certificates carry no formulas, so binders never cross between the
  layers. Grounding maps logic-variable bindings to closed terms,
  and maps each branch's Skolem terms to `(par k)` in the order of that
  branch's δ steps.
- Removing `once-forall` and the call-free γ gate will make some searches
  slower. Fuel turns this into `:unresolved`, never into a wrong answer.
- Rejected alternatives:
  - *Trim the root project in place.* Rejected because it breaks SJAS on the
    branch and cannot drop the vendored core.logic.
  - *New repository.* Rejected at the owner's direction (branch + ADR).
  - *Keep `once-forall` as a §8 compromise.* Rejected; see Context.
  - *Engine only, no reference.* Rejected because the existing audit shows that
    assertions of fidelity without an independent oracle let D1–D5 through.
  - *Plain-Clojure checker beside a separate reference prover* (first draft).
    Rejected 2026-11-08: it needs two artifacts that are correct and agree.
  - *The engine as its own checker* (the ADR-0117 pattern). Rejected because
    it accepts its own unsound proofs.
  - *Nominal logic in Layer 1* (second revision). Rejected 2026-11-08. Its
    capture-avoidance solves a problem the calculus does not have. Its
    alpha-equivalence changes the paper's closure condition. Noms do not
    print readably.
  - *De Bruijn indices in Layer 1* (third revision). Rejected 2026-11-08.
    They are correct, but the code and certificates no longer read like the
    paper, and scope resolution becomes extra code to test.
  - *Nominal logic in the engine*, carried over from the root code until
    2026-11-08. Rejected on its merits.
    - The engine never unifies under a binder, so alpha-equivalence is never
      used.
    - Where unification under a binder does happen, nominal logic still needs
      explicit freshness constraints to be sound.
    - Using different binder machinery in the two layers adds little, because
      they share no code and each `substo` is small and tested directly.

## Test Obligations

Red first, in this order. Each test states its `s_P` value and the §3 argument.

Status at revision 6 (in `fitting/test/proflog/fitting/`):
- Obligations 1, 2, 4 and 5 are met.
- Obligation 3 is met with the following changes:
  - Certificates do not name δ parameters or rule components, because the
    relation computes them. The δ-parameter falsifier is therefore replaced
    by its γ counterpart: a γ term that is a parameter not yet on the
    branch. That is D1's shape, and it would make the later δ parameter not
    new.
  - The wrong-component falsifier is replaced by a wrong rule tag for the
    sentence.
  - The `stepo` diagnostics are not built.
- Obligation 6's D1–D5, binder-capture and D5 falsifier cases run against
  Layer 1 at the query level, with positive controls. The binder-escape
  falsifier and obligation 7 need the engine, so they move to the follow-up
  ADR.

1. Subproject skeleton: a namespace-closure test asserting that the transitive
   requires of `proflog.fitting.*` are within `clojure.*`, `clojure.core.logic*`,
   and `proflog.fitting.*`.
2. `tableauo` check mode, positive: hand-written certificates for paper
   examples are accepted. Each check terminates with exactly one answer.
3. `tableauo` check mode, falsifiers (each must be rejected, and `stepo`
   diagnostics must name the offending node):
   - a δ parameter `(par j)` whose index is not the branch's counter, that is,
     one already on the branch;
   - an instantiation that replaces an occurrence bound by an inner
     quantifier of the same name;
   - a call on an atom containing a parameter;
   - a γ term outside the allowed set (L symbols and parameters already on
     the branch);
   - a γ term that is not closed, that is, one containing `(var x)`;
   - an `A`/`¬A` closure on alpha-variants, such as `(∀x)P(x)` and
     `¬(∀y)P(y)`, which are different sentences in the paper;
   - an open leaf;
   - a wrong α or β component;
   - Free Closure applied to `p = c` with `p` a parameter.
4. `tableauo` prove mode: certificates for the values stated in the paper for
   P1 and P2, and for the §8 factored-`move` non-example. Every certificate it
   produces is also accepted in check mode.
5. Purity and syntax:
   - a source test asserting that Layer 1 namespaces use no nominal logic,
     `!=`, `fd`, `conda`, `condu` or `project`;
   - programs violating Def 2.1 are rejected: two clauses for one relation,
     a repeated head variable, or a body with a free variable not in its head;
   - `substo` stops at an inner binder of the same name (shadowing);
   - `(∀x)P(x)` and `¬(∀y)P(y)` close through δ, γ and an atomic closure;
     the one-step closure is the falsifier in 3;
   - every certificate in the suite prints and reads back equal.
6. Engine, copied unchanged from Appendix A: the D1–D5 table above, reproduced
   red. Required results:
   - D1: no closure; `t(0)` is not `:fails`.
   - D2: no closure; `r(0)` is `:succeeds`.
   - D3: `r(0)` is `:succeeds`; `r3(0)` is `:fails` (control).
   - D4: `false` closes; `r(x)←false` gives `:fails`; `r2(x)←true` gives
     `:succeeds`.
   - D5: `q0(0)` is `:fails` with and without the unrelated clauses.
   - **Binder-escape falsifier:** with `v` registered as a free variable of
     the branch, a branch holding only `(∀y)P(v)` and `¬(∀y)P(y)` does not
     close. For every closed `t`, `{P(t), (∃y)¬P(y)}` is satisfiable. The
     engine's only route is γ giving `P(v)`, δ giving `¬P(sk i v)`, and the
     occurs check rejecting `v := sk i v`. One-step unification of the
     quantified pair would bind `v := (var y)`.
   - **Binder-capture falsifier:** with L = {0}, no function symbols, and no
     clauses for R or Q, the program `p(w) ← S` (S as in Decision §2, "With
     §8's free variables") gives `s_P(p(0)) = ⊥`. The result must not be
     `:fails`, and the engine must not emit a certificate that Layer 1
     rejects. Unification under binders refutes S through escape and then
     capture.
   - **D5 falsifier:** with L = {0} and no function symbols, `q0(w)←∀u(u=0)`
     gives a ⊥ value for `q0(0)`. The model `{0}` satisfies the body, and a
     model with one extra element falsifies it. The result must not be
     `:fails`; naive core.logic `!=` disunification, which assumes an infinite
     domain, would return `:fails`.
7. Differential: engine against reference on the test corpus.

## Exit Criteria

Revision 6, for Layer 1 (this ADR):

- Obligations 1–5 and the Layer 1 part of 6 are green in the default suite.
  The deeper P1 queries are green in `lein test :slow`.
- Prove mode finds the paper's §6 tableau.
- An independent fidelity review of the code against the paper has been done
  and its findings addressed.
- An AAR records line counts, timings and the design measurements.

As proposed, for both layers (the engine criteria move to the follow-up ADR):

- All obligations above are green in both layers, with the reference within
  its fuel budget.
- Every definite engine answer in the suite carries a certificate that
  `tableauo` accepts in check mode.
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
| `ast` | 304 | §2 terms, formulas | drop bounded quantifiers and `once-forall`; binders become named (Decision §3) |
| `language` | 474 | §2 language, validation, clause grouping (multiple surface clauses are combined into one disjunctive body, recovering one clause per relation) | drop guarded-alternative IR (`language.clj:251-377`) |
| `normalize` | 126 | NNF, ¬¬/¬true/¬false | drop SJAS bounded-quantifier lowering; ¬∃ becomes ∀ |
| `subst` | 288 | substitution | drop `acyclic-unifyo` (occurs check off, SJAS); rewritten for named binders, with no ties and no `!=` on names |
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
