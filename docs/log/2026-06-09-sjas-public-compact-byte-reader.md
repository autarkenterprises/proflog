# SJAS Public Compact Byte Reader

## Context

The ADR-0073 Track 1 audit found that public compact SJAS code readers were
using the byte-first builder relation for byte arguments. That builder is
correct for embedded payload reconstruction after bytes have already been
decoded, but public input must parse the presented U-Grounding numeral
structure.

## Change

- Added ADR-0083.
- Added a red source audit for the public compact-code reader shape.
- Changed `code-argso` and `code-args-coreo` to call `code-byte-termo`.
- Kept `code-byte-build-termo` in `code-args-buildo` for byte-first embedded
  reconstruction.

## Evidence

The focused source audit failed red with the intended public-reader failures:

```text
sjas-public-compact-code-readers-parse-presented-byte-numerals
4 expected assertion failures
elapsed 0:34.47 maxrss 307736KB
```

After the implementation:

```text
sjas-public-compact-code-readers-parse-presented-byte-numerals
Ran 1 tests containing 5 assertions.
0 failures, 0 errors.
elapsed 0:45.00 maxrss 263828KB

sjas-compact-code-byte-reader-interprets-byte-numerals-arithmetically
Ran 1 tests containing 2 assertions.
0 failures, 0 errors.
elapsed 0:46.33 maxrss 276068KB

sjas-profile-source-audit-rejects-host-proof-checker-route
Ran 1 tests containing 128 assertions.
0 failures, 0 errors.
elapsed 0:57.09 maxrss 234768KB

sjas-tableau-proof-accepts-u-grounding-formula-bearing-proof-certificate
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 1:07.18 maxrss 393864KB

sjas-proof-code-decoder-checks-wide-formula-bearing-tableau-nodes
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
elapsed 1:00.73 maxrss 287164KB
```

Broad gates:

```text
lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 3:41.77 maxrss 411656KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 8:40.19 maxrss 588716KB
```

## Notes

This closes a public-code reader arithmeticization gap. It does not make any
claim that the current-source public SelfCons proof predicate probe has
finished; that probe remains tracked in the ADR-0073 Track 1 audit.
