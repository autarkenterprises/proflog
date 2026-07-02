# AAR-0144: Proof-Checker Ground-Mode Determinism

- Date: 2026-07-01
- ADR: [ADR-0144](../adr/ADR-0144-checker-ground-determinism.md)
- Branch: `adr-0144-checker-ground-determinism`

## Outcome

Of the four proposed Level-1 optimizations, **1C and 1D landed** (pure,
verified), and **1A and 1B were implemented, measured to regress the target
case, and reverted**. The durable deliverables are (i) the two landed
optimizations, (ii) a **measured re-diagnosis** — the ground-tree checker cost
is a ~78 000-node search for a 4-node proof, which no goal-reorder fixes — and
(iii) the `decoded-node` tree representation 1D introduces, which is the
natural substrate for the follow-up determinism ADR.

A persistent nREPL (`clojure-mcp`) was the enabling tool — it removed the ~55 s
lein compile floor, turning "edit, recompile, run one test" (~2 min) into
"reload, run, profile" (seconds), which is what made measure-first possible.

## What was measured (the value)

Instrumenting `wrong-premise-leaves-the-universal-open` (a ground tableau tree,
ground target, must reject) at fuel 10:

- `sjas-proof-check-stateo` entered **78 312** times; `formula-bearing-proof-nodeo`
  (decode) called **6 981** times — for a **4-node** tree.
- Cost is **fuel-flat** (≈20 s at fuel 3 = ≈20 s at fuel 12) → wide-shallow
  branching, not deep re-instantiation.
- Self-time: core.logic scheduling ≈36 %, decode path ≈41 % (re-run every
  backtrack), substitution ≈10 %.

Conclusion: the checker **searches** for a rule interpretation of each ground node
instead of **following** the ground tree, and re-decodes on every backtracking
path. That is a checker-architecture issue; 1D removes the redundant *decode*
work, and the follow-up determinism ADR should remove the redundant *visits*.

## Red/green and revert evidence

- **1A (select-before-decode):** benchmark 106 s → **>200 s (timeout)**. Ground
  proofs decode deterministically decode-first; select-first adds
  `|agenda|×match` decodes × ~78 K visits. Answer-set correct, cost wrong for the
  mode. Reverted.
- **1B (reconcile-in-selection):** benchmark **>240 s**. The 4-arm match (incl.
  alpha) is per-visit work × ~78 K visits. Reverted.
- **1C (byte-count nested-digit regroup):** benchmark fuel-10 20 200 → 18 434 ms
  (**~9 %**); enumeration-order identical by construction. Landed.
- **1D (upfront ground-tree decode):** `decode-proof-treeo` +
  `decoded-proof-nodeo` recognizer + in-`run` pre-decode in
  `structural-proof-valid?`. Warm same-JVM before/after on `valid-tree?` over
  real mul-system codes: **1.79×** and **2.12×** (tree-builder positives),
  1.14×/1.15× (semprfk positive/negative); wrong-premise negative neutral
  (search-dominated); free-proof synthesis unregressed
  (`synthesizes-beta-axiom-citation` 31.1 s baseline vs 31.4 s landed). Landed.
- **Gates:** SJAS not-slow 1447/0/0 and fast 273/2350/0 after 1C; re-run green
  after 1D (recorded in the ADR).

## The 1D attribution detour (worth its cost)

The first 1D recognizer used a deep `logic/walk*`. During validation,
`sjas-tableau-proof-synthesizes-selfcons-citation` (`^:slow`) timed out
(>420 s) against a remembered envelope of 6.9 s (AAR-0095), which read as a
catastrophic synthesis regression. Two hypotheses failed in sequence (CPU
contention from a concurrent gate; the deep-walk cost — replaced with a shallow
O(1) head `walk`, still >600 s). The decisive experiment was a
**stash-differential on the same warm REPL**: the baseline `c6d3f97` *also*
times out at >600 s. The blowup is **pre-existing branch drift** somewhere in
the ADR-0119..0142 window (the `^:slow` lane had not been re-run across it);
1D is exonerated. The shallow-walk hardening was kept anyway (strictly cheaper,
O(1) in every mode).

