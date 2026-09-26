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
