# ADR-0092: SJAS NNF-Based Pi*1-Encodability

- Status: completed
- Date: 2026-06-10
- Branch: `adr-0088-sjas-runtime-rebaseline`
- AAR: [AAR-0092](../aar/AAR-0092-sjas-nnf-pi-star-1-encodability.md)

## Context

The ADR-0088 namespace sweep surfaced a defect in ADR-0087's reflected-basis
validation: `pi-star-1-encodable?` classifies the raw surface shape, so it
rejects
`(implies (exists v0 (true)) (pos (app guarded-scope-structural-demo)))` —
the guarded-scope reflected-body fixture — even though an antecedent
existential prenexes universally
(`(exists x phi) -> psi  ==  forall x (phi -> psi)`) and the formula
therefore has a `Pi*1` encoding, which is all Willard 2013 Definition 5.1
requires ("axioms that have `Pi*1` encodings"). The ADR-0087 validator was
stricter than the literature and broke the guarded-scope regression
(`sjas-proof-check-accepts-formula-bearing-guarded-scope-reflected-bodies`,
ERROR at system construction).

## Decision

Classify encodability on negation normal form: a formula is
`Pi*1`-encodable iff its NNF (via `proflog.normalize/to-nnf`) contains no
unbounded positive existential — unbounded `forall`s anywhere in NNF prenex
outward classically, bounded quantifiers belong to the `Delta*0` matrix,
and NNF eliminates `implies`/`not` so polarity is explicit. The
`delta-star-0?` / `pi-star-1?` shape classifiers are unchanged (they
classify presented shapes and are used as such elsewhere); only the
ADR-0087 build-time validation predicate moves to the NNF criterion.

## Test Obligations

- Red: the guarded-scope regression above (already failing in the sweep),
  plus a focused addition to the ADR-0087 rejection selector asserting
  that an antecedent-existential clause body is accepted while a positive
  unbounded existential is still rejected.
- Green: those selectors, the ADR-0087 selector batch, and the broad
  gates shared with ADR-0091.

## Exit Criteria

- Antecedent existentials in reflected bodies build again; positive
  unbounded existentials are still rejected with the diagnostic error;
  no classifier behavior changes outside the validation predicate.
