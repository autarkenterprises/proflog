# ADR-0037: Core.logic miniKanren Feature and Performance Enhancements

- Status: proposed
- Date: 2026-05-03
- Branch: `adr-0037-core-logic-minikanren-enhancements`
- AAR: pending
- Depends On:
  - [ADR-0032](ADR-0032-core-logic-performance.md)
  - [ADR-0036](ADR-0036-speculative-relational-arithmetic-and-tabling.md)

## Context

ADR-0036 imported faster-minikanren-style relational arithmetic to test whether
`step-fuelo` can stop depending on finite-domain arithmetic constraints. The
reason to do that is not aesthetic: hardcoded finite-domain ranges can block or
distort reverse and partial synthesis modes when the domain bound becomes part
of the search semantics.

That import exposed a broader host-library question. Core.logic was chosen
because it brings miniKanren to Clojure with useful host integration and a mature
nominal logic subsystem. It is not necessarily a strict superset of other
miniKanren implementations. The translated upstream arithmetic tests needed
`symbolo` and `absento`; core.logic 1.0.1 exposes lower-level constraint
machinery that can express them, but does not expose those relations directly.

## Decision

Create a separate speculative ADR branch to evaluate whether this project should
carry a project-local enhanced core.logic profile or overlay before replacing
production fuel arithmetic.

The branch should be run as a delegated research and implementation track. The
subagent coordinator may spawn subordinate agents for independent research or
implementation slices, but production Proflog behavior must not be changed until
the evidence is concrete.

## Work Tracks

1. Survey faster-minikanren and canonical Scheme/Racket miniKanren
   implementations for features core.logic lacks or underexposes.
2. Identify low-risk core.logic additions first, especially `symbolo`,
   `numbero`, `absento`, and related tree constraints needed by imported
   relational arithmetic tests.
3. Revisit core.logic generic performance TODOs and known hot paths without
   weakening miniKanren semantics.
4. If a project-local core.logic overlay is justified, add it behind an explicit
   profile or vendor boundary with tests against upstream-style behavior.
5. Re-examine the Proflog greenfield codebase for places where the improved
   constraint vocabulary or generic optimizations can replace project-specific
   membership/type checks or improve tableau proof handling.

## Candidate Questions

- Can production `step-fuelo` retain the public integer/nil API while delegating
  bounded arithmetic to pure relational bit-list numerals internally?
- Should `symbolo`, `numbero`, and `absento` become public relations in a
  project-local core.logic overlay?
- Can Proflog-specific type relations be expressed as reusable core.logic
  predicate or tree constraints without making the kernel less relational?
- Which Proflog membership checks are actually absence/type constraints and
  would become cleaner or more complete with `absento`-style constraints?
- Which core.logic TODOs are performance-only improvements, and which risk
  changing search order, reification, constraints, nominal behavior, or tabling?

## Guardrails

- Preserve miniKanren correctness and fair search semantics.
- Keep core.logic changes behind a project-local profile or explicit source
  overlay until upstream compatibility and regression risk are understood.
- Do not replace production `step-fuelo` until reverse/partial synthesis probes
  show a concrete benefit and existing integer-fuel callers remain supported or
  receive an explicit migration.
- Treat direct raw core.logic tabling as closed by ADR-0036 unless a new
  canonical-state integration point is discovered.
- Prefer generic relations and constraints over Proflog-specific shortcuts.

## Initial Evidence Required

- Survey log with primary-source links and a feature matrix across selected
  miniKanren implementations.
- Core.logic TODO/performance audit with ranked opportunities and risk levels.
- Tests for any imported or newly exposed constraints.
- Focused `step-fuelo` replacement probes showing reverse/partial synthesis
  behavior without finite-domain hard bounds.
- Proflog integration audit listing candidate replacements and cases rejected.

## Initial Test Obligations

- ADR-36 arithmetic tests.
- Existing kernel, query, and synthesis-mode tests affected by fuel handling.
- Upstream-style constraint tests for `symbolo`, `numbero`, `absento`, and any
  additional imported relation.
- Focused performance probes before and after any core.logic engine change.

## References

- [ADR-0036](ADR-0036-speculative-relational-arithmetic-and-tabling.md)
- faster-minikanren `numbers.scm` and `test-numbers.scm`
- core.logic source and current project dependency profiles
- miniKanren.org implementation and paper catalog
