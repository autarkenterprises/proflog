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
| `decomposition-can-bind-earlier-arguments-before-finding-a-later-clash` | `proflog.equality-test` | `exists a,b,t. [1] = cons(a, cons(b, t))` | `422.261319 ms` | Regression for contradiction discovered only after an earlier parameter binding during equality decomposition. |
| `factored-move-warning-leaves-small-win-positions-unresolved` | `proflog.query-test` | Ground `move/2` plus factored-vs-inline `win/1` | `4226.645269 ms` | Direct proof search still decides ground `move/2`; bounded status leaves factored `win(0)` and `win(1)` unresolved. |
| `acyclic-quantified-spec-distinguishes-acyclic-and-cyclic-small-graphs` | `proflog.quantified-programs-test` | `acyclic-abc`, `acyclic-aba`, `acyclic-abca` | `2400.870986 ms` | Inline graph-property quantifiers prove the acyclic graph and refute the cyclic ones. |
| `sorted2-quantified-spec-distinguishes-small-sorted-and-unsorted-lists` | `proflog.quantified-programs-test` | `sorted2` over `[]`, `[1]`, `[0,1,2]`, `[2,1]`, `[1,2]` | `14.79 s` | Covers the restored legacy empty, singleton, sorted, unsorted, and two-element sorted cases after the equality fix. |
| `subset-quantified-spec-handles-true-false-and-reflexive-cases` | `proflog.quantified-programs-test` | `sub-ab-abc`, `sub-abc-ab`, `sub-a-a` | `2154.439012 ms` | Quantified finite-domain subset specification closes both true cases and refutes the false one. |
| `query-answers-collect-unique-answers-beyond-duplicate-proof-paths` | `proflog.answers-test` | duplicate `dup(x)` proofs for `0` before distinct answer `1` | `7.03 s` | Answer search now collects unique records while the kernel prunes stale disequalities before they can surface as `neq(0, 0)`. |
| `query-answer-diagnostics-reports-raw-vs-unique-growth` | `proflog.answers-test` | duplicate `dup(x)` diagnostics across raw limits `1`, `2`, `4` | `16.66 s` | Diagnostics helper now forces each raw slice eagerly so search time is measured honestly before export/merge analysis. |
| `query-answer-diagnostics-can-explain-a-recursive-symbolic-frontier` | `proflog.answers-test` | `reverse([a,b], r)` diagnostics at `call-depth 1` | `17.67 s` | Captures the first symbolic frontier as `r = []` plus deferred `reverse/append` obligations. |
| `member-empty-list-fails` | `proflog.list-programs-test` | `member(a, [])` | `565.030374 ms` | Immediate constructor-clash failure after opening the existential list shape. |
| `append-two-step-ground-case-succeeds` | `proflog.list-programs-test` | `append([a, b], [c], [a, b, c])` | `154219.489533 ms` | Required fuel `256`; semantically closed but expensive. |
| `append-forward-query-binds-a-three-element-result` | `proflog.list-programs-test` | `append([a], [b, c], z)` | `68873.149268 ms` | Concrete three-element result exported at call-depth `2`; shallow `neq` residuals remain. |
| `reverse-two-element-list-succeeds` | `proflog.list-programs-test` | `reverse([a, b], [b, a])` | `276769.773115 ms` | Required fuel `256`; recursive reverse remains materially slower than append. |
| `append-nested-forward-query-binds-the-concrete-result` | `proflog.list-programs-test` | `append([[a]], [[b]], z)` | `41655.620203 ms` | Concrete nested binding exported at call-depth `2`; shallow `neq` residuals remain. |
| `append-nested-suffix-query-binds-the-concrete-second-argument` | `proflog.list-programs-test` | `append([[a, b]], z, [[a, b], [c]])` | `26539.838541 ms` | Concrete nested suffix exported at call-depth `2`; shallow `neq` residuals remain. |

## Exploratory Runtime Boundaries

| Probe | Final successful runtime | Result | Operational note |
|---|---:|---|---|
| `append(xs, ys, [a, b, c])` diagnostics, `call-depth 2`, `raw-limit 1` | `28915.464495 ms` | 1 unique answer | Only the base split `([], [a,b,c])` is visible at the first raw frontier. |
| `append(xs, ys, [a, b, c])` diagnostics, `call-depth 2`, `raw-limit 2` | `41559.381232 ms` | 2 unique answers | The first recursive split `([a], [b,c])` appears, but no deeper split yet. |
| `append(xs, ys, [a, b, c])` diagnostics, `call-depth 2`, `raw-limit 4` | `53524.490474 ms` | 3 raw proofs, still only 2 unique answers | The third raw proof is a duplicate witness for the second split family. |
| `append(xs, ys, [a, b, c])` answer enumeration | `1372558.603771 ms` | Returned only 2 answer records | Older long probe agrees with the diagnostics: inverse list enumeration remains a major performance gap. |
| `reverse([a, b], r)` diagnostics, `call-depth 1`, `raw-limit 1` | `1815.796755 ms` | 1 symbolic frontier | Exports `r = []` with deferred `reverse([b], a_3)` and `append(a_3, [a], [])` obligations. |
| `reverse([a, b], r)` diagnostics, `call-depth 2`, `raw-limit 1` | `54681.940331 ms` | 0 raw proofs | The first fully unfolded raw proof does not appear at this fuel slice. |
| `reverse([a, b], r)` answer synthesis | `>300000 ms` | No result before manual stop | Reverse synthesis is still materially worse than ground reverse truth checking. |
| Nested `append(x, y, [[a], [b]])` split enumeration | `>180000 ms` | No result before manual stop | Even the short nested inverse family remains operationally expensive. |
| Depth-3 forward `append(left, right, z)` answer synthesis | `>360000 ms` | No result before manual stop | Structural depth alone is enough to make open answer export impractical right now. |

## Legacy Reference Runs

| Legacy test | Final successful runtime | Result |
|---|---:|---|
| `test-Y10-reverse-synth-result` | `27.20 s` | `reverse([a,b], R)` returned `R = [b,a]`. |
| `test-Y12-append-inverse-synth-all-splits` | `28.02 s` | `append(A, B, [a,b,c])` returned all 4 splits. |
| `test-Y15-append-nested-inverse-all-splits` | `17.76 s` | `append(A, B, [[a],[b]])` returned all 3 nested splits. |
| `test-Z04-append-depth3-combined-three-levels` | `17.06 s` | Combined level-0/1/3 depth-3 append synthesis succeeded. |
