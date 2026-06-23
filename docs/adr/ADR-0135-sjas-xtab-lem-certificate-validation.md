# ADR-0135: SJAS Xtab / LEM Certificate Validation

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0135-sjas-xtab-lem-certificate-validation`
- AAR: [AAR-0135](../aar/AAR-0135-sjas-xtab-lem-certificate-validation.md)

## Context

[ADR-0134](ADR-0134-sjas-xtab-lem-full-target.md) exposed the generated
SelfCons refutation target for the Xtab/LEM-as-axiom reduced-witness system.
[ADR-0131](ADR-0131-sjas-boundary-certificate-verifier.md) already supplies a
generic verifier contract, and [ADR-0132](ADR-0132-sjas-total-mul-certificate-validation.md)
instantiated that contract for total multiplication.

The Xtab/LEM target needs the same proof-validation bridge before any future
constructed certificate can count as intermediate Workstream B evidence.

## Decision

Add `xtab-lem-constructed-certificate-validation`, mirroring the
total-multiplication validation helper:

- build the ADR-0133 Xtab/LEM reduced-witness system;
- derive the ADR-0134 full generated SelfCons target report;
- classify the supplied proof code;
- run `tableau-proof/3` against the generated Group-3 theorem code;
- return the validation record consumed by
  `verify-boundary-constructed-certificate`.

Update the Workstream B audit so `:xtab-or-lem-axiom` advertises the
constructed-certificate verifier and validation helper. This does not complete
the `:constructed-certificate` obligation; it only makes future certificate
evidence checkable against the generated target.

## Consequences

- Xtab/LEM constructed-certificate candidates can now be screened and validated
  with the same two-stage evidence intake used by total multiplication.
- The proof-validation record is tied to the generated target code, so a
  candidate for another system or target is rejected by the generic verifier.
- Actual constructed certificates and proof-search synthesis evidence remain
  open.

## Test Obligations

Red first:

- no public Xtab/LEM certificate-validation helper exists;
- the Workstream B audit has no Xtab/LEM constructed-certificate verifier
  metadata.

Green after implementation:

- validation records identify `:xtab-or-lem-axiom`, the ADR-0133 system code,
  the ADR-0134 Group-3 theorem code, and the ADR-0134 target code;
- readable `sjas-axiom` proof code validates as an ordinary Group-3 proof;
- unreadable proof code is rejected without attempting proof search;
- the roadmap records `xtab-lem-constructed-certificate-validation` as the
  validation helper for Xtab/LEM constructed certificates;
- open obligations still include both `:constructed-certificate` and
  `:proof-search-synthesis`;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0135, the ADR index, and `LOG.md` identify this Workstream B validation
  slice.
- Public validation helpers can check Xtab/LEM proof-code candidates against
  the generated target.
- No constructed certificate or proof-search synthesis completion is claimed.
