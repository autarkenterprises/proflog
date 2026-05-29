# SJAS Syntax Symbol-ID Decoder

Date: 2026-05-29
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This Track 1 syntax slice removes the finite source-time symbol table from
`wff`, formula-class, and `neg-pair` code checks.

Formula byte streams already carry enough structure for syntax predicates:

- a formula constructor tag;
- for application terms, an application tag;
- a positive numeric symbol id;
- an arity byte;
- recursively encoded argument terms.

The syntax predicates do not need to know whether symbol id `n` denotes host
symbol `lt`, `demo`, or any other name. They only need to prove that the byte
stream is a well-formed formula tree and, for `neg-pair`, that the two decoded
trees are structural complements. The new syntax decoder therefore keeps
application heads as structural `(sym n)` terms instead of resolving `n`
through `:sjas/symbol-index-entries`.

## Red/Green Evidence

The focused regression
`sjas-syntax-predicates-decode-application-codes-without-symbol-registry`
removes `:sjas/registry` from a compiled SJAS program and asks `wff` to decode
the code for the application-bearing formula `lt(1, 2)`.

Before the change, this failed because `decode-app-termo` called
`sjas-symbol-indexo`, which had no entries without the source registry:

```text
FAIL in (sjas-syntax-predicates-decode-application-codes-without-symbol-registry)
expected: (successful? wff-proofs)
  actual: (not (successful? ()))

Ran 1 tests containing 4 assertions.
2 failures, 0 errors.
```

After the change, the same selector passes and its proof still contains the
object-level compact-code byte-reader evidence.

## Remaining Boundary

This does not complete symbol-table internalization. Proof-facing decoding
still uses `sjas-symbol-indexo` when it must recover host-symbol names for:

- fixed arithmetic/proof-predicate relation recognition;
- reflected Group-2b clause formulas and procedure-call bodies;
- conversion from decoded formula-code trees to ordinary Proflog AST terms.

The next Track 1 symbol-code step must either encode enough signature data in
`system-code` for those proof-facing decoders, or move more of the proof
checker onto internal numeric symbol ids so those host-symbol projections are
unneeded.

## Verification

```text
timeout -k 5s 90s lein test :only proflog.willard-sjas-test/sjas-syntax-predicates-decode-application-codes-without-symbol-registry
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-syntax-predicates-decode-formula-godel-codes
  Ran 1 tests containing 15 assertions.
  0 failures, 0 errors.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-u-grounding-syntax-predicates-decode-numeral-codes
  Ran 1 tests containing 8 assertions.
  0 failures, 0 errors.

timeout -k 5s 140s lein test :only proflog.willard-sjas-test/sjas-u-grounding-bound-code-decoding-uses-byte-cons-relation
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-structural-code-predicates-accept-non-generated-formula-codes
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.

timeout -k 5s 100s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-do-not-require-source-preprocessing-registry
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.
```
