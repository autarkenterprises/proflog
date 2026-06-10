# AAR-0092: SJAS NNF-Based Pi*1-Encodability

- Date: 2026-06-10
- ADR: [ADR-0092](../adr/ADR-0092-sjas-nnf-pi-star-1-encodability.md)
- Branch: `adr-0088-sjas-runtime-rebaseline`

## Outcome

`pi-star-1-encodable?` now classifies on negation normal form: a formula
has a `Pi*1` encoding when its NNF (via `proflog.normalize/to-nnf`)
contains no positive unbounded existential, with the bounded-existential
desugaring `(exists x (and (leq x bound) body))` recognized as bounded.
The presented-shape classifiers `delta-star-0?` and `pi-star-1?` are
unchanged and are still accepted first as the cheap common case. The
ADR-0087 rejection regression was corrected by the same literature
reading: a reflected clause body's quantifiers sit negatively, so an
antecedent existential prenexes universally and is admissible, while a
universal clause body negates to a positive existential and is rejected.

## Evidence

Red (ADR-0088 sweep): the guarded-scope reflected-body regression errored
at system construction —
`SJAS reflected basis formula lacks a Pi*1 encoding` for
`(implies (exists v0 (true)) (pos (app guarded-scope-structural-demo)))` —
a formula Willard 2013 Definition 5.1 admits, since
`(exists x phi) -> psi == forall x (phi -> psi)`.

Green: the guarded-scope regression and the revised
`sjas-system-rejects-non-pi-star-1-reflected-basis` (universal body
rejected, antecedent existential accepted, worked-example shapes
buildable) in the 29-assertion run recorded in AAR-0091, plus the
ADR-0087 classifier selectors in the 147-assertion semantic batch.

Broad gates (shared with ADR-0091): `lein test-proflog-fast` Ran 171 tests containing 679 assertions. 0 failures, elapsed 3:40.68; `lein test-proflog-extended` Ran 68 tests containing 203 assertions. 0 failures, elapsed 8:58.27.

## Follow-up

- The NNF criterion treats `once-forall` as universal-flavored, matching
  the normalizer; if Track 2a classifies `once-forall` differently for
  apparatus purposes, this validator inherits that classification.
