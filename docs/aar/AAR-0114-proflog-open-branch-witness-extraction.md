# AAR-0114: Proflog Open-Branch Witness Extraction

- Date: 2026-06-17
- ADR: [ADR-0114](../adr/ADR-0114-proflog-open-branch-witness-extraction.md)
- Branch: `adr-0114-proflog-open-branch-witness-extraction`
- Status: complete

## Outcome

Added `proflog.diagnostics.witness` with conservative witness extraction for the
v1 ground propositional fragment.

## Supported fragment

- Flat conjunctions of ground atomic `pos`/`neg` literals on nullary relations.
- Explicit `{:literals [[sym :pos|:neg] ...]}` input maps.

## Rejected / unsupported

- Quantifiers, disjunctions, implications, disequality, nested non-flat structure.
- Contradictory literal sets return `:closed` rather than a witness.

## Evidence

Tests: `lein test proflog.diagnostics.witness-test` — 6 tests, 14 assertions,
including golden-suite integration on three ADR-0112 open-branch examples.
Wired into `lein test-proflog-fast`.

## Follow-up

Broader fragments require new correctness tests per ADR-0114 consequences.
