# SJAS Code-Format Registry Removal

Date: 2026-05-29
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This is a small Track 1 code-format slice for ADR-0073. The proof profile no
longer reads `:sjas/code-format` from the source-preprocessing registry while
closing syntax-code predicates.

The public code reader already relates a code term to bytes by trying the two
supported object-language representations:

- compact `code-N` terms;
- U-Grounding numeral terms with the sentinel byte convention.

The former `sjas-code-format` helper did not add semantic information; it was
a host metadata read left over from a search-order optimization. Removing it
narrows the registry boundary to the remaining finite symbol-index table.

## Change

The focused regression
`sjas-code-format-dispatch-does-not-read-source-registry` audits
`willard_sjas_profile.clj` for the removed helper. Syntax predicates continue
to decode compact and U-Grounding code terms through `sjas-formal-code-byteso`.

This does not remove the finite symbol-table boundary. Formula and reflected
clause decoding still resolve application-symbol indexes through
`:sjas/symbol-index-entries`, so formulas containing user or arithmetic
application symbols still require that codebook. The next Track 1 target is to
replace that registry lookup with a structurally internalized signature/codebook
or an internal-symbol proof checker that no longer has to project encoded
application symbols back to host AST symbols.

## Verification

```text
timeout -k 5s 60s lein test :only proflog.willard-sjas-test/sjas-code-format-dispatch-does-not-read-source-registry
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-u-grounding-syntax-predicates-decode-numeral-codes
  Ran 1 tests containing 8 assertions.
  0 failures, 0 errors.

timeout -k 5s 140s lein test :only proflog.willard-sjas-test/sjas-u-grounding-bound-code-decoding-uses-byte-cons-relation
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 140s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-do-not-require-source-preprocessing-registry
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.
```
