# SJAS U-Grounding Formula-Bearing Proof Path

## Context

ADR-0073 Track 1 already had formula-bearing tableau certificates for the public
`tableau-proof/3` predicate. The remaining gap was representation: substantive
proof certificates still had to work when supplied as public U-Grounding
numerals, not only as compact `code-N` terms.

## Change

- Added a focused regression proving that public `tableau-proof/3` accepts a
  formula-bearing structural proof certificate encoded as a U-Grounding numeral.
- Extended the SJAS public code reader so proof codes can be decoded through the
  arithmeticized U-Grounding numeral relation without host byte projection.
- Kept compact `code-N` handling relational and optimized fixed axiom
  membership so compact axiom-citation regressions do not time out.
- Added acyclic unification only at constructor/identity boundaries where large
  ground SJAS object terms are being carried, avoiding host stack overflows from
  ordinary occurs checks.
- Preserved the generic tabled kernel path by making singleton agenda selection
  a structural `conde` split with an acyclic singleton branch and a guarded
  non-singleton `support/selecto` branch; committed scheduling remains limited
  to the theory-profile route.
- Updated structural source-slice audits to inspect
  `sjas-structural-proof-check-state-decodedo`, which is where formula-bearing
  recursive closures live.

## Red/Green Evidence

- Red: the new U-Grounding formula-bearing proof selector initially failed with
  a `StackOverflowError` in core.logic occurs checking.
- Green focused selectors:
  - `lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-u-grounding-formula-bearing-proof-certificate`
    passed, 1 test / 6 assertions, `elapsed 1:55.14 maxrss 421672KB`.
  - `lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-formula-bearing-true-theorem-certificates`
    passed, 1 test / 6 assertions.
  - `lein test :only proflog.willard-sjas-test/sjas-proof-predicates-do-not-require-source-preprocessing-registry`
    passed, 1 test / 4 assertions, latest observed `elapsed 3:17.85 maxrss 286596KB`.
  - `lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route`
    passed, 1 test / 128 assertions.
- Broad gates:
  - `lein test-proflog-fast` passed, 165 tests / 656 assertions,
    `elapsed 1:58.01 maxrss 404856KB`.
  - `lein test-proflog-extended` passed, 68 tests / 203 assertions,
    `elapsed 4:23.02 maxrss 576832KB`.
- `git diff --check` passed.

## Notes

The generic proof stream for a conjunction containing a disequality and a later
equality can expose an equality-first closure before the stored-disequality
closure. The proof regression now checks the bounded stream for the stored
disequality trace instead of assuming it is the first answer.
