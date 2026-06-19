# ADR-0139: SJAS Boundary Synthesis Probe

- Status: completed
- Date: 2026-06-19
- Branch: `adr-0139-sjas-boundary-synthesis-probe`
- AAR: [AAR-0139](../aar/AAR-0139-sjas-boundary-synthesis-probe.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) requires every Workstream B
negative variant to finish with both constructed certificates and proof-search
synthesis evidence. ADR-0124 through ADR-0138 added the variant surfaces,
reduced witnesses, full generated SelfCons targets, cheap evidence screen,
generic constructed-certificate verifier, and target-specific validation
bridges.

The remaining gap is evidence production. The current public proof search can
synthesize an ordinary `sjas-axiom` proof code for a generated Group-3
SelfCons theorem. ADR-0127 intentionally rejects that proof shape as ordinary
SelfCons citation evidence. The next step must make this synthesis path
executable and auditable, while still refusing to count ordinary Group-3
citation as completion.

## Decision

Add a bounded Workstream B proof-search synthesis probe:

- expose every ADR-0119 Workstream B variant as a selectable synthesis target;
- build the generated target report and the validation program for that
  variant;
- run `tableau-proof/3` with a fresh proof-code variable against the generated
  Group-3 theorem code;
- turn the first synthesized proof code, when present, into a
  `:proof-search-synthesis` evidence candidate;
- accept an explicitly supplied `:synthesized-proof-code` from a durable probe
  run so candidate screening can be reproduced without rerunning an expensive
  search;
- require the caller to provide a durable synthesis log path before such a
  candidate may pass the cheap screen;
- classify the current ordinary synthesized `sjas-axiom` citation as rejected
  intermediate evidence, not final boundary-failure evidence;
- keep `:constructed-certificate` and `:proof-search-synthesis` open for every
  variant unless a later ADR supplies nontrivial screened and verified
  evidence.

The probe is intentionally an evidence-production and evidence-classification
surface. It does not add a Tab-2 checker, does not generate nontrivial
certificates by construction, and does not claim final Workstream B completion.

## Consequences

- Future certificate work has a common entrypoint for durable proof-search
  probes across total multiplication, Xtab/LEM, and Tab-2-or-stronger.
- Ordinary generated SelfCons citation is recorded as a real synthesized proof
  shape and a real rejection reason, which prevents accidental completion
  claims.
- Long-running synthesis runs can use the same helper while writing stdout,
  stderr, and timing output under `test-runs/`.

## Test Obligations

Red first:

- no public boundary synthesis probe namespace exists;
- no Workstream B roadmap metadata advertises a proof-search synthesis probe;
- no report can synthesize and screen proof-code candidates for a generated
  boundary target.

Green after implementation:

- all three Workstream B variants are selectable synthesis targets;
- the total-multiplication target can classify a durable synthesis result for
  the current ordinary `sjas-axiom` proof code;
- the synthesized candidate carries `:proof-search-synthesis` evidence kind,
  the generated system code, SelfCons code, target code, proof code, and
  durable log path;
- the candidate is screened through `screen-boundary-evidence` and rejected as
  `:ordinary-selfcons-citation`;
- the report records zero completed obligations and keeps both final evidence
  obligations open;
- roadmap metadata points future Workstream B proof-search evidence to the new
  probe helper;
- focused selectors for the new probe tests pass;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0139, the ADR index, and `LOG.md` identify this synthesis-probe slice.
- Public probe helpers can synthesize and classify candidate proof-code
  evidence for Workstream B generated targets.
- No constructed-certificate, proof-search synthesis, or Workstream B
  completion claim is made from ordinary SelfCons citation.
