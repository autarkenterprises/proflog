# AAR-0115: Proflog Proof-Preserving Scheduling Benchmarks

- Date: 2026-06-20
- ADR: [ADR-0115](../adr/ADR-0115-proflog-proof-preserving-scheduling-benchmarks.md)
- Branch: `adr-0115-proflog-proof-preserving-scheduling-benchmarks`
- Status: complete

## Outcome

Added `proflog.scheduling-benchmarks` with eight baseline benchmarks covering
single pending-goal, multi-goal, deterministic, nondeterministic, and branching
cases. Tests in `proflog.scheduling-benchmark-test` require semantic preservation
before branch-growth envelope checks.

## Baseline observations (proof-step counts)

| Benchmark | Semantic | Steps |
|-----------|----------|------:|
| `:single-pending-goal-open` | open | 0 |
| `:single-pending-goal-closed` | closes | 3 |
| `:multi-goal-open` | open | 0 |
| `:multi-goal-closed` | closes | 5 |
| `:deterministic-alpha-beta` | open | 0 |
| `:nondeterministic-disjunction` | open | 0 |
| `:branching-closed` | closes | 5 |
| `:branching-open` | open | 0 |

## Evidence

`lein test proflog.scheduling-benchmark-test` — 6 tests, 59 assertions.
Fast gate: six benchmarks; extended gate: two benchmarks (`^:slow` extended-only
test plus shared catalog entries marked `:extended`).

## Residual gaps

Wall-clock timing envelopes are intentionally omitted from the fast gate; future
optimization ADRs should extend this catalog rather than bypass semantic checks.
