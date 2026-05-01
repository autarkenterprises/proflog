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

## 2026-05-01

- Accepted [ADR-0032](docs/adr/ADR-0032-core-logic-performance.md) on branch
  `adr-0032-core-logic-performance`. ADR-0032 carries forward ADR-0031's still
  failing ordinary/raw reverse and synthesis rows, but moves the next experiment
  below Proflog into generic `core.logic` host performance and deployment work.
  The initial research and deployment design is recorded in
  [Core.logic Performance Research and Design](docs/log/2026-05-01-core-logic-performance-research-design.md).
- Added a runtime `core.logic` host probe and a published-upgrade Leiningen
  profile for `org.clojure/core.logic` 1.1.1. The upgrade profile is compatible
  with the focused suites and is modestly faster on the carried raw matrix rows,
  but it does not close any carried reverse or synthesis target. Longer note:
  [Core.logic 1.1.1 Upgrade Probe](docs/log/2026-05-01-core-logic-1-1-1-upgrade-probe.md).
- Added a verified source-overlay deployment lane for local `core.logic` host
  patches. The `+core-logic-source-overlay` profile resolves
  `clojure/core/logic.clj` to `vendor/core.logic-1.1.1/src`, reports an
  ADR-0032 marker var, and passes the host, constructor-recursive, and fast
  Proflog suites. Longer note:
  [Core.logic Source Overlay Deployment](docs/log/2026-05-01-core-logic-source-overlay.md).
- Tested and rejected a tiny generic `core.logic/unify` fast path that returned
  immediately when both walked terms were identical. The patch was compatible
  with focused suites, but timing was mixed and it did not close any carried
  matrix target, so it was reverted. Longer note:
  [Core.logic Unify Identical-After-Walk Probe](docs/log/2026-05-01-core-logic-unify-identical-probe.md).
- Tested and rejected a generic `ISeq` walk structural-sharing patch in the
  source overlay. It passed focused suites but slowed two of three carried rows
  and closed none, so it was reverted. Longer note:
  [Core.logic ISeq Walk Sharing Probe](docs/log/2026-05-01-core-logic-iseq-walk-probe.md).
- Compared the pinned 1.0.1 JVM source with the published 1.1.1 JVM source and
  found no implementation diff in the reviewed files beyond Proflog's overlay
  marker. The 1.1.1 artifact updates POM metadata, but Proflog still runs
  Clojure 1.11.1 in all ADR-0032 profiles. Longer note:
  [Core.logic 1.0.1 vs 1.1.1 Source Comparison](docs/log/2026-05-01-core-logic-1-0-1-1-1-source-comparison.md).
- Probed `core.logic` tabling/reification internals before patching them. The
  carried rows allocate the ordinary tabled-capable substitution, but they do
  not exercise `AnswerCache`, `reuse`, `subunify`, tabled reification, or
  suspended streams. No production host patch was retained. Longer note:
  [Core.logic Tabling/Reification Probe](docs/log/2026-05-01-core-logic-tabling-reification-probe.md).
- Tested and rejected batched `run-constraints*` dispatch across changed
  variables. It passed focused compatibility tests, but it did not close carried
  rows and materially slowed one of them. Longer note:
  [Core.logic Constraint Run Batch Probe](docs/log/2026-05-01-core-logic-constraint-run-batch-probe.md).
- Added a bounded Proflog-side `core.logic` count probe. The carried
  `reverse-input-flat` row shows counted calls dominated by `walk*` /
  reification and unification, with tabling unused. Longer note:
  [Core.logic Count Probe](docs/log/2026-05-01-core-logic-count-probe.md).
- Tested and rejected two additional small stream/walk allocation patches:
  `Choice.take*` lazy-tail simplification and `LCons` walk structural sharing.
  Both preserved answer shape and were slower on the carried rows. Longer note:
  [Core.logic Stream/Walk Negative Probe](docs/log/2026-05-01-core-logic-stream-walk-negative-probe.md).
- Ran a diagnostic no-occurs-check source-overlay experiment after the count
  probe showed high `occurs-check` volume. It was somewhat faster on carried
  rows but still closed none, so no unsound production path was retained.
  Longer note:
  [Core.logic No Occurs-Check Diagnostic](docs/log/2026-05-01-core-logic-no-occurs-check-diagnostic.md).
