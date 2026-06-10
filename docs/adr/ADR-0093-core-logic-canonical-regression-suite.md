# ADR-0093: Core.logic Canonical Regression Suite

- Status: completed
- Date: 2026-06-10
- Branch: `adr-0093-core-logic-canonical-regressions`
- AAR: [AAR-0093](../aar/AAR-0093-core-logic-canonical-regression-suite.md)

## Context

ADR-0090 changed the vendored core.logic substitution engine by adding a
conservative ground-term fast path to `ext`, `occurs-check`, `walk*`, and the
walk-term rebuild protocols. Its focused tests prove the new optimization is
observable and that a small set of unification, disequality, and occurs-check
cases still behave correctly. The change is nevertheless below Proflog's own
kernel: any semantic regression there can corrupt SJAS proof machinery, nominal
binding, tabled search, or arithmetic constraints while remaining invisible in
the SJAS selectors that motivated the optimization.

The miniKanren/core.logic literature gives a wider contract than ADR-0090's
initial regression. The miniKanren project describes the core language as a
small relational language with extensions for constraint logic programming,
nominal logic programming, and tabling. Core.logic advertises exactly that
surface for Clojure: miniKanren, cKanren-style constraints, alphaKanren nominal
logic programming, and CLP(FD). Byrd's dissertation treats fair interleaving,
reification, and tabling as core implementation concerns; cKanren makes
disequality and finite-domain constraints first-class; alphaKanren makes
freshness and binding terms first-class.

The optimization must therefore be checked against canonical behavior across
the whole core.logic surface, not only against the SJAS-shaped ground tree that
triggered the fix.

## Decision

Add two canonical regression namespaces:

- `proflog.core-logic-canonical-test`, included in the normal
  `lein test-proflog-fast` gate for cheap engine/conformance coverage.
- `proflog.core-logic-canonical-extended-test`, included in
  `lein test-proflog-extended` for slower literature-derived pearls and
  constraint puzzles that should not run on the fast path.

Both suites will stay at core.logic level and will not depend on Proflog
proof-search internals. Their examples should be useful to other miniKanren
implementers as a recognizable conformance layer, with core.logic-specific
extensions clearly separated from portable miniKanren semantics. They will
cover:

1. Core miniKanren semantics: unification orientation, sound occurs-check
   rejection, reification order, committed answer bounds, `project`, and
   `onceo`.
2. Fair search and classic list relations: `conde` interleaving, recursive
   natural-number generation, `appendo`, `membero`, `member1o`, `rembero`, and
   improper `lcons` tails.
3. Constraint semantics: core.logic disequality, project-local `symbolo`,
   `numbero`, and `absento`, including delayed residual constraints that later
   become either satisfied or rejected.
4. Nominal semantics: `nominal/fresh`, `nominal/hash`, and `nominal/tie` must
   continue to behave through walked structures that also contain ground
   subtrees.
5. Tabling semantics: tabled answer reuse must preserve results over ground
   and partially open arguments; the ground metadata must not collapse table
   keys or answer reification.
6. Literature-derived relational programs: attributed Clojure adaptations of
   classic miniKanren examples, including the standard relational-interpreter
   quine and twine queries from the "miniKanren, Live and Untagged" line of
   work. The fast suite should include only examples whose expected duration
   stays modest; the extended suite carries the slower exact generated
   quine/twine checks.
7. CLP(FD): finite-domain interval/domain constraints, arithmetic propagation,
   disequality, distinctness, and equation sugar must still enumerate the
   expected small solution sets.
8. Performance guardrails: modest, deterministic walk/occurs probes should
   record expected duration in test metadata and fail only on clear regression
   envelopes, while hour-scale SJAS probes remain outside this canonical fast
   namespace.

The tests will prefer public core.logic APIs. Only the existing ADR-0090 helper
style may inspect metadata tags, and only to assert that the performance fast
path is still being exercised. Maps, records, sets, and nominal records remain
outside the ground-tag grammar by design and must be covered semantically rather
than forced into the optimization.

## Consequences

- The fast suite gains a core.logic canary that is independent of the SJAS
  codebase and easier to diagnose when a future vendored core.logic change
  regresses semantics.
- The suite intentionally mixes classic miniKanren examples with host-specific
  Clojure structures because ADR-0090 changed host-level walk behavior. Tests
  intended to be portable are written as self-contained relations and annotated
  with their literary origin.
- The test namespace is not a replacement for long SJAS proof probes. It is the
  broad, cheap layer that should catch incorrect core.logic changes before
  expensive proof predicates are blamed.
- Infinite host lazy sequences are not part of the finite miniKanren term model
  and are not admitted as fast tests. Finite lazy/seq-shaped terms are covered.

## Test Obligations

- Before being added to `test-proflog-fast`, each new test var must be run
  directly with `lein test :only proflog.core-logic-canonical-test/<var>`.
- The complete new namespace must pass on the ADR-0090 patched default overlay.
- `lein test-proflog-fast` must pass after the namespace is added.
- Before being added to `test-proflog-extended`, each extended test var must be
  run directly with
  `lein test :only proflog.core-logic-canonical-extended-test/<var>`.
- `lein test-proflog-extended` must pass after the extended namespace is added.
- Because the project also keeps an opt-in core.logic 1.1.1 overlay, the new
  canonical namespaces should be run at least once under
  `lein with-profile +core-logic-source-overlay test :only
  proflog.core-logic-canonical-test` and
  `lein with-profile +core-logic-source-overlay test :only
  proflog.core-logic-canonical-extended-test`.

## Exit Criteria

- ADR, implementation, tests, and AAR are recorded.
- The canonical namespaces are in the fast and extended gates.
- Any uncovered core.logic surface is explicitly documented as a residual risk
  or future test expansion.

## References

- miniKanren project overview: https://minikanren.org/
- core.logic README: https://github.com/clojure/core.logic
- Jason Hemann and Daniel P. Friedman, "microKanren: A Minimal Functional Core
  for Relational Programming": https://webyrd.net/scheme-2013/papers/HemannMuKanren2013.pdf
- Claire Alvis et al., "cKanren: miniKanren with Constraints":
  https://www.schemeworkshop.org/2011/papers/Alvis2011.pdf
- William E. Byrd, Eric Holk, and Daniel P. Friedman, "miniKanren, Live and
  Untagged: Quine Generation via Relational Interpreters":
  https://webyrd.net/quines/quines.pdf
- Jason Hemann and Daniel P. Friedman, "Some Novel miniKanren Synthesis Tasks":
  https://minikanren.org/workshop/2020/minikanren-2020-paper9.pdf
- William E. Byrd and Daniel P. Friedman, "alphaKanren: A Fresh Name in
  Nominal Logic Programming": https://webyrd.net/alphamk/alphamk_workshop.pdf
- William E. Byrd, "Relational Programming in miniKanren: Techniques,
  Applications, and Implementations":
  https://scholarworks.iu.edu/dspace/bitstreams/27f1ebb8-5114-4fa5-b598-dcfaddfd6af5/download
