# ADR-0142 Phase 3: the BOT-closure falsifier guardrail (do not promote semprf-alpha)

Date: 2026-06-25
Context: investigating the single remaining open-boundary item (the cut-free
combination trees for steps 1/3/4/B, with step 5 reducing to step 4).

This is a process note from a deep investigation of how to construct the
combination tableaux that would close `BOT` over the multiplication-total system.
It records one **solid, durable guardrail** and two honest observations. It makes
no closure claim; the repo state is unchanged (the finding is documentation only).

## The guardrail (solid): do NOT promote `semprf-alpha` to a named clash

The pow-vocabulary work promoted `semprf**k**-alpha` to a globally-named symbol so a
decoded `(neg SemPrf^k)` node matches its branch atom and complementary literals
clash. It is tempting to do the same for `semprf-alpha` (the un-superscripted
`SemPrf`) so the SelfCons clash `SemPrf(BOT,p) ^ not SemPrf(BOT,p)` fires
syntactically. **This must not be done -- it would break the addition-only
falsifier.**

Argument (independent of any tableau construction, so it does not rest on the
buggy hand-constructions below):

- The SelfCons refutation target is `AxiomConj(S) ^ not SelfCons_S`, and `SelfCons_S`
  is a conjunct of `AxiomConj(S)`.
- `SelfCons_S` is (at Level 0) `forall p. not SemPrf(BOT,p)`; `not SelfCons_S`
  delta-expands to `SemPrf(BOT,p0)`; the SelfCons conjunct gamma-instantiates to
  `not SemPrf(BOT,p0)`.
- If `semprf-alpha` were a named primitive, those two would clash **syntactically**,
  closing `AxiomConj ^ not SelfCons` TRIVIALLY -- for EVERY system, the
  addition-only (self-justifying, consistent) variant included.
- That is precisely the boundary the construction must distinguish: the
  multiplication-total variant must close and the addition-only variant must NOT
  (the falsifier, AAR-0142). A trivial `SemPrf` clash erases the distinction.

Therefore the closure must route through the **constructed bounded proof**: the
operative relation is the *interpreted* `SemPrf^k` V-route (which actually checks a
decoded bounded proof and the Definition 2.1 `Log` bound), on a real constructed
proof code, where multiplication-totality is what makes the bound hold (and where
the addition-only variant fails). `SemPrf(BOT,x)` likewise must close by the V-route
on a constructed `x`, never by a syntactic literal clash on a free parameter.

Confirmed empirically: the SelfCons refutation does NOT close under a trivially
constructed proof tree (`:trivial-selfcons-refutation-closes false`) -- the
falsifier is currently intact, and keeping `semprf-alpha` profile-local is part of
why.

## Observations (honest, partial)

- The generated SelfCons is the **measured Level-1** form (`forall x y p q. ...`,
  the `dsjas-subst-prf` SelfCons), not the simple Level-0 `forall p. not
  SemPrf(BOT,p)`. The combination trees operate over this measured form.
- Hand-constructing the combination tableaux is genuinely hard: naive
  construct-and-check shapes for the complex-formula closures (e.g. closing
  `Dk ^ not Dk`, or even a small `F ^ not F` with `F` quantified) either raise a
  core.logic protocol error or do not terminate under the builder shapes tried.
  This is consistent with ADR-0142 criterion 6 ("a fully expanded ordinary tableau
  may be very large") but the specific failures may also be construction bugs, so
  no structural-capability conclusion is drawn here. A cleaner construction path
  (or a verified composition/expansion) is needed; this remains open research.

## Status (unchanged)

No closure step is promoted. The open boundary remains the cut-free combination
trees for steps 1/3/4/B (step 5 reduces to step 4 = the bounded proof of Dk). The
single substantive addition here is the guardrail: the final closure may NOT be
obtained by promoting `semprf-alpha` to a syntactic clash; it must go through the
interpreted bounded-proof V-route, preserving the addition-only falsifier.
