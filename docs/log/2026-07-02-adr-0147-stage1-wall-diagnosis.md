# ADR-0147 stage 1: the giant-numeral wall is diagnosed and fixed; nested-forall gamma is the next probe

Date: 2026-07-02. Branch `adr-0147-theorem23-bot-closure`. Warm-REPL, measure-first.

## The wall (fixed)

The real-diagonal step-5 tree timeout was NOT a canonical-child mismatch and NOT
search width: stack sampling showed 20/20 samples inside `occurs-check`, and
size-scaled probes (`l1-probe`, argument numerals of 4..64 bytes) showed
superlinear per-size cost. Three compounding mechanisms, three landed fixes in
`willard_sjas_profile.clj`:

1. `byte-list-bitso` used general `arith/*o` per byte (quadratic radix step);
   now the fixed-radix six-bit `bit-prefixo` overlay (`byte-cons-equation-coreo`
   pattern). Highest byte unpadded+positive; zero is `'()`; sole caller is the
   forward `num`-payload conversion.
2. `sjas-internal-term-asto`'s `num`/`code` arms reconstruct numerals under a
   new `with-occurs-check-offo` goal wrapper (goal-level `sjas-acyclic-unifyo`,
   same acyclicity soundness argument; restores `:oc` on every answer state).
3. `structural-proof-valid?` (a) runs the whole ground construct-and-check
   under `with-occurs-check-offo` (explicit acyclic proof+target data — no
   cyclic binding expressible; kernel search/synthesis entries unaffected), and
   (b) `deep-ground-tag`s the host-built target and proof ONCE (ADR-0090 tags
   on every nom-free subtree, so `ext`'s ground-tag scan and occurs/walk skips
   short-circuit; ties/noms keep their spine untagged — the tag never lies).

Measured (small-basis mul system, warm): argument-scaling probe 32-byte
19.5s -> 7.1s, 64-byte 24.5s -> 15.0s (near-linear + constant); **the real
small-basis L1 (Subst premise with the actual 47-byte nbar and 96-byte
code(Dk)) went from >120s TIMEOUT to CLOSES in 34.1s**. `dpf`/
`decoded-proof-formula` is a misleading proxy (reifies noms; its run-shape has
its own pathology) — pipeline phases measured individually are instant.

## The frontier (bisected, open)

The full step-5 not-Dk tree (3 nested forall gammas + implies + two premise
clashes) still REJECTS (fast now: 0.7-4.7s — iteration is cheap). Bisection:

- D1 = ONE gamma + implies + both clash leaves: **closes** (shapes of the
  implies children `(not antecedent)` / consequent and the leaf `(neg atom)`
  nodes are right; premise clash binds the var).
- E1 = pure THREE-gamma chain (no implies): **rejects**, with the depth-2 child
  named `v1` AND with `v0` (both hypotheses false).

So nested-FORALL gamma chains are the precise break: the only pinned
nested-binder certificate is the EXISTS test
(`...distinct-nested-existential-parameters`, which uses `(par v0)`/`(par v1)`
canonical children and interleaves and-continuation nodes). Next probes, in
order: (i) replicate the exists-test shape for forall (interleave the body
`and`-continuation? use the test's `structural-tableau-node` builders); (ii)
inspect which forall arm (A/B/C) actually consumes a quantified-child node at
depth 2 by instrumenting arms in the warm REPL (alter-var-root counters, the
ADR-0145 method); (iii) if arm-B's unchanged-env path is selected, the
next-branch-nom depth key and the child's binder index genuinely diverge —
that would be a checker-side fix with its own answer-set argument.

Tooling note: `ast->canonical2` (session helper) lowers AST formulas to
canonical child syntax — `(var nom)`->`(var vK)` by a nom map and
`forall/once-forall/exists` ties to `(quant vK body)` index form (binder names
assigned in encounter order). Worked for depth-1 (D1/L1); the depth-2 mismatch
is between the checker's expectation and SOME part of this rendering or the
chain shape.

## Status

Perf fixes landed (this commit), gates pending below. The construction
(T_p-bar -> T_q -> final four-premise tree) remains as mapped in ADR-0147;
stage-1's residual is exactly the nested-forall-gamma child shape.
