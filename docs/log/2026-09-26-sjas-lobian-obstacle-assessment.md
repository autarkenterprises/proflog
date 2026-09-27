# SJAS And The Löbian Obstacle: Literature Assessment

Date: 2026-09-26

## Context

Question: *What effect does SJAS have with regards to resolving the Löbian
Obstacle?*

A first answer was drawn from this repository's own records, chiefly
[Computational Self-Justification: Assessment Against The Artifact](2026-06-10-computational-self-justification-assessment.md)
and the 2026-05-20 diagonalization notes. The user then asked for every element
of that answer to be independently corroborated or refuted, and for the question
to be treated as mathematical reasoning over the primary literature, including
the `github.com/jpt4/sjas` corpus. This note records the resulting assessment as
delivered. Two derivations in Section 4 are the assessor's own, not the
literature's. The user asked for them to be double-checked immediately after
delivery; the result, and a follow-up question about Level-1 consistency, are
appended as an addendum.

Sources, all read directly from text extracted from `jpt4/sjas` at commit
`53d56a0`:

- Yudkowsky and Herreshoff, "Tiling Agents for Self-Modifying AI, and the
  Löbian Obstacle" (2013 early draft):
  `nachlass/works-citing-dew/Yudkowsky_E_Herreshoff_M/yudkowsky_herreshoff2013_tiling_agents_draft.pdf`.
- Willard 2001 JSL, "Self-Verifying Axiom Systems, the Incompleteness Theorem
  and Related Reflection Principles":
  `nachlass/papers/willard2001_self_verifying_axiom_systems_author_jsl1.pdf`.
- Willard 2002 JSL ("JSL2"), semantic tableaux almost to Robinson's Q:
  `nachlass/papers/willard2002_semantic_tableaux_robinson_q_author_jsl2.pdf`.
- Willard 2006 JSL, real-valued arithmetic:
  `nachlass/papers/willard2006_real_valued_arithmetic_author_jsl6.pdf`.
- Willard 2011 (arXiv 1108.6330) and 2013/2014 (arXiv 1307.0150):
  `nachlass/papers/willard2011_self_justifying_logics_arxiv_1108.6330.pdf`,
  `nachlass/papers/willard2013_significance_self_justifying_axiom_systems_arxiv_1307.0150.pdf`.
- Works citing Willard: Pakhomov 2019 (`works-citing-dew/Pakhomov_F/`),
  Cheng 2020 and 2021 (filed under `works-citing-dew/Visser_A/`, but both are by
  Yong Cheng), Dvorkin 2024 (`works-citing-dew/Dvorkin_LV/`), Artemov 2019
  (`works-citing-dew/Artemov_S/`).
- Programme documents: `README.md`, `sjas.org`, `prose/motivation.txt`.

## Summary

SJAS partly resolve the Löbian obstacle. Willard's systems prove more than their
own consistency: for bounded (Δ₀) and existential (Σ₁) statements some of them
prove exact soundness of their own proofs, and that makes a large difference for
Löb. For claims with unbounded universal quantifiers (Π₁), exact self-trust is
impossible in any system that can check its own proofs, however weak its
arithmetic. SJAS offer only an approximation there, and it degrades with each
generation of successors. The costs are no total integer multiplication, axioms
that must stay frozen, and a consistency guarantee that comes from outside the
system.

## 1. What the Löbian obstacle is

In Yudkowsky and Herreshoff's (Y&H) setup:

- A child agent A₀ acts only on a proof: it does action b only if
  Axm₀ ⊢ b̄ → G, where G is the goal.
- By inspecting A₀'s code, the parent A₁ gets: if A₀ is built, then
  ∀b (b̄ → □₀⌜b̄ → G⌝), where □₀ means "provable from Axm₀".
- To conclude that building A₀ achieves G, A₁ needs their soundness schema
  (2.5): A₁ ⊢ ∀x (□₀⌜φ(x)⌝ → φ(x)).
- Löb's theorem: if T ⊢ □φ → φ then T ⊢ φ. So parent and child cannot share one
  theory, and each successor must be strictly weaker. Only finitely many
  generations are possible, which they call the "telomere".

Löb's proof needs a self-referential sentence L ↔ (□L → φ) and three derivability
conditions on □:

- D1: if T ⊢ ψ then T ⊢ □ψ
- D2: □(ψ→θ) → (□ψ → □θ)
- D3: □ψ → □□ψ

Gödel's second incompleteness theorem is the case φ = ⊥ (Y&H footnote 9).

Y&H ask for four properties of a solution:

1. Trust tiles indefinitely without the trusted mathematics dropping.
2. Quantified belief is coherent: the agent never believes "for every x I can
   prove φ(x)" without also believing "for every x, φ(x)".
3. The agent trusts its own code.
4. The agent trusts an outside copy of its own reasoning as it trusts itself.

Their own partial fixes each give something up:

- Infinitely descending theories (T-n) meet properties 1-2 but are unsound: the
  theory proves "ZF is inconsistent".
- Fallenstein's Tκ is sound under every reading of κ, but trusts a successor only
  "for κ−1 steps".
- Probabilistic reflection has no tiling formalism yet.

## 2. What overcoming it would require

The target is a theory T that:

1. is sound (true in the standard naturals ℕ);
2. is strong enough to model the agent's world and its own computations;
3. proves ∀x (□_T φ(x) → φ(x)) for every goal formula φ, with the successor
   using T itself;
4. stays trustworthy when agents modify themselves.

Three results bound what is possible:

- Löb: any T satisfying D1-D3 cannot have property 3 for any φ it does not
  already prove.
- Willard 2001, Theorem 7.2: a consistent system cannot prove soundness of its
  own proofs for all Π₁ sentences if it checks its own proofs with a bounded (Δ₀)
  formula, uses a sound and complete deduction method, and proves Peano
  Arithmetic's Π₁ theorems about its function symbols. This holds even if
  addition, multiplication and successor are not total. The proof is the
  Gödel-sentence argument: reflection for Θ ≡ "no proof of me" yields ⊢ Θ;
  Δ₀-completeness then yields ⊢ Prf(⌜Θ⌝, m̄), hence ⊢ ¬Θ. It needs only the
  external form of D1 and a provably unique substitution function, not D2 or D3.
- Willard 2001, Theorem 7.4: a system that recognizes multiplication as total
  cannot prove even the uniform (free-variable) version of soundness for bounded
  formulas, given a mild bound on proof sizes (its equation 76).

As delivered: the most any system can have is uniform self-soundness for bounded
and existential formulas, and only without total multiplication; weakening
arithmetic never yields exact self-trust for Π₁ claims. (The addendum refines
both statements.) Any solution must restrict the goal formulas, weaken its
conclusions (as with κ), change what "belief" means, or give up soundness.

## 3. What SJAS prove about themselves

| System | Deduction | Arithmetic | What it proves about itself |
|---|---|---|---|
| IS(A) (2001) | semantic tableaux | addition total, multiplication not | No tableau proof of 0=1 exists from itself |
| ISD(A) with Tab-1 (2013) | tableaux plus reuse of Π*₁ lemmas | same | Level-1 consistency: no proof of both a Π*₁ sentence and its negation |
| ISλ(A) (2001 §5-6) | tableaux | same | Tangibility reflection ∀x[□Ψ ∧ TangRoot(x) → Ψˣ] for every prenex* sentence Ψ |
| ISREF(A) (2001) | Hilbert-style, with modus ponens | neither addition nor successor total | The same schema with TangPred |
| Level-1 systems (2011, Thm 6.12) | as configured | addition total | □Ψ → Ψᵀ, where Ψᵀ is equivalent to Ψ only from outside the system |

- "Tangible" means not too large relative to numbers known to exist. Ψˣ bounds
  only Ψ's unbounded universal quantifiers by x, so for bounded and existential
  sentences Ψˣ = Ψ, and ISλ(A) and ISREF(A) prove exact soundness □Ψ → Ψ
  (2001, equation 4).
- Willard asserts versions for formulas with free variables (ISλ_U, ISREF_U;
  2001 Remark 6.3), but "did not provide [the consistency proofs] in this
  article".
- Willard's consistency proofs (Theorems 3.4, 4.3, 5.1) show every axiom,
  including the self-referential one, is true in ℕ, assuming the base system A
  proves no false Π₁ sentence. So these systems are sound, unlike T-n.
- Through Group-2 they prove every Π₁ theorem of any consistent A, even A = ZF:
  strong on Π₁, weak on Π₂.

Failure points confirmed in the sources:

- Total multiplication breaks tableau self-consistency: JSL2 Lemma 4.7 builds a
  squaring chain uᵢ₊₁ = uᵢ² in a proof of size O((log log z)^C), and the paper's
  closing paragraph attributes the collapse under a 3-way relation M(x,y,z) to
  exactly this lemma. A multiplication-total ISλ variant is infeasible
  (2001, Thm 7.3; §6).
- Tab-2 lemma reuse breaks Level-0 self-consistency for addition-total systems
  (2013, Remark 3.3).
- Total successor breaks Hilbert-style self-consistency (Pudlák; Solovay).
- Floating-point arithmetic can keep all its operations total, including
  multiplication (Willard 2006); only exact integer multiplication has to go.

## 4. What this means for tiling agents

(a) Consistency level. An SJAS asserts its own consistency and stays consistent,
so the footnote-9 route (self-soundness implies Gödel II implies inconsistency)
is closed.

(b) Bounded goals: "Tκ without κ". Assessor's derivation, conditional on
Willard's unpublished claim that ISλ_U is consistent; double-check requested
(see addendum). As delivered, using Y&H's time-windowed goals:

- The child acts at time v only if S ⊢ φ(v), where
  φ(v) ≡ b̄ᵥ → ∀u ≤ v+K (∅ᵤ ∨ Gᵤ), taken to be bounded in v.
- By inspection, the parent (built at time t) gets: if A₀ is built, every child
  action at v comes with an S-proof of φ(v), and v ≥ t+1.
- Uniform reflection gives ∀v (□_S⌜φ(v̇)⌝ → φ(v)). So each child action is safe
  through v+K ≥ t+K+1, and nothing happens before the first action.
- That is exactly the parent's own requirement, with the same theory S and the
  same window K, and no decrement.

Tκ needs κ−1 because PA-strength theories cannot have uniform bounded
self-soundness (Theorem 7.4, Löb). SJAS drop the one thing that forbids it. Y&H
only considered the ISREF-style "soundness up to n−1" and wrote: "We cannot yet
see how to adopt Willard's self-verifying theories exactly to the Aα formalism,
since reasoning about ever-smaller numbers would again introduce a 'telomere'."

