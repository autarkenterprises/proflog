# AAR-0105: SJAS Proof-Search Substate Tabling Investigation

- Date: 2026-06-13
- ADR: [ADR-0105](../adr/ADR-0105-sjas-substate-tabling-investigation.md)
- Branch: `adr-0105-sjas-substate-tabling`

## Outcome

Investigated whether tabling makes the subst-prf negative-exhaustion wall
tractable. **Verdict: no — tabling is not the systemic fix here**, established by
measurement rather than plausibility.

- **Hotspot (JVM):** the grinding negative subst-prf is search-width-bound — the
  core.logic search trampoline (`_inc`/`eval2364`) + unification driving the
  relational decoder `parse-code-payload-byteso`. Constant-factor levers
  (ADR-0090/0094) are exhausted.
- **Re-derivation (measured):** with a conservative reify-keyed probe, the probed
  decoder `decode-syntax-formula-byteso` shows **1.00× re-derivation** (12 calls,
  12 distinct); the cheap top-level code reader shows 4.33× (13/3). The search
  explores *distinct* intermediate terms. Tabling removes only re-derivation, so
  it buys ~nothing here.
  - **Correction (ADR-0106):** an earlier draft inferred "~7.5 s each /
    intrinsically expensive decodes." That is wrong — ground decode is ~1 ms, and
    the grind is variable-dense decode + static-table enumeration in
    `decode_formula_byteso` / `decode_embedded_code_bodyo` (not the probed
    `decode-syntax-formula-byteso`). The re-derivation verdict is unaffected.
- **Tabling facilities surveyed:** core.logic `l/tabled` (reify-keyed,
  constraint-store-unaware → sound only constraint-free, guarded by ADR-0093) and
  `proflog.tabling` (ADR-0017 canonical-state keys capturing `sigma`/`neqs`,
  tabled-vs-untabled–agreement-tested, kernel-only). The SJAS profile search is
  untabled; that gap is real but not the bottleneck.

The real lever is **search-width reduction** (a relevance/structural prefilter on
substitution candidates before the expensive decode + alpha-equivalence, or a
non-provability decision that avoids exhaustion) — not memoization. Absent that,
the two `subst-prf` negatives remain the documented `^:slow` envelope-exceeders.

This is a deliberate negative result: it prevents future agents from building
tabling that the evidence says will not pay, and points the tractability work at
the correct (algorithmic, width-reducing) lever.

## Evidence

- [Measurement note](../log/2026-06-13-sjas-substate-tabling-measurement.md):
  methodology (the reify-keyed probe patch + `measure_substate.clj`) and the
  90 s-window result table.
- jstack of the grinding test (97-129% CPU): trampoline + unification +
  `parse-code-payload-byteso`.

The measurement scaffolding (probe goals) was reverted after measuring; no
kernel/checker behavior change remains. Broad gates were not re-run because no
source change is retained.

## Follow-up

- **Width reduction (the real tractability lever):** design a relevance/structural
  prefilter that prunes substitution candidates before the deep decode, or a
  decision procedure for the subst-prf negative — a successor ADR. This connects
  to the Track 2a relevance work (correspondence relevance) only by analogy; it is
  a *search* concern, not a correspondence one.
- If a future workload is found whose SJAS search *does* re-derive substates
  (high ratio), revisit option A via the `proflog.tabling` canonical-state
  substrate (not raw `l/tabled`) with the ADR-0017 + ADR-0093 safeguards.
