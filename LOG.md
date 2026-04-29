# Development Log

This log is the project timeline: inclusive process notes, links to durable
records, exploratory turns, dead ends, backtracks, and decision context that may
not belong in a polished README, Memory, Lessons, ADR, or AAR.

It does not supersede specialized records. Instead, it is the spine from which
those views branch:

- [MEMORY.md](MEMORY.md) keeps high-priority facts that should remain present
  in working context.
- [LESSONS.md](LESSONS.md) captures lessons learned during the work.
- [ADR records](docs/adr/README.md) capture feature-sized decisions before
  implementation.
- [AAR records](docs/aar/README.md) capture post-implementation outcomes.
- `docs/log/` contains longer notes linked from dated entries here.

This file was introduced on 2026-04-29 after the project was already mature.
Entries before that date are reconstructed from git history and existing
documentation, so they intentionally summarize rather than pretend to be a
complete contemporaneous transcript.

## 2026-04-29

- Introduced this development log as a central timeline and documentation spine.
  The immediate prompt was a discussion about how to keep optimized Pelletier
  kernel layers useful inside general Proflog program execution, rather than
  only at the top-level theorem entry point. Longer note:
  [Kernel Layer Interoperation](docs/log/2026-04-29-kernel-layer-interoperation.md).
- Added a characterization test for the current layering gap on branch
  `pelletier-program-layering-gap-test`: two Pelletier subproblem relations
  close through theorem dispatch individually, but an aggregate Proflog program
  query remains on the full program kernel and does not reach the optimized
  first-order layer. Commit: `0522179`. Test:
  [pelletier_layering_test.clj](test/proflog/pelletier_layering_test.clj).
- Completed ADR-0025 and AAR-0025 for the lean Pelletier search policy. All
  Pelletier Problems 1-46 are now in the passing catalog without problem-id
  dispatch. See [ADR-0025](docs/adr/ADR-0025-pelletier-lean-search-policy.md),
  [AAR-0025](docs/aar/AAR-0025-pelletier-lean-search-policy.md), and
  [Pelletier Lean Search Policy Comparison](docs/PELLETIER_LEAN_SEARCH_POLICY_COMPARISON.md).

## 2026-04-28

- Completed the profiled-kernel sequence that made Pelletier progress possible.
  ADR-0023 introduced entry-only propositional dispatch, ADR-0024 introduced an
  equality-free first-order theorem layer and comparison report, and ADR-0025
  followed with alphaleanTAP-shaped search and narrow host-side Skolemization.
  See [ADR-0023](docs/adr/ADR-0023-profiled-kernel-layers.md),
  [ADR-0024](docs/adr/ADR-0024-pelletier-first-order-performance.md), and
  [Pelletier First-Order Comparison](docs/PELLETIER_FIRST_ORDER_COMPARISON.md).
- Memory was updated with detailed working-context records for ADR-0023 through
  ADR-0025. See [MEMORY.md](MEMORY.md).

## 2026-04-27

- Ported and classified the upstream Pelletier benchmark suite in greenfield.
  This created the baseline for later profiled-kernel work: passing problems,
  too-slow problems, and one propositional search problem that motivated the
  propositional layer. See
  [ADR-0022](docs/adr/ADR-0022-pelletier-problems.md),
  [AAR-0022](docs/aar/AAR-0022-pelletier-problems.md), and
  [worked-examples/pelletier-problems.md](worked-examples/pelletier-problems.md).
- Closed the gamma-candidate purity and search-repair sequence. ADR-0019 added
  bounded closed-term gamma candidates, ADR-0020 moved candidate choice outside
  the kernel path, and ADR-0021 repaired the regressions exposed by that
  boundary. See [ADR-0019](docs/adr/ADR-0019-closed-term-gamma-instantiation.md),
  [ADR-0020](docs/adr/ADR-0020-pure-gamma-candidate-boundary.md), and
  [ADR-0021](docs/adr/ADR-0021-gamma-search-regression-repair.md).
- Added and completed ADR-0018 around existential disequality witnesses,
  preserving the boundary between proof-time parameters and exportable
  object-language answers. See
  [ADR-0018](docs/adr/ADR-0018-existential-disequality-witnesses.md).

## 2026-04-26

- Reconstructed ADR scheduling, then completed the fair-agenda, micro-fuel, and
  relational tabling line of work. These records are the main source for the
  current proof-state scheduling and memoization story. See
  [ADR-0016](docs/adr/ADR-0016-fair-agenda-and-micro-fuel.md) and
  [ADR-0017](docs/adr/ADR-0017-relational-tabling-and-canonical-state.md).
- Recovered the hard-family overlay and legacy parity explorations that kept
  unresolved `GV` and related families visible while the greenfield kernel
  changed underneath. See [AAR-0014](docs/aar/AAR-0014-generic-legacy-evaluation.md).

## 2026-04-25

