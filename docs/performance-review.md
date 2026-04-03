# Performance Review

Date: 2026-04-03
Branch: `perf-lab-review`

## Scope

This note records an initial end-to-end review of the current Proflog implementation in `src/cljtap/alphaleantap_ep.clj`, with emphasis on where the performance ceiling appears to come from and what kind of experimental rewrite is justified.

## Immediate Evidence

Bounded probes were kept intentionally small to avoid stressing the machine.

- `timeout 20s lein test :only cljtap.gamma-budget-test/test-GB09-proc-call-with-budget` passed.
- `timeout 20s lein test :only cljtap.alphaleantap-ep-test/test-GV04-z2-assoc` timed out.
- `timeout 20s lein test :only cljtap.alphaleantap-ep-test/test-GV10-z2-7univ-assoc-neg-call` timed out.
- `timeout 20s lein test :only cljtap.alphaleantap-ep-test/test-GV13-z2-chained-assoc-neg-call` timed out.
- `timeout 20s lein test :only cljtap.alphaleantap-ep-test/test-GV17-z4-assoc-precomputed-neg-call` timed out.

The small control case passing while multiple GV cases still time out strongly suggests the main issue is not “the prover is globally broken”; it is search explosion plus repeated branch-local recomputation in the hard formulas.

## Main Findings

### 1. The worst blow-up is architectural, not just low-level

The finite-model encodings in the GV section are semantically faithful but operationally brutal.

- `gv-op-eq-inline` expands an operation lookup into one disjunct per table entry.
- `gv-neg-op-eq-inline` expands a negated lookup into one conjunct per table entry.
- `gv-assoc-program` stacks four such lookups under seven universals.

This means the prover is being asked to solve a large explicit search tree generated before it has any branch-local information to prune with. For the difficult GV cases, the dominant cost is the formula shape itself, not merely Clojure overhead.

Relevant code:

- `test/cljtap/alphaleantap_ep_test.clj`: `gv-op-eq-inline`
- `test/cljtap/alphaleantap_ep_test.clj`: `gv-neg-op-eq-inline`
- `test/cljtap/alphaleantap_ep_test.clj`: `gv-assoc-program`

### 2. Equality support is correct-looking but repeatedly reconstructive

The hot path repeatedly rebuilds equality-derived state from raw literals.

- `collect-eqso` walks the whole branch and derives both top-level and one-one pairs.
- That result is recomputed separately for `para-close`, `subst-call`, `neg-subst-call`, `eq-refl-close`, `para-free-close`, `eq-triggered-call`, `eq-triggered-neg-call`, and `eq-triggered-neq-close`.
- Each of the rewrite/closure relations (`eq-membero`, `eq-neq-closeo`, `para-free-closeo`) then performs its own search over the derived equality list.

This is the clearest implementation-level bottleneck in the current design. The same branch facts are being scanned and re-derived many times.

Relevant code:

- `src/cljtap/alphaleantap_ep.clj`: `collect-eqso`
- `src/cljtap/alphaleantap_ep.clj`: `eq-membero`
- `src/cljtap/alphaleantap_ep.clj`: `eq-neq-closeo`
- `src/cljtap/alphaleantap_ep.clj`: `para-free-closeo`
- `src/cljtap/alphaleantap_ep.clj`: `proveo` literal branches

### 3. `proveo` carries too little indexed branch state

`proveo` threads:

- current formula
- unexpanded stack
- literal list
- environment
- program
- proof term
- gamma budget
- lemma list

What it does not thread is any indexed or cached branch structure. As a result:

- complementary closure uses linear `membero` scans
- equality complement uses linear scans
- equality closures first scan `lits`, then scan derived equality pairs
- procedure calls perform relation lookup by linear search

This keeps the implementation declarative, but it makes the runtime pay the same branch-analysis cost over and over.

Relevant code:

- `src/cljtap/alphaleantap_ep.clj`: `proveo`
- `src/cljtap/alphaleantap_ep.clj`: `lookup-clauseo`

### 4. Some expensive operations are still represented relationally even when used deterministically

Several relations are semantically attractive but operationally expensive in the actual forward-running mode used by the tests:

- `lookup-clauseo`
- `bind-argso`
- `appendo` for gamma re-enqueue
- repeated `membero` scans over branch literals

This is not a soundness bug. It is a cost-model mismatch. In the performance-critical proving direction, these are effectively deterministic data-structure operations.

Relevant code:

- `src/cljtap/alphaleantap_ep.clj`: `lookup-clauseo`
- `src/cljtap/alphaleantap_ep.clj`: `bind-argso`
- `src/cljtap/alphaleantap_ep.clj`: gamma rule in `proveo`

### 5. Recent optimizations are sensible, but they are pruning a search tree that is still too implicit

The current branch already added several reasonable ideas:

- type-dispatched literal grouping
- gamma budgets and iterative deepening
- lemma reuse across beta branches
- depth-limited equality rewriting
- `eq-conflict-close`

These all help. None changes the more basic fact that the prover still rebuilds branch knowledge from lists and still explores finite-structure formulas through generic tableau branching rather than compiled finite-domain reasoning.

## Overall Assessment

The project does not look “merely under-optimized”. It looks like a correct reference-style relational prover that has been pushed into workloads which now demand explicit execution structure.

The performance wall appears at two levels:

1. formula design
2. prover state representation

The first major conclusion is that a clean experiment branch is justified. The second is that a full rewrite should not replace the current prover immediately; it should be developed beside it and checked against it.

## Recommended Direction

### Keep the current prover as the semantic reference

The current `alphaleantap_ep.clj` is valuable as the high-purity baseline. It should remain the oracle for small and medium cases even if a faster execution engine is built.

### Build an optimized forward prover beside it

The next serious experiment should be a second engine, not more local surgery inside the existing `proveo`.

Target properties:

- forward proof search only
- explicit branch state record
- cached complement indexes
- cached equality index
- compiled clause lookup map
- deterministic queue handling for gamma re-enqueue
- optional instrumentation counters

This can preserve semantics while relaxing the insistence that every internal step stay fully relational.

### Treat finite-structure checkers as a separate workload

The GV programs are exposing a genuine mismatch between “generic tableau relation” and “finite model checker”.

For finite algebra verification, the next experiment should consider compiling operation tables into a more direct form instead of encoding every lookup as a large disjunction inside the object language.

That is still faithful to Proflog at the semantic boundary if the compiled representation is only an execution strategy, not a change in the logic.

## Concrete Next Experiments

1. Introduce a branch-state cache layer for the current prover.
   Thread cached positive literals, negative literals, disequalities, and equality pairs so `collect-eqso` is not recomputed from scratch at every closure attempt.

2. Split program lookup into a prevalidated map at the top-level API.
   Keep the clause surface syntax unchanged, but compile `program` once before search.

3. Build a timeout-based probe harness for a small fixed benchmark set.
   Use only a handful of representative formulas and hard caps so regressions are visible without risking runaway compute.

4. Prototype a second prover namespace.
   Suggested name: `cljtap.alphaleantap-ep-fast`.
   Treat it as an execution engine checked against the current reference implementation on small cases.

## Bottom Line

The main bottleneck is not a single bad line. It is the combination of:

- large explicit tableau encodings for finite structures
- equality-heavy branch reasoning
- recomputation of branch-derived state
- using relational list processing where the runtime needs indexed execution

That is enough to justify a new experimental engine rather than continuing to tune the current one in place.
