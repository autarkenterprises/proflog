# AAR-0123: SJAS Roadmap Integration Baseline

- Date: 2026-06-18
- ADR: [ADR-0123](../adr/ADR-0123-sjas-roadmap-integration-baseline.md)
- Branch: `adr-0123-sjas-roadmap-integration`

## Outcome

ADR-0123 is complete.

The integration branch starts from `origin/main` at `8800d70` and merges
`adr-0122-sjas-tab1-theorem-reuse`. The resolved baseline contains:

- ADR-0111 through ADR-0115 from current `main`;
- ADR-0116 through ADR-0118 from the Fitting audit line;
- ADR-0119 through ADR-0122 from the SJAS roadmap and completed Tab-1 line;
- fast-gate coverage for both `proflog.scheduling-benchmark-test` and
  `proflog.fitting-fidelity-test`.

The merge did not require semantic conflict resolution in SJAS source files.
Conflicts were limited to `LOG.md`, `docs/adr/README.md`,
`docs/aar/README.md`, and `project.clj`.

## Evidence

Focused selectors:

```text
lein test :only proflog.willard-sjas-test/tab1-proof-reuses-earlier-pi-star-1-theorem-as-axiom-citation
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.

lein test :only proflog.sjas-correspondence-test/tab1-roadmap-audit-reconciles-rank1-terminology
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 245 tests containing 2112 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 92 tests containing 971 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1107 fail=0 error=0
```

## Follow-up

- Workstream A's positive Tab-1 proof-list validation is integrated, but
  generic Tab-k and Tab-2-or-stronger variants remain Workstream B subjects
  unless a later ADR promotes them.
- Workstream B remains open: programmatized Goedel-boundary failure variants
  for total multiplication, Tab-2-or-stronger theorem reuse, and Xtab/LEM as an
  axiom schema.
- Workstream C remains open: reflected data-structure beta extensions, beginning
  with the required encoding survey before pairs/lists implementation.
