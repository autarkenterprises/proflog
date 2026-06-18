# ADR-0127: SJAS Boundary Evidence Screen

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0127-sjas-boundary-evidence-screen`
- AAR: [AAR-0127](../aar/AAR-0127-sjas-boundary-evidence-screen.md)

## Context

[ADR-0126](ADR-0126-sjas-total-mul-full-target.md) exposes the generated
SelfCons refutation target for the total-multiplication reduced-witness system.
The next open Workstream B obligations are a constructed certificate and
proof-search synthesis evidence.

There is an easy false positive that future work must reject. The existing SJAS
machinery can prove a system's generated Group-3 SelfCons theorem by citing the
Group-3 axiom or by supplying the ordinary structural SelfCons tableau. That is
the positive self-justification path, not the total-multiplication boundary
failure. Workstream B evidence must instead be tied to the negative variant's
own target and to the reduced squaring-chain witness from ADR-0125.

## Decision

Add an executable evidence screen for Workstream B candidate records. The screen
is not a proof checker. It decides whether a candidate may proceed to proof
verification or is rejected before verification because it is the wrong kind of
evidence.

For the total-multiplication variant, the screen rejects candidates that:

- cite Group-3 with the `sjas-axiom` certificate;
- use the ordinary structural SelfCons tableau as the claimed boundary
  certificate;
- target a different system code or SelfCons theorem code;
- omit the reduced squaring-chain witness dependency;
- claim proof-search synthesis without a durable `test-runs/` log.

Candidates that pass this screen still complete no Workstream B obligation by
themselves. They become `:verification-required` inputs for later ADRs that must
actually validate the constructed certificate or replay the synthesis evidence.

## Consequences

- Future total-multiplication certificate ADRs get an executable intake
  boundary before expensive proof checking.
- The audit can distinguish ordinary self-justification from
  Goedel-boundary-failure evidence.
- `:constructed-certificate` and `:proof-search-synthesis` remain open after
  this ADR.

## Test Obligations

Red first:

- the Workstream B audit lacks evidence-screen criteria;
- screen tests fail because no public screen helper exists.

Green after implementation:

- the audit records an evidence screen for total multiplication;
- a Group-3 `sjas-axiom` citation is rejected as ordinary self-justification;
- an ordinary structural SelfCons tableau candidate is rejected;
- a candidate for the wrong generated system/target is rejected;
- a nontrivial candidate shape tied to the ADR-0125 reduced witness and the
  ADR-0126 target returns `:verification-required`, not completion;
- synthesis candidates without durable logs are rejected;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0127, the ADR index, and `LOG.md` identify the evidence-screen slice.
- Public audit/screen helpers prevent trivial SelfCons proofs from satisfying
  Workstream B boundary obligations.
- No constructed certificate or proof-search synthesis completion is claimed.
