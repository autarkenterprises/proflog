# AAR-0093: Core.logic Canonical Regression Suite

- Date: 2026-06-10
- ADR: [ADR-0093](../adr/ADR-0093-core-logic-canonical-regression-suite.md)
- Branch: `adr-0093-core-logic-canonical-regressions`

## Outcome

Added `proflog.core-logic-canonical-test` as a core.logic-level regression and
miniKanren conformance namespace, and included it in `lein test-proflog-fast`.
The suite is intentionally below Proflog's proof kernel. It checks the
optimization-sensitive core.logic surface that ADR-0090 could have perturbed:
walk/reification, sound occurs-check rejection, fair `conde` search, classic
list relations, disequality and delayed constraints, project-local
`symbolo`/`numbero`/`absento`, nominal `fresh`/`hash`/`tie`, tabling with
ground keys, CLP(FD), and an explicit tagged-ground walk performance canary.

Per the user's portability requirement, the namespace also includes
literature-derived examples that other miniKanren implementers can recognize:
classic `appendo`/`membero`/`member1o`/`rembero` behavior, Peano natural
generation, and a tiny attributed relational interpreter that evaluates the
standard self-quoting quine from the "miniKanren, Live and Untagged" line of
work. The raw unconstrained `evalo q q` search is deliberately not in the fast
suite; instead, the fast suite checks the ground quine and a skeleton-based
backward query that still exercises residual symbol and disequality
constraints.

Review of ADR-0090 found no correctness regression in the patched surface. The
implementation remains conservative: maps, sets, records, and nominal records
are not ground-tagged; tagged metadata does not alter Clojure equality/hash; and
nominal/table keys are covered by the new tests. The one documented residual
risk is host infinite lazy sequences, which are outside the finite miniKanren
term model and are not made a fast regression test.

## Evidence

Focused red/green during test authoring:

- Initial canonical namespace run was red on an over-specified `conde`
  ordering assertion: core.logic returned the later finite branch first while
  still proving non-starvation. The assertion was corrected to the portable
  contract: the finite branch must appear within the bounded prefix.
- All eight test vars then passed individually with
  `lein test :only proflog.core-logic-canonical-test/<var>`.

Focused namespace gates:

```text
lein test :only proflog.core-logic-canonical-test
Ran 8 tests containing 53 assertions.
0 failures, 0 errors.

lein with-profile +core-logic-source-overlay test :only proflog.core-logic-canonical-test
Ran 8 tests containing 53 assertions.
0 failures, 0 errors.
```

Broad gates:

```text
/usr/bin/time -f "elapsed %E maxrss %MKB" lein test-proflog-fast
Ran 179 tests containing 732 assertions.
0 failures, 0 errors.
elapsed 5:33.00 maxrss 438040KB

/usr/bin/time -f "elapsed %E maxrss %MKB" lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 15:02.76 maxrss 565364KB
```

## Follow-up

- Build a separate extended miniKanren conformance suite for slower exact
  literature programs: fuller `evalo q q` quine synthesis, twines/quine cycles,
  larger relational interpreters from the 2012 appendix code, Send More Money
  and N-Queens CLP(FD), and selected pure relational arithmetic pearls.
- If ADR-0090 is ever proposed upstream, use this namespace as the local
  conformance evidence, but separate portable tests from Clojure/core.logic
  extension tests.
