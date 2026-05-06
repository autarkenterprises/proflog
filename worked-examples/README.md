# Worked Examples

This folder records concrete greenfield prover traces, answer records, and
query walkthroughs for the current `test/proflog` suites.

## Current Index

- [Frontend To Kernel Descent](./frontend-to-kernel-descent.md)
- [Query And Program Behavior](./query-and-program-behavior.md)
- [Reverse Program Synthesis](./reverse-program-synthesis.md)
- [Integration Families](./integration-families.md)
- [List Programs](./list-programs.md)
- [Quantified Programs](./quantified-programs.md)
- [Boxed Zero](./boxed-zero.md)
- [Program Calls](./program-calls.md)
- [Kernel And Proof Objects](./kernel-and-proof.md)
- [Query Boundaries](./query-boundaries.md)
- [Answers API](./answers-api.md)
- [Adversarial Cases](./adversarial-cases.md)
- [Equality And Disequality](./equality-and-disequality.md)
- [Existential Disequality Witness](./existential-disequality-witness.md)
- [Recursive Parity](./recursive-parity.md)
- [Herbrand Oracle](./herbrand-oracle.md)
- [Syntax And Normalization](./syntax-and-normalization.md)
- [Nim Synthesis](./nim-synthesis.md)
- [Synthesis Modes](./synthesis-modes.md)
- [Fitting Program Kernel Examples](./fitting-programs.md)
- [Kernel Finite Verifier Examples](./kernel-finite-verifiers.md)
- [Legacy Subsumption Parity Examples](./legacy-subsumption-parity.md)
- [Constructor-Recursive Profile Examples](./constructor-recursive-profile.md)
- [Pelletier Problems](./pelletier-problems.md)

## Reading Pattern

Every worked example should now be read through the same descent used in the
README quickstart:

```text
Fitting-style source
=> prefix frontend form, when the example is source-level
=> backend AST and compiled relation or direct formula
=> query, answer, or kernel entry point
=> proof status, exported answers, residuals, runtime, and shortcomings
```

The reusable details live in
[Frontend To Kernel Descent](./frontend-to-kernel-descent.md). Individual
examples below add local notes about the program family they exercise.

| Example | Main Boundary | Modes |
| --- | --- | --- |
| Query And Program Behavior | procedure-call source programs through `query-status` | success, failure, unresolved |
| Program Calls | compiled relation lookup and subsidiary tableaux | positive and negative calls |
| Quantified Programs | quantified clause bodies and frontend inlining | success, failure, quantified invariants |
| Fitting Program Kernel Examples | ADR-38 catalog over compiled Fitting programs | forward, answer, partial synthesis |
| List Programs | recursive list clauses and answer export | forward, answer, partial synthesis |
| Answers API | answer-overlay export from compiled queries | bindings, residuals, diagnostics |
| Kernel Finite Verifier Examples | profiled equality-fragment kernel path | quantified success and refutation |
| Constructor-Recursive Profile Examples | guarded recursive profile over compiled IR | reverse and partial synthesis |
| Pelletier Problems | direct theorem formulas without program clauses | first-order proof closure |

## Conventions

- Peano numerals use `zero`, `s(zero)`, `s(s(zero))`, and so on.
- List terms use `null` and `cons`.
- Residuals are recorded exactly when the answer exporter does not fully
  discharge the symbolic family.
- Proof terms are quoted in the current greenfield kernel vocabulary rather
  than translated into legacy notation.
- For current list-family reachability and timing, use
  [docs/log/2026-05-03-list-kernel-matrix-long-timeout-sweep.md](../docs/log/2026-05-03-list-kernel-matrix-long-timeout-sweep.md)
  rather than the older pre-ADR-0035 examples as the latest operational
  baseline.
