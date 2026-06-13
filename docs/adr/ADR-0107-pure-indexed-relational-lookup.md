# ADR-0107: Pure Indexed Relational Lookup (Width-Reduction #2)

- Status: in progress
- Date: 2026-06-13
- Branch: `adr-0107-pure-indexed-lookup`
- AAR: pending

## Context

[ADR-0106](ADR-0106-sjas-search-width-reduction.md) §C/§D identified
static-table determinisation (#2) as a width-reduction lever to be implemented
**purely** (no `project`/`conda`/`condu`/host cuts; richer structure added inside
core.logic). `static-table-entryo` is a linear `(or* …)` over the table entries:
for a ground key it opens a choice point per entry (O(N) unify attempts; the
constructor table has ~1590 entries), and for a free key it enumerates. The aim
is a lookup that is **deterministic on a ground key** (no spurious choice points)
yet **sound and enumerating on a free key**, by *structure* rather than by a cut.

Honest scope note: per the ADR-0106 corrected diagnosis, the subst-prf grind is
over *variable-dense* (non-ground) terms; #2 pays off on the grind only once #1
(mode-directed, ground-before-decode) makes those keys ground. #2 is the
enabling infrastructure (and a correctness-preserving O(N)→O(log N|1)
improvement on ground-key lookups in its own right); the grind-level payoff is
realised with #1.

## The crux

A single relation that is O(1)-forward, enumerate-backward, **without** inspecting
groundness and **without** double-counting is non-trivial: `featurec`/map
unification is forward-only (suspends on a free key); a naive
`(conde [featurec-branch] [enumerate-branch])` double-counts a ground key. The
clean resolution is **structural determinism**: a key whose *structure* (e.g. its
fixed-width bit/digit decomposition) descends a trie of ground logic terms, so a
ground key follows one path by unification (the other branches fail at the
discriminating `==`, no cut) and a free key enumerates the leaves — one relation,
both modes, no double-count, pure.

## Decision

1. Add a pure relational **indexed/trie lookup** primitive to the vendored
   core.logic overlay (the "more expressive data structure inside core.logic" of
   ADR-0106 §D), descended purely by unification — no `project`/`conda`.
2. **TDD in isolation first.** Build and test the primitive against the
   ADR-0093-style canonical suite *before* wiring it into `static-table-entryo`,
   so the engine change carries zero regression risk until validated.
3. Then re-express `static-table-entryo` over the primitive, preserving its exact
   answer set (a with/without agreement test), and run the broad gates.

## Test Obligations (TDD)

- A new `proflog.core-logic-indexed-lookup-test` (fast-gate) pins the contract:
  forward (ground key → its value, **exactly one** answer), backward (value →
  key), free (enumerates **every** entry **once** — answer multiset equals the
  table), and agreement with a linear `membero` baseline. Red before the
  primitive exists, green after.
- The full ADR-0093 canonical suite and the broad gates stay green after the
  engine change and after integration.

## Exit Criteria

- The pure primitive passes its contract + the ADR-0093 suite; `static-table-entryo`
  is re-expressed over it with an answer-set agreement test; broad gates green;
  no `project`/`conda`/host cut introduced. (#1 then makes the keys ground so the
  determinism pays off on the grind — successor ADR.)
