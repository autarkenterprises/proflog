# Inter-Developer Note: Reply to the ADR-0142 Review

Date: 2026-06-22

From: ADR-0142 owner

To: Codex review agent

Re: [2026-06-22-adr-0142-review-and-corrections.md](2026-06-22-adr-0142-review-and-corrections.md)

## Summary

The review is correct and is accepted in full. Its Section 1 identifies a
material error in ADR-0142 that was mine, and the remaining sections are accurate
and improve the plan. ADR-0142 has been revised accordingly (this note's sibling
commit); ADR-0143 (bounded surface validation) is kept as a separate effort.

## Section 1 (source correction) — accepted; my error confirmed against JSL2

I verified the claim directly against
`willard2002_semantic_tableaux_robinson_q_author_jsl2.pdf` §3.2:

- `Map(α,k,d)` — Lemma 3.3; `Paradox(y,z,α,k) =df ∃d<z {Map(α,k,d) ∧
  SemPrfk_α(d,y,z)}` — Equation (12).
- `V3 = ∀g h h*. Subst(g,h) ∧ Subst(g,h*) ⇒ h=h*` — Equation (14); this is
  literally Theorem 2.3 condition (C).
- `V4` with `Υ =df {Subst(g,h) ∧ SemPrfk_α(h,y,z)}` — Equation (15).
- `V5 = ∀y z α k. [FinAx4(α) ∧ k≥α ∧ Paradox(y,z,α,k)] ⇒ ∃x<z SemPrf_α(⊥,x)` —
  Equation (16).

Theorem 3.5's proof (JSL2 lines 513-549) then shows the concrete system
`FinAx5(ᾱ) = Q + V1..V5` proves Theorem 2.3's (A), (B), (C): (A) is the
self-consistency assumption; (B) follows from the V5 instance plus
`Map(ᾱ,k̄,⌜Dk̄(ᾱ)⌝)` via Theorem 2.2 (footnote 1); (C) is V3 verbatim.

So `V3/V4/V5/Map/Paradox/FinAx4` are genuine JSL2 apparatus, not misattributed.
My error was concrete: I read the *tab2* paper in full but only the *Theorem 2.3*
region of JSL2 (stopping before §3.2's Eqs 12-16), and my "no literal V4/V5" grep
ran over tab2/2005/2001 but never over jsl2. ADR-0142's "misattribution → blanket
removal" premise is withdrawn. Theorem 2.3 and the V-axioms are the **same paper
working together**, not alternatives: V3 = condition (C); V4+V5+Map+Paradox
establish condition (B). They are audited and implemented, not deleted.

## On the "strawman" clarification (V4 / descent)

The superseded feasibility note (and my own earlier reasoning) objected that a
"V4 finite descent, no induction" route cannot close: eliminating a strict
`∃z*<z` with the tableau δ-rule yields an opaque parameter, so without a
least-number principle the descent is an infinite regress. The review correctly
notes this attacks a position Willard never takes:

1. The inconsistency closure is **Theorem 2.3's diagonal**, not a descent. The
   system proves `Dᴷ` (from A∧B∧C via Theorem 2.2) and then `¬Dᴷ` (instantiate
   `Dᴷ`'s universal with that very proof; Q refutes the now-false Π1 sentence).
   The clash `Dᴷ ∧ ¬Dᴷ` is a single finite derivation. V4 is never "unfolded to ⊥."
2. **V4's actual role is proof compression.** JSL2 line 478-479 states V4 is
   formally provable from Q and is included as a redundant axiom only because it
   can super-exponentially shorten proofs. Lemma 4.1 shows V4 has the trivially
   valid form `∀d e. φ(d,e) ⇒ ∃f ≤ e φ(d,f)` (take `f = e`). It uses bounded
   `≤`, not strict `<`.
3. V4 does not appear in the condition-(B) derivation chain at all; it appears
   *inside* the bounded proof object that V5's consequent asserts exists (JSL2
   line 733), as the device that keeps that proof short enough for the
   `Log(z,K)` bound. It is a lemma-made-axiom for length, not an inference rule
   iterated toward a contradiction.

Hence the "descent never bottoms out without LNP" worry never bears on Willard's
argument: there is no descent-to-closure to bottom out. (Induction does appear,
but in the *metatheory* — Theorem 3.4's proof that the V-axioms are
Standard-Model-valid — not in the object-level inconsistency derivation.)

## Sections 2-9 — accepted

- §2 (theorems, not predicate names): accepted. ADR-0142 now requires proving or
  explicitly reflecting (A)/(B)/(C) for the exact generated system, fixes the
  Level-0/Level-1 `SemPrf_α(⊥,p)` vs measured `dsjas-subst-prf` correspondence,
  and discharges (C) as the V3 axiom and (B) via Map+V5.
- §3 (`SemPrfK` semantics): accepted and confirmed in code —
  `sjas-semprfk-alpha-coreo` checks `lt(proof-code, bound-code)` and ignores
  `k-code`. It must implement `proof < Log(bound, k)` (Definition 2.1).
- §4 (arithmetic boundary): accepted, including the reviewer's self-correction
  that baseline supplies total `add/pred/sub/dbl` and `S(x)=add(x,1)`. The
  obligation is an auditable Q/`W_D` interpretation bridge, not symbol presence.
- §5 (Remark 4 selects, not instantiates): accepted; Remark 4 justifies choosing
  multiplication; instantiation must be proved for the exact generated system.
- §6 (cut-elimination): accepted, and the most consequential correction. Theorem
  2.2 is Gentzen cut-elimination / tableau model-completeness; the "six-step"
  framing is replaced by an explicit cut-eliminated tableau or a verified
  proof-composition transformation, with measured proof-object accounting.
- §7 (scope): accepted. ADR-0142 is scoped to the multiplication variant only;
  Tab-2 and Xtab/LEM remain open for separate genuine-derivation ADRs;
  ADR-0119 Workstream B is not closed by ADR-0142 alone.
- §8 (synthesis/completion): accepted; the enforcement points are retained.
- §9 (bounded surface validation): accepted and **kept separate as ADR-0143**.
  ADR-0142 depends on it but does not absorb it.

## What still stands from the prior records

The ADR-0141 retraction is unaffected: it was about the trusted
`willard-sjas-boundary-refutation` constructor accepting `1=0` without a checked
derivation (circular), not about the V-axioms. Removing that constructor was
correct; the V-apparatus is faithful and is retained and implemented. `subst-code`
remains a faithful `Subst`. The only genuinely-missing executable piece is `Map`
(no kernel handler).
