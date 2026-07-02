# ADR-0147: Theorem 2.3 BOT Closure — Architecture Established, Assembly Wall Measured

- Status: in progress (architecture + evidence; no closure claimed, no ledger flips)
- Date: 2026-07-02
- Branch: `adr-0147-theorem23-bot-closure` (off `adr-0146-sjas-boundary-behavioral-contrast` @ `cf72b81`)
- AAR: [AAR-0147](../aar/AAR-0147-theorem23-bot-closure.md)

## Context

The instruction was to implement the full Theorem 2.3 BOT closure — the ledger's
open boundary (cut-free combination trees for steps 1/3/4/B, step 5 gated on
step 4). This ADR records what a full-effort session established: the **complete,
code-grounded construction architecture** (new), several **live-verified
mechanical facts** (new), and a **measured assembly wall** at the real-diagonal
scale (new, reproducible). It makes no closure claim; per the ADR-0141/0142
discipline no ledger status changes.

## The architecture (the session's principal result)

**1. `subst-prf`'s structural arm fixes every inner-proof target.** From
`sjas-subst-prf-coreo` (profile `:9628`): a structural (non-citation) proof code
`P` for `subst-prf(sys, G, F, P)` is accepted iff `P`'s decoded tree **closes
`(AxiomConj ∧ SubstAnt(G)) ∧ ¬F`**. So a "measured proof of F" is exactly a
closed tableau over the axiom conjunction plus the substitution antecedent,
against ¬F. This grounds the whole construction in objects the existing checker
already validates.

**2. The final collision is measured-vocabulary, four-premise.** The generated
SelfCons (mul profile) is the Level-1 measured form
`∀x y p q. ¬Π*₁(x) ∨ ¬NegPair(x,y) ∨ ¬SubstPrf(sys,nbar_sc,x,p) ∨
¬SubstPrf(sys,nbar_sc,y,q)`. Its refutation is a premise-clash tree over
`P ∧ SelfCons` with `P = Π*₁(x₀) ∧ NegPair(x₀,y₀) ∧ SubstPrf(…,x₀,p₀) ∧
SubstPrf(…,y₀,q₀)`: gamma with fresh vars, beta-split, all four branches close
by complementary clash against the premises (the step-5 pinned mechanism). With
`x₀ = code(SelfCons)`, `y₀ = code(¬SelfCons)`: **p₀ already exists** — the
fixed-point axiom-citation, accepted by the (pre-existing, `^:slow`)
`sjas-subst-prf-checks-selfcons-fixed-point-certificate`. **q₀ is the crux**:
by fact 1, its inner tree must close `(AxiomConj ∧ SubstAnt) ∧ SelfCons` — the
inconsistency tableau itself.

**3. q₀'s inner tree is the V5-route, closed by clash + interpretation.**
V5 (= `∀y z α k. [FinAx4(α) ∧ α≤k ∧ Paradox(y,z,α,k)] ⇒ ∃p≤z (p<z ∧
SemPrf_α(BOT,p))`, with `Paradox = ∃d≤z (d<z ∧ Map(α,k,d) ∧
SemPrf^k_α(d,y,z))`) is beta-split after gamma; because the structural
checker's gamma is **fresh-variable-only** (verified: `gamma-terms` is threaded
but never consumed for term instantiation in the structural arms), every
antecedent branch must close by clash against a positive on the trunk or by a
ground-argument interpreted closure. The Paradox branch bottoms out at
`¬SemPrf^k(code(Dk), p̄, 2^(p̄+1))` — the interpreted V-route on the
**constructed bounded proof of the diagonal** (ADR-0146's demonstrated
mechanism), where `p̄ = code(T_p̄)` and `T_p̄` is the proof-of-Dk tree.

**4. `T_p̄` (step 4) closes without deeper proof objects.** Refuting
`α ∧ ¬Dk`: the ¬Dk deltas put **positive** `Subst(nbar,h₀)` and
`SemPrf^k(h₀,y₀,z₀)` on the branch; V3 binds `h₀ = code(Dk)`; the V5-instance's
Paradox branch closes by clash against the branch's **own** positive `SemPrf^k`
(named decode, post-pow-vocabulary) — no nested proof object. The recursion
grounds at depth 2.

