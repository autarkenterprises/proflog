# AAR-0087: SJAS Level-1 Pi-star-1 Pair Restriction And Basis Classification

- Date: 2026-06-09
- ADR: [ADR-0087](../adr/ADR-0087-sjas-level1-pi-star-1-pair-restriction.md)
- Branch: `adr-0073-sjas-correspondence-program`

## Outcome

The Level-1 Group-3 sentence now matches Willard 2013 sentence (7): its
matrix opens with a `pi-star-1-code(x)` restriction ahead of the
`neg-pair(x,y)` complement pairing, in both the builder
(`selfcons1-formula`) and the profile reconstruction template
(`level1-selfcons-internal-formula`, shared by the proof-bearing and
proof-free Group-3 relations). Willard's single `Delta*0` `Pair(x,y)` is
encoded as the conjunction of the two reserved vocabulary atoms; the
skeleton/diagonal fixed-point mechanism is unchanged apart from carrying the
extra literal.

`Delta-star-0` classification is now closed under `not` and `implies` in
both the host classifier and the relational `sjas-delta-star-0-formulao`,
matching the formula-code grammar tags. `system` validates at build time
that every beta member and reflected Group-2b clause formula is
`Pi*1`-encodable (`delta-star-0?` or `pi-star-1?`), rejecting violations
with a diagnostic `ex-info`; external clauses remain unconstrained.

The audit's naming clarification is recorded: `:willard-sjas-tableau0` is
the finite-basis IS(A)-style instance (Willard 2001, Group-3 target
`0 = 1`); `IS#_D(beta)` proper, per Definition 5.1, is the
`:willard-sjas-level1` line corrected here.

## Evidence

Red before implementation (one run, all four new selectors):

```text
lein test :only proflog.willard-sjas-test/sjas-formula-classifiers-close-delta-star-0-under-connectives
                proflog.willard-sjas-test/sjas-level1-group-three-restricts-pair-to-pi-star-1
                proflog.willard-sjas-test/sjas-system-rejects-non-pi-star-1-reflected-basis
                proflog.willard-sjas-test/sjas-syntax-class-predicates-accept-implies-codes
Ran 4 tests containing 11 assertions.
8 failures, 0 errors.
elapsed 0:40.96 maxrss 267276KB
```

Green after implementation:

```text
Ran 4 tests containing 11 assertions.
0 failures, 0 errors.
elapsed 2:01.07 maxrss 332512KB
```

Affected Level-1 and tableau0 regressions:

```text
lein test :only proflog.willard-sjas-test/sjas-level1-group-three-uses-substitution-proof-vocabulary
                proflog.willard-sjas-test/sjas-level1-group-three-uses-selfcons-skeleton-code
                proflog.willard-sjas-test/sjas-tableau-proof-cites-level1-group-three-from-system-code
                proflog.willard-sjas-test/sjas-tableau0-selfcons-godel-code-is-publicly-printable
Ran 4 tests containing 21 assertions.
0 failures, 0 errors.
elapsed 0:55.02 maxrss 347916KB
```

Broad gates after implementation:

```text
lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 5:51.24 maxrss 411024KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 14:26.15 maxrss 651064KB
```

The refreshed SelfCons Godel code for the default ordinary-tableau instance
(`lein print-sjas-selfcons-godel-code`, post ADR-0086/0087 coding path):

```text
1895911909320248794237471524907560082878513227
```

The `^:slow` fixed-point certificate selectors and a supplementary Level-1
coverage batch (seven selectors including the 128-assertion profile source
audit and the bounded contradiction probe) were started as durable detached
runs per AGENTS.md practice 17:

```text
test-runs/adr0087-subst-prf-fixed-point-20260610T003800Z.log
test-runs/adr0087-level1-supplement-20260610T004505Z.log
```

Probe outcome (2026-06-10 update). Both probes were stopped for differential
attribution after each exceeded two CPU-hours on a single test var:

- the fixed-point run had passed both positive `subst-prf` checks (stack
  evidence: execution inside the third, negative query) and was stopped at
  `137:53` CPU during that negative exhaustive search;
- the supplementary batch had completed
  `sjas-u-grounding-subst-code-computes-level1-fixed-point`,
  `sjas-level1-bounded-contradiction-probe-records-timing`, and
  `sjas-subst-code-relates-structural-substitution-codes` without failure
  output — including the direct Level-1 fixed-point computation over the
  revised skeleton — and was stopped at `128:59` CPU inside
  `sjas-tableau0-and-level1-query-generated-axioms-through-selected-profile`.

Differential runs of both grinding vars at commit `1fa3e53` (the state
immediately before this ADR) timed out at `40:00` and `45:00` respectively.
The runtime regression therefore predates ADR-0087 and entered with the
ADR-0086-era state; it had gone unnoticed because these vars belong to the
SJAS namespace, which the fast/extended gates do not cover and which was not
re-run in full after ADR-0086. The semantic correction of this ADR stands;
runtime re-baselining and scheduling work is proposed as
[ADR-0088](../adr/ADR-0088-sjas-whole-program-query-runtime.md).

The profile source audit was re-run on this ADR's code after the probes were
stopped:

```text
sjas-profile-source-audit-rejects-host-proof-checker-route
Ran 1 tests containing 128 assertions.
0 failures, 0 errors.
elapsed 1:29.21 maxrss 243580KB
```

## Follow-up

- The Track 2a relevance matrix opens with the apparatus-extension entries
  named by the
  [motivation/correctness audit](../log/2026-06-09-motivation-alignment-and-correctness-audit.md):
  closure through arithmeticized profile relations versus Willard 2005's
  complementary-pair-only closure, the Group-1 truth-role absorption, and a
  regression for the >= 5J-bit proof-size discipline.
- The `lopstr-ppdp26/` and `mk2026/` artifact snapshots predate ADR-0086 and
  ADR-0087 and must be refreshed before submission.
- Truth of beta in the standard model remains Willard's external
  consistency-preservation premise; the builder enforces only the formula
  class.
