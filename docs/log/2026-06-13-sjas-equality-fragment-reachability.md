# SJAS Equality/Disequality Fragment Reachability (Track 2a)

Date: 2026-06-13
ADR: [ADR-0098](../adr/ADR-0098-sjas-equality-fragment-reachability.md)

This note resolves the **equality and disequality profile rules** row of the
[SJAS tableau relevance matrix](2026-05-25-sjas-tableau-relevance-matrix.md),
previously *unresolved and high risk*, and the open question left by the
[equality relevance note](2026-05-26-sjas-equality-relevance.md): are the
generic equality/disequality constructors primitive, macro-expandable, or
excluded from the first correspondence fragment?

## Resolution: unreachable in the first fragment (equality absorbed into formula-bearing closure)

The SJAS structural proof checker
(`sjas-structural-proof-check-state-decodedo` in
`proflog.kernel.willard-sjas-profile`) closes equality- and disequality-laden
branches **by formula-bearing recognition**, not by consuming equality proof
tags. Reading every equality/disequality clause:

- **Reflexive disequality** — a leaf `(neq t t)` closes via
  `equality/same-termo` (no child, no tag).
- **Rigid disequality progression** — `(neq a b)` with
  `support/rigid-different-termo` continues to one child.
- **Disequality storage + recheck** — `(neq a b)` is stored in the branch
  `neqs` state; a later positive `(eq a b)` unifies the terms and closes the
  branch through `sjas-neq-violated-coreo` / `prune-contradictory-neqso`.
- **Positive equality** — `(eq a b)` unifies via `sjas-unify-termo-coreo` and
  closes on `sjas-eq-contradiction-coreo` or `sjas-contradictory-atoms-coreo`,
  or continues.
- **Equality-triggered calls** — the highest-risk feature the equality note
  flagged: a positive `(eq a b)` that fires a stored reflected call atom
  (`membero (list 'pos atom) lits` / `(list 'neg atom)`) is driven by the
  `(eq …)` formula plus the branch `lits`, expanding into a child subtree, not
  a single `eq-triggered-call` tag.

In every case the decoded proof is the **tree shape** (children lists); the
branch equality state (`sigma`, `neqs`, `lits`) carries the equality semantics.
The equality-extension tags (`eq-step`, `eq-triggered-call`,
`eq-triggered-neg-call`, `eq-refl`, `eq-bind`, `par-bind`) and the
disequality-closure tags (`refl-close`, `neq-rigid`, `neq-store`, `neq-close`)
are **never consumed** by the structural checker. They remain encodable for
legacy/public proof evidence, but they are unreachable in accepted
first-fragment `tableau-proof/3` / `subst-prf/4` certificates.

Consequence for the relevance matrix: the equality calculus does **not** add
out-of-fragment rule power and does **not** compress proof subtrees into single
tags. Equality reasoning lives *inside* the formula-bearing fragment ADR-0096
already admits, and is therefore subject to the same finite-tree and
proof-size discipline ADR-0097 audits — exactly the property the matrix row
required before equality could be called relevant-but-safe rather than a hidden
rule-power leak.

## Evidence

Executable, in `proflog.sjas-correspondence` and the SJAS suites
(see [AAR-0098](../aar/AAR-0098-sjas-equality-fragment-reachability.md)):

- `audit-equality-reachability` reports the equality/disequality constructors
  present in a decoded proof term; empty means absorbed.
- An end-to-end probe closes `(neq one one)` through the structural checker with
  a formula-bearing certificate, asserting the certificate is tag-free, carries
  no equality constructor, and audits as `:formula-bearing-tableau`.

## Residual obligation

This is a reachability/absorption result, not yet a Track 2b correspondence
theorem. The remaining obligation, when Track 2b formalizes the equality
fragment, is to prove that the formula-bearing equality/disequality closures
correspond to the selected SJAS deduction method `D`'s equality treatment (or
to a specified free-constructor theory) and preserve the proof-size lower
bound — over the formula-bearing tree, where the relevance matrix now places
them.
