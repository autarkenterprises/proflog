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
- [Query Boundaries](./query-boundaries.md)
- [Answers API](./answers-api.md)
- [Adversarial Cases](./adversarial-cases.md)
- [Equality And Disequality](./equality-and-disequality.md)
- [Recursive Parity](./recursive-parity.md)
- [Syntax And Normalization](./syntax-and-normalization.md)
- [Nim Synthesis](./nim-synthesis.md)
- [Synthesis Modes](./synthesis-modes.md)

## Conventions

- Peano numerals use `zero`, `s(zero)`, `s(s(zero))`, and so on.
- List terms use `null` and `cons`.
- Residuals are recorded exactly when the answer exporter does not fully
  discharge the symbolic family.
- Proof terms are quoted in the current greenfield kernel vocabulary rather
  than translated into legacy notation.
