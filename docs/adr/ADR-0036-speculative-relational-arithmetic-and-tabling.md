# ADR-0036: Speculative Relational Arithmetic and Core.logic Tabling

- Status: proposed
- Date: 2026-05-03
- Branch: `adr-0036-spec-relational-arithmetic-tabling`
- AAR: pending
- Depends On:
  - [ADR-0029](ADR-0029-relational-fuel-purity.md)
  - [ADR-0017](ADR-0017-relational-tabling-and-canonical-state.md)
  - [ADR-0035](ADR-0035-relational-residual-continuation.md)

## Context

ADR-0029 made `step-fuelo` relational by using `clojure.core.logic.fd`
constraints. That solved the immediate open-fuel mode problem, but it still
depends on finite-domain arithmetic rather than ordinary miniKanren-style
numeric relations.

The faster-minikanren repository has `numbers.scm`, a relational arithmetic
library over little-endian binary bit-list numerals, plus `test-numbers.scm`, a
suite that exercises multiplication, factorization, comparison, subtraction,
and a small relational interpreter that uses the arithmetic relations.

Separately, `core.logic` already provides tabled goals. ADR-0017 built Proflog's
own canonical proof-state tabling layer on top of `core.logic/tabled`, but the
project should reassess whether direct `core.logic` tabling surfaces can do more
useful work now that ADR-0035 has moved structural residual continuation into
the answer overlay.

## Decision

Treat both ideas as speculative. They should live on a dedicated ADR branch and
must not be merged into `main` unless correctness and performance evidence are
acceptable.

The branch may add:

- a Clojure translation of faster-minikanren `numbers.scm` as a reusable
  core.logic relation library;
- translated tests from `test-numbers.scm`;
- an opt-in `step-fuelo` experiment that replaces finite-domain constraints with
  the translated arithmetic relation;
- focused probes comparing direct `core.logic` tabling behavior with the
  existing `proflog.tabling` layer and ADR-0035 residual continuation needs.

The production `step-fuelo` API currently accepts host integers and `nil`. A
bit-list arithmetic relation is not a drop-in replacement unless the kernel
fuel representation changes or an adapter layer is introduced.

## Current Findings

The arithmetic translation is usable as an opt-in relation library. It supports
little-endian bit-list numerals, upstream-style multiplication/factorization,
and the small interpreter cases from `test-numbers.scm`.

The exact upstream suite depends on `absento` and `symbolo`, which
`org.clojure/core.logic 1.0.1` does not expose as public relations. The branch
keeps the translated tests runnable with test-local delayed constraints built
from `core.logic/treec` and `core.logic/predc`; those replacements are evidence
for this ADR, not a production constraint API.

The bit-list `step-fuelo` experiment can run a direct kernel proof when callers
pass finite fuel as miniKanren numerals, but it intentionally fails as a
drop-in replacement for the existing integer-fuel API.

The tabling probes close the direct-core.logic-tabling question for ADR-0036.
Proflog is not trivially duplicating core.logic functionality: the existing
`proflog.tabling` layer is an extension/refinement that wraps `core.logic/tabled`
with project-specific canonical proof-state keys. A smoke probe exercises the
core.logic tabled answer-cache path, but the ADR-0035 list-family rows only
create tabled substitutions and do not hit answer-cache reuse/subunification.
ADR-0036 will not pursue direct raw `core.logic/tabled` replacement further.

See
[ADR-36 Relational Arithmetic and Tabling Probes](../log/2026-05-03-adr36-relational-arithmetic-and-tabling-probes.md).

## Merge Gates

This branch is mergeable only if:

- the translated arithmetic tests pass, including the upstream-style factor and
  interpreter cases that are reasonable under Clojure core.logic;
- any fuel replacement either preserves the public integer-fuel API or is
  proposed as an explicit representation migration;
- focused kernel tests show no regression for `step-fuelo`, open-fuel proofs,
  and bounded query helpers;
- tabling probes demonstrate a concrete benefit over the current
  `proflog.tabling` wrapper or show a narrower integration point worth keeping;
- long-running list-family rows are measured before and after any proposed
  tabling integration; and
- the branch documents whether each feature is recommended, parked, or rejected.

At the current checkpoint, ADR-0036 remains proposed and speculative. The
arithmetic library is worth keeping on the branch for further evaluation; the
fuel replacement is parked behind an explicit representation-migration decision;
and the direct raw core.logic tabling subtrack is closed with a decision not to
pursue it further in this ADR.

## Non-Goals

- Do not replace production `step-fuelo` with bit-list numerals without an API
  migration plan.
- Do not patch `core.logic` internals as part of this ADR unless probes show the
  current Proflog workloads exercise the relevant tabled answer-cache path.
- Do not add predicate-specific list-family dispatch.

## Initial Test Obligations

- `lein test proflog.relational-arithmetic-test`
- `lein test proflog.relational-arithmetic-upstream-test`
- `lein test proflog.kernel-test`
- `lein test proflog.tabling-test`
- `lein probe-core-logic-tabling`
- focused `probe-core-logic-tabling` runs for ADR-0035 list-kernel rows

## References

- faster-minikanren `numbers.scm`: `https://github.com/michaelballantyne/faster-minikanren/blob/master/numbers.scm`
- faster-minikanren `test-numbers.scm`: `https://github.com/michaelballantyne/faster-minikanren/blob/master/test-numbers.scm`
- core.logic tabled implementation:
  `https://github.com/clojure/core.logic/blob/f41b8847/src/main/clojure/clojure/core/logic.clj#L1798-L2034`
  and current project dependency `org.clojure/core.logic 1.0.1`.
