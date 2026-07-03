# ADR-0147 step-1 research: the falsifier guardrail blocks Willard's own (A)+(B) closure

Date: 2026-07-03. Branch `adr-0147-claude-step1-tree` (worktree off `1926cfe`).
Author: Claude. A genuine construction attempt at step 1, GROUNDED in the JSL2
audit. NOT a closure. (Supersedes an earlier draft of this note that proposed a
wrong "align V5 to SemPrf^k" fix -- see the correction below.)

## Willard's conditions, from the JSL2 audit (interdev 2026-06-22-adr-0142-review)

    (A) forall p. not SemPrf_alpha(BOT, p)                       ; UNBOUNDED SemPrf
    (B) (exists y z. SemPrfK_alpha(code(Dk), y, z))              ; bounded antecedent
        -> exists x. SemPrf_alpha(BOT, x)                        ; UNBOUNDED consequent
    (C) forall g h h*. Subst(g,h) AND Subst(g,h*) -> h = h*

Step 1 (Eq 6) is D* = `forall y z. not SemPrf^k(code(Dk),y,z)` by modus tollens
on (A) and (B). Its tableau refutes `A ∧ B ∧ ¬D*`:
- `¬D*` δ-expands to `SemPrfK(Dk, p, b)`;
- (B) beta-splits; the consequent branch δ-expands to a positive, **abstract**
  witness `SemPrf_alpha(BOT, w)`;
- (A) gamma-instantiates to `not SemPrf_alpha(BOT, w)`;
- **leaf (iii): the two SemPrf_alpha(BOT,w) literals clash -> BOT.**

## Correction: V5's consequent is FAITHFULLY unbounded (do NOT change it)

An earlier draft proposed aligning V5's consequent to the bounded `semprfk-alpha`
because only the k-version interpreted leaf-closure fires. **That is wrong.**
JSL2 (A) and (B)'s consequent both use the UNBOUNDED `SemPrf_alpha(BOT,·)`; the
witness is bounded by the existential `exists x<z`, not by the predicate. The
current `total-multiplication-willard-v5-axiom` already emits exactly this
(`exists proof<z. lt(proof,z) ∧ semprf-alpha(alpha,BOT,proof)`) and is faithful.
Aligning it to `semprfk-alpha` would distort Theorem 2.3. (Recorded so the wrong
fix is not retried.)

## The real obstruction: the guardrail blocks Willard's own closure

Leaf (iii) is a **syntactic** `SemPrf_alpha(BOT,w)` complementary clash of an
ABSTRACT existential witness (`w` is (B)'s delta-witness; nothing concrete). The
2026-06-25 falsifier guardrail disables exactly that clash -- so it blocks
Willard's actual (A)+(B) argument, not just the trivial one.

Measured (`sjas-step1-l0-probe`, both sides):
- `l0-fake-witness-syntactic-clash` false; `subst-control-closes` true (identical
  tree shape over `subst-code` closes -> structure valid, the exclusion is
  `semprf-alpha`-specific);
- `k-interpreted-route-axiom-proof` true / `nonk-...` false -- but this is a RED
  HERRING for leaf (iii): the interpreted route needs a CONCRETE proof code, while
  Willard's leaf (iii) closes on the ABSTRACT existential witness `w`. The
  interpreted route is not the (iii) mechanism.

The guardrail was built to stop `AxiomConj ∧ ¬SelfCons` closing trivially (there
`¬SelfCons` DIRECTLY supplies the positive `SemPrf(BOT)` -- no diagonal needed, so
it closes over EVERY system). But Willard's demonstration target is the full
diagonal `A ∧ B ∧ ¬D*`, where the positive `SemPrf(BOT,w)` is DERIVED through
(B) = V5+Map+Paradox+FinAx4 (which encode the mul-totality). The guardrail's clash
disable is a PROXY that conflates the two: it blocks the legitimate diagonal
closure to block the degenerate one.

## Sound resolution (soundness-critical -- needs owner sign-off, NOT unilateral)

Re-enable the `semprf-alpha(BOT,·)` clash, but make the DEMONSTRATION target the
full diagonal `A ∧ B ∧ ¬D*` (never `A ∧ ¬SelfCons`), and RESTATE the falsifier
from "`AxiomConj ∧ ¬SelfCons` must not close" to "the addition-only side does not
close the full diagonal." That is boundary-faithful IFF, with the clash enabled,
`A ∧ B ∧ ¬D*` closes on the mul side and FAILS on the addition side (because
(B)'s V-route machinery needs the mul-totality). This must be EMPIRICALLY VERIFIED
before it lands -- reframing a falsifier is precisely the ADR-0141 sin if it
merely turns a red gate green. Building `A ∧ B ∧ ¬D*` to verify it IS the step-1
combination tree (the crux), so this cannot be validated cheaply.

## The residual deep crux (independent of the above): step 4/5 fixed-point

Even with leaf (iii) admitted, steps 4/5 need the CONCRETE measured proof tuple
`p = code(step 4)` cited as `SemPrfK(code(Dk), p, 2^(p+1))` -- Willard's
self-referential diagonal. That is the genuine open research core and is NOT the
same as leaf (iii)'s abstract-witness clash.

## Status

NOT a closure. Grounded corrections: (1) V5 is faithfully unbounded -- do not
change it; (2) leaf (iii) is a guardrail-disabled syntactic clash that blocks
Willard's own (A)+(B) argument, resolvable only by a soundness-critical falsifier
reframe that needs owner sign-off and combination-tree validation; (3) the
step-4/5 concrete fixed-point remains the deep crux. Probe:
`lein run -m proflog.sjas-step1-l0-probe`. Coordination: Codex's state-count lever
is orthogonal to all three.