(c) Unbounded goals: the telomere is real. Assessor's derivation; double-check
requested (see addendum). The tangible bound x caps every unbounded universal
quantifier, including one hidden in a tangibility guard J(t) ≡ ∃v (t < v^{1/k}).
So if the child proves a claim restricted to tangible numbers, the parent
recovers it only on the smaller set {t : tᵏ ∈ J}. Each level of delegation takes
another k-th root (ISλ) or subtracts one (ISREF, whose tangibility predicate is
not closed under successor). The mathematics does not weaken; the guaranteed
horizon shrinks.

(d) Modification breaks trust. Changing any axiom changes the self-referential
consistency axiom, and the successor's version is not a theorem of the parent.
SJAS tile across program changes, not axiom changes.

(e) The guarantee is external. This is no special defect with respect to Löb
(T-n and Tκ are also justified from outside), but it is a defect with respect to
autarky claims (Section 5).

| Property | SJAS (assuming ISλ_U is consistent) |
|---|---|
| 1. Indefinitely tiling trust | Yes for bounded or existential time-window goals with frozen axioms; for unbounded (Π₁) goals, a shrinking numeric horizon |
| 2. Coherent quantified belief | Yes for bounded and existential φ; no for Π₁ φ (Theorem 7.2) |
| 3. Trust in own code | Yes for bounded goals |
| 4. Trust in outside copies | Plausibly yes; there is no κ whose meaning could differ between copies |

## 5. The `jpt4/sjas` programme's claims

- "Consistency relative to PA and self-provability of consistency": correct.
  Theorem 4.3 needs A to prove no false Π₁ sentence, which for systems
  containing Robinson's Q is equivalent to consistency.
- "Any systems stronger than SJAS thus satisfy the prerequisites for [the second
  incompleteness theorem]": overstated. There is no single strength order
  (Willard 2001: "it is futile to seek an idealized form of self-verifying
  system"). The theorem also presupposes a computably enumerable axiom set and a
  natural provability predicate. Niebergall's arithmetic extends PA and proves
  its own consistency (not c.e.); Pakhomov's H<ω is a natural weak theory that
  proves its own Hilbert-style consistency.
- Epistemic security "without reliance on an external meta-system": the
  assertion is internal, its justification is not. A Kleene-style extension
  asserts the same sentence while inconsistent (Willard 2001 §1), and
  `prose/motivation.txt` concedes the proof "relies on a supra-SJAS
  metalanguage".
- An SJAS agent as "the maximally expressive agent" with confidence about
  "arbitrary statements": false for Π₁ statements (Theorem 7.2); maximality is
  unproven (Willard 2011 says only "come close").
- "Cannot be tricked into corrupting its knowledge base with inconsistency":
  false for updates. The guarantee covers only true added axioms; adding 0=1
  yields an inconsistent system that still asserts its consistency, and the
  system cannot check an update's truth. (Proflog's
  `sjas-selfcons-demonstration-uses-substantive-proof-targets` builds exactly
  such a system with β = {0=1}.)
- Local objective 3, "closing over the metatheory" for autonomous confidence:
  circular as justification, because an inconsistent system also proves its own
  consistency.
- Independent assessments (Pakhomov; Cheng) regard Willard's construction as
  legitimate but "not completely natural", because the consistency axiom is
  built by self-reference.

## 6. Corrections to earlier answers and records

Confirmed:

- The Gödel-sentence argument against full self-soundness is Willard's own
  Theorem 7.2, including the caveat that the substitution function must be
  provably unique.
- Total multiplication is the trigger for tableau systems (JSL2 Lemma 4.7;
  Willard 2011 Fact D.3).
- Tab-1 lemma reuse survives while Tab-2 fails (2013 Remark 3.3).
- Changing axioms breaks trust; the consistency guarantee is external.

Wrong or overstated:

- "SJAS assert only consistency": true of IS(A) and of Proflog's implemented
  `:willard-sjas-tableau0`/`:willard-sjas-level1` profiles, false of ISλ, ISREF
  and the Level-1 systems.
- "Trusted results can't be chained; Hilbert-style deduction collapses": ISREF
  handles Hilbert proofs by giving up total successor, and tiling needs
  soundness instances, not internal chaining of proofs.
- "SJAS don't resolve the obstacle", stated flatly: for bounded goals they
  plausibly do (Section 4(b)).
- "D2 is the condition that fails": Willard 2001 Theorem A.1 only guarantees
  that at least one of D2 and D3 fails.
- [2026-06-10 assessment](2026-06-10-computational-self-justification-assessment.md),
  "the tiling-agents / Lobian-obstacle literature ... where Willard's systems
  were explicitly proposed as an escape hatch": contradicted by the Y&H quotation
  in Section 4(b).
- The same note's "Second Incompleteness Theorem rendered as a runtime
  phenomenon": a category error. A slow rejection of one bounded certificate is
  the cost of a finite search, and the fact that no search can establish a Π₁
  claim holds for PA too, so it says nothing specific about self-justification.

## Net assessment

The Löbian obstacle is really two obstacles:

- Bounded self-trust (consistency, existential and bounded claims) comes from
  arithmetic that allows very short proofs, and SJAS remove it. With the uniform
  variant, that gives a sound, same-theory, decrement-free solution for
  time-window goals, at the cost of total integer multiplication and frozen
  axioms.
- Π₁ self-trust depends only on proving true bounded facts plus a provably unique
  substitution function, so no weakening of arithmetic removes it. SJAS reproduce
  Y&H's telomere in numeric form.

SJAS are therefore a genuine technical partial resolution, not a full one. The
load-bearing premise is Willard's unpublished proof that the uniform variant is
consistent. Proflog's SJAS profiles implement only IS(A)-style consistency
axioms; ISλ's reflection schemas (2001 equations 9, 54 and 60) are the natural
target for testing Section 4(b) executably.

## Addendum: Double-Check Of The Assessor's Derivations (2026-09-26)

### A1. Section 4(b), "Tκ without κ": survives with a repair

- Error. As delivered, φ(v) ≡ b̄ᵥ → ∀u ≤ v+K (∅ᵤ ∨ Gᵤ) treats v+K as a term.
  That is valid in Willard's 2011/2013 U-Grounding language, where addition and
  Double are function symbols. But there only a non-uniform principle, for
  sentences, is documented (2013, principle 23). The uniform principle
  (2001, equation 60) is asserted for ISλ_U and ISREF_U. Those use the 2001
  Grounding language, whose seven function symbols are all non-growth: no term
  exceeds the maximum of its arguments. There v+K is not a term, and φ(v) is not
  Δ₀.
- Repair. State the window existentially:
  φ′(v) ≡ b̄ᵥ → ∃z (z − v = K̄ ∧ ∀u ≤ z (∅ᵤ ∨ Gᵤ)).
  This is Σ₁, so the tangibility bound leaves it unchanged and uniform reflection
  still gives it exactly. The parent picks z′ with z′ − t = K̄; z′ exists by
  ISλ's Group-1 axiom that addition is total (2001, equation 6). The child's
  guarantee reaches v+K > t+K, and ∅ holds before the first action, so the
  parent's own Σ₁ criterion follows. The theory S and the window K are the same,
  and there is no decrement.
- Scope. The repair needs total addition, so it applies to ISλ_U, not ISREF_U,
  which omits equation 6.
- Premises, now explicit:
  - P1: ISλ_U is consistent. This is Willard 2001 Remark 6.3; the proof is not
    published.
  - P2: environment predicates (∅ᵤ, Gᵤ, b̄ᵥ, "A₀ is built at t") can be added
    with true Π₁ axioms without breaking consistency preservation. This is
    plausible, because Willard's proofs argue by truth in a fixed standard
    structure, but it is not in the papers.
  - P3: the code-inspection facts the parent uses (A₀ acts only after its
    checker accepts an S-proof; A₀ does not act before t+1) are true Π₁ facts,
    available through Group-2 or β.
- Verdict: the conditional claim stands in the Σ₁ form.

### A2. Section 4(c), numeric telomere: confirmed with a correction

- Recheck. The child's J-relativized claim ∀t (J(t) → θ(t)) is, in prenex form,
  ∀t ∀v (t < Root(v,k) → θ(t)). The tangibility bound caps both t and v by x.
  So for tangible x the parent gets θ(t) only for t < Root(x,k), that is, on
  J₂ = {t : ∃x (J(x) ∧ t < Root(x,k))}, roughly {t : tᵏ ∈ J}.
- ISλ proves J₂ ⊆ J, because it recognizes TangRoot as a definable cut, which is
  downward closed. J ⊆ J₂ would need J closed under x ↦ xᵏ, a multiplication-like
  totality ISλ cannot prove. Each level of delegation therefore relativizes to a
  smaller cut: J, J₂, J₃, and so on.
- Correction. For ISREF the loss per level is a small constant, not exactly one:
  TangPred(x) ≡ ∃v (x < v − 1) nests to roughly "t+4 exists" against
  "t+2 exists".
- Scope. The telomere applies only to goals with unbounded universal quantifiers.
  Horizons that exist as numbers, stated Σ₁ as in A1, transfer exactly.

### A3. Section 2: two statements refined

- "The most any system can have is uniform self-soundness for bounded and
  existential formulas" is overstated. Theorem 7.2 forbids canonical reflection
  for all Π₁ sentences at once, not for every particular Π₁ sentence, and
  approximations such as tangibility and translation remain. Precise form:
  Theorems 7.2 and 7.4 leave exact uniform self-soundness possible only below Π₁
  and only without total multiplication; for the full Π₁ class only
  approximations are available.
- "Weakening arithmetic never yields exact self-trust for Π₁ claims" needs
  Theorem 7.2's hypotheses: a Δ₀ self-proof predicate, a sound and complete
  deduction method, and proof of PAX's Π₁⁻ theorems, which supply
  Δ₀-completeness and provable SUBST uniqueness. A system too weak to prove those
  facts is outside the theorem. SJAS meet the hypotheses through Group-2, so the
  theorem applies to them.

### A4. Section 4(d): not labelled a derivation, but also the assessor's

- "The successor's self-consistency axiom is not a theorem of the parent" was
  stated flatly. What is established:
  - it is not among the parent's axioms;
  - it cannot be imported from A through Group-2, because Con(S′) → Con(A) is
    provable (an A-proof of 0=1 becomes an S′-proof via the Group-2 axiom for
    "0=1"), so A ⊢ Con(S′) would give A ⊢ Con(A);
  - the parent has no internal consistency-preservation theorem.

  No derivation is therefore available. A proof of underivability is not claimed.

## Addendum: Does Level-1 Consistency Help With Unvetted Updates? (2026-09-26)