## Lessons

1. **Measure before optimizing a search.** The review named 1A the "headline";
   the data named it a regression. Profiling first would have caught this before
   two reverts.
2. **In a big search, per-visit work is multiplied by the visit count.** 1A/1B
   added goals to cut branching; with ~78 K visits and tiny agendas, they lost.
   The wins either remove work (1C fewer scheduled goals; 1D decode once) or
   must remove visits (the follow-up determinism ADR) — never add per-visit
   work. Deep `walk*` inside a per-visit goal is per-visit work too.
3. **Don't force the completion.** Shipping a measured 2× regression labeled
   "optimization," or a memo that risks accepting invalid proofs, would have
   been worse than a truthful partial (ADR-0141 lesson). The 1D that finally
   landed is the pure upfront-decode variant, not the risky memo.
4. **Validate the `^:slow` lane's *current* envelope before attributing a
   timeout to your change.** An 18-day-old AAR envelope on a ~40-ADR-younger
   tree misattributed a pre-existing drift to 1D; the same-REPL
   stash-differential was the cheap, decisive attribution instrument.
5. **Client-side eval timeouts leave zombie computations in the REPL JVM.**
   Every timed-out eval kept burning ~4 cores; measurements taken next to a
   zombie are contended garbage. Kill/restart the REPL (and re-check `top`)
   before the next measurement.
5b. **`query-status`'s deadline is only cooperative — and that makes
   `fitting-fidelity-test` sec7 a machine-state-sensitive landmine.** The
   wallclock deadline is checked *between* deterministic fuel slices, so
   background machine state changes which slice is reached within budget, and
   one combinatorially-exploding slice cannot be preempted. Observed matrix
   (all on this session's machine): full fast gate GREEN at 1C-baseline with a
   4.3 GB REPL resident (134.8 s); HUNG ≥2 h at 1D with the REPL resident;
   HUNG ≥900 s at 1D on a quiet machine; **HUNG ≥900 s at 1C-baseline on the
   same quiet machine** — the hang is tree-independent, exonerating 1D. The
   namespace passes green in isolation on the 1D tree (8 tests / 222
   assertions, 287 s — itself far from its in-gate ~seconds, confirming
   context sensitivity). Open issue: a preemptible (chunked or
   budget-inside-slice) deadline in `query-status` would defuse the landmine;
   until then full-fast-gate greenness is machine-state roulette regardless
   of tree. The 1D commit gate therefore ran the fast suite **minus** the
   landmine namespace (38 namespaces) plus the isolated fitting-fidelity run.
6. **Recognizer, not generator.** Any fast-path arm added to a relation that
   also runs in synthesis mode must *fail* on free input (shallow host
   inspection), or synthesis will fabricate fast-path structures and bypass the
   byte decoder — a soundness hole, not just a perf bug.

## Follow-ups (each its own ADR)

- **Deterministic ground-proof rule dispatch** over 1D's `decoded-node` trees —
  tighten multi-arm heads (`forall`/`once-forall`/`exists`) so a ground node +
  children count selects one rule; prove the partition preserves completeness.
  This kills the ~78 K-node search and subsumes the remaining per-visit
  re-decode.
- **Pre-existing `^:slow` synthesis drift**: `synthesizes-selfcons-citation`
  6.9 s (2026-06-13) → >600 s at `c6d3f97`, cause in the ADR-0119..0142 window;
  needs bisection/attribution if the slow lane's envelopes are to be trusted
  again.
- **Fully-relational payload count** — replace 1C's residual 128-way regroup
  with a relational count-down from the header digits (true O(1) over the
  header).
- **Extend pre-decode** to the other explicit-proof entries
  (`tab2-proof-list-valid?`, `dsjas-tab2-proof-valid?`, `dsjas-subst-prf-valid?`)
  if their workloads become decode-bound; they are unchanged (byte path) today.
