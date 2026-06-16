# Tableau Golden Suite Reconciliation Ledger

ADR-0112 must use this ledger, or an equivalent structured table, whenever an
expected result differs across the upstream `tableaux` corpus, an external
source, and Proflog.

No reconciliation items have been recorded yet; this planning branch only
creates the structure.

## Required Fields

| Field | Meaning |
|---|---|
| Source test | Upstream file and test name, or external source example |
| Upstream result | Result asserted by `bradleypallen/tableaux`, if any |
| External expectation | Result stated or implied by the source literature |
| Proflog observation | Result obtained by running the Proflog analog |
| Status | `resolved`, `deferred`, `unsupported`, or `source-disputed` |
| Resolution | The adopted golden-suite expectation and rationale |
| Evidence | Commands, test vars, source notes, or links |

## Template

| Source test | Upstream result | External expectation | Proflog observation | Status | Resolution | Evidence |
|---|---|---|---|---|---|---|
| _none yet_ | | | | | | |