The question: Section 5 says a false update (for example 0=1) yields an
inconsistent system that still asserts its consistency. Does the stronger
Level-1 statement, that only one of p and ¬p is provable, help?

Answer: essentially no. Level-1 cannot vet updates. What detection exists comes
from Σ₁-completeness, and what internal guarantee exists comes from Σ₁
reflection.

- **Reading.** Level-1 consistency (Willard 2013 sentence 7; Proflog's
  `SelfCons1`) says at most one of p and ¬p is provable, for Π*₁ p. It does not
  say exactly one. "Exactly one" would be completeness for Π*₁ sentences, which a
  sound system cannot have: its Gödel sentence Θ is Π*₁, unprovable by the
  Theorem 7.2 argument, and unrefutable by soundness.
- **Equal truth conditions.** Semantic tableaux are sound and complete. So in ℕ,
  Level-0 and Level-1 consistency are both true exactly when the system is
  consistent. Level-1 is stronger only as an axiom to reason with, because the
  system cannot combine tableau proofs of p and ¬p into a proof of 0=1 without
  cut (Willard 2001, comment after Lemma 7.1). Whether either axiom is true after
  an update depends only on the update's truth.
- **A false update violates Level-1 at once.** Take a false Π*₁ update β′. It is
  an axiom, and ¬β′ is a true Σ*₁ sentence, provable from its Δ₀ counterexample.
  The updated system proves both, so its Level-1 axiom is false. Being
  inconsistent, it proves that axiom anyway, along with everything else.
- **Detection comes from Σ₁-completeness, not Level-1.** Every false Π*₁ update
  has a finite counterexample, so a refutation search eventually finds it. A true
  update can never be confirmed by search, because Π₁ truth is only co-r.e. Any
  update worth making is one the system cannot already prove, so by definition it
  is one the system cannot confirm.
- **What does help is Σ₁ reflection, not Level-1.**
  - ISλ and ISREF have exact Σ₁ reflection, as do Willard's 2013 hybrid Level-1
    systems that also verify principle 23. From □_S(¬β′) → ¬β′ they prove
    β′ → ¬□_S(¬β′).
  - By the trivial tableau deduction transformation, that says: if the update is
    true, the naive extension S ∪ {β′} is tableau-consistent.
  - Level-1 alone cannot derive this; it would need Σ₁ reflection or
    Π₁-completeness.
  - Even so, the guarantee is conditional on β′'s truth. It also covers only the
    naive extension, whose consistency axiom still refers to the old S. The
    self-justifying successor with a new fixed point still needs Willard's
    external theorem.
- **Practical options:**
  - Vet a Π*₁ update by bounded counterexample search; confirming it below a
    bound B is a Δ₀, tangibility-style check.
  - Treat updates as provisional, withdrawn if a counterexample appears.
  - Or keep unvetted material in Proflog's external layer, which SelfCons does
    not vouch for, at the cost of no self-trust over it.

## Addendum: Non-Numeric Translations And The Obstacle (2026-09-26)

The question: Willard's SJAS constrain proofs through numeric codes. A proof is
blocked when the number needed to represent its result is not accessible. Much
of this repository's work generalizes self-justification away from numeric
codes. Does that work affect whether self-justification can evade the Löbian
obstacle?

### The answer as given

No. The move away from numeric codes had not increased the capacity to evade
the obstacle, and its one implemented instance (Workstream C) puts the existing
capacity at risk. Numbers were never essential: the evasion rests on a
description-size budget, and numbers are one way to express size.

- **The hard part (Π₁ self-trust) does not depend on codes.** Willard 2001
  Theorem 7.2 needs only three things: the system checks concrete proofs of
  itself, its self-reference uses a substitution it proves unique, and its
  deduction method is sound and complete. Words, trees, terms or types
  reproduce all three.
- **The soft part is the size budget.** It covers consistency, and exact trust
  for Σ₁ and uniform bounded claims: a short proof cannot establish an object
  much larger than itself (Willard 2011 Fact D.3). Total multiplication breaks
  this, because squaring doubles a number's length at each step (JSL2 Lemma
  4.7).
