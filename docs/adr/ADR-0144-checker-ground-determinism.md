# ADR-0144: Proof-Checker Ground-Mode Determinism (Level 1 kernel optimizations)

- Status: accepted (1C landed; 1A/1B reverted with data; 1D deferred to its own ADR)
- Date: 2026-07-01
- Branch: `adr-0144-checker-ground-determinism` (off `adr-0142-sjas-mul-boundary-derivation` @ `6e0f847`)
- AAR: [AAR-0144](../aar/AAR-0144-checker-ground-determinism.md)

## Context

ADR-0142's genuine-derivation work stalled on the SJAS proof checker's
performance over constructed proof objects. A review re-grounded the diagnosis by
classifying the `^:slow` negatives in `proflog.sjas-not-dk-qdisproof-test`:
`wrong-premise-leaves-the-universal-open` submits a **fully ground** tableau tree
(`tb/flex-tableau-node` = `node-from-bytes (formula-code-bytes …)`) and a ground
target, and asserts `(not (valid-tree? …))`. It is `^:slow` because *"a
correctly-failing refutation exhausts the closure search."*

The organizing hypothesis: the wall is **the checker running in search mode on
ground input**, a formulation problem rather than term size, addressable by making
the ground-check mode semi-deterministic (≤1 live answer per goal) with pure goal
reordering / regrouping / search→arithmetic replacement — no
`project`/`conda`/`condu`/host cut (the §D constraint), answer-set-identical.

Four Level-1 optimizations were proposed: **1A** ground-target propagation
(select-before-decode), **1B** ground-input dispatch pruning, **1C** arithmetic
byte-count, **1D** decode-once. This ADR records what a persistent-REPL,
measure-first implementation actually found.

## What the measurements showed

A warm nREPL (`clojure-mcp`) removed the ~55 s lein compile floor and made
per-experiment iteration seconds, enabling profiling and instrumentation.

**The benchmark is a ~78 000-node search for a 4-node ground proof.** Instrumenting
`wrong-premise-leaves-the-universal-open` (fuel 10, ~15 s):

| driver | count |
|---|---:|
| `sjas-proof-check-stateo` entries | **78 312** |
| `sjas-structural-proof-check-state-decodedo` entries | 8 177 |
| `formula-bearing-proof-nodeo` calls (decodes) | **6 981** |
| `sjas-proof-guided-selecto` calls | 4 379 |

A 4-node ground tree is decoded ~6981 times: the search re-traverses from the root
~1745 times on backtracking, re-decoding ground nodes each path. Cost is
**fuel-flat** (≈20 s at fuel 3 = ≈20 s at fuel 12): the branching is wide-and-shallow
(the checker searches for a valid rule interpretation of each ground node), not
deep re-instantiation. Stack-sampling self-time (proflog frames): core.logic
scheduling in `structural-proof-valid?` ≈36 %, **decode path ≈41 %**
(`decode-natural-bodyo` 4096-way `or*`, `sjas-acyclic-unifyo`,
`skip-length-prefixed-payloado`, the `sjas-internal-*asto` AST build), substitution
≈10 %.

The decode cost is dominated by *re-decoding the same ground nodes on every
backtrack*, because the proof carries only formulas (rule inferred) — the checker
must *search* the rule, so it re-enters and re-decodes.

### 1A — ground-target propagation (select-before-decode): REVERTED, measured 2× regression

Reordering the entry and literal-continuation arms to select→reconcile→decode made
the benchmark go **106 s → >200 s (timed out)**. A ground proof decodes
deterministically in decode-first order (one decode/node); select-first instead
does `|agenda| × (4-arm match)` forward-decodes per node, multiplied by the ~78 K
node-visits. 1A only helps when the *proof is free* (search mode) — which ADR-0142
rules out as "inaccessible" and construct-and-check never uses. Correct on the
answer set, wrong on cost for the target mode. Reverted.

### 1B — reconcile-in-selection (decode-first): REVERTED, measured regression

Making `sjas-proof-guided-selecto` reconcile each candidate against the decoded
node-formula (`sjas-subst-formulao` + `sjas-proof-node-formula-matcho`) made the
benchmark **>240 s**. The reconcile's own 4-arm match (including alpha) is per-visit
work multiplied by ~78 K node-visits; it added far more than the agenda-selection
determinism removed (agenda is tiny here). Reverted.

