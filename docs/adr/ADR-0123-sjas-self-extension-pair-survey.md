# ADR-0123: SJAS Self-Extension Pair Survey

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0123-sjas-self-extension-pair-survey`
- AAR: [AAR-0123](../aar/AAR-0123-sjas-self-extension-pair-survey.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) defines the third SJAS
workstream as consistency-preserving self-interpretation / self-extension. It
requires a design survey before implementation, with reflected pair axioms as
the default first demonstration and list axioms second.

The current system builder already accepts finite reflected beta axioms and
regenerates the encoded system source, Group-3 axiom, and SelfCons code from
that reflected basis. That capability needs to be made explicit for the
self-extension workstream: a data-structure extension should be visible as a
changed system identity and a changed generated self-consistency statement, not
as an external runtime helper.

## Survey

Candidate encodings:

- **Fresh pair functions**: add `pair/2`, `fst/1`, and `snd/1` as fresh
  function symbols plus the two universal beta axioms
  `forall x y. fst(pair(x,y)) = x` and
  `forall x y. snd(pair(x,y)) = y`.
- **Lists from pairs**: add `nil`, `cons/2`, and recursive list relations after
  pairs are available.
- **Tagged constants only**: add a finite set of fresh constants or unary tags
  to demonstrate source identity changes without useful data operations.

The pair-first path is selected:

- the beta axioms are finite and beta-axiomatizable;
- the axioms are universal closures over Delta-star-0 equalities, so they stay
  within the existing Level-1 classifier discipline;
- the extension changes the encoded system source and therefore the generated
  Group-3 / SelfCons code;
- proof-search cost is bounded because the first demo only cites the projection
  axioms and checks regenerated identity, rather than introducing recursive
  list search.

Lists remain the second stage. Constants-only encodings are rejected as too weak
for self-extension: they demonstrate identity churn but not data-structure
operations.

## Decision

Add a public pair-extension surface:

- `pair-term`, `fst-term`, and `snd-term` constructors;
- `pair-functions`, the finite function-signature extension;
- `pair-projection-axioms`, returning the two reflected beta axioms;
- `pair-extension-options`, returning the beta/functions fragment that can be
  merged into an SJAS system configuration;
- `pair-extended-system`, building a finite SJAS system whose reflected beta
  basis includes the pair projection axioms.

The implementation must not add pair axioms as external clauses. They must be
reflected beta axioms so the encoded system source, Group-3 reconstruction, and
SelfCons formula all change.

## Consequences

- Workstream C has an inspectable first demonstration of reflected
  consistency-preserving extension.
- Pair axioms become the selected intermediate representation layer for future
  list axioms.
- This ADR does not implement list recursion or richer encoded syntax
  manipulation.
- Truth of the fresh pair axioms remains the standard definitional-extension
  premise: the code proves that the extension is reflected and class-admissible,
  not a model-theoretic consistency theorem inside the object language.

## Test Obligations

Red first:

- pair-extension helper tests fail because no public helpers exist;
- self-extension survey audit fails because no executable survey record exists;
- identity/SelfCons tests fail because there is no pair-extended system helper.

Green after implementation:

- pair projection axioms classify as `Pi*_1` encodable;
- the pair-extended system contains two additional Group-2 beta records;
- pair projection beta records are citeable by `axiom-member` and
  `tableau-proof` with `sjas-axiom`;
- the pair-extended system code and Group-3 code differ from the baseline;
- an external-only pair-like runtime clause does not change the baseline
  encoded SelfCons code;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- The selected pair-first survey is recorded in an executable audit.
- Reflected pair beta axioms are public, class-admissible, and part of the
  generated encoded system source.
- Regenerated Group-3/SelfCons content changes when pair beta axioms are added.
- No list-extension completion is claimed.
