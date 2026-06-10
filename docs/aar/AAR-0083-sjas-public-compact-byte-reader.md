# AAR-0083: SJAS Public Compact Byte Reader

- Date: 2026-06-09
- ADR: [ADR-0083](../adr/ADR-0083-sjas-public-compact-byte-reader.md)
- Branch: `adr-0073-sjas-correspondence-program`

## Outcome

Closed a Track 1 public-code arithmeticization gap in compact SJAS code
reading. `code-argso` and `code-args-coreo` now parse each presented public
byte numeral with `code-byte-termo`. The byte-first reconstruction relation,
`code-byte-build-termo`, remains in `code-args-buildo` for embedded payloads
whose bytes have already been decoded.

This was a correctness/internalization change. It did not optimize SelfCons
search and did not change the intended public proof predicate surface.

## Evidence

The focused source audit failed red before implementation with the intended
four assertion failures: the public proof-producing and proof-free compact-code
readers did not call `code-byte-termo`, and both still called
`code-byte-build-termo`.

```text
sjas-public-compact-code-readers-parse-presented-byte-numerals
4 expected assertion failures
elapsed 0:34.47 maxrss 307736KB
```

After the reader change, the focused audit passed:

```text
sjas-public-compact-code-readers-parse-presented-byte-numerals
Ran 1 tests containing 5 assertions.
0 failures, 0 errors.
elapsed 0:45.00 maxrss 263828KB
```

Related focused checks passed:

```text
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

The post-change broad gates passed:

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

## Follow-up

Continue ADR-0073 Track 1 audit against the public SelfCons `tableau-proof/3`
MVP probe. The current-source public probe was still running when this AAR was
written, so ADR-0083 should be counted as closing a reader gap, not as proving
the full Track 1 endpoint.
