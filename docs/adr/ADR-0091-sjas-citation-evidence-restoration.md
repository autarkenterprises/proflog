# ADR-0091: SJAS Tableau-Proof Citation Evidence Restoration

- Status: completed
- Date: 2026-06-10
- Branch: `adr-0088-sjas-runtime-rebaseline`
- AAR: [AAR-0091](../aar/AAR-0091-sjas-citation-evidence-restoration.md)

## Context

The ADR-0088 namespace sweep — the first full SJAS namespace run since the
suite became impractically slow around ADR-0086 — surfaced one semantic
failure:
`sjas-composite-examples-distinguish-beta-axioms-from-reflected-procedures`
expects a public `tableau-proof/3` citation proof for a reflected Group-2b
axiom to contain the `sjas-system-reflected-axiom` evidence step, and the
accepted proof is instead the bare marker
`(profiled willard-sjas-tableau0 (profiled willard-sjas-proof-check))`.

Differential attribution: the same failure reproduces at `97f70a7`
(pre-ADR-0090), so the walk fast path is exonerated. The bare wrapper was
introduced by commit `e248c8b` ("Remove SJAS proof predicate trace
evidence", 2026-06-04): `sjas-tableau-proof-closeo` validates certificates
through the proof-free `sjas-tableau-proof-coreo` — whose `sjas-axiom`
branch calls the proof-free `sjas-walked-axiom-member-coreo` — and then
attaches a constant marker. The change was performance-motivated, in the
period when reifying evidence dominated proof-search runtime, and the
affected regression had not been runnable since.

This conflicts with the project's own intensional standards: the
arithmeticization specification requires proof objects to remain
inspectable with local rule evidence, ADR-0073 lists proof-object
inspectability among the presumptively relevant tableau properties, and
the profile source audit exists precisely to reject marker summaries that
stand in for object-level evidence. ADR-0090 has removed the performance
rationale: the proof-bearing membership relations now run in ordinary
focused-selector time.

## Decision

Restore citation evidence in the public `tableau-proof/3` closure: the
`sjas-axiom` certificate branch validates membership through the
proof-bearing `sjas-walked-axiom-membero` and nests its evidence inside
the profile wrapper, so reflected Group-2b citations again carry
`sjas-system-reflected-axiom` (and fixed/beta/Group-3 citations their
respective steps). The structural-certificate branch keeps the plain
wrapper: its inspectable evidence is the decoded proof code itself, which
the checker validates node by node.

The same evidence-thinning pattern in `sjas-subst-prf-closeo` is recorded
here but deliberately left unchanged: the current `subst-prf` regressions
define its contract (`willard-sjas-subst-proof-check` without a nested
trace), and widening that contract is follow-up work to be weighed against
the Track 2a relevance matrix rather than smuggled into this repair.

## Consequences

- Public citation proofs are evidence-bearing again; the e248c8b trade is
  reversed now that ADR-0090 pays for it.
- Proof shapes for citation acceptances change from the bare marker to the
  nested form; only the failing regression asserts the inner step, and the
  bare-marker form was never recorded as a contract elsewhere.

## Test Obligations

- Red: the sweep failure above, reproduced standalone (and at `97f70a7`).
- Green: the failing var; the seven-selector semantic batch from AAR-0090
  (citations, ADR-0087 selectors, source audit); the bisect probes must
  stay in their post-ADR-0090 envelopes; both broad gates.

## Exit Criteria

- The composite-examples regression passes with the citation evidence
  step present, with no other selector or gate regressions, and the
  baseline notes which sweep rows were measured pre- and post-this-ADR.
