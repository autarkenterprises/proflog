# AAR-0076: Stack-Safe Language Term Validation

- Date: 2026-06-08
- ADR: [ADR-0076](../adr/ADR-0076-stack-safe-language-term-validation.md)
- Branch: `adr-0073-sjas-correspondence-program`

## Outcome

Made `proflog.language/validate-term` stack-safe for deeply nested declared
terms by replacing recursive argument validation with an explicit worklist. The
validator still rejects proof-time `par` terms, malformed syntax, undeclared
function symbols, and arity mismatches with the same public error categories.

This closed the next ADR-0073 Track 1 host-stack failure after ADR-0075 fixed
core.logic occurs-check traversal.

## Evidence

The focused language regression failed red before the validator change:

```text
lein test :only proflog.language-test/validate-term-accepts-deep-declared-unary-terms
Ran 1 tests containing 1 assertions.
0 failures, 1 errors.
elapsed 0:10.43 maxrss 234704KB
```

After the worklist validator patch, the same selector passed:

```text
Ran 1 tests containing 1 assertions.
0 failures, 0 errors.
elapsed 0:09.17 maxrss 213336KB
```

The focused U-Grounding proof-certificate selector that exposed the validation
overflow then passed end to end:

```text
lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-u-grounding-formula-bearing-proof-certificate
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 2:18.62 maxrss 427616KB
```

The final broad gates passed:

```text
lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 2:22.51 maxrss 444196KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 5:49.71 maxrss 570300KB
```

## Follow-up

Formula validation remains recursive because the observed failure was deep term
syntax, not deep formula syntax. If ADR-0073 later introduces formula-depth
terms that overflow at the formula validator, that should receive a focused
regression and decision rather than being folded into this closed term fix.
