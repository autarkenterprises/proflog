# ADR-0131: SJAS Boundary Constructed-Certificate Verifier

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0131-sjas-boundary-certificate-verifier`
- AAR: [AAR-0131](../aar/AAR-0131-sjas-boundary-certificate-verifier.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) requires Workstream B
boundary failures to finish with both explicit constructed certificates and
proof-search synthesis evidence. [ADR-0126](ADR-0126-sjas-total-mul-full-target.md)
exposed the total-multiplication generated SelfCons refutation target, and
[ADR-0127](ADR-0127-sjas-boundary-evidence-screen.md) rejects obvious false
evidence such as ordinary SelfCons citations and candidates for the wrong
system code.

The next implementation step should not be the final proof-search synthesis
loop. Before a constructed certificate can count even as intermediate evidence,
there must be a public verification contract that combines the ADR-0127 screen
with a proof-check result against the exact ADR-0126 target. Otherwise a future
caller could mark `:constructed-certificate` complete merely by attaching
plausible metadata.

## Decision

Add an executable constructed-certificate verifier for Workstream B evidence:

- run `screen-boundary-evidence` before any proof result is considered;
- require the candidate to be a `:constructed-certificate` evidence record;
- require the proof-validation result to name the same variant, system code,
  generated SelfCons code, target formula, and proof code as the candidate and
  target;
- reject failed or mismatched proof-validation results with explicit reasons;
- return `:verified-intermediate-evidence` only for a candidate whose metadata
  passes the screen and whose proof-validation result is successful;
- keep proof-search synthesis as the remaining final obligation after a
  constructed certificate is verified.

This ADR implements the verification contract, not the actual
total-multiplication contradiction certificate. The Workstream B roadmap must
still report the total-multiplication constructed-certificate obligation as
open until a later ADR provides a concrete verified certificate.

## Consequences

- Constructed-certificate intake becomes two-stage: cheap metadata screen,
  then target-specific proof validation.
- Screened-in metadata is no longer enough to count as evidence.
- Future ADRs can plug a real proof-checker result into the verifier without
  changing the Workstream B audit contract.

## Test Obligations

Red first:

- the Workstream B audit has no constructed-certificate verifier metadata;
- no public `verify-boundary-constructed-certificate` helper exists.

Green after implementation:

- the audit records a total-multiplication constructed-certificate verifier;
- screened-out candidates remain rejected at the screen stage;
- mismatched target formula, proof code, or failed proof-validation results are
  rejected with explicit reasons;
- a matching successful proof-validation result returns
  `:verified-intermediate-evidence`, completes only
  `:constructed-certificate`, and keeps `:proof-search-synthesis` open;
- the roadmap still keeps total multiplication's constructed-certificate and
  proof-search obligations open because no concrete certificate is supplied by
  this ADR;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0131, the ADR index, and `LOG.md` identify this Workstream B verifier
  slice.
- Public audit helpers expose the constructed-certificate verifier contract.
- No concrete constructed certificate or synthesis evidence is claimed.
