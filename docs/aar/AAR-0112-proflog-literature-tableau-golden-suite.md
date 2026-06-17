# AAR-0112: Proflog Literature Tableau Golden Suite

- Date: 2026-06-17
- ADR: [ADR-0112](../adr/ADR-0112-proflog-literature-tableau-golden-suite.md)
- Branch: `adr-0112-proflog-literature-tableau-golden-suite`
- Status: complete (fidelity audit applied 2026-06-17)

## Outcome

Implemented the literature tableau golden suite as `proflog.literature-tableau-golden`
with tests in `proflog.literature-tableau-golden-test` and extended branch-growth
probes in `proflog.literature-tableau-golden-extended-test`.

Interdev review corrections (2026-06-17) added explicit upstream translation
records, corrected mis-mapped formulas, downgraded `:source-confirm` literature
rows to `:analog`, expanded the reconciliation ledger, and strengthened tests with
spot checks for named upstream cases.

## Evidence

Inventory at reviewed upstream commit `fa5a736`:

| Disposition | Count |
|-------------|------:|
| `:direct` | 14 |
| `:analog` | 19 |
| `:performance` | 11 |
| `:unsupported` | 32 |
| **Total** | **76** |

Runnable Proflog-confirmed entries: 44. Translation records: 24 keyed upstream
tests. Reconciliation ledger: 12 entries.

Coverage kinds present: `:closure`, `:open-branch`, `:alpha-beta`, `:contradiction`,
`:branch-growth`.

Gates:

- `lein test-proflog-fast` includes `proflog.literature-tableau-golden-test`.
- `lein test-proflog-extended` includes `proflog.literature-tableau-golden-extended-test`.

## Residual gaps

- 32 upstream items remain `:unsupported` (weak Kleene, signed tableaux, Ferguson
  epistemic fragments, predicate API shape tests).
- Signed branch-shape and model-extraction assertions from Fitting/Smullyan remain
  deferred (recorded in translation `dropped-assertions`).
- Performance upstream tests retain Proflog-observed semantics; timing envelopes
  deferred to ADR-0115 closed-proof baselines.
