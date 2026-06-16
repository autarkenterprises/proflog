# AAR-0112: Proflog Literature Tableau Golden Suite

- Date: 2026-06-20
- ADR: [ADR-0112](../adr/ADR-0112-proflog-literature-tableau-golden-suite.md)
- Branch: `adr-0112-proflog-literature-tableau-golden-suite`
- Status: complete

## Outcome

Implemented the literature tableau golden suite as `proflog.literature-tableau-golden`
with tests in `proflog.literature-tableau-golden-test`.

## Evidence

Inventory at reviewed upstream commit `fa5a736`:

| Disposition | Count |
|-------------|------:|
| `:direct` | 14 |
| `:analog` | 13 |
| `:source-confirm` | 6 |
| `:performance` | 11 |
| `:unsupported` | 32 |
| **Total** | **76** |

Runnable Proflog-confirmed entries: 44. Reconciliation ledger: 4 entries
(WK3, Priest excluded middle, optimization semantic note, signed setup smoke).

Coverage kinds present: `:closure`, `:open-branch`, `:alpha-beta`, `:contradiction`,
`:branch-growth`.

Gates:

- `lein test-proflog-fast` includes `proflog.literature-tableau-golden-test`
  (non-`^:slow` assertions).
- `lein test-proflog-extended` includes the same namespace for branch-growth
  envelope tests (`^:slow`).

Command: `lein test proflog.literature-tableau-golden-test` — 6 tests, 298 assertions.

## Residual gaps

- 32 upstream items remain `:unsupported` (weak Kleene, signed tableaux, Ferguson
  epistemic fragments, predicate API shape tests).
- Performance upstream tests are represented with Proflog-observed semantics;
  timing envelopes deferred to ADR-0115.
