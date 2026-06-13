# AAR-0098: SJAS Equality/Disequality Fragment Reachability

- Date: 2026-06-13
- ADR: [ADR-0098](../adr/ADR-0098-sjas-equality-fragment-reachability.md)
- Branch: `adr-0098-sjas-equality-relevance`

## Outcome

The Track 2a relevance-matrix row **equality and disequality profile rules**
(previously *unresolved and high risk*) is resolved via the **unreachability**
route: the SJAS structural proof checker closes equality- and
disequality-laden branches by formula-bearing recognition, not by consuming
equality/disequality proof tags, so those tags are unreachable in accepted
first-fragment certificates. The equality calculus is *absorbed* into the
formula-bearing tableau fragment ADR-0096 already admits — including the
equality-triggered calls the equality note flagged as highest risk — so it adds
no out-of-fragment rule power and compresses no subtree into a single tag. Full
reasoning in the
[equality fragment reachability note](../log/2026-06-13-sjas-equality-fragment-reachability.md).

This ADR is an audit: it does not change the kernel, proof checker, proof-code
encoder, or query behavior.

## Evidence

Source-level: every equality/disequality clause of
`sjas-structural-proof-check-state-decodedo` is formula-driven (reflexive
`same-termo` close, `rigid-different-termo` progression, disequality storage +
`sjas-neq-violated-coreo` recheck, positive-equality unification, and
formula-driven equality-triggered positive/negative calls). The decoded proof
is the tree shape; no equality-extension or disequality-closure tag is consumed.

Executable (red/green TDD):

- `equality-reachability-audit-flags-tags-and-clears-formula-bearing-certificates`
  was red on the missing `audit-equality-reachability`; green after. It checks
  a formula-bearing node and a bare `sjas-axiom` certificate report no equality
  constructors, while a tagged `(eq-step (neq-close (refl-close)))` term reports
  all three.
- `equality-reachability-audit-covers-the-equality-disequality-alphabet` pins
  the audited constructor set to encodable, Track 2a `:relevant` symbols.
- `sjas-equality-closure-is-formula-bearing-and-tag-free` closes `(neq one one)`
  through `sjas-proof-check-programo` with a `structural-tableau-node`
  certificate, asserting acceptance, zero proof-symbol tags, an empty
  `audit-equality-reachability`, and `:formula-bearing-tableau` fragment status.

Focused run: `Ran 3 tests containing 14 assertions. 0 failures, 0 errors.`

Broad gates (post-implementation):

- `lein test-proflog-fast` — Ran 193 tests containing 1016 assertions, 0
  failures (carries `proflog.sjas-correspondence-test`, including the two new
  audit tests).
- `lein test-proflog-sjas` (not-slow) — 140 vars, 1000 assertions, 0 failures
  (carries the willard-sjas equality-closure probe and the source audits).

## Follow-up

- Track 2b correspondence theorem for the equality fragment: prove the
  formula-bearing equality/disequality closures correspond to the selected SJAS
  `D`'s equality treatment (or a specified free-constructor theory) and preserve
  the proof-size lower bound, over the formula-bearing tree.
- Remaining high-risk relevance-matrix rows for later Track 2a slices:
  procedure-call / profile-interleaved theory rules, and quantifier
  instantiation / witness policy.
