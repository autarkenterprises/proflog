# ADR-0014: Generic Legacy Unsatisfied Family Evaluation

- Status: accepted
- Date: 2026-04-24
- Branch: `adr-0014-generic-legacy-evaluation`
- AAR: pending

## Context

ADR-0012 and ADR-0013 closed the legacy reverse / append answer surface, and
the inherited experimental probes now show behavior beyond the original legacy
scope as well. But that closure came with an important qualification:

- the public answer surface now reaches parity and beyond for those known list
  families,
- the raw kernel diagnostics underneath remain symbolic,
- and the completed answer surface depends on extra-kernel stream processing and
  known-family materialization rather than on pure kernel behavior alone.

That distinction matters for the next stage of the project.

In a relational system, execution is best understood as producing a
semidecidable answer stream. A correct answer may:

1. appear directly in the raw stream early,
2. appear only after substantial fair search,
3. already be present but obscured by equivalent or shallower frontiers and
   therefore require generic stream sifting above the kernel,
4. or fail to appear at all within the current relational engine.

Legacy development stalled on exactly this problem. The group-verifier and other
still-unsatisfied families did not clearly fail because of one isolated bug;
work instead drifted into undirected performance tuning without a disciplined
account of whether the desired answers were absent from the raw stream, merely
late, or recoverable only after extra-kernel interpretation.

The greenfield project now needs a dedicated branch for the legacy families that
remain unsatisfied, especially:

- the legacy group-verifier `GV` family,
- the finite-domain `FD` family,
- and any remaining legacy tests that still lack a greenfield determination.

This next branch should push as far as possible in a generic and purely
relational manner before authorizing new family-specific handlers.

## Decision

- Preserve an explicitly accessible pure-core proof surface. The core tableau
  prover should remain callable as directly as possible, while bounded `fuel`,
  bounded `call-depth`, raw proof collection limits, answer ranking, closed
  answer filtering, and any family-specific materializers remain documented
  overlays above that core rather than becoming part of the meaning of
  `prove`.
- Create a dedicated branch to evaluate the still-unsatisfied legacy families as
  answer-stream problems before adding new family-specific materializers.
- Treat each promoted legacy query at three explicitly documented layers:
  - raw kernel / generic symbolic answer stream,
  - generic stream sifting or post-processing above that stream,
  - specialty family handling only if the first two layers prove insufficient.
- Prefer generic relational methods and generic stream processing over
  data-structure-specific or theory-specific handlers.
- Make the layer distinction explicit in tests and docs. For every target query,
  record whether the desired answer is:
  - absent from the raw stream within measured bounds,
  - present but late,
  - recoverable by generic post-processing,
  - or only recoverable by specialty handling.
- Start with the hard legacy families that previously resisted closure:
  - promote representative `GV` tests first,
  - then at least one `FD` or comparably unsatisfied family,
  - and document any remaining unsatisfied set that is deferred within the
    branch after those first promotions.
- Keep slower exploratory probes out of the default and current extended
  selectors unless and until they are proven stable enough to belong there.

## Consequences

- The project gets a disciplined method for talking about hard legacy queries:
  not just "too slow" or "needs optimization," but which layer currently fails
  to expose the desired answer.
- This branch may show that some unsatisfied families are fundamentally search
  fairness or answer-stream ordering problems rather than semantic
  incompleteness.
- It may also show the opposite: that some legacy families, including the group
  verifier, expose a real need for new generic relational machinery such as
  memoization, tabling, better fairness, or principled domain bounding.
- The branch deliberately raises the bar for new specialty handlers. If a new
  family-specific evaluator is proposed, the repo should already know that:
  - the raw stream did not expose the answer within documented bounds, and
  - generic stream processing was not enough.
- Novel data-structure or theory support should not be inferred casually from
  one-off handlers. If the project begins to support new families this way, that
  becomes a language-design question and must be documented explicitly.
- The branch now has an additional architectural obligation: every operational
  deviation from the pure core should be nameable and configurable as an
  overlay. This makes it possible to ask two different questions cleanly:
  - what the core tableau prover itself can produce,
  - and what a bounded or answer-oriented overlay can recover above it.

## Initial Probe Boundary

The first ADR-14 raw-kernel list probes were run through
`proflog.legacy-stream-probe`, which bypasses `query-answers` and its list
 fast path and calls the raw kernel/export path directly.

Measured on `2026-04-24` with external `timeout -k 30s 900s` wall-clock bounds:

- `reverse([a,b], r)` with `fuel=nil` and `call-depth=1` exhausts by raw limit
  `8` (five raw proof states, four unique exported symbolic records) and never
  exports the closed witness `r = [b,a]`.
- `reverse([a,b], r)` with `fuel=nil` and `call-depth=2`, `3`, `4`, and `nil`
  reaches raw limits `1`, `2`, and `4` with no closed witness, then fails to
  complete the next raw slice within fifteen minutes.
- `append([a], [b,c], z)` with `fuel=nil` and `call-depth=nil` exhausts by raw
  limit `8` (six raw proof states, three unique exported records) and never
  exports the closed witness `z = [a,b,c]`.
- `append(x, y, [a,b,c])` with `fuel=nil` and `call-depth=nil` immediately
  exports the correct base split `x = [], y = [a,b,c]`, but by raw limit `8`
  it still has not exported the other three closed legacy splits, and the next
  raw slice did not complete within fifteen minutes.

So the current measured boundary is:

- the pure-core path is accessible for direct probing,
- unbounded `fuel` and unbounded `call-depth` are available there,
- but the raw kernel/export stream still does not produce full reverse or
  append synthesis parity within the measured long slices.

## Test Obligations

- Add a dedicated exploratory selector for legacy-unsatisfied-family probes, so
  long-running `GV` / `FD` work is isolated from the ordinary regression path.
- Promote at least one representative `GV` query into a greenfield exploratory
  namespace with an explicit expected answer surface and a failing or initially
  bounded regression.
- Promote at least one additional unsatisfied legacy query outside the list
  family, ideally from `FD`, under the same discipline.
- Add probe helpers or diagnostics tests that can distinguish:
  - raw-stream presence,
  - generic post-processing recovery,
  - and outright absence within the measured slice.
- Add or update worked examples for at least one hard unsatisfied family so the
  repo shows the exact query, bounds, stream shape, and current conclusion.

## Exit Criteria

- The repo has an explicit set of promoted legacy-unsatisfied target queries.
- At least one representative `GV` query has been evaluated end to end across:
  - raw kernel stream,
  - generic answer-layer stream processing,
  - and, if necessary, documented fallback handling.
- At least one additional non-`GV` unsatisfied legacy family has been evaluated
  under the same method.
- The branch concludes, query by query, whether the desired answer is:
  - already present in the raw stream,
  - present but impractically late,
  - recoverable by generic post-processing,
  - or still absent.
- The branch leaves behind a clear architectural recommendation for the next
  step:
  - continue generic relational engine work,
  - add a general-purpose stream-processing layer,
  - or explicitly justify a new specialty handler.
