# ADR-0076: Stack-Safe Language Term Validation

- Status: completed
- Date: 2026-06-08
- Branch: `adr-0073-sjas-correspondence-program`
- AAR: [AAR-0076](../aar/AAR-0076-stack-safe-language-term-validation.md)

## Context

ADR-0073 Track 1 now carries SJAS proof certificates as large U-Grounding
numerals. ADR-0075 removed the first host-stack failure in core.logic's occurs
check, but the focused U-Grounding proof-path selector then exposed the next
host recursion: `proflog.language/validate-term`.

The validator recursively descends through every argument of an application
term. That is clear for ordinary hand-written terms, but large arithmeticized
proof certificates can be tens of thousands of unary constructor layers deep.
Those terms are still finite, declared, acyclic terms and should not be rejected
because the host JVM stack is too small.

## Decision

Make `validate-term` iterative. The public behavior remains unchanged:

- variables validate immediately;
- proof-time `par` terms are still rejected at any depth;
- application symbols must be declared in the function signature;
- application arity must match the declared function arity;
- malformed terms, undeclared symbols, and arity mismatches still raise the same
  category of `ExceptionInfo`.

The implementation should use an explicit worklist of terms to validate instead
of recursive calls for application arguments. Formula validation can continue to
use its existing recursion because the current failure is in deeply nested term
syntax, not deeply nested formula connectives.

## Consequences

- Large U-Grounding proof and formula numerals can pass the public language
  boundary before proof search begins.
- Ordinary language validation semantics stay the same for existing tests.
- If future proof work exposes deeply nested formulas rather than terms, that
  should receive its own focused ADR and regression.

## Test Obligations

- Add a red regression validating a deeply nested declared unary term.
- Rerun the focused language regression after implementation.
- Rerun the U-Grounding proof-certificate selector that exposed the validator
  stack overflow.
- Rerun `lein test-proflog-fast` and `lein test-proflog-extended` after the
  validator and core.logic patches are both in place.
