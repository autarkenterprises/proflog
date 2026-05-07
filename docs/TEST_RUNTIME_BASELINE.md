# Test Runtime Baseline

Date: 2026-04-23
Branch: `adr-0009-legacy-program-closure`

This document records the duration of the final successful iteration used to
promote a test into the committed greenfield suite. Timings are intentionally
kept as observed wall-clock measurements from the exact successful run that
justified the test.

The reverse/append answer-mode entries below are historical timings from the
pre-ADR-0011 hybrid staging policy. ADR-0011 later moved the default path to
direct kernel entry-call descent and remapped the relevant stage numbers; those
entries are kept here as branch-local runtime history until the new policy is
re-baselined explicitly.

Historical post-ADR-0011 notes from the direct-entry / completion-ranked path:

These notes predate ADR-0013 and ADR-0035. They are retained to explain why
later answer-overlay and residual-continuation work happened; they are not
current public `query-answers` capability claims.

- Nested suffix `append([[a,b]], z, [[a,b],[c]])` does not recover the concrete
  suffix at raw caps `8`, `16`, or `32`, but a longer exploratory probe showed
  the concrete answer surfacing first at `max-raw-proof-limit 64`.
- `reverse([a,b], r)` remained materially harder: a `>120 s` exploratory probe
  at `fuel 64`, `call-depth 3`, and raw budgets up to `64` still did not return
  an exported result slice before manual stop.

Current ADR-0013 note:

- The public `query-answers` surface for the known list-family `append/3` and
  `reverse/2` queries now reuses the ADR-0012 closed-answer materializer. The
  older reverse/append rows below therefore describe the pre-ADR-0013 raw
  symbolic behavior and timings, not the current public closed-answer surface.

Current ADR-0035 note:

- The promoted list-kernel matrix now reaches all catalog targets through the
  ordinary probe path under longer wrappers. The full sweep is recorded in
  [2026-05-03-list-kernel-matrix-long-timeout-sweep.md](log/2026-05-03-list-kernel-matrix-long-timeout-sweep.md).
  The practical default gate remains narrower because one row,
  `append-inverse-flat-longer`, took about `509.5 s` of Clojure-process
  elapsed time.

Current ADR-0040 note:

- The focused legacy-subsumption selector passed on 2026-05-06 with
  `Ran 3 tests containing 63 assertions`, `0 failures, 0 errors`, and
  `elapsed 120.54 s`.
- Passing per-row timings are recorded in
  [AAR-0040](aar/AAR-0040-legacy-subsumption-parity.md). The expensive row is
  the direct kernel Peano proof `PA10 forward 3 + 4 = 7` at `70586.114 ms`.
  Peano answer-mode parity rows use the constructor-recursive profile and close
  in milliseconds.

Current ADR-0042 note:

- The focused equality-fragment status selector passed on 2026-05-06 with
  `Ran 1 tests containing 16 assertions`, `0 failures, 0 errors`, and
  `elapsed 31.82 s`.
- The full kernel finite verifier suite passed after the proof-scoping fix with
  `Ran 4 tests containing 67 assertions`, `0 failures, 0 errors`, and
  `elapsed 135.63 s`.
- The ADR-42 commit gate also passed `lein test-proflog-fast` with
  `Ran 117 tests containing 381 assertions`, `0 failures, 0 errors`, and
  `elapsed 85.00 s`, plus `lein test-proflog-extended` with
  `Ran 68 tests containing 203 assertions`, `0 failures, 0 errors`, and
  `elapsed 231.52 s`.
- After ADR-41 landed on top, `lein test-proflog-kernel-finite-verifiers` was
  rerun with `Ran 4 tests containing 67 assertions`, `0 failures, 0 errors`, and
  `elapsed 113.40 s`.

Current ADR-0041 note:

- The promoted constructor-recursive profile namespace passed on 2026-05-06 with
  `Ran 4 tests containing 21 assertions`, `0 failures, 0 errors`, and
  `elapsed 11.32 s`.
- The constructor-recursive gate, now including the promoted profile tests,
  passed with `Ran 10 tests containing 42 assertions`, `0 failures, 0 errors`,
  and `elapsed 39.97 s`.
- The ADR-40 legacy-subsumption selector passed after migrating Peano answer rows
  to the promoted profile with `Ran 3 tests containing 63 assertions`,
  `0 failures, 0 errors`, and `elapsed 50.37 s`.
