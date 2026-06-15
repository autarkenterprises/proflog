# Interdev: Proof of the D_SJAS Rule-Family Soundness Clauses (ADR-0108 Lemma 2)

- Date: 2026-06-14
- From: review of the `D_SJAS` self-justification line (ADR-0104/0108/0109)
- To: the parallel agent owning `D_SJAS`
- Purpose: discharge the per-rule open-branch preservation clauses that ADR-0108
  (`dsjas-quantitative-rule-family-preservation`) currently records as
  `:status :proved` one-liners. This note turns them into actual lemmas, so they
  can be referenced rather than asserted. Anchors are **relation names** (robust
  to checkout divergence); line numbers are `main @ 39d8593`, approximate.

## What must be proved (the obligation Fact D.3 imposes)

ADR-0108's Lemma 2 generalizes Willard's Fact D.3 (the `Normed(a,b)` open-branch
lemma, willard2011 Appendix D) from ordinary semantic tableaux to `D_SJAS`.
Fact D.3 builds, from the `Normed(a,b)` hypothesis, a model **M** — the standard
model, faithful on Δ₀ facts for arguments ≤ the U-height bound — of `Z`'s axioms,
and shows a deduction tree with Gödel number `< (a/b)⁴` cannot close every branch
that M satisfies. The per-rule obligation is therefore exactly:

> **(Sound↓ w.r.t. M).** For each `D_SJAS` rule and each branch true in M under
> some assignment: a **closure** rule does **not** fire, and an **expansion** rule
> leaves **≥ 1** child branch true in M.

Equivalently: *closure only on M-unsatisfiable branches; expansion preserves
M-satisfiability.* This is the satisfiability-preservation Willard's proof of
Fact D.3 establishes by examining his eight elimination rules; `D_SJAS` has more
families, so each must be re-checked. The clauses below do that.

It is worth separating two properties the ADR-0108 one-liners conflate:
**Δ₀-boundedness** (a complexity claim, used by Lemma 1 / the `Normed` parameters)
and **satisfiability-preservation** (the soundness claim Fact D.3 needs). Only the
latter is proved here; it is what Lemma 2 actually requires.

## Per-family lemmas

**Family 1 — base tableau (α, β, ¬¬, →, complementary-literal closure,
root/child).** Classical, model-independent tableau soundness: α keeps both
conjuncts; β (`sjas-...` disjunctive split) keeps ≥1 disjunct; `¬¬` and `→`
standard; complementary closure (`sjas-complementary-lit-close-coreo`) fires only
on `{p, ¬p}`, which is M-false. **Sound, outright.**

**Family 2 — truth normalization.** Closes on `false` / `¬true` (M-false) and
skips `true` / `¬false` (M-true). **Sound, outright.**

**Family 3 — quantifiers (γ, δ, bounded γ/δ, negated duals).** Classical
soundness; the only subtlety is δ-witnesses, which Willard's framework keeps
**U-grounded** (≤ bound) so they exist in M's domain. This is the literal
bounded-witness tableau step. **Sound, outright (within the bounded-witness
discipline).**

**Family 4 — equality / disequality. (Upgraded: verified, not asserted.)**
This is the family with a *prima facie* unsoundness: closing `eq(sub(2,1), 1)` by
free-constructor contradiction would be unsound because `sub(2,1)` and `1` denote
the same element of M. The apparatus avoids it **structurally**:
- `sjas-neq-close-coreo` closes `neq(l, r)` **iff `sjas-normal-equalo l r`** — i.e.,
  `l`, `r` normalize to the *same* numeral. So a disequality closes only when it is
  M-false.