**5. The one formalization gap: the (A)-side literal.** Every route's final
collision needs `¬SemPrf_α(BOT,·)` in clashable form against V5's consequent
witness. The measured Level-1 SelfCons does not surface it. The candidate
resolution — auditable against jsl2, since it is literally Willard's condition
(A) — is adding the **Level-0 SelfCons** (`∀p ¬SemPrf_α(BOT,p)`) as a reflected
beta axiom **on both sides** (both Willard systems assert their own
consistency; the falsifier survives because the addition side lacks V5 and so
never produces the positive witness). The known trivial-closure vector (target
`AxiomConj ∧ ¬SelfCons-L0`) must be pinned excluded — demonstration targets
stay L1/AxiomConj-only. **This is a formalization decision requiring the
ADR-0142 C1-style audit before implementation** — deliberately not made
unilaterally here. A doc-vs-code tension was also found and must be resolved in
that audit: the falsifier-guardrail log describes `semprf-alpha` as
profile-local, but `reserved-coding-symbols` (willard_sjas_code.clj:136) lists
it in the globally-reserved boundary cluster; the empirical guard
(trivial closure measured false; ADR-0146's pinned falsifier) currently holds
regardless of which description is accurate.

## Live-verified this session (warm REPL, full mul system, depth-1)

- Step 2 `Subst(nbar, code(Dk))` holds through the public kernel query path.
- `theorem23-diagonal` produces the real Dk:
  `∀h y z (Subst(nbar,h) ⇒ ¬SemPrf^k(sys,k,h,y,z))`, ~7 749 `app` nodes
  (embedded codes dominate).
- Canonical gamma-child formulas for the real Dk are programmatically
  generable (`canonical-formula` with binder-nom → `v0/v1/v2` maps; Tie fields
  `.-binding_nom`/`.-body`).
- The fixed-point `p₀` citation acceptance is already pinned by the existing
  `^:slow` test.

## The measured wall (honest, reproducible)

The **real-diagonal not-Dk tree** — the exact step-5 shape, scaled from the
pinned small-universal test (2.5 s) to the real Dk with premises
`P1 = Subst(nbar, code(Dk))`, `P2 = SemPrf^k(code(Dk), cite, 2^(cite+1))` —
did **not close within 600 s** (fuel 120, warm REPL, post-ADR-0144/0145
checker). Cause undiagnosed between: (a) a canonical-child mismatch sending the
checker into exhaustive alternatives (most likely — a mismatch converts
acceptance into whole-space exhaustion), or (b) genuine scale cost of ~7.7K-node
formulas re-encoded/re-matched per node through the structured-head matcho
ladder and the forall strategy arms (the ADR-0145 residuals). Diagnosis is
mechanical with the fast REPL loop (bisect: validate each subtree prefix
bottom-up) but was out of session budget.

## Consequences / the ordered path to closure

1. **Diagnose the step-5 wall** (subtree bisection; fix mismatch or extend the
   perf line to giant-formula nodes — the ADR-0145 residual quantifier-arm and
   compound-matcho items are the likely levers).
2. **Audit + add the (A)-bridge** (Level-0 SelfCons as reflected beta on both
   sides; falsifier tests for the excluded trivial target; resolve the
   `semprf-alpha` doc-vs-code tension).
3. **Verify ground-argument interpreted closures** for `FinAx4`, `leq`, `Map`
   on the concrete instances the V5 branches need (Map's relational semantics
   vs the diagonal is unprobed).
4. **Assemble `T_p̄`** (step 4; closes at depth 2 per fact 4), encode as `p̄`.
5. **Assemble `T_q`** (the V5-route inconsistency tableau) with `p̄` at the
   Paradox leaf; wrap as `q₀`.
6. **Final four-premise tree** (fact 2) + falsifier pair (ADR-0146 harness) +
   `theorem23-closure-status` flips with evidence.

Each stage lands independently with its own checker-verified test; ADR-0146's
contrast namespace pins each stage's side-pair behavior.

## Test obligations

None land in this ADR (docs only; no source changes). The next ADR in this
line lands stage 1 with a pinned real-diagonal tree test.