- The ADR-41 final commit gate passed `lein test-proflog-fast` with
  `Ran 117 tests containing 381 assertions`, `0 failures, 0 errors`, and
  `elapsed 106.12 s`, plus `lein test-proflog-extended` with
  `Ran 68 tests containing 203 assertions`, `0 failures, 0 errors`, and
  `elapsed 278.50 s`.

Current query-status boundary note:

- A red characterization pass on 2026-05-06 wired the new inconsistent-status
  assertion to an ordinary compiled `p(0)` program; it failed with
  `actual: :succeeds` and `elapsed 19.65 s`.
- The final characterization test
  `proflog.query-test/query-status-can-report-inconsistent-for-unsound-compiled-program`
  passed with `Ran 1 tests containing 3 assertions`, `0 failures, 0 errors`,
  and `elapsed 15.12 s`.
- The commit gate passed `lein test-proflog-fast` with
  `Ran 118 tests containing 384 assertions`, `0 failures, 0 errors`, and
  `elapsed 74.65 s`.

Current ADR-0043 documentation-refresh note:

- Historical exploratory runtime rows are now labelled as historical rather
  than current capability boundaries. The post-ADR-35 sweep is the current
  reachability reference for the raw list-kernel matrix.
- The current focused reverse answer row
  `proflog.answers-test/query-answers-use-call-depth-1-to-refine-the-direct-reverse-frontier`
  passed with `Ran 1 tests containing 2 assertions`, `0 failures, 0 errors`,
  and `elapsed 14.06 s`.
- The current focused inverse append row
  `proflog.answers-test/query-answers-prefer-the-first-concrete-inverse-append-split-over-symbolic-frontiers`
  passed with `Ran 1 tests containing 2 assertions`, `0 failures, 0 errors`,
  and `elapsed 10.91 s`.
- The ADR-43 commit gate passed `lein test-proflog-fast` with
  `Ran 118 tests containing 384 assertions`, `0 failures, 0 errors`, and
  `elapsed 67.73 s`.

Current ADR-0010 frontend note:

- The focused frontend selector passed on 2026-05-06 with
  `Ran 6 tests containing 21 assertions`, `0 failures, 0 errors`, and
  `elapsed 12.25 s`.
- The ADR-0010 frontend commit gate passed `lein test-proflog-fast` with
  `Ran 124 tests containing 405 assertions`, `0 failures, 0 errors`, and
  `elapsed 74.84 s`.
- The worked-example descent enrichment pass on 2026-05-06 reran both standard
  greenfield gates. `lein test-proflog-fast` passed with
  `Ran 124 tests containing 405 assertions`, `0 failures, 0 errors`, and
  `elapsed 75.22 s`. `lein test-proflog-extended` passed with
  `Ran 68 tests containing 203 assertions`, `0 failures, 0 errors`, and
  `elapsed 204.88 s`.
- The tutorial refresh on 2026-05-07 reran both standard greenfield gates after
  documenting the ADR-0010 frontend, the then-open answer-query binder boundary,
  and ADR-0041 constructor-recursive profile.
  `lein test-proflog-fast` passed with
  `Ran 124 tests containing 405 assertions`, `0 failures, 0 errors`, and
  `elapsed 99.12 s`. `lein test-proflog-extended` passed with
  `Ran 68 tests containing 203 assertions`, `0 failures, 0 errors`, and
  `elapsed 265.39 s`.
- The ADR-0010 answer-query binder pass on 2026-05-07 first failed red with
  `No such var: pf/answer-query` in `9.77 s`. After implementation,
  `lein test proflog.frontend-test` passed with
  `Ran 8 tests containing 27 assertions`, `0 failures, 0 errors`, and
  `elapsed 14.21 s`. The commit gate passed `lein test-proflog-fast` with
  `Ran 126 tests containing 411 assertions`, `0 failures, 0 errors`, and
  `elapsed 81.60 s`, plus `lein test-proflog-extended` with
  `Ran 68 tests containing 203 assertions`, `0 failures, 0 errors`, and
  `elapsed 215.41 s`.
