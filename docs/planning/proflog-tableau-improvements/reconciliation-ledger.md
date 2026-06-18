# Tableau Golden Suite Reconciliation Ledger

ADR-0112 uses this ledger when an expected result differs across the upstream
`tableaux` corpus, an external source, and Proflog.

## Recorded entries

The machine-readable ledger records each unsupported upstream row with an
explicit reason. The table below lists representative reconciliation classes;
see `proflog.literature-tableau-golden/reconciliation-entries` for the complete
26-row unsupported ledger.

| Source test | Upstream result | External expectation | Proflog observation | Status | Resolution | Evidence |
|---|---|---|---|---|---|---|
| `test_wk3_contradiction_satisfiable` | open | open under WK3 | unsupported | unsupported | Classical Proflog cannot reproduce weak Kleene satisfiable contradictions. | inventory `:comprehensive/test-wk3-contradiction-satisfiable` |
| `test_priest_excluded_middle_not_tautology` | open under WK3 | not tautology in Priest logic | unsupported | unsupported | Deferred to unsupported non-classical entries. | `literature/test-priest-excluded-middle-not-tautology` |
| `test_mode_aware_api` | signed/WK3 API behavior | mode-aware signed tableau API | unsupported | unsupported | Upstream API behavior has no portable Proflog semantic assertion. | `comprehensive/test-mode-aware-api` |
| `test_ferguson_epistemic_closure_conditions` | wKrQ closure behavior | Ferguson epistemic closure conditions | unsupported | unsupported | Ferguson epistemic closure semantics are outside the classical kernel. | `literature/test-ferguson-epistemic-closure-conditions` |

Machine-readable copy: `proflog.literature-tableau-golden/reconciliation-entries`.
