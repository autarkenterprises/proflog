# ADR-0133: SJAS Xtab / LEM Reduced Witness

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0133-sjas-xtab-lem-reduced-witness`
- AAR: [AAR-0133](../aar/AAR-0133-sjas-xtab-lem-reduced-witness.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) requires each Workstream B
negative variant to proceed through a reduced reflected-beta witness before a
full generated SelfCons contradiction target. [ADR-0130](ADR-0130-sjas-xtab-lem-boundary-surface.md)
added the Xtab / Law of Excluded Middle boundary surface but deliberately did
not add an axiom schema, a reduced witness, or a generated contradiction
target.

The implemented SJAS baseline already supports excluded-middle behavior through
semantic-tableau reasoning. The Workstream B variant is different: it packages
excluded middle as reflected beta material. This ADR adds only the smallest
finite witness of that packaging move, so later ADRs can derive the generated
target from a concrete variant system.

## Decision

Implement the Xtab/LEM reduced witness as a one-predicate universal beta seed:

```text
forall x. xtab_lem_demo(x) or not xtab_lem_demo(x)
```

The implementation will add:

- `xtab-lem-relations`, declaring the witness predicate;
- `xtab-lem-witness-axioms`, returning the finite reflected LEM beta seed;
- `xtab-lem-reduced-witness-options`, returning mergeable system options;
- `xtab-lem-reduced-witness-system`, building the Level-1-compatible variant
  system;
- Workstream B audit updates marking `:reduced-reflected-beta-witness` complete
  for `:xtab-or-lem-axiom` while keeping the full generated target,
  constructed certificate, and proof-search synthesis obligations open.

This is a reduced witness only. It is not a complete Xtab proof checker, not a
full LEM schema over all formulas, and not a SelfCons contradiction witness.

## Consequences

- The Xtab/LEM variant moves from a surface record to an executable reflected
  beta witness system.
- The reflected LEM seed must be Pi*1-admissible, included in Group 2, decoded
  through `axiom-member/2`, and citeable through `tableau-proof/3` using the
  existing `sjas-axiom` certificate.
- The full generated SelfCons target remains a separate Workstream B ADR.

## Test Obligations

Red first:

- the Workstream B audit still reports the Xtab/LEM reduced witness as open;
- no public Xtab/LEM reduced-witness helper functions exist.

Green after implementation:

- the witness relation map is `{'xtab-lem-demo 1}`;
- the witness axiom is the universal LEM seed over that predicate;
- the axiom is Pi*1-admissible;
- adding the reflected beta seed changes encoded system identity and
  regenerated Group-3/SelfCons code compared with a system that has only the
  witness relation declaration;
- the witness beta record is visible through `axiom-member/2` and citeable by
  `tableau-proof/3` with `sjas-axiom`;
- the Workstream B audit marks `:xtab-or-lem-axiom` as
  `:reduced-witness-implemented`, records the reduced witness metadata, and
  leaves the full generated target plus final evidence open;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0133, the ADR index, and `LOG.md` identify this Workstream B slice.
- Public helpers build the reduced reflected-beta Xtab/LEM witness.
- No full generated SelfCons target, constructed certificate, or synthesis
  evidence is claimed.
