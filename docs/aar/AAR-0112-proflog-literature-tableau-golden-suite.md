# AAR-0112: Proflog Literature Tableau Golden Suite

- Date: 2026-06-17
- ADR: [ADR-0112](../adr/ADR-0112-proflog-literature-tableau-golden-suite.md)
- Branch: `adr-0112-proflog-literature-tableau-golden-suite`
- Status: complete

## Outcome

Completed the literature tableau golden suite as
`proflog.literature-tableau-golden`, with fast tests in
`proflog.literature-tableau-golden-test` and slow branch-growth checks in
`proflog.literature-tableau-golden-extended-test`.

## Evidence

Inventory at reviewed upstream commit `fa5a736`:

| Disposition | Count |
|-------------|------:|
| `:direct` | 30 |
| `:analog` | 13 |
| `:performance` | 7 |
| `:unsupported` | 26 |
| **Total** | **76** |

Runnable Proflog-confirmed entries: 50. Reconciliation ledger: 26 unsupported
rows with explicit non-portability reasons.

The final catalog does not preserve a separate `:source-confirm` disposition.
Literature-attributed rows are classified as `:direct`, `:analog`,
`:performance`, or `:unsupported` according to what Proflog actually checks,
with source notes retained on the row.

Gates:

- `lein test-proflog-fast` includes `proflog.literature-tableau-golden-test`
  (non-`^:slow` assertions).
- `lein test-proflog-extended` includes
  `proflog.literature-tableau-golden-extended-test` for branch-growth envelope
  tests (`^:slow`).

Commands:

- `lein test proflog.literature-tableau-golden-test` — 8 tests, 593 assertions.
- `lein test proflog.literature-tableau-golden-extended-test` — 2 tests, 9 assertions.

## Residual gaps

- 26 upstream items remain `:unsupported` (weak Kleene, signed tableaux, Ferguson
  epistemic fragments, predicate API shape tests).
- Performance upstream tests are represented with Proflog-observed semantics
  and measured structurally by ADR-0115.
