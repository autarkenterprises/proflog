# Test Matrix

Date: 2026-04-18

This matrix is the release gate until a separate automated coverage tool is added. Every ADR must map its code changes to these obligations.

## Coverage Policy

- Every public relation or externally visible function must have at least one direct test.
- Every semantic rule must have at least one contradiction test and one non-closure test where applicable.
- Every major relation should be exercised in both straightforward forward use and at least one partially instantiated or reverse-style use when the semantics claim mode freedom.
- Any uncovered path must be called out in the ADR or AAR with a concrete reason.
- Performance instrumentation, debug printers, and intentionally unreachable guard code are the only acceptable routine exceptions.

## Matrix

| Area | Micro-tests | End-to-end tests | Oracle or property checks | Must pass before merge |
|---|---|---|---|---|
| AST and constructors | tagged term and formula shape tests | build a small program/query pair from the public constructors | none required | yes |
| Language declaration | arity checks, declared-symbol checks, clause merge/desugar checks | compile multi-clause surface input into single-clause Fitting form | none required | yes |
| NNF and negation | connective pushdown, quantifier duality, literal negation | negative procedure-call body preparation | round-trip checks for bounded formulas | yes |
| Substitution | variable lookup, nominal binding, parameter pass-through | clause instantiation for calls | parameter non-leak checks | yes |
| Base tableau kernel | alpha, beta, gamma, delta, complementary closure | classical first-order examples without programs | bounded theorem regression set | yes |
| Equality kernel | reflexivity, clash, injectivity, congruence, occurs-check, disequality-store behavior | equality inside subsidiary tableaux | bounded Herbrand evaluator for small signatures | yes |
| Procedure-call rule | positive call, negative call, recursion, mutual recursion | Fitting `P1` even/odd and `P2` Nim | clause biconditional spot-checks over bounded ground terms | yes |
| Query API | succeed/fail race, unresolved search budget handling | user-facing query helpers over sample programs | consistency checks between `A` and `not A` races | yes |
| Answer discipline | admissible substitutions, residual disequalities, proof attachment | open-query examples with quantified bodies | answer terms contain only language `L` symbols and no `par` | yes |
| Regression and performance | previously fixed bug cases, ordering/pathology cases | flagship program suite | bounded runtime budgets only after baseline correctness exists | yes |

## Flagship Program Families

- Fitting `P1`: even/odd.
- Fitting `P2`: Nim.
- Undefined self-reference such as `p <- not p`.
- Extensional definitions such as subset or set equality using universal quantification.
- Global specification examples such as sortedness or uniqueness over structural relations.

## Equality Oracle Requirements

The equality milestone is not complete until the greenfield implementation can be checked against a bounded direct evaluator over a tiny declared language. The oracle should compare:

- `eq` versus structural identity over free constructors,
- `neq` versus structural non-identity,
- complementary atom closure under the current walked substitution,
- small equality-bearing program clauses in subsidiary tableaux.

## Release Checklist

- New tests are organized by semantic area, not by edited source file.
- Every bug fix adds a regression test.
- Every optimization claim names the semantic surface it must preserve.
- The AAR for the merged ADR records which matrix rows were satisfied and which remain open.
