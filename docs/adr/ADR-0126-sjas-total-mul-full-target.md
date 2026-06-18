# ADR-0126: SJAS Total Multiplication Full SelfCons Target

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0126-sjas-total-mul-full-target`
- AAR: [AAR-0126](../aar/AAR-0126-sjas-total-mul-full-target.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) requires every Workstream B
negative variant to pass through two witness stages before final evidence is
even considered: a reduced reflected-beta witness and the full generated
SelfCons contradiction target. [ADR-0125](ADR-0125-sjas-total-mul-reduced-witness.md)
completed the reduced total-multiplication stage by reflecting a finite
squaring-chain beta fragment into the variant system.

The next step is not yet the destructive proof. We first need a public,
executable target that names the exact generated Level-1 SelfCons statement for
the total-multiplication reduced-witness system and the refutation formula a
later contradiction certificate must close:

```text
AxiomConj(S_total-mul) /\ not(SelfCons(S_total-mul))
```

Keeping this as a separate ADR prevents later proof-search work from relying on
test-private reconstruction helpers or an informal description of the target.

## Decision

Expose the full generated SelfCons contradiction target for the ADR-0125
total-multiplication system:

- `selfcons-negation-target(system)`, returning `not(SelfCons_S)` from the
  system's generated Group-3 formula;
- `selfcons-refutation-target(system)`, returning
  `AxiomConj(S) /\ not(SelfCons_S)`;
- `total-multiplication-full-target-report(opts)`, returning an audit record
  for the total-multiplication reduced-witness system, including the system
  code, Group-3 theorem code, SelfCons formula, negated SelfCons formula,
  refutation target, and remaining evidence statuses.

The report is a target construction artifact, not a proof artifact. Its
certificate and synthesis statuses must remain open. Long-running future
synthesis attempts against this target must use durable `test-runs/` logs and
PID files under the existing SJAS probe discipline.

Update the Workstream B audit so total multiplication records both witness
stages as complete while still keeping final evidence incomplete:

- completed: `:reduced-reflected-beta-witness`;
- completed: `:full-generated-selfcons-contradiction-target`;
- open: `:constructed-certificate`;
- open: `:proof-search-synthesis`.

## Consequences

- Future Workstream B proof ADRs can cite a stable helper for the exact formula
  they are trying to refute.
- The target is tied to the generated Group-3/SelfCons code of the reduced
  total-multiplication system, so any reflected beta change changes the target.
- The total-multiplication variant is still not a completed Goedel-boundary
  failure. Completion still requires both an explicit constructed certificate
  and proof-search synthesis evidence against this same target.

## Test Obligations

Red first:

- the boundary audit still lists the full generated SelfCons target as open;
- the target-report test fails because no public target helpers exist.

Green after implementation:

- the audit marks total multiplication as `:full-target-implemented`;
- completed witness stages for total multiplication contain both required
  stages;
- open obligations for total multiplication are exactly
  `:constructed-certificate` and `:proof-search-synthesis`;
- the report names the ADR-0125 reduced-witness system code and generated
  Group-3 theorem code;
- the negated SelfCons formula is the normalization-layer negation of the
  generated Group-3 formula;
- the refutation target is exactly
  `AxiomConj(S_total-mul) /\ not(SelfCons(S_total-mul))`;
- report evidence statuses remain open and require durable probes;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0126, the ADR index, and `LOG.md` identify this Workstream B target
  construction slice.
- Public helpers expose the full generated SelfCons contradiction target for
  the total-multiplication reduced-witness system.
- No constructed certificate or proof-search synthesis completion is claimed.
