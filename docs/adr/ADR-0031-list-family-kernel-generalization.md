# ADR-0031: List-Family Kernel Generalization

- Status: accepted
- Date: 2026-04-29
- Branch: `adr-0031-list-family-kernel-generalization`
- AAR: pending
- Depends On: [ADR-0030](ADR-0030-relational-constructor-search.md)

## Context

ADR-0030 is technically complete: it made the originally selected raw
two-step list proofs close through the ordinary kernel without list-specific
production code. The post-completion raw matrix shows that this is not enough
to satisfy the family-level objective.

The matrix in
[List Kernel Test Matrix](../log/2026-04-29-list-kernel-test-matrix.md)
distinguishes three layers:

- the ordinary program kernel;
- the answer overlay's raw symbolic answer path;
- public `query-answers`, which can materialize known list-family answers
  above the kernel.

The central finding is that ADR-0030 improved only a narrow ground closure
slice:

- two-step flat and nested ground `append/3` pass;
- two-element flat and nested ground `reverse/2` pass;
- length-three outer-list append and reverse time out under a 90 second
  process budget;
- raw answer-mode output, input, inverse, and partial rows do not produce the
  intended closed targets within the tested bounds.

This means the spirit of ADR-0030 is not complete. Once the kernel can reverse
a two-element list, it should in principle be able to reverse an arbitrary
proper list: a longer list is still only a finite chain of constructor pairs.
Runtime may grow, but that growth should be measurable, explainable, and
attached to a clear complexity cause rather than hidden behind an accidental
timeout.

## Decision

Revisit constructor-recursive list-family support as a new feature branch.
The goal is not to add more list materialization. The goal is to make the
central prover and raw answer path scale over the family represented by the
examples.

The implementation must remain generic over constructor-recursive definite
programs. Production kernel code must not name `append`, `reverse`, `cons`,
or `null`.

One explicit design premise is that a multi-layer Proflog implementation should
make source-to-intermediate-representation transformations useful. A source
program may be lowered into a prover-facing representation that exposes guards,
recursive calls, residual formulas, demand, and layer-eligible subproblems. That
IR must preserve relational meaning, including reverse and partial synthesis
modes, but it does not need to mirror the surface program's syntax when a more
structured kernel-facing form is easier to evaluate.

## Required Capabilities

### 1. Family-Parametric Matrix

Replace one-off examples with a size-parametric matrix. The matrix should
cover at least:

- `append(left, right, whole)` ground forward mode for outer-list lengths
  0 through a documented bound;
- `reverse(input, output)` ground forward mode for the same bound;
- output synthesis, such as `append(left, right, z)` and `reverse(input, r)`;
- input synthesis, such as `reverse(r, output)`;
- partial synthesis, such as suffix, prefix, inverse split, and output-tail
  queries;
- flat elements and nested-list elements.

The matrix should run in two forms:

- a focused CI-safe selector with modest size bounds;
- a long-running diagnostic probe with larger bounds and timing/proof-shape
  output.

### 2. Raw Kernel Boundary

Every matrix row must be able to state which layer found the answer:

- ordinary ground proof kernel;
- raw answer overlay;
- public materializer.

For this ADR, the primary exit criteria concern the ordinary kernel and raw
answer overlay. Public list materialization may remain as a compatibility
surface, but it must not be credited as kernel success.

### 3. Generic Constructor-Recursive Descent

The kernel needs a generic way to prefer structurally descending recursive
calls after constructor guards have been saturated. ADR-0030's negative
alternative focusing was too narrow because it helped one short ground shape
but did not give the prover a durable recursive search discipline.

Candidate mechanisms include:

- guard saturation before recursive calls in both positive and negative call
  paths;
- a pure structural descent metric over walked constructor arguments;
- call-state summaries that can recognize proper-subterm descent without
  depending on relation or constructor names;
- tabling or duplicate-state suppression keyed by canonical branch state and
  descent depth;
- answer-mode continuation rules that keep descending toward a requested
  closed target before exporting a residual call frontier.

Any retained mechanism must be relational where it touches the kernel-facing
path. Host-side instrumentation is acceptable only for diagnostics and
complexity reporting outside the proof relation.

### 4. Complexity Accounting

Longer lists do not need to be constant time. They do need auditable growth.
The AAR must report, for each promoted size:

- wall-clock time;
- fuel/call-depth/raw-limit settings;
- proof-step signatures or representative proof tags;
- whether growth appears linear, polynomial, exponential, or dominated by a
  specific branching source;
- which layer answered the query.

If a row remains impractical, the AAR must identify the blocker in proof-search
terms rather than merely recording that it timed out.

## Implementation Order

1. Promote the existing list matrix into a size-parametric diagnostic surface
   with explicit layer, timing, fuel, and proof-shape reporting.
2. Compile generic guarded-clause IR from source programs. This IR must
   partition each top-level alternative into equality/disequality guards,
   procedure calls, and residual formulas without relation-specific or
   constructor-specific recognition.
3. Expose the guarded IR through relational program lookup while preserving the
   historical ordinary clause view.
4. Use the guarded IR to drive guard-first recursive descent in positive,
   negative, and raw-answer call paths.
5. Add canonical duplicate-state suppression or tabling only if the matrix shows
   repeated states still dominate after guard-first descent.
6. Re-run the matrix and write the AAR against family-level behavior rather
   than selected short examples.

## Progress Notes

- The guarded IR now retains source call order and a second constructor-demand
  call order. The answer overlay selects source order when the first source
  call already has walked constructor demand, and otherwise may use the demand
  order to constrain inverse modes earlier.
- The CI-safe matrix has been promoted to include raw `reverse(r, [b,a])`.
  Longer reverse output/output-tail rows and full flat inverse split
  enumeration still require more search-control work before the ADR can exit.

## Constraints

- Preserve kernel purity. No new executable `core.logic/project` may enter
  `src/proflog/kernel.clj`, `src/proflog/kernel_support.clj`,
  `src/proflog/subst.clj`, or the ordinary proof-facing path.
- Preserve reverse and partial synthesis abilities. The implementation must
  not trade away open or partially instantiated query behavior to improve
  forward ground proofs.
- Preserve Pelletier layer interoperation from ADR-0026.
- Keep public list-family materialization clearly separated from kernel
  success accounting.
- Do not add relation-specific production dispatch for `append` or `reverse`.

## Test Obligations

- Promote the raw matrix probe into a documented test/probe family with
  parameterized size bounds.
- Add CI-safe tests for:
  - forward ground `append` and `reverse` beyond the ADR-0030 two-step cases;
  - nested-list elements at the same outer-list sizes;
  - output, input, inverse, and partial answer rows through the raw answer
    path.
- Add diagnostic probes for larger bounds that emit timing and proof-shape
  summaries.
- Keep these existing regressions green:
  - `lein test-proflog-fast`
  - `lein test-proflog-constructor-recursive`
  - `lein test proflog.list-kernel-matrix-test`
  - `lein test proflog.subst-test proflog.kernel-test proflog.reverse-program-synthesis-test`
  - `lein test proflog.kernel.dispatch-test proflog.pelletier-layering-test proflog.proof-test`

## Exit Criteria

- The CI-safe matrix demonstrates family generalization beyond ADR-0030's
  narrow examples.
- The raw answer path produces closed targets for representative reverse and
  partial synthesis rows without relying on public list materialization.
- The long-running diagnostic matrix records complexity growth and identifies
  any remaining impractical rows by proof-search cause.
- The implementation remains generic and projection-free in the kernel-facing
  path.
- An AAR records whether the spirit of ADR-0030 has been satisfied or whether
  a further architectural change is required.
