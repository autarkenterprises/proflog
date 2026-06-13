# ADR-0104: D_SJAS Track 2c Correspondence Program

- Status: accepted
- Date: 2026-06-13
- Branch: `adr-0104-dsjas-track2c`
- AAR: pending

## Context

ADR-0103 completed Path A as a narrow literal-Willard theorem and completed
Path B negatively for literal Track 2b over the current accepted domain. The
remaining positive route is Track 2c: formalize the implemented extended SJAS
deductive apparatus as its own selected apparatus, `D_SJAS`, and prove
correspondence to that apparatus rather than to literal Willard `D`.

The user requested Track 2c to completion:

- define `D_SJAS`;
- repair `sjas-axiom` proof-object accounting;
- prove recursive proof/substitution well-foundedness;
- prove literature admissibility for the extended apparatus.

## Decision

Pursue a positive theorem for:

```text
CurrentProflogAccepts(P,S,F)
iff
SemPrf_D_SJAS(translate(P,S,F),S,F)
```

where `D_SJAS` is a mathematically specified selected apparatus, not
"whatever the implementation accepts."

The apparatus will be defined by explicit rule families:

- base semantic-tableau rules;
- branch bookkeeping and truth normalization;
- quantifier and bounded-quantifier rules;
- equality/disequality theory;
- arithmetic/profile closure;
- decoded axiom membership;
- reflected-call expansion from encoded `system-code`;
- recursive `tableau-proof/3`;
- recursive `subst-prf/4`;
- structural substitution and fixed-point support.

For `sjas-axiom` citation certificates, choose the combined proof-object repair:
the Track 2c proof-size measure is over the inspectable tuple `(S,F,P)`, not
over `P` alone. This preserves the existing public citation and synthesis
behavior while repairing the ADR-0102 counterexample: the cited formula and
system payload are no longer hidden outside the measured proof object.

Recursive `tableau-proof/3` and `subst-prf/4` closes must be justified by a
well-founded measure on decoded proof-code payloads. A recursive leaf may call
the proof checker only on proof bytes obtained from the object-level proof-code
argument at that leaf; the proof obligation is that nested calls descend into
the finite object proof tree or into a separately supplied finite proof-code
payload, and therefore cannot create an unbounded host-level oracle loop.

Literature admissibility must be argued honestly. The expected result is not
that `D_SJAS` is literally Willard's `D`, but that it is an explicitly selected
semantic-tableau-style apparatus with natural proof coding, bounded
object-language rule predicates, and the same size/inspectability discipline
needed by the SJAS self-reference argument. If a rule family cannot satisfy
that standard, the ADR remains incomplete.

## Consequences

- Existing public `sjas-axiom` certificates remain valid.
- The proof-size theorem changes from `size(P) >= 5J` to a Track 2c combined
  measure over `(S,F,P)` for citation leaves and over formula-bearing proof
  tree bytes for structural leaves.
- `D_SJAS` is a modified apparatus. Claims about it must be labeled
  `IS#_{D_SJAS}(beta)`, not literal `IS#_D(beta)`.
- Code changes must not weaken relational purity: Track 2c records semantics
  and proof obligations; it does not add host-oracle shortcuts to make the
  proof easier.

## Test Obligations

- Red tests must require an executable `D_SJAS` apparatus specification.
- Red tests must require the combined proof-object accounting repair for
  `sjas-axiom` citations.
- Red tests must require an explicit recursive well-foundedness audit for
  `tableau-proof/3` and `subst-prf/4`.
- Red tests must require a literature-admissibility status that distinguishes
  completed, incomplete, and impossible rule families.
- Existing Path A/Path B tests must remain green.

## Exit Criteria

- `D_SJAS` is defined as a stable mathematical/executable rule specification.
- The `sjas-axiom` citation counterexample is repaired under the selected
  combined-object measure.
- Recursive proof/substitution closes have a discharged well-foundedness proof
  or a precise blocking counterexample.
- Literature admissibility is proved for every selected rule family or the
  failed family is removed/excluded.
- Focused correspondence tests, `lein test-proflog-fast`, and
  `lein test-proflog-extended` pass before commits.
