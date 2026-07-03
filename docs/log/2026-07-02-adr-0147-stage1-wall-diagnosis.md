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

## Stage 1b addendum (same day, post-commit 247f32f)

Three decisive updates from the instrumented warm-REPL session:

1. **E1's nested-forall "failure" was never the checker.** Two probe artifacts
   stacked: (a) synthetic formulas used `add(h,y)` argument terms, which the
   encoder VALUE-reads when ground (deliberate many-to-one numeral reading,
   ADR-0095) so the decoded node disagreed at `(app add ...)` vs `(app dbl ...)`;
   (b) the rebuilt small system passed `:functions {'pow 2}` WITHOUT
   `total-multiplication-functions` -- the missing `mul` opened a compaction gap
   and `semprfk-alpha` decoded as `(sym 30)`. With the correct vocabulary
   (`(assoc sjas/total-multiplication-functions 'pow 2)`) the real not-Dk tree's
   gammas FIRE with canonical noms v0/v1 (matcho instrumentation: 104 calls).
   The `ast->canonical2` child generation (log above) is CORRECT as designed.

2. **Latent upstream bug found and patched: `clojure.core.logic.nominal`
   `-suspc`** -- the tie-alpha suspension's swap loop recurs
   `((hash nom t2) a)` without a nil guard, so a FAILING freshness constraint
   crashes (`No implementation of :walk ... for nil`, via fix-constraints ->
   run-constraint -> -suspc -> -hash -step) instead of failing the suspension.
   Exposed by real-Dk tie matching; reproduced with the UNMODIFIED entry (my
   stage-1 features exonerated by variant bisection). Fix: `nominal.clj`
   vendored from the 1.0.1 jar into the source-path (shadows the jar, same
   mechanism as the existing vendored logic.clj) with a 3-line nil
   short-circuit in the loop, marked `proflog ADR-0147`. Per the
   core.logic-patch doctrine this is called out for its own audit note.

3. **Next wall, precisely queued**: with the crash fixed the real not-Dk tree
   neither closes nor rejects in 280s -- the tie-alpha/binder-renaming
   matching on 96-byte-code-bearing bodies grinds (nominal swap/hash constraint
   work per attempt x residual structured-head matcho multiplicity). Next
   levers, in order: (i) sample the run (jcmd; the workflow above); (ii) land
   the ground-equal dedup for STRUCTURED heads in matcho (the ADR-0145
   literal-head dedup deliberately left the 4-arm ladder for structured heads;
   ground-identical quantified pairs re-deliver via binder/alpha arms and every
   duplicate re-explores the subtree); (iii) if needed, a cheap first-diff
   guard before the alpha arm. All three are warm-REPL-iterable in seconds
   against the fast-rejecting D1/L1 pins.

State: nominal patch + this log are the commit; the not-Dk tree recipe is
fully scripted above and in memory. Gammas verified firing; the construction
is unblocked pending the structured-matcho dedup.

## Stage 1c: the 280s grind profiled (hot-frame distribution)

Ten-sample jcmd distribution of the real not-Dk run: nominal `-hash`
(freshness constraints) + `-do-suspc` (tie suspensions) + **`ground-tree?`/
`ground-tag` inside `ext`** + ConstraintStore ops. Mechanism: every tie-alpha
attempt `swap-noms`-REBUILDS the 96-byte-code-scale body into fresh untagged
structure, so every subsequent `ext` re-runs the O(body) tagging groundness
scan, plus per-nom freshness constraints -- all times the structured-head
matcho ladder x search attempts.

Queued lever (next session's first edit, core.logic-deepening per doctrine):
short-circuit `swap-noms` on subtrees that cannot contain the swapped noms --
if ADR-0090's ground tag implies nom-free (CHECK ground-leaf?/ground-tree?
semantics on Nom first!), a ground-tagged subtree returns UNCHANGED (structural
sharing preserves both the object and its tag; O(spine) instead of O(body) per
swap, and the ext rescans vanish). If the tag does not imply nom-free, tag
nom-free-ness separately in deep-ground-tag (it already only tags nom-free
subtrees) and key the swap skip on that meta. Second lever: `ground-tree?`
should accept already-tagged subtrees without descent (verify; ADR-0090's
walk* comment claims it, confirm the ext path uses the same skip).

## Stages 1e/1f/1g: nested-gamma certificates close; the ladder's multipliers cut

**1e — recursive binder-renaming.** `sjas-proof-node-formula-matcho`'s binder
arm previously renamed ONE layer then demanded `==` of the bodies; for nested
quantifiers that unifies concretely-but-differently-named inner ties, which
routes through the nominal alpha-suspension machinery (`-do-suspc`/`hash`
freshness constraints over full code-bearing bodies) at every attempt. The arm
now recurses the proof-node match per layer (its exact arm subsumes the old
`==`), so ties never meet the suspension path during certificate matching.

