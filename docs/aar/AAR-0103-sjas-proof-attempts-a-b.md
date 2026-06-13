# AAR-0103: SJAS Proof Attempts A and B

- Date: 2026-06-13
- ADR: [ADR-0103](../adr/ADR-0103-sjas-proof-attempts-a-b.md)
- Branch: `adr-0103-sjas-proof-attempts-a-b`

## Outcome

Path A and Path B were advanced to executable branch-level proof audits.
Neither proof is complete yet.

Path A now has a narrowed branch inventory: direct Willard branches, lemma
branches, and excluded SJAS-extension branches are separated explicitly.

Path B now has a candidate `D_SJAS` apparatus inventory: every structural
checker branch is classified under one or more candidate rule families, and the
remaining blockers are recorded as open obligations.

## Evidence

The red test failed before implementation:

```text
lein test proflog.sjas-correspondence-test
Syntax error compiling at (proflog/sjas_correspondence_test.clj:385:17).
No such var: correspondence/audit-path-a-narrow-rule-inventory
Tests failed.
```

After implementation:

```text
lein test proflog.sjas-correspondence-test
Ran 28 tests containing 397 assertions.
0 failures, 0 errors.
```

Before commit:

```text
lein test-proflog-fast
Ran 199 tests containing 1046 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 73 tests containing 219 assertions.
0 failures, 0 errors.
```

## Follow-up

- Path A: prove the six named lemmas and add a final theorem note for the narrow
  fragment.
- Path B: choose `sjas-axiom` proof-object accounting, then formalize
  `D_SJAS` and prove literature admissibility or move the result to Track 2c.
