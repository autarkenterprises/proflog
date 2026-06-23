# ADR-0130: SJAS Xtab / LEM Boundary Surface

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0130-sjas-xtab-lem-boundary-surface`
- AAR: [AAR-0130](../aar/AAR-0130-sjas-xtab-lem-boundary-surface.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) identifies Xtab or Law of
Excluded Middle packaged as an axiom schema as the third planned Workstream B
negative variant family. The baseline SJAS implementation remains
tableau-first: excluded-middle behavior is handled by the tableau apparatus,
not promoted to a logical axiom schema in the reflected basis.

Willard's later boundary discussions make this packaging distinction relevant.
This ADR does not implement Xtab or a LEM axiom schema; it creates the
executable roadmap surface that prevents future work from treating ordinary
tableau handling of complementary formulas as the negative variant.

## Decision

Add an executable Xtab/LEM-as-axiom boundary surface to the Workstream B audit:

- identify `:xtab-or-lem-axiom` as a boundary variant;
- record that the baseline remains `:tableau-derived-lem`;
- record that the negative step is packaging Xtab/LEM as a logical axiom
  schema;
- mark only the `:variant-surface` obligation complete for
  `:xtab-or-lem-axiom`;
- keep `:reduced-reflected-beta-witness`,
  `:full-generated-selfcons-contradiction-target`,
  `:constructed-certificate`, and `:proof-search-synthesis` open.

This ADR is an audit and planning surface. It does not add an axiom schema,
change proof search, or claim a contradiction witness.

## Consequences

- Workstream B now has executable surfaces for all three ADR-0119 negative
  variant families: total multiplication, Tab-2-or-stronger, and
  Xtab/LEM-as-axiom.
- Future Xtab/LEM work must distinguish an axiom-schema variant from ordinary
  tableau decomposition/closure behavior.
- The full negative evidence obligations remain open.

## Test Obligations

Red first:

- the Workstream B audit still reports `:xtab-or-lem-axiom` as `:not-started`;
- no public Xtab/LEM boundary-surface audit helper exists.

Green after implementation:

- `:xtab-or-lem-axiom` is marked `:surface-implemented`;
- its open obligations no longer include `:variant-surface`;
- its remaining obligations include the reduced witness, full generated target,
  constructed certificate, and proof-search synthesis;
- the Xtab/LEM surface audit records that this is an axiom-schema boundary
  variant rather than ordinary tableau-derived excluded-middle behavior;
- the audit records that no axiom schema, reduced witness, or generated
  SelfCons target has been implemented by this ADR;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0130, the ADR index, and `LOG.md` identify this Workstream B slice.
- Public audit helpers expose the Xtab/LEM-as-axiom boundary surface.
- No axiom schema, reduced witness, generated target, constructed certificate,
  or synthesis evidence is claimed.
