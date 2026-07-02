# AAR-0146: SJAS Boundary Behavioral Contrast

- Date: 2026-07-02
- ADR: [ADR-0146](../adr/ADR-0146-sjas-boundary-behavioral-contrast.md)
- Branch: `adr-0146-sjas-boundary-behavioral-contrast`

## Outcome

Completed as scoped. The ADR-0142 falsifier guardrail's specified
demonstration — the same constructed proof step closing through the
**interpreted bounded-proof V-route** over the multiplication-total system and
**failing** over the addition-only sibling — is now an executable, pinned test
(`proflog.sjas-boundary-contrast-test`, in the SJAS not-slow gate), with the
controls that make it non-vacuous. The theorem23 ledger records it as
`:phase4-boundary-contrast`; the open boundary (steps 1/3/4/B combination
trees) is explicitly unchanged — no step status was flipped.

## What was measured (REPL, before any test was written)

- Sides built with identical vocabulary; **formula codes byte-identical across
  sides** (asserted first — this converts the contrast from "different systems
  behave differently" to "the same bytes are recognized differently").
- `total-multiplication-hypothesis-report`: mul side satisfied / addition side
  `:reflected-basis-valid? false` — the system-level side-of-boundary
  statement, via an existing public reporter that already refuses to accept a
  profile label alone.
- T1 (`neg SemPrf^k(code(S),1,code(mul-axiom),cite,2^(p+1))`): mul side
  **closes** (32.9 s at chain depth 3; 25.5 s at depth 1 — the landed fixture),
  addition side **fails** (0.4–0.8 s, fast at membership).
- T2 control (shared axiom): closes on **both** sides.
- T3 (too-small bound `2^p` on the mul side): **fails** — Log-bound genuinely
  checked over the full basis.
- T4 (step-5 premise-clash tree): closes on **both** sides.
- Namespace total: 5 tests / 11 assertions / 110.8 s warm.

## Why this is the guardrail's demonstration and not a tautology

"Addition-only lacks the axiom, so citing it fails" would be vacuous if the
addition side's machinery were simply broken, if the coding differed, or if
the mul side's closure were profile-granted. The controls close those doors:
T2 shows the addition side's V-route (decode-by-name, membership
interpretation, symbolic `pow` bound) works end-to-end; the byte-identity
assertion shows the cited codes are the same object; T3 shows the mul-side
closure still honors the Definition-2.1 bound; T4 shows the generic tableau
rules are boundary-blind. What remains as the *only* discriminator is whether
the system's encoded basis contains the multiplication-totality content — 
which is Willard's boundary, operationalized. The addition-side non-closure is
now a standing falsifier: the trivial-clash shortcut the guardrail forbids
would break this test, not just an argument in a log.

## Role of the performance line (ADR-0144/0145)

The T1 mul-side check is a single V-route closure over a full Type-M basis
(complete mul equations + V4/V5 + witnesses + generated groups): ~25 s warm
today. This measurement sat behind the checker wall before — the ~78 K-node
search regime made full-basis construct-and-check closures unrunnable inside
any test budget. The contrast namespace is the first direct consumer of the
perf work on the actual ADR-0142 workload.

## Lessons

1. **Byte-identity across sides is the load-bearing control.** Building the
   addition side as "same `system` call minus the mul beta" (same profile,
   same constants incl. the chain witnesses, same `pow` declaration) is what
   makes the contrast about *recognition*, not representation. An
   earlier-considered design (level1-profile sibling) would have varied the
   generated groups and SelfCons shape simultaneously — a much weaker
   experiment.
2. **The falsifier flipped from prose to test.** The 2026-06-25 guardrail was
   an argument about what *would* erase the boundary; T1's addition-side
   assertion makes it mechanical.
3. **Measure the fixture cost before promoting to the gate.** Depth-1 chain
   (25.5 s) vs depth-3 (32.9 s) was checked before choosing; the namespace
   still costs ~110 s, accepted deliberately under the project's
   long-tests-for-semantics doctrine and noted in the ADR.

## Follow-ups

- The remaining ADR-0142 work is unchanged: cut-free combination trees for
  steps 1/3/4/B (step 5 assembles once step 4's bounded proof of Dk exists).
  As each lands, its side-pair behavior belongs in the contrast namespace —
  the expected pattern is T4-style closure on both sides for pure-logic steps
  and T1-style asymmetry wherever V-route membership or the Log bound enters.
- If gate runtime pressure grows, the candidate trim is T3 (partially
  redundant with `sjas-semprfk-tree-closure-test`'s too-small pin over the
  small system), not T1/T2.
