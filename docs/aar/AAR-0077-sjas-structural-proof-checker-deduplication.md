# AAR-0077: SJAS Structural Proof Checker Deduplication

- Date: 2026-06-08
- ADR: [ADR-0077](../adr/ADR-0077-sjas-structural-proof-checker-deduplication.md)
- Branch: `adr-0073-sjas-correspondence-program`

## Outcome

Removed redundant formula-bearing structural proof-checker alternatives:

- exact duplicate guided literal continuation;
- exact duplicate complementary literal closure;
- exact duplicate conjunction continuation;
- a later unguided literal continuation branch subsumed by the decoded
  child-formula guided path.

The checker still uses ordinary `conde`, formula-node matching, and structural
rule inference. No committed-choice scheduler or proof-rule tag shortcut was
introduced.

## Evidence

The initial source-audit regression failed red with duplicate branch counts:

```text
lein test :only proflog.willard-sjas-test/sjas-structural-proof-checker-does-not-duplicate-guided-branches
3 failures, 0 errors.
elapsed 1:53.77 maxrss 247544KB
```

After removing exact duplicate branches, the same selector passed. Extending the
audit to reject the subsumed unguided literal continuation then failed red:

```text
Ran 1 tests containing 4 assertions.
1 failures, 0 errors.
elapsed 2:21.44 maxrss 278572KB
```

After deleting the unguided literal branch, the selector passed:

```text
Ran 1 tests containing 4 assertions.
0 failures, 0 errors.
elapsed 2:21.38 maxrss 254664KB
```

Focused semantic selectors stayed green:

```text
sjas-proof-check-accepts-formula-bearing-complementary-literal-closures
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 2:49.47 maxrss 297832KB

sjas-proof-check-accepts-formula-bearing-right-first-conjunction-tableaux
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 2:37.14 maxrss 276800KB

sjas-proof-check-accepts-formula-bearing-and-true-false-tableaux
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 3:01.97 maxrss 276200KB
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

These broad timings were taken while two durable SelfCons proof probes were
also running, so they are correctness evidence, not clean runtime baselines.

## Follow-up

The cleanup reduces redundant structural alternatives, but it does not make the
SelfCons selector fast enough under the loaded conditions observed on
2026-06-08. The latest-source durable run was still alive at `15:52` elapsed.
Further work should focus on deeper proof-guided rule dispatch or formula-code
decoding costs rather than more exact duplicate removal.
