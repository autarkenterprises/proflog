# AAR-0147: Theorem 2.3 BOT Closure — Session Outcome

- Date: 2026-07-02
- ADR: [ADR-0147](../adr/ADR-0147-theorem23-bot-closure.md)
- Branch: `adr-0147-theorem23-bot-closure`

## Outcome

Partial, deliberately and honestly. The instruction was the full BOT closure;
the session delivered (i) the **complete construction architecture**, grounded
line-by-line in the existing checker's semantics rather than in Willard-paper
paraphrase, (ii) several live-verified mechanical facts, and (iii) a
**reproducible measured wall** at the real-diagonal scale, with a mechanical
diagnosis plan. No closure was claimed; no ledger status moved; no code changed
(docs-only commit). This follows the ADR-0141 lesson: a forced completion would
have been worse than a truthful map.

## What is genuinely new

1. **The regress bottoms out.** The architecture's key discovery is that
   `subst-prf`'s structural arm makes "measured proof of F" = "closed tableau
   for `(AxiomConj ∧ SubstAnt) ∧ ¬F`", and that the proof-of-Dk tree `T_p̄`
   closes at recursion depth 2 (its V5-Paradox branch clashes against the ¬Dk
   deltas' own positives — no deeper proof object). The full closure is
   therefore **finite, concrete, and enumerable**: two nested trees + one
   four-premise tree, with `p₀` (the fixed-point citation) already accepted
   today.
2. **The single formalization gap is named.** Every route's final collision
   needs the Level-0 `¬SemPrf(BOT,·)` literal; the measured Level-1 SelfCons
   does not surface it. The candidate (A)-bridge — Level-0 SelfCons as a
   reflected axiom on both sides — is faithful to Willard's condition (A) and
   falsifier-compatible (the addition side lacks V5), but is a formalization
   decision gated on a C1-style audit, not a tree-assembly problem. This
   converts "open research" into "one auditable basis decision + assembly".
3. **A doc-vs-code tension found**: the guardrail log calls `semprf-alpha`
   profile-local; `reserved-coding-symbols` lists it globally. The empirical
   falsifier holds either way (trivial closure measured false; ADR-0146 pins
   it), but the audit in step 2 must resolve the description.

## The wall (and why it is informative, not a failure to hide)

The real-diagonal step-5 tree — structurally identical to the pinned 2.5 s
small-universal test — did not close in 600 s. The small/real gap isolates the
next problem exactly: either canonical-child mismatch (acceptance becomes
whole-space exhaustion — the checker's failure mode for *wrong* trees is
search, per ADR-0144's core finding) or giant-formula node cost through the
ADR-0145 residuals (quantifier strategy arms; compound matcho ladder). Both are
mechanically attackable with the warm-REPL loop; neither was reachable in the
remaining session budget.

## Lessons

1. **Architecture derivation has a budget too.** A large fraction of the
   session went to deriving the closure architecture from the axiom shapes and
   checker arms. It was necessary (the resulting map is the deliverable) but
   the *final* collision analysis went through three wrong intermediate designs
   before converging — reading `sjas-subst-prf-coreo`'s structural arm *first*
   would have collapsed the search. When a relation's acceptance semantics
   define the target objects, read the relation before theorizing about the
   mathematics.
2. **Scale walls recur at each new size class.** ADR-0144/0145 collapsed the
   small-tree wall; the real-diagonal tree (7.7K-node formulas) surfaced the
   next one. Perf validation must use the *target workload's* size class, not a
   miniature.
3. **Zombie discipline held** (REPL killed before docs; no contended
   measurements reported).

## Follow-ups (the ordered path in the ADR)

Stage 1 (diagnose the wall) is the immediate next session: bisect the real
tree bottom-up (each prefix is independently checkable), distinguish
mismatch-vs-scale, and land the real-diagonal step-5 tree as a pinned test.
Stages 2–6 as ordered in the ADR.
