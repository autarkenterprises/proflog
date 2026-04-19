# Memory

## Date

2026-04-18

## Branch / Mission Context

- Repository: `proflog`
- Active branch: `adr-0007-nim-correctness-query-bounds`
- Active ADR: `docs/adr/ADR-0007-nim-correctness-and-query-bounds.md`
- User priority on this branch remains:
  - semantic correctness over operational neatness
  - long-running proving is acceptable
  - incorrect proving is not acceptable

## Latest Pushed Commits

- `3fd9566` `Checkpoint ADR-0007 query remediation baseline`
- `2988f4e` `Add recursive parity ground regression tests`
- `12da63d` `Add recursive parity witness synthesis tests`
- `8f927a2` `Split slow recursive regressions into an extended suite`
- `ea13afe` `Add extended Nim synthesis regression tests`

## Current State

- The ADR-0007 kernel/equality/query remediation is still in place.
- Ground semantic checks for Fitting `P1`/`P2` are still present, but the slower recursive probes have been moved out of the fast path.
- There is now an explicit split between:
  - fast greenfield regression coverage
  - extended recursive / synthesis / operational regression coverage
- `development-practices.md` is now tracked and records the intended workflow:
  - use `lein test-proflog-fast` for the normal greenfield loop
  - use `lein test-proflog-extended` for deeper recursive and synthesis checks
  - run them in parallel during active semantic work
  - only block on the extended suite after major revisions
  - prefer the Clojure MCP + nREPL for semantic probing

## Files Touched In This Follow-On Round

- `project.clj`
- `development-practices.md`
- `test/proflog/query_test.clj`
- `test/proflog/query_extended_test.clj`
- `test/proflog/recursive_synthesis_test.clj`
- `test/proflog/nim_synthesis_test.clj`
- `MEMORY.md`

## What Changed

### 1. Fast vs extended suite split

`project.clj`

- Added alias `lein test-proflog-fast`
- Added alias `lein test-proflog-extended`
- The fast alias runs the greenfield core namespaces:
  - `proflog.ast-test`
  - `proflog.language-test`
  - `proflog.normalize-test`
  - `proflog.subst-test`
  - `proflog.kernel-test`
  - `proflog.proof-test`
  - `proflog.equality-test`
  - `proflog.oracle.herbrand-test`
  - `proflog.program-test`
  - `proflog.query-test`
- The extended alias runs:
  - `proflog.query-extended-test`
  - `proflog.recursive-synthesis-test`
  - `proflog.nim-synthesis-test`

### 2. Slow recursive parity coverage moved out of `query_test`

`test/proflog/query_test.clj`

- `query_test` is now the fast semantic core only:
  - `query-status` operational checks
  - `P1` direct checks `even(0)` and `odd(1)`
  - `P2` fast ground checks `win(0)`, `win(1)`, `win(2)`, and separate `win(3)` failure
- The heavier recursive parity checks were moved out:
  - higher ground parity success/failure coverage now lives in `recursive_synthesis_test`

### 3. Operational bounded-query regression moved to extended

`test/proflog/query_extended_test.clj`

- The old future/deref wall-clock assertion from `query_test` was too load-sensitive under concurrent heavy runs.
- The extended version now checks the actual contract:
  - `query/query-succeeds-within` eventually returns `()` after budget exhaustion
  - it no longer asserts a sub-second wall-clock threshold under load
- This matches the ADR-0007 contract: bounded helpers are operational, not hard real-time.

### 4. Extended parity suite now covers both ground depth and witness synthesis

`test/proflog/recursive_synthesis_test.clj`

- Retains positive witness enumeration for:
  - `even(x)` witnesses `0`, `2`
  - `odd(x)` witnesses `1`, `3`
- Also now contains the higher recursive ground checks moved out of the fast suite:
  - succeeds: `even(2)`, `odd(3)`, `even(4)`
  - fails: `odd(0)`, `even(1)`, `odd(2)`, `even(3)`
- Important semantic/operational note:
  - these are still witness-based and ground-based tests
  - unrestricted open-answer generation remains too operationally unstable for committed greenfield regressions

### 5. Extended Nim suite added

`test/proflog/nim_synthesis_test.clj`

