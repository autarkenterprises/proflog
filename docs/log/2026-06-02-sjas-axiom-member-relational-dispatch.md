# SJAS Axiom-Member Relational Dispatch

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`
ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Context

Track 1 requires proof-facing SJAS predicates to be ordinary object-level
relations over encoded systems, formulas, substitutions, proof trees, and
branch states. `tableau-proof/3` and `subst-prf/4` already consume
`axiom-member` evidence reconstructed from `system-code`, but the final
`sjas-axiom-membero` group dispatcher still used `conda` to select among beta,
reflected, fixed, Tableau-0 Group-3, and Level-1 Group-3 axiom classes.

This was not a host registry lookup, and the branches remain finite structural
relations. It was nevertheless proof-facing search control at the axiom-class
boundary. For the in-principle arithmeticized predicate, axiom membership
should be an ordinary finite relation: a candidate axiom code succeeds because
one of the encoded-system membership relations holds, not because committed
choice selected a class procedurally.

## Change

`sjas-axiom-membero` now uses ordinary `conde` disjunction across the finite
axiom classes. The source audit rejects a reintroduced committed-choice
dispatcher in this proof-facing relation.

This deliberately does not remove parser-local `conda` sites. Some of those
are disjoint byte-tag grammar discriminators, and some reflected-call
alternative collectors use committed choice as an if-then-else to avoid
omitting matching clauses. Those sites require separate treatment: either an
explicit object-level nonmatch relation or a proof that the committed choice is
only an implementation of a deterministic byte grammar and does not affect
proof evidence.

## Red Evidence

The source audit was extended first and failed on the old dispatcher:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route

FAIL axiom-member group selection must be an ordinary finite relation, not committed-choice search control
actual matched: defn- sjas-axiom-membero ... (conda
```

## Verification

Completed current-source checks:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 70 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-system-does-not-generate-axiom-member-fact-registry
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.
```

Additional focused axiom/proof-predicate selectors were still running when
this note was recorded:

- `sjas-beta-axiom-member-decodes-application-codes-without-symbol-registry`
- `sjas-reflected-axiom-member-decodes-application-codes-without-symbol-registry`
- `sjas-proof-predicates-do-not-require-source-preprocessing-registry`

Long subst-prf probes launched before this slice remain separate durable
evidence under `test-runs/`.
