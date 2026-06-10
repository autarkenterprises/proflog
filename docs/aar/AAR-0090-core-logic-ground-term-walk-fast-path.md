# AAR-0090: Core.logic Ground-Term Walk Fast Path

- Date: 2026-06-10
- ADR: [ADR-0090](../adr/ADR-0090-core-logic-ground-term-walk-fast-path.md)
- Branch: `adr-0088-sjas-runtime-rebaseline`

## Outcome

The vendored core.logic overlays (1.0.1 default and 1.1.1 opt-in) carry the
ground-term fast path: a conservative `ground-tree?` worklist scanner, a
`::ground` metadata tag applied at `ext` bind time **and to ground `walk*`
results**, tagged short-circuits in `walk*` and the occurs-check worklist,
and copy-on-write `walk-term` rebuilds for `ISeq`, `IPersistentVector`, and
`LCons`. The upstream substitution-relative `ground-term?` is untouched;
the scanner deliberately rejects maps, sets, records, and nominal
structures so they are never tagged or skipped.

The design iterated once on evidence. The first cut tagged only at bind
time, on the theory that bound code terms dominated; both `axiom-member`
probe cases still hit their 15-minute caps, because the hot SJAS code
terms are ground literals inside walked formula structures that never pass
through `ext` whole. Tagging ground `walk*` results closed the loop: walked
chunks carry their tags forward through kernel states, so each re-walk is
constant after the first scan. Per the ADR-0032 memoization lesson, the
scan aborts at the first variable and accepts tagged subtrees without
descent, avoiding the cache-without-avoided-work regression mode.

## Evidence

Focused regression (red before the patch — 5 failures across the
copy-on-write and tagging assertions — then green; revised once during the
iteration to assert the stable contract, value equality plus tagged
fixed-point re-walk identity, rather than raw first-walk identity):

```text
lein test :only proflog.core-logic-ground-walk-test
Ran 4 tests containing 20 assertions.
0 failures, 0 errors.
```

ADR-0075 occurs-check regression on the patched overlay:

```text
lein test :only proflog.core-logic-occurs-check-test
Ran 1 tests containing 2 assertions.
0 failures, 0 errors.
```

ADR-0088 bisect probe (`proflog.sjas-runtime-probe`, fuel 64, one JVM per
case, `proofs=1` preserved in every completing case):

| Case | Pre-patch | Bind-tag only (v1) | With result tagging (v2) |
|---|---|---|---|
| tableau0 `beta` | `4.77 s` | `4.45 s` | (unchanged path) |
| level1 `beta` | `2.98 s` | `4.34 s` | (unchanged path) |
| tableau0 `axiom-member` | exceeded `15:00` cap | exceeded `15:00` cap | `21.4 s` |
| level1 `axiom-member` | exceeded `15:00` cap | exceeded `15:00` cap | `34.7 s` |

Semantic selectors and broad gates on the patched tree:

```text
seven-selector semantic batch (the four ADR-0087 selectors, the Level-1 and
tableau0 Group-3 citation selectors, and the 128-assertion profile source
audit, one JVM):
Ran 7 tests containing 145 assertions.
0 failures, 0 errors.
elapsed 1:57.44 maxrss 394916KB
(the two citation selectors alone took 2:44.07 and 2:58.16 each in the
AAR-0089 evidence)

lein test :only proflog.core-logic-occurs-check-test
Ran 1 tests containing 2 assertions, 0 failures, 0 errors.

lein test-proflog-fast (now includes proflog.core-logic-ground-walk-test)
Ran 171 tests containing 679 assertions.
0 failures, 0 errors.
elapsed 4:31.95 maxrss 417088KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 13:10.74 maxrss 548088KB
```

Both gates are faster than their pre-patch same-day baselines (4:31.95 vs
5:51.24 fast, 13:10.74 vs 14:26.15 extended) despite the fast gate gaining
a namespace, so the scan overhead is paid for even on non-SJAS workloads.

## Follow-up

- The ADR-0088 namespace sweep re-baselines every SJAS var on this patched
  state; the formerly multi-hour vars are expected to re-enter ordinary
  envelopes, which the sweep verifies var by var.
- Upstreaming: the patch is intentionally local to the vendored overlays.
  If it proves stable across the full baseline, proposing it upstream to
  core.logic (with the conservative scanner documented) is a candidate
  follow-up.
