# ADR-0134: SJAS Xtab / LEM Full SelfCons Target

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0134-sjas-xtab-lem-full-target`
- AAR: [AAR-0134](../aar/AAR-0134-sjas-xtab-lem-full-target.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) requires each Workstream B
negative variant to pass through two witness stages before final evidence is
complete: a reduced reflected-beta witness and the full generated SelfCons
contradiction target. [ADR-0133](ADR-0133-sjas-xtab-lem-reduced-witness.md)
completed the reduced Xtab/LEM-as-axiom witness by reflecting a finite
universal LEM beta seed into the variant system.

The next step is target construction, not proof. We need a public report that
derives the exact generated Level-1 SelfCons statement from the ADR-0133 system
and names the refutation formula later certificates or synthesis probes must
close:

```text
AxiomConj(S_xtab_lem) /\ not(SelfCons(S_xtab_lem))
```

## Decision

Expose the full generated SelfCons contradiction target for the ADR-0133
Xtab/LEM reduced-witness system:

- `xtab-lem-full-target-report(opts)` returns an audit record for
  `xtab-lem-reduced-witness-system`;
- the report includes the system code, Group-3 theorem code, axiom formula,
  SelfCons formula, negated SelfCons formula, refutation target, and target
  formula code;
- certificate and synthesis statuses remain open, with durable probe logging
  required for future long-running synthesis attempts.

Update the Workstream B audit so `:xtab-or-lem-axiom` records both witness
stages as complete while still keeping final evidence incomplete:

- completed: `:reduced-reflected-beta-witness`;
- completed: `:full-generated-selfcons-contradiction-target`;
- open: `:constructed-certificate`;
- open: `:proof-search-synthesis`.

## Consequences

- Future Xtab/LEM proof ADRs can target a stable generated formula instead of
  rebuilding a private target shape.
- The target is tied to the generated Group-3/SelfCons code of the reflected
  Xtab/LEM beta system, so any future beta change regenerates the target.
- This ADR still does not claim a constructed contradiction certificate or
  proof-search synthesis evidence.

## Test Obligations

Red first:

- the boundary audit still lists the Xtab/LEM full generated target as open;
- the target-report test fails because no public Xtab/LEM full-target helper
  exists.

Green after implementation:

- the audit marks `:xtab-or-lem-axiom` as `:full-target-implemented`;
- completed witness stages for `:xtab-or-lem-axiom` contain both required
  stages;
- open obligations for `:xtab-or-lem-axiom` are exactly
  `:constructed-certificate` and `:proof-search-synthesis`;
- the report names the ADR-0133 reduced-witness system code and generated
  Group-3 theorem code;
- the negated SelfCons formula is the normalization-layer negation of the
  generated Group-3 formula;
- the refutation target is exactly
  `AxiomConj(S_xtab_lem) /\ not(SelfCons(S_xtab_lem))`;
- report evidence statuses remain open and require durable probes;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0134, the ADR index, and `LOG.md` identify this Workstream B target
  construction slice.
- Public helpers expose the full generated SelfCons contradiction target for
  the Xtab/LEM reduced-witness system.
- No constructed certificate or proof-search synthesis completion is claimed.
