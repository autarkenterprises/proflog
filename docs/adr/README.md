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
| [ADR-0007](ADR-0007-nim-correctness-and-query-bounds.md) | accepted | `adr-0007-nim-correctness-query-bounds` | ADR-0005 remediation for Nim correctness and bounded query control | pending |
| [ADR-0008](ADR-0008-test-gap-closure.md) | accepted | `adr-0008-test-gap-closure` | greenfield test-gap closure and reverse-synthesis feasibility | pending |
| [ADR-0009](ADR-0009-legacy-program-closure.md) | accepted | `adr-0009-legacy-program-closure` | legacy program-family closure, worked examples, and semantic/performance findings | pending |
| [ADR-0010](ADR-0010-frontend-inlining-translation.md) | proposed | `adr-0010-frontend-inlining` | ergonomic helper-predicate translation into prover-amenable inline core form | pending |
| [ADR-0011](ADR-0011-open-answer-relationality.md) | accepted | `adr-0011-open-answer-relationality` | default greenfield open-answer execution via staged kernel descent rather than eager pre-unfolding | pending |
| [ADR-0012](ADR-0012-closed-answer-parity-mode.md) | completed | `adr-0012-closed-answer-parity-mode` | long-running closed-answer parity search mode kept separate from the generic symbolic API | [AAR-0012](../aar/AAR-0012-closed-answer-parity-mode.md) |
| [ADR-0013](ADR-0013-relational-answer-performance.md) | completed | `adr-0013-relational-answer-performance` | recursive nonground answer descent, frontier canonicalization, and residual normalization to reduce specialty modes | [AAR-0013](../aar/AAR-0013-relational-answer-performance.md) |
| [ADR-0014](ADR-0014-generic-legacy-evaluation.md) | accepted | `adr-0014-generic-legacy-evaluation` | generic evaluation of still-unsatisfied legacy families through raw-stream probes, generic post-processing, and explicit layer accounting | pending |
