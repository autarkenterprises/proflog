# Tableau Golden Suite Reconciliation Ledger

ADR-0112 uses this ledger when an expected result differs across the upstream
`tableaux` corpus, an external source, and Proflog.

## Recorded entries

| Source test | Upstream result | External expectation | Proflog observation | Status | Resolution | Evidence |
|---|---|---|---|---|---|---|
| `test_wk3_contradiction_satisfiable` | open | open under WK3 | unsupported | unsupported | Classical Proflog cannot reproduce weak Kleene satisfiable contradictions. | inventory `:comprehensive/test-wk3-contradiction-satisfiable` |
| `test_priest_excluded_middle_not_tautology` | open under WK3 | not tautology in Priest logic | classical refutation-open | source-disputed | Deferred to unsupported non-classical entries. | `literature/test-priest-excluded-middle-not-tautology` |
| `test_formula_prioritization` | optimization | alpha-before-beta heuristic | open | resolved | Proflog semantic `:open`; timing in ADR-0115. | `comprehensive/test-formula-prioritization` |
| `test_setup.py smoke` | signed build success | signed T:p branch | unsupported | unsupported | Signed smoke non-portable; analog `formulas/p`. | `setup/signed-tableau-smoke` |
| `test_contradiction_complex` | closes | `(p→q)∧p∧¬q` unsatisfiable | closes (NNF) | resolved | Scaffold mapped wrong formula initially; translation record corrected. | `translation-records/test_contradiction_complex` |
| `test_tautology_transitivity` | closes via negation | transitivity tautology | closes | resolved | Refutation mode negates `((a→b)∧(b→c))→(a→c)`. | `translation-records/test_tautology_transitivity` |
| `test_tautology_material_implication` | closes via negation | material implication equivalence | closes | resolved | Dual refutation analog; disposition `:analog`. | `translation-records/test_tautology_material_implication` |
| `test_smullyan_completeness_example` | open | satisfiable with models | open | resolved | Formula corrected to `(p∧q)∨(¬p∧¬q)`; model extraction deferred. | `translation-records/test_smullyan_completeness_example` |
| `test_fitting_satisfiable_example` | open | `(p∨q)∧(¬p∨r)` satisfiable | open | resolved | Formula corrected from bare `p`. | `translation-records/test_fitting_satisfiable_example` |
| `test_fitting_closure_example` | closes | `(p∧¬p)∧q` unsatisfiable | closes | resolved | Formula corrected to include conjunct `q`. | `translation-records/test_fitting_closure_example` |
| `test_fitting_basic_expansion_example` | open | `¬(p∧q)` branch shape | open | deferred | Semantic satisfiability retained; signed branch-shape deferred. | `translation-records/test_fitting_basic_expansion_example` |
| `modus-ponens-fail` CLI slug | closes | `(p→q)∧p∧¬q` unsatisfiable | closes | resolved | Expectation corrected from `:open` after NNF-faithful check. | `formulas/modus-ponens-fail` |

Machine-readable copy: `proflog.literature-tableau-golden/reconciliation-entries`.
