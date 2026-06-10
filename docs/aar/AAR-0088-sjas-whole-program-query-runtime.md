# AAR-0088: SJAS Whole-Program Query Runtime Re-Baseline

- Date: 2026-06-10
- ADR: [ADR-0088](../adr/ADR-0088-sjas-whole-program-query-runtime.md)
- Branch: `adr-0088-sjas-runtime-rebaseline`

## Outcome

All three ADR-0088 decisions completed, with the investigation resolving
into three subordinate ADRs:

1. **Mechanism identified and fixed at the core.logic layer.** The bisect
   probe (`proflog.sjas-runtime-probe`) isolated the grind to
   `axiom-member` citations (beta queries through the same programs run in
   seconds), and stack samples placed the cost in `walk-term` rebuild
   churn and occurs-check rescans over large ground code terms.
   [ADR-0090](../adr/ADR-0090-core-logic-ground-term-walk-fast-path.md)
   — ground-term tag-on-bind and tagged-result short-circuits with
   copy-on-write rebuilds, applied to both vendored overlays per the
   doctrine of optimizing core.logic rather than complexifying
   SJAS/Proflog — took both probe cases from exceeding 15-minute caps to
   `21.4 s` / `34.7 s`, and both broad gates ran faster than their
   pre-patch baselines.
2. **The namespace is re-baselined var by var** on the patched tree:
   the 137-var bulk lane completed in `13:35.26` wall in one JVM
   (slowest var `2:56`), where the pre-patch projection was hours; the
   nine heavy vars ran one JVM each under `timeout 1500`. The complete
   per-var table is
   [SJAS_RUNTIME_BASELINE_2026-06-10](../SJAS_RUNTIME_BASELINE_2026-06-10.md).
   The sweep was the first full namespace run since the ADR-0086-era
   slowdown and surfaced two latent semantic defects, repaired with
   red/green discipline as
   [ADR-0091](../adr/ADR-0091-sjas-citation-evidence-restoration.md)
   (citation evidence dropped by the e248c8b marker summary) and
   [ADR-0092](../adr/ADR-0092-sjas-nnf-pi-star-1-encodability.md)
   (over-strict Pi*1 validation) — exactly the "evidence of correct
   semantics" the re-baseline doctrine asks slow tests to provide.
3. **The gate is visibly partitioned.** `lein test-proflog-sjas` now runs
   the `:not-slow` namespace; `^:slow` semantic probes stay in
   `lein test-proflog-sjas-slow` with their envelopes (or
   exceeds-envelope markers) recorded in the baseline. The final partitioned gate run: `:SELECTION proflog.willard-sjas-test 138 tests (not-slow)`, `:SUMMARY pass=988 fail=0 error=0`, elapsed `6:46.34`, maxrss `536580KB`. The full-namespace run also exposed and resolved the e248c8b-era contract contradiction: tests pinning evidence absence (`accepts-axiom-citation-certificates`, the U-Grounding citation checks) were flipped to the restored evidence-bearing contract alongside the tests that always demanded the steps, and the remaining source-shape tests were repointed at the shared preamble window.

## Evidence

Bulk lane (post-ADR-0090 tree, commit `3a80e39`):

```text
137 vars, one JVM, wall 13:35.26 maxrss 891796KB
:SUMMARY pass=963 fail=5 error=1   (the six assertions repaired by
ADR-0091/0092; 134 vars green)
```

Heavy lane (final tree, one JVM per var, `timeout 1500`): seven of nine
vars pass in `0:44`-`1:45` each — including
`sjas-tableau0-and-level1-query-generated-axioms-through-selected-profile`
at `1:03.39`, which had burned over two CPU-hours before ADR-0090 and
exceeded a 40-minute cap at `1fa3e53` — while the two `subst-prf`
negative-exhaustion probes (`...checks-selfcons-fixed-point-certificate`,
`...rejects-selfcons-complement-axiom-certificate`, both `^:slow`) exceed
the 25-minute cap; their true envelopes are being established by the
uncapped durable probe below. Full rows in the baseline table.

Repairs and gates are evidenced in AAR-0090, AAR-0091, and AAR-0092.

## Follow-up

- The uncapped durable probe for the two `subst-prf` negatives is running
  per AGENTS.md practice 17
  (`test-runs/subst-prf-negatives-uncapped-20260610T100008Z.log`, pid file
  beside it); per the user's doctrine, very long negatives are legitimate
  semantic evidence, and this AAR will be updated with their final
  envelopes. If those envelopes stay above practical bounds, the candidate
  deeper investigation is the diagonal-substitution failure space, again
  core.logic-first.
- Proposing the ADR-0090 fast path upstream to core.logic once it has
  soaked across the baseline.
- Widening `subst-prf` answer evidence is deferred to the Track 2a
  relevance matrix (AAR-0091 follow-up).
- ADR-0073 Track 2a remains the next program work item.
