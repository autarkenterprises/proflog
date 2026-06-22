# Inter-Developer Note: Workstream B Redirected to Willard Theorem 2.3

Date: 2026-06-22

Follow-up to
[2026-06-22-adr-0141-completion-claim-review.md](2026-06-22-adr-0141-completion-claim-review.md)
and
[2026-06-22-adr-0141-genuine-derivation-feasibility.md](2026-06-22-adr-0141-genuine-derivation-feasibility.md).
Decision recorded in
[ADR-0142](../adr/ADR-0142-sjas-boundary-genuine-derivation.md).

## Why the feasibility note's path is abandoned

Reading the cited papers before writing any witness (per the feasibility note's
own next step) found that the `V4`/`V5`/`willard-map`/`Upsilon`/`Paradox`
apparatus is misattributed. Willard 2002 tab2 Eq 11-17 are a
*consistency-preservation* proof (safe side) by metalevel induction — no V4
descent axiom exists there or in willard2005/2001. The planned "V4 finite
descent, no induction" path is unsound: the tableau delta-rule yields opaque
parameters, not shrinking numerals, and SJAS has no least-number principle, so
the descent never closes. Building "one Upsilon witness" would only reproduce
the retracted circularity.

**Correction to this note's predecessor:** the feasibility note read "ordinary
search rejects 1=0 in ~18 ms" as the system not entailing its contradiction.
That conflates *search failure* with *consistency*. A genuinely inconsistent
system can have only a large, search-inaccessible Goedel-Loeb bottom-proof. The
contradiction must be **constructed and checked**, not searched for.

## The corrected target

Self-justification = the tableau system's *failure to close* a SelfCons
refutation; boundary failure = a closing tableau *appears*. Willard 2005 jsl5
Remark 4 (lines 977-984) shows multiplication-total destroys self-justification
"under any possible deduction method D, whether cut-free or otherwise" —
including the semantic tableaux this project implements. So the configured
multiplication-total system is genuinely unsafe under our deduction, and a
closing tableau exists.

The faithful, object-level construction is Willard 2002 jsl2 (ref [68],
`willard2002_semantic_tableaux_robinson_q_author_jsl2.pdf`), **Theorem 2.3**:
conditions (A) the SelfCons axiom, (B) bounded-proof-of-diagonal implies
`BOT`-provable, (C) `Subst` single-valuedness, plus the diagonal
`DK = Gamma(nbar)`, assembled into a six-step closure. All three conditions
already correspond to genuine kernel predicates (`Group-3 SelfCons`,
`semprfk-alpha`, `subst-code`); the only new content is the `SemPrfK` bound
arithmetic (`Log(z,K)`), which is exactly where multiplication-grade growth is
needed and where the addition-only variant fails to close.

ADR-0142 carries the full goal, success criteria, honesty constraints, test
obligations, and exit criteria. The misattributed apparatus is removed, not
relabeled; the principal risk is the predicate-trust audit on the closure path
(`semprfk-alpha`/`subst-code` must check, not destructure) and the bound
arithmetic, with the addition-only contrast as the falsifier against a vacuous
closure.
