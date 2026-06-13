# ADR-0106: SJAS Search-Width Reduction (and the Parallelism Verdict)

- Status: accepted (design + corrected diagnosis; implementation deferred to successor ADRs)
- Date: 2026-06-13
- Branch: `adr-0106-sjas-width-reduction`
- AAR: [AAR-0106](../aar/AAR-0106-sjas-search-width-reduction.md)

## Context

[ADR-0105](ADR-0105-sjas-substate-tabling-investigation.md) found tabling is not
the systemic fix for the subst-prf negative-exhaustion wall (re-derivation
1.00× on the probed hot relation). This ADR records (a) the literature verdict
on parallelising miniKanren, (b) a **corrected** diagnosis of where the negative
actually grinds, and (c) the search-width-reduction design space — the lever
that changes complexity rather than the constant factor.

## A. Parallelism is a constant factor (literature)

The directly-relevant work,
[concurrentKanren (2025)](https://arxiv.org/html/2510.04994), confirms: implicit
**OR-parallelism** over independent disjunction branches is the clean lever,
made sound by immutable structure-shared substitutions; a bounded worker pool
beats thread-per-branch; **measured speedups are modest and workload-variable**
(no absolute numbers, heavy variance), and **constraints are unaddressed**.
[Scheduling Complexity of Interleaving Search (Rozplokhas & Boulytchev, FLOPS
2022)](https://arxiv.org/abs/2202.08511) shows interleaving search has a
non-obvious cost model in which scheduling overhead is itself a component. The
classic OR-/AND-parallel Prolog line (Aurora, Muse, &-Prolog; Gupta et al.
TOPLAS 2001) gives the standing verdict: **parallelism is ≤#cores, never a
complexity reduction.** With 4 cores and ~5 GB free, it cannot make the
exponential negatives complete. It is therefore the wrong tool for the critical
path (and test-level parallelism does not touch a single-query critical path at
all).

## B. Corrected diagnosis of the grind

Component experiments (`probe_forward_mode.clj`) on a `:willard-sjas-level1`
system **corrected** the ADR-0105 interpretation:

| probe | result |
|---|---|
| decode group3 (ground) → formula | **1 ms** (not ~7.5 s) |
| decode S (ground) | 0 solutions — S is a *system* code, not a formula |
| `subst-code-any(prog, S, group3)` both-ground | **fails fast, n=0** — not the grind |
| `subst-code-any(prog, S, free)` forward | n=0 fast |

So ground decoding is ~1 ms and the both-ground subst-code check fails fast. A
direct jstack of the full grind (165% CPU) shows the time is in
**`decode_formula_byteso` / `decode_embedded_code_bodyo`** (proof-facing
formula/embedded-code decoding) + **`static_table_entryo`** (static-table
*enumeration*) + `sjas_acyclic_unifyo`, under `prove_program` — i.e. decoding and
table-enumeration over **variable-dense (non-ground) intermediate terms** in the
wide search. This matches the original 84-sample finding ("walk/occurs over
variable-dense terms = search width"). ADR-0105's "~12 distinct ~7.5 s decodes"
inference was wrong (the decode-probe fired 12× *during* 90 s of variable-dense
work; each ground decode is ~1 ms); the ADR-0105 *re-derivation* verdict (1.00×,
not tabling) is unaffected.

**The wall is: a wide search that keeps terms variable-dense, so the ground
fast-path (ADR-0090) and deterministic table lookup never engage, and decode +
table-enumeration branch combinatorially.**

## C. Search-width reduction design space

Ranked by leverage for this wall, grounded in the corrected diagnosis and the
relational-search literature:

1. **Mode-directed / ground-before-decode evaluation — highest leverage.**
   The grind is decoding/enumerating over *variable-dense* terms; ground decode
   is ~1000× faster (1 ms vs the grind). Reformulate the hot sub-relations so the
   code/formula being decoded is **ground at decode time** (compute it forward as
   a function, then decode), instead of decoding a partially-instantiated term
   produced by generate-and-test. This is the relational analogue of
   determinism/mode analysis: *"`conde` expressions recognized as deterministic
   are expanded before any others"*
   ([first-order miniKanren, 2019](https://minikanren.org/workshop/2019/minikanren19-final2.pdf)).

2. **Determinise static-table lookups on ground keys.** `static_table_entryo`
   *enumerates* the constructor/byte tables; with a ground key it should be an
   O(1) indexed lookup, not a relational scan that branches under a variable key.
   This is the ADR-0078 finite-table-lookup-scheduling line, extended to the
   decode tables.

3. **Goal ordering / early grounding + early failure.** Interleaving-search cost
   is acutely order-sensitive (Rozplokhas & Boulytchev). Order goals so codes
   are grounded and cheap necessary-conditions (byte-length, top-constructor,
   function-symbol count vs the target) are checked *before* the expensive
   decode + α-equivalence; partially evaluate `==` and propagate the target's
   structure to fail incompatible branches at the first byte
   (the `prune/stream`/`prune/goal` technique).

4. **Relevance prefilter** (cheap invariant index) before decoding — the
   search-side cousin of the Track-2a relevance work.

5. **Decision-engine offload.** For a decidable negative, replace brute
   interleaving with a specialised decision procedure or a SAT/BDD offload
   ([Committing to the bit, 2025](https://arxiv.org/pdf/2509.22614)).

Parallelism (Part A) is a complementary ≤#cores trim on whatever search remains
*after* width reduction — not a substitute for it.

## D. Purity constraint on the implementation (binding)

Width reduction must **not** be bought with extra-logical / committed-choice /
host-side procedures (`project`, `conda`, `condu`, host hash-map lookups behind
`project`, etc.). Such cuts would impair the relational purity the kernel
depends on. The governing principle: **replace implicit pruning (which tempts
cuts) with structural determinism** — the relation must be deterministic *by its
structure* when its key inputs are ground, so there is no choice point to prune
and therefore no cut is ever needed. Per option-by-option:

- **#1 mode-directed eval — pure by conjunction ordering.** core.logic's `bind`
  (conjunction) is sequential per answer: in `(fresh [] g₁ g₂)`, `g₂` sees `g₁`'s
  bindings (only `mplus`/disjunction interleaves). So sequencing a *pure*
  structural forward substitution (which builds a **ground** result from a ground
  source, like `sjas-internal-code-termo` builds a ground term from ground bytes)
  *before* the decode goal makes the decode run on a ground term — the ~1 ms
  ground path — with **no `project` to test groundness and no `conda` to commit**.
  The "forward functional" speed is an emergent property of ground inputs in a
  pure relation, not an operational cut.

- **#2 table determinisation — the one place to extend core.logic's data
  structure (purely).** `static-table-entryo` is a linear `(or* …)` disjunction,
  so a ground key still opens a choice point per entry. The pure fix is a
  **relational trie / indexed lookup** so a ground key descends *deterministically
  by unification* (structural determinism, sound in both directions, no cut) —
  realised either via `clojure.core.logic.pldb` indexed facts (already in the
  core.logic jar) or by extending the vendored core.logic with an indexed
  relational-lookup primitive. **Not** `conda`/`condu` to commit to the matching
  entry, and **not** a host map behind `project`.

- **#3 ordering / early failure — pure by unification.** Goal reordering is pure
  (sequential `bind`). Early failure is achieved by **unifying the target's
  structure into the candidate** so incompatible candidates fail *soundly* via
  `==`, and prefilters are **relational invariants** (e.g. a relational
  length/symbol-count goal shared between candidate and target), never a
  host-side check behind `project`.

If more expressive structure is genuinely needed, it is added *inside*
core.logic as a pure relational primitive/data structure (the standing doctrine:
complexify core.logic elegantly, preserving miniKanren semantics), not as an
impurity in the SJAS/Proflog layer.

## Decision

Pursue **width reduction**, led by mode-directed/ground-before-decode evaluation
(#1) and static-table determinisation on ground keys (#2), with goal-ordering /
early-failure (#3) as supporting. Parallelism is recorded as a sound but
constant-factor complement, not the critical-path fix. Each implementation step
is a successor ADR, gated by the ADR-0093 regression suite, a behaviour
(answer-set) preservation test, and the **§D purity constraint** (pure
relational/structural only — no `project`/`conda`/host cuts; richer structure is
added inside core.logic).

## Test Obligations

- The diagnosis experiments are reproducible (`probe_forward_mode.clj`,
  `grind.clj`, both reverted from the kernel; documented here).
- Any width-reduction implementation must preserve the proof/answer set (a
  with/without agreement test) and keep the broad gates green.

## Exit Criteria

- The parallelism verdict and the corrected diagnosis are recorded; the
  width-reduction design space is captured with the highest-leverage lever
  (mode-directed ground-before-decode) identified and evidence-backed; the
  successor implementation ADR (forward-mode reformulation of the hot decode
  path, measured) is named in AAR-0106.
