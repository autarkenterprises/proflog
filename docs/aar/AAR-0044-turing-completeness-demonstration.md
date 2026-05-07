# AAR-0044: Turing Completeness Demonstration

- Date: 2026-05-07
- Related ADR: [ADR-0044](../adr/ADR-0044-turing-completeness-demonstration.md)
- Status: completed
- Branch: `adr-0044-turing-completeness`

## Result

ADR-0044 added `proflog.turing-completeness`, a reusable two-counter Minsky
machine interpreter written through the ADR-0010 frontend. The namespace does
not add a host-side machine evaluator. Clojure helpers construct
object-language terms for tests and examples, while transition and reachability
semantics are ordinary compiled Proflog clauses.

The implementation demonstrates the standard expressive-power argument:
two-counter machines are Turing-complete, a finite instruction table can be
encoded as Proflog facts, and the generic `run/2` relation is the reflexive
transitive closure of `step/2`. Therefore Proflog can represent computations of
a known minimal Turing-complete model.

## Verification

The opt-in suite is `lein test-proflog-turing-completeness`. It is intentionally
not part of the default fast or extended gates because the proof searches are
slow enough that future users should choose when to run them.

Promoted checks cover:

- forward kernel proofs for increment, zero-branch, and decrement-branch
  transition cases;
- bounded recursive execution for a second instruction table, proving the
  interpreter is not hard-coded to the transfer example;
- frontend answer evaluation that exports the final halting configuration for a
  three-step transfer run;
- frontend partial synthesis over an instruction-table relation;
- a source audit that the TC namespace does not call query or answer evaluators
  and does not define a host `step`, `run`, or `halts-in` function.

Runtime details are recorded in
[TEST_RUNTIME_BASELINE](../TEST_RUNTIME_BASELINE.md).

Final focused result:

```text
lein test-proflog-turing-completeness
Ran 5 tests containing 12 assertions.
0 failures, 0 errors.
elapsed_seconds 94.45
```

Standard gate result:

```text
lein test-proflog-fast
Ran 128 tests containing 414 assertions.
0 failures, 0 errors.
elapsed_seconds 85.62

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed_seconds 227.20
```

## Shortcomings

The demonstration is definitive about representability, not about efficient
reachability. Direct open predecessor synthesis over `step/2` timed out inside a
180s wrapper, and recursive multi-step transfer proofs through `halts-in-steps`
timed out inside 180s wrappers for the sampled transfer cases. The passing
suite therefore uses individual `step/2` proofs, one bounded recursive
incrementer run, and an explicit three-step transfer trace for answer export.

This is the correct operational reading: Proflog can encode a Turing-complete
machine model in its kernel-level source language, but arbitrary machine
reachability remains undecidable and current bounded proof search can be
expensive even for small recursive traces.
