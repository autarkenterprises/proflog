# AAR-0094: Core.logic LVar Equality Fast Path

- Date: 2026-06-10
- ADR: [ADR-0094](../adr/ADR-0094-core-logic-lvar-equality-fast-path.md)
- Branch: `adr-0094-core-logic-lvar-equality`

## Outcome

Both vendored overlays carry the LVar-vs-LVar equality fast path:
type-hinted direct field access (`.-id` / `.-name`) replaces the
keyword-lookup callsite in the dominant case, with the original `IVar`
keyword branch kept verbatim for non-LVar implementors. `hashCode` was
already a cached field and is unchanged. The change is observationally
identical by the `valAt` case table; only cycles differ.

Measured effect, with baseline and patched passes run back to back under
identical machine conditions (the reniced `subst-prf` durable probe
grinding at priority 19 throughout):

| Measurement | Baseline | Patched | Improvement |
|---|---:|---:|---:|
| tableau0 `axiom-member` probe (query ms) | `24.0 s` | `16.2 s` | 1.48x |
| level1 `axiom-member` probe (query ms) | `40.1 s` | `32.5 s` | 1.23x |
| `query-generated-axioms` (wall, incl. startup) | `1:47.91` | `0:56.16` | 1.92x |
| `true-theorem-certificates` (wall) | `1:42.28` | `0:49.94` | 2.05x |
| `negating-witness` (wall) | `1:24.87` | `0:54.34` | 1.56x |

`proofs=1` and all assertion counts identical across passes. The
improvement is strongest on the lvar-dense whole-program vars, as the
stack analysis predicted; wall-clock rows include roughly 40 s of
JVM/lein startup, so their compute-portion deltas are larger than the
quoted ratios.

## Evidence

Semantic regressions on the patched overlays (the contract regression was
first run green on the original code, by design — the patch is
equivalence-preserving, so its red/green takes the performance-evidence
form):

```text
lein test :only proflog.core-logic-lvar-equality-test
                proflog.core-logic-ground-walk-test
                proflog.core-logic-occurs-check-test
Ran 9 tests containing 34 assertions.
0 failures, 0 errors.
```

Broad gates on the patched tree (now including the contract namespace): `lein test-proflog-fast` Ran 175 tests containing 691 assertions. 0 failures, elapsed 3:28.73; `lein test-proflog-extended` Ran 68 tests containing 203 assertions. 0 failures, elapsed 8:32.50 — both again faster than their immediate predecessors (3:40.68 / 8:58.27).

Durable logs: `test-runs/adr0094-baseline.log`,
`test-runs/adr0094-patched.log` in the ADR-0094 worktree.

## Follow-up

- The pre-change negative-exhaustion envelope continues to accrue in the
  reniced durable probe
  (`test-runs/subst-prf-negatives-uncapped-20260610T100008Z.log` in the
  main checkout). The post-0094 counterpart probe was launched 2026-06-10
  from this ADR's worktree, niced to the same priority 19
  (`test-runs/subst-prf-negatives-post0094-20260610T180416Z.log`, pid file
  beside it): the pair gives the before/after envelopes for the multi-hour
  negatives, and the post-0094 run doubles as evidence on whether the
  negatives can complete at all — which bears on future semantic testing
  if they cannot.
- Candidate upstream proposal alongside the ADR-0090 fast path.
- The deeper representational change (integer-id-keyed substitutions)
  remains shelved as upstream-divergent.
