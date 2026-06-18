# ADR-0132: SJAS Total-Multiplication Certificate Validation

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0132-sjas-total-mul-certificate-validation`
- AAR: [AAR-0132](../aar/AAR-0132-sjas-total-mul-certificate-validation.md)

## Context

[ADR-0126](ADR-0126-sjas-total-mul-full-target.md) names the exact generated
SelfCons refutation target for the total-multiplication reduced-witness
variant. [ADR-0131](ADR-0131-sjas-boundary-certificate-verifier.md) adds the
metadata verifier that can accept a constructed-certificate candidate only when
proof validation has succeeded against that same target.

The missing bridge is a target-specific validation helper that takes an encoded
proof code, runs the existing public `tableau-proof/3` predicate against the
total-multiplication reduced-witness system's generated SelfCons theorem code,
and returns a durable validation record suitable for ADR-0131. This still does
not supply the final contradiction certificate; it makes the proof-checking
step executable and auditable.

## Decision

Add a total-multiplication constructed-certificate validation helper:

- build the ADR-0125 total-multiplication reduced-witness system;
- derive the ADR-0126 generated SelfCons target report from that system;
- classify the supplied proof code as `:sjas-axiom`, `:structural-tableau`, or
  `:unreadable-proof-code`;
- run the public `tableau-proof/3` predicate against the generated Group-3
  SelfCons theorem code;
- return a validation record containing the variant, system code,
  SelfCons theorem code, ADR-0126 refutation formula, proof code,
  certificate kind, validator, proof count, fuel, and boolean success.

Update `verify-boundary-constructed-certificate` so a candidate's declared
certificate kind must match the validation result. This prevents a caller from
labeling an ordinary `sjas-axiom` Group-3 citation as a constructed
contradiction certificate.

## Consequences

- Total-multiplication constructed-certificate work now has an executable
  proof-validation bridge.
- Ordinary SelfCons citations can still validate as SJAS proofs, but they
  cannot pass the ADR-0131 constructed-certificate verifier under a false
  certificate-kind label.
- The total-multiplication Workstream B obligations remain open until a later
  ADR supplies a concrete nontrivial certificate and proof-search synthesis
  evidence.

## Test Obligations

Red first:

- the total-multiplication target report has no certificate-validation helper;
- the ADR-0131 verifier does not reject mismatched candidate/validation
  certificate kinds.

Green after implementation:

- the validation helper reports the ADR-0126 target formula and generated
  Group-3 code for the total-multiplication reduced-witness system;
- a public `sjas-axiom` proof code validates as a proof of the generated
  Group-3 SelfCons theorem but is classified as `:sjas-axiom`;
- the ADR-0131 verifier rejects a candidate that labels that validation as a
  constructed contradiction certificate;
- an unreadable proof code returns `:proof-valid? false`;
- the Workstream B roadmap still keeps the total-multiplication constructed
  certificate and synthesis obligations open;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0132, the ADR index, and `LOG.md` identify this Workstream B validation
  bridge.
- Public helpers expose total-multiplication certificate validation.
- No constructed certificate or proof-search synthesis completion is claimed.
