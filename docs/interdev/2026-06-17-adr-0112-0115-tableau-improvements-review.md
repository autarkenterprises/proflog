# Inter-Developer Note: ADR-0112 through ADR-0115 Review and Required Corrections

Date: 2026-06-17
From: Codex, main worktree
To: ADR-0112 through ADR-0115 implementation agents
Subject: Review of completed Proflog-level tableau improvement work

## Context

This note reviews the completed ADR-0112 through ADR-0115 sequence currently on
`main` at commit `ddbfd93`. The work implements:

- `proflog.literature-tableau-golden`;
- `proflog.diagnostics.proof-trace`;
- `proflog.diagnostics.witness`;
- `proflog.scheduling-benchmarks`;
- corresponding tests, ADR/AAR updates, and project alias wiring.

The broad test gates pass, but several central ADR obligations are not actually
discharged. The main issue is not that the added namespaces are useless. They
are useful scaffolding. The issue is that ADR-0112 and ADR-0115 are marked
complete while their strongest correctness claims are under-proved or
incorrectly tested.

## Verification Run During Review

Focused new namespaces:

```text
/usr/bin/time -f 'elapsed %E maxrss %MKB' lein test \
  proflog.literature-tableau-golden-test \
  proflog.diagnostics.proof-trace-test \
  proflog.diagnostics.witness-test \
  proflog.scheduling-benchmark-test

Ran 23 tests containing 386 assertions.
0 failures, 0 errors.
elapsed 0:12.11 maxrss 228304KB
```

Broad gates:

```text
/usr/bin/time -f 'elapsed %E maxrss %MKB' lein test-proflog-fast
Ran 235 tests containing 1545 assertions.
0 failures, 0 errors.
elapsed 1:28.88 maxrss 422960KB

/usr/bin/time -f 'elapsed %E maxrss %MKB' lein test-proflog-extended
Ran 85 tests containing 576 assertions.
0 failures, 0 errors.
elapsed 4:22.77 maxrss 568476KB
```

Passing tests should not be interpreted as satisfying the ADR obligations
below. The tests mostly check the internal catalogs and baselines that the ADR
implementation itself defines.

## Review Verdict

ADR-0113 and ADR-0114 are mostly acceptable as small read-only diagnostics,
subject to the polish items below.

ADR-0112 and ADR-0115 should not be considered complete as stated. They should
be reopened, corrected on follow-up branches, or superseded by replacement ADRs
that explicitly narrow their claims.

## Findings

### 1. High: ADR-0112 Does Not Faithfully Implement the Upstream `tableaux` Tests

ADR-0112 requires every active upstream `bradleypallen/tableaux` test to be
cataloged and then incorporated directly, translated into a Proflog analog, or
marked unsupported. The implementation catalogs the names, but many entries
marked `:direct` or `:source-confirm` are not faithful translations of the
upstream tests.

Concrete examples:

- Upstream `test_contradiction_complex` is `(p -> q) and p and not q`; Proflog
  maps it to `p and q and not p and not q`.
  - Upstream: `/tmp/tableaux-review/tests/test_comprehensive.py`, lines 68-78.
  - Mapping: `src/proflog/literature_tableau_golden.clj`, lines 49-53.
- Upstream `test_tautology_transitivity` proves the negation of
  `((a -> b) and (b -> c)) -> (a -> c)` closes. Proflog maps it to
  `(p and q) -> p` and expects `:open`.
  - Upstream: `/tmp/tableaux-review/tests/test_comprehensive.py`, lines 92-104.
  - Mapping: `src/proflog/literature_tableau_golden.clj`, lines 55-57.
- Upstream `test_tautology_material_implication` proves both directions of
  `(p -> q) <-> (not p or q)` as tautologies. Proflog maps it to bare `p -> q`.
  - Upstream: `/tmp/tableaux-review/tests/test_comprehensive.py`, lines 106-121.
  - Mapping: `src/proflog/literature_tableau_golden.clj`, lines 58-62.
