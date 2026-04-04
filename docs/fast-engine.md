# Fast Engine Architecture

Date: 2026-04-03
Status: initial implementation target

## Goal

Add a sibling execution engine for forward proof search that keeps the current Proflog surface syntax and tableau semantics, while replacing the current relation-driven control flow with explicit Clojure recursion and cached branch state.

The new engine is not intended to replace the reference prover immediately. It is intended to be:

- semantically aligned for forward proof search
- operationally explicit
- easier to instrument and optimize
- checked against the existing prover on bounded cases

## Architectural Correction

Forward proving alone is not a sufficient architecture for Proflog.

Reverse use and synthesis remain essential because partially specified
queries and programs are part of the language model. The correct structure
is therefore a dual engine:

- the fast engine for fully specified forward proof search
- the relational reference engine for symbolic use, reverse inference,
  and synthesis

That execution-layer dispatch lives in `cljtap.alphaleantap-ep-exec`.

The next layer of cooperation is branch-local cutover inside the relational
prover itself:

- outer search stays symbolic when the overall problem is partial
- once a current branch has an explicit executable shape, the residual branch
  is discharged by the fast engine
- the branch's incoming lemma thread is passed across the cutover boundary and
  the fast engine returns the outgoing lemma thread to the symbolic caller

This is implemented conservatively in `cljtap.alphaleantap-ep` via
`fast-cutovero`.

## Non-Goals For The Fast Engine Itself

The first version does not try to preserve the full relational character of `alphaleantap_ep.clj`.

In particular, it does not target:

- backward query generation
- sideways program synthesis
- exact proof-stream ordering parity with the reference prover

Those capabilities remain part of the overall architecture via the
relational engine, not part of the explicit fast engine.

## Core Design

### 1. Explicit DFS, not relational scheduling

The engine performs tableau search with direct recursion over formula structure.

This preserves the tableau rules but avoids paying core.logic scheduling overhead for:

- rule dispatch
- branch control
- queue manipulation
- repeated branch scans

### 2. Compiled branch state

Each branch carries explicit state:

- current substitution state
- current environment
- deque of unexpanded formulas
- saved literals
- incoming lemma thread
- cached indexes over those literals
- gamma budget

The literal cache separates:

- positive atoms by relation symbol
- negative atoms by relation symbol
- positive lemmas by relation symbol
- negative lemmas by relation symbol
- equalities
- disequalities
- derived equality pairs

This avoids recomputing branch-derived equality state every time a closure rule fires.

### 3. Use core.logic only as the unification substrate

The engine still uses low-level core.logic term machinery:

- `lvar`
- `unify`
- `walk*`
- occurs-checking substitutions
- nominal `Nom` / `Tie` structures

This keeps term semantics compatible with the current codebase without keeping the whole prover relational.

### 4. Procedure calls stay semantically isolated

A procedure call still starts a fresh subsidiary tableau with:

- empty unexpanded queue
- empty literal store
- a fresh parameter environment
- the same compiled program
- the caller's substitution state

This preserves the key soundness property from Fitting: subsidiary tableaux do not inherit the caller's branch formulas.

### 5. Cooperative cutover criterion

Top-level dispatch and branch-level dispatch use different criteria.

- top-level fast dispatch requires fully specified inputs
- branch-level cutover is weaker: raw logic variables may still appear in
  term positions as long as the formula/program shape is explicit

That matters for mixed-mode synthesis: the symbolic engine can determine the
structure of a clause body or subgoal, and the fast engine can then finish the
remaining first-order proof search over those term variables.

## Equality Strategy

The first implementation preserves the existing equality rule family, but makes it explicit and cached:

- free closure
- arity mismatch closure
- eq/neq complementary closure
- one-one decomposition
- paramodulated closure via branch equalities
- substitutivity for procedure calls
- eq-triggered closure/call variants

The important difference is operational:

- equalities are indexed once per branch state
- rewrite search runs on the cached equality pairs
- deterministic scans replace repeated relational reconstruction

## Search Semantics

### Quantifiers

- `forall`: instantiate with a fresh logic variable and re-enqueue the quantified formula at the back of the branch deque
- `once-forall`: instantiate once, no re-enqueue
- `exists`: instantiate with a fresh rigid parameter `(par p)`

### Beta

At a split, both branches start from the same structural branch state, but the right branch receives the substitution produced by the left branch.

The right branch also receives the left branch's outgoing lemma thread. This
matches the reference prover's `lem-in`/`lem-out` discipline rather than
treating lemmas as a global cache.

This matches the operational effect of the current prover, where branch closures may instantiate shared gamma variables.

## Planned Public Interface

The fast namespace exposes forward-oriented entry points parallel to the reference API:

- `prove-fast`
- `query-succeeds-fast`
- `query-fails-fast`
- `query-succeeds-id-fast`
- `query-fails-id-fast`

Each function should accept the same formula/program syntax as the current engine.

The unified execution namespace exposes:

- `prove`
- `query-succeeds`
- `query-fails`
- `query-succeeds-id`
- `query-fails-id`
- `reference-proveo`

`reference-proveo` is the explicit entry point for symbolic use inside `run`.

## Validation Strategy

Validation should use bounded cases only.

The first comparison set should cover:

- base closure
- quantifiers with gamma budget
- equality decomposition / paramodulation
- positive and negative procedure calls
- one representative GV case

The reference prover remains the oracle for those checks.
