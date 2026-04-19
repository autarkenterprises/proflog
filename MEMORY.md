# Memory

## Date

2026-04-18

## Branch / Mission Context

- Repository: `proflog`
- Active remediation track: `ADR-0007` (`docs/adr/ADR-0007-nim-correctness-and-query-bounds.md`)
- High-level goal: finish the greenfield Proflog kernel/query remediation without reintroducing normalize-order hacks.
- User priority on this branch:
  - semantic completeness matters more than bounded operational neatness
  - long-running but correct theorem proving is acceptable
  - inaccurate theorem proving is not acceptable

## State At Hand-Off

- The kernel/equality refactor from the prior turn is still in place:
  - explicit branch substitution state in `src/proflog/kernel.clj`
  - proof-variable-sensitive disequality closure
  - saved-call closure reopened by equality state
- The query layer is no longer in the broken `Thread.stop` state.
- `src/proflog/query.clj` now treats bounded queries as operational probes built from finite fuel slices.
- Semantic Nim checks in `test/proflog/query_test.clj` now use direct success/failure semidecision calls instead of bounded status races.
- The focused green suites are green again.

## Files Touched This Turn

- `src/proflog/query.clj`
- `test/proflog/query_test.clj`
- `test/proflog/program_test.clj`
- `docs/adr/ADR-0007-nim-correctness-and-query-bounds.md`
- `docs/EXECUTION_PLAN.md`
- `MEMORY.md`

## Pre-Existing Dirty State I Did Not Revert

These were already modified or otherwise dirty and were left alone:

- `docs/SEMANTIC_VARIANTS.md`
- `docs/adr/README.md`
- `src/cljtap/alphaleantap_ep.clj`
- `src/proflog/equality.clj`
- `src/proflog/kernel.clj`
- `test/cljtap/alphaleantap_ep_test.clj`
- several untracked scratch/reference files such as `debug_gv04*.clj`, `deep-research-report*.md`, `.nrepl-port`, `target/`, `META-INF/`, and related local artifacts

## Concrete Changes Made This Turn

### 1. Query boundary changed from thread-stopping to finite fuel slices

`src/proflog/query.clj`

- Removed the broken timeout approach that depended on daemon workers plus forced thread shutdown.
- Bounded helpers now use a conservative fuel schedule `0, 1, 2, 4, ...`.
- `query-succeeds-within` and `query-fails-within` repeatedly run finite-fuel proof searches until either:
  - a proof is found, or
  - the wall-clock budget is exhausted
- `query-status` now interleaves bounded success/failure probes instead of launching unbounded runaway workers.
- `query-status` short-circuits on a discovered success/failure path instead of always forcing both sides before returning.

Important caveat:

- This is an operationally bounded design, not a hard real-time timeout guarantee.
- A final admitted fuel slice may run past the nominal deadline before returning.
- That tradeoff is intentional for now because in-process core.logic search is not safely preemptible.

### 2. Query tests now separate semantics from bounded API behavior

`test/proflog/query_test.clj`

- Added direct helpers:
  - `succeeds-directly?`
  - `fails-directly?`
- Moved semantic P1/P2 checks onto direct proof search:
  - `even(0)` uses direct success search
  - `odd(1)` uses direct success search with explicit fuel `16`
  - Nim checks use direct success/failure semidecision proofs instead of `query-status`
- Current semantic coverage in that file:
  - losing: `win(0)`, `win(3)`
  - winning: `win(1)`, `win(2)`, `win(4)`
  - extra winning check: `win(5)` with explicit fuel `16`
- Kept the bounded regression:
  - `bounded-success-query-helper-returns-control-on-timeout`
- Kept the operational query-status regression, but with a larger timeout budget because the contract is now “bounded operational probe,” not “cold-process 200ms oracle.”

### 3. Program tests adjusted to the new bounded-query contract

`test/proflog/program_test.clj`

- Raised the `query-status` timeout budget used by the simple procedure-call status assertions from `200` to `1000`.
- This was needed because the status API is no longer being used as a microbenchmark target.

### 4. ADR / plan docs updated

- `docs/adr/ADR-0007-nim-correctness-and-query-bounds.md`
  - now explicitly distinguishes:
    - direct semidecision proof checks for semantic correctness
    - bounded query helpers for operational timeout behavior
- `docs/EXECUTION_PLAN.md`
  - now states that ADR-0007 semantic Nim coverage should not treat bounded query races as the semantic authority

## Verification Performed

This combined selector passes:

```bash
lein test proflog.query-test proflog.program-test proflog.kernel-test proflog.equality-test
```

Observed result:

- `Ran 23 tests containing 50 assertions.`
- `0 failures, 0 errors.`

I also re-ran targeted selectors while debugging:

- `lein test :only proflog.query-test/bounded-success-query-helper-returns-control-on-timeout`
- `lein test :only proflog.query-test/query-status-distinguishes-success-failure-and-unresolved`
- `lein test :only proflog.program-test/positive-and-negative-procedure-calls-close-literals`

## Useful Direct Findings

These shell probes were useful and are worth preserving:

- `query-succeeds` for `status-program` / `p(0)` succeeds at fuel `1`
- `query-status` had been returning `:unresolved` only because it was evaluating the expensive opposite side before checking the already-found success proof
- `win(5)` is semantically provable, but:
  - unbounded direct success search was about `9.3s` in a cold shell probe
  - explicit fuel `16` reduced that to about `3.0s`
- `win(6)` failure proof exists under direct search, but it was slow enough to omit from the committed regression suite for now:
  - direct unbounded `query-fails` shell probe was about `19.8s`

Those timings came from one-off shell entrypoints such as:

```bash
CP=$(lein classpath) && java -cp "$CP" clojure.main -e "..."
```

I did not rely on `clojure_eval` for this turn because the MCP tool call was blocked by safety handling in this conversation context.

## Open Problems / Next Likely Work

### 1. Broader semantic coverage is still incomplete

The current committed semantic suite is enough for ADR-0007’s stated `win(0..4)` obligations plus one extra `win(5)` check, but it is not the “deep semantic exercise” yet.

Likely next probes:

- Nim `win(6)`, `win(7)`, `win(8)` with explicit fuel where helpful
- additional P1 parity cases beyond `even(0)` / `odd(1)`
- possibly a dedicated semantic harness namespace separate from `query_test`

### 2. Bounded query helpers are operationally bounded, not strict wall-clock oracles

This is the biggest design caveat still on the branch.

- Current behavior is good enough for the tests now in-tree.
- It is not a true hard-timeout implementation.
- If a future requirement demands strict wall-clock cutoffs, the likely fix is not more in-process thread tricks.

Most plausible stricter options:

- subprocess isolation for bounded probes
- a dedicated worker JVM model
- or a more explicit “timeout is operational only” API contract

### 3. `query-status` inconsistency detection is best-effort

Current behavior:

- it checks the opposite semidecision side only after finding a proof on the first side and only while budget remains

That keeps the common success/failure cases fast enough, but it is not an exhaustive inconsistency detector under tight budgets.

## Practical Guidance For The Next Agent

1. Do not collapse the semantic tests back onto `query-status` or `query-succeeds-within`.
2. Treat bounded query helpers as operational tools only.
3. If you extend Nim coverage to larger positions, prefer explicit fuel when it materially reduces runtime.
4. Preserve the existing kernel/equality refactor unless you have a concrete counterexample; the current green suites depend on it.
5. Be careful with the already-dirty repo state. Do not revert unrelated files casually.
