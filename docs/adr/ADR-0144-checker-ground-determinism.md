# ADR-0144: Proof-Checker Ground-Mode Determinism (Level 1 kernel optimizations)

- Status: accepted (1C + 1D landed; 1A/1B reverted with data)
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

### 1D — upfront tree decode: LANDED (pure; 1.8–2.1× on decode-heavy ground checks)

"Decode-once" splits into two distinct redundancies: **(a)** the *same node*
re-decoded across search re-visits (the 6981 count — a symptom of the ~78 K-node
search, subsumed by any future determinism fix), and **(b)** *cross-node*
redundancy, where each node's bytes are re-decoded from scratch on every
backtracking path. The landed 1D removes (b) for the construct-and-check entry
without host state, memoization, or reification:

- `decode-proof-treeo` / `decode-proof-foresto`: decode the whole **ground**
  proof tree once into `(decoded-node formula children)` nodes (`xtab-lem`
  wrappers preserved). Deterministic on a ground proof (the two arms are
  structurally exclusive).
- `decoded-proof-nodeo`: a **recognizer, not a generator** — a custom goal (same
  style as `sjas-acyclic-unifyo`) that destructures a `decoded-node` in O(1) and
  *fails* on anything else, so free-proof synthesis can never fabricate decoded
  nodes; the byte decoder remains the only source of synthesized proofs. The
  inspection is a **shallow protocol `walk`** of the proof and its head (a
  var-chain lookup) — never a deep `walk*` rebuild, which would be
  O(certificate) per entry on the large partially-instantiated certificates
  synthesis builds.
- `formula-bearing-proof-nodeo` gains the recognizer as a first `conde` arm; raw
  byte nodes and free proofs fall through to the byte decoder unchanged.
- `structural-proof-valid?` pre-decodes **inside the `run`** (canonical
  code-noms stay live, never reified) and checks over the decoded tree.

Nominal identity is preserved because the decoded AST is built from the shared
`code-nom-entries` noms and never leaves the substitution — the reify-mangling
hazard that deferred the memo variant does not arise.

**Measured (warm REPL, same JVM before/after):**

| test (`valid-tree?` over real mul-system codes) | baseline | 1D | ratio |
|---|---:|---:|---|
| `checker-validates-constructed-cut-free-trees` | 949 ms | 531 ms | **1.79×** |
| `complementary-closure-uses-named-primitives` | 2152 ms | 1013 ms | **2.12×** |
| `decoded-semprfk-leaf-closes-with-symbolic-tower-bound` | 3981 ms | 3500 ms | 1.14× |
| `too-small-tower-bound-does-not-close` (negative) | 3626 ms | 3155 ms | 1.15× |

The wrong-premise ground negative is **neutral** (search-dominated, tiny
formulas — its cost is the ~78 K-node search, not decode), consistent with the
(a)/(b) split. Synthesis mode (free proof; kernel `tableau-proof/3` route, which
does not pre-decode) pays only the O(1) recognizer arm:
`synthesizes-beta-axiom-citation` 31.1 s baseline → ~33 s (≤ ~6 %, single-run
noise band).

**Pre-existing drift found during attribution (not caused by this ADR):**
`sjas-tableau-proof-synthesizes-selfcons-citation` (`^:slow`, outside the
gates) times out at **>600 s on the baseline `c6d3f97` itself** (stash-differential
on the same warm REPL; the AAR-0095 envelope of 6.9 s is 2026-06-13-stale, and the
`^:slow` lane has not been re-run across the ADR-0119..0142 window). Recorded as
an open branch-drift issue for its own investigation; both baseline and 1D time
out identically, exonerating 1D.

## Decision

Land **1C** and **1D** (pure, verified). Revert **1A/1B** (measured regressions
on the target ground-proof mode).

Record the redirected roadmap: the checker's ~78 K-node search for a 4-node ground
proof is the actual blocker, and it is a **checker-architecture** problem, not a
micro-optimization one. The checker *searches* for a rule interpretation of each
ground node instead of *following* the ground tree. The remaining fix, its own
ADR with soundness/completeness care:

1. **Deterministic ground-proof rule dispatch.** Make rule inference
   mode-deterministic given a ground node formula + children count (e.g. the three
   `forall`/`once-forall` arms are the main residual multi-arm head). Requires
   proving the arms partition the ground cases so tightening preserves
   completeness. 1D's `decoded-node` trees are the natural substrate: rule
   dispatch can now key on an already-decoded formula head instead of bytes.
   This also subsumes the per-node re-visit re-decode (redundancy (a)) by
   removing the re-visits themselves.

## Consequences

- Pure-relational semantics preserved; no new operator, no host cut, no memo/
  reification. The only host-level construct added is the `decoded-proof-nodeo`
  recognizer — a non-generating O(1) destructure in the established
  `sjas-acyclic-unifyo` custom-goal style; answer sets are unchanged because its
  arm and the byte arm are mutually exclusive on every proof shape (a
  `decoded-node` head never destructures as bytes, and a free proof never
  matches the recognizer). Upgrading core.logic is not a lever (vendored 1.0.1
  and 1.1.1 `Substitutions`/`walk`/`ext` are byte-identical).
- 1C is a modest but genuine, verified win on the whole decode path (every SJAS
  proof-check decodes numerals/embedded codes through these two relations); 1D
  is a 1.8–2.1× win on decode-heavy ground construct-and-check — the exact mode
  ADR-0142's tree assembly uses.
- Honest scope: this ADR does **not** make the ground-tree negative "fast" (its
  cost is the ~78 K-node search, untouched here), and it surfaced two
  **pre-existing** issues, both differentially attributed to the baseline:
  (i) `^:slow`-lane drift — `synthesizes-selfcons-citation` 6.9 s → >600 s
  somewhere in the ADR-0119..0142 window; (ii) `fitting-fidelity-test` sec7 is
  a machine-state-sensitive in-gate landmine (cooperative `query-status`
  deadline + one unpreemptible fuel slice; hung the full fast gate ≥900 s on
  **both** the 1D tree and the untouched baseline, while passing in isolation
  — see AAR lesson 5b). Each needs its own investigation. The next ADR gets a
  data-grounded target (deterministic ground-proof rule dispatch over the
  now-available `decoded-node` trees) instead of a guess.
- These optimizations serve the *checking* half of ADR-0142. They do not construct
  Willard's tower-sized bottom-proof; that remains a construction obligation, not a
  performance one.

## Test obligations

- Answer-set preservation: SJAS not-slow gate + `sjas-tree-builder-test` +
  `sjas-semprfk-tree-closure-test` green after 1C and after 1D (1C is
  enumeration-order identical by construction; 1D adds only a mutually-exclusive
  recognizer arm and an in-`run` deterministic pre-decode; the gates verify both
  empirically). Free-proof synthesis re-verified directly
  (`synthesizes-beta-axiom-citation` green; SelfCons sibling pre-existing-slow,
  differentially attributed to the baseline).
- The ADR-0110 header-before-payload source guard was retargeted (same
  invariant, new `high-digit`/`low-digit` structure) and the profile-source
  hygiene audit kept green (no forbidden host-shortcut vocabulary).
- Performance: measurements reported above (1C ~9 % benchmark; 1D 1.8–2.1×
  decode-heavy; 1A/1B regressions recorded as the reason for revert).
