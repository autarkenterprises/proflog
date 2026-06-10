# AAR-0078: SJAS Finite Table Lookup Scheduling

- Date: 2026-06-08
- ADR: [ADR-0078](../adr/ADR-0078-sjas-finite-table-lookup-scheduling.md)
- Branch: `adr-0073-sjas-correspondence-program`

## Outcome

Replaced recursive `membero` scans in fixed SJAS metadata lookup helpers with a
shared explicit finite-table relation. Static table entries now unify through
the local acyclic unifier; dynamic proof-state membership, such as literal-list
or environment membership, remains unchanged.

The non-invasive JVM sample that motivated the change showed the SelfCons core
probe spending time in `clojure.core.logic/occurs-check-worklist` via
`membero` and ordinary `==`. A later sample from an ADR-0078 run no longer had
`membero` or `occurs-check-worklist` at the top; it had moved into core.logic
stream/protocol scheduling.

## Evidence

The source-audit regression failed red before implementation:

```text
lein test :only proflog.willard-sjas-test/sjas-static-code-table-lookups-avoid-membero-scheduling
Ran 1 tests containing 14 assertions.
14 failures, 0 errors.
elapsed 1:52.51 maxrss 243368KB
```

After replacing static table scans, the same selector passed:

```text
Ran 1 tests containing 14 assertions.
0 failures, 0 errors.
elapsed 1:46.14 maxrss 280744KB
```

Focused semantic selectors that exercise formula/proof code tables passed:

```text
sjas-proof-code-decoder-checks-wide-formula-bearing-tableau-nodes
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
elapsed 3:01.38 maxrss 333264KB

sjas-tableau-proof-accepts-u-grounding-formula-bearing-proof-certificate
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 2:50.42 maxrss 379368KB
```

The loaded broad gates passed:

```text
lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 15:38.49 maxrss 370056KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 35:08.07 maxrss 935680KB
```

## Follow-up

The ADR-0078 SelfCons measurement was intentionally left running under
`test-runs/selfcons-core-adr78-20260608T220400Z.log`; it passed 15 minutes and
remained alive past 58 minutes, but it was launched before the final ADR-0077
guided-only literal cleanup. A latest-source run,
`test-runs/selfcons-core-latest-20260608T230532Z.log`, also remained alive past
15 minutes under heavy load. Static table scheduling fixed the sampled
`membero`/occurs-check hot path but is not sufficient by itself to make the core
SelfCons selector fast.
