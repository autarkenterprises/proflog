# AAR-0115: Proflog Proof-Preserving Scheduling Benchmarks

- Date: 2026-06-17
- ADR: [ADR-0115](../adr/ADR-0115-proflog-proof-preserving-scheduling-benchmarks.md)
- Branch: `adr-0115-proflog-proof-preserving-scheduling-benchmarks`
- Status: complete

## Outcome

Completed `proflog.scheduling-benchmarks` with eight baseline benchmarks covering
single pending-goal, multi-goal, deterministic, nondeterministic, and branching
cases. Tests in `proflog.scheduling-benchmark-test` require semantic
preservation before branch-growth envelope checks.

## Baseline Observations

Open tableaux have no closed-proof step count; their branch-growth evidence is
the deterministic structural estimate. Closed tableaux additionally record
recognized proof steps.

| Benchmark | Semantic | Closed steps | Estimated branches | Expansions | Formula size |
|-----------|----------|-------------:|-------------------:|-----------:|-------------:|
| `:single-pending-goal-open` | open | n/a | 1 | 0 | 1 |
| `:single-pending-goal-closed` | closes | 3 | 1 | 1 | 3 |
| `:multi-goal-open` | open | n/a | 1 | 1 | 3 |
| `:multi-goal-closed` | closes | 7 | 2 | 3 | 7 |
| `:deterministic-alpha` | open | n/a | 1 | 9 | 19 |
| `:nondeterministic-disjunction` | open | n/a | 10 | 9 | 19 |
| `:branching-closed` | closes | 8 | 2 | 3 | 7 |
| `:branch-bound-fitting` | open | n/a | 16 | 7 | 15 |

## Evidence

- `lein test proflog.scheduling-benchmark-test` — 3 tests, 87 assertions.
- `lein test proflog.scheduling-benchmark-extended-test` — 1 test, 5 assertions.

Fast gate: six benchmarks. Extended gate: two benchmark cases plus the
`^:slow` extended-only namespace.

## Residual gaps

Wall-clock timing envelopes are intentionally secondary to semantic preservation
and structural branch-growth evidence. Future optimization ADRs should extend
this catalog rather than bypass semantic checks.
