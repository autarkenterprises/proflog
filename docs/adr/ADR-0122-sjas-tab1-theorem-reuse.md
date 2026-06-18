# ADR-0122: SJAS Tab-1 Theorem Reuse

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0122-sjas-tab1-theorem-reuse`
- AAR: [AAR-0122](../aar/AAR-0122-sjas-tab1-theorem-reuse.md)

## Context

[ADR-0121](ADR-0121-sjas-tab1-entry-validation.md) made `tab1-proof/3` and
`dsjas-tab1-proof/3` executable for proof-list entry validation. Each entry
proof is decoded and checked by the arithmeticized tableau proof predicate, and
intermediate entries are required to be `Pi*_1` or `Sigma*_1`.

ADR-0121 deliberately did not complete the central Tab-k proof-list clause from
[ADR-0119](ADR-0119-sjas-next-research-roadmap.md): each `p_i` must prove
`t_i` from beta plus the earlier `t_j`. The current checker validates every
entry against the base system alone. That is enough for single-entry proof
lists, but it does not yet make earlier proof-list theorems usable by later
entries.

## Decision

Extend the Tab-1 entry checker with earlier-theorem reuse:

- maintain an accumulated list of earlier theorem-code byte strings while
  validating `H`;
- after an intermediate entry validates and passes the `Pi*_1` / `Sigma*_1`
  restriction, add its theorem bytes to the reusable antecedent list;
- when a later proof is the fixed `sjas-axiom` citation, accept it if the
  current theorem bytes match either a finite-system axiom or one earlier
  theorem byte string;
- when a later proof is a structural tableau certificate, reconstruct
  `AxiomConj(S)` and conjoin proof-antecedent forms of the earlier theorem
  formulas before appending the negated current theorem;
- keep the public measured-object checks from ADR-0121 unchanged.

The reusable theorem comparison is byte-level. The formula antecedent path still
decodes those same bytes into proof-side formulas before structural checking, so
the byte identity and formula use are tied to the same public code payload.

## Consequences

- Tab-1 proof lists can now model the intended `beta + earlier t_j` validation
  contract for later entries.
- `sjas-axiom` remains a fixed proof-code marker, but within Tab-1 it can cite
  an earlier proof-list theorem as well as a system axiom.
- Structural later-entry proofs can use earlier theorem formulas as ordinary
  antecedents.
- Full Tab-k generality, Tab-2, and stronger variants remain outside this
  positive implementation path.

## Test Obligations

Red first:

- a core prior-theorem relation test fails because no relation recognizes
  earlier theorem bytes;
- the correspondence audit still reports `:proof-search-theorem-reuse` as a
  deferred obligation;
- the public multi-entry theorem-reuse proof list is not considered green until
  the prior-citation path is implemented.

Green after implementation:

- focused core prior-theorem and audit selectors;
- focused public multi-entry `tab1-proof/3` selector;
- ADR-0121 focused selectors;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- A later Tab-1 entry can cite an earlier theorem by `sjas-axiom` without that
  theorem being a finite-system axiom.
- Earlier theorem formulas are available as structural tableau antecedents for
  later entries.
- The intermediate theorem class restriction remains in force before a theorem
  enters the reusable list.
- The correspondence audit records theorem reuse as implemented and leaves no
  Workstream A Tab-1 proof-list validation obligation open except future
  generalization or optimization ADRs.
