# Worked Examples

This folder records concrete greenfield prover traces, answer records, and
query walkthroughs for the current `test/proflog` suites.

## Current Index

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
- [Pelletier Problems](./pelletier-problems.md)

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
