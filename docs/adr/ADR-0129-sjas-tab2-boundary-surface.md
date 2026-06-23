# ADR-0129: SJAS Tab-2 Boundary Surface

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0129-sjas-tab2-boundary-surface`
- AAR: [AAR-0129](../aar/AAR-0129-sjas-tab2-boundary-surface.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) separates positive Tab-1
work from boundary-failure variants. [ADR-0120](ADR-0120-sjas-tab1-proof-list-surface.md),
[ADR-0121](ADR-0121-sjas-tab1-entry-validation.md), and
[ADR-0122](ADR-0122-sjas-tab1-theorem-reuse.md) implemented the positive Tab-1
path: proof-list objects, measured accounting, arithmeticized validation, and
earlier-theorem reuse under the Level-1 classifier discipline.

The roadmap explicitly says Tab-2 and stronger proof-list variants are not
positive Workstream A goals. They belong to Workstream B as negative variants
expected to cross Willard's Goedel boundary. Before reduced witnesses or
generated targets can be added, the implementation needs an executable surface
that records what the Tab-2-or-stronger variant is and how it differs from the
implemented Tab-1 apparatus.

## Decision

Add an executable Tab-2-or-stronger boundary surface to the Workstream B audit:

- identify `:tab-2-or-stronger` as a boundary variant, not a positive Tab-k
  extension;
- point to the existing `:willard-sjas-tab1` profile as the implemented
  baseline that this negative variant exceeds;
- record that the first stronger step is theorem-reuse beyond the
  `Pi*_1` / `Sigma*_1` intermediate restriction;
- mark only the `:variant-surface` obligation complete for
  `:tab-2-or-stronger`;
- keep `:reduced-reflected-beta-witness`,
  `:full-generated-selfcons-contradiction-target`,
  `:constructed-certificate`, and `:proof-search-synthesis` open.

This ADR does not implement a Tab-2 proof checker, a reduced witness, or a
SelfCons contradiction target. It makes the negative variant executable in the
roadmap so later ADRs can add those pieces without blurring Tab-1 and Tab-2.

## Consequences

- Workstream A remains the positive Tab-1 line.
- Workstream B now has surfaces for two planned variant families:
  total multiplication and Tab-2-or-stronger deduction.
- The Tab-2 negative variant cannot be counted complete from Tab-1 theorem
  reuse; it still needs its own reduced witness, generated target, constructed
  certificate, and synthesis evidence.

## Test Obligations

Red first:

- the Workstream B audit still reports `:tab-2-or-stronger` as `:not-started`;
- no public Tab-2 boundary-surface audit helper exists.

Green after implementation:

- `:tab-2-or-stronger` is marked `:surface-implemented`;
- its open obligations no longer include `:variant-surface`;
- its remaining obligations include the reduced witness, full generated target,
  constructed certificate, and proof-search synthesis;
- the Tab-2 surface audit records that this is a boundary variant beyond
  `:willard-sjas-tab1`;
- the audit records that no Tab-2 proof checker, reduced witness, or generated
  SelfCons target has been implemented by this ADR;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0129, the ADR index, and `LOG.md` identify this Workstream B slice.
- Public audit helpers expose the Tab-2-or-stronger boundary surface.
- No reduced witness, generated target, constructed certificate, or synthesis
  evidence is claimed.
