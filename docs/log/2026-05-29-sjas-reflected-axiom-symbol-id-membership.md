# SJAS Reflected Axiom Symbol-ID Membership

Date: 2026-05-29
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This Track 1 system-code reconstruction slice removes the finite source symbol
table from reflected Group-2b axiom membership.

Reflected procedure-call recovery still needs relation identity in the ordinary
Proflog AST, so that path continues to use the proof-facing decoder. Reflected
axiom membership is narrower: it reconstructs the axiom formula induced by a
reflected clause record and compares it with the queried formula code. That
comparison can be done over decoded formula structure with numeric symbol ids.

The new reflected axiom decoder reads reflected records as:

```text
[reflected-clause-tag, relation-index, arity+1, body-formula-bytes...]
```

and reconstructs the formula using `(sym relation-index)` as the head of the
reflected relation. The queried formula code is decoded by the syntax-only
decoder, so both sides compare without `:sjas/symbol-index-entries`.

## Red/Green Evidence

The focused regression
`sjas-reflected-axiom-member-decodes-application-codes-without-symbol-registry`
removes `:sjas/registry` and asks `axiom-member` to cite the generated
Group-2b axiom for the reflected `demo/1` clause.

Before the change, the selector failed because both the queried formula decode
and reflected-clause reconstruction tried to resolve relation indexes through
the missing source registry:

```text
FAIL in (sjas-reflected-axiom-member-decodes-application-codes-without-symbol-registry)
expected: (successful? proofs)
  actual: (not (successful? ()))

Ran 1 tests containing 3 assertions.
1 failures, 0 errors.
```

After the change, the same selector passes.

## Remaining Boundary

This still does not complete proof-facing symbol-table internalization.
Procedure-call proof reconstruction and proof-antecedent AST conversion still
need host relation/function identities. The next Track 1 options are:

- encode enough signature data in `system-code` to recover those identities
  structurally; or
- push more of the proof checker onto internal numeric symbol ids so host AST
  projection is unnecessary for reflected procedure calls and arithmetic
  relation closure.

## Verification

```text
timeout -k 5s 150s lein test :only proflog.willard-sjas-test/sjas-reflected-axiom-member-decodes-application-codes-without-symbol-registry
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-beta-axiom-member-decodes-application-codes-without-symbol-registry
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 140s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-axiom-citation-certificates
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-do-not-require-source-preprocessing-registry
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 280s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-from-system-code
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.
```

The broader focused selector
`sjas-composite-examples-distinguish-beta-axioms-from-reflected-procedures`
was stopped by the `180 s` timeout during this slice. That var was already a
known expensive SJAS selector; this run is recorded as a runtime boundary, not
as green evidence.
