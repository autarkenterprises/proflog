# Memory

## Date

2026-04-18

## Branch / Mission Context

- Repository: `proflog`
- Active branch: `adr-0008-test-gap-closure`
- Active ADR: `docs/adr/ADR-0008-test-gap-closure.md`
- User priority on this branch remains:
  - semantic correctness over operational neatness
  - long-running proving is acceptable
  - incorrect proving is not acceptable

## Current ADR-0008 State

- Added `docs/TEST_GAP_CLOSURE_CHECKLIST.md` as the tracked greenfield parity
  and gap-closure checklist.
- Added `docs/adr/ADR-0008-test-gap-closure.md` and registered it in:
  - `docs/adr/README.md`
  - `docs/EXECUTION_PLAN.md`
- Added `test/proflog/reverse_program_synthesis_test.clj`.
- Added `proflog.reverse-program-synthesis-test` to
  `lein test-proflog-extended`.
- Existing branch work in `test/proflog/synthesis_modes_test.clj` is now being
  treated as part of ADR-0008 gap closure rather than as unrelated residue.
  It already expands non-trivial greenfield coverage for structured recursive
  reverse/partial answer export via `plus` and `append`.
- Current feasibility finding:
  - internal compiled-program synthesis is feasible under a fixed clause shape
  - this is now covered by committed greenfield tests
  - full surface-program synthesis is still unresolved because
    `language/compile-program` is one-way and the internal compiled form does
    not relationally enforce `body`/`negated-body` coherence

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
- Direct JVM probe with a `15` minute ceiling:
  - `win(6)` failure was confirmed on `2026-04-20`
  - measured elapsed time was `522.93` seconds, about `8m 43s`
  - this confirms the semantics extend to `win(6)` but also shows that deeper negative Nim cases remain too slow for the normal regression path
- Additional direct JVM probes on `2026-04-20`:
  - `win(7)` success was confirmed in `198.85` seconds, about `3m 19s`
  - `win(8)` success was confirmed in `472.40` seconds, about `7m 52s`
  - free-variable answer generation for `win(x)` was attempted through `kernel/prove-programo` because the public query API does not return substitutions
  - `run 6 [x] ...` with fuel `16` timed out after `300.01` seconds with no answers
  - `run 1 [x] ...` with fuel `16` timed out after `120.01` seconds with no first answer
  - on the user's request, the same `run 1 [x] ...` probe was then allowed to run for the full `15` minute ceiling
  - it still produced no first answer and timed out after `900.01` seconds

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

## 2026-04-21 ADR-0009 Program-Family Closure Kickoff

- Active branch is now `adr-0009-legacy-program-closure`.
- Active ADR is `docs/adr/ADR-0009-legacy-program-closure.md`.
- This branch starts from the ADR-0008 parity/gap-closure state and turns the
  remaining legacy comparison into:
  - a maintained program-family parity matrix,
  - worked examples for extant greenfield families,
  - deeper closure for families already present but still weaker than legacy,
  - and implementation of currently absent legacy families where justified.
- New parity tracker:
  - `docs/LEGACY_PROGRAM_PARITY_MATRIX.md`
- Immediate execution order recorded in ADR-0009:
  1. worked examples for extant greenfield families,
  2. closure of present-but-not-comparable families,
  3. implementation of absent families in mission-relevant order.
- The user explicitly wants commit/push boundaries after each logical unit of
  work, and semantic/performance findings recorded as they appear.

## 2026-04-21 ADR-0009 Phase 1 Worked Examples

- Added worked-example coverage for the current greenfield family/query/
  synthesis namespaces:
  - `worked-examples/README.md`
  - `worked-examples/query-and-program-behavior.md`
  - `worked-examples/reverse-program-synthesis.md`
  - `worked-examples/integration-families.md`
  - `worked-examples/list-programs.md`
  - `worked-examples/quantified-programs.md`
  - `worked-examples/answers-api.md`
  - `worked-examples/recursive-parity.md`
  - `worked-examples/nim-synthesis.md`
  - `worked-examples/synthesis-modes.md`
- Existing `worked-examples/boxed-zero.md` remains the detailed mixed-quantifier
  example and is now linked from the worked-example index.
- The worked examples were grounded in live headless nREPL probes rather than
  just restating test names. Extracted artifacts included:
  - query statuses for `p/1` and `undef/1`
  - proof terms for direct procedure calls, `P1`, `P2`, direct `tc`, base
    `plus`, `zero-only`, and `boxed-zero`
  - exported answer records for open `append`, `even`, `win`, `step`, `jump`,
    `down`, `plus`, and `append`
- A temporary headless REPL was started for these probes on `2026-04-21`; shut
  it down after this logical unit is committed.

## 2026-04-21 Resume Findings

- The interrupted follow-on round had already added new ADR-0008 namespaces for
  list programs, quantified programs, and broader integration families, and
  wired them into `lein test-proflog-fast` / `lein test-proflog-extended`.
- Direct resume validation showed those first drafts were too ambitious as
  committed regressions:
  - `member(a, [a])` and `member(c, [a,b])` stayed `:unresolved` under a
    `1000ms` `query/query-status` budget, and the original direct
    `query/query-succeeds` tests did not complete within `30s`.
  - non-empty recursive list proofs such as `append([a],[b],[a,b])` and
    `reverse([a],[a])` were likewise not prompt enough for baseline regressions.
  - `tc(a,c)`, `plus(1,2,3)`, and mixed quantified `boxed-zero` style clause
    bodies were still operationally unresolved in the same prompt regime.
