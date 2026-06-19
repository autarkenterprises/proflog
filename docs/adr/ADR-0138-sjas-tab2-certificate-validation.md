# ADR-0138: SJAS Tab-2 Certificate Validation

- Status: completed
- Date: 2026-06-19
- Branch: `adr-0138-sjas-tab2-certificate-validation`
- AAR: [AAR-0138](../aar/AAR-0138-sjas-tab2-certificate-validation.md)

## Context

[ADR-0137](ADR-0137-sjas-tab2-full-target.md) added the target-only
`:willard-sjas-tab2-boundary` profile and generated the Tab-2-or-stronger
SelfCons contradiction target. The next Workstream B step is the same
constructed-certificate validation bridge already present for total
multiplication and Xtab/LEM.

This ADR must keep the boundary clear: validating a proof code against the
generated target is not a constructed certificate and is not proof-search
synthesis. It also must not implement a Tab-2 proof-list checker.

## Decision

Add `tab2-or-stronger-constructed-certificate-validation`:

- derive the ADR-0137 target-only system and full target report;
- classify the supplied proof code;
- validate readable proof codes through `tableau-proof/3` against the generated
  Group-3 theorem code;
- use an ordinary validation program with the Tab-2 boundary relation in its
  language, while the checked system code remains the ADR-0137 Tab-2 target;
- return the same validation-record shape used by total multiplication and
  Xtab/LEM.

Extend object-level system-code reconstruction just enough for
`:willard-sjas-tab2-boundary` Group-3 axiom membership and proof-antecedent
reconstruction. This is support for validating the generated target; it is not
a `dsjas-tab2-proof/3` checker.

Update the Workstream B roadmap so `:tab-2-or-stronger` advertises the generic
constructed-certificate verifier and the new validation helper, while keeping
both final evidence obligations open.

## Consequences

- Future Tab-2 constructed-certificate candidates can be screened and validated
  against the exact ADR-0137 generated target.
- The target-only profile remains non-executable as a program proof profile.
- Constructed-certificate completion and synthesis evidence remain later work.

## Test Obligations

Red first:

- no public Tab-2 certificate-validation helper exists;
- the object proof predicate cannot cite the Tab-2 boundary Group-3 axiom from
  the new system-code tag;
- the Workstream B roadmap has no Tab-2 constructed-certificate verifier
  metadata.

Green after implementation:

- validation records identify `:tab-2-or-stronger`, the ADR-0137 system code,
  the ADR-0137 Group-3 theorem code, and the ADR-0137 target code;
- readable `sjas-axiom` proof code validates as an ordinary Group-3 proof;
- unreadable proof code is rejected without proof search;
- the roadmap records
  `tab2-or-stronger-constructed-certificate-validation` as the Tab-2 validation
  helper;
- open obligations still include both `:constructed-certificate` and
  `:proof-search-synthesis`;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0138, the ADR index, and `LOG.md` identify this Workstream B validation
  slice.
- Public validation helpers can check Tab-2 proof-code candidates against the
  generated target.
- No constructed certificate, proof-search synthesis, or Tab-2 checker
  completion is claimed.
