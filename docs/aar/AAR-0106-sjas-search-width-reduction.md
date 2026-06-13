# AAR-0106: SJAS Search-Width Reduction (and the Parallelism Verdict)

- Date: 2026-06-13
- ADR: [ADR-0106](../adr/ADR-0106-sjas-search-width-reduction.md)
- Branch: `adr-0106-sjas-width-reduction`

## Outcome

Researched the miniKanren parallelism literature and elaborated the
search-width-reduction design space, with a corrected diagnosis of the subst-prf
negative wall.

- **Parallelism is a constant factor.** [concurrentKanren (2025)](https://arxiv.org/html/2510.04994)
  endorses implicit OR-parallelism (sound via immutable substitutions, bounded
  worker pool) but reports modest, workload-variable speedups and no constraint
  handling; [Rozplokhas & Boulytchev (FLOPS 2022)](https://arxiv.org/abs/2202.08511)
  add that scheduling overhead is itself a cost component. With 4 cores it is
  ≤#cores — it cannot make the exponential negatives complete and is the wrong
  tool for the critical path.

- **Corrected diagnosis.** Component experiments showed ground decode is ~1 ms
  (not ~7.5 s), and the both-ground `subst-code-any` check fails fast — so they
  are not the grind. A direct jstack of the full grind put the time in
  proof-facing formula/embedded-code decoding (`decode_formula_byteso`,
  `decode_embedded_code_bodyo`) + static-table *enumeration* (`static_table_entryo`)
  + unification, over **variable-dense intermediate terms** in the wide search.
  This corrects ADR-0105's "~12 distinct ~7.5 s decodes" inference (the
  re-derivation verdict there is unaffected). The wall is a search that keeps
  terms non-ground, so the ADR-0090 ground fast-path and deterministic table
  lookup never engage.

- **Width-reduction lever (highest leverage):** mode-directed / ground-before-decode
  evaluation — make the code/formula ground at decode time (forward functional
  computation) so the 1 ms ground path and O(1) table lookups apply, instead of
  decoding partially-instantiated generate-and-test terms. Supported by
  static-table determinisation on ground keys (the ADR-0078 line), goal ordering
  / early-failure propagation, a relevance prefilter, and — for a decidable
  negative — a decision-engine/SAT offload.

## Evidence

- Diagnosis experiments (`probe_forward_mode.clj`, `grind.clj`): ground decode
  1 ms; `subst-code-any` both-ground n=0 fast; forward subst-code n=0 fast; full
  grind jstack hot relations as above. Scripts were scratch evals over existing
  relations (no kernel change) and were removed after measuring; the methodology
  is documented in ADR-0106 for reproducibility.
- Literature: see the Sources in ADR-0106.

## Follow-up

- **Successor implementation ADR:** reformulate the hot decode path so the
  decoded code/formula is ground before decode (forward functional mode), and
  measure whether the negative's variable-dense grind collapses toward the 1 ms
  ground-decode regime. Gate with the ADR-0093 suite + a with/without answer-set
  agreement test, **and the ADR-0106 §D purity constraint** (pure
  relational/structural only — no `project`/`conda`/host cuts).
- Determinise `static_table_entryo` on ground keys via a **pure relational
  trie / indexed lookup** (core.logic.pldb indexed facts, or a vendored
  core.logic indexed-relation primitive) — structural determinism, not a cut.
- Parallelism (concurrentKanren-style bounded OR-parallel disjunction) remains a
  recorded, sound ≤#cores complement for any residual search, not a critical-path
  fix.
