# AAR-0022: Pelletier Problem Replication

- Date: 2026-04-27
- Related ADR: [ADR-0022](../adr/ADR-0022-pelletier-problems.md)
- Outcome: completed for the initial greenfield catalog and Problems 1-20 port

## What Happened

ADR-0022 added `test/proflog/pelletier_test.clj` as a pure-theorem benchmark
for the greenfield kernel. The test namespace treats the upstream
`namin/leanTAP` Clojure and Scheme files as the formula source of record.

The port uses local formula builders for implication, equivalence, quantifiers,
and branch construction, but those builders compile directly to existing
`proflog.ast` forms. A theorem is tested by proving the NNF of:

```clojure
axiom-1 and ... and axiom-n and not(theorem)
```

No Proflog program clauses, overlays, or theorem-specific recognizers were
added.

## Results

- Problems 1-11 and 13-20 are `ported-passing`.
- Problem 12 is `ported-too-slow`: the formula is ported, but a fresh-process
  probe did not find a proof within 120 seconds.
- Problems 21-46 are explicitly cataloged as `not-yet-ported`.
- The already mirrored legacy slice, Problems 1, 2, and 18, now has greenfield
  coverage through the ordinary kernel.

Observed targeted timings:

| Problem | Result | Timing |
|---:|---|---:|
| 10 | passed | 30405 ms |
| 12 | too slow | >120000 ms |
| 17 | passed | 35809 ms |
| 20 | passed | 7611 ms |

Problem 12 is therefore a search-performance boundary, not a hidden passing
regression.

## Selectors

ADR-0022 added three dedicated aliases:

- `lein test-proflog-pelletier-prompt`
- `lein test-proflog-pelletier`
- `lein test-proflog-pelletier-exploratory`

The default Pelletier alias runs the currently passing tranche and remains
separate from `test-proflog-fast` and `test-proflog-extended`.

## Verification

- `timeout 120s lein test-proflog-pelletier-prompt`
  - `Ran 2 tests containing 20 assertions.`
  - `0 failures, 0 errors.`
- `timeout 180s lein test-proflog-pelletier`
  - `Ran 3 tests containing 22 assertions.`
  - `0 failures, 0 errors.`
- `timeout 60s lein test-proflog-pelletier-exploratory`
  - `Ran 1 tests containing 2 assertions.`
  - `0 failures, 0 errors.`
- `timeout 180s lein test proflog.pelletier-test`
  - `Ran 5 tests containing 26 assertions.`
  - `0 failures, 0 errors.`

## Follow-Up

Problem 12 should be the first follow-up if the next branch focuses on
propositional search performance. Problems 21-46 remain visible as
`not-yet-ported` catalog entries and can be ported in staged tranches without
weakening the pure-kernel boundary.
