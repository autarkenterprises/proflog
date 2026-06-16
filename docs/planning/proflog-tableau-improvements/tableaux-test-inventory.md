# `bradleypallen/tableaux` Test Inventory

- Source: https://github.com/bradleypallen/tableaux
- Reviewed commit: `fa5a736090465d0ddf35362a6271d4298d668d42`
- Review date: 2026-06-16

This file seeds ADR-0112. It is not the final golden-suite inventory; it is the
minimum corpus that the ADR-0112 implementation must account for.

## Disposition Labels

- `direct`: implement the same semantic expectation in Proflog.
- `analog`: implement the closest Proflog-level semantic analog.
- `performance`: convert to a branch-growth or runtime-envelope test.
- `source-confirm`: seek an external source and independently confirm the
  expected result through Proflog.
- `unsupported`: record why Proflog does not support the construct or why the
  upstream test is non-portable.
- `pending`: not yet classified.

## Required Source Files

| Source file | Active items | Required ADR-0112 treatment |
|---|---:|---|
| `tests/test_comprehensive.py` | 35 test functions | classify every test |
| `tests/test_literature_examples.py` | 26 test functions | classify every test and seek cited sources |
| `tests/test_performance.py` | 4 test functions | convert to semantic plus envelope tests |
| `tests/test_setup.py` | 1 smoke script | classify as setup smoke or non-portable |
| `tests/test_formulas.txt` | 10 CLI formulas | classify parser/input examples |

Deprecated commented tests in the source files are not counted as active
upstream tests, but ADR-0112 may mine them for future examples if useful.

## `test_comprehensive.py`

Initial disposition for every item: `pending`.

### Classical Propositional Logic

- `test_simple_atom`
- `test_simple_negation`
- `test_contradiction_basic`
- `test_contradiction_complex`
- `test_tautology_excluded_middle`
- `test_tautology_transitivity`
- `test_tautology_material_implication`
- `test_satisfiable_conjunction`
- `test_satisfiable_disjunction`
- `test_satisfiable_implication`
- `test_complex_nested_formula`
- `test_de_morgan_laws`
- `test_multiple_formulas_consistent`
- `test_multiple_formulas_inconsistent`

### Weak Kleene Logic

- `test_wk3_simple_atom`
- `test_wk3_contradiction_satisfiable`
- `test_wk3_truth_values`
- `test_wk3_model_evaluation`

### First-Order Predicate Syntax

- `test_term_creation`
- `test_predicate_creation`
- `test_predicate_with_variables`
- `test_atom_backward_compatibility`

### Mode-Aware System

- `test_mode_detection`
- `test_mode_aware_api`
- `test_mixed_mode_prevention`

### Optimizations And Performance

- `test_formula_prioritization`
- `test_subsumption_elimination`
- `test_early_satisfiability_detection`
- `test_performance_complex_formula`
- `test_model_extraction_correctness`

### Edge Cases And Regressions

- `test_empty_formula_list`
- `test_single_formula_in_list`
- `test_very_deep_nesting`
- `test_large_disjunction`
- `test_large_conjunction`

## `test_literature_examples.py`

Initial disposition for every item: `pending`.

### Priest Examples

- `test_priest_weak_kleene_conjunction_table`
- `test_priest_excluded_middle_not_tautology`
- `test_priest_contradiction_satisfiable_wk3`
- `test_priest_signed_tableau_rules`

### Fitting Examples

- `test_fitting_basic_expansion_example`
- `test_fitting_closure_example`
- `test_fitting_satisfiable_example`

### Smullyan Examples

- `test_smullyan_alpha_beta_classification`
- `test_smullyan_systematic_tableau_construction`
- `test_smullyan_completeness_example`

### Handbook Examples

- `test_handbook_signed_semantic_tableaux`
- `test_handbook_three_valued_tableaux`
- `test_handbook_optimization_techniques`

### Literature Edge Cases

- `test_deep_nesting_priest_example`
- `test_three_valued_non_classical_behavior`
- `test_fitting_branch_bound_example`

### Ferguson wKrQ Examples

- `test_ferguson_epistemic_disjunction_example`
- `test_ferguson_epistemic_contradiction_non_closure`
- `test_ferguson_classical_contradiction_still_works`
- `test_ferguson_sign_duality_in_negation`
- `test_ferguson_restricted_quantifier_example`
- `test_ferguson_universal_quantifier_uncertainty`
- `test_ferguson_mixed_epistemic_reasoning`
- `test_ferguson_non_classical_tautology_behavior`
- `test_ferguson_comparison_with_classical_three_valued`
- `test_ferguson_epistemic_closure_conditions`

## `test_performance.py`

Initial disposition for every item: `pending`.

- `test_prioritization_benefit`
- `test_subsumption_benefit`
- `test_complex_formula_performance`
- `test_termination_correctness`

## `test_setup.py`

Initial disposition: `pending`.

- Smoke script constructs atom `p`, builds a classical signed tableau for `T:p`,
  and expects a successful build.

## `test_formulas.txt`

Initial disposition for every formula: `pending`.

- `p`
- `p & q`
- `p | q`
- `p -> q`
- `p | ~p`
- `(p -> q) | (q -> p)`
- `p & ~p`
- `(p -> q) & p & ~q`
- `(p | q) & (~p | r)`
- `((p -> q) -> p) -> p`

## External Source Search Targets

ADR-0112 must seek and record source expectations for at least:

- Smullyan alpha/beta classification and systematic tableau construction.
- Fitting signed tableau expansion, closure, satisfiable branches, and branch
  bound examples.
- Priest weak Kleene truth tables and non-classical behavior.
- Ferguson weak Kleene restricted quantification examples where Proflog has a
  supported analog.
- D'Agostino/Gabbay/Hahnle/Posegga handbook-style signed semantic tableaux and
  optimization examples.
