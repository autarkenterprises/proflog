# AAR-0096: SJAS Correspondence Fragment Audit

- Date: 2026-06-13
- Related ADR: [ADR-0096](../adr/ADR-0096-sjas-correspondence-fragment-audit.md)
- Outcome: completed

## What Happened

Added a fragment-boundary audit layer to `proflog.sjas-correspondence`. The
existing Track 2a symbol classification remains intact, but ADR-0096 now
separates that classification from first Track 2b fragment admission.

The first fragment admits only formula-bearing structural tableau proof terms
with no proof-symbol tags and the bare `sjas-axiom` citation certificate. Legacy
kernel proof-rule traces, generic sidecars, answer-overlay evidence, and other
encoded proof symbols remain outside this first fragment unless a later Track
2b slice proves how to admit them.

## What Worked

The red test failed before implementation on the missing
`proof-symbol-fragment-boundaries` var. After implementation, the new focused
selectors and the full `proflog.sjas-correspondence-test` namespace passed.
The broad gates also passed: `lein test-proflog-fast` with 187 tests and 992
assertions, and `lein test-proflog-extended` with 73 tests and 219 assertions.

The change stayed outside proof search. No kernel, proof-checker, proof-code
encoder, public query, or answer behavior changed.

## What Did Not Work

This is not a correspondence proof. It is a guardrail for one prerequisite of
that proof: an explicit, executable boundary between encoded proof evidence and
admitted proof-code fragments.

The first fragment is conservative. It intentionally leaves most encoded proof
symbols outside the admitted fragment even when their Track 2a status is
`:relevant`.

## Follow-Up

- Use this audit when opening the first formal Track 2b correspondence proof
  slice.
- For each future admitted symbol family, add a separate primitive,
  macro-expansion, wrapper-erasure, or unreachability proof and update the
  fragment boundary with red/green tests.
- Keep ADR-0095's proof-synthesis work separate; this ADR makes no claim about
  proof-code synthesis or compact-code generation.