**1f — ground-mode gamma-arm gating.** The quantifier strategy ladder keeps
redundant gamma arms for proof SEARCH: the env=() arm is a verbatim duplicate
of the deferred-substitution arm (remove-bindo/subst are identities there), and
the eager-substitution forall/exists arms accept exactly the ground
certificates the deferred arm accepts (identical reconciled visible formula).
On a ground certificate every redundant arm multiplies the residual search per
nesting level. `structural-proof-valid?` now sets a state-meta ground-check
flag (`sjas-ground-check-flago`); `sjas-unless-ground-checko` gates the three
redundant arms off in that mode only. Kernel search/synthesis states never
carry the flag. Measured (small system, tiny args, implies+two-clash core):
depth-1 5.1s / depth-2 16.1s / depth-3 74.1s ALL CLOSE — pre-gating, depth-2+
rejected or timed out. The REAL not-Dk tree's gammas verifiably fire.

**Negative result (reverted):** a matcho ground-equal host-`=` shortcut with
alpha-arm gating made the depth probes SLOWER (13.9/24.4/116.8) — the deep
host comparison pays on every call and wins nothing on the non-equal pairs
that dominate; reverted to the 1e ladder.

**1g — budgeted ext-time tagging (vendored logic.clj).** With the above, the
real-mass profile showed per-`ext` `ground-tree?`/`ground-tag` scans dominating:
ADR-0090's tag never amortizes on short-lived mid-search rebuilds. `ground-tag`
now scans under a 64-node budget (`ground-tree-within?`; tagged subtrees and
atoms are budget-free, so bottom-up construction still tags every durable
level; walk*/entry deep-tag still cover durable structures; the tag is an
optimization hint, so skipping is semantics-free). Post-patch samples show the
tag-scan frames gone; the real not-Dk run is pure unify/walk/subst churn.

Real not-Dk envelope: >41 min on the pre-1g engine (killed); the budgeted run
is in flight at this writing. The synthetic depth-3 certificate (the exact
structure) closes in 74s; the pinned `^:slow` test
`real-diagonal-not-dk-tree-closes` carries the real tree; the
`tb/ast->canonical-child` helper is promoted for all further ADR-0147 trees.

## Stage 3 (2026-07-03): the 58K wall root-caused, cut 5x, mid-scale unblocked; real-mass is memory-bound

**3a head-guard (commit ba0d9f5):** guided-selecto prunes agenda candidates
whose top tag can't match the decoded node (ground-check only; sound -- matcho
fixes the head on every arm). L1 premise-clash 34.1->24.2s; depth-3 mixed.
Kept (helps the premise-clash shape the final four-premise refutation uses).

**Instrumented root cause (fixed 13-node depth-3 tree):** decoded-checker
entered 58,138x, matcho 80,217x, but the clash search is CHEAP (atom-unify
1,606x). The multiplier is matcho REDUNDANT DELIVERY: for an identical
structured pair (forall/implies/and -- the construct-and-check norm) the exact
+ identity-binder-rename + reflexive-alpha arms ALL succeed, and fair mplus
interleaving explores each, compounding per nesting level. ADR-0145 deduped
literal heads but left the structured ladder.

**3c structured-head dedup (commit a7fe30c, gate 1460/0/0):**
sjas-proof-node-distincto (ground-check only) gates binder/compound/alpha
behind a non-identity check -> exact-arm-alone for identical pairs.
Answer-set-preserving. MEASURED: decoded-checker 58,138->11,583 (5x); matcho
->11,971 (~1x/entry); tiny 83->24.8s. **16-byte-arg depth-3, which OOM'd every
heap before, CLOSES in 158.7s in a ~3GB working set -- mid-scale unblocked.**

**Real-mass (47/96-byte) status: MEMORY-BOUND on this 15GB machine.** The real
Dk formula is ~7,749 app nodes (it embeds the system code) and appears at
P1/P2/dk/gamma-children; the residual ~11,583-state search keeps many live
branch-substitutions each referencing giant formulas, so the working set
exceeds available RAM (killed at 6.3GB RSS climbing, only ~7.5GB usable after
baseline). 16-byte fits (3GB) because its formulas are small; 96-byte does not.

**What closes it (own stage, soundness-critical):** reduce SIMULTANEOUS search
breadth so the giant-formula branches don't co-exist -- depth-first / ground-
mode committed choice, which needs a rule/clash PARTITION proof (each ground
node admits one rule; each leaf one clash target -- true for these trees but
must be proven) OR guaranteed formula sharing (no per-branch rebuild of the
embedded codes). The step-5 MECHANISM is validated end-to-end at mid-scale;
the real instance awaits this breadth/memory stage.
