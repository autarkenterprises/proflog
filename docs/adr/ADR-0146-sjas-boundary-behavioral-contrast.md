# ADR-0146: SJAS Boundary Behavioral Contrast (mul-total vs addition-only)

- Status: accepted
- Date: 2026-07-02
- Branch: `adr-0146-sjas-boundary-behavioral-contrast` (off `adr-0145-ground-proof-rule-dispatch` @ `fd6c19c`)
- AAR: [AAR-0146](../aar/AAR-0146-sjas-boundary-behavioral-contrast.md)

## Context

ADR-0142's goal sentence asks for an executable demonstration that "recognizing
multiplication as a total function carries the system across the boundary," and
its falsifier guardrail (2026-06-25) specifies the admissible route exactly: the
closure must go through the **interpreted bounded-proof V-route on a real
constructed proof code**, "where multiplication-totality makes the Log bound
hold and the addition-only variant fails" — never through a syntactic SemPrf
clash, which would close over every system and erase the boundary.

The theorem23 ledger's remaining open item is the cut-free combination trees
for steps 1/3/4/B (the full BOT closure — flagged as the central open research
problem, not one-session completable). What ADR-0144/0145's checker speedups
newly make *tractable* is the **per-step behavioral contrast**: running the
same constructed proof steps over both sides of the boundary and observing the
asymmetry at the boundary-relevant step. The V-route closure over a full
Willard Type-M basis is a ~25–33 s check now; before the perf line it was not
realistically testable.

## Decision

Pin the boundary contrast as an executable, falsifying test namespace
(`proflog.sjas-boundary-contrast-test`, added to the SJAS not-slow gate), over
a maximally controlled pair of systems:

- **mul-total side** = `sjas/total-multiplication-complete-system` (complete
  equational mul basis + Willard V4/V5 route axioms + squaring-chain witnesses
  in the reflected beta), depth-1 chain, `pow` declared, `:u-grounding`.
- **addition-only side** = the **identical** `sjas/system` call — same profile,
  same constants (including the chain constants), same functions (`pow`, mul
  vocabulary), same relations, same coding format, same shared beta seed —
  with the multiplication-totality content simply **absent from beta**.

Because the vocabulary is identical, formula codes are **byte-identical across
the sides** (asserted), so every observed difference is purely about what the
system *recognizes* (encoded basis membership), never coding drift. This is the
sharpest available operationalization of Willard's boundary: multiplication is
*declared* on both sides; it is *recognized as total* on one.

**The five pinned legs:**

| leg | construction | mul-total | addition-only |
|---|---|---|---|
| hypotheses | `total-multiplication-hypothesis-report` | satisfied | **not satisfied** |
| T1 boundary | `neg SemPrf^k(code(S),1,code(mul-totality-axiom),cite,2^(p+1))` | **closes** (V-route) | **fails** |
| T2 control | same leaf citing the shared beta axiom | closes | closes |
| T3 non-vacuity | T1's mul-side leaf with too-small bound `2^p` | **fails** | — |
| T4 localization | step-5 premise-clash witness-binding tree | closes | closes |

T2 proves the addition side's V-route machinery (decode-by-name, membership
interpretation, symbolic Log bound) is fully functional, so T1's failure is
exactly non-membership of the multiplication-totality content. T3 proves the
mul-side closure is genuinely `Log`-bound-checked rather than profile-granted.
T4 proves the tableau plumbing (alpha/gamma-fresh-variable/beta/
complementary-clash) is boundary-blind — the boundary is **localized** to the
interpreted V-route membership step. The T1 addition-side non-closure is the
**pinned falsifier** the guardrail called for: any future shortcut that closes
it (e.g. promoting `semprf-alpha` to a named clash) breaks this test.

The theorem23 ledger gains `:phase4-boundary-contrast` recording exactly this,
with the open boundary (steps 1/3/4/B combination trees) explicitly unchanged.

## Honest scope

This demonstrates the **per-step boundary asymmetry** at the exact route the
guardrail mandates. It is **not** the full Theorem 2.3 BOT closure: the
combination trees for steps 1/3/4/B (deriving Dk, the fixed-point measured
proof object, and condition (B) via V5+Map) remain the open research problem,
and no ledger step status was changed. What the contrast adds beyond the prior
state: the addition-side failure was previously an *argument* (the guardrail's
reasoning); it is now a *measurement*, over a full Type-M basis, paired with
the controls that make it non-vacuous.

Willard-fidelity note: non-closure of one constructed leaf is not a
consistency proof for the addition side (ADR-0142 already stipulates bounded
non-closure is not evidence of consistency); it is the executable witness that
*this* proof route — the one Theorem 2.3 needs — is available exactly on the
mul side. The existing `^:slow` synthesis guards (no synthesized counterexample)
remain the search-side falsifier.

## Consequences

- The SJAS gate now carries the boundary contrast as a standing falsifier
  (~110 s namespace; dominated by three V-route closures over the full Type-M
  basis at ~25 s each — a cost that only became payable after ADR-0144/0145).
- The remaining ADR-0142 work is unchanged and precisely scoped: build the
  step-1/3/4/B combination trees; step 5 assembles once step 4's bounded proof
  of Dk exists. The contrast namespace is where their side-pair behavior gets
  pinned as they land.

## Test obligations

- `proflog.sjas-boundary-contrast-test` (5 tests / 11 assertions) green and in
  the not-slow gate; SJAS gate + fast-minus-landmine + isolated
  fitting-fidelity green (ADR-0144 lesson-5b protocol).
- Falsifier direction: T1's addition-side assertion is a *negative* pin; the
  existing guardrail text in `theorem23-closure-status` points to it.
