# Proflog Tableau Improvement Planning

This directory holds planning support for ADR-0112 through ADR-0115. The
improvements are Proflog-level and profile-transparent.

## Records

- [ADR-0112](../../adr/ADR-0112-proflog-literature-tableau-golden-suite.md):
  literature tableau golden suite.
- [ADR-0113](../../adr/ADR-0113-proflog-proof-object-diagnostic-renderer.md):
  proof object diagnostic renderer.
- [ADR-0114](../../adr/ADR-0114-proflog-open-branch-witness-extraction.md):
  open-branch witness extraction.
- [ADR-0115](../../adr/ADR-0115-proflog-proof-preserving-scheduling-benchmarks.md):
  proof-preserving scheduling benchmarks.

## Supporting Files

- [tableaux-test-inventory](tableaux-test-inventory.md) seeds the required
  source-test catalog for ADR-0112.
- [reconciliation-ledger](reconciliation-ledger.md) gives the required format
  for recording differences among upstream tests, literature/source
  expectations, and Proflog observations.

## Implementation Order

The ADRs are independent enough to be implemented on separate branches. ADR-0112
is the best first implementation target because it supplies external examples
that can later exercise the renderer, witness extractor, and scheduling
benchmarks.

ADR-0113 and ADR-0114 may share internal artifact-reading helpers, but each
must keep its public diagnostic API read-only. ADR-0115 may consume diagnostic
traces if they exist, but its semantic-preservation checks must not depend on
diagnostic rendering.