- Completed ADR-0015, extracting answer-oriented execution from the pure kernel
  into an overlay while leaving common proof mechanics in `kernel_support`.
  This became the project’s explicit pure-core / overlay boundary. See
  [ADR-0015](docs/adr/ADR-0015-answer-overlay-extraction.md),
  [AAR-0015](docs/aar/AAR-0015-answer-overlay-extraction.md), and
  [HANDOFF-2026-04-24-ADR-0015](docs/HANDOFF-2026-04-24-ADR-0015.md).

## 2026-04-24

- Concentrated on generic legacy evaluation, raw-kernel probes, group-verifier
  probes, and answer-performance boundaries. The project started treating
  "which layer first has the answer?" as a central diagnostic question. See
  [ADR-0014](docs/adr/ADR-0014-generic-legacy-evaluation.md),
  [AAR-0014](docs/aar/AAR-0014-generic-legacy-evaluation.md), and
  [LESSONS.md](LESSONS.md).
- Completed ADR-0013, improving relational answer performance while preserving
  the explicit closed-answer parity mode introduced by ADR-0012. See
  [ADR-0013](docs/adr/ADR-0013-relational-answer-performance.md).

## 2026-04-23

- Advanced open-answer relationality and closed-answer parity planning.
  ADR-0011 moved default open-answer search toward staged kernel descent, while
  ADR-0012 isolated long-running closed-answer parity work from the generic
  symbolic API. See [ADR-0011](docs/adr/ADR-0011-open-answer-relationality.md)
  and [ADR-0012](docs/adr/ADR-0012-closed-answer-parity-mode.md).

## 2026-04-22

- Deepened equality, procedure-call, list, quantified-program, synthesis, and
  documentation coverage. Several lessons from this date remain important:
  equality and disequality are operationally asymmetric; stale disequalities
  must be pruned after binding-producing steps; and first-order comparator
  relations should not be treated as higher-order predicate arguments. See
  [LESSONS.md](LESSONS.md).
- Added ADR-0010 for frontend inlining translation after sortedness and
  comparator examples made the language boundary explicit. See
  [ADR-0010](docs/adr/ADR-0010-frontend-inlining-translation.md).

## 2026-04-21

- Added ADR-0009 parity matrix work, worked examples, integration-family
  coverage, quantified clause-body executability, reverse program synthesis
  regressions, and baseline list program regressions. See
  [ADR-0009](docs/adr/ADR-0009-legacy-program-closure.md),
  [LEGACY_PROGRAM_PARITY_MATRIX](docs/LEGACY_PROGRAM_PARITY_MATRIX.md), and
  [worked-examples/README.md](worked-examples/README.md).

## 2026-04-20

- Recorded deeper Nim and `win(x)` probe results, then added symbolic answer
  export and synthesis coverage. These entries bridged ADR-0007 query
  remediation and the later answer-surface ADRs. See
  [ADR-0007](docs/adr/ADR-0007-nim-correctness-and-query-bounds.md) and
  [TEST_RUNTIME_BASELINE](docs/TEST_RUNTIME_BASELINE.md).

## 2026-04-18 to 2026-04-19

- Bootstrapped the greenfield process: execution plan, ADR/AAR stacks, branch
  policy, semantic boundary, pure relational kernel, equality kernel, procedure
  calls, query API, and query remediation baseline. See
  [EXECUTION_PLAN](docs/EXECUTION_PLAN.md),
  [ADR-0001](docs/adr/ADR-0001-greenfield-foundation.md),
  [ADR-0002](docs/adr/ADR-0002-language-and-semantic-boundary.md),
  [ADR-0003](docs/adr/ADR-0003-pure-relational-kernel.md),
  [ADR-0004](docs/adr/ADR-0004-equality-kernel.md),
  [ADR-0005](docs/adr/ADR-0005-procedure-calls-and-query-api.md), and
  [ADR-0007](docs/adr/ADR-0007-nim-correctness-and-query-bounds.md).
- Split slow recursive, reverse, and partial-synthesis regressions into the
  extended suite, a practice later codified in
  [development-practices.md](development-practices.md).

## 2026-04-03 to 2026-04-06

- Ran a performance-lab phase on the legacy/experimental implementation:
  forward execution, dual-engine dispatch, symbolic-to-fast cutover, lemma
  threading through cutover, equality closure optimization, neg-call caching,
  substitution caching, and non-default `Z4` group-verifier coverage. These
  were productive experiments but not the final greenfield architecture.

## 2026-03-13 to 2026-03-16

- Explored semantic variants and performance ideas in the experimental prover:
  closed-world / Clark-completion notes, L-ground guard justification,
  gamma-budgeted iterative deepening, lemma reuse between beta siblings, and
  group-verifier progress. See [SEMANTIC_VARIANTS](docs/SEMANTIC_VARIANTS.md).

## 2026-02-27 to 2026-03-08

- Initial experimental αleanTAP-E and αleanTAP-EP implementation work: equality,
  delta/existential handling, procedure-call rules, paramodulated closure,
  Nim/list/Peano program tests, groundness guards, equality-triggered procedure
  calls, and adversarial review cases. This phase remains reference material
  for greenfield comparisons rather than the authoritative design.
