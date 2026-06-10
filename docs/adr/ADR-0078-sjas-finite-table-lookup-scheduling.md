# ADR-0078: SJAS Finite Table Lookup Scheduling

- Status: completed
- Date: 2026-06-08
- Branch: `adr-0073-sjas-correspondence-program`
- AAR: [AAR-0078](../aar/AAR-0078-sjas-finite-table-lookup-scheduling.md)

## Context

After ADR-0075 made core.logic's occurs check stack-safe and ADR-0077 removed
exact duplicate structural proof-checker branches, the long-running SelfCons
core proof probe was sampled without stopping it. The active JVM was spending
time in `clojure.core.logic/occurs-check-worklist`, reached through
`clojure.core.logic/membero` and ordinary `==`.

That means the previous stack overflow has turned into an operational hot path:
finite membership relations and branch alternatives repeatedly bind terms whose
surface shape is known to be acyclic, but ordinary `membero` still drives each
candidate through a general occurs check.

## Decision

For fixed finite SJAS code tables, prefer explicit finite alternatives over
recursive `membero` table scans. Where the table relation only relates known
acyclic SJAS code data, use the local acyclic unifier already introduced for
large proof-code numerals instead of the general occurs-checking `==`.

This decision is limited to finite decoder/checker lookup tables whose entries
are constructed from static SJAS metadata. It does not apply to dynamic branch
state such as literal lists or environments, where membership is part of the
object proof state and must remain semantically transparent.

## Consequences

- Formula/proof byte decoding should spend less time repeatedly scanning large
  terms in the general occurs check.
- The relations remain relational finite tables: they can still enumerate table
  entries, but the scheduler no longer recurses through `membero` for static
  metadata.
- Dynamic proof-state searches remain unchanged.

## Test Obligations

- Add a red source-audit regression that rejects `membero` in fixed SJAS table
  lookup helpers while allowing it for dynamic branch state.
- Keep existing formula-code, proof-code, and formula-bearing tableau selectors
  green.
- Re-sample or remeasure the SelfCons core selector after implementation.
- Rerun the fast and extended suites before closing this ADR.