- Prompt greenfield boundaries confirmed through direct probing:
  - list programs:
    - `append([], [a], [a])` succeeds directly with fuel `8`
    - `reverse([], [])` succeeds directly with fuel `8`
    - `append([], [a], z)` exports `z = [a]` through `answers/query-answers`
  - integration families:
    - direct edges `tc(a,b)` and `tc(b,c)` succeed directly with fuel `8`
    - base-case Peano addition `plus(0,2,2)` succeeds directly with fuel `8`
  - quantified programs:
    - `zero-only(0)` succeeds and `zero-only(1)` fails directly with fuel `8`
    - original `P1` still directly refutes `odd(0)` with fuel `8`
- Branch conclusion for the resumed round:
  - keep those prompt cases as committed ADR-0008 regressions
  - treat deeper recursive `member` / non-empty list proofs, recursive
    transitive closure, non-base `plus`, and mixed `exists`/`forall`
    integration as still exploratory, and document them in the checklist
    rather than overstating them as baseline-green tests

## 2026-04-21 Quantified Executability Follow-On

- The user explicitly clarified the semantic bar:
  exploratory programs, especially quantified ones, must remain executable even
  if they are too slow or too large for the fast baseline suite.
- I treated the legacy `once-forall` device only as a reference pointer, not as
  authority. The greenfield justification is local and operational:
  negating an existential clause body for a procedure call should yield a
  single-use universal branch obligation, not an ordinary re-enqueued `forall`
  that destroys executability.
- Implemented a greenfield internal NNF form `once-forall` across:
  - `src/proflog/ast.clj`
  - `src/proflog/normalize.clj`
  - `src/proflog/language.clj`
  - `src/proflog/subst.clj`
  - `src/proflog/answers.clj`
  - `src/proflog/pretty.clj`
  - `src/proflog/kernel.clj`
- Added/updated greenfield tests for the new form in:
  - `test/proflog/ast_test.clj`
  - `test/proflog/normalize_test.clj`
  - `test/proflog/kernel_test.clj`
  - `test/proflog/proof_test.clj`
- Restored executable quantified exploratory coverage in
  `test/proflog/quantified_programs_test.clj`:
  - original `P1` deeper ground success/failure:
    - `even(2)` succeeds with fuel `32`
    - `odd(0)` fails with fuel `8`
  - mixed `exists`/`forall` clause body:
    - `boxed-zero(0)` succeeds with fuel `32`
    - `boxed-zero(1)` fails with fuel `32`
- Direct fresh nREPL probes after the change:
  - `P1` probe elapsed about `8.07s` total for `even(2)` success plus `odd(0)`
    failure checks
  - `boxed-zero` probe elapsed about `620.93ms` for success/failure checks
- Verification after the change:
  - `lein test proflog.quantified-programs-test`
    - `Ran 3 tests containing 6 assertions.`
    - `0 failures, 0 errors.`
  - `lein test proflog.query-test proflog.normalize-test proflog.kernel-test proflog.proof-test`
    - `Ran 25 tests containing 47 assertions.`
    - `0 failures, 0 errors.`
  - `lein test proflog.answers-test proflog.synthesis-modes-test proflog.recursive-synthesis-test proflog.query-extended-test`
    - `Ran 22 tests containing 42 assertions.`
    - `0 failures, 0 errors.`

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

### 4. `win(6)` is semantically confirmed, but operationally expensive

- A direct JVM probe confirmed that `win(6)` fails.
- The complete proof took about `8m 43s`.
- That is within the user's suggested `15` minute ceiling for a non-trivial Proflog program, so it is not yet evidence of outright failure to handle the example.
- It is still strong evidence that deeper recursive Nim evaluation needs optimization before cases like `win(6)` should be promoted into the committed extended suite.

### 5. `win(7)` and `win(8)` are semantically confirmed, but still too expensive for committed regression coverage

- Direct JVM probes confirmed that `win(7)` and `win(8)` succeed.
- Measured times were about `3m 19s` for `win(7)` and `7m 52s` for `win(8)`.
- This extends the greenfield semantic envelope through `win(8)`.
- It does not justify adding `win(7)` or `win(8)` to the committed extended suite yet; both remain in the REPL/JVM exploration tier for now.

### 6. Free-variable Nim answer generation is not currently operational

- The public `proflog.query` layer returns proofs only, not answer substitutions.
- A lower-level kernel probe using `run` plus `kernel/prove-programo` was used to test `win(x)` with `x` free.
- That probe produced no answers within:
  - `120.01` seconds for the first requested answer
  - `300.01` seconds for the first six requested answers
  - `900.01` seconds for a repeated first-answer probe run to the full `15` minute ceiling
- Current conclusion:
  - ground and constrained-witness Nim semantics extend through `win(8)`
  - open-answer generation for `win(x)` is still not operationally usable in the greenfield prover

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

1. Keep `win(6)`, `win(7)`, and `win(8)` recorded as confirmed REPL/JVM semantic probes, not committed extended regressions, unless the prover is optimized enough to reduce their runtimes materially.
2. Treat free-variable `win(x)` answer generation as a current implementation gap rather than a committed capability.
3. If deeper Nim positions are meant to become regular extended regressions, prioritize optimization work on recursive search before adding them to the suite.
4. If answer generation is required, design an explicit answer-oriented query layer or harness rather than trying to infer answers indirectly from proof-returning helpers.
