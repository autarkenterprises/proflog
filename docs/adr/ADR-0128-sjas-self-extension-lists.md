# ADR-0128: SJAS Self-Extension Lists From Pairs

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0128-sjas-self-extension-lists`
- AAR: [AAR-0128](../aar/AAR-0128-sjas-self-extension-lists.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) sets the default
Workstream C path as reflected pair axioms first and list axioms second, with
pairs serving as the intermediate representation layer for lists.
[ADR-0123](ADR-0123-sjas-self-extension-pair-survey.md) completed the survey
and the pair projection demo. The next Workstream C slice should therefore
install a small, reflected list layer that depends on the pair layer without
attempting full recursive list processing or encoded syntax manipulation.

## Decision

Add a reflected list extension on top of the ADR-0123 pair layer:

- `list-constants`, declaring a distinguished empty-list constant;
- `list-functions`, declaring list constructor/projection functions;
- `list-nil-term`, `list-cons-term`, `list-head-term`, and
  `list-tail-term` source helpers;
- `list-constructor-axioms`, finite beta laws that define
  `list-cons(x,xs)` as the pair representation and expose head/tail
  projections;
- `list-extension-options`, returning the mergeable pair-plus-list reflected
  beta fragment;
- `list-extended-system`, building a Level-1-compatible system whose generated
  source identity and SelfCons statement include both pair and list beta
  records.

This ADR does not implement recursive list predicates, induction, or encoded
syntax manipulation. It is a data-structure extension slice, not the complete
self-interpretation program.

## Consequences

- Workstream C now has both stages of its default data-representation spine:
  pairs first, then lists backed by pairs.
- The list layer remains finite and Pi*1-admissible.
- Future self-extension ADRs can add list recursion or encoded syntax
  operations while citing this pair-backed list representation.

## Test Obligations

Red first:

- the Workstream C audit still records lists as a second-stage candidate rather
  than an implemented reflected demo;
- list helper tests fail because no public list extension surface exists.

Green after implementation:

- list constants/functions have the expected shape;
- list constructor/projection axioms are Pi*1-admissible;
- list extension options include the pair representation layer;
- `list-extended-system` declares pair and list symbols;
- reflected list beta records are decoded by `axiom-member/2`;
- reflected list beta records are citeable by `tableau-proof/3` with
  `sjas-axiom`;
- adding the list extension changes system identity and regenerated Group-3 /
  SelfCons code compared with the same signatures and only the pair extension;
- the Workstream C audit marks the pair-backed list demo implemented while
  keeping recursive list processing and encoded syntax manipulation open;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- ADR-0128, the ADR index, and `LOG.md` identify this Workstream C slice.
- Public helpers expose a reflected pair-backed list extension.
- No recursive list-processing or full self-interpretation completion is
  claimed.
