# AAR-0085: SJAS Structural Quantifier Sibling Scheduling

- Date: 2026-06-09
- ADR: [ADR-0085](../adr/ADR-0085-sjas-structural-quantifier-sibling-scheduling.md)
- Branch: `adr-0073-sjas-correspondence-program`

## Outcome

Completed the final ADR-0073 Track 1 MVP repair. The public
`tableau-proof/3(s,t,p)` Group-3 SelfCons certificate now validates through the
formula-bearing, arithmeticized SJAS proof predicate on current source.

The repair preserved relational purity. It did not introduce host proof
checking, host byte projection, generated proof registries, mutation-backed
caches, or committed-choice dispatch. The changes were relational scheduling
and representation fixes:

- delayed structural agenda entries now carry `[formula env]` snapshots;
- branch selection enumerates saved agenda candidates and leaves proof-node
  formula validation to the decoded structural checker;
- Tableau-0 Group-3 antecedent reconstruction keeps the walked public
  `system-code` term so compact and U-Grounding embeddings are selected by
  object structure.

## Evidence

Red evidence before the repair:

```text
timeout 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-selfcons-core-tableau
exit 124, elapsed 3:00.01

timeout 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-in-memory-formula-bearing-selfcons-tableau
exit 124, elapsed 3:00.00

timeout 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-selfcons-tableau
exit 124, elapsed 3:00.00
```

Focused green evidence after the repair:

```text
sjas-proof-check-accepts-formula-bearing-selfcons-core-tableau
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
elapsed 1:08.67 maxrss 667564KB

sjas-proof-check-accepts-in-memory-formula-bearing-selfcons-tableau
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 2:23.34 maxrss 989076KB

sjas-proof-check-accepts-formula-bearing-selfcons-tableau
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 2:20.39 maxrss 1036916KB

sjas-tableau-proof-accepts-formula-bearing-selfcons-certificate
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 2:17.71 maxrss 992768KB
```

Purity/source and broad regression gates passed:

```text
sjas-profile-source-audit-rejects-host-proof-checker-route
Ran 1 tests containing 128 assertions.
0 failures, 0 errors.
elapsed 0:17.19 maxrss 239172KB

sjas-structural-proof-checker-preserves-delayed-sibling-environments
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
elapsed 0:23.93 maxrss 238896KB

sjas-proof-check-preserves-delayed-sibling-scope-after-quantifiers
Ran 1 tests containing 1 assertions.
0 failures, 0 errors.
elapsed 0:24.48 maxrss 250348KB

lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 1:56.85 maxrss 407724KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 4:34.15 maxrss 606528KB

git diff --check
clean
```

## Follow-up

ADR-0073 Track 1 is complete by its public SelfCons MVP criterion. Track 2
work can resume from this proof predicate shape; further performance work
should remain subordinate to preserving the internalized/arithmeticized proof
relation.
