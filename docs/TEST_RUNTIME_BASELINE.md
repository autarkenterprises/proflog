# Test Runtime Baseline

Date: 2026-04-22
Branch: `adr-0009-legacy-program-closure`

This document records the duration of the final successful iteration used to
promote a test into the committed greenfield suite. Timings are intentionally
kept as observed wall-clock measurements from the exact successful run that
justified the test.

## Committed Test Iterations

| Test var | Namespace | Query family | Final successful runtime | Notes |
|---|---|---|---:|---|
| `member-empty-list-fails` | `proflog.list-programs-test` | `member(a, [])` | `565.030374 ms` | Immediate constructor-clash failure after opening the existential list shape. |
| `append-two-step-ground-case-succeeds` | `proflog.list-programs-test` | `append([a, b], [c], [a, b, c])` | `154219.489533 ms` | Required fuel `256`; semantically closed but expensive. |
| `append-forward-query-binds-a-three-element-result` | `proflog.list-programs-test` | `append([a], [b, c], z)` | `68873.149268 ms` | Concrete three-element result exported at call-depth `2`; shallow `neq` residuals remain. |
| `reverse-two-element-list-succeeds` | `proflog.list-programs-test` | `reverse([a, b], [b, a])` | `276769.773115 ms` | Required fuel `256`; recursive reverse remains materially slower than append. |
| `append-nested-forward-query-binds-the-concrete-result` | `proflog.list-programs-test` | `append([[a]], [[b]], z)` | `41655.620203 ms` | Concrete nested binding exported at call-depth `2`; shallow `neq` residuals remain. |
| `append-nested-suffix-query-binds-the-concrete-second-argument` | `proflog.list-programs-test` | `append([[a, b]], z, [[a, b], [c]])` | `26539.838541 ms` | Concrete nested suffix exported at call-depth `2`; shallow `neq` residuals remain. |

## Exploratory Runtime Boundaries

| Probe | Final successful runtime | Result | Operational note |
|---|---:|---|---|
| `append(xs, ys, [a, b, c])` answer enumeration | `1372558.603771 ms` | Returned only 2 answer records | Base split plus first recursive split family only; inverse list enumeration remains a major performance gap. |
