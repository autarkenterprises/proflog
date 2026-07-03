# ADR-0147 stage 2: condition-(A) bridge audit (Level-0 SelfCons as reflected axiom)

Date: 2026-07-03. Branch `adr-0147-theorem23-bot-closure`.

This discharges the audit the ADR-0147 architecture gated stage 2 on: whether
the Level-0 consistency sentence may be added to the reflected basis, and under
what falsifier obligations. Verdict: **yes, on both sides of the ADR-0146
boundary harness, with three pinned falsifier obligations** — and the addition
is a *faithfulness repair*, not an invention.

## What JSL2's Theorem 2.3 actually consumes

The C1 audit (2026-06-23) verified Eqs (12)-(16) but did not audit condition
(A)'s own formalization. JSL2 Theorem 2.3's (A) is the system's consistency
sentence in the *Level-0* form the theorem manipulates: no proof code derives
the contradiction — `forall x. not SemPrf_alpha(code(BOT), x)` — and the
theorem's step 1 uses exactly its universal instances against V5's consequent
witness (`EXISTS x<z SemPrf_alpha(BOT,x)`), a gamma-vs-delta collision.

The generated Group-3 SelfCons for the mul profile is the *Level-1 measured*
form (Willard 2013 sentence (7), `selfcons1-formula`), and the theorem23
ledger's (A) note already records the relationship: "Level-1 (dsjas-subst-prf)
realizes the Level-0 `forall p. not SemPrf(BOT,p)`". The realization is
currently a *derivation obligation* standing between every closure route and
its final collision (ADR-0147 architecture fact 5). Reflecting the Level-0
sentence as an explicit basis member converts that obligation into a citable
axiom — precisely what JSL2's own alpha contains for the theorem's purposes.

## The decision

Add `total-multiplication-selfcons-l0-axiom`:

    forall p. not semprf-alpha(code(alpha), code(BOT), p)

as a reflected beta axiom of **both** harness sides (mul-total AND
addition-only): Willard's self-justifying systems assert their own consistency
on both sides of the boundary; asymmetric addition would fake the contrast.
The boundary asymmetry continues to come only from V5 + the mul-totality
content (ADR-0146's pinned mechanism): on the addition side no V5-consequent
witness ever appears, so the L0 axiom is never contradicted.

## Falsifier analysis (why this does not re-open the guardrail)

The 2026-06-25 guardrail forbade obtaining closure via a *syntactic
`SemPrf(BOT,p0) ^ not SemPrf(BOT,p0)` clash on the SelfCons-refutation
target*, because `AxiomConj ^ not SelfCons-L0` delta-expands the negated
target into the positive witness, closing trivially over EVERY system. Three
observations keep that guardrail intact:

1. The trivial vector requires `not SelfCons-L0` **as a refutation-target
   conjunct**. Adding SelfCons-L0 as a positive basis member creates no such
   target; the demonstration targets remain the L1/measured ones, and the
   V5-route trees consume the L0 axiom only through gamma instances clashing
   against *derived* V5 witnesses — the interpreted route the guardrail
   mandates upstream (the witness exists only where Paradox's bounded diagonal
   proof closes by the V-route).
2. `semprf-alpha` is already globally reserved in `reserved-coding-symbols`
   (the guardrail log's "profile-local" description was the stale side of the
   doc-vs-code tension ADR-0147 flagged; the code has been global since the
   pow-vocabulary change). The operative falsifier was never name-locality —
   it is the *pinned tests*: the trivial closure measured false because no
   L0 form appeared anywhere, and with L0 added the exclusions below pin it.
3. Willard-faithfulness: both of his systems assert consistency; the theorem
   distinguishes them by what else they can prove, not by whether they say it.

## Pinned falsifier obligations (test contract for the implementation)

- (F1) The ADR-0146 contrast namespace stays green with SelfCons-L0 in both
  sides' beta (T1 asymmetry, T2 control, T3 bound-check, T4 plumbing).
- (F2) NEW pin: the L1 SelfCons refutation target (`AxiomConj ^ not
  SelfCons-L1`-style measured target) still does NOT close under a trivially
  constructed tree over either side — the guardrail's original empirical
  check, re-asserted post-addition.
- (F3) NEW pin (vacuity exclusion): the L0-negated target `SelfCons-L0-axiom
  ^ not SelfCons-L0` closes by the syntactic clash over BOTH sides — i.e. it
  is explicitly pinned as boundary-blind and therefore NOT a demonstration
  target. Any future claim routed through that target is mechanically exposed
  as vacuous by this test's both-sides closure.

## Consequences for the closure path

With the L0 axiom citable, ADR-0147 architecture facts 3-4 complete their
leaf inventory: `T_p-bar`'s V5-instance branches close by (i) premise clashes
against the not-Dk deltas' own positives (Paradox), (ii) ground interpreted
closures (`FinAx4`, `leq`; `Map` handler per C4), and (iii) the L0 gamma
clash against V5's consequent witness — no remaining literal lacks a closure
mechanism. Stage 3 (Map/leq/FinAx4 ground-instance verification) and stage 4
(assemble `T_p-bar`) proceed on that basis.

## Implementation correction: group-generated, not beta

A beta-member L0 axiom would mention `code(alpha)` while being part of the
source that `code(alpha)` encodes — an encoding regress. The repository's own
tableau0 profile already solves exactly this: `selfcons0-formula` is the
L0-shaped `forall p. not dsjas-tableau-proof(system-code, contradiction-code,
p)` and enters as the GENERATED Group-3 record, built after the source code
exists (generated groups are derived from the encoded source, not encoded
into it). The L0 (A)-bridge therefore lands as an additional generated group
record for the total-multiplication profile (an L0 companion alongside the
measured Level-1 Group-3), with a corresponding axiom-membership route in the
checker — NOT as a `:beta` entry. Both ADR-0146 harness sides share the
profile, so both receive it automatically, as the audit requires. The
falsifier obligations (F1)-(F3) are unchanged.