- Added constrained winning-move generation coverage using witness formulas.
- Positive move witnesses:
  - from `1` to `0`
  - from `2` to `0`
  - from `4` to `3`
  - from `5` to `3`
- Negative move witnesses:
  - `1 -> 1` fails
  - `4 -> 2` fails
- Added deeper ground Nim checks in extended coverage:
  - `win(4)` succeeds
  - `win(5)` succeeds with explicit fuel `16`

## Verification Performed

Clean final verification was done after killing stale local `lein test` JVMs from earlier workflow experiments:

```bash
pkill -f "lein test"
```

Then I ran the intended workflow commands.

Fast suite:

```bash
lein test-proflog-fast
```

Observed result:

- `Ran 34 tests containing 110 assertions.`
- `0 failures, 0 errors.`

Extended suite:

```bash
lein test-proflog-extended
```

Observed result:

- `Ran 8 tests containing 18 assertions.`
- `0 failures, 0 errors.`

I also validated key expensive namespaces through the Clojure MCP / nREPL path:

- `proflog.recursive-synthesis-test`
  - `Ran 2 tests containing 2 assertions.`
  - before the later parity-ground migration, witness enumeration took about `19s`
- `proflog.nim-synthesis-test`
  - `Ran 2 tests containing 6 assertions.`
  - before the later deep-ground addition, the namespace took about `52s` of prover time
- Direct MCP probe:
  - `query/query-succeeds-within` on `win(0)` with timeout `25` returned `()` in about `79ms` on an unloaded REPL

## Practical REPL Guidance

- The user explicitly reminded that the Clojure MCP and REPL are available; use them.
- Use `list_nrepl_ports` first rather than assuming the port.
- Use `clojure_eval` for:
  - targeted semantic probes
  - timing one expensive relation or test var
  - confirming whether a slowdown is semantic or just `lein` process buildup
- Prefer MCP/nREPL over shell one-offs for this repository unless the MCP path is insufficient.

Useful pattern:

```clojure
(require '[clojure.test :as t]
         '[proflog.query-test :as qt] :reload)
(time (t/test-vars [#'qt/fitting-p1-odd-one-succeeds]))
```

## Important Findings / Limits

### 1. Greenfield free-answer generation is still limited

- The greenfield prover can support committed recursive/synthesis regression tests when the witness set is constrained.
- It is not yet stable enough for unrestricted open-answer enumeration regressions such as:
  - blind `even(x)` generation over mixed candidate classes
  - blind `odd(x)` generation
  - blind Nim move enumeration over candidate ranges

That is why the committed extended tests use:

- positive witness enumeration for expected answers
- selected negative witness refutations
- deeper ground checks where witness search is too unstable

### 2. Bounded query helpers are operational only

This remains the main query-boundary caveat:

- `query-succeeds-within`
- `query-fails-within`
- `query-status`

all use finite fuel slices and may overshoot the nominal wall-clock budget while finishing the last admitted slice.

Do not reintroduce hard wall-clock assumptions into the fast suite.

### 3. Concurrent verification is the intended workflow now

- Running fast and extended concurrently is useful and was validated.
- If timings suddenly look much worse than expected, check for stale parallel `lein test` JVMs before concluding there is a semantic regression.

## Pre-Existing Dirty State I Did Not Revert

These were already dirty and were left alone:

- `docs/SEMANTIC_VARIANTS.md`
- `docs/adr/README.md`
- `src/cljtap/alphaleantap_ep.clj`
- `test/cljtap/alphaleantap_ep_test.clj`
- several untracked local artifacts and scratch files such as:
  - `.nrepl-port`
  - `.lein-failures`
  - `.lein-repl-history`
  - `target/`
  - `META-INF/`
  - `debug_gv04*.clj`
  - `deep-research-report*.md`
  - `cljs/`
  - `clojure/`

## Likely Next Work

1. Extend the greenfield semantic envelope beyond the current constrained witness checks:
   - more parity witnesses
   - more Nim positions
   - possibly selected partial-synthesis cases beyond the current witness forms
2. Decide whether `win(6)` / `win(7)` / `win(8)` belong in the extended suite or in ad hoc REPL validation only.
3. If true open-answer generation becomes a requirement, add a dedicated answer-generation harness instead of overloading the current semidecision-oriented query helpers.
