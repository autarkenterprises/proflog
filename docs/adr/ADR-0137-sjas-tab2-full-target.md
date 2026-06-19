# ADR-0137: SJAS Tab-2 Full Target

- Status: completed
- Date: 2026-06-19
- Branch: `adr-0137-sjas-tab2-full-target`
- AAR: [AAR-0137](../aar/AAR-0137-sjas-tab2-full-target.md)

## Context

[ADR-0136](ADR-0136-sjas-tab2-reduced-witness.md) completed the reduced witness
stage for the Tab-2-or-stronger Workstream B variant. The roadmap now needs the
second witness stage: a generated SelfCons contradiction target for the same
variant.

This ADR must not implement Tab-2 proof-list validation. The target can name
the dangerous proof relation and generate the self-consistency formula around
it, but constructed certificates and proof-search synthesis remain later work.

## Decision

Add a target-only Tab-2 boundary profile:

- generate a system profile named `:willard-sjas-tab2-boundary`;
- declare `dsjas-tab2-proof/3` only for that target system;
- generate Group-3/SelfCons with `dsjas-tab2-proof`, not
  `dsjas-tab1-proof`;
- omit the Tab-1 `pi-star-1-code` intermediate guard from the Tab-2 boundary
  SelfCons sentence;
- expose `tab2-or-stronger-full-target-system` and
  `tab2-or-stronger-full-target-report`;
- keep `tab2-proof` checking, constructed certificates, and synthesis evidence
  open.

Update the Workstream B roadmap so `:tab-2-or-stronger` has both reduced and
full target witness stages complete.

## Consequences

- Tab-2-or-stronger now has the same two-stage target surface as total
  multiplication and Xtab/LEM.
- The generated target is explicit and code-bearing, but it is not an
  executable proof-profile implementation.
- Future ADRs can add certificate validation and synthesis evidence against a
  concrete target instead of inventing target shape during proof work.

## Test Obligations

Red first:

- no `tab2-or-stronger-full-target-system` helper exists;
- no `tab2-or-stronger-full-target-report` helper exists;
- the Workstream B roadmap still marks Tab-2 as reduced-only.

Green after implementation:

- the target system uses profile `:willard-sjas-tab2-boundary`;
- the generated Group-3 formula names `dsjas-tab2-proof` and `neg-pair`;
- the generated Group-3 formula does not name `dsjas-tab1-proof` or
  `pi-star-1-code`;
- the full-target report names the generated Group-3 code, negated SelfCons,
  refutation target, and target code;
- roadmap open obligations for Tab-2 keep only constructed certificate and
  proof-search synthesis;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0137, the ADR index, and `LOG.md` identify this Workstream B full-target
  slice.
- Tab-2-or-stronger has a generated SelfCons contradiction target.
- No Tab-2 checker, constructed certificate, or proof-search synthesis
  completion is claimed.
