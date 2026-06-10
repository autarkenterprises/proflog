# ADR-0088: SJAS Whole-Program Query Runtime Re-Baseline

- Status: proposed
- Date: 2026-06-10
- Branch: `adr-0073-sjas-correspondence-program` (or a dedicated successor)
- AAR: pending

## Context

The 2026-06-09/10 audit found that the opaque `lein test-proflog-sjas` gate
has not been runtime-green since the ADR-0086 `0 = 1` SelfCons target landed.
Differential evidence, recorded in
[AAR-0087](../aar/AAR-0087-sjas-level1-pi-star-1-pair-restriction.md) and the
[audit note](../log/2026-06-09-motivation-alignment-and-correctness-audit.md):

- `sjas-tableau0-and-level1-query-generated-axioms-through-selected-profile`
  (not `^:slow`) exceeds a 40-minute timeout at commit `1fa3e53`
  (pre-ADR-0087) and ran past two CPU-hours at `e18f7b7` before being
  stopped.
- `sjas-subst-prf-checks-selfcons-fixed-point-certificate` exceeds a
  45-minute timeout at `1fa3e53`; at `e18f7b7` its two positive fixed-point
  checks passed and the run was stopped during the third, negative query.
- The direct Level-1 fixed-point computation, the bounded contradiction
  probe, the structural substitution-code selector, and the 128-assertion
  profile source audit all pass at `e18f7b7`.

The cost concentrates in two places. First, whole-program queries: every
query through a generated SJAS program decomposes the full `AxiomConj`,
including gamma-instantiating the enlarged Group-3 sentences, whose
embedded codes grew with the `0 = 1` target (and marginally again with the
ADR-0087 `pi-star-1-code` restriction). Saved profile-relation literals
produced by those instantiations (for example negated `tableau-proof`,
`subst-prf`, and formula-class atoms over enumerated terms) expose closure
attempts whose failure paths decode arbitrary terms as codes. Second,
negative exhaustive searches over the enlarged codes.

Both broad gates remain green; the regression is confined to the SJAS
namespace, which the gates do not cover.

## Decision (proposed)

1. Re-baseline the SJAS namespace var by var through
   `lein test-proflog-sjas-focused` with durable `test-runs/` logs, and
   record an expected-duration envelope (or an explicit
   exceeds-envelope marker) for every var in `TEST_RUNTIME_BASELINE.md`.
2. Investigate the scheduling of profile-atom closure under gamma
   instantiation in whole-program queries, and the failure-path cost of
   code-decode attempts on enumerated terms. Any change must be
   semantics-preserving with respect to the arithmeticized proof predicate
   (AAR-0086 discipline: performance work is subordinate to the predicate),
   and any change to closure behavior must be coordinated with the ADR-0073
   Track 2a relevance matrix, since closure discipline is exactly the
   apparatus-extension question that matrix must classify.
3. Partition vars that remain legitimately expensive after investigation
   into explicitly slow-marked selectors with documented envelopes, so the
   opaque gate is either runtime-green or visibly partitioned.

## Test Obligations

- Runtime envelopes recorded per var with durable logs.
- No semantic regressions: the ADR-0087 selectors, the affected Level-1 and
  tableau0 regressions, the profile source audit, and both broad gates must
  stay green through any scheduling change.

## Exit Criteria

- `lein test-proflog-sjas` is runtime-green, or its expensive vars are
  explicitly partitioned with recorded envelopes and rationale.
- The whole-program query cost mechanism is identified and either improved
  or documented as inherent to the literature-faithful encoding.
