# Core.logic Canonical Regression Suite Survey

Date: 2026-06-10

## Purpose

ADR-0090 optimized the host miniKanren engine instead of Proflog-specific SJAS
relations. That is the right layer for repeated walks over large ground proof
codes, but it expands the regression obligation: the test suite must cover the
canonical miniKanren/core.logic surface, not only the single SJAS workload shape.

## Literature-Derived Surface

- The miniKanren project presents the core as a small relational language and
  explicitly identifies constraint logic programming, nominal logic programming,
  and tabling as extensions in the family.
- Core.logic advertises the same Clojure surface: Prolog-like relational
  programming, constraint logic programming, nominal logic programming, and an
  implementation rooted in Byrd's miniKanren dissertation plus cKanren and
  alphaKanren.
- microKanren gives the minimal core to preserve: unification, fresh variables,
  conjunction, disjunction, reification, and complete interleaving search.
- cKanren adds the important constraint obligations: disequality over trees,
  finite domains, constraint ordering independence for productive queries, and
  residual reification.
- alphaKanren adds names, freshness, and binding terms; those structures are
  deliberately outside ADR-0090's ground-tag grammar and need semantic tests.
- Byrd, Holk, and Friedman make relational interpreters and quine generation a
  canonical miniKanren programming pearl; Hemann and Friedman later use quine
  and quine-cycle synthesis tasks as compact stress tests for program synthesis
  behavior. The fast suite should include a tiny attributed interpreter and
  quine query rather than merely engine micro-tests.
- Byrd's dissertation motivates tabling as the canonical performance extension
  for relations whose repeated subcalls should reuse answers; tabled answer
  keys and tabled reification must not be affected by metadata tagging.

## Test Matrix

- Core miniKanren: run/run*, fresh, ==, occurs-check rejection, conde
  interleaving, reified variable order, onceo, project.
- List relations: appendo forward and backward, membero/member1o duplicate
  behavior, lcons improper-tail preservation.
- Constraints: != satisfaction/rejection/residuals, project-local symbolo,
  numbero, and absento delayed checks.
- Classic programs: `rembero` and a tiny relational interpreter that evaluates
  the standard self-quoting quine and can synthesize the quine binder with
  residual symbol/disequality constraints.
- Nominal: nominal/fresh, nominal/hash, and nominal/tie unification across
  alpha-renaming, with ground payloads present inside nominal bodies.
- Tabling: answer reuse over repeated tabled calls, partially open arguments,
  and ground structures large enough to exercise the ADR-0090 tag path.
- CLP(FD): interval/domain membership, +, *, <, !=, distinct, and eq sugar.
- Performance canaries: bounded walk/occurs probes with expected durations in
  metadata. Hour-scale SJAS probes remain in the SJAS suite and durable
  `test-runs/` logs, not in the canonical fast namespace.

## Residual Risks

- Infinite host lazy sequences are outside finite miniKanren terms. ADR-0090
  scans finite `seq?` terms, so the canonical suite should cover finite lazy
  sequences but should not make nontermination over infinite host sequences a
  regression test.
- The suite is intentionally not exhaustive over every finite-domain puzzle,
  every full relational interpreter, or every nominal theorem-prover use. It is
  a canonical canary layer below the existing Proflog/SJAS semantic tests; a
  later extended conformance suite can host slower pearls such as fuller
  quine-cycle/twine generation, larger CLP(FD) puzzles, and broader relational
  arithmetic examples.
