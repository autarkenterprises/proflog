# ADR-0124: SJAS Boundary Variant Surface

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0124-sjas-boundary-variant-surface`
- AAR: [AAR-0124](../aar/AAR-0124-sjas-boundary-variant-surface.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) defines Workstream B as the
negative SJAS path: implement variants expected to cross Willard's Goedel
boundary, then demonstrate failure against the variant's own generated
SelfCons target. The roadmap names three variant families: total
multiplication, Tab-2-or-stronger deduction, and Xtab/LEM-as-axiom.

The existing implementation intentionally keeps multiplication relational:
`mult/3` is an object-language graph relation, and no `mul/2` function appears
in the baseline SJAS language. The earlier diagonalization notes identify this
as the critical Willard boundary: total multiplication is dangerous because it
permits short squaring-chain proof fragments, not because adding any arbitrary
inconsistent beta axiom is interesting.

The first Workstream B implementation slice should therefore add a narrow,
inspectable negative-variant surface without pretending that it already
synthesizes the destructive diagonal witness. This avoids turning the known
trivial inconsistent-beta fixture into a false Workstream B completion.

## Decision

Add a total-multiplication boundary surface:

- `mul-term`, constructing the object-language term `mul(left,right)`;
- `total-multiplication-functions`, declaring `mul/2`;
- `total-multiplication-seed-axioms`, a small finite reflected beta fragment
  that uses `mul/2` and remains Pi*1-admissible;
- `total-multiplication-boundary-options`, returning the mergeable
  functions/beta fragment;
- `total-multiplication-boundary-system`, building an SJAS system with the
  official `mul/2` function and seed reflected beta axioms.

The seed axioms are not the final negative witness. They exist to make the
variant's language and reflected source identity concrete, citeable, and
testable. The Workstream B audit must keep the full witness obligations open:

- reduced reflected-beta contradiction witness;
- full generated SelfCons contradiction target for the variant;
- explicit constructed certificate;
- proof-search synthesis evidence.

Constructed certificates alone do not satisfy this ADR's completion standard,
and this ADR must not mark the total-multiplication variant as fully negative
until proof search also synthesizes the witness.

## Consequences

- The baseline SJAS language remains unchanged: it still excludes `mul/2`.
- The first negative variant has a public system-builder surface and reflected
  beta records whose identity changes are covered by tests.
- Future ADRs can replace the seed fragment with a reduced diagonal witness
  without changing callers that select the total-multiplication variant.
- Workstream B remains open after this slice unless a later ADR supplies both
  constructed and synthesized witnesses for a variant's own SelfCons target.

## Test Obligations

Red first:

- boundary audit tests fail because no Workstream B audit record exists;
- total-multiplication surface tests fail because `mul-term` and variant
  builders do not exist.

Green after implementation:

- the baseline SJAS language still has no `mul/2` function;
- the total-multiplication boundary system declares `mul/2`;
- seed reflected beta axioms are Pi*1-admissible and become Group-2 records;
- those records are visible through `axiom-member/2` and citeable by
  `tableau-proof/3` with `sjas-axiom`;
- adding the seed reflected beta fragment changes encoded system identity and
  regenerated Group-3/SelfCons code compared with the same function signature
  and no seed beta;
- the audit records that reduced/full SelfCons witnesses and synthesis evidence
  are still open obligations;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0124, the ADR index, and `LOG.md` identify this as a Workstream B slice.
- The total-multiplication boundary surface is public and tested.
- No final Workstream B contradiction witness is claimed.
