# ADR-0105: SJAS Proof-Search Substate Tabling Investigation

- Status: accepted (investigation; verdict: do not pursue tabling for this wall)
- Date: 2026-06-13
- Branch: `adr-0105-sjas-substate-tabling`
- AAR: [AAR-0105](../aar/AAR-0105-sjas-substate-tabling-investigation.md)

## Context

The subst-prf negative-exhaustion tests (e.g.
`sjas-subst-prf-checks-selfcons-fixed-point-certificate`, 3rd assertion) are the
SJAS tractability wall: proving non-existence forces exhaustion of a large
fuel-bounded search (documented to exceed 25-45 min and run 137:53 CPU without
completing).

A fresh JVM hotspot sample of that grinding test (97.5% CPU) shows the cost is
**search width**, not a slow relation: the core.logic search trampoline
(`eval2364`/`_inc`) dominates, with unification (`bind`, `unify_terms`) and the
pure code decoders (`parse-code-payload-byteso`) in the inner loop. This
confirms the ADR-0090/0094 finding that constant-factor levers are exhausted; the
remaining lever is algorithmic.

The project already has two tabling facilities:

- **core.logic `l/tabled`** — SLG resolution keyed on `(-reify a argv)`, the
  reified *argument terms only*. The cKanren constraint store is **not** captured
  by the answer cache, so it is sound only for constraint-free relations.
  Safeguarded by ADR-0093's `tabling-preserves-answers-and-ground-keys`.
- **`proflog.tabling`** (ADR-0017) — wraps `l/tabled` behind a **canonical
  branch-state key** (folding alpha-equivalent vars/pars/bindings, reordered
  agenda/lits/disequalities, walked substitution) so the explicit `sigma`/`neqs`
  state is captured. Proven correct by a tabled-vs-untabled agreement test. It
  tables `proflog.kernel`, **not** the SJAS profile checker.

The SJAS profile search (`sjas-proof-check-stateo`,
`sjas-structural-proof-check-state-decodedo`, `subst-code`, `subst-prf`) runs
**untabled** — the systemic gap.

## Decision

Tabling helps only if the search re-derives duplicate canonical substates;
otherwise it is pure overhead. Before building any tabling, **measure the
re-derivation rate** (this ADR), then choose:

- **A. Canonical-state tabling of `sjas-proof-check-stateo`** — extend ADR-0017's
  canonical key to the richer SJAS proof-check state. Highest payoff; the
  canonical key captures `sigma`/`neqs`, dodging `l/tabled`'s constraint-loss.
- **B. Table the pure decoders** (`parse-code-payload-byteso`, code/formula
  readers) via `l/tabled` — sound because they are constraint-free. Lower-risk,
  narrower win.
- **C. Width reduction** (relevance prefilter) — not tabling; the alternative if
  re-derivation is low.

This ADR adds a **measurement-only** hook: `*sjas-search-stats*` and the
`sjas-search-probeo` goal record, per node visit, a conservative reified
branch-state key. The hook is `nil` (zero-overhead no-op) outside measurement.
The key never over-merges (it includes `sigma` and uses the core.logic
reifier), so a high `:total`/`:distinct` ratio is sound evidence of
re-derivation; a ratio near 1 is a conservative lower bound that would warrant a
fuller canonical-key remeasurement before concluding.

## Measurement

Negative subst-prf `subst-prf(S, S, group3, sjas-axiom)` on a `:willard-sjas-level1`
system, sampled by running the real query (fuel 120) in a future for a 90 s wall
window with `*sjas-search-stats*` bound, reading the live counters. Probes at the
top-level code reader (`sjas-formal-code-bytes-coreo`) and — after a call-graph
check showed the jstack hotspot `parse-code-payload-byteso` lives in formula
decoding, not the top-level reader — at the formula decoder
(`decode-syntax-formula-byteso`). The `:node` probe (tableau checker) never
fired, confirming the negative subst-prf bypasses the structural checker via the
`sjas-axiom` citation path.

| probe (relation) | total calls (90 s) | distinct keys | revisit ratio |
|---|---:|---:|---:|
| `:decode` — `sjas-formal-code-bytes-coreo` (top-level code read) | 13 | 3 | 4.33× |
| `:formula-decode` — `decode-syntax-formula-byteso` (hotspot path) | 12 | 12 | **1.00×** |

The key is the core.logic reifier hashed (conservative: never over-merges).
jstack of the same grind shows the time is in the core.logic search trampoline +
unification driving `parse-code-payload-byteso`, i.e. *inside* the ~12 distinct,
~7.5 s-each formula decodes.

## Conclusion: tabling is **not** the systemic fix here

The hot relation has **1.00× re-derivation** — every expensive formula-decode is
on a *distinct* byte sequence, because the substitution search genuinely produces
*distinct* candidate formulas. Tabling only removes re-derivation, so it buys
essentially nothing on the hot path; even the cheap top-level reads repeat only
4.33×. The "high re-derivation ⇒ tabling transforms it" hypothesis is **refuted
by measurement**.

- **Option A (canonical-state tabling)** — not justified: the search does not
  re-derive substates at a rate that would pay for the tabling overhead.
- **Option B (table the pure decoders)** — sound (decoders are constraint-free)
  but a bounded ≤4.33× on the *cheap* reads and 1× on the expensive path; not a
  tractability change. Not worth the hot-path complexity on this evidence.
- **Option C (reduce search width)** — the real lever: the negative exhausts a
  search that generates *many distinct, intrinsically expensive* formula decodes.
  Tractability requires cutting *how many distinct candidates are decoded* — a
  relevance/structural prefilter on the substitution candidates before the deep
  decode + alpha-equivalence, or a non-provability decision that avoids the
  exhaustion — not memoization. Absent that, these `subst-prf` negatives remain
  the documented `^:slow` envelope-exceeders.

## Correctness safeguard (for the record)

Had tabling been justified, it would have been gated by ADR-0093's core.logic
`l/tabled` regressions (substrate soundness), ADR-0017's tabled-vs-untabled
*agreement* discipline (no over-merging), and a new SJAS tabled-vs-untabled test.
The canonical-state approach (`proflog.tabling`) — not raw `l/tabled` — is the
only sound substrate, because `l/tabled`'s answer cache drops the cKanren
constraint store, while the SJAS search carries `sigma`/`neqs` explicitly.

## Correctness safeguard

Any tabling of SJAS relations is gated by: ADR-0093's core.logic `l/tabled`
regressions (substrate soundness), ADR-0017's tabled-vs-untabled *agreement*
discipline (no over-merging), and a new SJAS tabled-vs-untabled test asserting
the proof/answer set is identical with tabling on vs off. Governing rule
(ADR-0017): conservative under-merging over unsound over-merging.

## Test Obligations

- The measurement hook is a no-op when unbound; broad gates stay green.
- The measurement is recorded with its fuel/ratio table.

## Exit Criteria

- The re-derivation ratio is measured and recorded; the A/B/C decision is made on
  the evidence (verdict: C, not tabling); AAR-0105 records the result.
- The measurement scaffolding (the `*sjas-search-stats*`/`sjas-search-probeo`
  probe goals) is reverted to keep the kernel clean; the methodology is
  documented in
  [the measurement note](../log/2026-06-13-sjas-substate-tabling-measurement.md)
  so the result is reproducible.