- `sjas-eq-progresso` consumes `eq(l, r)` only via the same arithmetic
  normalization (its docstring states the design reason precisely: the generic
  free-constructor layer "is sound for ordinary Proflog programs but wrong for the
  SJAS U-grounding profile, where `sub(2,1)` and `1` denote the same number").
- The `equality/*` namespace appears in the SJAS profile **only** as `walk`/`unify`
  (substitution mechanics), never as a free-constructor *contradiction* closure.
  The unsound rule is simply not in `D_SJAS`.
**Sound** (modulo `sjas-normal-equalo` deciding standard equality — see the
standard-model-soundness note).

**Family 5 — arithmetic / relation closure. (Upgraded: verified.)** The relations
are exactly `{mult, leq, lt}` — the Δ₀ Add/Mult-graph relations Willard's
**Definition 3.4(2) requires**. Two-sided closure
(`sjas-neg-relation-close-coreo` / `sjas-pos-relation-close-coreo` via
`sjas-relation-holdso` / `sjas-relation-failso`):
- `holdso` fires (closing `¬R`) only when R is standard-true on the decoded
  numerals; `failso` fires (closing `R`) only when R is standard-false
  (`expected = a·b`, then `distinct-num-bitso expected product`).
- **Critical guard:** `failso` requires **ground args** — every `sjas-num-inputo`
  call demands an *empty* pending-bind output, so "the checker cannot make a false
  atom close by assigning an open proof variable." This blocks the only obvious
  unsoundness (closing `R(x)` by choosing a falsifying `x`).
- Branch terms are U-grounded ≤ bound, and M is faithful to the standard model on
  Δ₀ facts ≤ bound, so the computed truth value equals M's.
**Sound w.r.t. M** (modulo the arithmetic primitives computing standard
arithmetic — the standard-model-soundness note). NB: a decidable mult *graph* is
required and does **not** confer multiplication *totality*; the Gödel-2 evasion is
untouched.

**Family 6 — axiom membership.** `axiom-member(S, F)` closes `¬axiom-member(S, F)`
when `F` is in the decoded basis of `S` (`sjas-walked-axiom-membero`); citing an
axiom adds an M-true sentence. **Sound modulo** faithful decoding + `Bξ` axioms
true in M (Willard Def 3.4(3)).

**Family 7 — reflected calls. (Residual resolved.)**
`reflected-clause-formulao` reconstructs each clause as the axiom
`∀args. (body → head)` (`head = (pos (app relation args))`). Using it is the
**classical β-rule on a standard-true implication axiom** (branches to
`¬body | head`, ≥1 M-true); the negative call `¬head ⊢ ¬body` is its
**contrapositive**. So **no Clark completion / negation-as-failure is assumed** —
the apparent closed-world risk dissolves once the clauses are seen to be ordinary
implication axioms. **Sound modulo** faithful decoding + reflected-clause axioms
true in M (same assumption as family 6).

**Families 8–9 — recursive `tableau-proof/3`, `subst-prf/4`.** Induction on
proof-call-graph height (ADR-0104 `dsjas-recursive-well-foundedness`, least fixed
point over finite acyclic proof-call graphs). Height 0: families 1–7. Height
`n+1`: the proof-predicate atom's truth is decided by a strictly-smaller-height
check, sound by IH; closing on a polarity mismatch is then closure on an M-false
literal. **Sound, reducing to 1–7 + well-foundedness.**

**Family 10 — substitution support.** `subst-code` is a decidable syntactic
operation (decode, replace the distinguished variable, compare modulo α). **Sound
modulo** faithful substitution decoding.

## Verdict

Every `D_SJAS` rule family preserves the M-open branch, so **Fact D.3 extends to
`D_SJAS` and ADR-0108's Lemma 2 holds.** No counterexample was found. The two
families that carried genuine risk (4 equality, 5 arithmetic) are **positively
sound by verified implementation guards**, not by assertion; family 7's NAF
concern is dispelled by the implication-axiom reconstruction.

The soundness reduces, with nothing left over at the *rule* level, to assumptions
about the **system** and the **arithmetic kernel**:
1. the decoded `Bξ` / reflected-clause axioms are true in the standard model
   (Willard Def 3.4(3); ADR-0108 `:external-beta-pi-star-1-truth`); and
2. the U-grounding numeral primitives (`sjas-num-inputo`, `arith/*o`,
   `arith/<=o`, `arith/<o`, `sjas-normal-equalo`) compute standard arithmetic on
   the bit encoding (ADR-0108 `:standard-model-soundness-of-ugrounding-primitives`).

(2) is discharged separately in the standard-model-soundness note dated the same
day; (1) is the irreducible semantic premise Willard's Theorem 6.3 also requires
(willard2011: consistency-preservation holds when ξ is EA-stable *and* the
sentences are "true in the Standard-Model").

## Suggested integration

Replace each `:status :proved` string in
`dsjas-quantitative-rule-family-preservation` with a `:lemma` reference to the
corresponding family above, and split each clause's claim into
`:satisfiability-preservation` (proved here) and `:delta0-bounded` (Lemma 1 /
`Normed`). The two reductions (system-axiom truth; arithmetic-kernel soundness)
should be the only `:status :assumed`/standing entries that remain under Lemma 2.

Epistemic status: a rigorous semantic argument grounded in the actual relations,
not machine-checked — the same standing as the rest of the `D_SJAS` line.