**Lesson (both 1A and 1B):** with a ~78 K-node search, *any* per-visit work is
multiplied ~78 000×. Optimizations that add goals to reduce branching backfire
unless they remove far more branching than they add. The benchmark's branching is
not in selection or decode ordering.

### 1C — byte-count nested-digit regroup: LANDED (pure, ~9%)

`decode-embedded-code-bodyo` and `decode-natural-bodyo` searched all
`max-code-bytes`+1 = 4096 candidate byte-counts in a flat `or*`. The byte-count is
its own two base-64 header digits (`byte-count = high·byte-base + low`, capped at
`byte-base²-1 = max-code-bytes` by the 2-byte header). Regrouped high-digit-outer /
low-digit-inner: `byte-base + byte-base` = 128 header unifications (and that many
fewer scheduled goals) instead of 4096. `high`-outer/`low`-inner preserves the exact
ascending byte-count order, so it is **enumeration-order identical**; the two header
`==`s still precede the payload `fresh` (ADR-0110 header-before-payload guard).

Pure disjunction regrouping. Measured: benchmark fuel-10 **20 200 ms → 18 434 ms
(~9 %)**, consistent with `decode-natural-bodyo`'s ~12 % profiled self-time.
Correctness: `sjas-tree-builder-test` + `sjas-semprfk-tree-closure-test` (17
decode-heavy proof-check assertions) green; SJAS not-slow gate green.

Why not fully O(1)? The downstream `parse-code-payload-byteso` recurses on a **host**
integer (`zero?`/`dec`); extracting a host int from the logic-var digits `low`/`high`
without enumeration needs `project` (forbidden). A truly O(1) version requires
rewriting the payload parse to count down relationally from the header digits — a
larger, riskier change deferred as a follow-up.

### 1D — decode-once: DEFERRED (the real lever, but blocked for a safe one-session change)

The data says the biggest win is eliminating the ~6981 redundant decodes (the ~41 %
decode path is re-run every backtrack). But a decode memo is blocked from a safe
one-session landing by two issues: (a) decoded ASTs contain **nominal logic values**
(`sjas-internal-nom-termo`) whose identity a `run`/reify-based cache mangles;
(b) a host-keyed memo is a purity call (referentially transparent, but host state)
that belongs in a dedicated ADR with soundness validation — a checker that caches
wrong could *accept an invalid proof*, the worst outcome for this project. Deferred.

## Decision

Land **1C** (the one clean, pure, verified Level-1 win). Revert **1A/1B** (measured
regressions on the target ground-proof mode). Defer **1D** to a dedicated ADR.

Record the redirected roadmap: the checker's ~78 K-node search for a 4-node ground
proof is the actual blocker, and it is a **checker-architecture** problem, not a
micro-optimization one. The checker *searches* for a rule interpretation of each
ground node instead of *following* the ground tree. Two candidate fixes, each its
own ADR with soundness/completeness care:

1. **Deterministic ground-proof rule dispatch.** Make rule inference
   mode-deterministic given a ground node formula + children count (e.g. the three
   `forall`/`once-forall` arms are the main residual multi-arm head). Requires
   proving the arms partition the ground cases so tightening preserves completeness.
2. **Nom-safe decode-once.** Decode each ground proof node once (upfront AST, or a
   nominal-preserving memo) so backtracking does not re-decode. Requires nominal
   identity preservation and an explicit purity decision.

## Consequences

- Pure-relational semantics preserved; no new operator, no committed choice, no host
  cut. Upgrading core.logic is not a lever (vendored 1.0.1 and 1.1.1
  `Substitutions`/`walk`/`ext` are byte-identical).
- 1C is a modest but genuine, verified win on the whole decode path (every SJAS
  proof-check decodes numerals/embedded codes through these two relations).
- Honest scope: this ADR does **not** make the ground-tree negative "fast." The
  measurements show why the simple pure reorders can't, and hand the next ADR a
  data-grounded target (kill the ~78 K-node search) instead of a guess.
- These optimizations serve the *checking* half of ADR-0142. They do not construct
  Willard's tower-sized bottom-proof; that remains a construction obligation, not a
  performance one.

## Test obligations

- Answer-set preservation: SJAS not-slow gate + `sjas-tree-builder-test` +
  `sjas-semprfk-tree-closure-test` green after 1C (1C is enumeration-order identical
  by construction; the gates verify it empirically).
- Performance: benchmark timing reported above (1C ~9 %; 1A/1B regressions recorded
  as the reason for revert).