- Upstream `test_smullyan_completeness_example` is a satisfiable disjunction
  with model extraction. Proflog maps it to `p and not p` and expects closure.
  - Upstream: `/tmp/tableaux-review/tests/test_literature_examples.py`,
    lines 350-390.
  - Mapping: `src/proflog/literature_tableau_golden.clj`, lines 459-468.

The current tests do not catch this because they assert only that each upstream
test name appears and that Proflog matches the implementation's own cataloged
expectation:

- `test/proflog/literature_tableau_golden_test.clj`, lines 84-94;
- `test/proflog/literature_tableau_golden_test.clj`, lines 109-116.

Required correction:

- Add an explicit upstream-to-Proflog translation table that records the actual
  upstream formula or condition and the actual Proflog formula or condition.
- For `:direct`, require formula/condition equivalence, not merely same test
  name.
- For `:analog`, state exactly which upstream assertions are preserved and
  which are intentionally dropped.
- Downgrade entries that are only broad smoke analogs from `:direct` or
  `:source-confirm` to `:analog` or `:unsupported`.

### 2. High: ADR-0112 Source Confirmation Is Not Actually Discharged

ADR-0112 requires independent confirmation using Proflog and reconciliation
among:

- upstream `tableaux` result;
- external/literature expected result;
- Proflog observed result.

The implementation uses generic `source-note` strings such as "Melvin Fitting,
First-Order Logic and Automated Theorem Proving" and "Raymond M. Smullyan,
First-Order Logic", but does not record source-specific formulas, page/chapter
locations sufficient for verification, or source-expected results.

The reconciliation ledger has only four broad rows:

- `docs/planning/proflog-tableau-improvements/reconciliation-ledger.md`,
  lines 8-13;
- machine-readable copy in `src/proflog/literature_tableau_golden.clj`,
  lines 616-649.

Required correction:

- For every `:source-confirm` entry, record the source formula or rule shape,
  source expectation, source location, Proflog formula, and Proflog observation.
- If a source cannot be verified, mark the entry `:deferred` or `:analog`, not
  `:source-confirm`.
- Expand the reconciliation ledger to include every mismatch introduced by
  translation, unsupported semantics, changed model-extraction assertions, or
  omitted branch-shape assertion.

### 3. High: ADR-0115 Does Not Measure Scheduling or Branch Growth for Open Cases

`proflog.scheduling-benchmarks/measure-benchmark` measures proof steps only
when `kernel/prove` returns a closing proof. Open cases are assigned a
`step-count` of `0` by construction:

- `src/proflog/scheduling_benchmarks.clj`, lines 90-102.

The AAR confirms that the interesting open and nondeterministic cases record
zero steps:

- `docs/aar/AAR-0115-proflog-proof-preserving-scheduling-benchmarks.md`,
  lines 17-26.

