# AAR-0145: Ground-Proof Rule Dispatch — Proof-Node Match Deduplication

- Date: 2026-07-02
- ADR: [ADR-0145](../adr/ADR-0145-ground-proof-rule-dispatch.md)
- Branch: `adr-0145-ground-proof-rule-dispatch`

## Outcome

Completed. The ADR-0144 hand-off ("kill the ~78 K-node search via deterministic
ground-proof rule dispatch") landed as a **head-dispatched, literal-family
deduplicated `sjas-proof-node-formula-matcho`** — a pure relational change of
one relation plus two head-tag tables. The ground-tree wall benchmark collapsed
**15.9 s → ~3 s within-JVM paired** (~4–5.9×; cumulatively ~106 s → ~3 s across
ADR-0144+0145), with proof-check suites, the 131-assertion source audit, and
synthesis parity all green.

The decisive discovery: the "~78 K-node search" was dominated not by the 43
rule arms themselves but by **match-reconciliation duplication upstream of
them** — the 4-arm matcho ladder delivering the same success 2–4+ ways per node
(alpha is reflexive; rename-to-self is the identity; the compound arm compounds
recursively), each duplicate re-exploring the whole downstream subtree.
Instrumented per-arm counts (matcho 5 528 constructions; alpha pulled 8 705×;
quantifier arms pulled 16 926× — for a 4-node tree) located it in minutes.

## Red/green and measurement evidence

- Arm-level instrumentation first (ADR-0144 lesson 1): counts above.
- Prototype dedup via `alter-var-root`: benchmark 19.0 s → 2.6 s in-session;
  counts collapsed (quantifier pulls 16 926 → 3 024; alpha 8 705 → 456).
- Landed file version (fully relational dispatch): benchmark ~2.7–3.9 s warm at
  fuel 10 and 80 (fuel-flat, as diagnosed); all 8 proof-check tests
  (20 assertions, including the `^:slow` wall test itself) green; source audit
  131/0/0.
- Within-JVM paired baseline (original matcho restored by `alter-var-root` in
  the same JVM): benchmark 15 926 ms; synthesis 56.2 s — establishing both the
  speedup ratio and synthesis parity.

## The attribution detour (two wrong theories, one right protocol)

An initial beta-axiom synthesis reading of ~55–58 s against a remembered ~31 s
envelope looked like a 1.8× synthesis regression. Two mechanisms were
hypothesized and **disproven by prototype**: (i) relational-dispatch per-call
overhead — an O(1) host head-peek recognizer variant kept the benchmark
collapsed but synthesis stayed ~56 s; (ii) loss of duplicate-delivery
scheduler weight under `run 1` interleaving — a mode-scoped variant leaving the
free-node path bit-identical to baseline also stayed ~58 s. The decisive third
experiment restored the **original** matcho in the same JVM: 56.2 s. The ~31 s
figure was a different JVM on a different day; there was no regression at all.

## Lessons

1. **The bottleneck was upstream of the named suspect.** ADR-0144 named "the
   43-arm rule conde"; per-arm instrumentation showed the preamble match
   relation was the multiplier. Instrument arms before regrouping arms.
2. **Duplicate successes are as expensive as extra branches.** Overlapping
   relational arms that re-deliver the *same* state multiply downstream
   exploration exactly like genuine nondeterminism. Dedup arguments
   (identical-state deliveries) are as valuable as partition proofs — and
   easier to prove.
3. **Only same-JVM paired measurements count for search runtimes.** The false
   synthesis regression consumed two full prototype cycles because a
   cross-JVM/cross-day number was treated as a baseline. `alter-var-root`
   swap-in/swap-out within one warm JVM is the honest instrument (this is
   ADR-0144 lesson 4 sharpened: not just stale envelopes — *any* cross-JVM
   comparison).
4. **Completeness arguments beat mode tests.** The literal-family dedup is
   sound in every mode (binder-free alpha ≡ structural equality), so no
   host-level mode dispatch was needed; the purely relational form landed. The
   two rejected prototypes (host head-peek; lvar-mode scoping) were both
   answers to a phantom problem.

## Follow-ups

- **Quantifier-arm strategy ladder** (three `forall`/`once-forall` arms, two
  `exists` arms): the remaining deliberate nondeterminism (~3 024 residual
  pulls on the benchmark). These arms produce non-identical converging states,
  so collapsing them needs a real partition/subsumption proof — its own ADR if
  it becomes the binding cost on real ADR-0142 workloads.
- **Agenda selection** (`sjas-proof-guided-selecto`) remains proof-node-blind
  by design (source-audit-pinned); revisit only with workload evidence.
- The two ADR-0144 pre-existing landmines (SelfCons `^:slow` drift;
  `fitting-fidelity` sec7 cooperative deadline) remain open and unclaimed by
  this ADR.
