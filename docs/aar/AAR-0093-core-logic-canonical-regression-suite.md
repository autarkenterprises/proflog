# AAR-0093: Core.logic Canonical Regression Suite

- Date: 2026-06-10
- ADR: [ADR-0093](../adr/ADR-0093-core-logic-canonical-regression-suite.md)
- Branch: `adr-0093-core-logic-canonical-regressions`

## Outcome

Added `proflog.core-logic-canonical-test` as a core.logic-level regression and
miniKanren conformance namespace, and included it in `lein test-proflog-fast`.
Then, after user review pointed out that the extended follow-up suite had only
been documented rather than written, added
`proflog.core-logic-canonical-extended-test` to `lein test-proflog-extended`.
Both suites are intentionally below Proflog's proof kernel. They check the
optimization-sensitive core.logic surface that ADR-0090 could have perturbed:
walk/reification, sound occurs-check rejection, fair `conde` search, classic
list relations, disequality and delayed constraints, project-local
`symbolo`/`numbero`/`absento`, nominal `fresh`/`hash`/`tie`, tabling with
ground keys, CLP(FD), and explicit tagged-ground walk performance canaries.

Per the user's portability requirement, the namespaces also include
literature-derived examples that other miniKanren implementers can recognize:
classic `appendo`/`membero`/`member1o`/`rembero` behavior, Peano natural
generation, and a tiny attributed relational interpreter that evaluates the
standard self-quoting quine from the "miniKanren, Live and Untagged" line of
work. The extended suite now adds the paper's first twine shape, a relational
twine-shape check, SEND+MORE=MONEY, all 92 8-queens FD solutions, and backward
binary multiplication factorization for 30. A direct raw
`(run 1 [q] (evalo q '() q))` probe against the tiny interpreter did not finish
within a 90-second bounded run, so the committed regression uses the exact
generated quine/twine shapes rather than making a nonterminating raw search part
of the suite.

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

lein test :only proflog.core-logic-canonical-extended-test
Ran 4 tests containing 10 assertions.
0 failures, 0 errors.

lein with-profile +core-logic-source-overlay test :only proflog.core-logic-canonical-extended-test
Ran 4 tests containing 10 assertions.
0 failures, 0 errors.
```

Extended vars also passed individually:
`live-untagged-quine-and-twine-pearls` (6 assertions),
`send-more-money-cryptarithmetic` (1 assertion),
`eight-queens-fd-counts-all-solutions` (2 assertions), and
`pure-relational-binary-arithmetic-factorization` (1 assertion).

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

After adding proflog.core-logic-canonical-extended-test:

/usr/bin/time -f "elapsed %E maxrss %MKB" lein test-proflog-fast
Ran 179 tests containing 732 assertions.
0 failures, 0 errors.
elapsed 9:46.96 maxrss 427008KB

/usr/bin/time -f "elapsed %E maxrss %MKB" lein test-proflog-extended
Ran 72 tests containing 213 assertions.
0 failures, 0 errors.
elapsed 22:46.99 maxrss 623292KB
```

## Follow-up

- A raw, unconstrained `evalo q q` quine-generation regression still needs a
  more literature-faithful interpreter/ordering than the tiny fast-suite
  interpreter; the tiny interpreter's raw query exceeded a 90-second bounded
  probe. The committed suite covers exact generated quine/twine behavior and
  relational twine-shape checking instead.
- Further extended conformance candidates remain: larger relational
  interpreters from the 2012 appendix code, twine-cycle synthesis beyond the
  first documented pair, Zebra, Sudoku, and additional pure relational
  arithmetic pearls.
- If ADR-0090 is ever proposed upstream, use this namespace as the local
  conformance evidence, but separate portable tests from Clojure/core.logic
  extension tests.
