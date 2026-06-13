# ADR-0099: SJAS Track 2a Completion (Procedure-Call and Quantifier Fragment Reachability)

- Status: accepted
- Date: 2026-06-13
- Branch: `adr-0099-sjas-track2a-completion`
- AAR: [AAR-0099](../aar/AAR-0099-sjas-track2a-completion.md)

## Context

[ADR-0098](ADR-0098-sjas-equality-fragment-reachability.md) resolved the first
of three *unresolved and high risk* rows in the
[SJAS tableau relevance matrix](../log/2026-05-25-sjas-tableau-relevance-matrix.md)
— equality/disequality — via the unreachability route: the SJAS structural
proof checker closes those branches formula-bearing, so the equality
constructor tags are unreachable in first-fragment certificates.

Two high-risk rows remain:

- **Procedure-call and profile-interleaved theory rules**
  ([note](../log/2026-05-26-sjas-procedure-call-relevance.md)): reflected
  clauses and guarded alternatives run inside Proflog's program/profile
  machinery; a compact `pos-call`/`neg-call`/guarded constructor could hide a
  reflected-axiom expansion.
- **Quantifier instantiation and witness policy**
  ([note](../log/2026-05-26-sjas-quantifier-witness-relevance.md)): a compact
  `univ`/`witness` node could let Proflog accept a proof while hiding large
  instantiated terms or many universal instances outside the proof-size
  accounting.

Since those notes were written, Track 1 (ADR-0091/0095) rebuilt non-`sjas-axiom`
certificate checking as formula-bearing structural tableau validation, and the
existing suite already shows the structural checker accepting reflected calls
and quantifier expansions *without* call/witness tags
(`sjas-proof-check-accepts-formula-bearing-positive-reflected-calls`,
`...-quantifier-expansions`, etc.). This ADR completes Track 2a by recording the
corresponding fragment-reachability resolution and closing the matrix.

This is an audit (like ADR-0096/0097/0098): no kernel, checker, encoder, or
query change.

## Hypothesis

The reflected procedure-call expansion and the quantifier instantiation are both
realized by the structural checker as **formula-bearing child subtrees** — a
reflected call expands to the cited/negated clause body
(`sjas-system-reflected-call-clauseo` → child), and a quantifier node introduces
a `par-term` parameter (or gamma witness) and continues with the instantiated
body — with the decoded proof being the *tree shape*. The call constructors
(`pos-call`, `neg-call`, `alt`, guarded variants) and the quantifier
constructors (`univ`, `once-univ`, `witness`) are therefore unreachable in
accepted first-fragment certificates. Because the expansion is the *explicit*
formula-bearing subtree, its size is carried by the certificate and accounted by
the ADR-0097 structural tree audit, addressing the proof-size concern both notes
raised: no compact tag hides an unbounded subtree.

## Decision

1. Generalize the ADR-0098 reachability audit to all three high-risk aspects:
   add `fragment-reachability-constructor-sets` (equality, procedure-call,
   quantifier) and `audit-fragment-reachability` to
   `proflog.sjas-correspondence`.
2. Add reachability probes: a reflected-call closure and a quantifier-expansion
   closure, each asserting the accepted certificate is formula-bearing and that
   `audit-fragment-reachability` reports the relevant aspect empty.
3. Close the matrix: record the final status of every relevance-matrix row in a
   completion note, with the three high-risk rows resolved via unreachability
   and the proof-size obligation discharged by the explicit formula-bearing
   expansion.

## Test Obligations

- Red before: no executable per-aspect reachability evidence for procedure-call
  or quantifier constructors.
- Green after: the audit unit test (per-aspect reporting; coverage of the
  high-risk alphabet), the two reachability probes, and the existing
  formula-bearing reflected-call / quantifier acceptance selectors all green;
  source-audit selectors and both broad gates stay green.

## Exit Criteria

- All three high-risk relevance-matrix rows resolved with test-backed fragment
  reachability; the matrix completion note records every row's final status;
  residual Track 2b correspondence-theorem obligations (per row) named in
  AAR-0099.
