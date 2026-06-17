# AAR-0115: Proflog Proof-Preserving Scheduling Benchmarks

- Date: 2026-06-17
- ADR: [ADR-0115](../adr/ADR-0115-proflog-proof-preserving-scheduling-benchmarks.md)
- Branch: `adr-0115-proflog-proof-preserving-scheduling-benchmarks`
- Status: complete (narrowed scope per interdev review)

## Outcome

Added `proflog.scheduling-benchmarks` with eight baseline benchmarks covering
single pending-goal, multi-goal, deterministic, nondeterministic, and branching
cases. Tests in `proflog.scheduling-benchmark-test` require semantic preservation
before branch-growth envelope checks on **closed** benchmarks only.

Interdev review corrections (2026-06-17) narrowed branch-growth claims: open
benchmarks no longer fabricate `step-count 0` as search cost. Extended probes live
in `proflog.scheduling-benchmark-extended-test`.

## Baseline observations (closed-proof step counts only)

| Benchmark | Semantic | Steps |
|-----------|----------|------:|
| `:single-pending-goal-closed` | closes | 3 |
| `:multi-goal-closed` | closes | 5 |
| `:branching-closed` | closes | (envelope ≤ 48) |

Open benchmarks (`:single-pending-goal-open`, `:multi-goal-open`,
`:deterministic-alpha-beta`, `:nondeterministic-disjunction`, `:branching-open`)
record semantic baselines only; `branch-growth-applicable` is false.

## Evidence

`lein test proflog.scheduling-benchmark-test` — fast gate.
`lein test proflog.scheduling-benchmark-extended-test` — extended gate.

Closed-proof regression pins `:single-pending-goal-closed` at 3 proof steps.

## Residual gaps

Open-branch search instrumentation (attempt counts, branch counts, pending-goal
selection traces) is not exposed by the kernel API. Future optimization ADRs must
extend instrumentation before claiming open-case scheduling coverage.
