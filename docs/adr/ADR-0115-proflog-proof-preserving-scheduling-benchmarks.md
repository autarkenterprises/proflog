# ADR-0115: Proflog Proof-Preserving Scheduling Benchmarks

- Status: accepted
- Date: 2026-06-16
- Branch: `adr-0115-proflog-proof-preserving-scheduling-benchmarks`
- AAR: [AAR-0115](../aar/AAR-0115-proflog-proof-preserving-scheduling-benchmarks.md)

## Context

The reviewed `tableaux` project highlights the practical value of formula
selection, alpha-before-beta expansion, early closure, and branch-growth
measurements. Proflog needs the same kind of guardrail at its own level:
optimizations should be judged only after answer preservation and proof
acceptance preservation are established.

This ADR is about benchmarks and regression tests, not a scheduling
optimization. It should create the fixtures that future optimization ADRs must
pass.

## Decision

Add proof-preserving scheduling benchmarks for representative Proflog
proof-search cases. The suite must cover:

- single pending-goal cases;
- multi-goal cases;
- deterministic expansion;
- nondeterministic expansion;
- branching cases with open and closed outcomes;
- mixed cases where rule ordering can affect branch count or runtime.

Each benchmark must record:

- the semantic expectation, such as accepted/rejected status or answer set;
- the measured scheduling or branch-growth quantity;
- the expected runtime envelope and whether the test belongs in the fast or
  extended suite;
- the baseline Proflog behavior used for comparison.

No benchmark may treat speed as success unless the semantic result is already
proved identical to the baseline for that case.

## Consequences

- Future proof-search changes have concrete Proflog-level guardrails.
- Performance regressions become easier to diagnose without tying the suite to
  one profile.
- Benchmarks can use diagnostic traces from ADR-0113 if available, but they
  must not depend on diagnostic rendering to prove semantic preservation.
- Branch-count and runtime envelopes must be loose enough to avoid machine
  noise but strict enough to catch clear regressions.

## Test Obligations

- Red tests must require baseline semantic expectations for all benchmark
  cases before performance assertions are accepted.
- Red tests must cover single pending-goal, multi-goal, deterministic,
  nondeterministic, and branching cases.
- Red tests must fail when a scheduling change preserves runtime but changes
  answers, proof acceptance, or expected open/closed status.
- Extended tests must record expected duration and branch-growth envelopes.
- Any optimization-specific benchmark must state the optimization it is meant
  to protect against, or remain a generic regression.

## Exit Criteria

- Scheduling benchmark namespaces are added to the appropriate fast and
  extended gates.
- Every benchmark has a baseline semantic expectation and an explicit runtime
  or branch-growth envelope.
- Focused benchmark tests and broad gates pass.
- AAR-0115 records baseline timings, branch-growth observations, and residual
  coverage gaps.