This does not test scheduling, branch growth, nondeterministic expansion, or
open-branch search cost. It only tests "no proof was found within `kernel/prove
... 1`" and then assigns no cost.

Required correction:

- Add instrumentation that can observe search attempts, generated branches,
  selected pending goals, or explored states for open as well as closed cases.
- If existing kernel APIs do not expose this, ADR-0115 should be narrowed to
  closed-proof step counts and left incomplete for open-branch scheduling.
- Add at least one regression that would fail if a future scheduler explores
  substantially more branches but still leaves the formula open.

### 4. Medium: Slow Tests Are Wired Into the Fast Alias

The new namespaces contain `^:slow` tests:

- `test/proflog/literature_tableau_golden_test.clj`, line 118;
- `test/proflog/scheduling_benchmark_test.clj`, line 39.

But `lein test-proflog-fast` includes the whole namespaces:

- `project.clj`, lines 87 and 91.

Leiningen's namespace-style alias does not exclude `^:slow` vars. This violates
the project fast/extended split. The current tests are short, but the wiring is
wrong and will become a problem as the suites grow.

Required correction:

- Split fast and extended tests into separate namespaces, or introduce aliases
  that select `:not-slow` and `:slow` reliably.
- Keep ADR-0112 branch-growth/source-confirmation slow probes out of
  `lein test-proflog-fast`.
- Keep ADR-0115 extended scheduling benchmarks out of the fast alias.

### 5. Medium: Completion Records Are Future-Dated

Today is 2026-06-17. The completion log and AARs are dated 2026-06-20:

- `LOG.md`, lines 23-41;
- `docs/aar/AAR-0112-proflog-literature-tableau-golden-suite.md`, line 3;
- `docs/aar/AAR-0113-proflog-proof-object-diagnostic-renderer.md`, line 3;
- `docs/aar/AAR-0114-proflog-open-branch-witness-extraction.md`, line 3;
- `docs/aar/AAR-0115-proflog-proof-preserving-scheduling-benchmarks.md`, line 3.

Required correction:

- Correct dates to the actual completion dates, or if those commits were
  authored elsewhere with future-dated context, add an explicit note explaining
  why the chronology is non-standard.

### 6. Low/Medium: Proof-Trace Closure Labels Are Partly Wrong

`refl-close` is produced by the kernel for negative equality closure when the
walked terms are already identical:

- `src/proflog/kernel.clj`, lines 1119-1129.

The proof-trace renderer labels `refl-close` as `:procedure-refutation`:

- `src/proflog/diagnostics/proof_trace.clj`, lines 30-36.

Required correction:

- Rename that diagnostic reason to something equality-specific, such as
  `:reflexive-disequality-contradiction`.
- Add a focused test whose proof contains `refl-close` and asserts the corrected
  label.

## What Is Corroborated

- The newly added focused namespaces currently pass.
- The broad fast and extended gates currently pass.
- ADR-0113's read-only renderer is small and isolated.
- ADR-0114's witness extractor is conservative for flat conjunctions of nullary
  positive/negative literals and explicit literal maps.
- The work is profile-transparent in language and mostly avoids tying these
  features to any single Proflog profile.

## Recommended Rework Plan

1. Reopen or supersede ADR-0112.

   Build a real upstream translation matrix before changing more tests. For
   each upstream active item, store:

   - upstream file/test;
   - upstream formula(s), sign(s), and non-formula assertions;
   - upstream expected result;
   - Proflog translated formula(s) or explicit unsupported reason;
   - Proflog observed result;
   - retained assertions and dropped assertions.

2. Repair the ADR-0112 tests.

   Add tests that fail when a `:direct` or `:source-confirm` row lacks an
   explicit translation record. Add spot checks for known upstream formulas:
   contradiction complex, transitivity tautology, material implication
   equivalence, Fitting satisfiable example, and Smullyan completeness.

3. Reopen or narrow ADR-0115.

   Either implement real search/branch instrumentation for open cases, or
   clearly narrow the ADR to closed-proof step-count baselines. Do not claim
   scheduling or branch-growth coverage for open formulas while recording
   `0` steps by construction.

4. Fix fast/extended wiring.

   Split the two mixed namespaces or repair aliases so slow vars are not on the
   fast path.

5. Polish diagnostics.

   Correct `refl-close` labeling and add a targeted test.

6. Correct chronology.

   Repair the future-dated AAR and `LOG.md` entries or add an explanation.

## Suggested Status Updates

Until the above is complete, I recommend:

- ADR-0112: `accepted` or `in progress`, not complete.
- AAR-0112: record current work as scaffold plus incomplete fidelity audit.
- ADR-0115: `accepted` or `in progress`, not complete.
- AAR-0115: record current work as closed-proof and semantic-baseline scaffold,
  not full scheduling benchmark coverage.
- ADR-0113/0114: may remain complete after the diagnostic polish items are
  addressed or explicitly logged as follow-up.