- Logged the remaining generic `core.logic` optimization frontiers after the
  first wave of rejected micro-patches. ADR-0032 is not treating the host as
  exhausted; it is splitting vector-specialized unification and bounded
  walk/reification memoization into independent worktree experiments. Longer
  note:
  [Core.logic Remaining Optimization Frontiers](docs/log/2026-05-01-core-logic-remaining-frontiers.md).
- Evaluated the concurrent ADR-0032 vector-unification and walk/reify-memo
  workers. Both were rejected as implementation merge candidates: the vector
  path was generic and exercised but did not improve carried rows, and the
  walk/reify memo variants regressed runtime without closing targets. The main
  ADR-0032 branch retest kept host, constructor-recursive, fast, and CI-safe
  matrix checks green, while the three carried raw reverse rows and two
  synthesis-mode failures remain. Longer notes:
  [Concurrent Probe Evaluation](docs/log/2026-05-01-adr32-concurrent-core-logic-probe-evaluation.md),
  [Vector Unification Probe](docs/log/2026-05-01-core-logic-vector-unification-probe.md),
  and
  [Walk/Reify Memo Probe](docs/log/2026-05-01-core-logic-walk-reify-memo-probe.md).
- Added worked legacy/greenfield traces for the exact current ADR-32 failures.
  Legacy closes the three carried reverse shapes by letting bare host logic
  variables flow through ordinary `proveo`; greenfield's ordinary raw answer
  path still exports residual frontiers, even though the constructor-recursive
  sidecar closes those rows. The two synthesis failures are narrower:
  `jump(x, 0)` has the right ground set with a non-disequality residual, and
  `down(2, y)` has the right set in legacy order reversed. Longer note:
  [Legacy / Greenfield Failure Traces](docs/log/2026-05-01-legacy-greenfield-failure-traces.md).
- Logged design lessons from the legacy/greenfield traces. The next promising
  direction is answer-frontier repair: complete procedural residuals before
  export, preserve base-before-recursive ordering where appropriate, integrate
  constructor-recursive descent into the ordinary raw path, and keep
  structurally safe answer variables live through recursion rather than turning
  them into residual frontiers. Longer note:
  [Greenfield Lessons From Legacy Traces](docs/log/2026-05-01-greenfield-lessons-from-legacy-traces.md).
- Started [ADR-0033](docs/adr/ADR-0033-structural-answer-variable-recursion.md)
  on branch `adr-0033-structural-answer-variable-recursion`. ADR-0033 keeps
  ADR-0031's list-family goal but moves the next implementation strategy to
  structural answer-variable recursion in the greenfield raw answer path:
  structurally safe answer variables should remain live across recursive
  descent instead of becoming premature residual frontiers. Longer note:
  [Structural Answer-Variable Recursion Architecture](docs/log/2026-05-01-structural-answer-variable-recursion-architecture.md).
- Continued ADR-0033 with a generic structural residual-completion hook at the
  ordinary program answer export boundary. The focused carried rows now close
  through the ordinary raw matrix path, `proflog.synthesis-modes-test` passes,
  `test-proflog-constructor-recursive` and `test-proflog-fast` pass, and answer
  diagnostics still opt out to expose raw unresolved frontiers. Longer note:
  [ADR-33 Structural Completion Progress](docs/log/2026-05-01-adr33-structural-completion-progress.md).
- Added [Language Namespace Spec](docs/LANGUAGE_NAMESPACE_SPEC.md), a
  pedagogical specification of declaration normalization, validation,
  alpha-renaming, NNF compilation, compiled program views, guarded alternatives,
  demand ordering, and the public language/proof-kernel boundary.

## 2026-04-30

- Continued ADR-0031 by compiling and executing guarded clause alternatives in
  both the ordinary kernel and raw answer overlay. The promoted matrix now
  closes longer flat/nested ground append and reverse rows plus representative
  raw append output/suffix/prefix and reverse output rows. Reverse input and
  full inverse split enumeration remain bounded-search follow-up work.
