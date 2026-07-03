# ADR-0147 audit correction patch

Date: 2026-07-02
Branch: `adr-0147-theorem23-bot-closure`

## Summary

This note records the correction patch following the audit of commit `0d60252`
and the later ADR-0142/0147 boundary work. The audit verdict was:

- the `core.logic.nominal/-suspc` fix is plausible and narrow, but was missing
  an exact committed regression for the nil-state failure;
- the ADR-0147/AAR-0147 record still described the ADR as docs-only after later
  same-branch source stages landed;
- the synthesis guard checked host object shape (`lvar?`) but not whether those
  lvars were still unbound in the live core.logic substitution;
- the Theorem 2.3 / Workstream B completion claim must remain open until the
  cut-free combination trees, audited `(A)` bridge, and final boundary falsifier
  evidence land.

## Corrections in this patch

1. Exact nominal suspension regression.

   Added `nominal-suspension-short-circuits-after-failing-freshness` to
   `proflog.core-logic-nominal-hash-test`. The test builds the public
   `suspc` shape that exposed the bug: the suspension sees the same logic
   variable on both sides, then the state binds that variable to the first
   swapped nom. The first freshness check fails. The correct result is logical
   failure (`[]`), not a second freshness check against a nil state.

   Correctness criterion: this test must fail on the pre-`0d60252` nil-guard
   implementation by reaching the nil-state protocol error, and pass on the
   patched implementation by returning no answers.

2. State-aware synthesis independence.

   Added `state-fresh-lvar?`, `dataflow-independent-in-state?`, and
   `assert-dataflow-independent-in-state!` to `proflog.sjas-synthesis-guard`.
   The state-aware path walks each tuple component through the current
   core.logic state before accepting it as fresh. This closes the audit gap
   where an object could still be an lvar but already be bound before the proof
   relation is entered.

   Correctness criterion: an all-fresh tuple inside a live run is accepted; a
   tuple whose component was bound by an earlier goal is rejected.

3. ADR/AAR scope correction.

   Updated ADR-0147 and AAR-0147 to state that the first checkpoint was
   docs-only, but later same-branch source stages do exist. The record now
   explicitly says source-stage progress does not close Workstream B.

## Still not corrected by this patch

This patch is not a BOT-closure completion. The following remain required:

- checker-accepted cut-free combination trees for Theorem 2.3 steps 1/3/4/B;
- an audited decision on the `(A)` bridge, including the Level-0 SelfCons role
  and the addition-side falsifier for the excluded trivial target;
- concrete `FinAx4`, `leq`, and `Map` interpreted-closure evidence on the V5
  instances needed by the final construction;
- final multiplication-side closure plus addition-side non-closure evidence
  through the same public structural checker path;
- durable logs for any slow semantic probes used as evidence;
- a deliberate decision on the retained `logic/project` ground compact-code
  reader: either keep it as a documented, ground-only byte-reader optimization,
  or replace it with a fully relational reader if the current rigor bar requires
  no executable `project` on the proof path.

## Handoff

Treat this patch as a correction to the evidence discipline around ADR-0147, not
as a change to the roadmap status. A future completion patch should flip
`theorem23-closure-status` only after the remaining proof trees and falsifiers
are executable and verified.
