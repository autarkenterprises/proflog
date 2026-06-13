# ADR-0103: SJAS Proof Attempts A and B

- Status: completed
- Date: 2026-06-13
- Branch: `adr-0103-sjas-proof-attempts-a-b`
- AAR: [AAR-0103](../aar/AAR-0103-sjas-proof-attempts-a-b.md)

## Context

ADR-0102 refuted ADR-0100 as stated and split the remaining work into two
corrected proof targets:

- Path A: a narrowed literal-Willard structural fragment.
- Path B: an extended selected `D_SJAS` apparatus.

The user clarified that this ADR is not complete merely because an inventory
exists. The proof obligations must be discharged or conclusively shown
impossible.

## Decision

Add executable proof-audit artifacts to `proflog.sjas-correspondence`.

For Path A, prove the narrowed theorem by direct examination over the admitted
structural checker branches. The executable proof audit records:

- every admitted branch;
- every excluded SJAS-extension branch;
- the six named lemma obligations;
- the proof clause discharging each obligation;
- the final no-open-obligations verdict.

For Path B, do not erase blockers by relabeling the current implementation as
literal Willard `D`. Instead, record the conclusive Track 2b verdict: the
current accepted domain cannot be a literal-Willard correspondence theorem
because it includes non-Willard extended rule families and the fixed-size
`sjas-axiom` citation counterexample. A full `D_SJAS` theorem is therefore a
Track 2c task with a different selected apparatus and repaired proof-object
accounting.

## Consequences

Path A is complete as a narrow theorem:

```text
For non-axiom formula-bearing structural proof trees whose checker path uses
only Path-A-admitted branches,

  ProflogAccepts_A(P,S,F) iff SemPrf_D(decode(P),S,F)

up to the recorded agenda, truth, NNF, quantifier, and bounded-guard
irrelevance lemmas.
```

This theorem intentionally excludes equality progression, arithmetic/profile
closure, axiom-membership closure, reflected calls, recursive `tableau-proof/3`,
`subst-prf/4`, and bare `sjas-axiom` citation certificates.

Path B is complete as a negative Track 2b result for the current accepted
domain. The extended `D_SJAS` apparatus remains viable only as a future Track 2c
theorem after choosing a proof-object accounting repair:

- replace bare citations with formula-bearing axiom leaves; or
- count a combined proof object carrying the needed theorem/system payload.

## Test Obligations

- Red tests must require the Path A proof-status API, not only the branch
  inventory.
- Red tests must require the Path B conclusive verdict API, not only open
  blockers.
- The tests must verify Path A has no open obligations and Path B is separated
  from literal Willard `D`.

## Exit Criteria

- Focused correspondence audit tests pass.
- Path A proof notes record the completed narrow theorem.
- Path B proof notes record the negative literal-Willard verdict and the Track
  2c handoff.
- Broad fast/extended regression gates remain green before commit.
