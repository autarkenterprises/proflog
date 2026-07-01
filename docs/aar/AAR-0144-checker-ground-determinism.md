# AAR-0144: Proof-Checker Ground-Mode Determinism

- Date: 2026-07-01
- ADR: [ADR-0144](../adr/ADR-0144-checker-ground-determinism.md)
- Branch: `adr-0144-checker-ground-determinism`

## Outcome

Partial, and deliberately so. Of the four proposed Level-1 optimizations, **1C
landed** (pure, verified), **1A and 1B were implemented, measured to regress the
target case, and reverted**, and **1D was scoped and deferred** to its own ADR.
The durable deliverable is not one micro-optimization but a **measured
re-diagnosis**: the ground-tree checker cost is a ~78 000-node search for a
4-node proof, which no goal-reorder fixes.

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
instead of **following** the ground tree, and re-decodes from the root on every
backtracking path. That is a checker-architecture issue.

## Red/green and revert evidence

- **1A (select-before-decode):** benchmark 106 s → **>200 s (timeout)**. Ground
  proofs decode deterministically decode-first; select-first adds
  `|agenda|×match` decodes × ~78 K visits. Answer-set correct, cost wrong for the
  mode. Reverted.
- **1B (reconcile-in-selection):** benchmark **>240 s**. The 4-arm match (incl.
  alpha) is per-visit work × ~78 K visits. Reverted.
- **1C (byte-count nested-digit regroup):** benchmark fuel-10 20 200 → 18 434 ms
  (**~9 %**); `sjas-tree-builder-test` + `sjas-semprfk-tree-closure-test` 17/0/0;
  SJAS not-slow gate green. Enumeration-order identical by construction. Landed.
- **1D (decode-once):** the data-indicated big lever (kills ~41 % re-decode) but
  blocked from a safe one-session landing by nominal-identity in decoded ASTs and
  a host-memo purity decision. Deferred.

## Lessons

1. **Measure before optimizing a search.** The review named 1A the "headline";
   the data named it a regression. Profiling first would have caught this before
   two reverts.
2. **In a big search, per-visit work is multiplied by the visit count.** 1A/1B
   added goals to cut branching; with ~78 K visits and tiny agendas, they lost.
   The only wins here either remove work (1C: fewer scheduled goals) or remove
   visits (the deferred determinism/decode-once work) — never add per-visit work.
3. **Don't force the completion.** The instruction was to implement 1A–1D
   cumulatively; the honest result is one landed, two reverted with evidence, one
   deferred. Shipping a measured 2× regression labeled "optimization," or a memo
   that risks accepting invalid proofs, would have been worse than a truthful
   partial (ADR-0141 lesson).

## Follow-ups (each its own ADR)

- **Deterministic ground-proof rule dispatch** — tighten multi-arm heads
  (`forall`/`once-forall`/`exists`) so a ground node + children count selects one
  rule; prove the partition preserves completeness. This is what kills the
  ~78 K-node search.
- **Nom-safe decode-once** — decode each ground proof node once (upfront AST or a
  nominal-preserving memo) with a soundness gate.
- **Fully-relational payload count** — remove the residual 128-way byte-count
  regroup by counting the payload down from the header digits relationally (true
  O(1) over the header), replacing 1C's constant-factor win.
