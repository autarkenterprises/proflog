# ADR Index

Date: 2026-04-18

Use ADRs for every feature-sized decision before implementation starts. Each ADR should name its intended branch, exit criteria, and test obligations. When the ADR is complete, write its AAR in `docs/aar/`.

## Records

| ADR | Status | Branch | Scope | AAR |
|---|---|---|---|---|
| [ADR-0000](ADR-0000-template.md) | template | n/a | template | n/a |
| [ADR-0001](ADR-0001-greenfield-foundation.md) | completed | `greenfield` | process bootstrap and greenfield structure | [AAR-0001](../aar/AAR-0001-greenfield-foundation.md) |
| [ADR-0002](ADR-0002-language-and-semantic-boundary.md) | completed | `adr-0002-language-boundary` | language declaration, AST, NNF, substitution | [AAR-0002](../aar/AAR-0002-language-and-semantic-boundary.md) |
| [ADR-0003](ADR-0003-pure-relational-kernel.md) | completed | `adr-0003-kernel` | base tableau kernel and proof terms | [AAR-0003](../aar/AAR-0003-pure-relational-kernel.md) |
| [ADR-0004](ADR-0004-equality-kernel.md) | completed | `adr-0004-equality` | equality and disequality kernel | [AAR-0004](../aar/AAR-0004-equality-kernel.md) |
| [ADR-0005](ADR-0005-procedure-calls-and-query-api.md) | completed | `adr-0005-calls-query` | calls, programs, query race | [AAR-0005](../aar/AAR-0005-procedure-calls-and-query-api.md) |
| [ADR-0006](ADR-0006-answer-discipline-and-variant-boundary.md) | proposed | `adr-0006-answers-variants` | answer admissibility and variant control | pending |
| [ADR-0007](ADR-0007-nim-correctness-and-query-bounds.md) | completed | `adr-0007-nim-correctness-query-bounds` | ADR-0005 remediation for Nim correctness and bounded query control | [AAR-0007](../aar/AAR-0007-nim-correctness-and-query-bounds.md) |
| [ADR-0008](ADR-0008-test-gap-closure.md) | completed | `adr-0008-test-gap-closure` | greenfield test-gap closure and reverse-synthesis feasibility | [AAR-0008](../aar/AAR-0008-test-gap-closure.md) |
| [ADR-0009](ADR-0009-legacy-program-closure.md) | completed | `adr-0009-legacy-program-closure` | legacy program-family closure, worked examples, and semantic/performance findings | [AAR-0009](../aar/AAR-0009-legacy-program-closure.md) |
| [ADR-0010](ADR-0010-frontend-inlining-translation.md) | proposed | `adr-0010-frontend-inlining` | ergonomic helper-predicate translation into prover-amenable inline core form | pending |
| [ADR-0011](ADR-0011-open-answer-relationality.md) | completed | `adr-0011-open-answer-relationality` | default greenfield open-answer execution via staged kernel descent rather than eager pre-unfolding | [AAR-0011](../aar/AAR-0011-open-answer-relationality.md) |
| [ADR-0012](ADR-0012-closed-answer-parity-mode.md) | completed | `adr-0012-closed-answer-parity-mode` | long-running closed-answer parity search mode kept separate from the generic symbolic API | [AAR-0012](../aar/AAR-0012-closed-answer-parity-mode.md) |
| [ADR-0013](ADR-0013-relational-answer-performance.md) | completed | `adr-0013-relational-answer-performance` | recursive nonground answer descent, frontier canonicalization, and residual normalization to reduce specialty modes | [AAR-0013](../aar/AAR-0013-relational-answer-performance.md) |
| [ADR-0014](ADR-0014-generic-legacy-evaluation.md) | accepted | `adr-0014-generic-legacy-evaluation` | generic evaluation of still-unsatisfied legacy families through raw-stream probes, generic post-processing, and explicit layer accounting | [AAR-0014](../aar/AAR-0014-generic-legacy-evaluation.md) |
| [ADR-0015](ADR-0015-answer-overlay-extraction.md) | completed | `adr-0015-answer-overlay` | extract answer-oriented execution flow from the kernel into a separate overlay namespace | [AAR-0015](../aar/AAR-0015-answer-overlay-extraction.md) |
| [ADR-0016](ADR-0016-fair-agenda-and-micro-fuel.md) | completed | `adr-0016-fair-scheduling` | fair agenda scheduling and refined micro-step fuel | [AAR-0016](../aar/AAR-0016-fair-agenda-and-micro-fuel.md) |
| [ADR-0017](ADR-0017-relational-tabling-and-canonical-state.md) | completed | `adr-0017-relational-tabling` | separate relational tabling and canonical proof-state reuse | [AAR-0017](../aar/AAR-0017-relational-tabling-and-canonical-state.md) |
| [ADR-0018](ADR-0018-existential-disequality-witnesses.md) | completed | `adr-0018-existential-disequality-witnesses` | accurate object-language witnesses for existential disequality programs | [AAR-0018](../aar/AAR-0018-existential-disequality-witnesses.md) |
| [ADR-0019](ADR-0019-closed-term-gamma-instantiation.md) | completed | `adr-0019-closed-term-gamma-instantiation` | generic bounded closed-term generation for Fitting gamma instantiation | [AAR-0019](../aar/AAR-0019-closed-term-gamma-instantiation.md) |
| [ADR-0020](ADR-0020-pure-gamma-candidate-boundary.md) | completed | `adr-0020-0021-gamma-purity-regressions` | remove projected gamma candidate choice from the kernel path | [AAR-0020](../aar/AAR-0020-pure-gamma-candidate-boundary.md) |
| [ADR-0021](ADR-0021-gamma-search-regression-repair.md) | completed | `adr-0020-0021-gamma-purity-regressions` | repair closed-term gamma search regressions in extended suites | [AAR-0021](../aar/AAR-0021-gamma-search-regression-repair.md) |
| [ADR-0022](ADR-0022-pelletier-problems.md) | completed | `adr-0022-pelletier-problems` | replicate the upstream alphaleanTAP Pelletier problem benchmark suite in greenfield | [AAR-0022](../aar/AAR-0022-pelletier-problems.md) |
| [ADR-0023](ADR-0023-profiled-kernel-layers.md) | completed | `adr-0023-profiled-kernel-layers` | formula-profiled kernel layers that preserve a readable Proflog rule structure | [AAR-0023](../aar/AAR-0023-profiled-kernel-layers.md) |
| [ADR-0024](ADR-0024-pelletier-first-order-performance.md) | accepted | `adr-0024-pelletier-first-order-performance` | equality-free first-order performance closure for remaining Pelletier problems | pending |