- The ADR-0010 `pf/run` answer-evaluator pass on 2026-05-07 first failed red
  with `No such var: pf/run` in `10.33 s`. After implementation,
  `lein test proflog.frontend-test` passed with
  `Ran 10 tests containing 30 assertions`, `0 failures, 0 errors`, and
  `elapsed 26.96 s`. The commit gate passed `lein test-proflog-fast` with
  `Ran 128 tests containing 414 assertions`, `0 failures, 0 errors`, and
  `elapsed 180.26 s`, plus `lein test-proflog-extended` with
  `Ran 68 tests containing 203 assertions`, `0 failures, 0 errors`, and
  `elapsed 610.08 s`. The fast and extended gates were run concurrently, so
  these wall times include resource contention from the paired run.

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
| `query-stage-diagnostics-summarize-proof-families` | `proflog.answers-test` | duplicate `dup(x)` stage diagnostics at first unfolded stage | `20.16 s` | The stronger harness now reports duplicate exported answers separately from distinct proof signatures. |
| `query-answer-diagnostics-can-explain-a-recursive-symbolic-frontier` | `proflog.answers-test` | `reverse([a,b], r)` diagnostics at `call-depth 1` | `17.67 s` | Captures the first symbolic frontier as `r = []` plus deferred `reverse/append` obligations. |
| `query-stage-diagnostics-distinguish-productive-and-dry-reverse-stages` | `proflog.answers-test` | `reverse([a,b], r)` stage sweep across depths `0`, `1`, `2` | `95.98 s` | Confirms stage `1` is productive while stage `2` is completely dry at `fuel 32`, `raw-limit 1`. |
| `query-answers-use-call-depth-1-to-refine-the-direct-reverse-frontier` | `proflog.answers-test` | `reverse([a,b], r)` at `call-depth 1`, `fuel 64`, `max-raw-proof-limit 64` | `14.06 s` | Current public `query-answers` returns the closed answer `r = [b,a]` with no residuals while diagnostics still expose the raw symbolic frontier. |
| `query-answers-prefer-the-first-concrete-inverse-append-split-over-symbolic-frontiers` | `proflog.answers-test` | `append(a, b, [a,b,c])` inverse query at `call-depth 1` | `10.91 s` | Current public `query-answers` returns all four closed inverse splits with empty residuals. |
| `member-empty-list-fails` | `proflog.list-programs-test` | `member(a, [])` | `565.030374 ms` | Immediate constructor-clash failure after opening the existential list shape. |
| `append-two-step-ground-case-succeeds` | `proflog.list-programs-test` | `append([a, b], [c], [a, b, c])` | `154219.489533 ms` | Required fuel `256`; semantically closed but expensive. |
| `append-forward-query-binds-a-three-element-result` | `proflog.list-programs-test` | `append([a], [b, c], z)` | `68873.149268 ms` | Concrete three-element result exported at call-depth `2`; shallow `neq` residuals remain. |
| `reverse-two-element-list-succeeds` | `proflog.list-programs-test` | `reverse([a, b], [b, a])` | `276769.773115 ms` | Required fuel `256`; recursive reverse remains materially slower than append. |
| `append-nested-forward-query-binds-the-concrete-result` | `proflog.list-programs-test` | `append([[a]], [[b]], z)` | `41655.620203 ms` | Concrete nested binding exported at call-depth `2`; shallow `neq` residuals remain. |
| `append-nested-suffix-query-binds-the-concrete-second-argument` | `proflog.list-programs-test` | `append([[a, b]], z, [[a, b], [c]])` | `26539.838541 ms` | Concrete nested suffix exported at call-depth `2`; shallow `neq` residuals remain. |

## Historical Exploratory Runtime Boundaries

These rows are retained as branch-local history from earlier answer-mode and
raw-export probes. They are not the current capability boundary when a later
section or focused test row contradicts them. In particular, ADR-35 and ADR-43
supersede the older reverse, inverse-append, and nested-append "no closed answer
yet" readings for current public `query-answers` or long-timeout matrix
reachability.

