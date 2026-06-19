# ADR-0136: SJAS Tab-2 Reduced Witness

- Status: completed
- Date: 2026-06-19
- Branch: `adr-0136-sjas-tab2-reduced-witness`
- AAR: [AAR-0136](../aar/AAR-0136-sjas-tab2-reduced-witness.md)

## Context

[ADR-0129](ADR-0129-sjas-tab2-boundary-surface.md) recorded
Tab-2-or-stronger theorem reuse as a Workstream B boundary variant, separate
from the positive Tab-1 proof-list implementation completed by
[ADR-0122](ADR-0122-sjas-tab1-theorem-reuse.md).

The roadmap still needs an executable reduced witness for this variant. Unlike
the total-multiplication and Xtab/LEM variants, the Tab-2 boundary is not first
a new reflected beta vocabulary. The reduced witness is the theorem-reuse move
itself: a concrete intermediate theorem shape whose ordinary tableau closure is
not admissible under Tab-1's `Pi*_1` / `Sigma*_1` intermediate restriction.

## Decision

Add a public Tab-2-or-stronger reduced-witness report that records:

- the witness formula `forall x. exists y. true`;
- the formula code under the implemented `:willard-sjas-tab1` baseline;
- classifier evidence that the formula is not `Pi*_1`, not `Sigma*_1`, and not
  accepted by the current host `pi-star-1-encodable?` admission helper;
- an ordinary structural tableau proof code for the witness theorem;
- a focused `tableau-proof/3` validation result for that proof code;
- explicit non-claims for a Tab-2 proof checker, the full generated SelfCons
  contradiction target, constructed certificate evidence, and proof-search
  synthesis.

Update the Workstream B roadmap so `:tab-2-or-stronger` has completed only the
reduced witness stage. The full generated target and both final evidence
obligations remain open.

## Consequences

- Tab-2-or-stronger work now has an executable witness that is stronger than
  the ADR-0129 surface metadata.
- The witness is deliberately a boundary witness for theorem-reuse strength,
  not an implementation of Tab-2 proof-list validation.
- Future Tab-2 ADRs can build the full generated SelfCons target from this
  recorded witness without confusing it with the positive Tab-1 path.

## Test Obligations

Red first:

- no public `tab2-or-stronger-reduced-witness-report` helper exists;
- the Workstream B roadmap still marks `:tab-2-or-stronger` as surface-only;
- no reduced-witness metadata is recorded for the Tab-2 variant.

Green after implementation:

- the report identifies `:tab-2-or-stronger` and
  `:reduced-reflected-beta-witness`;
- the witness formula is coded against a `:willard-sjas-tab1` system;
- classifier evidence records `false` for `Pi*_1`, `Sigma*_1`, and
  `pi-star-1-encodable?`;
- the report includes a structural proof code that validates through
  `tableau-proof/3`;
- the roadmap marks only the reduced stage complete for `:tab-2-or-stronger`;
- open obligations still include the full generated target, constructed
  certificate, and proof-search synthesis;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0136, the ADR index, and `LOG.md` identify this Workstream B reduced
  witness slice.
- Public helpers expose a concrete rank-2 theorem-shape witness outside the
  Tab-1 intermediate classifier.
- No Tab-2 checker, full generated target, constructed certificate, or
  proof-search synthesis completion is claimed.
