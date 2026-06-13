# ADR-0098: SJAS Equality/Disequality Fragment Reachability

- Status: accepted
- Date: 2026-06-13
- Branch: `adr-0098-sjas-equality-relevance`
- AAR: [AAR-0098](../aar/AAR-0098-sjas-equality-fragment-reachability.md)

## Context

Track 2a (the [SJAS tableau relevance matrix](../log/2026-05-25-sjas-tableau-relevance-matrix.md))
flags **equality and disequality profile rules** as *unresolved and high risk*:
Proflog has equality support beyond a bare propositional/first-order tableau,
and derived equality closures could compress proof trees or add rule power not
present in the selected SJAS deduction method `D`.

Two axes now exist in `proflog.sjas-correspondence`:

- **Symbol classification** (Track 2a): the equality constructors `eq-step`,
  `eq-triggered-call`, `eq-triggered-neg-call`, `eq-refl`, `eq-bind`, `par-bind`
  are classified `:relevant` / `:equality-extension`.
- **Fragment boundary** (ADR-0096): every `:relevant` non-axiom symbol is
  `:outside-first-fragment`, with the obligation "do not admit to the first
  formula-bearing tableau fragment until Track 2b proves primitive status,
  bounded macro expansion, wrapper erasure, or **unreachability**."

The first correspondence fragment admits exactly two shapes: formula-bearing
structural tableau certificates (which carry *no* proof-symbol tags) and the
bare `sjas-axiom` citation. The [equality relevance note](../log/2026-05-26-sjas-equality-relevance.md)
names the remaining Track 2a work precisely: *collect reachability evidence for
the generic equality/disequality constructors in actual SJAS certificates, then
decide whether they are primitive, macro-expandable, or excluded from the first
correspondence fragment.*

This ADR supplies that reachability evidence executably and records the
resulting fragment resolution. It does not change the kernel, proof checker,
proof-code encoder, or query behavior — like ADR-0096/0097 it is an audit.

## Hypothesis

The SJAS structural proof checker closes equality- and disequality-laden
branches by validating **formula-bearing** tableau nodes (the formulas at each
node, checked node-by-node), not by consuming `eq-step`/`eq-triggered-*` proof
tags. If so, an equality-laden theorem provable in the selected `D` is provable
by a formula-bearing structural certificate that is *already inside* the first
fragment, and the generic equality constructors are **unreachable** in accepted
first-fragment `tableau-proof/3` / `subst-prf/4` certificates — the equality
calculus is *absorbed* into formula-bearing closure rather than admitted as
separate rule tags. The alternative outcome is that the checker requires an
equality tag for some closure, which would put those certificates outside the
first fragment and demand a bounded-macro-expansion proof instead.

## Decision

1. Add an executable **equality-reachability audit** to
   `proflog.sjas-correspondence`: given a decoded proof term, report which
   equality/disequality constructors (if any) it contains, reusing the existing
   symbol-walk used by `audit-proof-term`.
2. Drive it with **reachability probes**: SJAS targets whose proofs require
   equality and disequality closure (reflexive equality, constructor clash,
   occurs-check, disequality storage/recheck), checked through the structural
   proof checker, asserting whether the accepted public certificate is
   formula-bearing and equality-tag-free.
3. Record the resolved fragment status for the equality constructors based on
   the evidence (unreachable-in-first-fragment vs reachable-needs-expansion),
   updating the fragment-boundary rationale without weakening ADR-0096's
   conservative default.

## Test Obligations

- Red before this ADR: no executable evidence distinguishes "equality absorbed
  into formula-bearing closure" from "equality requires out-of-fragment tags."
- Green after: reachability probes for representative equality/disequality
  closures, each asserting the fragment status of the accepted certificate;
  the equality-reachability audit; existing correspondence, fragment-boundary,
  and source-audit selectors and both broad gates stay green.

## Exit Criteria

- The equality/disequality constructors have a recorded, test-backed fragment
  resolution (the high-risk relevance-matrix row moves from open to resolved),
  with the proof medium for any residual macro-expansion obligation named in
  AAR-0098.
