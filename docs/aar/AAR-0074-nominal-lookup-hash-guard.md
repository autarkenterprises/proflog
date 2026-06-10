# AAR-0074: Nominal Lookup Hash Guard

- Date: 2026-05-27
- ADR: [ADR-0074](../adr/ADR-0074-nominal-lookup-hash-guard.md)
- Branch: `adr-0073-sjas-correspondence-program`

## Outcome

Implemented the nominal freshness guard required when a relational
association-list lookup skips a nominal key. The fix applies to:

- `cljtap.alphaleantap-e/lookupo`
- `cljtap.alphaleantap-ep/lookupo`
- `proflog.subst/lookupo`
- `proflog.equality/lookupo`
- `proflog.kernel.first-order/lookupo`

The researched core.logic concern is not present in the currently used
dependency versions for the tested behavior. Core.logic `nom/hash` rejects the
delayed self-aliasing case under the default 1.0.1 dependency, the 1.1.1
profile, and the vendored 1.1.1 source overlay.

## Evidence

The new regression first failed in every local unguarded lookup relation by
returning `(:first :second)` where the nominal finite-map invariant permits only
`[:first]`. After the guard was added, all focused regressions passed.

The normal greenfield regression path passed:

```text
lein test-proflog-fast
Ran 158 tests containing 580 assertions.
0 failures, 0 errors.
```

## Follow-up

Generic association-list relations outside nominal environments were left
unchanged. Future nominal-key lookup helpers should either reuse the guarded
relations or carry the same `nom/hash` recursion guard from the start.
