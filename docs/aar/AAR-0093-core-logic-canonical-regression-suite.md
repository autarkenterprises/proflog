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
work. The extended suite now adds a paper-faithful raw
`(run 1 [q] (evalo q '() q))` quine-generation test, the paper's first twine
shape, a relational twine-shape check, SEND+MORE=MONEY, all 92 8-queens FD
solutions, and backward binary multiplication factorization for 30. The first
raw probe against the tiny fast-suite interpreter exceeded a 90-second bounded
run; adapting the paper's extended `eval-expo` with `absento closure` and
relational `proper-listo` made the raw query complete and promoted it into the
extended suite.

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
Ran 5 tests containing 16 assertions.
0 failures, 0 errors.

lein with-profile +core-logic-source-overlay test :only proflog.core-logic-canonical-extended-test
Ran 5 tests containing 16 assertions.
0 failures, 0 errors.
```

Extended vars also passed individually:
`live-untagged-quine-and-twine-pearls` (6 assertions),
`raw-evalo-quine-generation-completes` (6 assertions),
`send-more-money-cryptarithmetic` (1 assertion),
`eight-queens-fd-counts-all-solutions` (2 assertions), and
`pure-relational-binary-arithmetic-factorization` (1 assertion).

Raw quine experiment, recorded durably in
`test-runs/raw-evalo-quine-faithful-20260610T191619Z.log`:

```text
:raw-evalo-quine-answers ((((lambda (_0) (list _0 (list (quote quote) _0)))
  (quote (lambda (_0) (list _0 (list (quote quote) _0)))))
  :- (!= (_0 list)) (!= (_0 quote)) symbolo (absento closure _0))
elapsed 0:53.53 maxrss 217532KB
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

After adding `proflog.core-logic-canonical-extended-test` and promoting the
raw `evalo` quine regression:

/usr/bin/time -f "elapsed %E maxrss %MKB" lein test-proflog-fast
Ran 179 tests containing 732 assertions.
0 failures, 0 errors.
elapsed 8:01.09 maxrss 421256KB

/usr/bin/time -f "elapsed %E maxrss %MKB" lein test-proflog-extended
Ran 73 tests containing 219 assertions.
0 failures, 0 errors.
elapsed 3:04.35 maxrss 531468KB

/usr/bin/time -f "elapsed %E maxrss %MKB" lein with-profile +core-logic-source-overlay test-proflog-fast
Ran 179 tests containing 732 assertions.
0 failures, 0 errors.
elapsed 1:05.68 maxrss 387740KB

/usr/bin/time -f "elapsed %E maxrss %MKB" lein with-profile +core-logic-source-overlay test-proflog-extended
Ran 73 tests containing 219 assertions.
0 failures, 0 errors.
elapsed 3:11.65 maxrss 557292KB
```

## Follow-up

- Further extended conformance candidates remain: larger relational
  interpreters from the 2012 appendix code, twine-cycle synthesis beyond the
  first documented pair, Zebra, Sudoku, and additional pure relational
  arithmetic pearls.
- If ADR-0090 is ever proposed upstream, use this namespace as the local
  conformance evidence, but separate portable tests from Clojure/core.logic
  extension tests.
