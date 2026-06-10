# AAR-0080: SJAS Application Arity Byte Dispatch

- Date: 2026-06-09
- ADR: [ADR-0080](../adr/ADR-0080-sjas-app-arity-byte-dispatch.md)
- Branch: `adr-0073-sjas-correspondence-program`

## Outcome

Changed SJAS application-term decoding so the encoded arity byte is
destructured once from the byte stream before dispatching to the finite
argument-list parser. The same scheduling shape is used for ordinary term
decoding, syntax-only term decoding, and syntax-skip term decoding.

The change preserves the arithmeticized stream representation: arity is still a
byte in the object-level SJAS code, and the existing term-list parsers still do
the recursive structural work. The only change is that wrong arity candidates
now fail against the already-read byte instead of repeatedly matching the same
tail as `(arity . args)`.

## Evidence

The source-audit regression failed red before implementation:

```text
lein test :only proflog.willard-sjas-test/sjas-app-arity-decoders-destructure-arity-byte-once
Ran 1 tests containing 6 assertions.
3 failures, 0 errors.
elapsed 0:18.02
```

After changing the three app-arity helpers, the same selector passed. The final
current-worktree run was:

```text
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 0:36.08 maxrss 248284KB
```

Focused semantic selectors stayed green:

```text
sjas-proof-code-decoder-checks-wide-formula-bearing-tableau-nodes
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
elapsed 0:39.63 maxrss 271776KB

sjas-tableau-proof-accepts-u-grounding-formula-bearing-proof-certificate
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 0:47.49 maxrss 436728KB
```

The post-change broad gates passed:

```text
lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 2:32.35 maxrss 422208KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 5:47.17 maxrss 659796KB
```

## Follow-up

The post-ADR-0079 SelfCons probe was preserved and passed its 15-minute
milestone, but it remains runtime evidence rather than a result gate. Per the
current project goal, no further proof-predicate optimization should be started
until ADR-0073 Track 1 arithmeticization is audited and completed.