| Probe | Final successful runtime | Result | Operational note |
|---|---:|---|---|
| `append(xs, ys, [a, b, c])` diagnostics, `call-depth 2`, `raw-limit 1` | `28915.464495 ms` | 1 unique answer | Only the base split `([], [a,b,c])` is visible at the first raw frontier. |
| `append(xs, ys, [a, b, c])` diagnostics, `call-depth 2`, `raw-limit 2` | `41559.381232 ms` | 2 unique answers | The first recursive split `([a], [b,c])` appears, but no deeper split yet. |
| `append(xs, ys, [a, b, c])` diagnostics, `call-depth 2`, `raw-limit 4` | `53524.490474 ms` | 3 raw proofs, still only 2 unique answers | The third raw proof is a duplicate witness for the second split family. |
| `append(xs, ys, [a, b, c])` stage diagnostics, stages `0..2`, raw-limits `1,2` | `99704.194281 ms` | Stage `0` defer-call only; stages `1` and `2` both productive | Stage `1` reaches a symbolic recursive cons-family, and stage `2` concretizes it into the second split `([a],[b,c])`. |
| `append(xs, ys, [a, b, c])` diagnostics, `call-depth 2`, `raw-limit 4`, proof-family summary | `49118.794415 ms` | 3 raw proofs, 3 distinct proof signatures, 2 unique answers | The third raw proof is not an identical proof duplicate; it is a distinct proof family collapsing to the same exported second answer. |
| `append(xs, ys, [a, b, c])` `query-answers`, `call-depth 2` | `35258.2583 ms` | Returned 2 answer records | The stage policy now reaches the base split and the first recursive split family in one API call. |
| `reverse([a, b], r)` diagnostics, `call-depth 1`, `raw-limit 1` | `1815.796755 ms` | 1 symbolic frontier | Exports `r = []` with deferred `reverse([b], a_3)` and `append(a_3, [a], [])` obligations. |
| `reverse([a, b], r)` stage diagnostics, stages `0..2`, raw-limit `1` | `73709.59746 ms` | Stage `0` defer-call, stage `1` first recursive frontier, stage `2` dry | The reverse gap is currently a dry deeper stage, not just duplicate answer export. |
| `reverse([a, b], r)` diagnostics, `call-depth 2`, `raw-limit 1` | `54681.940331 ms` | 0 raw proofs | The first fully unfolded raw proof does not appear at this fuel slice. |
| `reverse([a, b], r)` `query-answers`, `call-depth 2` | `35910.784284 ms` | Returned 2 fallback symbolic frontier records | Historical pre-ADR-43 row. Current focused coverage now returns `r = [b,a]` through public `query-answers` at `call-depth 1`. |
| Nested `append(x, y, [[a], [b]])` split enumeration | `>180000 ms` | No result before manual stop | Historical row. The post-ADR-35 long-timeout matrix later found `append-inverse-nested` `3 / 3` at raw `8` in `13283.4934 ms`. |
| Depth-3 forward `append(left, right, z)` answer synthesis | `>360000 ms` | No result before manual stop | Historical row from the pre-ADR-35/41 answer-export boundary. Keep as a record of that failed exploratory slice, not as a current general claim about all focused or parity paths. |

## Post-ADR-0035 Long-Timeout List-Kernel Sweep

These rows are not routine gate timings. They record eventual reachability for
the full `proflog.list-kernel-matrix-probe` catalog after ADR-0035 structural
residual continuation.

| Probe | Successful runtime | Result | Operational note |
|---|---:|---|---|
| Full list-kernel catalog, isolated `900 s` wrappers | varies by row | every catalog row returned `:target-found? true` | Most rows returned within tens of seconds. |
| `append-inverse-flat-longer` | `509517.493191 ms` | all `5 / 5` splits found at raw `32` | Heavy outlier; keep outside default regression gates unless that cost is explicitly accepted. |
| Slow reverse and partial reverse rows | `21896.698837 ms` to `70285.584748 ms` | target found | Reverse rows are now eventually reachable, but still too expensive to treat as cheap smoke tests. |

## Legacy Reference Runs

| Legacy test | Final successful runtime | Result |
|---|---:|---|
| `test-Y10-reverse-synth-result` | `27.20 s` | `reverse([a,b], R)` returned `R = [b,a]`. |
| `test-Y12-append-inverse-synth-all-splits` | `28.02 s` | `append(A, B, [a,b,c])` returned all 4 splits. |
| `test-Y15-append-nested-inverse-all-splits` | `17.76 s` | `append(A, B, [[a],[b]])` returned all 3 nested splits. |
| `test-Z04-append-depth3-combined-three-levels` | `17.06 s` | Combined level-0/1/3 depth-3 append synthesis succeeded. |
