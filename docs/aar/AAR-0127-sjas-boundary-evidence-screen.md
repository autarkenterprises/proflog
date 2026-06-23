# AAR-0127: SJAS Boundary Evidence Screen

- Date: 2026-06-18
- ADR: [ADR-0127](../adr/ADR-0127-sjas-boundary-evidence-screen.md)
- Branch: `adr-0127-sjas-boundary-evidence-screen`

## Outcome

ADR-0127 is complete as an evidence-intake guard for Workstream B.

The implementation adds:

- `boundary-final-evidence-obligations`, the shared final evidence set
  `#{:constructed-certificate :proof-search-synthesis}`;
- `screen-boundary-evidence`, a cheap screen for candidate Workstream B
  evidence records;
- audit metadata under `:evidence-screens :total-multiplication`.

The screen rejects ordinary Group-3 `sjas-axiom` citation, ordinary structural
SelfCons tableau evidence, wrong generated system/target codes, missing
reduced-witness dependency, and synthesis claims without durable logs.

Candidates that pass the screen return `:verification-required` with no
completed obligations. This ADR therefore prevents false positives without
claiming either remaining total-multiplication evidence obligation.

## Evidence

Initial red selectors failed as intended at compilation because the public
screen helper did not exist:

```text
No such var: correspondence/screen-boundary-evidence
```

Focused green selectors:

```text
boundary-failure-roadmap-keeps-witness-contract-open
Ran 1 tests containing 10 assertions.
0 failures, 0 errors.

boundary-evidence-screen-rejects-trivial-selfcons-evidence
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.

boundary-evidence-screen-keeps-nontrivial-candidates-pending
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 223 tests containing 1393 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1154 fail=0 error=0
```

## Follow-up

- Build a real constructed total-multiplication contradiction certificate that
  passes this screen and then validates against the ADR-0126 target.
- Add proof-search synthesis evidence for the same target with durable
  `test-runs/` logs and PID files.
- Tab-2-or-stronger and Xtab/LEM-as-axiom variants remain open.
