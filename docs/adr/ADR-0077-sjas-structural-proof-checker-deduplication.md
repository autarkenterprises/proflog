# ADR-0077: SJAS Structural Proof Checker Deduplication

- Status: completed
- Date: 2026-06-08
- Branch: `adr-0073-sjas-correspondence-program`
- AAR: [AAR-0077](../aar/AAR-0077-sjas-structural-proof-checker-deduplication.md)

## Context

ADR-0073 Track 1 validates formula-bearing SJAS proof certificates through
`sjas-structural-proof-check-state-decodedo`. During the SelfCons investigation
on 2026-06-08, the lower-level SelfCons core selector exceeded the 15-minute
foreground envelope before the user instructed that future SelfCons probes
should be left running and recorded rather than killed.

The checker must remain relational: a formula-bearing proof node supplies a
formula and child list, and the proof checker infers the local tableau rule
from those constraints. However, source inspection showed exact duplicate
branches inside the checker's `conde`, including the guided literal
continuation, complementary literal closure, and conjunction continuation.
Those duplicates add redundant search alternatives at every recursive proof
node without adding proof rules or semantic coverage.

## Decision

Remove exact duplicate and formula-bearing-subsumed branches from the structural
proof checker while keeping the existing ordinary `conde` scheduling,
proof-node formula matching, and formula-bearing rule inference. This ADR does
not introduce committed choice, does not reintroduce legacy proof-rule tags, and
does not specialize the checker to SelfCons alone.

The cleanup criterion is structural: prove by source audit that the checker
contains each guided continuation shape once, then remove alternatives whose
formula-bearing behavior is already covered by the decoded child formula route.
If SelfCons still materially exceeds the expected envelope, later work may add a
separate ADR for more aggressive rule dispatch.

## Consequences

- The checker presents fewer redundant alternatives to core.logic during fixed
  formula-bearing proof validation.
- Existing formula-bearing tableau tests should continue to pass because no
  accepted rule shape is removed; only repeated branches are deleted.
- This may not be sufficient to make the full public SelfCons proof fast. The
  durable SelfCons probe remains the correctness witness while focused
  performance work proceeds.

## Test Obligations

- Add a red source-audit regression showing the duplicated or subsumed guided
  alternatives.
- Keep the existing formula-bearing proof-check tests green after removing the
  duplicates.
- Run the focused SelfCons core selector after the cleanup, recording whether it
  passes or still exceeds the long-test envelope.
- Rerun `lein test-proflog-fast` and `lein test-proflog-extended` before
  treating the cleanup as complete.
