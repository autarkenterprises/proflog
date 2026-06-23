# ADR-0143: SJAS Bounded Surface Validation

- Status: completed
- Date: 2026-06-22
- Branch: `adr-0143-sjas-bounded-surface-validation`
- AAR: [AAR-0143](../aar/AAR-0143-sjas-bounded-surface-validation.md)

## Context

The ADR-0142 review attempted to query a theorem from
`total-multiplication-complete-system`. The query failed before proof search
because the generated axiom conjunction contains `bounded-exists` formulas and
`proflog.language/validate-formula` rejects that tag as malformed.

This is an inconsistent language boundary. `proflog.ast/formula?`, NNF
normalization, capture-avoiding substitution, SJAS formula coding, formula-code
decoding, classification, and the structural tableau checker all support
`bounded-forall` and `bounded-exists`. The generic language validator supports
only unbounded `forall`, `once-forall`, and `exists`.

The defect is broader than ADR-0142. Any public query, clause, or generated
axiom conjunction that reaches generic validation with a bounded quantifier can
throw `Malformed formula` before the supported normalization and proof paths
run.

## Decision

Extend `language/validate-formula` for both bounded quantifier tags. For each
bounded form, validation must:

- validate the bound as an object-language term under the selected language;
- validate the body recursively under the same language;
- preserve the original nominal tie and formula unchanged;
- reject undeclared/mis-arity symbols in either bound or body through the
  existing term/atom diagnostics.

Do not special-case SJAS or normalize before validation. Bounded quantifiers are
already part of the shared AST, so the shared language validator is the correct
ownership boundary.

## Test Obligations

Red before implementation:

- `validate-query` accepts well-formed `bounded-forall` and `bounded-exists`;
- validation rejects an undeclared function in a bound;
- validation rejects an undeclared relation in a bounded body;
- `sjas/query-succeeds` reaches proof search for a generated SJAS system whose
  axiom conjunction contains bounded quantifiers, instead of throwing
  `Malformed formula`;
- the completed multiplication system can cite its Q6 multiplication axiom
  through the public theorem-query path.

The focused test should distinguish validator success from theoremhood. The
regression is that supported syntax reaches normalization/proof checking; it
must not assert that arbitrary bounded formulas are theorems.

## Coverage

Both bounded tags, bound validation, recursive body validation, successful SJAS
integration, and existing malformed-symbol behavior must be covered. No new
kernel branches are introduced; existing bounded-quantifier structural tests
remain the kernel-level coverage.

## Exit Criteria

- Focused language tests fail before the validator change and pass afterward.
- A focused SJAS public-query regression fails before and passes afterward.
- Existing normalization and structural bounded-quantifier tests remain green.
- `lein test-proflog-fast`, `lein test-proflog-extended`, and focused SJAS
  progression pass.
- AAR-0143 records the red/green evidence and any remaining bounded-query
  limitation.
