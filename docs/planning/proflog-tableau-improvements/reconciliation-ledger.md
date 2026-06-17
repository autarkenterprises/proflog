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

Machine-readable copy: `proflog.literature-tableau-golden/reconciliation-entries`.