- Reassessed the remaining ADR-0031 brainstormed enhancements after a narrow
  adaptive call-order experiment only improved `reverse(r, [b,a])`. The
  adaptive ordering was reverted; stricter residual deferral and residual
  frontier re-settlement were also rejected after slowing or regressing the
  matrix without closing length-three reverse rows. Longer note:
  [ADR-0031 Experiment Reassessment](docs/log/2026-04-30-adr31-experiment-reassessment.md).
- Registered generic `core.logic` host-language performance work as a possible
  ADR-0031 avenue. This includes tableau-prover specific handling only if it
  stays generic across Proflog programs, as well as fully general-purpose
  improvements that would benefit arbitrary `core.logic` programs.
- Tightened that avenue with prerequisites: upstream `core.logic` research,
  review of the exact patched implementation, a revised dependency/deployment
  sequence that selects the patched artifact or source path, and a runtime
  verification step proving Proflog is not still using the default dependency.
- Integrated ADR-0031 negative probe notes from parallel branches: the generic
  demand-selector idea regressed answer ordering, the answer-continuation
  prototype slowed passing rows without closing reverse blockers, and
  answer-path tabling diagnostics showed duplicate exported records rather than
  repeated raw proof families. Longer notes:
  [Demand IR Probe](docs/log/2026-04-30-adr31-demand-ir-worker2-probe.md),
  [Answer Continuation Probe](docs/log/2026-04-30-adr31-answer-continuation-probe.md),
  and [Answer-Path Tabling Probe](docs/probe/2026-04-30-adr31-answer-tabling.md).
- Collated all five ADR-0031 parallel sub-agent reports, including the parked
  structural-descent prototype and the promising constructor-recursive sidecar
  prototype. Longer note:
  [ADR-0031 Parallel Sub-Agent Reports](docs/log/2026-04-30-adr31-parallel-subagent-reports.md).
- Evaluated the parallel ADR-0031 experiments and merged the fruitful
  constructor-recursive sidecar prototype into the branch. It is generic over
  guarded constructor-recursive programs and closes representative blocked
  reverse/input, nested reverse/output, and append inverse-split matrix rows
  through an opt-in proof layer. Structural descent was parked as useful input
  but not merged because it did not improve reverse rows and left synthesis-mode
  failures.
- Closed ADR-0031 with AAR-0031. The branch is complete enough to merge back to
  `master`, but not because all list-family criteria are satisfied: ordinary
  raw reverse input, nested reverse output, reverse partial-output-tail, and two
  `proflog.synthesis-modes-test` failures are carried forward explicitly to
  ADR-0032.

## 2026-04-29

- Logged the ADR-0031 brainstorm for making list-family proof search genuinely
  family-parametric. The adopted order starts at the source-to-IR boundary:
  compile guarded alternatives, expose them relationally, then use them for
  guard-first recursive descent and answer residual handling before adding
  heavier tabling. Longer note:
  [List-Family Kernel Generalization Brainstorm](docs/log/2026-04-29-list-family-generalization-brainstorm.md).
- Completed [ADR-0030](docs/adr/ADR-0030-relational-constructor-search.md) on
  branch `adr-0030-relational-constructor-search`. The raw constructor-recursive
  list targets now close through the ordinary kernel using generic rigid
  constructor disequality discharge and call-local guarded alternatives; a
  non-list Peano recursive control is included in the new focused selector.
  See [AAR-0030](docs/aar/AAR-0030-relational-constructor-search.md).
- Added a raw-kernel append/reverse matrix to distinguish ADR-0030's ground
  closure improvement from remaining reverse and partial synthesis gaps. The
  matrix shows two-step flat and nested ground cases passing, longer outer-list
  ground cases timing out, and raw answer-mode synthesis rows failing to
  produce closed targets within the tested bounds. Longer note:
  [List Kernel Test Matrix](docs/log/2026-04-29-list-kernel-test-matrix.md).
