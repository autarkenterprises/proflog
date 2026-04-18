# Execution Plan

Date: 2026-04-18
Integration branch: `greenfield`

## Current Facts

- The repository already contains an experimental Proflog implementation in `src/cljtap/` and `test/cljtap/`.
- The repository did not previously contain a mission statement, ADR stack, AAR stack, execution tracker, or semantic-variant policy.
- The greenfield effort will treat the experimental implementation as reference material, not as the codebase to incrementally polish into authority.
- `greenfield` is a fresh sandbox, so existing code may be removed, rewritten, or refactored wherever the active ADR and tests justify it.

## Branch Policy

- `greenfield` remains the integration branch for the new implementation.
- Each implementation ADR should normally use a feature branch named `adr-XXXX-short-name`.
- Feature branches merge into `greenfield` only after their ADR exit criteria are met and the relevant tests pass.
- Promotion from `greenfield` to `master` is reserved for coherent, regression-checked milestones.

## Planned Namespace Layout

The greenfield implementation should land in a fresh namespace tree:

```text
src/proflog/ast.clj
src/proflog/language.clj
src/proflog/normalize.clj
src/proflog/subst.clj
src/proflog/proof.clj
src/proflog/kernel.clj
src/proflog/equality.clj
src/proflog/program.clj
src/proflog/query.clj

test/proflog/ast_test.clj
test/proflog/language_test.clj
test/proflog/normalize_test.clj
test/proflog/subst_test.clj
test/proflog/kernel_test.clj
test/proflog/equality_test.clj
test/proflog/program_test.clj
test/proflog/query_test.clj
test/proflog/answers_test.clj
test/proflog/oracle/herbrand_test.clj
```

## ADR Sequence

| ADR | Status | Branch | Scope | Depends on | First failing tests to write | Exit criteria |
|---|---|---|---|---|---|---|
| [ADR-0001](adr/ADR-0001-greenfield-foundation.md) | completed | `greenfield` | mission, process, branch plan, ADR/AAR stack | none | none; documentation-only bootstrap | docs exist, naming is fixed, implementation order is explicit |
| [ADR-0002](adr/ADR-0002-language-and-semantic-boundary.md) | proposed | `adr-0002-language-boundary` | AST, language declaration, clause desugaring, NNF, substitution | ADR-0001 | `ast_test`, `language_test`, `normalize_test`, `subst_test` | language declaration enforced, surface syntax compiles to tagged core, NNF and substitution proven by tests |
| [ADR-0003](adr/ADR-0003-pure-relational-kernel.md) | proposed | `adr-0003-kernel` | proof terms, tableau kernel, base quantifier/connective rules | ADR-0002 | `kernel_test`, `proof_test` | αleanTAP-style pure relational kernel runs the base first-order tableau fragment |
| [ADR-0004](adr/ADR-0004-equality-kernel.md) | proposed | `adr-0004-equality` | free-constructor equality, occurs-check, disequality store | ADR-0003 | `equality_test`, `oracle/herbrand_test` | equality and disequality pass micro-tests and bounded Herbrand oracle checks |
| [ADR-0005](adr/ADR-0005-procedure-calls-and-query-api.md) | proposed | `adr-0005-calls-query` | program lookup/binding, subsidiary tableaux, succeed/fail race | ADR-0004 | `program_test`, `query_test` | Fitting `P1` and `P2` run end-to-end and query statuses are honest |
| [ADR-0006](adr/ADR-0006-answer-discipline-and-variant-boundary.md) | proposed | `adr-0006-answers-variants` | answer projection, residual constraints, proof replay, variant gating | ADR-0005 | `answers_test`, `query_test` open-query cases | exported answers are admissible and semantic variants are explicit |

## Deferred Tracks

The following work is intentionally downstream of the baseline implementation and should not be folded into earlier ADRs by default:

- tabling or memoization of calls,
- congruence-cache acceleration,
- bounded disunifier enumeration,
- arithmetic extensions beyond symbolic Peano coverage,
- any closed-world or Clark-completion semantic profile.

Each deferred track should become its own ADR if it graduates from backlog to active work.

## Merge Gate For Each ADR

- ADR status is updated from `proposed` to `accepted` before implementation starts.
- Failing tests for the ADR exist before production code lands.
- The relevant greenfield test namespaces pass.
- Existing regression suites needed for confidence still pass.
- The ADR has either a completed AAR or an explicit note that the AAR will be written immediately after merge because data collection is still pending.

## Working Loop

1. Start from the next accepted ADR in dependency order.
2. Write the narrowest failing tests that express the ADR success criteria.
3. Implement only enough code to make those tests pass, and do so substantively: do not satisfy tests by bypassing, defrauding, hard-coding around, or otherwise failing to implement the feature the tests are meant to capture.
4. Run the targeted greenfield tests and any necessary regression selectors.
5. Update the ADR, write or update the AAR, then merge back into `greenfield`.
