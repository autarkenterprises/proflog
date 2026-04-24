# ADR-0013: Relational Answer Performance

- Status: accepted
- Date: 2026-04-23
- Branch: `adr-0013-relational-answer-performance`
- AAR: pending

## Context

ADR-0011 made the default greenfield open-answer path more relationally pure,
but it also clarified where the remaining legacy gap actually lives.

- `append` often looks like an ordering and budget problem: more concrete
  answers are already in the raw stream, but later than the first symbolic
  frontiers.
- `reverse` looks more structural: longer-running direct-entry probes still do
  not export `r = [b,a]`.

The main structural asymmetry is the procedure-call boundary.

- Top-level literal program queries get a direct answer-mode entry path.
- Recursive program calls still rely on the ordinary procedure-call rule, which
  requires ground arguments before descent.

That makes the generic path more relationally pure than legacy, but it still
leaves open recursive modes vulnerable to residual-call export instead of full
materialization. The search also pays obvious duplication costs:

- repeated residual literals,
- distinct proof paths collapsing to the same exported answer,
- and no branch/frontier canonicalization strong enough to keep equivalent
  states from being explored repeatedly.

The project therefore needs a second branch that aims to close parity without
adding more specialty modes than necessary.

## Decision

- Attempt to close the remaining answer-path gap by strengthening the generic
  relational search itself rather than by defaulting to a new specialty mode.
- This branch will investigate and, where justified, implement the following as
  one coherent track:
  - recursive direct-entry semantics for nonground answer-mode subcalls,
  - frontier canonicalization, memoization, or tabling sufficient to cut off
    equivalent recursive answer states,
  - residual normalization and deduplication so answer merging and ranking see
    semantically identical frontiers as the same state.
- The design goal is to reduce the need for additional search modes, not to
  create another one.
- If ADR-0012 proves that a closed-answer parity mode is still necessary even
  after this work, its changes may be pulled into this branch deliberately and
  documented as such.

## Consequences

- If successful, the generic open-answer path becomes both more relationally
  pure and closer to legacy parity.
- Allowing recursive nonground descent will widen the search space, so some form
  of canonicalization or memoization is likely to be required to keep the branch
  operationally honest.
- Residual normalization should improve both correctness presentation and search
  quality, because duplicate residuals and unstable answer shapes currently make
  merging and prioritization weaker than they should be.
- The main risks are:
  - semantic drift if nonground recursive descent changes the intended
    first-order procedure-call boundary too aggressively,
  - runaway runtime or memory growth,
  - and overfitting to reverse/append without yielding a principled generic
    improvement.

## Test Obligations

- Add failing regressions that make the generic path answer the actual open
  parity questions:
  - `reverse([a,b], r)` should eventually export `r = [b,a]`,
  - `append(x, y, [a,b,c])` should recover deeper split families without a
    separate parity-only API,
  - nested suffix and nested inverse append queries should either close under
    the generic path or remain explicitly documented as open.
- Add regressions for frontier normalization:
  - duplicate residual literals collapse,
  - alpha-equivalent answer frontiers merge,
  - and repeated proof families do not crowd out later distinct answers.
- Add at least one targeted regression around recursive nonground subcall entry
  so this branch proves whether the procedure-call boundary has actually moved.

## Exit Criteria

- The generic open-answer path is remeasured with longer-running probes.
- The repo documents whether recursive nonground answer-mode descent is both
  semantically acceptable and operationally useful.
- Residual normalization and frontier canonicalization demonstrably reduce
  duplicate answer families.
- The branch concludes explicitly whether ADR-0012 is still needed:
  - unnecessary because the generic path now closes the parity gap well enough,
    or
  - still required, in which case its branch is intentionally pulled in.