- **Theorem 7.4 transfers to any coding (the assessor's derivation).**
  Multiplication does one job in Willard's proof: turning a proof of ∀x φ(x)
  into proofs of its instances. Any coding in which the system proves that its
  own proof codes compose (instantiation, concatenation, grafting) gets the same
  result. So uniform bounded self-trust requires that proof-code composition is
  not provably total.
- **Operations sorted by description length (the assessor's synthesis).**
  - *Safe:* operations that grow length by a constant, such as successor,
    addition, doubling, appending a symbol, and fixed-precision floating point.
  - *Dangerous:* operations that add lengths, such as multiplication,
    concatenation, pairing and grafting.
- **The repository's translations.**
  - *Words, trees or terms as codes:* the 2026-05-20 mechanism note's own
    test, a Fact-D.3-like invariant, is not met.
  - *Workstream C `pair/2` and list `cons`* (ADR-0123, ADR-0128) are total
    growth functions; no injective pairing on ℕ is non-growth. ADR-0108's Fact
    D.3 extension predates them. The June review's repair shows consistency
    without the new Group-3 axiom, not with it.
  - *Decidable theories* (WS1S, WS2S, Büchi arithmetic) dissolve the obstacle
    rather than evade it: trust gives way to decision.
  - *Native type-theory sketches* with universe stratification rebuild the
    telomere.
  - *The Lawvere programme* explains the hard limit. It can explain the evasion
    only if refined by resources.
- **Net, as given.** The capacity cannot exceed that of the numeric SJAS, and a
  translation preserves it only if it rebuilds the size budget in its own
  measure.

### Corrections after reading the refinement branch

The answer was given before reading `jpt4/sjas` branch `sjas-codification`
(commit `f5a8490`), whose refinement stage (2026-09-02 to 2026-09-26) bears
directly on it.

1. **"No translation rebuilds the size budget" is wrong.** λᶜᵉʳᵗ (next
   addendum) rebuilds it as a token budget on a separate certificate sort. Its
   consistency proof uses a budget-stratified model. It is the "separate sort
   with its own size measure" the answer recommended, already built.
2. **"Cannot exceed the numeric SJAS" needs a qualification.** λᶜᵉʳᵗ trades
   differently, and neither design dominates.
   - *What it gains:* its ordinary layer keeps Heyting arithmetic in all finite
     types, with multiplication and exponentiation total. Its pair-form
     consistency `H₁` ranges over every closed type. Willard's Level-1
     statements are confined to Π*₁ sentences, and Level(2+) is impossible once
     addition is total (Willard 2004 Theorem 1, as recorded in R4 §2.5).
   - *What it pays:* its self-consistency covers only certificates a program
     holds, and its code-level consistency is unprovable (its P5).
3. **The length sorting is a heuristic at the two ends, not a criterion.**
   Refined-sjas §5 shows no growth rate is exact. Hybrid(1), with naming rate
   Θ(n log n), is self-justifying, and Willard's boundary sits within one
   logarithmic factor. The exact form is an additive margin: Willard 2011
   Definition 4.5, "Tight", Log(q_β) ≥ ♯(β) + 2.
4. **The hard part holds for free representations only.** Theorem 7.2's
   argument uses the witness proof twice: as input to reflection, and as the
   counterexample to the Gödel sentence's instance. With affine certificates the
   double use cannot be typed. A code witness also cannot become a certificate
   without paying for its size. Trust over held certificates is therefore not
   blocked by Theorem 7.2; code-level trust still is.

## Addendum: The λᶜᵉʳᵗ And Affine-Calculus Work (2026-09-26)

Sources, all in `github.com/jpt4/sjas`:

- branch `sjas-codification` (`f5a8490`): `nachlass/refinement/`
  `R4-certificate-calculus.md`, `R4-metatheory.md`,
  `ADR-0005-lcert-implementation.md`, `RO1-affine-separation.md`,
  `RO2-contraction-growth-rate.md` and `refined-sjas.md`; and `code/lcert`;
- branch `adr-0003-alsjas-paper-first` (`6efbda1`):
  `docs/theory/alsjas-calculus.md`, ADR-0002, ADR-0003 and `code/alsjas`.

Neither line mentions the Löbian obstacle or tiling agents. The bearing drawn
below is the assessor's.

### What λᶜᵉʳᵗ is

- **The ordinary layer.** A dependently typed calculus with usages 0, 1 and ω.
  Without the certificate machinery it has the strength of Heyting arithmetic
  in all finite types.
- **Two representation sorts.**
  - `Syn` holds free code trees.
  - `R` holds certificate trees. Each internal node consumes one token of
    Hofmann's resource type ◇, which has no closed terms. A closed map from
    `Syn` into `R` yields only leaves (its Proposition 4.1).
- **Self-reference and self-trust constants.**
  - `chk′` is a primitive checker, so self-reference is by name, with no
    diagonal gadget.
  - `H : Π(r :₁ R). T(chk′(print r, c⊥)) ⊸ 0` says that no held certificate
    refutes the calculus. Its pair form `H₁` says that no two held certificates
    prove a type and its negation.
  - `reflect_D` runs a certified program of base data type D.
- **Consistency (T1)** is proved with a set-theoretic model in which values
  carry token footprints, by strong induction on the budget. The key step is
  strict overhead: a certificate that checks declares fewer tokens than its own
  node count, because contexts are written entry by entry. So the refutation it
  encodes has a smaller budget.
- **Limits.**
  - G2 for codes (P5): no budget derives code-level consistency. The proof
    reduces to G2 for PA and cites three standard results.
  - Uniform D3 and boxed contraction fail, by a counting argument (Proposition
    4.4).
  - Uniform D2 is conjectured to fail (Conjecture 4.6).
- **Implementation.** It is implemented in Clojure (`code/lcert`, fast and
  extended test suites). An Ansatz kernel, which is Lean-4-compatible, checks
  strict overhead and the budget arithmetic of the `H₁` case.

### What was checked here

- The `H₁` and `Reflect` cases of the fundamental lemma (metatheory Lemma 3.6)
  were re-derived. Both descend to a strictly smaller budget before the outer
  hypothesis is used. No gap was found. The other cases were read, not
  re-derived.
- The uniform failures in the derivability table follow, as stated, from size
  soundness and the quotation-cost bound: every certificate of □A has more than
  2μ(A) nodes.
- The documents state their status accurately. P5 cites standard results,
  Conjecture 4.6 is labelled a conjecture, and erasure correctness is labelled
  a sketch.

### Bearing on the Löbian obstacle

1. **As built, λᶜᵉʳᵗ offers consistency and certified evaluation, not
   reflection for propositions.** The only proposition it trusts its
   certificates for is 0, through `H` and `H₁`. `reflect` at `Bool` or `Nat`
   runs a program; as propositions, those types are trivially inhabited.
2. **Reflection extends to every ordinary proposition (the assessor's
   derivation).**
   - *Why the proof carries over.* The `Reflect` case needs V^m_m(D) ⊆ V^n_k(D).
     The metatheory proves this for base data and leaves higher types open. But
     consider any type in which neither `R` nor ◇ occurs and whose T-arguments
     contain no `reflect`. Its semantic set is independent of budget and
     footprint: V(T(b)) depends only on ⟦b⟧, and the usage-1 Π and Σ clauses
     collapse when V_j(A) is the same set for every j.
   - *What follows.* The same argument justifies `reflect_A` for every such A,
     which covers every proposition of arithmetic in finite types. The closed,
     budget-0 term
     `λ r e. reflect_A r e : Π(r :₁ R). T(chk′(print r, ⌜A⌝)) ⊸ A`
     is then the tiling agents' soundness schema, uniform over the certificates
     a program will hold.
   - *Why Löb stays blocked.* Löb's derivation is not recovered, because it
     needs the Löb sentence's certificate twice.
   - *Not yet checked:* whether P5 and the evaluator theorem (T4) survive the
     extension.
3. **This is Willard's tangibility, dualized.** Willard's tangibility
   reflection accepts any proof (∃y Prf(⌜Ψ⌝, y)) and relativizes the conclusion
   to tangible x. Extended λᶜᵉʳᵗ gives the conclusion in full, but only for
   proofs the program holds.
4. **Pay-to-trust.** A proof found at runtime is a `Syn` code. To act on it, a
   program:
   1. mints a certificate with the typed parser (metatheory §4.7), paying one
      token per node;
   2. checks it with `inspect`;
   3. consumes it with `reflect`.

   Each exercise of trust burns at least one token for good, by strict
   overhead.
5. **The telomere, relocated.**
   - *Nested trust.* Suppose a trusted certificate's program itself holds and
     reflects a certificate. Then sizes at least double per level: the
     program's context declares at least the inner certificate's size, and its
     term mentions each of those tokens. Nesting depth within a budget n is at
     most log₂(n+1).
   - *Trust in claims about future resources.* A typical claim: "the
     grandchild, given any token supply, only acts safely". Its type has `R` at
     usage 1. The model does not justify reflecting it. After decoding at
     budget m, V^m_m(Π(s :₁ R). B) constrains the function only on supplies of
     zero footprint.
   - That second step is exactly what a parent needs in order to trust a
     child's approval of a grandchild. A better model or a counterexample would
     settle whether λᶜᵉʳᵗ-style trust tiles. As it stands, the argument
     reproduces the telomere Yudkowsky and Herreshoff anticipated for Willard
     ("reasoning about ever-smaller numbers"), with budgets in place of numbers.

   > **Revised (2026-09-26, follow-up addendum below).** The fixed-budget model
   > cannot justify this step, but a reading that charges each call to its
   > caller's resources plausibly can, for first-order types. The halving
   > bound applies only to programs that build nested certificates from their
   > own declared tokens.

### ALSJAS, the affine modal calculus

- **The calculus.**
  - *Types:* 0, 1, ⊗, ⊸, box, and one selected fixed point G ≅ box(G ⊸ 0).
  - *Structural rules:* weakening, no contraction.
  - *Boxes* carry closed derivations. Boxed composition (K) and introspection
    (4) are uniform rules. There is no unboxing and no copying.
  - *Profiles:* `J[S]` adds `self0 : box 0 ⊸ 0`. The control `C[S]` also adds
    `copy : box A ⊸ box A ⊗ box A`.
- **Status.**
  - It is a paper specification (2026-08-29), with implementation suspended
    pending review (ADR-0003).
  - The Lean code mechanizes step determinism, syntactic rejection of
    `copyBox`, and an abstract G2. That G2 is stated over a host-function
    interface that does not enforce affinity, as ADR-0003 itself notes.
  - The headline preservation theorem (10.2) is not mechanized.
- **Checked here.**
  - *Consistency of `J[S]` relative to `B[S]`.* The argument is by descent on
    hereditary derivation size (Theorem 10.2), and it is sound in outline:
    normalization does not increase hereditary size, and a closed normal
    refutation must be `self0` applied to a quoted, smaller refutation.
  - *The Jeroslow construction in `C[S]`* is correct and uses `copy` exactly
    once. It renders Beklemishev and Shamkanov's box-contraction boundary as
    proof programs.
  - *RO1c.* The calculus passes the programme's own test: its box rule is not
    K4's contracting form.
- **Bearing on the obstacle: none directly.** ALSJAS forbids unboxing, so it
  has no reflection. It has no data or quantifiers, so it cannot state an
  agent's safety.
- **An affine `unbox : box A ⊸ A` appears consistent (the assessor's
  derivation).**
  - *The rule:* `unbox(quote D)` reduces to D's term.
  - *Why it appears consistent:* every step strictly decreases the pair
    (hereditary size, active size) lexicographically, and no closed normal term
    of 0 exists. This holds with the fixed point included.
  - *Consequence:* `self0` becomes `unbox` at 0.
  - *Why the diagonal fails:* refuting G needs G twice. This is why Curry's
    paradox fails without contraction (Grišin; Restall; Zardini — standard, not
    verified here).
  - *Limitation:* this is still not Beklemishev and Shamkanov's target, a
    mathematical theory, because ALSJAS has no arithmetic.

### The joint lesson

Löb's derivation, like Curry's paradox, uses the trust evidence twice. Make the
evidence affine and uniform self-trust survives: ALSJAS by lacking copy,
λᶜᵉʳᵗ by tokens.

The cost appears wherever evidence must be made from copyable data, and a proof
found by search is copyable data. So every design here must charge for turning
a found proof into trust, and nested trust pays compounding charges. That
charge is the telomere in resource form.

| | Willard ISλ/ISREF | λᶜᵉʳᵗ as built | λᶜᵉʳᵗ with ordinary reflect (proposed) | ALSJAS `J[S]` |
| --- | --- | --- | --- | --- |
| ordinary arithmetic | multiplication not total | HA in finite types | HA in finite types | none |
| self-consistency | code level (tableaux) | held certificates | held certificates | closed boxes |
| reflection | Σ₁ exact; Π₁ tangible | base data (evaluation) | all ordinary propositions, held certificates | none |
| what blocks Löb | composition not provably total | no uniform D3 or copy (tokens) | same | no box copy |
| telomere | tangible cut shrinks under nesting | budget halves under nesting; R-quantified trust unjustified | same | none needed; no reflection |

> The λᶜᵉʳᵗ telomere entries are revised in the follow-up addendum below.

## Addendum: Can A Child SJAS Add Efficiency Axioms Conservatively? (2026-09-26)

The question: can a child of a parent SJAS conservatively extend the parent's
axiom basis without differing in expressivity? The example is axioms that
correspond to more efficient programs.

The answer: a child can be an SJAS with such axioms, but it cannot be a
conservative extension that its parent can trust. The speed-up is exactly what
the parent cannot verify.

1. **The child can be self-justifying.** IS(A ∪ E) is consistent by Willard's
   theorem whenever three conditions hold: A ∪ E is true in ℕ, E lies in the
   admitted formula class, and E adds no growth function.
   - Efficiency axioms that make a growth function total are fatal (Willard 2001
     Theorems 7.3–7.4). Examples are fast multiplication as a total function
     symbol, and exponentiation.
   - A growth function may enter only as a Δ₀ relation, which verifies
     y = f(x) but does not compute y.
   - The naming convention must also stay within Willard's margin
     (refined-sjas §5).
2. **It is never strictly conservative.** The child's Group-3 sentence is a new
   sentence of the shared language, and the parent does not derive it
   (addendum A4 above). Only a child with the parent's exact axioms, which is the
   same system, is conservative.
3. **Even when conservative apart from Group-3, the child cannot be trusted by
   the parent (the assessor's derivation).**
   - Take E ⊆ Thm(parent): pure lemma axioms.
   - To prove the child consistent, the parent must turn any child refutation
     into one of its own, inlining a proof of each lemma at each use. In a
     tableau system that is cut elimination, which is proof composition.
   - Even a constant-factor increase in proof length squares the proof code, and
     IS(A) proves no squaring total. So the parent cannot prove the
     transformation total, and cannot prove the child consistent.
   - The speed-up factor is precisely the growth the parent lacks.
   - Willard 2020 (printed p. 279, quoted in RO2) calls this the "Linear-Sum
     Effect": modus ponens makes proof lengths add. Xtab recovers it through
     φ ∨ ¬φ nodes. Xtab is then inconsistent given three more hypotheses:
     successor totality, the ring laws stated as 3-way relations, and a
     self-consistency axiom (statement ⊙; a sketch per the programme's
     registry).
4. **The same holds in λᶜᵉʳᵗ.**
   - A child calculus with a faster δ-rule has shorter certificates, but the
     parent's `Check` rejects them.
   - Translating a child certificate into a parent certificate expands each
     fast step into the slow computation. That overhead grows with the input,
     but the only uniformly typable overhead is a fixed, additive number of
     tokens (Proposition 4.4).
   - Within one calculus, recorded conversions mean proof-by-reflection shortens
     no certificate: "compressing a term never compresses a certificate".
5. **What works.**
   - *Keep the basis fixed, and speed up the search.* A prover whose output the
     unchanged checker checks needs no trust for validity.
     - Descendants that are the same theory inherit the root's self-trust at
       every generation, with no decrement.
     - The only open ingredient is the uniform reflection principle ISλ_U,
       already noted in A1.
   - *Certifying algorithms.* An untrusted fast program computes a result plus a
     certificate, and the fixed basis checks the certificate cheaply (a Δ₀
     relation, or a witness). In Proflog terms: external clauses whose
     conclusions the reflected basis re-certifies.
   - *Pre-register lemmas in the root.*
     - A finite lemma set in the root's basis is trusted by every descendant.
     - An infinite family must stay recognizable by the Δ₀ proof predicate.
       Making it recognizable by Craig's trick attaches each lemma's proof, so
       each use costs the proof's size and the length speed-up vanishes. This
       assumes the Δ₀-recognizability requirement, which has not been
       re-checked for every Willard variant.
   - *Family self-consistency (open).* Consider a root axiom asserting the
     consistency of every IS(A ∪ E) with E in a recognizable efficiency class,
     where each member also includes the axiom itself.
     - It would let a lineage add pre-approved efficiency axioms.
     - It is a self-referential family fixed point, in the manner of ISλ_U.
     - Willard's argument does not settle whether it is true, because that
       argument needs every base axiom to be true.

## Addendum: Why Proof Predicates Range Over Codes (2026-09-26)

The question: why must a proof predicate operate over codes at all, rather than
over the raw deduction? This is the motivation for seeking SJAS analogues in
pure type systems and other symbolic models, where metaprogramming is well
established.

1. **Any internal proof predicate ranges over a representation.** It is a
   formula or type, so its arguments are objects of the theory. To speak about
   deductions, deductions must form a sort. The inhabitants of such a sort are
   codes in the functional sense, whatever their carrier: numbers, words, trees
   or S-expressions. Gödel numbering is the carrier that one-sorted arithmetic
   forces, not the essence.
2. **"The raw deduction" has three readings, and two of them already allow
   reflection.**
   - *The proof term itself (Curry–Howard).* The predicate collapses to
     inhabitation. `⊥ → ⊥` is the identity function and says nothing about
     derivability: a hypothesis x : ⊥ is not a closed derivation.
   - *Closed derivability as an opaque modality.* Several systems have uniform
     reflection and stay consistent:
     - Pfenning and Davies' □ validates □A → A, K and 4.
     - Artemov's Logic of Proofs has explicit reflection t:F → F.
     - Brown and Palsberg's typed self-interpreter for Fω has
       unquote : Exp A → A.

     None has an internal diagonal or a way to make evidence from data: boxes
     come only from closed terms, and quotation is meta-level.
   - *Codes as data.* This reading gives inspection, quantification, runtime
     discovery, rule modification and the diagonal, and it is where Löb bites.
     Kavvos (arXiv:1703.01288) reads the Gödel–Löb axiom as intensional
     recursion in a modal λ-calculus. Adding an evaluation map □A → A then
     inhabits every type: from □⊥ → ⊥, necessitation and Löb give □⊥, and
     reflection gives ⊥. A programming language merely diverges; a logic becomes
     inconsistent.
3. **Tiling needs the third reading.**
   - The child finds proofs at runtime, and a found proof is data.
   - Self-modification changes rules. So the parent must refer to the child's
     derivability, which is a different modality. It can define that modality
     only from a description of the child's rules, which is data again.
   - Opaque boxes cover only a system's own, statically quoted proofs.
4. **SJAS mechanisms need intensional representations.** Every mechanism in the
   corpus measures proof size or shape: Fact D.3, the margin, strict overhead. A
   representation up to conversion cannot see these. A quotation that respects
   β-conversion cannot return distinct codes for (λz.z)t and t. See the PTS note
   on `jpt4/sjas` branch `research/pts-lowering-obstruction`, §9.
5. **What type theory adds is types on the code sort, not an escape from
   codes.**
   - *Arithmetic* has one sort, with unrestricted contraction on terms. It can
     block duplication only by crippling the operations that make duplication
     costly (the programme's 2026-09-24 log, C1–C5).
   - *A typed calculus* can make the representation sort affine, modal or
     resource-indexed. It can forbid exactly what Löb consumes — box
     contraction, uniform quotation, and free manufacture of evidence from data
     — while the ordinary layer stays strong. λᶜᵉʳᵗ is the first working
     instance.
6. **Summary (the assessor's synthesis).** The Löbian obstacle arises when
   trust-bearing evidence can be made freely from copyable data.
   - Opaque modalities forbid that: evidence comes only from closed terms.
   - Willard makes it unaffordable in numbers: the codes are too large to name.
   - λᶜᵉʳᵗ makes it unaffordable in tokens.

   Runtime proof search must make evidence from data. So any agent that trusts
   what it finds pays per trusted proof, with compounding charges for nested
   trust.

The sources in item 2 are cited from standard knowledge and were not re-read
for this addendum:

- Pfenning and Davies, "A Judgmental Reconstruction of Modal Logic" (MSCS
  2001);
- Artemov, "Explicit Provability and Constructive Semantics" (BSL 2001);
- Brown and Palsberg, "Breaking Through the Normalization Barrier: A
  Self-Interpreter for F-omega" (POPL 2016);
- Kavvos, "Intensionality, Intensional Recursion, and the Gödel–Löb Axiom"
  (arXiv:1703.01288).

## Addendum: Empty Supplies, P5 And T4, And The Self-Trust Boundary (2026-09-26)

Three follow-up questions on the λᶜᵉʳᵗ addendum:

1. What "a reflected certificate only constrains behaviour on empty supplies"
   means.
2. Whether P5 and the evaluator theorems survive reflection at ordinary types.
3. Which program behaviours lie outside the self-trust boundary.

**The experiments.** They ran on a scratch copy of `jpt4/sjas` `code/lcert` at
`f5a8490`. The diff is saved beside this note as
[`2026-09-26-lcert-reflect-experiments.patch`](2026-09-26-lcert-reflect-experiments.patch);
apply it with `patch -p1` at the `jpt4/sjas` root. It makes three changes:

- `reflectable-type?` admits closed types in which R and ◇ do not occur and
  whose T-arguments contain no `reflect`;
- two switches for the demo in item 1 (`*allow-resource-reflect*` and
  `*dynamic-cap*`), both off by default;
- two new test namespaces, and one updated test that pinned the old rule.

With the patch, the fast suite plus the PA-translation tests run 57 tests with
502 assertions, all passing. Nothing was pushed to `jpt4/sjas`.

### 1. "Only constrains behaviour on empty supplies"

- **The model's sets.** V^n_k(A) is read at a budget n, the tokens in play, for
  a value accounting for k of them. A function that consumes a certificate,
  Π(s :₁ R). B, must be correct on every certificate that fits in the remaining
  budget n − k.
- **What the Reflect case proves.** It interprets the decoded program at its
  own declared budget m, with all m tokens assigned to it (k = m). The
  remaining budget is m − m = 0. So the induction hypothesis covers only
  certificates with zero nodes: leaves, which carry no tokens. It says nothing
  about the reflected function once it is handed a real certificate.
- **It is not pedantry: the fixed-cap evaluator misbehaves.** The demo is
  `resource_reflect_test.clj`.
  - A certified closed program F takes a certificate of Σ(x :ω Nat). T(x ≠ 0)
    and reflects it.
  - With reflect allowed at F's type, the test reflects F's certificate and
    hands the result a certificate of (1, ⋆).
  - F runs under its own cap, 0. The certificate it receives is over that cap,
    so F's inner reflect returns the default (0, ⋆), which claims T(0 ≠ 0).
  - Under the fixed-budget semantics the rule would therefore be unsound.
- **A caller-charged reading restores the descent (the assessor's sketch).**
  - Charge each call the resources it actually has: the certificate's declared
    m tokens plus the supplies a₁ … a_p passed to it.
  - Strict overhead gives m < ‖v‖, and affinity puts the supplies outside v's
    footprint. So m + Σ‖aᵢ‖ < ‖v‖ + Σ‖aᵢ‖ ≤ n, and every call runs strictly
    below the caller's budget, where the outer induction hypothesis applies.
  - *First-order types* are those whose inputs are certificates, tokens or
    ordinary data, and whose result mentions R only positively. For these,
    interpret the reflected value as a function that evaluates the decoded
    program only when fully applied, at budget m + Σ‖aᵢ‖, and returns the
    default past n. The Reflect case then needs only
    V^{n′}_{n′}(C) ⊆ V^n_K(C) for n′ ≤ K ≤ n, which holds for such result
    types C.
  - Higher-order types need a Kripke-style relation whose worlds are the
    available resources. That is not done.
  - *Operationally,* charging the call to the caller's cap returns the witness
    1. With that cap switched on, the whole suite stays green, except for the
    demo's own fixed-cap assertion.
- **Consequence for tiling (the assessor's analysis).**
  - State an agent's safety as the evidence it returns, not as a proposition
    about its run. For example, certify a program of type
    Π(s :₁ R). Π(i :ω I). Σ(b :ω Act). Safe(b).
  - A proposition about the run would put `reflect` inside a T-argument,
    whose meaning is budget-dependent.
  - Trusting a child then means reflecting its certificate and running it on a
    supply. That is a first-order type at every generation.
- **Correction to the λᶜᵉʳᵗ addendum.** Its item 5 and its table said trust in
  claims about future resources is unjustified, and that this reproduces
  Yudkowsky and Herreshoff's telomere. Revised:
  - The fixed-budget model cannot justify that trust, and the fixed-cap
    evaluator would misbehave. The caller-charged reading plausibly justifies
    the first-order case.
  - The halving bound holds only when a certificate's program builds its nested
    certificates from its own declared tokens. A program that takes its supply
    as an argument pays additively.
  - If the caller-charged model is completed, the trust schema is the same
    closed term at every generation, and only exercising it costs tokens. The
    telomere becomes a resource bill, not a decrement.

### 2. P5 and T4 under reflection at ordinary types

- **T4 and T4′ survive unchanged.**
  - Their Reflect cases never used the restriction to base data. The evaluator
    runs the decoded program at its budget m < n, and the outer induction
    relates the result to the model's value at whatever type.
  - The restriction entered only in T4′'s closing remark that "related" means
    equal at data types.
  - Tested in `ordinary_reflect_test.clj`: the erasing and non-erasing
    evaluators agree on a reflected Σ-witness and on a reflected Π-function.
- **P5 survives, but its proof needs a real repair.**
  - *Step 1* (PA interprets into λᶜᵉʳᵗ, provably in PA) is unaffected.
    Translated PA proofs use no `reflect`, so the extended Check accepts them
    unchanged. The PA-translation tests pass on the extended checker.
  - *Step 2 breaks as written.* It interprets `reflect_D` by the default value
    of D. That works for base data, because each base type contains its
    default. At a non-base ordinary type it fails: the default for
    Σ(x :ω Nat). T(x ≠ 0) is (0, ⋆), which is not a witness. The test shows
    exactly this fallback when the evaluator's cap is set below the
    certificate's size.
  - *The repair (the assessor's derivation).*
    - Interpret `reflect_A` by the value of a fixed certified witness: the
      decoded term of a smallest certificate of A.
    - When no certificate of A has at most n nodes, make the case vacuous by a
      PA-provable bounded fact, as BF₀ does for H.
    - The witness's derivation has a budget below its certificate's size. So
      Lemma 6.4 now needs an outer induction on budgets, at the meta level,
      with finitely many cases for fixed n. The original proof had avoided
      this.
    - Uses of `reflect_B` inside a witness either have smaller minimal
      certificates, so the recursion on μ is well founded, or are vacuous by
      bounded facts.
  - With the repair, E-PA^ω still proves Con_λ from a derivation of Con′_ω, and
    G2 for PA still forbids that.
  - As before, the bounded facts are true only if the extended calculus is
    consistent. That rests on the extended model argument: a paper argument,
    checked in its key case, not mechanized.

### 3. What lies outside the self-trust boundary

The boundary has three layers: what the language can express, what a program
can hold as a certificate, and what a held certificate can be trusted for.
Trusted claims are then metered.

- **Not expressible at all.**
  - *Non-termination and unbounded interaction.* The calculus is total (T4):
    there is no general recursion, no stream, and no agent loop that runs
    forever. Only finite runs, or statements quantified over a horizon, can be
    certified.
  - *Computation beyond Gödel's System T.* Every function the calculus defines
    is provably total in arithmetic in finite types; P5's Step 2 interprets it
    in System T. The Goodstein function, System F normalization and λᶜᵉʳᵗ's
    own evaluator are out of reach. There is no total interpreter for the
    calculus's own codes, by the usual diagonal argument; `reflect` runs only
    certified programs.
  - *Effects:* I/O, state, randomness, concurrency, interaction. Claims about
    the world are only as good as the world's model in the types; this is the
    tiling agents' "naturalistic trust" gap.
  - *Open, hypothetical judgments.* `Check` certifies only closed types over a
    token-only context. Assumptions must be internalized as Π-types.
- **Expressible, but not held.**
  - *Behaviour whose certificate exceeds the budget.* Certificates record every
    judgment with its full context and every conversion step. The D1
    certificate of the `not` literal has 58,180 nodes at budget 35. Anything
    whose typing needs a long computation has a correspondingly long
    certificate.
  - *Proofs known only as codes,* whether found by search, received or
    computed. They are free `Syn` values until minted with tokens. Statements
    about all codes (Con′, code-level reflection) are unprovable (P5).
  - *Proofs in other systems:* a child calculus with other rules, PA, Willard's
    systems. `Check` accepts only λᶜᵉʳᵗ₀ derivations, and no uniform budget
    translates arbitrary foreign certificates.
  - *Erased (usage-0) certificates.* They exist only in types and can be
    arbitrarily large, and `H` and `reflect` refuse them.
- **Held, but not trusted.**
  - *As built, every proposition other than 0.* With the ordinary-type
    extension, every R-free, ◇-free proposition becomes trustable (item 2).
  - *Claims about resource-consuming behaviour,* i.e. types mentioning R or ◇.
    The current model does not justify them; the caller-charged reading
    (item 1) plausibly covers the first-order ones.
  - *Claims about certified evaluation:* types whose T-arguments contain
    `reflect`, such as "the certified program computes an even number". Their
    meaning depends on the budget. Returning evidence avoids them.
  - *Uniform operations on trust:* composing two held certificates into one
    (D2), quoting one (D3), or copying one. Per instance these cost extra
    tokens in proportion to size. Uniformly they are unavailable, by
    Proposition 4.4 and Conjecture 4.6.
- **Trusted, but metered.**
  - *Each exercise of trust consumes its certificate* and loses at least one
    token for good, by strict overhead. So a program run in Θₙ exercises trust
    at most n times in total.
  - *Reflected evidence inherits the certificate's usage 1.* A trusted
    universal claim, such as Π(x :ω Nat). T(f x), can therefore be applied at
    one point per certificate.
  - *A reusable variant would be sound, but is not implemented.* Reflect could
    return reusable evidence, Σ(a :ω A). 1. For R-free A this is sound in the
    model, because such values have zero footprint. It keeps runtime tokens
    distinct if restricted to certificates whose derivations use no tokens,
    because the reflected value then captures none.

## Addendum: Higher-Order Reflection, Delegation, Paying For Codes, And Reusable Evidence (2026-09-26)

Four follow-ups to the previous addendum:

1. Check the higher-order case of the caller-charged argument.
2. Restate the consequence for tiling.
3. Say who pays for proofs known only as codes.
4. Say which model makes reusable evidence sound, and note its implementation.

The experiments extend the same scratch copy of `jpt4/sjas` `code/lcert`, and
[`2026-09-26-lcert-reflect-experiments.patch`](2026-09-26-lcert-reflect-experiments.patch)
is updated:

- a third cap mode, `*charged-cap*`;
- `ho_forms.clj` and `higher_order_reflect_test.clj`.

With the patch applied to a fresh copy at `f5a8490`, 62 tests with 523
assertions pass.

### 1. The higher-order case: corroborated in part, refuted beyond that as a proof in this model

- **The charged cap.** A reflected program runs under w′ = w − ‖v‖ + m: its
  caller's cap w, less the tokens the reflection burns.
  - This replaces the lazy construction of the previous addendum. It is fixed
    at the reflect, and w′ < w keeps the model's recursion well founded.
  - It covers every later supply, since at most w − ‖v‖ tokens remain outside
    the certificate.
- **What the Reflect case needs.** Call it Q: V^{w′}_m(A) ⊆ V^w_k(A) whenever
  k − m ≥ w − w′. The reflect term's footprint k ≥ ‖v‖ supplies the gap.
- **How Q decomposes (the assessor's derivation).**
  - *Function types.* For Π(x :₁ A′). B, Q needs N(A′) and Q(B), with the same
    gap. N(A′) is V^w_j(A′) ⊆ V^{w′}_j(A′).
  - *Inputs of inputs.* For Π(y :₁ C). D, N needs M(C) and N(D). M(C) is
    V^{w′}_i(C) ⊆ V^w_i(C).
  - *Where M holds.* M holds for certificates, tokens and data. It fails for
    any certificate-consuming function, whose required range of supplies grows
    with the budget.
  - *Pairs.* For a tensor, Q needs the gap once for each component that
    consumes certificates. For a reusable (ω) component, Q needs M.
- **Corroborated.** Q holds when certificate consumers occur in the target
  type only in three ways:
  - as inputs;
  - as inputs of inputs whose own inputs are certificates, tokens or data
    (second order at most);
  - in results, at most one per tensor and never at usage ω.

  This class contains the delegation type Agent of item 2.
- **Refuted beyond that, as an argument in this model.** In each of three
  shapes, the model's own value for the reflected program lies outside the set
  its type requires. Write H for a certificate consumer that returns a
  certified Σ-pair.
  - *(a) A third-order argument:* (H ⊸ Σ) ⊸ Σ. The model's function space
    contains set-theoretic g that apply h to certificates g does not own. For
    such g, the reflected program returns the default witness.
  - *(b) Two consumers in a tensor:* 1 ⊸ H ⊗ H. Each component must be correct
    up to w, less its share of the footprint. Together they need a gap of
    2(‖v‖ − m), but the footprint supplies only ‖v‖ − m.
  - *(c) A reusable consumer:* 1 ⊸ Σ(f :ω H). 1. An ω component has zero
    footprint at both budgets, so it must be correct up to w. It is correct
    only up to w′.
- **Not refuted as a claim about λᶜᵉʳᵗ.** In each shape, the out-of-range
  behaviour needs either a certificate bigger than any run can still hold
  after the reflection burns its tokens, or, in (a), a function that passes
  on resources it does not own. The tests in `higher_order_reflect_test.clj`
  show both halves:
  - On runnable programs of shapes (a)–(c), the model's own cap returns
    witness 0, with "evidence" of 0 ≠ 0. The charged cap and the caller's cap
    return the certified witnesses: (1, ⋆) in (a), and 1 + 2 = 3 in (b) and
    (c).
  - The reusable consumer of (c) was handed a certificate of more than w′
    nodes, which is more tokens than remain after the reflection. It returned
    the default witness. So the model's value is wrong exactly on the inputs
    no run can hold, and only there.
- **What would settle it.** A model whose function spaces pass on only the
  resources they own, and whose tensors split the remaining budget between
  their components, rather than granting each component the whole of it.
  - Hofmann's resource-counting realizability is the natural candidate; λᶜᵉʳᵗ's
    ◇ comes from it.
  - Types that mention `reflect` need separate care. Barring `reflect` from
    types, since it is needed only at runtime, is a plausible simplification.
  - Not constructed. The higher-order case remains open, with no
    counterexample.

### 2. The consequence for tiling, restated and run

- **The problem.** A parent must approve a successor before the successor
  acts, without predicting what it will do. The successor must be able to do
  the same, with no loss of trust per generation. In theories like PA the
  approval needs a reflection schema that Löb's theorem forbids, so each
  generation must be weaker (Tκ): the telomere.

  > **Corrected (2026-09-27, next addendum).** The weakening tower is
  > Yudkowsky and Herreshoff's T+1, T+2, …. Their Tκ (Fallenstein's
  > parametric polymorphism) keeps syntactically identical theories and
  > decrements a counter κ that may be any natural number. The token supply is
  > the analogue of κ, not an escape from it.
- **An agent is a program whose type is its guarantee.** Take
  Agent = Π(s :₁ R). Σ(b :ω Act). Safe(b): given a token supply, return an
  action with evidence that it is safe. Type-checking the agent is its
  approval: by soundness, every action a well-typed agent returns is safe,
  whatever it computes internally.
- **Trusting a successor.** The agent holds a certificate of the successor,
  received or found, and checks it against Agent with `inspect`. It then
  `reflect`s it at Agent to get a runnable agent, and runs that on the rest of
  its supply. The successor's evidence comes back and becomes the parent's.
- **Approval in advance.** The parent's type-correctness does not depend on
  which successor turns up. `reflect` makes "run whatever certified agent
  arrives" well typed at Agent. So the parent is approved once, before any
  successor exists.
- **It tiles.** The demo uses one agent program at every level. It reads a
  certificate from the front of its supply, checks it, reflects it, and runs
  it on the rest; otherwise it takes a known-safe action. No theory index
  falls per generation: the rule and the trust term are the same at every
  level.
- **What falls instead is tokens.**
  - Each successor's certificate is paid for from the supply its parent passes
    down; the demo's agent certificate has 21,135 nodes.
  - Each reflection burns at least one token.
  - So a chain's depth is limited by the supply it starts with. The cost adds
    up linearly, and delegation continues while fresh supply arrives.
- **Why return evidence, not a theorem about the successor's run.** "For every
  supply, the successor's run is safe" would put the successor's code, which
  itself uses `reflect`, inside a type. The model reads such types differently
  at different budgets, so their certificates cannot be trusted. Returning
  evidence mentions no run in any type; the only type involved is Agent.
- **Why the halving does not apply.** The earlier bound (sizes at least double
  per nesting level) holds for a certificate whose program builds its nested
  certificates from its own declared tokens. An agent that takes its supply as
  an argument has a fixed-size certificate, however deep the chain below it.
- **The run** (`self-similar-delegation`). The successor chain is two agents
  and then a leaf agent that takes action 2.
  - Under the charged cap, delegation through zero, one and two agent levels
    returns (2, ⋆), valid evidence.
  - Under the model's own cap, one or two levels return (0, ⋆): the trusted
    successor's own `reflect` falls back to the default.
  - A certificate that is not an agent's gets the fallback action (1, ⋆).
- **Caveats.**
  - This rests on reflection at Agent, a certificate-consuming type. Item 1
    justifies it on paper for this first-order shape; it is not mechanized.
  - It needs the charged cap: under the model's cap, delegation yields false
    evidence.
  - Safe is about a modelled world (the naturalistic gap).
  - Evidence is single-use, which suffices for a single action.

### 3. Who pays for proofs known only as codes

- **What is free.** A code (`Syn`) is free data. A program can search for,
  receive, compute, copy, print and store codes without tokens. `chk′` checks
  a code against a type at the cost of computation only. It gives runtime
  evidence that the code is a valid derivation, which is verification. Bounded
  code consistency (Proposition 4.9) needs no tokens either.
- **What costs.** Relying on the code's content costs. That means concluding
  the proved proposition (`reflect` at its type), running the certified
  program on its own authority (`reflect` at a data or function type), or
  answering a claimed refutation (`H`, `H₁`). All three take a certificate.
- **How.**
  1. Mint the certificate with the typed parser
     `parse : Π(c :ω Syn). R ⊸ R ⊗ R` (metatheory §4.7). It spends one node of
     a supply certificate per internal node of c.
  2. Check the minted certificate with `inspect` (free).
  3. Consume it with `reflect`. The decoded program gets m of its tokens; the
     other ‖v‖ − m, at least one, are lost for good.

  The bill is the size of the full-judgment derivation, much larger than the
  proof term: the D1 certificate of the `not` literal has 58,180 nodes.
- **Who.** Whoever holds the supply where the trust is exercised. No program
  can create tokens, so every token comes from some program's initial budget.
  Ultimately that is whoever runs the root program: an operator or the
  environment. In a delegation chain the root's supply pays for every level
  below, unless new supply arrives.
- **When.** Only to rely on a proof the program did not itself contain, and
  whose conclusion it cannot simply compute. It need not pay:
  - for proofs written into its own text, which are terms whose conclusions
    hold by typing;
  - for decidable conclusions, which it can compute directly;
  - for checking, search or storage.

  Runtime-found and received proofs of undecidable claims, such as universal
  statements or "this program is an Agent", are exactly the case that pays.
- **Why payment is unavoidable.** Free trust in codes would be code-level
  reflection, T(chk′ c ⌜A⌝) → A. At A = 0 it would prove code-level
  consistency, which P5 rules out. Minting charges the one resource the
  consistency argument counts.

### 4. The model behind reusable evidence, and an implementation note

- **The model.** This is the budget-stratified model of `R4-metatheory.md`
  §3, used only in the consistency proof and never run by programs.
  - *Carriers:* each type gets a carrier set by its simple type (skeleton). The
    carriers are {⋆}, the Booleans, ℕ, labels, code trees (`Syn`), certificate
    trees with ◇ at the nodes (`R`), all set functions, and pairs.
  - *Denotations* are computed under a cap n. `reflect` of a certificate with
    at most n nodes that checks runs the decoded program at the program's own
    budget m; otherwise it gives a fixed default.
  - *Semantic types* V^n_k(A) ⊆ carrier: the values of A that account for at
    most k of the n tokens.
    - V(0) is empty, and V(T(b)) is {⋆} if b is true.
    - V_k(R) holds the trees with at most k nodes.
    - A usage-1 function must be correct on every argument that fits in the
      remaining n − k tokens, with footprints adding.
    - A usage-ω argument must have zero footprint.
  - *Soundness.* Lemma 3.6: every well-typed term denotes a value in the
    semantic type of its type. It is proved by strong induction on n, and
    gives consistency because V(0) is empty.
- **Why reusable evidence is sound in it.** A reusable result must have zero
  footprint: it must lie in V₀(A). For a type without R and ◇, nothing can
  hold tokens, so V_k(A) is the same set for every k and V₀(A) = V(A). The
  reflected value lies in V(A) by the induction hypothesis, so a rule returning
  Σ(a :ω A). 1 passes the Reflect case unchanged.
- **Why also restrict to token-free derivations.** The model counts nodes, not
  individual tokens, so it cannot see tokens captured inside a closure. A
  program decoded from a certificate with m > 0 may capture some of its tokens
  in the value it returns, and reusing that value would reuse the tokens at
  runtime. With m = 0 the decoded program is closed and captures none.
- **Implementation note: reusable reflected evidence (not started).** Target:
  `jpt4/sjas` `code/lcert`, under an ADR in that repository as an R4
  follow-up.
  - *Evidence:* T(chk′ (print r) ⌜A⌝ ∧ budget0 (print r)). `budget0` decides
    whether the root judgment's context is empty; it is definable by `recSyn`,
    or can be a primitive.
  - *Typing rule:* `reflect-w_A r e :¹ Σ(a :ω A). 1`, for closed A in which
    neither R nor ◇ occurs and whose T-arguments contain no `reflect`.
  - *Check:* validate the new rule, with its evidence type.
  - *Evaluator:* run the decoded program with no tokens, and return
    `[:pv value :star]`.
  - *Metatheory:* the Reflect case as above. P5's Step 2 needs the
    fixed-witness repair already required for reflection at ordinary types.
  - *Tests, first:*
    - a trusted Π(x :ω Nat). T(f x) applied at two points;
    - a certificate with budget > 0 refused;
    - R- and ◇-mentioning types refused;
    - the Check round trip;
    - the two evaluators agreeing;
    - no token duplicated at runtime when the reusable value is used many
      times.

## Addendum: What The Token Supply Is, And The Delegation Demo Dissected (2026-09-27)

Two follow-ups:

1. Does the token supply occupy space, or is it only an indicator? What
   changes when a program is given 42,000 tokens rather than 21,000? What
   precludes a budget of 10^100, and what would it do?
2. The delegation demo, dissected, with instructions for running it.

The patch beside this note gains a trace hook (`*trace*`), the narrated
runner `lcert.delegation-demo`, and the unexpanded agent template. It still
passes all 62 tests on a fresh copy of `code/lcert` at `f5a8490`.

### 1. What the token supply is

- **A token carries nothing.** ◇ has one element in the model (C(◇) = {◇}).
  Its only property is that it can be used once. It is a permission, not a
  datum.
- **Tokens are represented in unary.**
  - A program's budget is the length of its token context Θₙ, one distinct
    variable per token.
  - A supply passed at runtime is a certificate tree with one token per node.
  - In `code/lcert`, each token is also a runtime object.
  - In the demo the tokens *are* the certificates' nodes, plus one per spine
    node. There is no separate currency.
- **Where unary is essential.** Inside certificates, a certificate lists every
  token of the context it grants, entry by entry (property E3). So it always
  has more nodes than the tokens it grants (strict overhead). That listing is
  what makes the consistency argument descend. If a certificate could write
  "m tokens" as a number, O(log m) nodes could grant m tokens and the descent
  would fail (R4 draft §3).
- **Where unary is incidental.** The root supply is never inside a
  certificate unless someone certifies the root program. `code/lcert`
  materializes it because that is the simplest correct representation. A
  supply materialized lazily, as it is consumed, would be indistinguishable to
  the program.
- **42,000 tokens rather than 21,000.**
  - The agent program does not change. It is closed, takes its supply as an
    argument, and has a 21,135-node certificate in every run. Only the supply
    argument doubles.
  - Memory in `code/lcert` grows linearly with the supply.
  - Time grows too, but that is an artifact of the implementation, not the
    calculus:
    - `out`, the destructor of R, is defined by iteration, so each agent
      walks its whole remaining supply;
    - the runtime linearity check walks both subtrees of every node it
      rebuilds. That is quadratic on a spine, so the scaling run appends its
      spare tokens as a balanced tree.

  The scaling run (`delegation-demo scaling`): one agent level and the leaf
  agent, under the charged cap, with spare tokens appended.

  | spare tokens | supply tokens | seconds | result |
  | --- | --- | --- | --- |
  | 0 | 21,343 | 3.45 (includes JVM warm-up) | (2, ⋆) |
  | 21,135 | 42,478 | 2.30 | (2, ⋆) |
  | 42,270 | 63,613 | 4.96 | (2, ⋆) |
  | 200,000 | 221,343 | 17.15 | (2, ⋆) |
  | 1,000,000 | 1,021,343 | 51.86 | (2, ⋆) |
- **10^100: nothing in the logic precludes it.** The consistency proof is by
  induction on the budget and covers every finite budget with one argument.
  The calculus is the same at every budget, and a larger budget adds no
  strength.
- **What precludes it is the representation.** `code/lcert` cannot build a
  10^100-node supply. A lazily materialized supply, with a constant-time
  primitive destructor for R, would make such a budget cheap to promise,
  because a run materializes only what it uses.
- **What a 10^100 budget would do.**
  - *Soundness:* unaffected.
  - *The self-trust boundary:* unaffected. Only held certificates are
    trusted, P5 holds at every budget, and the justified reflect types are
    the same.
  - *Delegation depth:* bounded only by physical time and memory. At 21,136
    tokens per agent level, 10^100 tokens would pay for about 4.7 × 10^95
    levels.
  - *So the token count stops constraining anything.* The earlier phrase "a
    resource bill, not a decrement" gave the count too much weight. The
    tokens are the consistency proof's accounting device. What does the work
    is structural:
    - no program can create tokens;
    - a certificate always costs more than the budget it grants;
    - a held certificate cannot be copied for free.
- **Comparison with Fallenstein's Tκ, and a correction.** Yudkowsky and
  Herreshoff §4.2 give Tκ a constant κ intended as any natural number.
  - They prove it sound for every value by induction on the models
    {N, κ=n}. It "demonstrates indefinite tiling without loss of mathematical
    strength using a series of agents employing syntactically identical
    theories".
  - So a large κ costs nothing either. The token supply is the analogue of κ,
    and a 10^100 budget is the analogue of choosing a large κ.
  - It is the T+1, T+2, … tower of successively stronger theories where a
    longer telomere costs strength. The previous addendum's "(Tκ)" label for
    weakening generations was wrong; the pointer added there records this.
  - What differs (the assessor's comparison):
    - *Where the restriction sits.* Tκ weakens the conclusion: a child's
      theorem φ holds only as φ[κ\κ−1], so agents prove their offspring
      "safe for κ steps". λᶜᵉʳᵗ restricts the premise: only a held
      certificate can be reflected, and the evidence returned is for Safe(b)
      outright.
    - *What the agent believes.* A Tκ agent reasons about a specific,
      unknown, finite κ that appears in its goal: the "odd mental state"
      Yudkowsky and Herreshoff object to. A λᶜᵉʳᵗ agent's type does not
      mention its supply. The bound lives in what it holds, not in what it
      believes.
  - What is shared:
    - each run is finite, and unbounded futures rest on the external
      argument that the counter could be any number;
    - soundness is proved outside the system by induction on the counter;
    - neither has reflectively coherent quantified belief (P5 for λᶜᵉʳᵗ;
      Yudkowsky and Herreshoff note Tκ does not address it).

### 2. The delegation demo, dissected

**The files** (all under `code/lcert/test/lcert/` with the patch applied):

- `ho_forms.clj`: the programs. The agent is given both as a template with
  abbreviations and expanded.
- `delegation_demo.clj`: the narrated runner.
- `higher_order_reflect_test.clj`: the assertions.

The switches live in `src/lcert/eval.clj` and `src/lcert/syntax.clj`:

- `*allow-resource-reflect*` lets `reflect` target types that mention R or ◇;
- `*charged-cap*` runs a reflected program under n − ‖v‖ + m;
- `*trace*` reports every `inspect` and `reflect`.

**The types.**

- `sigma` = Σ(x :ω Nat). T(x ≠ 0) is an action x with evidence that it is
  safe. Safety here is the toy predicate x ≠ 0.
- `Agent` = Π(s :₁ R). sigma: given a supply, return a safe action. `Agent`
  mentions R, so reflecting at it needs `*allow-resource-reflect*`.

**The agent, step by step** (SIGMA = sigma, AGENT = Agent,
K = ◇ ⊗ (R ⊗ R), OUT = the definable destructor of R,
FALLBACK = (pair SIGMA 1 star)):

```clojure
(fn [s 1 R]                                    ; the supply, usable once
  (let-pair SIGMA [b q] (OUT s)                ; view the supply's root: b says node or leaf
    (let-pair SIGMA [a f] q                    ; a: the label; f: yields a node's parts
      ((elim-bool [zz (-o (-o (T zz) K) SIGMA)] b
         (fn [g 1 (-o (T tt) K)]               ; a node:
           (let-pair SIGMA [d uv] (g star)     ;   d: its token (discarded)
             (let-pair SIGMA [l r] uv          ;   l: left child, r: right child (the rest)
               (inspect SIGMA l (code AGENT)   ;   runtime Check: is l a certificate of AGENT?
                 [x e] ((reflect AGENT x e) r) ;     yes: trust it, run it on the rest
                 [x e] FALLBACK))))            ;     no: the known-safe action (1, ⋆)
         (fn [g 1 (-o (T ff) K)] FALLBACK))    ; a leaf: nothing to delegate to
       f))))
```

The leaf agent is `(fn [s 1 R] (pair SIGMA 2 star))`. Its certificate has 206
nodes; the agent's has 21,135.

**The supply.** It is a right spine of nodes whose left children are the
successors' certificates, ending in a leaf:

node[agent] → node[agent] → node[leaf agent] → leaf.

Every node of every certificate carries its own token, and so does every spine
node: 3 + 2 × 21,135 + 206 = 42,479 tokens.

**The run, charged cap** (the trace, verbatim numbers):

1. *Level 0.* The top agent runs directly under cap 42,479.
   - It inspects a 21,135-node certificate against Agent: valid.
   - It reflects it. The decoded program declares 0 tokens and runs under cap
     42,479 − 21,135 = 21,344. The 21,135 tokens are burnt.
2. *Level 1.* The successor, the same program, inspects the next 21,135-node
   certificate: valid. It reflects it; the next successor runs under cap 209.
3. *Level 2.* That successor inspects the 206-node leaf-agent certificate:
   valid. It reflects it; the leaf agent runs under cap 3 and returns (2, ⋆).
4. *The way back.* The evidence travels back up unchanged. The result is
   (2, ⋆): action 2, with valid evidence.

**The same run under the model's own cap.** The reflected program runs under
its declared budget, 0.

- Level 1's `reflect` is refused, since 21,135 nodes exceed cap 0. It returns
  the default value of Agent's type: a function yielding (0, ⋆).
- The result is (0, ⋆): action 0, with "evidence" of T(0 ≠ 0). This is the
  "empty supplies" limitation, made visible.

**What it shows.**

- One agent program, approved once by type-checking, delegates repeatedly to
  successors known only by their certificates.
- Each successor is trusted by `reflect` after a runtime `inspect`.
- The leaf's action returns with valid evidence.
- Tokens are consumed exactly as certificates are.
- A certificate that is not an agent's is rejected by `inspect`, and gets the
  fallback action (tested).

**What it does not show.**

- That reflection at Agent is sound: that is the paper argument of the
  previous addendum, not mechanized.
- That successors can be discovered. The environment supplies the
  certificates; minting found codes with the typed parser is skipped.
- Realistic safety (x ≠ 0), or efficiency (21,135-node certificates, and
  `out` linear in the supply).

**How to run it.**

```sh
git clone https://github.com/jpt4/sjas.git && cd sjas
git checkout f5a8490            # sjas-codification, as assessed
patch -p1 < /path/to/proflog/docs/log/2026-09-26-lcert-reflect-experiments.patch
cd code/lcert
clojure -J-Xss1g -J-Xmx3g -J-Dstdout.encoding=UTF-8 -M:test -m lcert.delegation-demo chain
clojure -J-Xss1g -J-Xmx4g -J-Dstdout.encoding=UTF-8 -M:test -m lcert.delegation-demo scaling
clojure -J-Xss1g -J-Xmx3g -M:test -m lcert.test-runner fast lcert.higher-order-reflect-test
```

Without the Clojure CLI, put `clojure-1.12.0.jar`, `spec.alpha-0.5.238.jar`
and `core.specs.alpha-0.4.74.jar` (Maven Central) on the classpath with `src`
and `test`, then run
`java -Xss1g -Xmx3g -Dstdout.encoding=UTF-8 -cp <jars>:src:test clojure.main -m lcert.delegation-demo chain`.

From a REPL, `(lcert.delegation-demo/run-agent mode supply-code trace-fn)`
runs one chain. For example:

```clojure
(require '[lcert.delegation-demo :as d] '[lcert.ho-forms :as f])
(def ca (d/code-of f/agent-form))   ; the agent's certificate, as a code
(def cl (d/code-of f/leaf-agent))
(d/run-agent :charged (d/supply-code [ca ca cl]) prn)   ; prints each event; result (2, ⋆)
(d/run-agent :fixed   (d/supply-code [ca ca cl]) prn)   ; result (0, ⋆)
```

## Addendum: Does An Arbitrary, Lazily Produced Budget Resolve The Obstacle? (2026-09-27)

The user's claim, verbatim:

> The ability to specify an arbitrary token budget, produced lazily, seems to
> substantially resolve the Tiling Agents/Lobian Obstacle problem, for all
> agents feasible within the physical universe.

**Assessment: it resolves one part of the problem, the telomere for tiling
with an identical system.** That rests on two conditions not yet met, and the
same part was already resolved in principle by Fallenstein's Tκ. The other
parts remain.

### What it resolves

- **Indefinitely tiling trust with an identical system.**
  - One agent program, approved once by type-checking, can trust successors
    running the same calculus. The successors can trust theirs by the same
    rule, with no per-generation weakening.
  - The count of tokens is the only thing that falls. A budget chosen
    arbitrarily large, and materialized only as used, makes that count
    irrelevant for any lineage whose certificates are physically buildable.
  - This is Yudkowsky and Herreshoff's A1 ≅ A0 target.
- **What is new relative to Tκ.** Tκ already tiles indefinitely with
  syntactically identical theories, sound for every κ (§4.2, "without loss of
  mathematical strength"). λᶜᵉʳᵗ adds two things.
  - *Epistemic:* the bound is not in the agent's goal or beliefs, and the
    evidence it returns is for Safe(b) outright, not "safe for κ steps". This
    answers Yudkowsky and Herreshoff's "odd mental state" objection to Tκ.
  - *Computational:* agents are typed programs, and trust is a runtime
    `inspect` plus `reflect`.

### Conditions not yet met

- **The key rule is not established.** `reflect` at Agent, a
  certificate-consuming type, is not in λᶜᵉʳᵗ as built. Its soundness under
  the charged cap is the assessor's paper argument for first- and
  second-order types: not written up formally, not mechanized, not reviewed.
  P5 for the extension is repaired on paper only for types without R and ◇.
- **Laziness needs a calculus change.** The definable destructor `out`
  iterates over the whole supply, so it would force a lazy supply to
  materialize in full. A lazy budget therefore needs a primitive,
  constant-time destructor for R; ADR-0005 lists it as a follow-up. That
  changes the calculus, and its metatheory must be re-checked.
- **The promise must be finite.** The consistency argument covers every
  finite budget, however large. An unbounded generator is outside it. It also
  loses liveness: a chain that keeps finding its own certificate in an endless
  supply delegates forever and never acts. That is safe, but never done.

### What remains

- **Code-level quantified self-trust.**
  - The agent cannot prove, as one theorem, that every code which checks as
    an Agent is safe. That is P5, at every budget.
  - With unlimited tokens it can mint and trust any code it actually
    obtains, but only as a runtime act, never as a belief about all codes.
  - Tκ lacks this too; Yudkowsky and Herreshoff note that it "does not
    address reflectively coherent quantified belief".
- **Successors with a different proof system.**
  - Every generation shares one checker. A successor with improved or
    extended rules produces certificates the parent's `Check` rejects.
  - Translating them costs more than any fixed number of tokens can cover
    (the child-efficiency addendum).
  - Self-improvement of the reasoning system itself, the case the programme's
    RSI motivation cares about, is untouched.
- **Naturalistic trust.** Safe(b) is a proposition about a modelled world, and
  the checker's implementation is trusted from outside.
- **The physical bound is certificate size, not tokens.**
  - Certificates record every judgment and conversion step: a trivial agent's
    has 21,135 nodes.
  - Agents whose types compute have correspondingly larger certificates.
  - "Feasible within the physical universe" must therefore be read as "whose
    certificates are feasible". A more compact encoding that keeps strict
    overhead is an open item of R4.

### What would make the claim solid

1. Write the charged-cap model and prove the Reflect case for first-order
   resource types. Mechanize the key inclusion.
2. Add a primitive destructor for R, re-check the metatheory, and give lazy
   finite supplies a semantics.
3. Settle the higher-order case, for which a resource-counting realizability
   model is the likely route.
4. Test tiling across a changed proof system, to mark exactly where trust
   stops.
