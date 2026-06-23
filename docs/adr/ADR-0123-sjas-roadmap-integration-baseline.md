# ADR-0123: SJAS Roadmap Integration Baseline

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0123-sjas-roadmap-integration`
- AAR: [AAR-0123](../aar/AAR-0123-sjas-roadmap-integration-baseline.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) spawned the next SJAS
research phase. Workstream A then produced [ADR-0120](ADR-0120-sjas-tab1-proof-list-surface.md),
[ADR-0121](ADR-0121-sjas-tab1-entry-validation.md), and
[ADR-0122](ADR-0122-sjas-tab1-theorem-reuse.md), completing the positive
Tab-1 proof-list validation path at the theorem-reuse level.

Those branches were developed on a line that also contained the Fitting
fidelity audit work from ADR-0116 through ADR-0118. Meanwhile `main` advanced
with ADR-0111 through ADR-0115: the D_SJAS counting lemma and Proflog tableau
fidelity improvements, including the literature golden suite, diagnostic proof
rendering, open-branch witness extraction, and scheduling benchmarks.

Future ADR-0119 workstreams should not be built on a split history. Workstream B
will intentionally construct negative SJAS variants, and Workstream C will
change reflected beta content; both require a baseline that simultaneously
contains the current Proflog tableau-fidelity work and the completed Tab-1 SJAS
surface.

## Decision

Create an integration branch from `origin/main` and merge
`adr-0122-sjas-tab1-theorem-reuse` into it before starting further ADR-0119
implementation work.

Conflict resolution is limited to preserving both sides of shared project
metadata:

- keep the ADR/AAR index entries for ADR-0111 through ADR-0122;
- keep the chronological development-log entries for both the mainline
  Proflog tableau-fidelity work and the SJAS Tab-1 work;
- include both `proflog.scheduling-benchmark-test` and
  `proflog.fitting-fidelity-test` in the fast gate.

No semantic SJAS or Proflog behavior is changed by ADR-0123 beyond the merged
source already accepted by the source ADRs.

## Consequences

- ADR-0119 continuation work can branch from a baseline containing current
  `main`, the Tab-1 proof-list machinery, and the Fitting audit tests.
- Workstream B negative-variant work will inherit the literature golden suite,
  proof diagnostics, witness extraction, and scheduling benchmark coverage.
- Workstream C self-extension work will inherit the completed Tab-1 profile and
  measured proof-list object support.
- Any later failure in the integrated branch cannot be excused as merely a
  branch-history artifact; it must be debugged against the merged baseline.

## Test Obligations

Because ADR-0123 is an integration ADR, the red/green evidence comes from
merge-conflict resolution and cross-branch regression gates rather than a new
feature-specific failing selector.

Required green evidence:

- focused Tab-1 theorem-reuse selector;
- focused Tab-1 correspondence-audit selector;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- The integration branch merges `adr-0122-sjas-tab1-theorem-reuse` onto
  `origin/main`.
- Conflict resolutions preserve both sets of indexed documentation and both
  fast-gate namespaces.
- Fast, extended, and focused SJAS gates pass on the integrated branch.
- An AAR records the integration evidence and names the remaining ADR-0119
  workstreams.