- Reassessed ADR-0030 after the raw matrix. The branch is technically closed,
  but its result is too narrow to satisfy the family-level goal: arbitrary
  proper lists should be handled by a measurable recursive proof discipline,
  not only by selected two-step examples. Accepted
  [ADR-0031](docs/adr/ADR-0031-list-family-kernel-generalization.md) on branch
  `adr-0031-list-family-kernel-generalization` to revisit the work against
  deeper forward, reverse, partial, flat, and nested matrix rows.
- Accepted [ADR-0030](docs/adr/ADR-0030-relational-constructor-search.md) on
  branch `adr-0030-relational-constructor-search`. The plan treats the
  legacy-passing raw list proofs as constructor-recursive kernel search
  failures and proposes generic, pure relational improvements: rigid
  constructor disequality discharge, structural agenda focusing, guarded
  procedure-call descent, and optional call-stack descent preference. Longer
  note:
  [List-Family Kernel Search Plan](docs/log/2026-04-29-list-family-kernel-search-plan.md).
- Completed [ADR-0029](docs/adr/ADR-0029-relational-fuel-purity.md) for
  relational fuel stepping in `kernel_support.clj`. `step-fuelo` is now a
  structural finite-domain relation over unbounded `nil` and bounded fuel
  steps, with direct fuel synthesis, open-fuel `proveo`, and open-fuel
  procedure-call synthesis regressions. The discussion and examples are
  recorded in
  [Step Fuel Relational Purity Gap](docs/log/2026-04-29-step-fuelo-relational-purity-gap.md).
  Follow-up list-family probes showed this purity repair does not make the
  legacy-passing raw `append([a,b], [c], [a,b,c])` or
  `reverse([a,b], [b,a])` proofs close within a 45 second slice; the result is
  recorded in [AAR-0029](docs/aar/AAR-0029-relational-fuel-purity.md).
- Completed [ADR-0028](docs/adr/ADR-0028-kernel-support-disequality-purity.md)
  for saved disequality maintenance purity in `kernel_support.clj`.
  `prune-contradictory-neqso` and `stable-neqso` are now structural, with
  reverse/open branch-state regressions in `proflog.kernel-test`; the preceding
  analysis and examples are recorded verbatim in
  [Kernel Support Disequality Purity Gap](docs/log/2026-04-29-kernel-support-disequality-purity-gap.md).
- Completed [ADR-0027](docs/adr/ADR-0027-transitive-relational-purity.md) for
  transitive relational purity. `subst-formulao` is now structural rather than
  projected, reverse/partial substitution preimage regressions pass, and
  [AAR-0027](docs/aar/AAR-0027-transitive-relational-purity.md) records the
  remaining recursive synthesis boundaries.
- Logged a broader transitive purity risk: `proflog.subst/subst-formulao` uses
  `core.logic/project`, and that relation is called throughout the kernel and
  answer overlay. This is the basis for
  [ADR-0027](docs/adr/ADR-0027-transitive-relational-purity.md). Longer note:
  [`subst-formulao` Transitive Purity Risk](docs/log/2026-04-29-subst-formulao-transitive-purity-risk.md).
- Recovered the ADR-0026 branch profiler from a `core.logic/project`-based
  classifier to structural relational goals. This is now documented as a
  reusable example for preserving and recovering relational purity:
  [Structural Profiler Purity Recovery](docs/log/2026-04-29-structural-profiler-purity-recovery.md).
- Completed [ADR-0026](docs/adr/ADR-0026-kernel-layer-interoperation.md) for
  proof-producing kernel layer interoperation. The full program kernel can now
  close purified compound residual branches through propositional or
  equality-free first-order `proveo` relations, and
  [AAR-0026](docs/aar/AAR-0026-kernel-layer-interoperation.md) records the
  proof-boundary and partial-synthesis constraints.
- Logged the tableau foreground/background literature relevant to kernel layer
  interoperation. The key architectural note is that delegated branch closure
  must remain proof-producing; kernel purity rules out opaque background
  oracles. Longer note:
  [Tableau Foreground/Background Lessons](docs/log/2026-04-29-tableau-foreground-background-lessons.md).
- Accepted [ADR-0026](docs/adr/ADR-0026-kernel-layer-interoperation.md) to
  implement branch-level interoperation between the full program kernel and the
  propositional / equality-free first-order layers.
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
